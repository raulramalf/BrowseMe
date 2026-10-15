package io.github.raulramalf.browseme.infrastructure.config;

import io.github.raulramalf.browseme.application.port.in.GetAnalysisUseCase;
import io.github.raulramalf.browseme.application.port.in.SubmitAnalysisUseCase;
import io.github.raulramalf.browseme.application.port.out.AiInterpreter;
import io.github.raulramalf.browseme.application.port.out.AnalysisRepository;
import io.github.raulramalf.browseme.application.port.out.PageFetcher;
import io.github.raulramalf.browseme.application.port.out.PageRenderer;
import io.github.raulramalf.browseme.application.service.AnalysisMaintenance;
import io.github.raulramalf.browseme.application.service.AnalysisProcessor;
import io.github.raulramalf.browseme.application.service.GetAnalysisService;
import io.github.raulramalf.browseme.application.service.SubmitAnalysisService;
import io.github.raulramalf.browseme.domain.seo.SeoRuleEngine;
import io.github.raulramalf.browseme.domain.url.UrlPolicy;
import io.github.raulramalf.browseme.infrastructure.ai.DisabledAiInterpreter;
import io.github.raulramalf.browseme.infrastructure.ai.FakeAiInterpreter;
import io.github.raulramalf.browseme.infrastructure.ai.SpringAiOllamaInterpreter;
import io.github.raulramalf.browseme.infrastructure.fetch.FetchLimits;
import io.github.raulramalf.browseme.infrastructure.fetch.SafeHttpPageFetcher;
import io.github.raulramalf.browseme.infrastructure.fetch.SafeResourceFetcher;
import io.github.raulramalf.browseme.infrastructure.persistence.AnalysisJpaDao;
import io.github.raulramalf.browseme.infrastructure.persistence.InMemoryAnalysisRepository;
import io.github.raulramalf.browseme.infrastructure.persistence.JpaAnalysisRepository;
import io.github.raulramalf.browseme.infrastructure.render.ChromeRenderer;
import java.time.Clock;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * Cableado de la arquitectura hexagonal: aquí, y solo aquí, cada adaptador se
 * conecta a su puerto. El dominio y la aplicación no llevan anotaciones de Spring.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(BrowseMeProperties.class)
public class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    UrlPolicy urlPolicy() {
        return new UrlPolicy();
    }

    @Bean
    SeoRuleEngine seoRuleEngine() {
        return SeoRuleEngine.withDefaultRules();
    }

    @Bean
    PageFetcher pageFetcher(UrlPolicy policy, BrowseMeProperties properties) {
        BrowseMeProperties.Fetch fetch = properties.fetch();
        return new SafeHttpPageFetcher(policy, new FetchLimits(fetch.connectTimeout(), fetch.readTimeout(),
                fetch.totalTimeout(), fetch.robotsTimeout(), fetch.maxBodyBytes(), fetch.maxRedirects()),
                fetch.userAgent());
    }

    @Bean
    PageRenderer pageRenderer(UrlPolicy policy, BrowseMeProperties properties) {
        BrowseMeProperties.Fetch fetch = properties.fetch();
        BrowseMeProperties.Visual visual = properties.visual();
        SafeResourceFetcher resources = new SafeResourceFetcher(policy, new FetchLimits(fetch.connectTimeout(),
                fetch.readTimeout(), fetch.totalTimeout(), fetch.robotsTimeout(), fetch.maxBodyBytes(),
                fetch.maxRedirects()), fetch.userAgent());
        return new ChromeRenderer(new ChromeRenderer.Settings(visual.mode() == BrowseMeProperties.VisualMode.AUTO,
                visual.browserPath(), visual.timeout(), visual.maxRequests(), visual.maxTotalBytes(),
                visual.maxResourceBytes()), resources);
    }

    @Bean
    AiInterpreter aiInterpreter(BrowseMeProperties properties, ObjectProvider<ChatModel> chatModel) {
        BrowseMeProperties.Ai ai = properties.ai();
        return switch (ai.mode()) {
            case OLLAMA -> new SpringAiOllamaInterpreter(chatModel.getObject(), ai.model(), ai.timeout());
            case FAKE -> new FakeAiInterpreter();
            case DISABLED -> new DisabledAiInterpreter();
        };
    }

    @Bean
    AnalysisRepository analysisRepository(BrowseMeProperties properties, ObjectProvider<AnalysisJpaDao> dao) {
        return switch (properties.persistence()) {
            case JPA -> new JpaAnalysisRepository(dao.getObject());
            case MEMORY -> new InMemoryAnalysisRepository();
        };
    }

    /**
     * Ejecutor acotado: N hilos y una cola limitada. Cuando se llena, rechaza
     * (la API responde 503) en lugar de acumular trabajo sin límite.
     */
    @Bean(destroyMethod = "shutdownNow")
    ThreadPoolExecutor analysisExecutor(BrowseMeProperties properties) {
        BrowseMeProperties.Executor config = properties.executor();
        return new ThreadPoolExecutor(config.threads(), config.threads(), 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(config.queueCapacity()), new CustomizableThreadFactory("analysis-"),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean
    AnalysisProcessor analysisProcessor(AnalysisRepository repository, PageFetcher fetcher, PageRenderer renderer,
                                        SeoRuleEngine engine, AiInterpreter interpreter, UrlPolicy policy,
                                        Clock clock) {
        return new AnalysisProcessor(repository, fetcher, renderer, engine, interpreter, policy, clock);
    }

    @Bean
    SubmitAnalysisUseCase submitAnalysisUseCase(UrlPolicy policy, AnalysisRepository repository,
                                                AnalysisProcessor processor, ThreadPoolExecutor analysisExecutor,
                                                Clock clock, BrowseMeProperties properties) {
        return new SubmitAnalysisService(policy, repository, processor, analysisExecutor, clock,
                properties.retention());
    }

    @Bean
    GetAnalysisUseCase getAnalysisUseCase(AnalysisRepository repository, Clock clock) {
        return new GetAnalysisService(repository, clock);
    }

    @Bean
    AnalysisMaintenance analysisMaintenance(AnalysisRepository repository, Clock clock) {
        return new AnalysisMaintenance(repository, clock);
    }
}
