package io.github.raulramalf.browseme.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import org.junit.jupiter.api.Test;

/** Ida y vuelta agregado -> fila -> agregado sin base de datos. */
class AnalysisJsonMapperTest {

    private final AnalysisJsonMapper mapper = new AnalysisJsonMapper();

    private WebsiteAnalysis roundTrip(WebsiteAnalysis analysis) {
        AnalysisEntity entity = new AnalysisEntity(analysis.id());
        mapper.copyInto(analysis, entity);
        return mapper.toDomain(entity);
    }

    @Test
    void a_completed_analysis_survives_the_round_trip() {
        WebsiteAnalysis original = PersistenceFixtures.completed();

        assertThat(roundTrip(original).toSnapshot()).isEqualTo(original.toSnapshot());
    }

    @Test
    void a_pending_analysis_survives_the_round_trip() {
        WebsiteAnalysis original = PersistenceFixtures.pending();

        assertThat(roundTrip(original).toSnapshot()).isEqualTo(original.toSnapshot());
    }

    @Test
    void never_stores_raw_html_but_does_store_page_text_as_data() {
        AnalysisEntity entity = new AnalysisEntity(PersistenceFixtures.completed().id());
        mapper.copyInto(PersistenceFixtures.completed(), entity);

        assertThat(entity.getSnapshot()).doesNotContain("<html").contains("Panadería");
    }
}
