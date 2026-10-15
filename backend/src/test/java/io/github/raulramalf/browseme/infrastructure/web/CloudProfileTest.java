package io.github.raulramalf.browseme.infrastructure.web;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.raulramalf.browseme.application.port.out.PageFetcher;
import io.github.raulramalf.browseme.domain.seo.SnapshotFixture;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Perfil {@code cloud} (alojamiento gratuito): arranca sin PostgreSQL ni Ollama,
 * analiza sin IA y acepta peticiones CORS solo desde el frontend configurado.
 */
@SpringBootTest(properties = "browseme.web.allowed-origins=https://browseme.example")
@AutoConfigureMockMvc
@ActiveProfiles("cloud")
class CloudProfileTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PageFetcher fetcher;

    @Test
    void analyses_without_database_or_ai() throws Exception {
        when(fetcher.fetch(any())).thenReturn(SnapshotFixture.perfect().finalUrl("http://example.com/").build());

        String location = mvc.perform(post("/api/v1/analyses").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"http://example.com\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getHeader("Location");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> mvc.perform(get(location))
                .andExpect(jsonPath("$.status").value("COMPLETED_WITHOUT_AI")));
        mvc.perform(get(location)).andExpect(jsonPath("$.issues[0].ruleId").value("NOT_HTTPS"));
    }

    @Test
    void allows_cors_only_from_the_configured_frontend() throws Exception {
        mvc.perform(options("/api/v1/analyses")
                        .header("Origin", "https://browseme.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://browseme.example"));

        mvc.perform(options("/api/v1/analyses")
                        .header("Origin", "https://otro.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sends_hsts_because_the_provider_serves_https() throws Exception {
        mvc.perform(get("/api/v1/analyses/00000000-0000-0000-0000-000000000000"))
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age")));
    }
}
