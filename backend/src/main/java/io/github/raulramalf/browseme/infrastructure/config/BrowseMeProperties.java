package io.github.raulramalf.browseme.infrastructure.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración propia ({@code browseme.*}). Todo se puede cambiar por variable
 * de entorno, por ejemplo {@code BROWSEME_AI_MODEL=qwen2.5:3b}.
 */
@ConfigurationProperties("browseme")
public record BrowseMeProperties(
        @DefaultValue("P7D") Duration retention,
        @DefaultValue("jpa") Persistence persistence,
        @DefaultValue Executor executor,
        @DefaultValue Fetch fetch,
        @DefaultValue Ai ai,
        @DefaultValue Web web) {

    public enum Persistence { JPA, MEMORY }

    public enum AiMode { OLLAMA, FAKE, DISABLED }

    /** Pool acotado: pocos hilos porque la IA en CPU es el cuello de botella. */
    public record Executor(@DefaultValue("2") int threads, @DefaultValue("20") int queueCapacity) {
    }

    public record Fetch(
            @DefaultValue("BrowseMeBot/0.1 (+[COMPLETAR: URL informativa])") String userAgent,
            @DefaultValue("5s") Duration connectTimeout,
            @DefaultValue("10s") Duration readTimeout,
            @DefaultValue("20s") Duration totalTimeout,
            @DefaultValue("5s") Duration robotsTimeout,
            @DefaultValue("2097152") int maxBodyBytes,
            @DefaultValue("3") int maxRedirects) {
    }

    public record Ai(
            @DefaultValue("ollama") AiMode mode,
            @DefaultValue("llama3.2:3b") String model,
            @DefaultValue("120s") Duration timeout) {
    }

    public record Web(
            @DefaultValue("http://localhost:5173") List<String> allowedOrigins,
            @DefaultValue("5") int submitsPerMinute,
            @DefaultValue("120") int readsPerMinute,
            @DefaultValue("false") boolean hsts) {
    }
}
