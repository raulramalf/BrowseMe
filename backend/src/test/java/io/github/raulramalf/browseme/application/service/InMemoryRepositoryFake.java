package io.github.raulramalf.browseme.application.service;

import io.github.raulramalf.browseme.application.port.out.AnalysisRepository;
import io.github.raulramalf.browseme.domain.analysis.AnalysisStatus;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Repositorio falso que guarda copias y registra el historial de estados guardados. */
class InMemoryRepositoryFake implements AnalysisRepository {

    final Map<UUID, WebsiteAnalysis.Snapshot> store = new ConcurrentHashMap<>();
    final List<AnalysisStatus> savedStatuses = new ArrayList<>();

    @Override
    public synchronized void save(WebsiteAnalysis analysis) {
        store.put(analysis.id(), analysis.toSnapshot());
        savedStatuses.add(analysis.status());
    }

    @Override
    public Optional<WebsiteAnalysis> findById(UUID id) {
        return Optional.ofNullable(store.get(id)).map(WebsiteAnalysis::restore);
    }

    @Override
    public List<WebsiteAnalysis> findByStatusIn(Set<AnalysisStatus> statuses) {
        return new ArrayList<>(store.values().stream().filter(s -> statuses.contains(s.status()))
                .map(WebsiteAnalysis::restore).toList());
    }

    @Override
    public void delete(UUID id) {
        store.remove(id);
    }

    @Override
    public int deleteExpired(Instant now) {
        List<UUID> expired = store.values().stream().filter(s -> !now.isBefore(s.expiresAt()))
                .map(WebsiteAnalysis.Snapshot::id).toList();
        expired.forEach(store::remove);
        return expired.size();
    }
}
