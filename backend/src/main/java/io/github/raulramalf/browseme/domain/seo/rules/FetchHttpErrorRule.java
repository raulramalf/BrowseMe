package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Respuesta 4xx/5xx: el análisis se detiene y se informa. */
public final class FetchHttpErrorRule implements SeoRule {

    public static boolean isHttpError(PageSnapshot snapshot) {
        return snapshot.httpStatus() >= 400;
    }

    @Override
    public RuleId id() {
        return RuleId.FETCH_HTTP_ERROR;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (!isHttpError(snapshot)) {
            return Optional.empty();
        }
        return issue("La página respondió con el código HTTP " + snapshot.httpStatus() + ".",
                "Con una respuesta de error no se analiza el contenido de la página.",
                "Comprueba que la dirección es correcta y que la página está publicada y accesible.");
    }
}
