package io.github.raulramalf.browseme.domain.url;

/** La entrada no es una URL admisible (formato, esquema, puerto, longitud...). */
public class InvalidUrlException extends RuntimeException {

    public InvalidUrlException(String reason) {
        super(reason);
    }
}
