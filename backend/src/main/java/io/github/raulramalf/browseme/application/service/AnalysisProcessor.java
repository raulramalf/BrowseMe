package io.github.raulramalf.browseme.application.service;

import io.github.raulramalf.browseme.application.port.out.AiDraft;
import io.github.raulramalf.browseme.application.port.out.AiInterpreter;
import io.github.raulramalf.browseme.application.port.out.AiInvalidOutputException;
import io.github.raulramalf.browseme.application.port.out.AiUnavailableException;
import io.github.raulramalf.browseme.application.port.out.AnalysisRepository;
import io.github.raulramalf.browseme.application.port.out.FetchFailedException;
import io.github.raulramalf.browseme.application.port.out.PageFetcher;
import io.github.raulramalf.browseme.domain.analysis.AiInterpretation;
import io.github.raulramalf.browseme.domain.analysis.AiStatus;
import io.github.raulramalf.browseme.domain.analysis.AnalysisErrorCode;
import io.github.raulramalf.browseme.domain.analysis.AnalysisStatus;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRuleEngine;
import io.github.raulramalf.browseme.domain.url.AnalyzedUrl;
import io.github.raulramalf.browseme.domain.url.InvalidUrlException;
import io.github.raulramalf.browseme.domain.url.UrlPolicy;
import io.github.raulramalf.browseme.domain.url.UrlRejectedException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orquesta un análisis: descarga, reglas, IA y persistencia. Se guarda tras
 * cada transición para que el cliente vea el progreso al sondear.
 *
 * <p>La IA es el último paso y es prescindible: cualquier fallo suyo deja el
 * análisis en {@code COMPLETED_WITHOUT_AI}, nunca en {@code FAILED}.
 */
public final class AnalysisProcessor {

    private static final Logger LOG = System.getLogger(AnalysisProcessor.class.getName());

    private final AnalysisRepository repository;
    private final PageFetcher fetcher;
    private final SeoRuleEngine engine;
    private final AiInterpreter interpreter;
    private final UrlPolicy policy;
    private final Clock clock;
    private final AiInputFactory inputFactory = new AiInputFactory();
    private final AiOutputValidator validator = new AiOutputValidator();

    public AnalysisProcessor(AnalysisRepository repository, PageFetcher fetcher, SeoRuleEngine engine,
                             AiInterpreter interpreter, UrlPolicy policy, Clock clock) {
        this.repository = repository;
        this.fetcher = fetcher;
        this.engine = engine;
        this.interpreter = interpreter;
        this.policy = policy;
        this.clock = clock;
    }

    public void process(UUID id) {
        Optional<WebsiteAnalysis> found = repository.findById(id);
        if (found.isEmpty() || found.get().status() != AnalysisStatus.PENDING) {
            return;
        }
        WebsiteAnalysis analysis = found.get();
        try {
            run(analysis);
        } catch (RuntimeException e) {
            // Los logs no llevan contenido de la página ni la URL: solo id y tipo de error.
            LOG.log(Level.ERROR, "Fallo inesperado en el análisis " + id + ": " + e.getClass().getName());
            if (!analysis.status().isFinal()) {
                analysis.fail(AnalysisErrorCode.INTERNAL_ERROR, clock.instant());
                repository.save(analysis);
            }
        }
    }

    private void run(WebsiteAnalysis analysis) {
        analysis.startFetching();
        repository.save(analysis);

        PageSnapshot snapshot;
        try {
            // La URL se valida otra vez: la del POST fue solo una respuesta rápida.
            AnalyzedUrl url = policy.parse(analysis.requestedUrl().toString());
            snapshot = fetcher.fetch(url);
        } catch (UrlRejectedException | InvalidUrlException e) {
            fail(analysis, AnalysisErrorCode.URL_REJECTED);
            return;
        } catch (FetchFailedException e) {
            fail(analysis, e.code());
            return;
        }

        List<SeoIssue> issues = engine.evaluate(snapshot);
        analysis.startAnalyzing(snapshot);
        if (snapshot.httpStatus() >= 400) {
            analysis.fail(AnalysisErrorCode.HTTP_ERROR, issues, clock.instant());
            repository.save(analysis);
            return;
        }
        repository.save(analysis);

        analysis.startInterpreting(issues);
        repository.save(analysis);

        Set<RuleId> detected = issues.stream().map(SeoIssue::ruleId).collect(Collectors.toSet());
        try {
            AiDraft draft = interpreter.interpret(inputFactory.create(snapshot, issues));
            Optional<AiInterpretation> interpretation = validator.validate(draft, detected);
            if (interpretation.isPresent()) {
                analysis.complete(interpretation.get(), clock.instant());
            } else {
                analysis.completeWithoutAi(AiStatus.INVALID_OUTPUT, clock.instant());
            }
        } catch (AiInvalidOutputException e) {
            analysis.completeWithoutAi(AiStatus.INVALID_OUTPUT, clock.instant());
        } catch (AiUnavailableException e) {
            analysis.completeWithoutAi(AiStatus.UNAVAILABLE, clock.instant());
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "La IA falló en el análisis " + analysis.id() + ": " + e.getClass().getName());
            analysis.completeWithoutAi(AiStatus.UNAVAILABLE, clock.instant());
        }
        repository.save(analysis);
    }

    private void fail(WebsiteAnalysis analysis, AnalysisErrorCode code) {
        analysis.fail(code, clock.instant());
        repository.save(analysis);
    }
}
