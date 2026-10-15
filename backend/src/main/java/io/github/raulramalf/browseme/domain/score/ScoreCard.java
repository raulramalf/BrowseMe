package io.github.raulramalf.browseme.domain.score;

import java.util.List;

/** Nota global de 0 a 10 (en décimas) y su desglose por áreas. */
public record ScoreCard(int tenths, List<AreaScore> areas) {

    public ScoreCard {
        if (tenths < 0 || tenths > ScoreCalculator.MAX_TENTHS) {
            throw new IllegalArgumentException("Nota fuera de rango: " + tenths);
        }
        areas = List.copyOf(areas);
    }

    public boolean partial() {
        return areas.stream().anyMatch(AreaScore::partial);
    }
}
