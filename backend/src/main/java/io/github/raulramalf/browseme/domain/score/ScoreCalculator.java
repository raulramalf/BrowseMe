package io.github.raulramalf.browseme.domain.score;

import io.github.raulramalf.browseme.domain.page.VisualStatus;
import io.github.raulramalf.browseme.domain.seo.IssueSeverity;
import io.github.raulramalf.browseme.domain.seo.RuleArea;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Valoración de 0 a 10 con una fórmula pública y determinista: la misma página
 * siempre recibe la misma nota, y la IA no interviene.
 *
 * <ol>
 *   <li>Cada área empieza en 10.</li>
 *   <li>Cada hallazgo resta según su severidad: alta 3, media 1,5, baja 0,5,
 *       informativa 0. Un área no baja de 0.</li>
 *   <li>La nota global es la media ponderada: SEO 40 %, captación y
 *       fidelización 30 %, diseño 30 %.</li>
 * </ol>
 *
 * Se calcula en décimas con enteros para evitar errores de redondeo.
 */
public final class ScoreCalculator {

    public static final int MAX_TENTHS = 100;

    /** Penalización por severidad, en décimas. */
    public static final Map<IssueSeverity, Integer> PENALTY_TENTHS = Map.of(
            IssueSeverity.HIGH, 30,
            IssueSeverity.MEDIUM, 15,
            IssueSeverity.LOW, 5,
            IssueSeverity.INFO, 0);

    /** Peso de cada área en la nota global, en porcentaje (suman 100). */
    public static final Map<RuleArea, Integer> WEIGHT_PERCENT = Map.of(
            RuleArea.SEO, 40,
            RuleArea.ENGAGEMENT, 30,
            RuleArea.DESIGN, 30);

    private ScoreCalculator() {
    }

    /**
     * @param visual estado del análisis visual: si no se hizo, el área de diseño
     *               se marca como parcial (solo cuenta la estructura del HTML)
     */
    public static ScoreCard calculate(List<SeoIssue> issues, VisualStatus visual) {
        Map<RuleArea, Integer> penalties = new EnumMap<>(RuleArea.class);
        Map<RuleArea, Integer> findings = new EnumMap<>(RuleArea.class);
        for (SeoIssue issue : issues) {
            penalties.merge(issue.area(), PENALTY_TENTHS.get(issue.severity()), Integer::sum);
            findings.merge(issue.area(), 1, Integer::sum);
        }
        List<AreaScore> areas = new ArrayList<>();
        int weighted = 0;
        for (RuleArea area : RuleArea.values()) {
            int tenths = Math.max(0, MAX_TENTHS - penalties.getOrDefault(area, 0));
            int weight = WEIGHT_PERCENT.get(area);
            boolean partial = area == RuleArea.DESIGN && visual != VisualStatus.DONE;
            areas.add(new AreaScore(area, tenths, weight, findings.getOrDefault(area, 0), partial));
            weighted += tenths * weight;
        }
        // Redondeo a la décima más cercana (todos los valores son no negativos).
        return new ScoreCard((weighted + 50) / 100, areas);
    }
}
