package io.github.raulramalf.browseme.infrastructure.ai;

import io.github.raulramalf.browseme.application.port.out.AiDraft;
import io.github.raulramalf.browseme.application.port.out.AiInput;
import io.github.raulramalf.browseme.application.port.out.AiInterpreter;
import io.github.raulramalf.browseme.application.port.out.AiUnavailableException;

/** IA desactivada por configuración: todos los análisis terminan sin IA. */
public class DisabledAiInterpreter implements AiInterpreter {

    @Override
    public AiDraft interpret(AiInput input) {
        throw new AiUnavailableException("La interpretación con IA está desactivada", null);
    }
}
