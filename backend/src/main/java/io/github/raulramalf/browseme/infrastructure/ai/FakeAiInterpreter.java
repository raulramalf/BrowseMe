package io.github.raulramalf.browseme.infrastructure.ai;

import io.github.raulramalf.browseme.application.port.out.AiDraft;
import io.github.raulramalf.browseme.application.port.out.AiInput;
import io.github.raulramalf.browseme.application.port.out.AiInterpreter;
import java.util.List;

/**
 * IA simulada para desarrollo y tests: determinista, sin Ollama. Se anuncia
 * como simulada en el propio texto para que nadie la confunda con la real.
 */
public class FakeAiInterpreter implements AiInterpreter {

    @Override
    public AiDraft interpret(AiInput input) {
        List<AiDraft.Item> recommendations = input.issues().stream()
                .limit(8)
                .map(issue -> new AiDraft.Item("[IA simulada] Revisa el hallazgo " + issue.ruleId() + ".",
                        switch (issue.severity()) {
                            case "HIGH" -> "HIGH";
                            case "MEDIUM" -> "MEDIUM";
                            default -> "LOW";
                        },
                        List.of(issue.ruleId()), false))
                .toList();
        return new AiDraft(
                "[IA simulada] Se han detectado " + input.issues().size() + " hallazgos. Este texto lo genera un "
                        + "intérprete de prueba, no un modelo real.",
                List.of("[IA simulada] La página se ha podido descargar y analizar."),
                List.of(),
                recommendations,
                List.of("[IA simulada] Empieza por los hallazgos de severidad alta."));
    }
}
