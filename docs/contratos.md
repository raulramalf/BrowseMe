# Contratos de puertos y DTOs (Fase 2, paso 4)

Firmas de diseño, todavía sin implementar. Paquete raíz: `io.github.raulramalf.browseme`
. Los tipos del dominio y de la aplicación son `record`,
`enum` o `sealed` de Java 25 y no llevan anotaciones de frameworks.

## 1. Dominio

### Valores

```java
// domain.url
public record AnalyzedUrl(URI uri) {}                  // ya normalizada y validada sintácticamente
public final class UrlPolicy {
    public AnalyzedUrl parse(String raw);              // lanza InvalidUrlException / UrlRejectedException
    public boolean isForbidden(InetAddress address);   // loopback, RFC 1918, link-local, CGNAT, multicast,
                                                       // no especificada, ULA, IPv4 mapeada en IPv6...
}

// domain.page   (sin HTML crudo; textos truncados a un máximo fijo)
public record PageSnapshot(
    URI requestedUrl, URI finalUrl, int httpStatus, int redirectCount,
    String title, String metaDescription, String lang,
    String canonical,                      // href absoluto tal cual; puede no ser una URI válida
    Set<String> robotsDirectives,          // de <meta name="robots"> y X-Robots-Tag, en minúsculas
    List<Heading> headings,                // Heading(int level, String text), máx. 200
    LinkStats links,                       // LinkStats(int internal, int external)
    ImageStats images,                     // ImageStats(int total, int withAlt, int decorative, int missingAlt)
    boolean viewportPresent, int visibleWordCount,
    RobotsTxtStatus robotsTxt              // ALLOWED, BLOCKED, NOT_FOUND, UNAVAILABLE
) { public boolean https() { ... } }
```

`decorative` cuenta `alt=""` (válido); `missingAlt` cuenta las imágenes sin
atributo `alt`. Así `IMAGES_NO_ALT` no penaliza imágenes decorativas.

### Reglas e issues

```java
// domain.seo
public enum RuleId { TITLE_MISSING, TITLE_LENGTH, META_DESC_MISSING, ..., FETCH_HTTP_ERROR }
public enum IssueCategory { TECHNICAL, EDITORIAL }
public enum IssueSeverity { HIGH, MEDIUM, LOW, INFO }
public record SeoIssue(RuleId ruleId, IssueCategory category, IssueSeverity severity,
                       String evidence, String limitation, String recommendation) {}

public interface SeoRule { List<SeoIssue> evaluate(PageSnapshot snapshot); }
public final class SeoRuleEngine {
    public SeoRuleEngine(List<SeoRule> rules);
    public List<SeoIssue> evaluate(PageSnapshot snapshot);     // función pura
}
```

### Agregado

```java
// domain.analysis
public enum AnalysisStatus { PENDING, FETCHING, ANALYZING, INTERPRETING,
                             COMPLETED, COMPLETED_WITHOUT_AI, FAILED }
public enum AiStatus { AVAILABLE, UNAVAILABLE, INVALID_OUTPUT }
public enum AnalysisErrorCode { URL_REJECTED, DNS_FAILURE, CONNECTION_FAILED, TIMEOUT,
                                BODY_TOO_LARGE, UNSUPPORTED_CONTENT_TYPE, TOO_MANY_REDIRECTS,
                                INVALID_REDIRECT, HTTP_ERROR, ROBOTS_DISALLOWED,
                                INTERRUPTED, INTERNAL_ERROR }

public enum Priority { HIGH, MEDIUM, LOW }
public record Recommendation(String text, Priority priority, Set<RuleId> ruleIds,
                             boolean generalSuggestion) {}
public record AiInterpretation(String summary, List<String> strengths, List<String> opportunities,
                               List<Recommendation> recommendations, List<String> actionPlan) {}

public final class WebsiteAnalysis {
    public static WebsiteAnalysis create(UUID id, AnalyzedUrl url, Instant now, Duration retention);
    public void startFetching();                          // PENDING -> FETCHING
    public void startAnalyzing(PageSnapshot snapshot);    // FETCHING -> ANALYZING
    public void startInterpreting(List<SeoIssue> issues); // ANALYZING -> INTERPRETING
    public void complete(AiInterpretation ai, Instant now);          // INTERPRETING -> COMPLETED
    public void completeWithoutAi(AiStatus reason, Instant now);     // INTERPRETING -> COMPLETED_WITHOUT_AI
    public void fail(AnalysisErrorCode code, Instant now);           // cualquier estado no final -> FAILED
    public boolean isExpired(Instant now);
    // getters; una transición no válida lanza IllegalStateException
}
```

**Respuesta 4xx/5xx de la web analizada** (`FETCH_HTTP_ERROR`): el análisis acaba
en `FAILED` con `errorCode = HTTP_ERROR`. Se guarda un snapshot mínimo (código de
estado y URL final) y un único issue `FETCH_HTTP_ERROR`, para que el informe
explique qué ha pasado. No se llama a la IA.

## 2. Aplicación: puertos de entrada

```java
public interface SubmitAnalysisUseCase {
    SubmittedAnalysis submit(String rawUrl);
    // lanza: InvalidUrlException (400), UrlRejectedException (422),
    //        AnalysisCapacityExceededException (503)
}
public record SubmittedAnalysis(UUID id, AnalysisStatus status) {}

public interface GetAnalysisUseCase {
    Optional<WebsiteAnalysis> get(UUID id);   // vacío si no existe o ha caducado
}
```

`SubmitAnalysisService` aplica `UrlPolicy` (validación rápida y solo
sintáctica), crea el agregado con un UUID v4 (`UUID.randomUUID()`), lo guarda y
envía `AnalysisProcessor.process(id)` al `Executor` acotado. Si la cola está
llena, marca el análisis como `FAILED` y lanza `AnalysisCapacityExceededException`.
**Un análisis por URL en curso**: si la misma URL normalizada ya está en curso,
se devuelve el `id` existente con 202 en vez de lanzar un segundo análisis.
Esto se controla con un conjunto concurrente en memoria, suficiente porque hay
una sola instancia.

## 3. Aplicación: puertos de salida

```java
public interface PageFetcher {
    PageSnapshot fetch(AnalyzedUrl url);
    // Descarga segura (SSRF, límites), robots.txt y extracción con Jsoup.
    // El HTML crudo nunca sale del adaptador.
    // Errores: FetchFailedException(AnalysisErrorCode code)
}

public interface AiInterpreter {
    AiInterpretation interpret(AiInput input);
    // Errores: AiUnavailableException (caído, timeout), AiInvalidOutputException (JSON roto o fuera de esquema)
}
public record AiInput(String pageSummary, List<SeoIssue> issues) {}
// Lo construye AiInputFactory en la aplicación: resumen acotado (título,
// metadescripción y H1 truncados, cifras clave, máx. ~4 000 caracteres), nunca la página.

public interface AnalysisRepository {
    void save(WebsiteAnalysis analysis);                    // bloqueo optimista por versión
    Optional<WebsiteAnalysis> findById(UUID id);
    List<WebsiteAnalysis> findByStatusIn(Set<AnalysisStatus> statuses); // arranque: atascados -> FAILED
    int deleteExpired(Instant now);
}
```

Más `java.time.Clock` y `java.util.concurrent.Executor` inyectados (JDK, no son
puertos propios).

### Orquestación (`AnalysisProcessor.process(UUID id)`)

1. `startFetching` → `PageFetcher.fetch`. Si falla: `fail(code)`.
2. `startAnalyzing(snapshot)` → `SeoRuleEngine.evaluate`.
3. `startInterpreting(issues)` → `AiInterpreter.interpret(AiInputFactory.from(...))`.
4. `AiOutputValidator`: descarta recomendaciones cuyos `ruleIds` no estén entre
   los detectados, salvo que vengan marcadas como `generalSuggestion` y sin
   `ruleIds`. También recorta longitudes y número de elementos.
5. Si sale bien, `complete(ai)`. Si la IA no está disponible o su salida no es
   válida, `completeWithoutAi(UNAVAILABLE | INVALID_OUTPUT)`.
6. Se guarda tras cada transición, para que el cliente vea el progreso.

## 4. Infraestructura web: DTOs

```java
public record CreateAnalysisRequest(@NotBlank @Size(max = 2048) String url) {}
public record AnalysisCreatedResponse(UUID id, String status) {}     // 202 + Location

public record AnalysisResponse(
    UUID id, String status, String requestedUrl, String finalUrl,
    Instant analyzedAt, Instant expiresAt,
    PageSummaryDto pageSummary,              // null hasta ANALYZING
    List<IssueDto> issues,
    Map<String, Integer> severityCounts,     // {"HIGH":1,"MEDIUM":2,"LOW":0,"INFO":3}
    AiDto ai,                                // null si todavía no hay interpretación
    List<String> limitations,                // siempre presente
    ErrorDto error                           // null salvo FAILED
) {}

public record PageSummaryDto(int httpStatus, int redirectCount, boolean https,
    String title, String metaDescription, String lang, String canonical,
    List<String> robotsDirectives, Map<String, Integer> headingCounts,  // {"h1":1,...}
    int internalLinks, int externalLinks,
    ImagesDto images, boolean viewportPresent, int visibleWordCount, String robotsTxt) {}
public record ImagesDto(int total, int withAlt, int decorative, int missingAlt) {}

public record IssueDto(String ruleId, String category, String severity,
    String title, String evidence, String limitation, String recommendation) {}

public record AiDto(String status,           // AVAILABLE | UNAVAILABLE | INVALID_OUTPUT
    String summary, List<String> strengths, List<String> opportunities,
    List<RecommendationDto> recommendations, List<String> actionPlan) {}
public record RecommendationDto(String text, String priority, List<String> ruleIds,
                                boolean generalSuggestion) {}

public record ErrorDto(String code, String message) {}   // mensaje genérico en español
```

`IssueDto.title` es el nombre legible en español de la regla. Lo genera el
backend a partir de `RuleId`, no la IA.

`limitations` = lista fija de lo que no se puede afirmar (velocidad real,
usabilidad móvil real, calidad del contenido, posicionamiento, contenido
renderizado por JavaScript, estado de otras páginas) + limitaciones de este
análisis (por ejemplo «no se pudo leer robots.txt»).

## 5. Errores (RFC 9457)

`ProblemDetail` de Spring con la propiedad extra `code`, y `Content-Type: application/problem+json`.

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `INVALID_URL` | URL mal formada, esquema/puerto no admitido, demasiado larga |
| 400 | `INVALID_ID` | id que no es un UUID |
| 400 | `INVALID_REQUEST` | cuerpo ilegible o sin `url` |
| 404 | `ANALYSIS_NOT_FOUND` | no existe o ha caducado |
| 422 | `URL_REJECTED` | destino prohibido por la política de seguridad |
| 429 | `RATE_LIMITED` | límite por IP (con cabecera `Retry-After`) |
| 503 | `SERVICE_BUSY` | cola llena |
| 500 | `INTERNAL_ERROR` | cualquier otro error, sin detalles internos |

## 6. Esquema de salida exigido a la IA

```json
{
  "summary": "string (≤ 600)",
  "strengths": ["string (≤ 200)", "... máx. 5"],
  "opportunities": ["string (≤ 200)", "... máx. 5"],
  "recommendations": [
    { "text": "string (≤ 300)", "priority": "HIGH|MEDIUM|LOW",
      "ruleIds": ["TITLE_MISSING"], "generalSuggestion": false }
  ],
  "actionPlan": ["string (≤ 200)", "... máx. 5"]
}
```

Se pide con el formato estructurado de Spring AI (`BeanOutputConverter`, que
Ollama recibe como JSON Schema). Aun así, el adaptador valida el resultado y
la aplicación vuelve a comprobar los `ruleIds`: la salida del modelo nunca se
da por buena sin validar.
