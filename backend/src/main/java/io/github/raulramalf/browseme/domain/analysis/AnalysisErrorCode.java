package io.github.raulramalf.browseme.domain.analysis;

/** Motivo estable por el que un análisis no pudo completarse. Se expone en la API. */
public enum AnalysisErrorCode {
    /** El destino (o un salto de redirección) resuelve a una dirección prohibida. */
    URL_REJECTED,
    DNS_FAILURE,
    CONNECTION_FAILED,
    TIMEOUT,
    BODY_TOO_LARGE,
    UNSUPPORTED_CONTENT_TYPE,
    TOO_MANY_REDIRECTS,
    /** Redirección sin destino o a una URL no admisible. */
    INVALID_REDIRECT,
    /** La web analizada respondió 4xx o 5xx. */
    HTTP_ERROR,
    /** robots.txt prohíbe expresamente el acceso a BrowseMeBot. */
    ROBOTS_DISALLOWED,
    /** El proceso se reinició con el análisis a medias. */
    INTERRUPTED,
    INTERNAL_ERROR
}
