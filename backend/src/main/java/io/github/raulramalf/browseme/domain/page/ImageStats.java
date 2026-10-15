package io.github.raulramalf.browseme.domain.page;

/**
 * Recuento de imágenes según su atributo {@code alt}.
 *
 * @param withAlt    imágenes con {@code alt} no vacío
 * @param decorative imágenes con {@code alt=""}: válido para imágenes decorativas, no es un error
 * @param missingAlt imágenes sin atributo {@code alt}
 */
public record ImageStats(int total, int withAlt, int decorative, int missingAlt) {

    public ImageStats {
        if (withAlt < 0 || decorative < 0 || missingAlt < 0) {
            throw new IllegalArgumentException("Los recuentos de imágenes no pueden ser negativos");
        }
        if (withAlt + decorative + missingAlt != total) {
            throw new IllegalArgumentException("Los recuentos de imágenes no suman el total");
        }
    }

    public static ImageStats none() {
        return new ImageStats(0, 0, 0, 0);
    }
}
