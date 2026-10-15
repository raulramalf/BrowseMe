package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Sin vista previa al compartir: faltan og:title u og:image. */
public final class SharePreviewMissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.SHARE_PREVIEW_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        List<String> missing = new ArrayList<>();
        if (!snapshot.engagement().openGraphTitle()) {
            missing.add("og:title");
        }
        if (!snapshot.engagement().openGraphImage()) {
            missing.add("og:image");
        }
        if (missing.isEmpty()) {
            return Optional.empty();
        }
        return issue("Faltan las etiquetas Open Graph: " + String.join(" y ", missing) + ".",
                "",
                "Añade og:title y og:image para que, al compartir tu página por WhatsApp o redes, se vea con título e imagen.");
    }
}
