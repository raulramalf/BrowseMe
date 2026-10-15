package io.github.raulramalf.browseme.infrastructure.web;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
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
 * Perfil {@code local} (app de escritorio): arranca sin PostgreSQL, sirve el
 * frontend con su CSP de página y resuelve las rutas del SPA. La IA se
 * desactiva aquí para no depender de un Ollama en la máquina de test.
 */
@SpringBootTest(properties = {"browseme.ai.mode=disabled", "browseme.desktop.open-browser=false",
        "browseme.visual.mode=disabled"})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalProfileTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PageFetcher fetcher;

    @Test
    void analyses_in_memory_without_database() throws Exception {
        when(fetcher.fetch(any())).thenReturn(SnapshotFixture.perfect().finalUrl("http://example.com/").build());

        String location = mvc.perform(post("/api/v1/analyses").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"http://example.com\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getHeader("Location");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> mvc.perform(get(location))
                .andExpect(jsonPath("$.status").value("COMPLETED_WITHOUT_AI")));
    }

    @Test
    void serves_spa_routes_with_the_page_csp() throws Exception {
        mvc.perform(get("/analizar"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"))
                .andExpect(header().string("Content-Security-Policy", SecurityHeadersFilter.PAGE_CSP));
        mvc.perform(get("/analisis/7c9e6679-7425-40de-944b-e07fc1f90ae7"))
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void keeps_the_strict_csp_on_the_api() throws Exception {
        mvc.perform(get("/api/v1/analyses/00000000-0000-0000-0000-000000000000"))
                .andExpect(header().string("Content-Security-Policy", SecurityHeadersFilter.API_CSP));
    }
}
