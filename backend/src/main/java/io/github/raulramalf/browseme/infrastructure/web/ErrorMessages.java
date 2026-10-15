package io.github.raulramalf.browseme.infrastructure.web;

import io.github.raulramalf.browseme.domain.analysis.AnalysisErrorCode;

/** Mensajes para el usuario, genéricos: sin detalles internos. */
final class ErrorMessages {

    private ErrorMessages() {
    }

    static String of(AnalysisErrorCode code) {
        return switch (code) {
            case URL_REJECTED -> "La dirección apunta a una red privada o reservada y no se puede analizar.";
            case DNS_FAILURE -> "No se encontró el dominio. Revisa que esté bien escrito.";
            case CONNECTION_FAILED -> "No se pudo conectar con la web.";
            case TIMEOUT -> "La web tardó demasiado en responder.";
            case BODY_TOO_LARGE -> "La página es demasiado grande para analizarla (más de 2 MB).";
            case UNSUPPORTED_CONTENT_TYPE -> "La dirección no devuelve una página HTML.";
            case TOO_MANY_REDIRECTS -> "La web redirige demasiadas veces.";
            case INVALID_REDIRECT -> "La web redirige a una dirección que no se puede analizar.";
            case HTTP_ERROR -> "La web respondió con un error.";
            case ROBOTS_DISALLOWED -> "El archivo robots.txt de la web prohíbe el acceso a BrowseMe.";
            case INTERRUPTED -> "El análisis se interrumpió por un reinicio del servicio. Vuelve a intentarlo.";
            case INTERNAL_ERROR -> "Se produjo un error interno. Vuelve a intentarlo más tarde.";
        };
    }
}
