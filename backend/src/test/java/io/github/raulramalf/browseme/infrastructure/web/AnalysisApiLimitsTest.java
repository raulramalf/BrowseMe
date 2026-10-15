package io.github.raulramalf.browseme.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.raulramalf.browseme.application.port.out.PageFetcher;
import io.github.raulramalf.browseme.domain.seo.SnapshotFixture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Rate limit por IP y cola acotada, con límites diminutos para provocarlos. */
@SpringBootTest(properties = {
    "browseme.web.submits-per-minute=4",
    "browseme.executor.threads=1",
    "browseme.executor.queue-capacity=1"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DirtiesContext
class AnalysisApiLimitsTest {

    private final CountDownLatch release = new CountDownLatch(1);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PageFetcher fetcher;

    @AfterEach
    void releaseWorkers() {
        release.countDown();
    }

    private static MockHttpServletRequestBuilder submit(String url, String clientIp) {
        return post("/api/v1/analyses").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"" + url + "\"}")
                .with(request -> {
                    request.setRemoteAddr(clientIp);
                    return request;
                });
    }

    @Test
    void a_full_queue_answers_503_and_the_rate_limit_answers_429() throws Exception {
        when(fetcher.fetch(any())).thenAnswer(invocation -> {
            release.await(10, TimeUnit.SECONDS);
            return SnapshotFixture.perfect().build();
        });

        // 1 en ejecución + 1 en cola; la tercera URL distinta ya no cabe.
        mvc.perform(submit("https://uno.example.com", "203.0.113.1")).andExpect(status().isAccepted());
        Thread.sleep(200);
        mvc.perform(submit("https://dos.example.com", "203.0.113.1")).andExpect(status().isAccepted());
        mvc.perform(submit("https://tres.example.com", "203.0.113.1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.code").value("SERVICE_BUSY"));

        // La misma URL en curso no gasta hueco de cola: devuelve el análisis existente.
        mvc.perform(submit("https://uno.example.com", "203.0.113.1")).andExpect(status().isAccepted());

        // Quinta petición desde la misma IP en el mismo minuto: 429.
        mvc.perform(submit("https://cinco.example.com", "203.0.113.1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

        // Otra IP tiene su propio cupo.
        mvc.perform(submit("https://uno.example.com", "203.0.113.2")).andExpect(status().isAccepted());
    }
}
