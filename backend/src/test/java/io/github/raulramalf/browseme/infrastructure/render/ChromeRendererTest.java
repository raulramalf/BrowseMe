package io.github.raulramalf.browseme.infrastructure.render;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notContaining;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.raulramalf.browseme.domain.page.VisualCheck;
import io.github.raulramalf.browseme.domain.page.VisualMetrics;
import io.github.raulramalf.browseme.domain.page.VisualStatus;
import io.github.raulramalf.browseme.domain.url.UrlPolicy;
import io.github.raulramalf.browseme.infrastructure.fetch.FetchLimits;
import io.github.raulramalf.browseme.infrastructure.fetch.SafeResourceFetcher;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.SocketFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Análisis visual con un navegador real. Se omite si no hay Chrome, Edge ni
 * Chromium (o {@code BROWSEME_TEST_BROWSER} con su ruta). En CI hay Chrome.
 *
 * <p>Como en las pruebas del fetcher, un DNS falso da IPs públicas a nombres
 * {@code *.example} y los sockets se desvían a WireMock. Así la política real
 * de URLs se aplica sin excepciones para los tests.
 */
class ChromeRendererTest {

    private static final InetAddress PUBLIC_IP = InetAddress.ofLiteral("93.184.215.14");
    private static final InetAddress PRIVATE_IP = InetAddress.ofLiteral("10.0.0.5");
    private static final String PAGE = """
            <!doctype html><html lang="es"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <link rel="stylesheet" href="/estilo.css">
            <style>body{margin:0;font-family:Arial} .claro{color:#bbb;background:#fff;font-size:16px}
            .mini{font-size:10px} .ancho{width:800px;height:20px;background:#eee}
            .boton{display:inline-block;width:16px;height:16px} .lejos{margin-top:3000px;display:block}</style>
            </head><body>
            <h1>Panadería</h1>
            <p class="claro">Texto gris uno</p><p class="claro">Texto gris dos</p>
            <p class="claro">Texto gris tres</p><p class="claro">Texto gris cuatro</p>
            <p class="mini">a</p><p class="mini">b</p><p class="mini">c</p>
            <div class="ancho"></div>
            <a class="boton" href="/1">1</a><a class="boton" href="/2">2</a><a class="boton" href="/3">3</a>
            <img src="http://interno.example/privada.png" alt="">
            <img src="http://127.0.0.1:%d/directa.png" alt="">
            <script>fetch('http://10.0.0.1/metadatos').catch(() => {});
              navigator.sendBeacon && navigator.sendBeacon('/beacon', 'x');</script>
            <a class="lejos" href="/reserva">Reserva tu mesa</a>
            </body></html>
            """;

    private static WireMockServer wireMock;
    private static Optional<Path> browser;
    private final List<String> resolved = new CopyOnWriteArrayList<>();
    private ChromeRenderer renderer;

    @BeforeAll
    static void start() {
        String configured = System.getenv("BROWSEME_TEST_BROWSER");
        browser = new BrowserLocator().find(configured);
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stop() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        assumeTrue(browser.isPresent(), "No hay navegador basado en Chromium en esta máquina");
        wireMock.resetAll();
        wireMock.stubFor(get("/").willReturn(aResponse().withHeader("Content-Type", "text/html; charset=utf-8")
                .withHeader("Set-Cookie", "rastreo=1").withBody(PAGE.formatted(wireMock.port()))));
        wireMock.stubFor(get("/estilo.css").willReturn(aResponse().withHeader("Content-Type", "text/css")
                .withBody("h1{font-family:Georgia}")));
        SafeResourceFetcher fetcher = new SafeResourceFetcher(new UrlPolicy(),
                new FetchLimits(Duration.ofSeconds(2), Duration.ofSeconds(5), Duration.ofSeconds(10),
                        Duration.ofSeconds(2), 1024 * 1024, 3),
                "BrowseMeBot/test", this::resolve, new ToWireMock(wireMock.port()));
        renderer = new ChromeRenderer(new ChromeRenderer.Settings(true, browser.get().toString(),
                Duration.ofSeconds(60), 50, 10 * 1024 * 1024, 2 * 1024 * 1024), fetcher);
    }

    private List<InetAddress> resolve(String host) throws UnknownHostException {
        resolved.add(host);
        return switch (host) {
            case "tienda.example" -> List.of(PUBLIC_IP);
            case "interno.example" -> List.of(PRIVATE_IP);
            default -> throw new UnknownHostException(host);
        };
    }

    @Test
    void measures_the_drawn_page_in_mobile_and_desktop() {
        VisualCheck check = renderer.render(URI.create("http://tienda.example/"));

        assertThat(check.status()).isEqualTo(VisualStatus.DONE);
        VisualMetrics m = check.metrics();
        assertThat(m.viewportWidth()).isEqualTo(390);
        assertThat(m.horizontalOverflow()).isGreaterThan(300);
        assertThat(m.lowContrastTexts()).isGreaterThanOrEqualTo(4);
        assertThat(m.lowContrastSamples()).contains("Texto gris uno");
        assertThat(m.smallTexts()).isGreaterThanOrEqualTo(3);
        assertThat(m.smallTapTargets()).isGreaterThanOrEqualTo(3);
        assertThat(m.fontFamilies()).contains("Georgia", "Arial");
        assertThat(m.callToActionAboveFoldMobile()).isFalse();
        assertThat(check.mobileScreenshot()).isNotNull();
        assertThat(check.desktopScreenshot()).isNotNull();
        // JPEG: empieza por FF D8.
        assertThat(check.mobileScreenshot()[0]).isEqualTo((byte) 0xFF);
    }

    @Test
    void the_browser_never_reaches_the_network_on_its_own() {
        renderer.render(URI.create("http://tienda.example/"));

        // Todo lo que llegó al servidor lo pidió el cliente seguro, nunca el navegador directamente.
        wireMock.verify(0, anyRequestedFor(anyUrl()).withHeader("User-Agent", notContaining("BrowseMeBot/test")));
        // Ni la IP literal (127.0.0.1) ni el host privado ni el POST/beacon llegaron a ningún sitio.
        wireMock.verify(0, getRequestedFor(urlEqualTo("/directa.png")));
        wireMock.verify(0, getRequestedFor(urlEqualTo("/privada.png")));
        wireMock.verify(0, anyRequestedFor(urlEqualTo("/beacon")));
        wireMock.verify(0, anyRequestedFor(urlEqualTo("/metadatos")));
        assertThat(resolved).doesNotContain("127.0.0.1");
        // La hoja de estilos sí se sirvió y se pidió una sola vez para las dos pasadas.
        wireMock.verify(1, getRequestedFor(urlEqualTo("/estilo.css")));
    }

    @Test
    void reports_a_missing_browser_or_a_disabled_check() {
        SafeResourceFetcher fetcher = new SafeResourceFetcher(new UrlPolicy(),
                new FetchLimits(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(2),
                        Duration.ofSeconds(1), 1024, 1), "BrowseMeBot/test");
        assertThat(new ChromeRenderer(new ChromeRenderer.Settings(true, "/no/existe/chrome", Duration.ofSeconds(5),
                10, 1024, 1024), fetcher).render(URI.create("https://example.com/")).status())
                .isEqualTo(VisualStatus.BROWSER_NOT_FOUND);
        assertThat(new ChromeRenderer(new ChromeRenderer.Settings(false, "", Duration.ofSeconds(5),
                10, 1024, 1024), fetcher).render(URI.create("https://example.com/")).status())
                .isEqualTo(VisualStatus.DISABLED);
    }

    @Test
    void leaves_no_temporary_profile_behind() throws IOException {
        Path tmp = Path.of(System.getProperty("java.io.tmpdir"));
        long before = countProfiles(tmp);
        renderer.render(URI.create("http://tienda.example/"));
        assertThat(countProfiles(tmp)).isEqualTo(before);
    }

    private static long countProfiles(Path tmp) throws IOException {
        try (var files = Files.list(tmp)) {
            return files.filter(p -> p.getFileName().toString().startsWith("browseme-browser-")).count();
        }
    }

    /** Desvía toda conexión a WireMock en 127.0.0.1. */
    private static final class ToWireMock extends SocketFactory {
        private final int port;

        ToWireMock(int port) {
            this.port = port;
        }

        @Override
        public Socket createSocket() {
            return new Socket() {
                @Override
                public void connect(SocketAddress endpoint, int timeout) throws IOException {
                    super.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), timeout);
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
