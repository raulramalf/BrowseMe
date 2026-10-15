package io.github.raulramalf.browseme;

import io.github.raulramalf.browseme.infrastructure.desktop.PortSelector;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BrowseMeApplication {

    public static void main(String[] args) {
        PortSelector.chooseForLocalProfile(port -> System.out.println(
                "AVISO: el puerto " + PortSelector.DEFAULT_PORT + " está ocupado, probablemente por otra copia de "
                        + "BrowseMe que sigue abierta (quizá una versión anterior).\n"
                        + "       Esta copia usará el puerto " + port + ". Cierra la otra para liberar memoria."));
        SpringApplication.run(BrowseMeApplication.class, args);
    }
}
