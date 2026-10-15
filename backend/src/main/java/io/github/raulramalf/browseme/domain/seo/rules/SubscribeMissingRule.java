package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Sin forma de dejar el email para volver a contactar con el visitante. */
public final class SubscribeMissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.SUBSCRIBE_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (snapshot.engagement().emailForms() > 0) {
            return Optional.empty();
        }
        return issue("No hay ningún formulario con un campo de email (boletín, suscripción o contacto).",
                "No todos los negocios necesitan un boletín; tómalo como sugerencia si quieres que los clientes vuelvan.",
                "Ofrece algo a cambio del email (novedades, descuentos, avisos) para poder volver a contactar con quien te visita.");
    }
}
