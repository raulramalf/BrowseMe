package io.github.raulramalf.browseme.infrastructure.config;

import io.github.raulramalf.browseme.application.service.AnalysisMaintenance;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class MaintenanceJobs {

    private static final Logger LOG = System.getLogger(MaintenanceJobs.class.getName());

    private final AnalysisMaintenance maintenance;

    MaintenanceJobs(AnalysisMaintenance maintenance) {
        this.maintenance = maintenance;
    }

    /** Cada hora se borran los análisis caducados (minimización de datos). */
    @Scheduled(fixedRateString = "PT1H", initialDelayString = "PT1M")
    void purgeExpired() {
        int deleted = maintenance.purgeExpired();
        if (deleted > 0) {
            LOG.log(Level.INFO, "Análisis caducados borrados: " + deleted);
        }
    }

    /** Tras un reinicio, lo que quedó a medias no se va a terminar nunca. */
    @EventListener(ApplicationReadyEvent.class)
    void failInterrupted() {
        int failed = maintenance.failInterrupted();
        if (failed > 0) {
            LOG.log(Level.WARNING, "Análisis interrumpidos por el reinicio: " + failed);
        }
    }
}
