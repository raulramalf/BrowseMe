package io.github.raulramalf.browseme.infrastructure.render;

import io.github.raulramalf.browseme.infrastructure.fetch.SafeResourceFetcher;
import io.github.raulramalf.browseme.infrastructure.fetch.SafeResourceFetcher.BlockedResourceException;
import io.github.raulramalf.browseme.infrastructure.fetch.SafeResourceFetcher.Resource;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Atiende cada petición que hace la página dibujada ({@code Fetch.requestPaused}):
 * la descarga con {@link SafeResourceFetcher} y se la entrega al navegador, o la
 * rechaza. El navegador no tiene otra forma de salir a la red.
 */
final class RequestBroker implements AutoCloseable {

    /** Tipos de recurso necesarios para dibujar la página; el resto se rechaza. */
    static final Set<String> ALLOWED_TYPES = Set.of("Document", "Stylesheet", "Image", "Font", "Script", "XHR",
            "Fetch", "Manifest");

    private final SafeResourceFetcher fetcher;
    private final int maxRequests;
    private final int maxResourceBytes;
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicLong bytesLeft;
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicLong lastActivity = new AtomicLong(System.nanoTime());
    /** Las dos pasadas (móvil y escritorio) piden casi lo mismo: no se descarga dos veces. */
    private final Map<String, Resource> cache = new ConcurrentHashMap<>();
    private final ExecutorService workers = Executors.newFixedThreadPool(6, runnable -> {
        Thread thread = new Thread(runnable, "browser-requests");
        thread.setDaemon(true);
        return thread;
    });

    RequestBroker(SafeResourceFetcher fetcher, int maxRequests, int maxTotalBytes, int maxResourceBytes) {
        this.fetcher = fetcher;
        this.maxRequests = maxRequests;
        this.maxResourceBytes = maxResourceBytes;
        this.bytesLeft = new AtomicLong(maxTotalBytes);
    }

    void attach(CdpConnection cdp, String session) {
        cdp.on("Fetch.requestPaused", session, (params, s) -> {
            inFlight.incrementAndGet();
            lastActivity.set(System.nanoTime());
            workers.submit(() -> {
                try {
                    handle(cdp, session, params);
                } finally {
                    inFlight.decrementAndGet();
                    lastActivity.set(System.nanoTime());
                }
            });
        });
    }

    private void handle(CdpConnection cdp, String session, JsonNode params) {
        String requestId = params.path("requestId").asString();
        JsonNode request = params.path("request");
        String url = request.path("url").asString("");
        String method = request.path("method").asString("");
        String type = params.path("resourceType").asString("");

        if (!"GET".equals(method) || !ALLOWED_TYPES.contains(type) || !(url.startsWith("http://")
                || url.startsWith("https://"))) {
            fail(cdp, session, requestId, "BlockedByClient");
            return;
        }
        Resource resource = cache.get(url);
        if (resource == null) {
            if (requests.incrementAndGet() > maxRequests) {
                fail(cdp, session, requestId, "BlockedByClient");
                return;
            }
            long budget = Math.min(maxResourceBytes, bytesLeft.get());
            if (budget <= 0) {
                fail(cdp, session, requestId, "BlockedByClient");
                return;
            }
            try {
                resource = fetcher.fetch(url, request.path("headers").path("Accept").asString(null), (int) budget);
            } catch (BlockedResourceException e) {
                fail(cdp, session, requestId, "BlockedByClient");
                return;
            } catch (InterruptedIOException e) {
                fail(cdp, session, requestId, "TimedOut");
                return;
            } catch (IOException | RuntimeException e) {
                fail(cdp, session, requestId, "Failed");
                return;
            }
            bytesLeft.addAndGet(-resource.body().length);
            if (resource.status() == 200) {
                cache.put(url, resource);
            }
        }
        fulfill(cdp, session, requestId, resource);
    }

    private static void fulfill(CdpConnection cdp, String session, String requestId, Resource resource) {
        ObjectNode params = cdp.params();
        params.put("requestId", requestId);
        // Códigos fuera de rango o informativos no se pueden entregar tal cual.
        int status = resource.status() >= 200 && resource.status() <= 599 ? resource.status() : 502;
        params.put("responseCode", status);
        ArrayNode headers = params.putArray("responseHeaders");
        resource.headers().forEach((name, value) -> headers.addObject().put("name", name).put("value", value));
        params.put("body", Base64.getEncoder().encodeToString(resource.body()));
        cdp.send("Fetch.fulfillRequest", params, session);
    }

    private static void fail(CdpConnection cdp, String session, String requestId, String reason) {
        ObjectNode params = cdp.params();
        params.put("requestId", requestId);
        params.put("errorReason", reason);
        cdp.send("Fetch.failRequest", params, session);
    }

    /** Espera a que no haya peticiones en curso durante {@code quiet}, como mucho {@code max}. */
    void awaitQuiet(Duration quiet, Duration max) throws InterruptedException {
        long end = System.nanoTime() + max.toNanos();
        while (System.nanoTime() < end) {
            if (inFlight.get() == 0 && System.nanoTime() - lastActivity.get() >= quiet.toNanos()) {
                return;
            }
            Thread.sleep(50);
        }
    }

    @Override
    public void close() {
        workers.shutdownNow();
    }
}
