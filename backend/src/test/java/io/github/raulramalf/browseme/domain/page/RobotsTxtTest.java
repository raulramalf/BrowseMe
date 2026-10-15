package io.github.raulramalf.browseme.domain.page;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RobotsTxtTest {

    private static final String ROBOTS = """
            # Comentario
            User-agent: Googlebot
            User-agent: Bingbot
            Disallow: /privado
            Allow: /privado/publico
            Disallow: /*.pdf$
            Disallow: /buscar?*orden=

            User-agent: *
            Disallow: /admin   # fin de línea
            Disallow:
            Sitemap: https://example.com/sitemap.xml
            """;

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource({
        "googlebot, /,                          true",
        "googlebot, /privado,                   false",
        "googlebot, /privado/x,                 false",
        "googlebot, /privado/publico/x,         true",
        "googlebot, /docs/manual.pdf,           false",
        "googlebot, /docs/manual.pdf?v=2,       true",
        "googlebot, /buscar?q=pan&orden=precio, false",
        "googlebot, /buscar?q=pan,              true",
        "googlebot, /admin,                     true",
        "GOOGLEBOT, /privado,                   false",
        "bingbot,   /privado,                   false",
        "otrobot,   /admin/usuarios,            false",
        "otrobot,   /privado,                   true",
        "googlebot, /robots.txt,                true"
    })
    void applies_the_most_specific_rule_of_the_matching_group(String agent, String path, boolean allowed) {
        assertThat(RobotsTxt.parse(ROBOTS).isAllowed(agent, path)).isEqualTo(allowed);
    }

    @Test
    void allow_wins_ties_between_rules_of_equal_length() {
        RobotsTxt robots = RobotsTxt.parse("User-agent: *\nDisallow: /pagina\nAllow: /pagina");

        assertThat(robots.isAllowed("cualquiera", "/pagina")).isTrue();
    }

    @Test
    void everything_is_allowed_without_rules_or_groups() {
        assertThat(RobotsTxt.parse("").isAllowed("googlebot", "/x")).isTrue();
        assertThat(RobotsTxt.parse("Disallow: /\n").isAllowed("googlebot", "/x")).isTrue();
        assertThat(RobotsTxt.parse("User-agent: googlebot\nAllow: /\n\nUser-agent: *\nDisallow: /")
                .isAllowed("googlebot", "/x")).isTrue();
    }

    @Test
    void consecutive_user_agent_lines_share_a_group_even_across_blank_lines() {
        // Como el parser de Google: las líneas en blanco no separan grupos.
        RobotsTxt robots = RobotsTxt.parse("User-agent: googlebot\n\nUser-agent: *\nDisallow: /");

        assertThat(robots.isAllowed("googlebot", "/x")).isFalse();
    }

    @Test
    void knows_whether_an_agent_is_named_explicitly() {
        RobotsTxt robots = RobotsTxt.parse("User-agent: BrowseMeBot\nDisallow: /\n\nUser-agent: *\nAllow: /");

        assertThat(robots.namesAgent("browsemebot")).isTrue();
        assertThat(robots.namesAgent("googlebot")).isFalse();
        assertThat(robots.isAllowed("browsemebot", "/")).isFalse();
    }

    @Test
    @Timeout(2)
    void hostile_wildcard_patterns_do_not_explode() {
        String pattern = "/" + "*a".repeat(200) + "b";
        RobotsTxt robots = RobotsTxt.parse("User-agent: *\nDisallow: " + pattern);
        String path = "/" + "a".repeat(2000);

        long start = System.nanoTime();
        assertThat(robots.isAllowed("googlebot", path)).isTrue();
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
    }
}
