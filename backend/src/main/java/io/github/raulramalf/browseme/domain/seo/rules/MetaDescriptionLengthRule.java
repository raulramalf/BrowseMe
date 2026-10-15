package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Editorial: el rango es orientativo, no un error. */
public final class MetaDescriptionLengthRule implements SeoRule {

    public static final int MIN_LENGTH = 70;
    public static final int MAX_LENGTH = 160;

    @Override
    public RuleId id() {
        return RuleId.META_DESC_LENGTH;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (Evidence.isBlank(snapshot.metaDescription())) {
            return Optional.empty();
        }
        int length = Evidence.length(snapshot.metaDescription());
        if (length >= MIN_LENGTH && length <= MAX_LENGTH) {
            return Optional.empty();
        }
        String recommendation = length < MIN_LENGTH
                ? "Valora ampliar la metadescripción con lo que diferencia a tu negocio."
                : "Valora acortar la metadescripción para que no se corte en los resultados.";
        return issue("La metadescripción tiene " + length + " caracteres: "
                        + Evidence.quote(snapshot.metaDescription()) + ".",
                "El rango de " + MIN_LENGTH + " a " + MAX_LENGTH + " caracteres es orientativo.",
                recommendation);
    }
}
