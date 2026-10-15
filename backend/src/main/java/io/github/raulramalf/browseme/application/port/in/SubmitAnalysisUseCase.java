package io.github.raulramalf.browseme.application.port.in;

/** Inicia un análisis asíncrono. */
public interface SubmitAnalysisUseCase {

    /**
     * @throws io.github.raulramalf.browseme.domain.url.InvalidUrlException  URL no admisible (400)
     * @throws io.github.raulramalf.browseme.domain.url.UrlRejectedException destino prohibido (422)
     * @throws AnalysisCapacityExceededException                               cola llena (503)
     */
    SubmittedAnalysis submit(String rawUrl);
}
