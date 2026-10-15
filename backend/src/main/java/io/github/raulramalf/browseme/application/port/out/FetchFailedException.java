package io.github.raulramalf.browseme.application.port.out;

import io.github.raulramalf.browseme.domain.analysis.AnalysisErrorCode;
import java.util.Objects;

public class FetchFailedException extends RuntimeException {

    private final AnalysisErrorCode code;

    public FetchFailedException(AnalysisErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public FetchFailedException(AnalysisErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
    }

    public AnalysisErrorCode code() {
        return code;
    }
}
