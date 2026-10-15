package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Sin prueba social: ni reseñas marcadas con schema.org ni un apartado de opiniones o testimonios. */
public final class SocialProofMissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.SOCIAL_PROOF_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (snapshot.engagement().testimonials()
                || snapshot.engagement().declaresSchemaType("Review", "AggregateRating")) {
            return Optional.empty();
        }
        return issue("No hay datos estructurados de reseñas (Review o AggregateRating) ni un encabezado de opiniones, reseñas o testimonios.",
                "Las opiniones pueden estar en un widget cargado con JavaScript o sin encabezado propio y no detectarse.",
                "Muestra opiniones reales de clientes (con su permiso) en un apartado propio; generan confianza y animan a decidirse.");
    }
}
