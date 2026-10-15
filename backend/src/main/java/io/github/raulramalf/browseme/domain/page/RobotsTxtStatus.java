package io.github.raulramalf.browseme.domain.page;

public enum RobotsTxtStatus {
    /** robots.txt existe y permite la URL. */
    ALLOWED,
    /** robots.txt existe y bloquea la URL. */
    BLOCKED,
    /** No hay robots.txt (404): todo está permitido. */
    NOT_FOUND,
    /** No se pudo leer (error, timeout, destino prohibido): no se puede afirmar nada. */
    UNAVAILABLE
}
