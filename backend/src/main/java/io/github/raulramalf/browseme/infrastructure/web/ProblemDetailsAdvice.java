package io.github.raulramalf.browseme.infrastructure.web;

import io.github.raulramalf.browseme.application.port.in.AnalysisCapacityExceededException;
import io.github.raulramalf.browseme.domain.url.InvalidUrlException;
import io.github.raulramalf.browseme.domain.url.UrlRejectedException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Errores en formato RFC 9457 con un {@code code} propio y estable. Los
 * mensajes son genéricos: nunca trazas, clases ni detalles internos.
 */
@RestControllerAdvice
class ProblemDetailsAdvice {

    private static final Logger LOG = System.getLogger(ProblemDetailsAdvice.class.getName());

    static ResponseEntity<ProblemDetail> problem(HttpStatusCode status, String code, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:browseme:problem:" + code.toLowerCase().replace('_', '-')));
        problem.setTitle(title);
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(InvalidUrlException.class)
    ResponseEntity<ProblemDetail> invalidUrl(InvalidUrlException e) {
        // El motivo de InvalidUrlException es un texto fijo escrito en UrlPolicy, sin datos del usuario.
        return problem(HttpStatus.BAD_REQUEST, "INVALID_URL", "URL no válida", e.getMessage());
    }

    @ExceptionHandler(UrlRejectedException.class)
    ResponseEntity<ProblemDetail> rejected(UrlRejectedException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "URL_REJECTED", "Destino no permitido", e.getMessage());
    }

    @ExceptionHandler(AnalysisCapacityExceededException.class)
    ResponseEntity<ProblemDetail> busy() {
        ResponseEntity<ProblemDetail> response = problem(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_BUSY",
                "Servicio saturado", "Hay demasiados análisis en curso. Vuelve a intentarlo en un minuto.");
        return ResponseEntity.status(response.getStatusCode()).header("Retry-After", "60").body(response.getBody());
    }

    @ExceptionHandler(AnalysisController.InvalidIdException.class)
    ResponseEntity<ProblemDetail> invalidId() {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_ID", "Identificador no válido",
                "El identificador del análisis no es válido.");
    }

    @ExceptionHandler(AnalysisController.AnalysisNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound() {
        return problem(HttpStatus.NOT_FOUND, "ANALYSIS_NOT_FOUND", "Análisis no encontrado",
                "El análisis no existe o ha caducado.");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ProblemDetail> invalidRequest() {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Petición no válida",
                "El cuerpo de la petición debe ser JSON con un campo \"url\" de hasta 2048 caracteres.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> noResource() {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "No encontrado", "El recurso solicitado no existe.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> method() {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Método no permitido",
                "El método HTTP no está permitido en este recurso.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> mediaType() {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Tipo de contenido no admitido",
                "Envía la petición como application/json.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception e) {
        LOG.log(Level.ERROR, "Error no controlado en la API: " + e.getClass().getName());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Error interno",
                "Se produjo un error interno. Vuelve a intentarlo más tarde.");
    }
}
