package io.github.raulramalf.browseme.domain.seo;

import static io.github.raulramalf.browseme.domain.seo.IssueCategory.EDITORIAL;
import static io.github.raulramalf.browseme.domain.seo.IssueCategory.TECHNICAL;
import static io.github.raulramalf.browseme.domain.seo.IssueSeverity.HIGH;
import static io.github.raulramalf.browseme.domain.seo.IssueSeverity.INFO;
import static io.github.raulramalf.browseme.domain.seo.IssueSeverity.LOW;
import static io.github.raulramalf.browseme.domain.seo.IssueSeverity.MEDIUM;

/**
 * Catálogo cerrado de reglas SEO (docs/PROMPT_DESARROLLO.md, apartado 5). La
 * categoría y la severidad son propiedades de la regla, no de cada hallazgo,
 * para que no puedan contradecirse.
 */
public enum RuleId {
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
    FETCH_HTTP_ERROR(TECHNICAL, HIGH);

    private final IssueCategory category;
    private final IssueSeverity severity;

    RuleId(IssueCategory category, IssueSeverity severity) {
        this.category = category;
        this.severity = severity;
    }

    public IssueCategory category() {
        return category;
    }

    public IssueSeverity severity() {
        return severity;
    }
}
