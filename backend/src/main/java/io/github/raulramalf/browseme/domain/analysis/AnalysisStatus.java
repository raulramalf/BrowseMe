package io.github.raulramalf.browseme.domain.analysis;

public enum AnalysisStatus {
    PENDING,
    FETCHING,
    ANALYZING,
    INTERPRETING,
    COMPLETED,
    COMPLETED_WITHOUT_AI,
    FAILED;

    public boolean isFinal() {
        return this == COMPLETED || this == COMPLETED_WITHOUT_AI || this == FAILED;
    }
}
