package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Editorial: el rango es orientativo, no un error. */
public final class TitleLengthRule implements SeoRule {

    public static final int MIN_LENGTH = 30;
    public static final int MAX_LENGTH = 65;

    @Override
    public RuleId id() {
        return RuleId.TITLE_LENGTH;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (Evidence.isBlank(snapshot.title())) {
            return Optional.empty();
        }
        int length = Evidence.length(snapshot.title());
        if (length >= MIN_LENGTH && length <= MAX_LENGTH) {
            return Optional.empty();
        }
        String recommendation = length < MIN_LENGTH
                ? "Valora ampliar el título con lo que ofreces o tu localidad."
                : "Valora acortar el título para que no se corte en los resultados de búsqueda.";
        return issue("El título tiene " + length + " caracteres: " + Evidence.quote(snapshot.title()) + ".",
                "Google recorta los títulos por ancho en píxeles, no por caracteres; el rango de "
                        + MIN_LENGTH + " a " + MAX_LENGTH + " caracteres es orientativo.",
                recommendation);
    }
}
