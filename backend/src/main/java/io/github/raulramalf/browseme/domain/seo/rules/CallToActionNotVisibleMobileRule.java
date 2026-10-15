package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Hay llamadas a la acción, pero ninguna se ve en la primera pantalla del móvil. */
public final class CallToActionNotVisibleMobileRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.CTA_NOT_VISIBLE_MOBILE;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (!snapshot.visual().done() || snapshot.engagement().callsToAction() == 0
                || snapshot.visual().metrics().callToActionAboveFoldMobile()) {
            return Optional.empty();
        }
        return issue("La página tiene " + snapshot.engagement().callsToAction()
                        + " llamadas a la acción, pero ninguna aparece en la primera pantalla de un móvil.",
                "Se mide sin desplazarse y con un móvil simulado de tamaño medio.",
                "Sube la acción principal (reservar, llamar, pedir) a la primera pantalla: muchos visitantes no bajan.");
    }
}
