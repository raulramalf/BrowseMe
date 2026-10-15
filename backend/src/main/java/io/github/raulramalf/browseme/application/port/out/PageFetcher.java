package io.github.raulramalf.browseme.application.port.out;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.url.AnalyzedUrl;

/**
 * Descarga una página de forma segura y devuelve lo extraído de ella. El HTML
 * crudo nunca sale del adaptador.
 *
 * <p>Una respuesta 4xx/5xx de la web analizada no es una excepción: se devuelve
 * un snapshot con ese código de estado para que el informe lo explique.
 */
public interface PageFetcher {

    /** @throws FetchFailedException si no se pudo obtener una respuesta utilizable */
    PageSnapshot fetch(AnalyzedUrl url);
}
