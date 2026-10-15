package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Sin enlaces a redes sociales para que el visitante te siga. */
public final class SocialLinksMissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.SOCIAL_LINKS_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (!snapshot.engagement().socialNetworks().isEmpty()) {
            return Optional.empty();
        }
        return issue("No hay enlaces a Instagram, Facebook, X, LinkedIn, TikTok, YouTube ni Pinterest.",
                "Si tu negocio no usa redes sociales, puedes ignorar esta sugerencia.",
                "Enlaza las redes que uses de verdad, normalmente en la cabecera o el pie, para que te sigan y vuelvan.");
    }
}
