package io.github.raulramalf.browseme.infrastructure.render;

import io.github.raulramalf.browseme.application.port.out.PageRenderer;
import io.github.raulramalf.browseme.domain.page.VisualCheck;
import io.github.raulramalf.browseme.domain.page.VisualMetrics;
import io.github.raulramalf.browseme.domain.page.VisualStatus;
import io.github.raulramalf.browseme.infrastructure.fetch.CallToActionVocabulary;
import io.github.raulramalf.browseme.infrastructure.fetch.SafeResourceFetcher;
import io.github.raulramalf.browseme.infrastructure.render.CdpConnection.CdpException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Análisis visual con el Chrome, Edge o Chromium del equipo, controlado por el
 * DevTools Protocol.
 *
 * <p>Aislamiento de red (la regla anti-SSRF no se relaja por usar un navegador):
 * <ul>
 *   <li>El navegador no puede resolver ningún nombre ({@code --host-resolver-rules})
 *       y todo su tráfico va a un proxy inexistente en 127.0.0.1:1, de modo que
 *       no tiene salida propia, ni siquiera hacia IPs literales o WebRTC.</li>
 *   <li>Cada petición de la página se intercepta ({@code Fetch.requestPaused}) y
 *       la descarga {@link SafeResourceFetcher}, con la misma política de URLs,
 *       DNS validado y comprobación de la IP conectada que la página principal.</li>
 *   <li>Solo GET y tipos de recurso necesarios para dibujar; límites de número,
 *       tamaño y tiempo. Perfil temporal que se borra al terminar.</li>
 * </ul>
 * Las métricas se calculan en un mundo aislado de JavaScript que el código de
 * la página no puede alterar, y aun así se validan como datos no confiables.
 */
public class ChromeRenderer implements PageRenderer {

    private static final Logger log = LoggerFactory.getLogger(ChromeRenderer.class);
    private static final Pattern DEVTOOLS_URL = Pattern.compile("DevTools listening on (ws://\\S+)");
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration LOAD_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration QUIET_PERIOD = Duration.ofMillis(600);
    private static final Duration MAX_SETTLE = Duration.ofSeconds(4);
    private static final int SCREENSHOT_QUALITY = 60;
    private static final String MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome Mobile Safari/537.36 BrowseMeBot";
    private static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome Safari/537.36 BrowseMeBot";

    /** Ajustes del renderizador. */
    public record Settings(boolean enabled, String browserPath, Duration timeout, int maxRequests,
                           int maxTotalBytes, int maxResourceBytes) {
    }

    private final Settings settings;
    private final SafeResourceFetcher fetcher;
    private final BrowserLocator locator;
    private final String metricsScript;
    private final JsonMapper json = JsonMapper.builder().build();

    public ChromeRenderer(Settings settings, SafeResourceFetcher fetcher) {
        this(settings, fetcher, new BrowserLocator());
    }

    ChromeRenderer(Settings settings, SafeResourceFetcher fetcher, BrowserLocator locator) {
        this.settings = settings;
        this.fetcher = fetcher;
        this.locator = locator;
        this.metricsScript = loadScript().replace("__CTA_PATTERN__",
                json.writeValueAsString(CallToActionVocabulary.PATTERN));
    }

    @Override
    public VisualCheck render(URI pageUrl) {
        if (!settings.enabled()) {
            return VisualCheck.of(VisualStatus.DISABLED);
        }
        Optional<Path> browser = locator.find(settings.browserPath());
        if (browser.isEmpty()) {
            return VisualCheck.of(VisualStatus.BROWSER_NOT_FOUND);
        }
        long deadline = System.nanoTime() + settings.timeout().toNanos();
        Path profile = null;
        Process process = null;
        try {
            profile = Files.createTempDirectory("browseme-browser-");
            process = launch(browser.get(), profile);
            URI endpoint = awaitDevTools(process, deadline);
            try (CdpConnection cdp = CdpConnection.connect(endpoint, remaining(deadline));
                 RequestBroker broker = new RequestBroker(fetcher, settings.maxRequests(), settings.maxTotalBytes(),
                         settings.maxResourceBytes())) {
                return measure(cdp, broker, pageUrl, deadline);
            }
        } catch (CdpException | IOException | RuntimeException e) {
            // Sin URL ni contenido en el log: solo el tipo de fallo.
            log.info("El análisis visual no se pudo completar: {}", e.getClass().getSimpleName());
            return VisualCheck.of(VisualStatus.FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return VisualCheck.of(VisualStatus.FAILED);
        } finally {
            stop(process);
            delete(profile);
        }
    }

    private VisualCheck measure(CdpConnection cdp, RequestBroker broker, URI pageUrl, long deadline)
            throws CdpException, InterruptedException {
        ObjectNode target = cdp.params();
        target.put("url", "about:blank");
        String targetId = cdp.call("Target.createTarget", target, null, remaining(deadline)).path("targetId").asString();
        ObjectNode attach = cdp.params();
        attach.put("targetId", targetId);
        attach.put("flatten", true);
        String session = cdp.call("Target.attachToTarget", attach, null, remaining(deadline))
                .path("sessionId").asString();

        broker.attach(cdp, session);
        cdp.on("Page.javascriptDialogOpening", session, (params, s) -> {
            ObjectNode dismiss = cdp.params();
            dismiss.put("accept", false);
            CompletableFuture.runAsync(() -> cdp.send("Page.handleJavaScriptDialog", dismiss, session));
        });
        ObjectNode fetchPatterns = cdp.params();
        fetchPatterns.putArray("patterns").addObject().put("urlPattern", "*");
        cdp.call("Fetch.enable", fetchPatterns, session, remaining(deadline));
        cdp.call("Page.enable", cdp.params(), session, remaining(deadline));

        // Móvil: casi todas las métricas y la captura principal.
        emulate(cdp, session, 390, 844, true, deadline);
        load(cdp, broker, session, pageUrl, deadline);
        JsonNode mobile = evaluate(cdp, session, deadline);
        byte[] mobileShot = screenshot(cdp, session, deadline);

        // Escritorio: si la acción principal se ve sin desplazarse, y su captura.
        emulate(cdp, session, 1366, 800, false, deadline);
        load(cdp, broker, session, pageUrl, deadline);
        JsonNode desktop = evaluate(cdp, session, deadline);
        byte[] desktopShot = screenshot(cdp, session, deadline);

        return VisualCheck.done(metrics(mobile, desktop), mobileShot, desktopShot);
    }

    private void emulate(CdpConnection cdp, String session, int width, int height, boolean mobile, long deadline)
            throws CdpException {
        ObjectNode metrics = cdp.params();
        metrics.put("width", width);
        metrics.put("height", height);
        metrics.put("deviceScaleFactor", 1);
        metrics.put("mobile", mobile);
        cdp.call("Emulation.setDeviceMetricsOverride", metrics, session, remaining(deadline));
        ObjectNode touch = cdp.params();
        touch.put("enabled", mobile);
        cdp.call("Emulation.setTouchEmulationEnabled", touch, session, remaining(deadline));
        ObjectNode agent = cdp.params();
        agent.put("userAgent", mobile ? MOBILE_USER_AGENT : DESKTOP_USER_AGENT);
        cdp.call("Emulation.setUserAgentOverride", agent, session, remaining(deadline));
    }

    private void load(CdpConnection cdp, RequestBroker broker, String session, URI url, long deadline)
            throws CdpException, InterruptedException {
        CompletableFuture<Void> loaded = new CompletableFuture<>();
        cdp.on("Page.loadEventFired", session, (params, s) -> loaded.complete(null));
        ObjectNode navigate = cdp.params();
        navigate.put("url", url.toString());
        JsonNode result = cdp.call("Page.navigate", navigate, session, remaining(deadline));
        if (result.hasNonNull("errorText") && !result.path("errorText").asString().isEmpty()) {
            throw new CdpException("La página no se pudo dibujar");
        }
        try {
            loaded.get(Math.min(LOAD_TIMEOUT.toMillis(), remaining(deadline).toMillis()), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // Algunas páginas nunca terminan de cargar (conexiones abiertas): se mide lo que haya.
        }
        broker.awaitQuiet(QUIET_PERIOD, min(MAX_SETTLE, remaining(deadline)));
    }

    private JsonNode evaluate(CdpConnection cdp, String session, long deadline) throws CdpException {
        String frameId = cdp.call("Page.getFrameTree", cdp.params(), session, remaining(deadline))
                .path("frameTree").path("frame").path("id").asString();
        ObjectNode world = cdp.params();
        world.put("frameId", frameId);
        world.put("worldName", "browseme-metrics");
        world.put("grantUniveralAccess", false);
        long context = cdp.call("Page.createIsolatedWorld", world, session, remaining(deadline))
                .path("executionContextId").asLong();
        ObjectNode evaluate = cdp.params();
        evaluate.put("expression", metricsScript);
        evaluate.put("contextId", context);
        evaluate.put("returnByValue", true);
        JsonNode result = cdp.call("Runtime.evaluate", evaluate, session, remaining(deadline));
        if (result.has("exceptionDetails")) {
            throw new CdpException("El script de métricas falló");
        }
        return result.path("result").path("value");
    }

    private byte[] screenshot(CdpConnection cdp, String session, long deadline) {
        try {
            ObjectNode params = cdp.params();
            params.put("format", "jpeg");
            params.put("quality", SCREENSHOT_QUALITY);
            String data = cdp.call("Page.captureScreenshot", params, session, remaining(deadline))
                    .path("data").asString("");
            byte[] image = Base64.getDecoder().decode(data);
            return image.length > VisualCheck.MAX_SCREENSHOT_BYTES ? null : image;
        } catch (CdpException | IllegalArgumentException e) {
            return null;
        }
    }

    /** Convierte y valida el resultado del script: es dato no confiable. */
    VisualMetrics metrics(JsonNode mobile, JsonNode desktop) {
        int checked = bounded(mobile, "textElementsChecked", 100_000);
        int taps = bounded(mobile, "tapTargetsChecked", 100_000);
        return new VisualMetrics(
                Math.max(1, bounded(mobile, "viewportWidth", 10_000)),
                bounded(mobile, "contentWidth", 1_000_000),
                checked,
                Math.min(checked, bounded(mobile, "lowContrastTexts", 100_000)),
                strings(mobile.path("lowContrastSamples")),
                Math.min(checked, bounded(mobile, "smallTexts", 100_000)),
                taps,
                Math.min(taps, bounded(mobile, "smallTapTargets", 100_000)),
                strings(mobile.path("fontFamilies")),
                mobile.path("callToActionAboveFold").asBoolean(false),
                desktop.path("callToActionAboveFold").asBoolean(false),
                bounded(mobile, "pageHeightScreens", 10_000));
    }

    private static int bounded(JsonNode node, String field, int max) {
        JsonNode value = node.path(field);
        if (!value.isNumber()) {
            return 0;
        }
        double number = value.asDouble();
        if (Double.isNaN(number) || number < 0) {
            return 0;
        }
        return (int) Math.min(max, Math.round(number));
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (array.isArray()) {
            for (JsonNode item : array) {
                if (item.isString() && values.size() < 10) {
                    values.add(item.asString());
                }
            }
        }
        return values;
    }

    // --- proceso del navegador ----------------------------------------------------------------

    private Process launch(Path browser, Path profile) throws IOException {
        List<String> command = new ArrayList<>(List.of(
                browser.toString(),
                "--headless=new",
                "--remote-debugging-port=0",
                "--user-data-dir=" + profile,
                // Sin salida de red propia: ningún nombre resuelve y todo va a un proxy inexistente.
                "--host-resolver-rules=MAP * ~NOTFOUND",
                "--proxy-server=http://127.0.0.1:1",
                "--proxy-bypass-list=<-loopback>",
                "--force-webrtc-ip-handling-policy=disable_non_proxied_udp",
                "--no-first-run",
                "--no-default-browser-check",
                "--disable-extensions",
                "--disable-component-update",
                "--disable-background-networking",
                "--disable-sync",
                "--disable-default-apps",
                "--disable-features=Translate,OptimizationHints,MediaRouter,DialMediaRouteProvider",
                "--no-pings",
                "--mute-audio",
                "--hide-scrollbars",
                "--disable-gpu",
                "--password-store=basic",
                "--use-mock-keychain"));
        if (runningAsRootOnLinux() || "true".equals(System.getenv("BROWSEME_NO_SANDBOX"))) {
            // Chrome se niega a arrancar como root con sandbox (contenedores) y los runners de CI
            // restringen los espacios de nombres que usa. En un escritorio normal no se desactiva:
            // BROWSEME_NO_SANDBOX solo la fija el CI para sus pruebas con páginas propias.
            command.add("--no-sandbox");
        }
        command.add("about:blank");
        return new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
    }

    private URI awaitDevTools(Process process, long deadline) throws IOException, CdpException {
        CompletableFuture<URI> endpoint = new CompletableFuture<>();
        InputStream stderr = process.getErrorStream();
        Thread reader = new Thread(() -> {
            try (BufferedReader lines = new BufferedReader(new InputStreamReader(stderr, StandardCharsets.UTF_8))) {
                String line;
                while ((line = lines.readLine()) != null) {
                    Matcher matcher = DEVTOOLS_URL.matcher(line);
                    if (!endpoint.isDone() && matcher.find()) {
                        endpoint.complete(URI.create(matcher.group(1)));
                    }
                    // Se sigue leyendo para que el navegador no se bloquee al llenar la tubería.
                }
            } catch (IOException | IllegalArgumentException e) {
                endpoint.completeExceptionally(e);
            }
            endpoint.completeExceptionally(new IOException("El navegador terminó sin abrir DevTools"));
        }, "browser-stderr");
        reader.setDaemon(true);
        reader.start();
        try {
            return endpoint.get(min(STARTUP_TIMEOUT, remaining(deadline)).toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new CdpException("El navegador no arrancó a tiempo", e);
        }
    }

    private static void stop(Process process) {
        if (process == null) {
            return;
        }
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        try {
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void delete(Path directory) {
        if (directory == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    // El sistema lo limpiará; no es motivo para fallar el análisis.
                }
            });
        } catch (IOException | UncheckedIOException e) {
            // Ídem.
        }
    }

    private static boolean runningAsRootOnLinux() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux")
                && "root".equals(System.getProperty("user.name"));
    }

    private static Duration remaining(long deadline) {
        long nanos = deadline - System.nanoTime();
        if (nanos <= 0) {
            throw new IllegalStateException("Se agotó el tiempo del análisis visual");
        }
        return Duration.ofNanos(nanos);
    }

    private static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private static String loadScript() {
        try (InputStream in = ChromeRenderer.class.getResourceAsStream("/render/metrics.js")) {
            if (in == null) {
                throw new IllegalStateException("Falta render/metrics.js");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
