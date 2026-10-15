package io.github.raulramalf.browseme.domain.analysis;

public enum AiStatus {
    /** Interpretación generada y validada. */
    AVAILABLE,
    /** Ollama caído, timeout o desactivado. */
    UNAVAILABLE,
    /** El modelo respondió algo que no superó la validación. */
    INVALID_OUTPUT
}
