package io.github.raulramalf.browseme.domain.score;

import io.github.raulramalf.browseme.domain.seo.RuleArea;
import java.util.Objects;

/**
 * Nota de un área, en décimas (0 a 100 = 0,0 a 10,0).
 *
 * @param findings número de hallazgos del área
 * @param partial  la nota se calculó sin todas sus comprobaciones (p. ej. sin análisis visual)
 */
public record AreaScore(RuleArea area, int tenths, int weightPercent, int findings, boolean partial) {

    public AreaScore {
        Objects.requireNonNull(area, "area");
        if (tenths < 0 || tenths > ScoreCalculator.MAX_TENTHS) {
            throw new IllegalArgumentException("Nota fuera de rango: " + tenths);
        }
    }
}
