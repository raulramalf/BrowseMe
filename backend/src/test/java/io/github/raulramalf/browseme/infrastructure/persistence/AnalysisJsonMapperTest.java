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

    @Test
    void keeps_signals_visual_metrics_and_screenshots() {
        WebsiteAnalysis restored = roundTrip(PersistenceFixtures.completed());
        var snapshot = restored.snapshot().orElseThrow();

        assertThat(snapshot.engagement().socialNetworks()).containsExactly("instagram");
        assertThat(snapshot.visual().done()).isTrue();
        assertThat(snapshot.visual().metrics().fontFamilies()).containsExactly("Inter");
        assertThat(snapshot.visual().mobileScreenshot()).containsExactly((byte) 0xFF, (byte) 0xD8, 1);
    }

    @Test
    void analyses_saved_before_the_new_signals_still_load() {
        AnalysisEntity entity = new AnalysisEntity(PersistenceFixtures.completed().id());
        mapper.copyInto(PersistenceFixtures.completed(), entity);
        String old = entity.getSnapshot().replaceAll(",\"engagement\":\\{.*$", "}");
        entity.setSnapshot(old);

        var snapshot = mapper.toDomain(entity).snapshot().orElseThrow();
        assertThat(snapshot.visual().status().name()).isEqualTo("NOT_RUN");
        assertThat(snapshot.engagement().callsToAction()).isZero();
    }
}
