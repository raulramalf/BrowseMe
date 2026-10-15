package io.github.raulramalf.browseme.infrastructure.desktop;

import io.github.raulramalf.browseme.infrastructure.config.BrowseMeProperties;
import java.awt.Desktop;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * App de escritorio: cuando el servidor está listo, indica la dirección y, si se
 * puede, abre el navegador. Si no hay escritorio (servidor, Linux sin soporte),
 * basta con abrir la dirección a mano.
 */
@Component
@Profile("local")
class BrowserLauncher {

    private static final Logger log = LoggerFactory.getLogger(BrowserLauncher.class);

    private final BrowseMeProperties properties;

    BrowserLauncher(BrowseMeProperties properties) {
        this.properties = properties;
    }

    @EventListener
    void onReady(ApplicationReadyEvent event) {
        String port = event.getApplicationContext().getEnvironment().getProperty("local.server.port");
        if (port == null) {
            return; // Sin servidor HTTP real (por ejemplo, en tests).
        }
        URI uri = URI.create("http://127.0.0.1:" + port + "/");
        log.info("BrowseMe está listo. Abre {} en tu navegador. Para salir, cierra esta ventana o pulsa Ctrl+C.", uri);
        if (!properties.desktop().openBrowser()) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            }
        } catch (Exception | LinkageError e) {
            log.info("No se pudo abrir el navegador automáticamente: abre {} a mano.", uri);
        }
    }
}
