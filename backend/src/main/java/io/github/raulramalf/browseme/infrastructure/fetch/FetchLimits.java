package io.github.raulramalf.browseme.infrastructure.fetch;

import java.time.Duration;
import java.util.Objects;

/**
 * Límites de la descarga. Los valores por defecto son los del apartado 6 del
 * prompt de desarrollo; los tests los reducen para no esperar segundos.
 *
 * @param totalTimeout tope para la página completa, contando todas las redirecciones
 * @param robotsTimeout tope para robots.txt, que es secundario y no debe retrasar el análisis
 */
public record FetchLimits(
        Duration connectTimeout,
        Duration readTimeout,
        Duration totalTimeout,
        Duration robotsTimeout,
        int maxBodyBytes,
        int maxRedirects) {

    public static final FetchLimits DEFAULTS = new FetchLimits(
            Duration.ofSeconds(5), Duration.ofSeconds(10), Duration.ofSeconds(20), Duration.ofSeconds(5),
            2 * 1024 * 1024, 3);

    public FetchLimits {
        Objects.requireNonNull(connectTimeout, "connectTimeout");
        Objects.requireNonNull(readTimeout, "readTimeout");
        Objects.requireNonNull(totalTimeout, "totalTimeout");
        Objects.requireNonNull(robotsTimeout, "robotsTimeout");
        if (maxBodyBytes <= 0 || maxRedirects < 0) {
            throw new IllegalArgumentException("Límites de descarga no válidos");
        }
    }
}
