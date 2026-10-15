package io.github.raulramalf.browseme.infrastructure.ai;

import io.github.raulramalf.browseme.application.port.out.AiDraft;
import io.github.raulramalf.browseme.application.port.out.AiInput;
import io.github.raulramalf.browseme.application.port.out.AiInterpreter;
import io.github.raulramalf.browseme.application.port.out.AiInvalidOutputException;
import io.github.raulramalf.browseme.application.port.out.AiUnavailableException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Intérprete con Ollama vía Spring AI. El modelo no tiene herramientas ni red:
 * solo recibe texto y devuelve JSON con el esquema de {@link AiResponseDto}.
 *
 * <p>El plazo es estricto y propio (en CPU un modelo de 3B tarda decenas de
 * segundos): si se supera, el análisis sigue sin IA.
 */
public class SpringAiOllamaInterpreter implements AiInterpreter {

    private final ChatModel chatModel;
    private final String model;
    private final Duration timeout;
    private final BeanOutputConverter<AiResponseDto> converter = new BeanOutputConverter<>(AiResponseDto.class);
    private final Map<String, Object> jsonSchema;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public SpringAiOllamaInterpreter(ChatModel chatModel, String model, Duration timeout) {
        this.chatModel = chatModel;
        this.model = model;
        this.timeout = timeout;
        this.jsonSchema = JsonMapper.builder().build()
                .readValue(converter.getJsonSchema(), new TypeReference<Map<String, Object>>() { });
    }

    @Override
    public AiDraft interpret(AiInput input) {
        Prompt prompt = new Prompt(
                List.of(new SystemMessage(PromptFactory.SYSTEM), new UserMessage(PromptFactory.user(input))),
                OllamaChatOptions.builder()
                        .model(model)
                        .format(jsonSchema)
                        .temperature(0.2)
                        .numCtx(4096)
                        .numPredict(1024)
                        .disableThinking()
                        .build());

        String text = call(prompt);
        AiResponseDto dto;
        try {
            dto = converter.convert(text);
        } catch (RuntimeException e) {
            throw new AiInvalidOutputException("La respuesta del modelo no es JSON válido", e);
        }
        if (dto == null) {
            throw new AiInvalidOutputException("Respuesta vacía del modelo", null);
        }
        return toDraft(dto);
    }

    private String call(Prompt prompt) {
        CompletableFuture<ChatResponse> future = CompletableFuture.supplyAsync(() -> chatModel.call(prompt), executor);
        ChatResponse response;
        try {
            response = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AiUnavailableException("El modelo no respondió a tiempo", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("Interrumpido esperando al modelo", e);
        } catch (ExecutionException e) {
            throw new AiUnavailableException("No se pudo consultar el modelo", e.getCause());
        }
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null
                || response.getResult().getOutput().getText() == null) {
            throw new AiInvalidOutputException("Respuesta vacía del modelo", null);
        }
        return response.getResult().getOutput().getText();
    }

    private static AiDraft toDraft(AiResponseDto dto) {
        List<AiDraft.Item> items = dto.recommendations() == null ? null : dto.recommendations().stream()
                .map(r -> r == null ? null : new AiDraft.Item(r.text(), r.priority(), r.ruleIds(), r.generalSuggestion()))
                .toList();
        return new AiDraft(dto.summary(), dto.strengths(), dto.opportunities(), items, dto.actionPlan());
    }
}
