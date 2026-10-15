package io.github.raulramalf.browseme.domain.url;

/** La URL es correcta pero apunta a un destino prohibido por la política anti-SSRF. */
public class UrlRejectedException extends RuntimeException {

    public UrlRejectedException(String reason) {
        super(reason);
    }
}
