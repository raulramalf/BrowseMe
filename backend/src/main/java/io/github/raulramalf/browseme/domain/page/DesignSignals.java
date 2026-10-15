package io.github.raulramalf.browseme.domain.page;

/**
 * Patrones de diseño y usabilidad detectables en la estructura del HTML, sin
 * dibujar la página.
 *
 * @param navigation              hay un {@code <nav>} o {@code role="navigation"}
 * @param favicon                 hay un {@code <link rel="icon">}
 * @param genericLinkTexts        enlaces con texto genérico («clic aquí», «leer más»…)
 * @param emptyInteractive        enlaces o botones sin texto ni nombre accesible
 * @param formFields              campos de formulario visibles para el usuario
 * @param unlabeledFormFields     de ellos, sin etiqueta asociada
 * @param imagesWithoutDimensions imágenes sin atributos {@code width} y {@code height}
 */
public record DesignSignals(
        boolean navigation,
        boolean favicon,
        int genericLinkTexts,
        int emptyInteractive,
        int formFields,
        int unlabeledFormFields,
        int imagesWithoutDimensions) {

    public DesignSignals {
        if (genericLinkTexts < 0 || emptyInteractive < 0 || formFields < 0 || unlabeledFormFields < 0
                || imagesWithoutDimensions < 0) {
            throw new IllegalArgumentException("Los recuentos no pueden ser negativos");
        }
        if (unlabeledFormFields > formFields) {
            throw new IllegalArgumentException("No puede haber más campos sin etiqueta que campos");
        }
    }

    public static DesignSignals none() {
        return new DesignSignals(false, false, 0, 0, 0, 0, 0);
    }
}
