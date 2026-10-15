package io.github.raulramalf.browseme.infrastructure.fetch;

import io.github.raulramalf.browseme.application.port.out.FetchFailedException;
import io.github.raulramalf.browseme.application.port.out.PageFetcher;
import io.github.raulramalf.browseme.domain.analysis.AnalysisErrorCode;
import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.page.RobotsTxt;
import io.github.raulramalf.browseme.domain.page.RobotsTxtStatus;
import io.github.raulramalf.browseme.domain.url.AnalyzedUrl;
import io.github.raulramalf.browseme.domain.url.InvalidUrlException;
import io.github.raulramalf.browseme.domain.url.UrlPolicy;
import io.github.raulramalf.browseme.domain.url.UrlRejectedException;
import io.github.raulramalf.browseme.infrastructure.fetch.BoundedBodyReader.BodyTooLargeException;
import io.github.raulramalf.browseme.infrastructure.fetch.JsoupSnapshotExtractor.ResponseInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Descarga segura de una sola página (apartado 6 del prompt de desarrollo).
 *
 * <ol>
 *   <li>Cada URL, incluida cada redirección, pasa por {@link UrlPolicy#parse}.</li>
 *   <li>{@link ValidatingDns} resuelve una vez y rechaza si alguna IP está prohibida;
 *       OkHttp conecta a esas IPs sin volver a resolver.</li>
 *   <li>Un interceptor de red comprueba la dirección real de la conexión antes de
 *       enviar nada: cubre también los hosts que son IPs literales, para los que
 *       OkHttp no consulta el DNS.</li>
 *   <li>Redirecciones manuales (máximo {@link FetchLimits#maxRedirects()}), con un
 *       único plazo total para toda la cadena.</li>
 *   <li>Cuerpo leído con tope sobre los bytes descomprimidos; solo {@code text/html}.</li>
 *   <li>Sin cookies, sin caché, sin proxy y sin cabeceras del usuario.</li>
 * </ol>
 */
public class SafeHttpPageFetcher implements PageFetcher {

    public static final String DEFAULT_USER_AGENT = "BrowseMeBot/0.1 (+[COMPLETAR: URL informativa])";
    /** Token con el que BrowseMeBot se identifica en robots.txt. */
    static final String OWN_ROBOTS_AGENT = "browsemebot";
    /** robots.txt se evalúa para Google, que es lo que importa al usuario. */
    static final String SEARCH_ENGINE_ROBOTS_AGENT = "googlebot";

    private static final Set<Integer> REDIRECT_CODES = Set.of(301, 302, 303, 307, 308);

    private final UrlPolicy policy;
    private final OkHttpClient client;
    private final FetchLimits limits;
    private final String userAgent;
    private final JsoupSnapshotExtractor extractor = new JsoupSnapshotExtractor();

    public SafeHttpPageFetcher(UrlPolicy policy, FetchLimits limits, String userAgent) {
        this(policy, limits, userAgent, HostResolver.system(), SocketFactory.getDefault());
    }

    /** Constructor con costuras para tests: resolución DNS y sockets sustituibles. */
    SafeHttpPageFetcher(UrlPolicy policy, FetchLimits limits, String userAgent,
                        HostResolver resolver, SocketFactory socketFactory) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.userAgent = Objects.requireNonNull(userAgent, "userAgent");
        this.client = new OkHttpClient.Builder()
                .dns(new ValidatingDns(resolver, policy))
                .socketFactory(socketFactory)
                .proxy(Proxy.NO_PROXY)
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(false)
                // Sin reutilizar conexiones: cada salto vuelve a pasar por DNS y validación.
                .connectionPool(new ConnectionPool(0, 1, TimeUnit.SECONDS))
                .connectTimeout(limits.connectTimeout())
                .readTimeout(limits.readTimeout())
                .writeTimeout(limits.readTimeout())
                .addNetworkInterceptor(chain -> {
                    InetAddress connected = chain.connection().route().socketAddress().getAddress();
                    if (policy.isForbidden(connected)) {
                        throw new ForbiddenAddressException(chain.request().url().host());
                    }
                    return chain.proceed(chain.request());
                })
                .build();
    }

    @Override
    public PageSnapshot fetch(AnalyzedUrl url) {
        RobotsResult requestedRobots = fetchRobots(url.uri());
        if (requestedRobots.robots() != null && requestedRobots.robots().namesAgent(OWN_ROBOTS_AGENT)
                && !requestedRobots.robots().isAllowed(OWN_ROBOTS_AGENT, pathAndQuery(url.uri()))) {
            throw new FetchFailedException(AnalysisErrorCode.ROBOTS_DISALLOWED,
                    "robots.txt prohíbe el acceso a BrowseMeBot");
        }

        long deadline = System.nanoTime() + limits.totalTimeout().toNanos();
        URI current = url.uri();
        int redirects = 0;
        while (true) {
            Request request = request(current, "text/html,application/xhtml+xml;q=0.9,*/*;q=0.1");
            try (Response response = execute(request, deadline)) {
                int status = response.code();
                if (REDIRECT_CODES.contains(status)) {
                    if (redirects >= limits.maxRedirects()) {
                        throw new FetchFailedException(AnalysisErrorCode.TOO_MANY_REDIRECTS,
                                "Más de " + limits.maxRedirects() + " redirecciones");
                    }
                    current = nextHop(response);
                    redirects++;
                    continue;
                }

                RobotsTxtStatus robotsStatus = robotsStatus(url.uri(), current, requestedRobots);
                ResponseInfo info = new ResponseInfo(url.uri(), current, status, redirects,
                        response.headers("X-Robots-Tag"), robotsStatus);
                if (status >= 400) {
                    return extractor.errorSnapshot(info);
                }
                if (status < 200 || status >= 300) {
                    throw new FetchFailedException(AnalysisErrorCode.HTTP_ERROR,
                            "Respuesta HTTP inesperada: " + status);
                }
                ResponseBody body = response.body();
                MediaType type = body.contentType();
                if (type == null || !type.type().equals("text") || !type.subtype().equals("html")) {
                    throw new FetchFailedException(AnalysisErrorCode.UNSUPPORTED_CONTENT_TYPE,
                            "Solo se analizan páginas text/html");
                }
                if (body.contentLength() > limits.maxBodyBytes()) {
                    throw new FetchFailedException(AnalysisErrorCode.BODY_TOO_LARGE,
                            "La página supera " + limits.maxBodyBytes() + " bytes");
                }
                byte[] html;
                try (InputStream in = body.byteStream()) {
                    html = BoundedBodyReader.readAtMost(in, limits.maxBodyBytes());
                }
                Charset charset = type.charset();
                return extractor.extract(html, charset == null ? null : charset.name(), info);
            } catch (IOException e) {
                throw translate(e, deadline);
            }
        }
    }

    private Response execute(Request request, long deadline) throws IOException {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) {
            throw new InterruptedIOException("timeout");
        }
        Call call = client.newCall(request);
        // El plazo de la llamada cubre conexión, cabeceras y lectura del cuerpo.
        call.timeout().timeout(remaining, TimeUnit.NANOSECONDS);
        return call.execute();
    }

    private URI nextHop(Response response) {
        String location = response.header("Location");
        HttpUrl target = location == null ? null : response.request().url().resolve(location);
        if (target == null) {
            throw new FetchFailedException(AnalysisErrorCode.INVALID_REDIRECT, "Redirección sin destino válido");
        }
        try {
            // Cada salto repite la validación completa: esquema, puerto, IPs literales y nombres locales.
            return policy.parse(target.toString()).uri();
        } catch (UrlRejectedException e) {
            throw new FetchFailedException(AnalysisErrorCode.URL_REJECTED, "Redirección a un destino prohibido", e);
        } catch (InvalidUrlException e) {
            throw new FetchFailedException(AnalysisErrorCode.INVALID_REDIRECT, "Redirección a una URL no admitida", e);
        }
    }

    private FetchFailedException translate(IOException e, long deadline) {
        if (e instanceof ForbiddenAddressException) {
            return new FetchFailedException(AnalysisErrorCode.URL_REJECTED, "El destino resuelve a una red prohibida", e);
        }
        if (e instanceof BodyTooLargeException) {
            return new FetchFailedException(AnalysisErrorCode.BODY_TOO_LARGE, e.getMessage(), e);
        }
        if (e instanceof UnknownHostException) {
            return new FetchFailedException(AnalysisErrorCode.DNS_FAILURE, "No se pudo resolver el dominio", e);
        }
        if (e instanceof InterruptedIOException || System.nanoTime() - deadline >= 0) {
            return new FetchFailedException(AnalysisErrorCode.TIMEOUT, "La web tardó demasiado en responder", e);
        }
        return new FetchFailedException(AnalysisErrorCode.CONNECTION_FAILED, "No se pudo conectar con la web", e);
    }

    // --- robots.txt -------------------------------------------------------------------------

    /**
     * robots.txt descargado, o {@code null} con el estado que lo explica
     * ({@code NOT_FOUND} o {@code UNAVAILABLE}).
     */
    private record RobotsResult(RobotsTxt robots, RobotsTxtStatus missingStatus) {
    }

    private RobotsTxtStatus robotsStatus(URI requested, URI finalUrl, RobotsResult requestedRobots) {
        RobotsResult result = sameOrigin(requested, finalUrl) ? requestedRobots : fetchRobots(finalUrl);
        if (result.robots() == null) {
            return result.missingStatus();
        }
        return result.robots().isAllowed(SEARCH_ENGINE_ROBOTS_AGENT, pathAndQuery(finalUrl))
                ? RobotsTxtStatus.ALLOWED : RobotsTxtStatus.BLOCKED;
    }

    /**
     * Descarga y parsea robots.txt con las mismas protecciones que la página.
     * Nunca hace fallar el análisis: cualquier problema se refleja en el estado.
     */
    private RobotsResult fetchRobots(URI pageUrl) {
        URI current = URI.create(pageUrl.getScheme() + "://" + pageUrl.getRawAuthority() + "/robots.txt");
        long deadline = System.nanoTime() + limits.robotsTimeout().toNanos();
        try {
            for (int redirects = 0; ; redirects++) {
                try (Response response = execute(request(current, "text/plain,*/*;q=0.1"), deadline)) {
                    int status = response.code();
                    if (REDIRECT_CODES.contains(status) && redirects < limits.maxRedirects()) {
                        current = nextHop(response);
                        continue;
                    }
                    if (status >= 200 && status < 300) {
                        byte[] content;
                        try (InputStream in = response.body().byteStream()) {
                            content = BoundedBodyReader.readTruncated(in, RobotsTxt.MAX_PARSED_BYTES);
                        }
                        return new RobotsResult(RobotsTxt.parse(new String(content, StandardCharsets.UTF_8)), null);
                    }
                    // RFC 9309: 4xx equivale a «sin restricciones»; 5xx y el resto, a «no disponible».
                    return new RobotsResult(null, status >= 400 && status < 500
                            ? RobotsTxtStatus.NOT_FOUND : RobotsTxtStatus.UNAVAILABLE);
                }
            }
        } catch (IOException | FetchFailedException e) {
            return new RobotsResult(null, RobotsTxtStatus.UNAVAILABLE);
        }
    }

    private Request request(URI uri, String accept) {
        return new Request.Builder()
                .url(HttpUrl.get(uri.toString()))
                .header("User-Agent", userAgent)
                .header("Accept", accept)
                .header("Accept-Language", "es,en;q=0.5")
                .get()
                .build();
    }

    private static boolean sameOrigin(URI a, URI b) {
        return a.getScheme().equalsIgnoreCase(b.getScheme())
                && a.getRawAuthority().equalsIgnoreCase(b.getRawAuthority());
    }

    private static String pathAndQuery(URI uri) {
        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
    }
}
