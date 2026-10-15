package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.page.RobotsTxtStatus;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

public final class RobotsTxtBlocksRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.ROBOTS_TXT_BLOCKS;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (snapshot.robotsTxt() != RobotsTxtStatus.BLOCKED) {
            return Optional.empty();
        }
        return issue("El archivo robots.txt del sitio impide a Googlebot rastrear esta URL.",
                "Interpretación simplificada de robots.txt (RFC 9309), evaluada para Googlebot.",
                "Si quieres que Google rastree esta página, revisa las reglas Disallow de robots.txt.");
    }
}
