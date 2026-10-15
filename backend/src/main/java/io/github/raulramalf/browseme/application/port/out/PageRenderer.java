package io.github.raulramalf.browseme.application.port.out;

import io.github.raulramalf.browseme.domain.page.VisualCheck;
import java.net.URI;

/**
 * Dibuja la página en un navegador real para medir patrones visuales. Es
 * prescindible: nunca lanza excepciones, sino que devuelve un
 * {@link VisualCheck} con el motivo (sin navegador, fallo, desactivado).
 */
public interface PageRenderer {

    /** @param pageUrl URL final de la página, ya validada y descargada con éxito */
    VisualCheck render(URI pageUrl);
}
