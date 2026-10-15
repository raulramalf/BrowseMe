package io.github.raulramalf.browseme.domain.seo;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import java.util.Optional;

/**
 * Regla SEO determinista: función pura de un {@link PageSnapshot}. Cada regla
 * produce como mucho un hallazgo; si hay varias apariciones (varias imágenes sin
 * {@code alt}, varios saltos de nivel), se resumen en la evidencia.
 */
public interface SeoRule {

    RuleId id();

    Optional<SeoIssue> evaluate(PageSnapshot snapshot);

    default Optional<SeoIssue> issue(String evidence, String limitation, String recommendation) {
        return Optional.of(new SeoIssue(id(), evidence, limitation, recommendation));
    }
}
