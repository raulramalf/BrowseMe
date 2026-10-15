package io.github.raulramalf.browseme.infrastructure.desktop;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.function.IntConsumer;

/**
 * App de escritorio: si el puerto está ocupado (casi siempre por otra copia de
 * BrowseMe que sigue abierta), usa el siguiente libre en lugar de fallar o, peor,
 * dejar al usuario mirando la copia antigua sin saberlo.
 */
public final class PortSelector {

    public static final int DEFAULT_PORT = 8765;
    static final int ATTEMPTS = 10;

    private PortSelector() {
    }

    /** @return el primer puerto libre entre {@code preferred} y {@code preferred + 9}, o {@code preferred} si no hay ninguno */
    public static int firstFree(int preferred) {
        for (int port = preferred; port < preferred + ATTEMPTS && port <= 65_535; port++) {
            if (isFree(port)) {
                return port;
            }
        }
        return preferred;
    }

    static boolean isFree(int port) {
        try (ServerSocket socket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            socket.setReuseAddress(false);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Fija {@code server.port} si el perfil es {@code local}. Se llama antes de
     * arrancar Spring. {@code notice} recibe el puerto elegido cuando no es el preferido.
     */
    public static void chooseForLocalProfile(IntConsumer notice) {
        if (!isLocalProfile()) {
            return;
        }
        int preferred = preferredPort();
        int chosen = firstFree(preferred);
        System.setProperty("server.port", Integer.toString(chosen));
        if (chosen != preferred) {
            notice.accept(chosen);
        }
    }

    private static boolean isLocalProfile() {
        String profiles = System.getProperty("spring.profiles.active", System.getenv("SPRING_PROFILES_ACTIVE"));
        return profiles != null && java.util.Arrays.asList(profiles.split(",")).contains("local");
    }

    private static int preferredPort() {
        String configured = System.getenv("BROWSEME_PORT");
        if (configured != null) {
            try {
                int port = Integer.parseInt(configured.strip());
                if (port > 0 && port <= 65_535) {
                    return port;
                }
            } catch (NumberFormatException e) {
                // Valor no válido: se usa el puerto por defecto.
            }
        }
        return DEFAULT_PORT;
    }
}
