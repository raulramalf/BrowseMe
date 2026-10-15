package io.github.raulramalf.browseme.application.service;

import io.github.raulramalf.browseme.application.port.in.AnalysisCapacityExceededException;
import io.github.raulramalf.browseme.application.port.in.SubmitAnalysisUseCase;
import io.github.raulramalf.browseme.application.port.in.SubmittedAnalysis;
import io.github.raulramalf.browseme.application.port.out.AnalysisRepository;
import io.github.raulramalf.browseme.domain.analysis.AnalysisStatus;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import io.github.raulramalf.browseme.domain.url.AnalyzedUrl;
import io.github.raulramalf.browseme.domain.url.UrlPolicy;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Valida la URL (respuesta rápida), guarda el análisis y lo encola.
 *
 * <p>Un análisis por URL en curso: si la misma URL normalizada ya se está
 * analizando, se devuelve ese análisis. El registro vive en memoria porque hay
 * una sola instancia del servicio.
 */
public final class SubmitAnalysisService implements SubmitAnalysisUseCase {

    private final UrlPolicy policy;
    private final AnalysisRepository repository;
    private final AnalysisProcessor processor;
    private final Executor executor;
    private final Clock clock;
    private final Duration retention;
    private final Map<String, UUID> inFlight = new ConcurrentHashMap<>();

    public SubmitAnalysisService(UrlPolicy policy, AnalysisRepository repository, AnalysisProcessor processor,
                                 Executor executor, Clock clock, Duration retention) {
        this.policy = policy;
        this.repository = repository;
        this.processor = processor;
        this.executor = executor;
        this.clock = clock;
        this.retention = retention;
    }

    @Override
    public SubmittedAnalysis submit(String rawUrl) {
        AnalyzedUrl url = policy.parse(rawUrl);
        String key = url.value();
        UUID id = UUID.randomUUID();

        // compute es atómico por clave: dos peticiones simultáneas no pueden ganar las dos.
        UUID owner = inFlight.compute(key, (k, current) -> current != null && isRunning(current) ? current : id);
        if (!owner.equals(id)) {
            AnalysisStatus status = repository.findById(owner).map(WebsiteAnalysis::status)
                    .orElse(AnalysisStatus.PENDING);
            return new SubmittedAnalysis(owner, status);
        }

        WebsiteAnalysis analysis = WebsiteAnalysis.create(id, url, clock.instant(), retention);
        try {
            repository.save(analysis);
        } catch (RuntimeException e) {
            inFlight.remove(key, id);
            throw e;
        }
        try {
            executor.execute(() -> {
                try {
                    processor.process(id);
                } finally {
                    inFlight.remove(key, id);
                }
            });
        } catch (RejectedExecutionException e) {
            inFlight.remove(key, id);
            repository.delete(id);
            throw new AnalysisCapacityExceededException();
        }
        return new SubmittedAnalysis(id, analysis.status());
    }

    /**
     * Una entrada cuyo análisis ya terminó es huérfana y se puede sustituir. Si
     * aún no está guardado, otra petición lo está creando en este momento.
     */
    private boolean isRunning(UUID id) {
        return repository.findById(id).map(analysis -> !analysis.status().isFinal()).orElse(true);
    }
}
