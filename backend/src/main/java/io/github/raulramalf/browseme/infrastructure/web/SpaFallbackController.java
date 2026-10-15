package io.github.raulramalf.browseme.infrastructure.web;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * App de escritorio: las rutas del frontend (React Router) se resuelven en el
 * navegador, así que al recargar /analizar o /analisis/{id} se devuelve
 * index.html. Las rutas con punto (assets) y /api no entran aquí.
 */
@Controller
@Profile("local")
class SpaFallbackController {

    @GetMapping({"/{path:[^.]*}", "/analisis/{id:[^.]*}"})
    String index() {
        return "forward:/index.html";
    }
}
