package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Sin navegación marcada (nav o role=navigation). */
public final class NavigationMissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.NAV_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (snapshot.design().navigation()) {
            return Optional.empty();
        }
        return issue("No hay ningún elemento <nav> ni role=\"navigation\".",
                "Un menú hecho con otros elementos puede funcionar visualmente aunque no esté marcado como navegación.",
                "Agrupa el menú principal en un <nav>: ayuda a lectores de pantalla y a entender la estructura del sitio.");
    }
}
