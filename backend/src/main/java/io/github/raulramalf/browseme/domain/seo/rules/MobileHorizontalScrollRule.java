package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import io.github.raulramalf.browseme.domain.page.VisualMetrics;
import java.util.Optional;

/** En móvil el contenido es más ancho que la pantalla. */
public final class MobileHorizontalScrollRule implements SeoRule {

    /** Margen para redondeos y barras de desplazamiento. */
    static final int TOLERANCE_PX = 8;

    @Override
    public RuleId id() {
        return RuleId.MOBILE_HORIZONTAL_SCROLL;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (!snapshot.visual().done()) {
            return Optional.empty();
        }
        VisualMetrics metrics = snapshot.visual().metrics();
        if (metrics.horizontalOverflow() <= TOLERANCE_PX) {
            return Optional.empty();
        }
        return issue("En una pantalla de móvil de " + metrics.viewportWidth() + " px el contenido mide "
                        + metrics.contentWidth() + " px: hay que desplazarse a los lados.",
                "Medido con un móvil simulado en el navegador; algunos dispositivos pueden mostrarlo distinto.",
                "Busca el elemento más ancho (tablas, imágenes o bloques con ancho fijo) y limita su ancho al de la pantalla.");
    }
}
