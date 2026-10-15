package io.github.raulramalf.browseme.application.port.in;

/** No hay hueco para más análisis en este momento. */
public class AnalysisCapacityExceededException extends RuntimeException {

    public AnalysisCapacityExceededException() {
        super("Demasiados análisis en curso");
    }
}
