package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Imágenes sin width y height: la página «salta» mientras carga. */
public final class ImagesNoDimensionsRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.IMAGES_NO_DIMENSIONS;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        int count = snapshot.design().imagesWithoutDimensions();
        if (count == 0) {
            return Optional.empty();
        }
        return issue(count + " de " + snapshot.images().total() + " imágenes no tienen atributos width y height.",
                "Si el tamaño se reserva con CSS (aspect-ratio), no hay salto aunque falten los atributos.",
                "Indica width y height en cada imagen para reservar su hueco y evitar que el contenido se mueva al cargar.");
    }
}
