package io.github.raulramalf.browseme.application.port.out;

/**
 * Pide a un modelo de IA que interprete hallazgos ya detectados. Devuelve un
 * borrador sin validar: la aplicación decide qué se muestra.
 */
public interface AiInterpreter {

    /**
     * @throws AiUnavailableException     el modelo no responde, está caído o desactivado
     * @throws AiInvalidOutputException   la respuesta no es JSON válido o no sigue el esquema
     */
    AiDraft interpret(AiInput input);
}
