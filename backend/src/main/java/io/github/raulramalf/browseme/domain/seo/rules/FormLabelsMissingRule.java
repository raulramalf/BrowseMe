package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Campos de formulario sin etiqueta. */
public final class FormLabelsMissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.FORM_LABELS_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        int unlabeled = snapshot.design().unlabeledFormFields();
        if (unlabeled == 0) {
            return Optional.empty();
        }
        return issue(unlabeled + " de " + snapshot.design().formFields()
                        + " campos de formulario no tienen etiqueta (label, aria-label ni title).",
                "Un placeholder no cuenta como etiqueta: desaparece al escribir.",
                "Asocia cada campo a un <label> visible: los formularios se rellenan antes y con menos errores.");
    }
}
