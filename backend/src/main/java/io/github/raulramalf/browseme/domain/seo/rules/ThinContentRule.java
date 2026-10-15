package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Editorial: una página breve puede ser adecuada. */
public final class ThinContentRule implements SeoRule {

    public static final int MIN_WORDS = 150;

    @Override
    public RuleId id() {
        return RuleId.THIN_CONTENT;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (snapshot.visibleWordCount() >= MIN_WORDS) {
            return Optional.empty();
        }
        return issue("La página tiene unas " + snapshot.visibleWordCount() + " palabras visibles (menos de "
                        + MIN_WORDS + ").",
                "Una página breve puede ser perfectamente adecuada. El recuento es aproximado y no incluye "
                        + "texto cargado con JavaScript.",
                "Si la página debe atraer visitas desde buscadores, valora explicar mejor qué ofreces y a quién.");
    }
}
