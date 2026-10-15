package io.github.raulramalf.browseme.domain.seo;

import static io.github.raulramalf.browseme.domain.seo.IssueCategory.EDITORIAL;
import static io.github.raulramalf.browseme.domain.seo.IssueCategory.TECHNICAL;
import static io.github.raulramalf.browseme.domain.seo.IssueSeverity.HIGH;
import static io.github.raulramalf.browseme.domain.seo.IssueSeverity.INFO;
import static io.github.raulramalf.browseme.domain.seo.IssueSeverity.LOW;
import static io.github.raulramalf.browseme.domain.seo.IssueSeverity.MEDIUM;

/**
 * Catálogo cerrado de reglas (docs/PROMPT_DESARROLLO.md, apartado 5). La
 * categoría, la severidad y el área son propiedades de la regla, no de cada
 * hallazgo, para que no puedan contradecirse.
 */
public enum RuleId {
    // --- SEO técnico y de contenido -------------------------------------------------------
    TITLE_MISSING(TECHNICAL, HIGH),
    TITLE_LENGTH(EDITORIAL, LOW),
    META_DESC_MISSING(TECHNICAL, MEDIUM),
    META_DESC_LENGTH(EDITORIAL, LOW),
    H1_MISSING(TECHNICAL, MEDIUM),
    H1_MULTIPLE(EDITORIAL, INFO),
    HEADING_SKIP(EDITORIAL, LOW),
    LANG_MISSING(TECHNICAL, LOW),
    CANONICAL_MISSING(TECHNICAL, INFO),
    CANONICAL_DIFFERENT(TECHNICAL, INFO),
    NOINDEX_DETECTED(TECHNICAL, HIGH),
    ROBOTS_TXT_BLOCKS(TECHNICAL, HIGH),
    NOT_HTTPS(TECHNICAL, HIGH),
    VIEWPORT_MISSING(TECHNICAL, MEDIUM),
    IMAGES_NO_ALT(TECHNICAL, MEDIUM),
    THIN_CONTENT(EDITORIAL, INFO),
    REDIRECT_CHAIN(TECHNICAL, LOW),
    FETCH_HTTP_ERROR(TECHNICAL, HIGH),

    // --- Captación y fidelización de clientes --------------------------------------------
    CONTACT_MISSING(EDITORIAL, MEDIUM, RuleArea.ENGAGEMENT),
    CTA_MISSING(EDITORIAL, MEDIUM, RuleArea.ENGAGEMENT),
    SUBSCRIBE_MISSING(EDITORIAL, LOW, RuleArea.ENGAGEMENT),
    SOCIAL_PROOF_MISSING(EDITORIAL, LOW, RuleArea.ENGAGEMENT),
    SOCIAL_LINKS_MISSING(EDITORIAL, INFO, RuleArea.ENGAGEMENT),
    BUSINESS_DATA_MISSING(TECHNICAL, LOW, RuleArea.ENGAGEMENT),
    SHARE_PREVIEW_MISSING(TECHNICAL, LOW, RuleArea.ENGAGEMENT),

    // --- Patrones de diseño: estructura del HTML -----------------------------------------
    NAV_MISSING(EDITORIAL, LOW, RuleArea.DESIGN),
    GENERIC_LINK_TEXT(EDITORIAL, LOW, RuleArea.DESIGN),
    EMPTY_INTERACTIVE(TECHNICAL, MEDIUM, RuleArea.DESIGN),
    FORM_LABELS_MISSING(TECHNICAL, MEDIUM, RuleArea.DESIGN),
    IMAGES_NO_DIMENSIONS(TECHNICAL, LOW, RuleArea.DESIGN),
    FAVICON_MISSING(TECHNICAL, LOW, RuleArea.DESIGN),

    // --- Patrones de diseño: página dibujada en un navegador real -------------------------
    MOBILE_HORIZONTAL_SCROLL(TECHNICAL, HIGH, RuleArea.DESIGN),
    LOW_CONTRAST_TEXT(TECHNICAL, MEDIUM, RuleArea.DESIGN),
    SMALL_TAP_TARGETS(TECHNICAL, MEDIUM, RuleArea.DESIGN),
    SMALL_TEXT_MOBILE(TECHNICAL, LOW, RuleArea.DESIGN),
    CTA_NOT_VISIBLE_MOBILE(EDITORIAL, LOW, RuleArea.DESIGN),
    TOO_MANY_FONTS(EDITORIAL, INFO, RuleArea.DESIGN);

    private final IssueCategory category;
    private final IssueSeverity severity;
    private final RuleArea area;

    RuleId(IssueCategory category, IssueSeverity severity) {
        this(category, severity, RuleArea.SEO);
    }

    RuleId(IssueCategory category, IssueSeverity severity, RuleArea area) {
        this.category = category;
        this.severity = severity;
        this.area = area;
    }

    public IssueCategory category() {
        return category;
    }

    public IssueSeverity severity() {
        return severity;
    }

    public RuleArea area() {
        return area;
    }
}
