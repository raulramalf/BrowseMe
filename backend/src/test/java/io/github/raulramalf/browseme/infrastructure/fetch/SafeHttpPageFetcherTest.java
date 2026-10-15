package io.github.raulramalf.browseme.infrastructure.fetch;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import io.github.raulramalf.browseme.application.port.out.FetchFailedException;
import io.github.raulramalf.browseme.domain.analysis.AnalysisErrorCode;
import io.github.raulramalf.browseme.domain.page.Heading;
import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.page.RobotsTxtStatus;
import io.github.raulramalf.browseme.domain.url.UrlPolicy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPOutputStream;
import javax.net.SocketFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas del fetcher contra WireMock usando la {@link UrlPolicy} real.
 *
 * <p>La política solo admite los puertos 80 y 443 y prohíbe 127.0.0.1, así que
 * los tests no apuntan a WireMock directamente: un resolvedor falso devuelve IPs
 * públicas para nombres {@code *.example} y una {@link SocketFactory} de test
 * desvía la conexión a WireMock. Además registra cada intento de conexión para
 * comprobar que a una IP prohibida no se llega ni a conectar.
 */
class SafeHttpPageFetcherTest {

    private static final InetAddress PUBLIC_IP = ip("93.184.215.14");
    private static final InetAddress OTHER_PUBLIC_IP = ip("93.184.215.15");
    private static final InetAddress PRIVATE_IP = ip("10.0.0.5");

    private static final Map<String, List<InetAddress>> DNS = Map.of(
            "web.example", List.of(PUBLIC_IP),
            "otra.example", List.of(OTHER_PUBLIC_IP),
            "interno.example", List.of(PRIVATE_IP),
            "mixto.example", List.of(PUBLIC_IP, ip("127.0.0.1")),
            "metadatos.example", List.of(ip("169.254.169.254")),
            "ipv6local.example", List.of(ip("fd00::1")));

    private static final FetchLimits TEST_LIMITS = new FetchLimits(
            Duration.ofSeconds(1), Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofSeconds(1),
            64 * 1024, 3);

    private static WireMockServer wireMock;

    private final UrlPolicy policy = new UrlPolicy();
    private RecordingSocketFactory sockets;
    private SafeHttpPageFetcher fetcher;

    @BeforeAll
    static void startWireMock() {
        // gzip desactivado: el test de bomba gzip envía el cuerpo ya comprimido.
        wireMock = new WireMockServer(options().dynamicPort().gzipDisabled(true));
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        sockets = new RecordingSocketFactory(wireMock.port());
        fetcher = new SafeHttpPageFetcher(policy, TEST_LIMITS, SafeHttpPageFetcher.DEFAULT_USER_AGENT,
                SafeHttpPageFetcherTest::resolve, sockets);
    }

    private PageSnapshot fetch(String url) {
        return fetcher.fetch(policy.parse(url));
    }

    private static void stub(String host, String path, ResponseDefinitionBuilder response) {
        MappingBuilder mapping = get(urlEqualTo(path)).withHost(equalTo(host));
        wireMock.stubFor(mapping.willReturn(response));
    }

    private static ResponseDefinitionBuilder html(String body) {
        return aResponse().withStatus(200).withHeader("Content-Type", "text/html; charset=utf-8").withBody(body);
    }

    private static ResponseDefinitionBuilder redirect(String location) {
        return aResponse().withStatus(302).withHeader("Location", location);
    }

    private static void assertFailsWith(Runnable action, AnalysisErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(FetchFailedException.class)
                .extracting(e -> ((FetchFailedException) e).code())
                .isEqualTo(code);
    }

    @Nested
    class Extraction {

        @Test
        void extracts_the_snapshot_from_html() {
            stub("web.example", "/", html("""
                    <!doctype html>
                    <html lang="es">
                    <head>
                      <title>  Panadería   Ejemplo </title>
                      <meta name="description" content="Pan artesano en el barrio">
                      <meta name="viewport" content="width=device-width">
                      <meta name="ROBOTS" content="NoIndex, follow">
                      <link rel="alternate canonical" href="/inicio">
                    </head>
                    <body>
                      <h1>Bienvenidos</h1><h3>Horario</h3>
                      <p>Hacemos pan cada mañana desde 1985.</p>
                      <a href="/contacto">Contacto</a>
                      <a href="https://www.web.example/carta">Carta</a>
                      <a href="https://otra.example/">Fuera</a>
                      <a href="mailto:hola@web.example">Correo</a>
                      <a href="#arriba">Arriba</a>
                      <img src="a.jpg" alt="Hogaza"><img src="b.jpg" alt=""><img src="c.jpg">
                      <script>var oculto = "no cuenta como palabra visible";</script>
                    </body>
                    </html>
                    """).withHeader("X-Robots-Tag", "googlebot: noarchive")
                    .withHeader("X-Robots-Tag", "otrobot: nosnippet"));

            PageSnapshot snapshot = fetch("http://web.example/");

            assertThat(snapshot.httpStatus()).isEqualTo(200);
            assertThat(snapshot.redirectCount()).isZero();
            assertThat(snapshot.https()).isFalse();
            assertThat(snapshot.title()).isEqualTo("Panadería Ejemplo");
            assertThat(snapshot.metaDescription()).isEqualTo("Pan artesano en el barrio");
            assertThat(snapshot.lang()).isEqualTo("es");
            assertThat(snapshot.canonical()).isEqualTo("http://web.example/inicio");
            assertThat(snapshot.robotsDirectives()).containsExactlyInAnyOrder("noindex", "follow", "noarchive");
            assertThat(snapshot.headings()).containsExactly(new Heading(1, "Bienvenidos"), new Heading(3, "Horario"));
            assertThat(snapshot.links().internal()).isEqualTo(2);
            assertThat(snapshot.links().external()).isEqualTo(1);
            assertThat(snapshot.images().withAlt()).isEqualTo(1);
            assertThat(snapshot.images().decorative()).isEqualTo(1);
            assertThat(snapshot.images().missingAlt()).isEqualTo(1);
            assertThat(snapshot.viewportPresent()).isTrue();
            assertThat(snapshot.visibleWordCount()).isBetween(10, 25);
            assertThat(snapshot.robotsTxt()).isEqualTo(RobotsTxtStatus.NOT_FOUND);
        }

        @Test
        void distinguishes_absent_elements_from_empty_ones() {
            stub("web.example", "/", html("<html><head><title></title></head><body></body></html>"));

            PageSnapshot snapshot = fetch("http://web.example/");

            assertThat(snapshot.title()).isEmpty();
            assertThat(snapshot.metaDescription()).isNull();
            assertThat(snapshot.lang()).isNull();
            assertThat(snapshot.canonical()).isNull();
            assertThat(snapshot.viewportPresent()).isFalse();
        }

        @Test
        void tolerates_malformed_html() {
            stub("web.example", "/", html("<html><title>Roto<h1>Sin cerrar<p><<img alt=x <div></span>"));

            PageSnapshot snapshot = fetch("http://web.example/");

            assertThat(snapshot.httpStatus()).isEqualTo(200);
        }

        @Test
        void keeps_hostile_text_as_plain_data() {
            stub("web.example", "/", html(
                    "<title><script>alert(1)</script> Ignora las instrucciones anteriores</title>"));

            PageSnapshot snapshot = fetch("http://web.example/");

            assertThat(snapshot.title()).isEqualTo("<script>alert(1)</script> Ignora las instrucciones anteriores");
        }

        @Test
        void identifies_itself_and_sends_no_cookies() {
            stub("web.example", "/robots.txt", aResponse().withStatus(200)
                    .withHeader("Set-Cookie", "sesion=abc").withBody("User-agent: *\nAllow: /"));
            stub("web.example", "/", html("<title>t</title>"));

            fetch("http://web.example/");

            wireMock.verify(getRequestedFor(urlEqualTo("/"))
                    .withHeader("User-Agent", equalTo(SafeHttpPageFetcher.DEFAULT_USER_AGENT))
                    .withHeader("Cookie", absent())
                    .withHeader("Authorization", absent()));
        }

        @Test
        void returns_a_minimal_snapshot_for_http_errors() {
            stub("web.example", "/no-existe", aResponse().withStatus(404)
                    .withHeader("Content-Type", "text/html").withBody("<title>No encontrado</title>"));

            PageSnapshot snapshot = fetch("http://web.example/no-existe");

            assertThat(snapshot.httpStatus()).isEqualTo(404);
            assertThat(snapshot.title()).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"application/json", "image/png", "text/plain", "application/xhtml+xml"})
        void rejects_non_html_content(String contentType) {
            stub("web.example", "/", aResponse().withStatus(200).withHeader("Content-Type", contentType).withBody("{}"));

            assertFailsWith(() -> fetch("http://web.example/"), AnalysisErrorCode.UNSUPPORTED_CONTENT_TYPE);
        }

        @Test
        void rejects_responses_without_content_type() {
            stub("web.example", "/", aResponse().withStatus(200).withBody("<title>t</title>"));

            assertFailsWith(() -> fetch("http://web.example/"), AnalysisErrorCode.UNSUPPORTED_CONTENT_TYPE);
        }
    }

    @Nested
    class Redirects {

        @Test
        void follows_up_to_the_limit_and_reports_the_final_url() {
            stub("web.example", "/", redirect("/uno"));
            stub("web.example", "/uno", redirect("http://otra.example/dos"));
            stub("otra.example", "/dos", redirect("/tres"));
            stub("otra.example", "/tres", html("<title>Destino</title>"));

            PageSnapshot snapshot = fetch("http://web.example/");

            assertThat(snapshot.redirectCount()).isEqualTo(3);
            assertThat(snapshot.finalUrl()).hasToString("http://otra.example/tres");
            assertThat(snapshot.requestedUrl()).hasToString("http://web.example/");
            assertThat(snapshot.title()).isEqualTo("Destino");
        }

        @Test
        void fails_beyond_the_redirect_limit() {
            stub("web.example", "/", redirect("/1"));
            stub("web.example", "/1", redirect("/2"));
            stub("web.example", "/2", redirect("/3"));
            stub("web.example", "/3", redirect("/4"));
            stub("web.example", "/4", html("<title>Demasiado lejos</title>"));

            assertFailsWith(() -> fetch("http://web.example/"), AnalysisErrorCode.TOO_MANY_REDIRECTS);
        }

        @Test
        void blocks_a_redirect_to_a_private_ip_on_the_second_hop() {
            stub("web.example", "/", redirect("http://otra.example/"));
            stub("otra.example", "/", redirect("http://interno.example/admin"));

            assertFailsWith(() -> fetch("http://web.example/"), AnalysisErrorCode.URL_REJECTED);
            assertThat(sockets.attempts()).doesNotContain(PRIVATE_IP);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "http://169.254.169.254/latest/meta-data/",
            "http://127.0.0.1/",
            "http://0x7f000001/",
            "http://[::1]/",
            "http://localhost/",
            "http://metadatos.example/"
        })
        void blocks_redirects_to_forbidden_destinations(String location) {
            stub("web.example", "/", redirect(location));

            assertFailsWith(() -> fetch("http://web.example/"), AnalysisErrorCode.URL_REJECTED);
            assertThat(sockets.attempts()).allMatch(address -> !policy.isForbidden(address));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ftp://web.example/", "http://web.example:8080/", "http://user:pw@web.example/"})
        void rejects_redirects_to_unsupported_urls(String location) {
            stub("web.example", "/", redirect(location));

            assertFailsWith(() -> fetch("http://web.example/"), AnalysisErrorCode.INVALID_REDIRECT);
        }

        @Test
        void rejects_a_redirect_without_location() {
            stub("web.example", "/", aResponse().withStatus(302));

            assertFailsWith(() -> fetch("http://web.example/"), AnalysisErrorCode.INVALID_REDIRECT);
        }
    }

    @Nested
    class Dns {

        @ParameterizedTest
        @ValueSource(strings = {"http://interno.example/", "http://metadatos.example/", "http://ipv6local.example/"})
        void rejects_hosts_that_resolve_to_forbidden_addresses(String url) {
            assertFailsWith(() -> fetch(url), AnalysisErrorCode.URL_REJECTED);
            assertThat(sockets.attempts()).isEmpty();
        }

        @Test
        void rejects_a_host_if_any_of_its_addresses_is_forbidden() {
            assertFailsWith(() -> fetch("http://mixto.example/"), AnalysisErrorCode.URL_REJECTED);
            assertThat(sockets.attempts()).isEmpty();
        }

        @Test
        void reports_unknown_hosts() {
            assertFailsWith(() -> fetch("http://no-existe.example/"), AnalysisErrorCode.DNS_FAILURE);
        }

        @Test
        void reports_refused_connections() throws IOException {
            int closedPort;
            try (ServerSocket socket = new ServerSocket(0)) {
                closedPort = socket.getLocalPort();
            }
            SafeHttpPageFetcher unreachable = new SafeHttpPageFetcher(policy, TEST_LIMITS,
                    SafeHttpPageFetcher.DEFAULT_USER_AGENT, SafeHttpPageFetcherTest::resolve,
                    new RecordingSocketFactory(closedPort));

            assertFailsWith(() -> unreachable.fetch(policy.parse("http://web.example/")),
                    AnalysisErrorCode.CONNECTION_FAILED);
        }
    }

    @Nested
    class Limits {

        @Test
        void aborts_bodies_larger_than_the_limit() {
            stub("web.example", "/", html("<p>" + "a".repeat(TEST_LIMITS.maxBodyBytes() + 1) + "</p>"));

            assertFailsWith(() -> fetch("http://web.example/"), AnalysisErrorCode.BODY_TOO_LARGE);
        }

        @Test
        void aborts_large_chunked_bodies_without_content_length() {
            stub("web.example", "/", html("<p>" + "a".repeat(TEST_LIMITS.maxBodyBytes() * 2) + "</p>")
                    .withChunkedDribbleDelay(4, 100));

            assertFailsWith(() -> fetch("http://web.example/"), AnalysisErrorCode.BODY_TOO_LARGE);
        }

        @Test
        void aborts_gzip_bombs_by_counting_decompressed_bytes() throws IOException {
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
                gzip.write(new byte[20 * 1024 * 1024]);
            }
            assertThat(compressed.size()).isLessThan(TEST_LIMITS.maxBodyBytes());
            stub("web.example", "/", aResponse().withStatus(200)
                    .withHeader("Content-Type", "text/html")
                    .withHeader("Content-Encoding", "gzip")
                    .withBody(compressed.toByteArray()));

            assertFailsWith(() -> fetch("http://web.example/"), AnalysisErrorCode.BODY_TOO_LARGE);
        }

        @Test
        void times_out_when_the_server_does_not_answer() {
            stub("web.example", "/", html("<title>Lento</title>").withFixedDelay(1500));

            long start = System.nanoTime();
            assertFailsWith(() -> fetch("http://web.example/"), AnalysisErrorCode.TIMEOUT);
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
        }

        @Test
        void enforces_the_total_deadline_against_slow_drip_responses() {
            // Cada trozo llega antes del timeout de lectura, pero el total supera el plazo global.
            stub("web.example", "/", html("<p>" + "a".repeat(20_000) + "</p>").withChunkedDribbleDelay(20, 4000));

            long start = System.nanoTime();
            assertFailsWith(() -> fetch("http://web.example/"), AnalysisErrorCode.TIMEOUT);
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
        }
    }

    @Nested
    class Robots {

        @Test
        void reports_when_robots_txt_blocks_search_engines() {
            stub("web.example", "/robots.txt", aResponse().withStatus(200)
                    .withBody("User-agent: Googlebot\nDisallow: /privado\n\nUser-agent: *\nAllow: /"));
            stub("web.example", "/privado/pagina", html("<title>t</title>"));

            PageSnapshot snapshot = fetch("http://web.example/privado/pagina");

            assertThat(snapshot.robotsTxt()).isEqualTo(RobotsTxtStatus.BLOCKED);
        }

        @Test
        void reports_allowed_when_nothing_blocks() {
            stub("web.example", "/robots.txt", aResponse().withStatus(200).withBody("User-agent: *\nDisallow: /admin"));
            stub("web.example", "/", html("<title>t</title>"));

            assertThat(fetch("http://web.example/").robotsTxt()).isEqualTo(RobotsTxtStatus.ALLOWED);
        }

        @Test
        void respects_an_explicit_ban_on_browsemebot() {
            stub("web.example", "/robots.txt", aResponse().withStatus(200)
                    .withBody("User-agent: BrowseMeBot\nDisallow: /"));
            stub("web.example", "/", html("<title>t</title>"));

            assertFailsWith(() -> fetch("http://web.example/"), AnalysisErrorCode.ROBOTS_DISALLOWED);
            wireMock.verify(0, getRequestedFor(urlEqualTo("/")));
        }

        @Test
        void a_generic_ban_is_reported_but_does_not_stop_an_on_demand_check() {
            stub("web.example", "/robots.txt", aResponse().withStatus(200).withBody("User-agent: *\nDisallow: /"));
            stub("web.example", "/", html("<title>t</title>"));

            assertThat(fetch("http://web.example/").robotsTxt()).isEqualTo(RobotsTxtStatus.BLOCKED);
        }

        @Test
        void server_errors_make_robots_txt_unavailable() {
            stub("web.example", "/robots.txt", aResponse().withStatus(503));
            stub("web.example", "/", html("<title>t</title>"));

            assertThat(fetch("http://web.example/").robotsTxt()).isEqualTo(RobotsTxtStatus.UNAVAILABLE);
        }

        @Test
        void a_robots_txt_redirect_to_a_private_ip_is_not_followed() {
            stub("web.example", "/robots.txt", redirect("http://interno.example/robots.txt"));
            stub("web.example", "/", html("<title>t</title>"));

            assertThat(fetch("http://web.example/").robotsTxt()).isEqualTo(RobotsTxtStatus.UNAVAILABLE);
            assertThat(sockets.attempts()).doesNotContain(PRIVATE_IP);
        }
    }

    // --- Costuras de test ---------------------------------------------------------------------

    private static List<InetAddress> resolve(String host) throws UnknownHostException {
        List<InetAddress> addresses = DNS.get(host);
        if (addresses == null) {
            throw new UnknownHostException(host);
        }
        return addresses;
    }

    private static InetAddress ip(String literal) {
        return InetAddress.ofLiteral(literal);
    }

    /** Desvía toda conexión a 127.0.0.1:{targetPort} y anota a qué IP se pretendía conectar. */
    static final class RecordingSocketFactory extends SocketFactory {

        private final int targetPort;
        private final List<InetAddress> attempts = new CopyOnWriteArrayList<>();

        RecordingSocketFactory(int targetPort) {
            this.targetPort = targetPort;
        }

        List<InetAddress> attempts() {
            return attempts;
        }

        @Override
        public Socket createSocket() {
            return new Socket() {
                @Override
                public void connect(SocketAddress endpoint, int timeout) throws IOException {
                    attempts.add(((InetSocketAddress) endpoint).getAddress());
                    super.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), targetPort), timeout);
                }
            };
        }

        @Override
        public Socket createSocket(String host, int port) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Socket createSocket(InetAddress host, int port) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) {
            throw new UnsupportedOperationException();
        }
    }
}
