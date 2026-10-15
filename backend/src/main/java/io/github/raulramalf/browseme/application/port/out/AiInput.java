package io.github.raulramalf.browseme.application.port.out;

import java.util.List;

/**
 * Lo único que ve el modelo: un resumen acotado de la página y los hallazgos.
 * Nunca el HTML. Los textos de la página van dentro como datos.
 *
 * @param pageSummary resumen en texto plano, ya acotado
 * @param issues      hallazgos con su identificador, severidad y evidencia
 */
public record AiInput(String pageSummary, List<IssueSummary> issues) {

    public record IssueSummary(String ruleId, String category, String severity, String evidence) {
    }

    public AiInput {
        issues = List.copyOf(issues);
    }

    public List<String> ruleIds() {
        return issues.stream().map(IssueSummary::ruleId).toList();
    }
}
