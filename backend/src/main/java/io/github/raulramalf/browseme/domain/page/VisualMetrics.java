package io.github.raulramalf.browseme.domain.page;

import java.util.List;
import java.util.Objects;

/**
 * Lo que se midió con la página dibujada en un navegador real, sobre todo en
 * pantalla de móvil. Los textos citados vienen de la página: son datos.
 *
 * @param viewportWidth        ancho de la pantalla de móvil simulada (px)
 * @param contentWidth         ancho real del contenido en esa pantalla (px)
 * @param textElementsChecked  elementos de texto visibles revisados
 * @param lowContrastTexts     de ellos, con contraste por debajo de WCAG AA
 * @param lowContrastSamples   ejemplos de esos textos (acotados)
 * @param smallTexts           textos de menos de 12 px en móvil
 * @param tapTargetsChecked    elementos pulsables revisados en móvil
 * @param smallTapTargets      de ellos, de menos de 24×24 px (WCAG 2.5.8)
 * @param fontFamilies         familias tipográficas distintas en uso
 * @param callToActionAboveFoldMobile  hay una llamada a la acción visible en la primera pantalla del móvil
 * @param callToActionAboveFoldDesktop ídem en escritorio
 * @param pageHeightScreens    alto de la página en pantallas de móvil
 */
public record VisualMetrics(
        int viewportWidth,
        int contentWidth,
        int textElementsChecked,
        int lowContrastTexts,
        List<String> lowContrastSamples,
        int smallTexts,
        int tapTargetsChecked,
        int smallTapTargets,
        List<String> fontFamilies,
        boolean callToActionAboveFoldMobile,
        boolean callToActionAboveFoldDesktop,
        int pageHeightScreens) {

    public static final int MAX_SAMPLES = 5;
    public static final int MAX_SAMPLE_LENGTH = 80;
    public static final int MAX_FONTS = 10;

    public VisualMetrics {
        if (viewportWidth <= 0 || contentWidth < 0 || textElementsChecked < 0 || lowContrastTexts < 0
                || smallTexts < 0 || tapTargetsChecked < 0 || smallTapTargets < 0 || pageHeightScreens < 0) {
            throw new IllegalArgumentException("Métricas visuales fuera de rango");
        }
        if (lowContrastTexts > textElementsChecked || smallTexts > textElementsChecked
                || smallTapTargets > tapTargetsChecked) {
            throw new IllegalArgumentException("Métricas visuales incoherentes");
        }
        lowContrastSamples = bounded(lowContrastSamples, MAX_SAMPLES);
        fontFamilies = bounded(fontFamilies, MAX_FONTS);
    }

    /** Píxeles que sobran a lo ancho en móvil (scroll horizontal). */
    public int horizontalOverflow() {
        return Math.max(0, contentWidth - viewportWidth);
    }

    private static List<String> bounded(List<String> values, int max) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(v -> Texts.truncate(v, MAX_SAMPLE_LENGTH))
                .filter(v -> !v.isEmpty())
                .distinct()
                .limit(max)
                .toList();
    }
}
