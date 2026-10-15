package io.github.raulramalf.browseme.application.port.out;

public class AiInvalidOutputException extends RuntimeException {

    public AiInvalidOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
