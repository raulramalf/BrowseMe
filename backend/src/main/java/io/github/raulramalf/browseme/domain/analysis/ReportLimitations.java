package io.github.raulramalf.browseme.domain.analysis;

import io.github.raulramalf.browseme.domain.page.RobotsTxtStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * Lo que el informe <em>no</em> puede afirmar (apartado 5). Se muestra siempre,
 * más las limitaciones concretas de cada análisis.
 */
public final class ReportLimitations {

    public static final List<String> ALWAYS = List.of(
            "No mide la velocidad real de carga de la página.",
            "No comprueba la usabilidad real en móviles; solo detecta indicios en el código.",
            "No valora la calidad ni la utilidad del contenido para tus clientes.",
            "No conoce tu posicionamiento real en Google ni el de tu competencia.",
            "No ejecuta JavaScript: lo que se genere con él en el navegador no se ha analizado.",
            "Solo analiza esta página, no el resto del sitio.");

    private ReportLimitations() {
    }

    public static List<String> of(WebsiteAnalysis analysis) {
        List<String> limitations = new ArrayList<>(ALWAYS);
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
