package io.github.raulramalf.browseme.domain.page;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Señales de captación y fidelización de clientes detectadas en el HTML: formas
 * de contacto, llamadas a la acción, suscripción, prueba social y datos para
 * compartir. Son indicios en el código; no miden visitas ni clientes reales.
 *
 * @param phoneLinks       enlaces {@code tel:}
 * @param emailLinks       enlaces {@code mailto:}
 * @param whatsappLinks    enlaces a WhatsApp (wa.me, api.whatsapp.com, whatsapp:)
 * @param forms            formularios de la página
 * @param emailForms       formularios con un campo de email (suscripción o contacto)
 * @param callsToAction    enlaces o botones con un verbo de acción («reserva», «contacta»…)
 * @param socialNetworks   redes sociales enlazadas, por nombre normalizado («instagram»…)
 * @param schemaTypes      tipos de schema.org declarados (JSON-LD o microdatos)
 * @param testimonials     encabezado que anuncia opiniones, reseñas o testimonios
 * @param openGraphTitle   {@code og:title} presente
 * @param openGraphImage   {@code og:image} presente
 */
public record EngagementSignals(
        int phoneLinks,
        int emailLinks,
        int whatsappLinks,
        int forms,
        int emailForms,
        int callsToAction,
        Set<String> socialNetworks,
        Set<String> schemaTypes,
        boolean testimonials,
        boolean openGraphTitle,
        boolean openGraphImage) {

    public static final int MAX_SCHEMA_TYPES = 30;
    public static final int MAX_NAME_LENGTH = 60;

    public EngagementSignals {
        if (phoneLinks < 0 || emailLinks < 0 || whatsappLinks < 0 || forms < 0 || emailForms < 0
                || callsToAction < 0) {
            throw new IllegalArgumentException("Los recuentos no pueden ser negativos");
        }
        socialNetworks = bounded(socialNetworks);
        schemaTypes = bounded(schemaTypes);
    }

    /** Sin señales: páginas de error o análisis guardados antes de existir este dato. */
    public static EngagementSignals none() {
        return new EngagementSignals(0, 0, 0, 0, 0, 0, Set.of(), Set.of(), false, false, false);
    }

    public int contactChannels() {
        return phoneLinks + emailLinks + whatsappLinks + forms;
    }

    public boolean declaresSchemaType(String... types) {
        for (String type : types) {
            if (schemaTypes.contains(type)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> bounded(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(v -> Texts.truncate(v, MAX_NAME_LENGTH))
                .filter(v -> !v.isEmpty())
                .limit(MAX_SCHEMA_TYPES)
                .collect(Collectors.toUnmodifiableSet());
    }
}
