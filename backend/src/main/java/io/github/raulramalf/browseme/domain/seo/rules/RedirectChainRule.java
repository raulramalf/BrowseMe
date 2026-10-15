package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

public final class RedirectChainRule implements SeoRule {

    public static final int MAX_REDIRECTS_WITHOUT_ISSUE = 2;

    @Override
    public RuleId id() {
        return RuleId.REDIRECT_CHAIN;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (snapshot.redirectCount() <= MAX_REDIRECTS_WITHOUT_ISSUE) {
            return Optional.empty();
        }
        return issue("Se necesitaron " + snapshot.redirectCount() + " redirecciones para llegar a "
                        + snapshot.finalUrl() + ".",
                "",
                "Enlaza y redirige directamente a la dirección final para ahorrar saltos.");
    }
}
