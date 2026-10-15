package io.github.raulramalf.browseme.infrastructure.fetch;

import io.github.raulramalf.browseme.domain.url.InvalidUrlException;
import io.github.raulramalf.browseme.domain.url.UrlPolicy;
import io.github.raulramalf.browseme.domain.url.UrlRejectedException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Descarga segura de los recursos que pide el navegador del análisis visual
 * (hojas de estilo, imágenes, fuentes, scripts). Mismas garantías anti-SSRF que
 * {@link SafeHttpPageFetcher}: cada URL pasa por {@link UrlPolicy}, el DNS se
 * valida una sola vez, la IP real de la conexión se comprueba y no hay proxy,
 * cookies ni caché. Las redirecciones no se siguen aquí: se devuelven al
 * navegador, que pide el destino y vuelve a pasar por este filtro.
 */
public class SafeResourceFetcher {

    /** Cabeceras de respuesta que se entregan al navegador; el resto se descarta (cookies incluidas). */
    private static final Set<String> FORWARDED_HEADERS = Set.of("content-type", "location", "cache-control",
            "access-control-allow-origin", "timing-allow-origin", "content-language", "last-modified", "etag");

    private final UrlPolicy policy;
    private final OkHttpClient client;
    private final String userAgent;
    private final Duration callTimeout;

    public SafeResourceFetcher(UrlPolicy policy, FetchLimits limits, String userAgent) {
        this(policy, limits, userAgent, HostResolver.system(), SocketFactory.getDefault());
    }

    /** Con costuras para tests: resolución DNS y sockets sustituibles. */
    public SafeResourceFetcher(UrlPolicy policy, FetchLimits limits, String userAgent, HostResolver resolver,
                               SocketFactory socketFactory) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.userAgent = Objects.requireNonNull(userAgent, "userAgent");
        this.callTimeout = limits.readTimeout();
        this.client = new OkHttpClient.Builder()
                .dns(new ValidatingDns(resolver, policy))
                .socketFactory(socketFactory)
                .proxy(Proxy.NO_PROXY)
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(true)
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

    /** Respuesta lista para entregar al navegador. */
    public record Resource(int status, Map<String, String> headers, byte[] body) {
    }

    /** El destino no está permitido (esquema, puerto, red privada…). */
    public static final class BlockedResourceException extends Exception {
        BlockedResourceException(String message) {
            super(message);
        }
    }

    /**
     * @param accept   cabecera Accept que envió el navegador (o {@code null})
     * @param maxBytes tope del cuerpo descomprimido
     */
    public Resource fetch(String url, String accept, int maxBytes) throws BlockedResourceException, IOException {
        String validated;
        try {
            validated = policy.parse(url).uri().toString();
        } catch (UrlRejectedException | InvalidUrlException e) {
            throw new BlockedResourceException("Destino no permitido");
        }
        Request request = new Request.Builder()
                .url(HttpUrl.get(validated))
                .header("User-Agent", userAgent)
                .header("Accept", accept == null || accept.isBlank() ? "*/*" : accept)
                .header("Accept-Language", "es,en;q=0.5")
                .header("Connection", "close")
                .get()
                .build();
        Call call = client.newCall(request);
        call.timeout().timeout(callTimeout.toMillis(), TimeUnit.MILLISECONDS);
        try (Response response = call.execute()) {
            Map<String, String> headers = new LinkedHashMap<>();
            response.headers().names().forEach(name -> {
                String lower = name.toLowerCase(Locale.ROOT);
                if (FORWARDED_HEADERS.contains(lower)) {
                    headers.put(lower, response.header(name));
                }
            });
            byte[] body;
            try (InputStream in = response.body().byteStream()) {
                body = BoundedBodyReader.readAtMost(in, maxBytes);
            }
            return new Resource(response.code(), headers, body);
        } catch (ForbiddenAddressException e) {
            throw new BlockedResourceException("El destino resuelve a una red prohibida");
        }
    }
}
