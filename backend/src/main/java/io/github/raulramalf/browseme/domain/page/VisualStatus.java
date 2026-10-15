package io.github.raulramalf.browseme.domain.page;

/** Resultado del análisis visual con un navegador real. */
public enum VisualStatus {
    /** Se dibujó la página y hay métricas. */
    DONE,
    /** Desactivado en la configuración. */
    DISABLED,
    /** No hay Chrome, Edge ni Chromium en este equipo. */
    BROWSER_NOT_FOUND,
    /** El navegador no pudo dibujar la página a tiempo. */
    FAILED,
    /** No aplica (página de error) o análisis anterior a esta función. */
    NOT_RUN
}
