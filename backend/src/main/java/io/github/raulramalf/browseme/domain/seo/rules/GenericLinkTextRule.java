package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Enlaces con texto genérico («clic aquí», «leer más»). */
public final class GenericLinkTextRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.GENERIC_LINK_TEXT;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        int count = snapshot.design().genericLinkTexts();
        if (count == 0) {
            return Optional.empty();
        }
        return issue(count + (count == 1 ? " enlace usa" : " enlaces usan")
                        + " un texto genérico como «clic aquí», «aquí», «leer más» o «ver más».",
                "",
                "Usa textos de enlace que digan adónde llevan («Ver la carta», «Horarios y ubicación»).");
    }
}
