package io.github.raulramalf.browseme.domain.analysis;

import io.github.raulramalf.browseme.domain.page.RobotsTxtStatus;
import io.github.raulramalf.browseme.domain.page.VisualStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * Lo que el informe <em>no</em> puede afirmar (apartado 5). Se muestra siempre,
 * más las limitaciones concretas de cada análisis.
 */
public final class ReportLimitations {

    public static final List<String> ALWAYS = List.of(
            "No mide la velocidad real de carga de la página.",
            "No valora la calidad ni la utilidad del contenido para tus clientes.",
            "No conoce tu posicionamiento real en Google ni el de tu competencia.",
            "No ve visitas, ventas ni clientes que repiten: las señales de captación y fidelización son "
                    + "indicios en la página, no datos de tu negocio.",
            "La nota de 0 a 10 resume los hallazgos con una fórmula fija; es orientativa.",
            "Solo analiza esta página, no el resto del sitio.");

    static final String WITHOUT_VISUAL_JS =
            "No ejecuta JavaScript: lo que se genere con él en el navegador no se ha analizado.";
    static final String WITHOUT_VISUAL_MOBILE =
            "No comprueba la usabilidad real en móviles; solo detecta indicios en el código.";
    static final String WITH_VISUAL =
            "El análisis visual dibuja la página una vez en un móvil y un escritorio simulados, sin interactuar; "
                    + "no sustituye probarla en dispositivos reales.";

    private ReportLimitations() {
    }

    public static List<String> of(WebsiteAnalysis analysis) {
        List<String> limitations = new ArrayList<>(ALWAYS);
        VisualStatus visual = analysis.snapshot().map(s -> s.visual().status()).orElse(VisualStatus.NOT_RUN);
        if (visual == VisualStatus.DONE) {
            limitations.add(WITH_VISUAL);
        } else {
            limitations.add(WITHOUT_VISUAL_JS);
            limitations.add(WITHOUT_VISUAL_MOBILE);
        }
        switch (visual) {
            case BROWSER_NOT_FOUND -> limitations.add("No se hizo el análisis visual porque no se encontró Google "
                    + "Chrome, Microsoft Edge ni Chromium en este equipo; la nota de diseño solo cuenta el HTML.");
            case FAILED -> limitations.add("El navegador no pudo dibujar la página a tiempo; la nota de diseño "
                    + "solo cuenta el HTML.");
            case DISABLED -> limitations.add("El análisis visual está desactivado; la nota de diseño solo cuenta "
                    + "el HTML.");
            default -> { }
        }
        analysis.snapshot().ifPresent(snapshot -> {
            if (snapshot.robotsTxt() == RobotsTxtStatus.UNAVAILABLE) {
                limitations.add("No se pudo leer robots.txt, así que no se sabe si bloquea la página.");
            }
        });
        analysis.aiStatus().ifPresent(status -> {
            if (status != AiStatus.AVAILABLE) {
                limitations.add("La interpretación con IA no está disponible en este informe; "
                        + "los hallazgos verificados por reglas sí lo están.");
            }
        });
        return List.copyOf(limitations);
    }
}
