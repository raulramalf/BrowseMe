package io.github.raulramalf.browseme.infrastructure.fetch;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.raulramalf.browseme.domain.page.DesignSignals;
import io.github.raulramalf.browseme.domain.page.EngagementSignals;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SignalsExtractorTest {

    private final SignalsExtractor extractor = new SignalsExtractor();

    private EngagementSignals engagement(String html) {
        return extractor.engagement(Jsoup.parse(html, "https://panaderia.example/"));
    }

    private DesignSignals design(String html) {
        return extractor.design(Jsoup.parse(html, "https://panaderia.example/"));
    }

    @Test
    void detects_contact_channels_and_social_networks() {
        EngagementSignals signals = engagement("""
                <a href="tel:+34600000000">Llámanos</a>
                <a href="mailto:hola@panaderia.example">Escríbenos</a>
                <a href="https://wa.me/34600000000">WhatsApp</a>
                <a href="https://www.instagram.com/panaderia">IG</a>
                <a href="https://es-es.facebook.com/panaderia">FB</a>
                <a href="https://instagram.com.evil.example/">falso</a>
                """);
        assertThat(signals.phoneLinks()).isEqualTo(1);
        assertThat(signals.emailLinks()).isEqualTo(1);
        assertThat(signals.whatsappLinks()).isEqualTo(1);
        assertThat(signals.socialNetworks()).containsExactlyInAnyOrder("instagram", "facebook");
    }

    @Test
    void counts_forms_with_an_email_field() {
        EngagementSignals signals = engagement("""
                <form><input type="email"></form>
                <form><input name="correo_electronico"></form>
                <form><input name="q"><button>Buscar</button></form>
                """);
        assertThat(signals.forms()).isEqualTo(3);
        assertThat(signals.emailForms()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Reserva tu mesa", "PIDE PRESUPUESTO", "Contáctanos", "Book now", "Suscríbete",
            "Llámanos ahora", "Get started"})
    void recognizes_calls_to_action(String text) {
        assertThat(engagement("<a href='/x'>" + text + "</a>").callsToAction()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Nuestra historia", "Inicio", "Comprueba el horario", "Leer más"})
    void ignores_texts_that_are_not_calls_to_action(String text) {
        assertThat(engagement("<a href='/x'>" + text + "</a>").callsToAction()).isZero();
    }

    @Test
    void a_long_linked_paragraph_is_not_a_call_to_action() {
        String paragraph = "Reserva " + "palabra ".repeat(20);
        assertThat(engagement("<a href='/x'>" + paragraph + "</a>").callsToAction()).isZero();
    }

    @Test
    void reads_schema_types_from_json_ld_graphs_and_microdata() {
        EngagementSignals signals = engagement("""
                <script type="application/ld+json">
                  {"@context":"https://schema.org","@graph":[
                    {"@type":"Bakery","aggregateRating":{"@type":"AggregateRating","ratingValue":"4.8"}},
                    {"@type":["WebSite","Thing"]}]}
                </script>
                <script type="application/ld+json">{ roto </script>
                <div itemscope itemtype="https://schema.org/Review"></div>
                """);
        assertThat(signals.schemaTypes()).contains("Bakery", "AggregateRating", "WebSite", "Thing", "Review");
    }

    @Test
    void schema_types_are_bounded_and_sanitized() {
        StringBuilder html = new StringBuilder("<script type='application/ld+json'>[{\"@type\":\"Bad Type!\"},");
        for (int i = 0; i < 100; i++) {
            html.append("{\"@type\":\"Tipo").append(i).append("\"},");
        }
        html.append("{\"@type\":\"<img onerror=alert(1)>\"}]</script>");
        EngagementSignals signals = engagement(html.toString());
        assertThat(signals.schemaTypes()).hasSize(EngagementSignals.MAX_SCHEMA_TYPES)
                .allMatch(type -> type.matches("[A-Za-z0-9]+"));
    }

    @Test
    void detects_testimonials_and_open_graph() {
        EngagementSignals signals = engagement("""
                <meta property="og:title" content="Panadería">
                <meta property="og:image" content="">
                <h2>Opiniones de nuestros clientes</h2>
                """);
        assertThat(signals.testimonials()).isTrue();
        assertThat(signals.openGraphTitle()).isTrue();
        assertThat(signals.openGraphImage()).isFalse();
    }

    @Test
    void structural_design_patterns() {
        DesignSignals signals = design("""
                <link rel="shortcut icon" href="/favicon.png">
                <nav><a href="/">Inicio</a></nav>
                <a href="/a">Clic aquí</a> <a href="/b">Leer más…</a> <a href="/c">La carta</a>
                <a href="/d"></a>
                <a href="/e" aria-label="Instagram"><svg></svg></a>
                <button><img src="x.png" alt="Abrir menú"></button>
                <button><img src="y.png"></button>
                <label for="nombre">Nombre</label><input id="nombre">
                <label>Email <input type="email"></label>
                <input placeholder="Teléfono">
                <input type="hidden" name="token"><input type="submit">
                <select aria-label="Provincia"></select>
                <img src="1.jpg" width="10" height="10"><img src="2.jpg" width="10"><img src="3.jpg">
                """);
        assertThat(signals.navigation()).isTrue();
        assertThat(signals.favicon()).isTrue();
        assertThat(signals.genericLinkTexts()).isEqualTo(2);
        assertThat(signals.emptyInteractive()).isEqualTo(2);
        assertThat(signals.formFields()).isEqualTo(4);
        assertThat(signals.unlabeledFormFields()).isEqualTo(1);
        // Dos sueltas y las dos de los botones.
        assertThat(signals.imagesWithoutDimensions()).isEqualTo(4);
    }

    @Test
    void an_empty_page_has_no_signals() {
        assertThat(engagement("<p>Hola</p>")).isEqualTo(EngagementSignals.none());
        assertThat(design("<p>Hola</p>")).isEqualTo(DesignSignals.none());
    }
}
