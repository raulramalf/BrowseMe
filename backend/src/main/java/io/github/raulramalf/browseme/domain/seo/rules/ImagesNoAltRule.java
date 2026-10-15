package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.ImageStats;
import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Solo cuentan las imágenes sin atributo alt; {@code alt=""} es válido en imágenes decorativas. */
public final class ImagesNoAltRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.IMAGES_NO_ALT;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        ImageStats images = snapshot.images();
        if (images.missingAlt() == 0) {
            return Optional.empty();
        }
        String evidence = images.missingAlt() + " de " + images.total() + " imágenes no tienen atributo alt.";
        if (images.decorative() > 0) {
            evidence += " Otras " + images.decorative()
                    + " tienen alt vacío, lo que es correcto si son decorativas, y no se cuentan.";
        }
        return issue(evidence,
                "Las imágenes decorativas deben llevar alt=\"\"; solo se cuenta como problema la ausencia del atributo.",
                "Describe cada imagen informativa en su atributo alt y pon alt=\"\" en las decorativas.");
    }
}
