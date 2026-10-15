package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Editorial: HTML5 permite varios H1. */
public final class H1MultipleRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.H1_MULTIPLE;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        long count = snapshot.headingCount(1);
        if (count <= 1) {
            return Optional.empty();
        }
        return issue("La página tiene " + count + " encabezados H1.",
                "HTML5 permite varios H1; no es un error, solo una sugerencia de claridad.",
                "Valora dejar un único H1 con el tema principal y usar H2 para las secciones.");
    }
}
