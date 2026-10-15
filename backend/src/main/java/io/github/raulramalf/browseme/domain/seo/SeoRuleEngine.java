package io.github.raulramalf.browseme.domain.seo;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.rules.BusinessDataMissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.CallToActionMissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.CallToActionNotVisibleMobileRule;
import io.github.raulramalf.browseme.domain.seo.rules.CanonicalDifferentRule;
import io.github.raulramalf.browseme.domain.seo.rules.CanonicalMissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.ContactMissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.EmptyInteractiveRule;
import io.github.raulramalf.browseme.domain.seo.rules.FaviconMissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.FetchHttpErrorRule;
import io.github.raulramalf.browseme.domain.seo.rules.FormLabelsMissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.GenericLinkTextRule;
import io.github.raulramalf.browseme.domain.seo.rules.H1MissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.H1MultipleRule;
import io.github.raulramalf.browseme.domain.seo.rules.HeadingSkipRule;
import io.github.raulramalf.browseme.domain.seo.rules.ImagesNoAltRule;
import io.github.raulramalf.browseme.domain.seo.rules.ImagesNoDimensionsRule;
import io.github.raulramalf.browseme.domain.seo.rules.LangMissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.LowContrastTextRule;
import io.github.raulramalf.browseme.domain.seo.rules.MetaDescriptionLengthRule;
import io.github.raulramalf.browseme.domain.seo.rules.MetaDescriptionMissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.MobileHorizontalScrollRule;
import io.github.raulramalf.browseme.domain.seo.rules.NavigationMissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.NoindexDetectedRule;
import io.github.raulramalf.browseme.domain.seo.rules.NotHttpsRule;
import io.github.raulramalf.browseme.domain.seo.rules.RedirectChainRule;
import io.github.raulramalf.browseme.domain.seo.rules.RobotsTxtBlocksRule;
import io.github.raulramalf.browseme.domain.seo.rules.SharePreviewMissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.SmallTapTargetsRule;
import io.github.raulramalf.browseme.domain.seo.rules.SmallTextMobileRule;
import io.github.raulramalf.browseme.domain.seo.rules.SocialLinksMissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.SocialProofMissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.SubscribeMissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.ThinContentRule;
import io.github.raulramalf.browseme.domain.seo.rules.TitleLengthRule;
import io.github.raulramalf.browseme.domain.seo.rules.TitleMissingRule;
import io.github.raulramalf.browseme.domain.seo.rules.TooManyFontsRule;
import io.github.raulramalf.browseme.domain.seo.rules.ViewportMissingRule;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Aplica el catálogo de reglas a un snapshot. Es una función pura: mismo
 * snapshot, mismos hallazgos, en el mismo orden (severidad y después catálogo).
 */
public final class SeoRuleEngine {

    private static final Comparator<SeoIssue> REPORT_ORDER = Comparator
            .comparing(SeoIssue::severity)
            .thenComparing(SeoIssue::ruleId);

    private final List<SeoRule> rules;

    public SeoRuleEngine(List<SeoRule> rules) {
        Set<RuleId> seen = EnumSet.noneOf(RuleId.class);
        for (SeoRule rule : rules) {
            if (!seen.add(rule.id())) {
                throw new IllegalArgumentException("Regla duplicada: " + rule.id());
            }
        }
        this.rules = List.copyOf(rules);
    }

    /** Motor con el catálogo completo del apartado 5 (SEO, captación y diseño). */
    public static SeoRuleEngine withDefaultRules() {
        return new SeoRuleEngine(List.of(
                new FetchHttpErrorRule(),
                new TitleMissingRule(),
                new TitleLengthRule(),
                new MetaDescriptionMissingRule(),
                new MetaDescriptionLengthRule(),
                new H1MissingRule(),
                new H1MultipleRule(),
                new HeadingSkipRule(),
                new LangMissingRule(),
                new CanonicalMissingRule(),
                new CanonicalDifferentRule(),
                new NoindexDetectedRule(),
                new RobotsTxtBlocksRule(),
                new NotHttpsRule(),
                new ViewportMissingRule(),
                new ImagesNoAltRule(),
                new ThinContentRule(),
                new RedirectChainRule(),
                // Captación y fidelización
                new ContactMissingRule(),
                new CallToActionMissingRule(),
                new SubscribeMissingRule(),
                new SocialProofMissingRule(),
                new SocialLinksMissingRule(),
                new BusinessDataMissingRule(),
                new SharePreviewMissingRule(),
                // Diseño: estructura del HTML
                new NavigationMissingRule(),
                new GenericLinkTextRule(),
                new EmptyInteractiveRule(),
                new FormLabelsMissingRule(),
                new ImagesNoDimensionsRule(),
                new FaviconMissingRule(),
                // Diseño: página dibujada (solo si hubo análisis visual)
                new MobileHorizontalScrollRule(),
                new LowContrastTextRule(),
                new SmallTapTargetsRule(),
                new SmallTextMobileRule(),
                new CallToActionNotVisibleMobileRule(),
                new TooManyFontsRule()));
    }

    public List<SeoIssue> evaluate(PageSnapshot snapshot) {
        // Una página de error no tiene contenido que analizar: solo se informa del error.
        List<SeoRule> applicable = FetchHttpErrorRule.isHttpError(snapshot)
                ? rules.stream().filter(r -> r.id() == RuleId.FETCH_HTTP_ERROR).toList()
                : rules;
        return applicable.stream()
                .map(rule -> rule.evaluate(snapshot))
                .flatMap(Optional::stream)
                .sorted(REPORT_ORDER)
                .toList();
    }
}
