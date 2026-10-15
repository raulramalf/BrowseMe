package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.Heading;
import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.List;
import java.util.Optional;

/**
 * Editorial: salto de nivel entre encabezados consecutivos (H2 a H4). Bajar de
 * nivel (H4 a H2) es normal y no cuenta.
 */
public final class HeadingSkipRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.HEADING_SKIP;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        List<Heading> headings = snapshot.headings();
        int skips = 0;
        String first = null;
        for (int i = 1; i < headings.size(); i++) {
            Heading previous = headings.get(i - 1);
            Heading current = headings.get(i);
            if (current.level() > previous.level() + 1) {
                skips++;
                if (first == null) {
                    first = "H" + previous.level() + " a H" + current.level()
                            + (current.text().isBlank() ? "" : " (" + Evidence.quote(current.text()) + ")");
                }
            }
        }
        if (skips == 0) {
            return Optional.empty();
        }
        String evidence = skips == 1
                ? "Hay un salto de nivel de encabezado: de " + first + "."
                : "Hay " + skips + " saltos de nivel de encabezado; el primero, de " + first + ".";
        return issue(evidence,
                "Afecta sobre todo a la accesibilidad y a la claridad de la estructura; su peso en SEO es menor.",
                "Usa los niveles en orden (H1, H2, H3...) sin saltarte ninguno al bajar.");
    }
}
