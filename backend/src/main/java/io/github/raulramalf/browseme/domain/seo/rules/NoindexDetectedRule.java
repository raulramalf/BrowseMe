package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

public final class NoindexDetectedRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.NOINDEX_DETECTED;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        String directive = snapshot.hasRobotsDirective("noindex") ? "noindex"
                : snapshot.hasRobotsDirective("none") ? "none" : null;
        if (directive == null) {
            return Optional.empty();
        }
        return issue("Las directivas de robots de la página (meta robots o cabecera X-Robots-Tag) incluyen «"
                        + directive + "»: los buscadores no la mostrarán en sus resultados.",
                "Puede ser intencionado si no quieres que esta página aparezca en buscadores.",
                "Si quieres que la página aparezca en Google, elimina la directiva " + directive + ".");
    }
}
