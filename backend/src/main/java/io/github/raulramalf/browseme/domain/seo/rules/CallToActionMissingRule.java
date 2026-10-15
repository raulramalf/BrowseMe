package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Sin llamadas a la acción: ningún enlace o botón invita a reservar, comprar, pedir o contactar. */
public final class CallToActionMissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.CTA_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (snapshot.engagement().callsToAction() > 0) {
            return Optional.empty();
        }
        return issue("Ningún enlace ni botón de la página contiene un verbo de acción (reservar, comprar, pedir, contactar, llamar, solicitar…).",
                "Se buscan verbos habituales en español e inglés; una llamada a la acción con otras palabras no se detecta.",
                "Añade un botón claro con lo que quieres que haga el visitante («Reserva tu mesa», «Pide presupuesto») cerca del principio de la página.");
    }
}
