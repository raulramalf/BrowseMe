package io.github.raulramalf.browseme.infrastructure.persistence;

import io.github.raulramalf.browseme.application.port.out.AnalysisRepository;
import io.github.raulramalf.browseme.domain.analysis.AnalysisStatus;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio en memoria para el perfil {@code dev} y los tests: sin base de
 * datos. Guarda copias inmutables del estado y aplica el mismo bloqueo
 * optimista que PostgreSQL.
 */
public class InMemoryAnalysisRepository implements AnalysisRepository {

    private final Map<UUID, WebsiteAnalysis.Snapshot> store = new ConcurrentHashMap<>();

    @Override
    public void save(WebsiteAnalysis analysis) {
        WebsiteAnalysis.Snapshot state = analysis.toSnapshot();
        long next = state.version() == null ? 0 : state.version() + 1;
        store.compute(analysis.id(), (id, current) -> {
            Long currentVersion = current == null ? null : current.version();
            if (!Objects.equals(currentVersion, state.version())) {
                throw new ConcurrentModificationException("Versión obsoleta del análisis " + id);
            }
            return withVersion(state, next);
        });
        analysis.assignVersion(next);
    }

    @Override
    public Optional<WebsiteAnalysis> findById(UUID id) {
        return Optional.ofNullable(store.get(id)).map(WebsiteAnalysis::restore);
    }

    @Override
    public List<WebsiteAnalysis> findByStatusIn(Set<AnalysisStatus> statuses) {
        return store.values().stream().filter(s -> statuses.contains(s.status())).map(WebsiteAnalysis::restore).toList();
    }

    @Override
    public void delete(UUID id) {
        store.remove(id);
    }

    @Override
    public int deleteExpired(Instant now) {
        List<UUID> expired = store.values().stream()
                .filter(s -> !now.isBefore(s.expiresAt()))
                .map(WebsiteAnalysis.Snapshot::id)
                .toList();
        expired.forEach(store::remove);
        return expired.size();
    }

    private static WebsiteAnalysis.Snapshot withVersion(WebsiteAnalysis.Snapshot s, long version) {
        return new WebsiteAnalysis.Snapshot(s.id(), s.requestedUrl(), s.finalUrl(), s.status(), s.snapshot(),
                s.issues(), s.ai(), s.aiStatus(), s.errorCode(), s.createdAt(), s.completedAt(), s.expiresAt(),
                version);
    }
}
