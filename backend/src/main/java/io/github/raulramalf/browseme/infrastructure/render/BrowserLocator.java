package io.github.raulramalf.browseme.infrastructure.render;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Busca un navegador basado en Chromium ya instalado (Chrome, Edge o Chromium)
 * en las rutas habituales de cada sistema. La app no incluye navegador propio.
 */
final class BrowserLocator {

    private static final List<String> LINUX_COMMANDS = List.of("google-chrome", "google-chrome-stable",
            "chromium", "chromium-browser", "microsoft-edge", "microsoft-edge-stable");
    private static final List<String> LINUX_PATHS = List.of("/opt/google/chrome/chrome", "/usr/bin/chromium",
            "/snap/bin/chromium", "/opt/microsoft/msedge/msedge");
    private static final List<String> MAC_APPS = List.of(
            "Google Chrome.app/Contents/MacOS/Google Chrome",
            "Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
            "Chromium.app/Contents/MacOS/Chromium");
    private static final List<String> WINDOWS_SUFFIXES = List.of(
            "Google\\Chrome\\Application\\chrome.exe",
            "Microsoft\\Edge\\Application\\msedge.exe",
            "Chromium\\Application\\chrome.exe");

    private final String osName;
    private final Map<String, String> env;
    private final String userHome;

    BrowserLocator() {
        this(System.getProperty("os.name", ""), System.getenv(), System.getProperty("user.home", ""));
    }

    BrowserLocator(String osName, Map<String, String> env, String userHome) {
        this.osName = osName.toLowerCase(Locale.ROOT);
        this.env = env;
        this.userHome = userHome;
    }

    /** @param configured ruta indicada en la configuración; si existe, tiene prioridad */
    Optional<Path> find(String configured) {
        if (configured != null && !configured.isBlank()) {
            Path path = Path.of(configured.strip());
            return Files.isExecutable(path) ? Optional.of(path) : Optional.empty();
        }
        return candidates().stream().filter(Files::isExecutable).findFirst();
    }

    List<Path> candidates() {
        List<Path> candidates = new ArrayList<>();
        if (osName.contains("win")) {
            for (String base : List.of("ProgramFiles", "ProgramFiles(x86)", "LOCALAPPDATA")) {
                String dir = env.get(base);
                if (dir != null && !dir.isBlank()) {
                    WINDOWS_SUFFIXES.forEach(suffix -> candidates.add(Path.of(dir, suffix)));
                }
            }
        } else if (osName.contains("mac")) {
            for (String app : MAC_APPS) {
                candidates.add(Path.of("/Applications", app));
                candidates.add(Path.of(userHome, "Applications", app));
            }
        } else {
            String pathVariable = env.getOrDefault("PATH", "");
            for (String command : LINUX_COMMANDS) {
                for (String dir : pathVariable.split(java.io.File.pathSeparator)) {
                    if (!dir.isBlank()) {
                        candidates.add(Path.of(dir, command));
                    }
                }
            }
            LINUX_PATHS.forEach(p -> candidates.add(Path.of(p)));
        }
        return candidates;
    }
}
