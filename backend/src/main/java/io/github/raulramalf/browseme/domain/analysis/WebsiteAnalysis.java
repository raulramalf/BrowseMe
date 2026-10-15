package io.github.raulramalf.browseme.domain.analysis;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.url.AnalyzedUrl;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Agregado raíz: un análisis de una URL y su ciclo de vida.
 *
 * <pre>
 * PENDING -> FETCHING -> ANALYZING -> INTERPRETING -> COMPLETED
 *                                                 \-> COMPLETED_WITHOUT_AI
 * (cualquier estado no final) -> FAILED
 * </pre>
 *
 * Una transición no válida lanza {@link IllegalStateException}: es un error de
 * programación, no del usuario.
 */
public final class WebsiteAnalysis {

    private final UUID id;
    private final URI requestedUrl;
    private final Instant createdAt;
    private final Instant expiresAt;
    private AnalysisStatus status;
    private URI finalUrl;
    private PageSnapshot snapshot;
    private List<SeoIssue> issues;
    private AiInterpretation ai;
    private AiStatus aiStatus;
    private AnalysisErrorCode errorCode;
    private Instant completedAt;
    private Long version;

    private WebsiteAnalysis(UUID id, URI requestedUrl, Instant createdAt, Instant expiresAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.requestedUrl = Objects.requireNonNull(requestedUrl, "requestedUrl");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.issues = List.of();
    }

    public static WebsiteAnalysis create(UUID id, AnalyzedUrl url, Instant now, Duration retention) {
        if (retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("La retención debe ser positiva");
        }
        WebsiteAnalysis analysis = new WebsiteAnalysis(id, url.uri(), now, now.plus(retention));
        analysis.status = AnalysisStatus.PENDING;
        return analysis;
    }

    /** Reconstruye un análisis guardado. Solo para adaptadores de persistencia. */
    public static WebsiteAnalysis restore(Snapshot state) {
        WebsiteAnalysis analysis = new WebsiteAnalysis(state.id(), state.requestedUrl(), state.createdAt(),
                state.expiresAt());
        analysis.status = Objects.requireNonNull(state.status(), "status");
        analysis.finalUrl = state.finalUrl();
        analysis.snapshot = state.snapshot();
        analysis.issues = state.issues() == null ? List.of() : List.copyOf(state.issues());
        analysis.ai = state.ai();
        analysis.aiStatus = state.aiStatus();
        analysis.errorCode = state.errorCode();
        analysis.completedAt = state.completedAt();
        analysis.version = state.version();
        return analysis;
    }

    /** Estado completo para persistir o reconstruir. */
    public record Snapshot(UUID id, URI requestedUrl, URI finalUrl, AnalysisStatus status, PageSnapshot snapshot,
                           List<SeoIssue> issues, AiInterpretation ai, AiStatus aiStatus,
                           AnalysisErrorCode errorCode, Instant createdAt, Instant completedAt, Instant expiresAt,
                           Long version) {
    }

    public Snapshot toSnapshot() {
        return new Snapshot(id, requestedUrl, finalUrl, status, snapshot, issues, ai, aiStatus, errorCode,
                createdAt, completedAt, expiresAt, version);
    }

    public void startFetching() {
        requireStatus(AnalysisStatus.PENDING);
        status = AnalysisStatus.FETCHING;
    }

    public void startAnalyzing(PageSnapshot pageSnapshot) {
        requireStatus(AnalysisStatus.FETCHING);
        this.snapshot = Objects.requireNonNull(pageSnapshot, "pageSnapshot");
        this.finalUrl = pageSnapshot.finalUrl();
        status = AnalysisStatus.ANALYZING;
    }

    public void startInterpreting(List<SeoIssue> detected) {
        requireStatus(AnalysisStatus.ANALYZING);
        this.issues = List.copyOf(detected);
        status = AnalysisStatus.INTERPRETING;
    }

    public void complete(AiInterpretation interpretation, Instant now) {
        requireStatus(AnalysisStatus.INTERPRETING);
        this.ai = Objects.requireNonNull(interpretation, "interpretation");
        this.aiStatus = AiStatus.AVAILABLE;
        finish(AnalysisStatus.COMPLETED, now);
    }

    public void completeWithoutAi(AiStatus reason, Instant now) {
        requireStatus(AnalysisStatus.INTERPRETING);
        if (reason == AiStatus.AVAILABLE) {
            throw new IllegalArgumentException("Sin IA el motivo no puede ser AVAILABLE");
        }
        this.aiStatus = Objects.requireNonNull(reason, "reason");
        finish(AnalysisStatus.COMPLETED_WITHOUT_AI, now);
    }

    /**
     * Falla el análisis. Si ya se descargó la página (por ejemplo una respuesta
     * 4xx/5xx), se conservan el snapshot y los hallazgos para explicar el fallo.
     */
    public void fail(AnalysisErrorCode code, List<SeoIssue> detected, Instant now) {
        if (status.isFinal()) {
            throw new IllegalStateException("El análisis ya terminó en " + status);
        }
        this.errorCode = Objects.requireNonNull(code, "code");
        if (detected != null && !detected.isEmpty()) {
            this.issues = List.copyOf(detected);
        }
        finish(AnalysisStatus.FAILED, now);
    }

    public void fail(AnalysisErrorCode code, Instant now) {
        fail(code, List.of(), now);
    }

    /** La asigna el adaptador de persistencia tras guardar (bloqueo optimista). */
    public void assignVersion(long newVersion) {
        this.version = newVersion;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    private void finish(AnalysisStatus finalStatus, Instant now) {
        status = finalStatus;
        completedAt = Objects.requireNonNull(now, "now");
    }

    private void requireStatus(AnalysisStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Transición no válida desde " + status + " (se esperaba " + expected + ")");
        }
    }

    public UUID id() { return id; }
    public URI requestedUrl() { return requestedUrl; }
    public AnalysisStatus status() { return status; }
    public Optional<URI> finalUrl() { return Optional.ofNullable(finalUrl); }
    public Optional<PageSnapshot> snapshot() { return Optional.ofNullable(snapshot); }
    public List<SeoIssue> issues() { return issues; }
    public Optional<AiInterpretation> ai() { return Optional.ofNullable(ai); }
    public Optional<AiStatus> aiStatus() { return Optional.ofNullable(aiStatus); }
    public Optional<AnalysisErrorCode> errorCode() { return Optional.ofNullable(errorCode); }
    public Instant createdAt() { return createdAt; }
    public Optional<Instant> completedAt() { return Optional.ofNullable(completedAt); }
    public Instant expiresAt() { return expiresAt; }
    public Long version() { return version; }
}
