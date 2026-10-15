package io.github.raulramalf.browseme.infrastructure.web;

import io.github.raulramalf.browseme.application.port.in.GetAnalysisUseCase;
import io.github.raulramalf.browseme.application.port.in.SubmitAnalysisUseCase;
import io.github.raulramalf.browseme.application.port.in.SubmittedAnalysis;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisCreatedResponse;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse;
import io.github.raulramalf.browseme.infrastructure.web.dto.CreateAnalysisRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import io.github.raulramalf.browseme.domain.page.VisualCheck;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analyses")
class AnalysisController {

    private final SubmitAnalysisUseCase submit;
    private final GetAnalysisUseCase get;

    AnalysisController(SubmitAnalysisUseCase submit, GetAnalysisUseCase get) {
        this.submit = submit;
        this.get = get;
    }

    @PostMapping
    ResponseEntity<AnalysisCreatedResponse> create(@Valid @RequestBody CreateAnalysisRequest request) {
        SubmittedAnalysis submitted = submit.submit(request.url());
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/analyses/" + submitted.id()))
                .body(new AnalysisCreatedResponse(submitted.id(), submitted.status().name()));
    }

    @GetMapping("/{id}")
    ResponseEntity<AnalysisResponse> find(@PathVariable String id) {
        UUID uuid = parseId(id);
        return get.get(uuid)
                .map(AnalysisResponseMapper::toResponse)
                .map(body -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body))
                .orElseThrow(AnalysisNotFoundException::new);
    }

    /**
     * Captura JPEG de la primera pantalla (móvil o escritorio) tomada en el
     * análisis visual. Son píxeles de una página no confiable: se sirven como
     * imagen, sin interpretar, con nosniff y la CSP de la API.
     */
    @GetMapping("/{id}/screenshots/{device}")
    ResponseEntity<byte[]> screenshot(@PathVariable String id, @PathVariable String device) {
        UUID uuid = parseId(id);
        VisualCheck visual = get.get(uuid)
                .flatMap(analysis -> analysis.snapshot())
                .map(snapshot -> snapshot.visual())
                .orElseThrow(AnalysisNotFoundException::new);
        byte[] image = switch (device) {
            case "mobile" -> visual.mobileScreenshot();
            case "desktop" -> visual.desktopScreenshot();
            default -> null;
        };
        if (image == null) {
            throw new AnalysisNotFoundException();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG).body(image);
    }

    private static UUID parseId(String raw) {
        try {
            UUID uuid = UUID.fromString(raw);
            // UUID.fromString acepta formas raras ("1-1-1-1-1"); se exige la forma canónica.
            if (!uuid.toString().equalsIgnoreCase(raw)) {
                throw new InvalidIdException();
            }
            return uuid;
        } catch (IllegalArgumentException e) {
            throw new InvalidIdException();
        }
    }

    static final class InvalidIdException extends RuntimeException {
    }

    static final class AnalysisNotFoundException extends RuntimeException {
    }
}
