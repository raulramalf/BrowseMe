package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Sin ninguna forma directa de contacto: ni teléfono, ni email, ni WhatsApp, ni formulario. */
public final class ContactMissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.CONTACT_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (snapshot.engagement().contactChannels() > 0) {
            return Optional.empty();
        }
        return issue("La página no tiene enlaces de teléfono (tel:), email (mailto:) ni WhatsApp, ni ningún formulario.",
                "Puede que el contacto esté en otra página del sitio; esta solo revisa la página analizada.",
                "Añade al menos un teléfono pulsable, un email o un botón de WhatsApp visibles, para que quien llegue pueda escribirte sin buscar.");
    }
}
