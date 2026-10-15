package io.github.raulramalf.browseme.infrastructure.render;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Cliente mínimo del Chrome DevTools Protocol sobre el WebSocket local que abre
 * el navegador (solo 127.0.0.1). Sesiones «planas»: cada mensaje lleva su
 * {@code sessionId}. Los eventos se entregan en el hilo del WebSocket, así que
 * los manejadores no deben bloquear.
 */
final class CdpConnection implements AutoCloseable {

    /** Error devuelto por el navegador o fallo de la conexión. */
    static final class CdpException extends Exception {
        CdpException(String message) {
            super(message);
        }

        CdpException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final JsonMapper json = JsonMapper.builder().build();
    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Object sendLock = new Object();
    private final WebSocket socket;

    private record Listener(String method, String sessionId, BiConsumer<JsonNode, String> handler) {
    }

    private CdpConnection(URI endpoint, Duration timeout) throws CdpException {
        HttpClient client = HttpClient.newBuilder()
                .proxy(HttpClient.Builder.NO_PROXY)
                .connectTimeout(timeout)
                .build();
        try {
            this.socket = client.newWebSocketBuilder()
                    .connectTimeout(timeout)
                    .buildAsync(endpoint, new Receiver())
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CdpException("Interrumpido al conectar con el navegador", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new CdpException("No se pudo conectar con el navegador", e);
        }
    }

    static CdpConnection connect(URI endpoint, Duration timeout) throws CdpException {
        if (!"127.0.0.1".equals(endpoint.getHost()) && !"localhost".equals(endpoint.getHost())) {
            throw new CdpException("El navegador debe escuchar solo en local");
        }
        return new CdpConnection(endpoint, timeout);
    }

    ObjectNode params() {
        return json.createObjectNode();
    }

    /** Suscribe un manejador a un evento; {@code sessionId} null = cualquier sesión. */
    void on(String method, String sessionId, BiConsumer<JsonNode, String> handler) {
        listeners.add(new Listener(method, sessionId, handler));
    }

    /** Envía una orden y espera su resultado. */
    JsonNode call(String method, ObjectNode params, String sessionId, Duration timeout) throws CdpException {
        try {
            return send(method, params, sessionId).get(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CdpException("Interrumpido esperando a " + method, e);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof CdpException cdp ? cdp : new CdpException(method + " falló", e.getCause());
        } catch (TimeoutException e) {
            throw new CdpException(method + " no respondió a tiempo", e);
        }
    }

    /** Envía una orden sin esperar (para responder a eventos desde otros hilos). */
    CompletableFuture<JsonNode> send(String method, ObjectNode params, String sessionId) {
        long id = ids.incrementAndGet();
        ObjectNode message = json.createObjectNode();
        message.put("id", id);
        message.put("method", method);
        message.set("params", params == null ? json.createObjectNode() : params);
        if (sessionId != null) {
            message.put("sessionId", sessionId);
        }
        CompletableFuture<JsonNode> result = new CompletableFuture<>();
        pending.put(id, result);
        String text = json.writeValueAsString(message);
        synchronized (sendLock) {
            try {
                // WebSocket no admite envíos solapados: se espera a que termine el anterior.
                socket.sendText(text, true).get(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(id, e);
            } catch (ExecutionException | TimeoutException e) {
                fail(id, e);
            }
        }
        return result;
    }

    private void fail(long id, Exception cause) {
        CompletableFuture<JsonNode> future = pending.remove(id);
        if (future != null) {
            future.completeExceptionally(new CdpException("No se pudo enviar la orden al navegador", cause));
        }
    }

    private void dispatch(String text) {
        JsonNode message;
        try {
            message = json.readTree(text);
        } catch (RuntimeException e) {
            return;
        }
        JsonNode id = message.get("id");
        if (id != null && id.canConvertToLong()) {
            CompletableFuture<JsonNode> future = pending.remove(id.asLong());
            if (future == null) {
                return;
            }
            JsonNode error = message.get("error");
            if (error != null) {
                future.completeExceptionally(new CdpException(error.path("message").asString("error del navegador")));
            } else {
                future.complete(message.path("result"));
            }
            return;
        }
        String method = message.path("method").asString("");
        String session = message.has("sessionId") ? message.get("sessionId").asString() : null;
        JsonNode params = message.path("params");
        for (Listener listener : listeners) {
            if (listener.method().equals(method)
                    && (listener.sessionId() == null || listener.sessionId().equals(session))) {
                try {
                    listener.handler().accept(params, session);
                } catch (RuntimeException e) {
                    // Un manejador defectuoso no debe tumbar la conexión.
                }
            }
        }
    }

    @Override
    public void close() {
        pending.values().forEach(f -> f.completeExceptionally(new CdpException("Conexión cerrada")));
        pending.clear();
        try {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "").get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            socket.abort();
        }
    }

    private final class Receiver implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String text = buffer.toString();
                buffer.setLength(0);
                dispatch(text);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            pending.values().forEach(f -> f.completeExceptionally(new CdpException("Conexión perdida", error)));
            pending.clear();
        }
    }
}
