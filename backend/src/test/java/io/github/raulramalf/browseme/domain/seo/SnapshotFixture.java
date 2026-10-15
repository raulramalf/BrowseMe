package io.github.raulramalf.browseme.domain.seo;

import io.github.raulramalf.browseme.domain.page.DesignSignals;
import io.github.raulramalf.browseme.domain.page.EngagementSignals;
import io.github.raulramalf.browseme.domain.page.Heading;
import io.github.raulramalf.browseme.domain.page.ImageStats;
import io.github.raulramalf.browseme.domain.page.LinkStats;
import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.page.RobotsTxtStatus;
import io.github.raulramalf.browseme.domain.page.VisualCheck;
import io.github.raulramalf.browseme.domain.page.VisualMetrics;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * Snapshot «perfecto» que no dispara ninguna regla; cada test cambia solo lo
 * que quiere probar con los métodos {@code with...}.
 */
public final class SnapshotFixture {

    private URI finalUrl = URI.create("https://example.com/");
    private int httpStatus = 200;
    private int redirectCount = 0;
    private String title = "Panadería Ejemplo: pan artesano en Valencia";
    private String metaDescription = "Pan de masa madre hecho cada mañana en el barrio. Encargos, horarios y cómo llegar a la tienda.";
    private String lang = "es";
    private String canonical = "https://example.com/";
    private Set<String> robots = Set.of();
    private List<Heading> headings = List.of(new Heading(1, "Panadería Ejemplo"), new Heading(2, "Horario"));
    private ImageStats images = new ImageStats(2, 2, 0, 0);
    private boolean viewport = true;
    private int words = 400;
    private RobotsTxtStatus robotsTxt = RobotsTxtStatus.ALLOWED;
    private EngagementSignals engagement = goodEngagement();
    private DesignSignals design = new DesignSignals(true, true, 0, 0, 2, 0, 0);
    private VisualCheck visual = VisualCheck.notRun();

    /** Contacto, CTA, suscripción, reseñas, redes, datos del negocio y Open Graph. */
    public static EngagementSignals goodEngagement() {
        return new EngagementSignals(1, 1, 0, 1, 1, 2, Set.of("instagram"), Set.of("Bakery", "AggregateRating"),
                true, true, true);
    }

    /** Métricas visuales sin ningún problema. */
    public static VisualMetrics goodVisual() {
        return new VisualMetrics(390, 390, 40, 0, List.of(), 0, 12, 0, List.of("Inter"), true, true, 3);
    }

    public static SnapshotFixture perfect() {
        return new SnapshotFixture();
    }

    public SnapshotFixture finalUrl(String value) { this.finalUrl = URI.create(value); return this; }
    public SnapshotFixture httpStatus(int value) { this.httpStatus = value; return this; }
    public SnapshotFixture redirects(int value) { this.redirectCount = value; return this; }
    public SnapshotFixture title(String value) { this.title = value; return this; }
    public SnapshotFixture metaDescription(String value) { this.metaDescription = value; return this; }
    public SnapshotFixture lang(String value) { this.lang = value; return this; }
    public SnapshotFixture canonical(String value) { this.canonical = value; return this; }
    public SnapshotFixture robots(String... values) { this.robots = Set.of(values); return this; }
    public SnapshotFixture headings(Heading... values) { this.headings = List.of(values); return this; }
    public SnapshotFixture images(int total, int withAlt, int decorative, int missing) {
        this.images = new ImageStats(total, withAlt, decorative, missing); return this;
    }
    public SnapshotFixture viewport(boolean value) { this.viewport = value; return this; }
    public SnapshotFixture words(int value) { this.words = value; return this; }
    public SnapshotFixture robotsTxt(RobotsTxtStatus value) { this.robotsTxt = value; return this; }
    public SnapshotFixture engagement(EngagementSignals value) { this.engagement = value; return this; }
    public SnapshotFixture design(DesignSignals value) { this.design = value; return this; }
    public SnapshotFixture visual(VisualCheck value) { this.visual = value; return this; }
    public SnapshotFixture visual(VisualMetrics value) { this.visual = VisualCheck.done(value, null, null); return this; }

    public PageSnapshot build() {
        return new PageSnapshot(URI.create("http://example.com/"), finalUrl, httpStatus, redirectCount,
                title, metaDescription, lang, canonical, robots, headings, new LinkStats(5, 2), images,
                viewport, words, robotsTxt, engagement, design, visual);
    }
}
