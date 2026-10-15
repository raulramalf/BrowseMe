package io.github.raulramalf.browseme.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Fila de la tabla {@code analysis}. Vive solo en infraestructura: la API y el
 * dominio nunca la ven. Las columnas jsonb guardan JSON ya serializado por
 * {@link AnalysisJsonMapper}.
 */
@Entity
@Table(name = "analysis")
public class AnalysisEntity {

    @Id
    private UUID id;

    @Column(name = "requested_url", nullable = false, length = 2048)
    private String requestedUrl;

    @Column(name = "final_url", length = 2048)
    private String finalUrl;

    @Column(nullable = false, length = 30)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    private String snapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    private String issues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_result")
    private String aiResult;

    @Column(name = "ai_status", length = 20)
    private String aiStatus;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Version
    private Long version;

    protected AnalysisEntity() {
    }

    AnalysisEntity(UUID id) {
        this.id = id;
    }

    UUID getId() { return id; }
    String getRequestedUrl() { return requestedUrl; }
    void setRequestedUrl(String value) { this.requestedUrl = value; }
    String getFinalUrl() { return finalUrl; }
    void setFinalUrl(String value) { this.finalUrl = value; }
    String getStatus() { return status; }
    void setStatus(String value) { this.status = value; }
    String getSnapshot() { return snapshot; }
    void setSnapshot(String value) { this.snapshot = value; }
    String getIssues() { return issues; }
    void setIssues(String value) { this.issues = value; }
    String getAiResult() { return aiResult; }
    void setAiResult(String value) { this.aiResult = value; }
    String getAiStatus() { return aiStatus; }
    void setAiStatus(String value) { this.aiStatus = value; }
    String getErrorCode() { return errorCode; }
    void setErrorCode(String value) { this.errorCode = value; }
    Instant getCreatedAt() { return createdAt; }
    void setCreatedAt(Instant value) { this.createdAt = value; }
    Instant getCompletedAt() { return completedAt; }
    void setCompletedAt(Instant value) { this.completedAt = value; }
    Instant getExpiresAt() { return expiresAt; }
    void setExpiresAt(Instant value) { this.expiresAt = value; }
    Long getVersion() { return version; }
    void setVersion(Long value) { this.version = value; }
}
