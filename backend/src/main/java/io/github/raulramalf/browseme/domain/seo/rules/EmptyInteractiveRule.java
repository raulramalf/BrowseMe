package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Enlaces o botones sin texto ni nombre accesible. */
public final class EmptyInteractiveRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.EMPTY_INTERACTIVE;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        int count = snapshot.design().emptyInteractive();
        if (count == 0) {
            return Optional.empty();
        }
        return issue(count + (count == 1 ? " enlace o botón no tiene" : " enlaces o botones no tienen")
                        + " texto, aria-label, title ni imagen con alt.",
                "Algunos iconos pueden tener texto añadido con CSS o JavaScript que aquí no se ve.",
                "Da a cada botón o enlace con icono un texto o un aria-label («Abrir menú», «Instagram»).");
    }
}
