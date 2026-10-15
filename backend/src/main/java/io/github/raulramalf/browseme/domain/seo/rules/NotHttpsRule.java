package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

public final class NotHttpsRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.NOT_HTTPS;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (snapshot.https()) {
            return Optional.empty();
        }
        return issue("La página final se sirve por HTTP, sin cifrar: " + snapshot.finalUrl() + ".",
                "",
                "Activa HTTPS con un certificado (muchos proveedores lo ofrecen gratis) y redirige HTTP a HTTPS.");
    }
}
