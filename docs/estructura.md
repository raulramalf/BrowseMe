# Estructura del monorepo (Fase 2, paso 3)

Paquete raíz: `io.github.raulramalf.browseme`
(ver «Pendiente de confirmar»).

```
browseme/
├── CLAUDE.md
├── README.md
├── LICENSE                          # Apache 2.0
├── .gitignore
├── .env.example
├── docker-compose.yml               # postgres, ollama, backend, frontend
├── .github/workflows/ci.yml         # ./mvnw verify + npm ci/test/build
├── docs/
│   ├── PROMPT_DESARROLLO.md
│   ├── decisiones.md
│   ├── versiones.md
│   ├── estructura.md
│   └── architecture.md              # diagramas Mermaid (paso 5)
├── backend/
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd, .mvn/wrapper/
│   ├── Dockerfile
│   └── src/
│       ├── main/java/io/github/raulramalf/browseme/
│       │   ├── BrowseMeApplication.java
│       │   ├── domain/                       # Java puro (solo JDK)
│       │   │   ├── analysis/                 # WebsiteAnalysis, AnalysisStatus, AiInterpretation,
│       │   │   │                             # AiStatus, Recommendation, AnalysisErrorCode
│       │   │   ├── page/                     # PageSnapshot, Heading, ImageStats, LinkStats, RobotsDirective
│       │   │   ├── seo/                      # SeoIssue, IssueSeverity, IssueCategory, SeoRule, SeoRuleEngine
│       │   │   │   └── rules/                # una clase por regla del catálogo (TitleMissingRule, ...)
│       │   │   └── url/                      # AnalyzedUrl, UrlPolicy, HostNormalizer, ForbiddenAddressPolicy
│       │   ├── application/
│       │   │   ├── port/in/                  # SubmitAnalysisUseCase, GetAnalysisUseCase
│       │   │   ├── port/out/                 # PageFetcher, AiInterpreter, AnalysisRepository
│       │   │   └── service/                  # SubmitAnalysisService, GetAnalysisService,
│       │   │                                 # AnalysisProcessor, AiOutputValidator
│       │   └── infrastructure/
│       │       ├── config/                   # UseCaseConfig (@Bean), ExecutorConfig, BrowseMeProperties
│       │       ├── web/                      # AnalysisController, dto/, ProblemDetailsAdvice,
│       │       │                             # RateLimitFilter, SecurityHeadersConfig, CorsConfig
│       │       ├── fetch/                    # SafeHttpPageFetcher, ValidatingDns, BoundedBodyReader,
│       │       │                             # JsoupSnapshotExtractor, RobotsTxtChecker
│       │       ├── ai/                       # SpringAiOllamaInterpreter, PromptFactory, AiResponseDto,
│       │       │                             # FakeAiInterpreter (perfiles dev/test)
│       │       └── persistence/              # JpaAnalysisRepository, AnalysisEntity, AnalysisJpaDao,
│       │                                     # InMemoryAnalysisRepository, ExpiredAnalysisCleaner,
│       │                                     # StuckAnalysisRecovery
│       ├── main/resources/
│       │   ├── application.yml               # + application-dev.yml, -test.yml, -prod.yml
│       │   └── db/migration/V1__create_analysis.sql
│       └── test/java/io/github/raulramalf/browseme/
│           ├── architecture/HexagonalArchitectureTest.java
│           ├── domain/...                    # tests unitarios de reglas, UrlPolicy, estados
│           ├── application/...               # flujo con puertos falsos
│           └── infrastructure/...            # WireMock (fetcher, SSRF), @WebMvcTest, *IT con Testcontainers
└── frontend/
    ├── package.json, package-lock.json
    ├── vite.config.ts, tsconfig*.json, index.html
    ├── Dockerfile, nginx.conf               # build estático servido por nginx; proxy /api -> backend
    └── src/
        ├── main.tsx, App.tsx                # rutas (react-router 7)
        ├── api/                             # client.ts (fetch + Problem Details), types.ts
        ├── hooks/useAnalysisPolling.ts
        ├── pages/                           # HomePage, AnalysisPage, AboutPage, PrivacyPage,
        │                                    # TermsPage, NotFoundPage
        ├── components/                      # Layout, UrlForm, StatusView, ErrorPanel, SeverityCounts,
        │                                    # IssueFilter, IssueList, IssueCard, AiSection, Limitations
        ├── styles/
        └── test/                            # setup de Vitest; tests junto a cada componente (*.test.tsx)
```

## Decisiones de estructura

1. **Un único módulo Maven con paquetes**, no multi-módulo. Las capas se
   separan por paquete y las protege ArchUnit (`domain` no depende de
   Spring/JPA/Jsoup/OkHttp/Spring AI ni de `application`/`infrastructure`;
   `application` no depende de `infrastructure` ni de frameworks). El
   multi-módulo daría la misma garantía con el compilador, pero triplica los
   pom y el plazo es de una semana. Si el proyecto crece, la división por
   paquetes permite migrar sin reescribir.
2. **Sin puerto `Clock` propio**: se inyecta `java.time.Clock` (JDK, permitido
   en el dominio). Tampoco hay puerto para el ejecutor: `AnalysisProcessor` se
   lanza sobre un `java.util.concurrent.Executor` inyectado, que en
   infraestructura es un `ThreadPoolExecutor` acotado.
3. **Reglas SEO como clases pequeñas** que implementan `SeoRule`
   (`PageSnapshot -> Optional/List<SeoIssue>`); `SeoRuleEngine` las recorre.
   Facilita un test por regla y añadir reglas sin tocar el motor.
4. **Validación de URL en dos niveles**: `UrlPolicy` (dominio, lógica pura:
   esquema, puerto, longitud, credenciales, normalización del host y rangos de
   IP prohibidos con `InetAddress`) y `ValidatingDns` (infraestructura: resuelve,
   aplica `UrlPolicy` a cada IP y entrega a OkHttp solo IPs validadas).
5. **JSON en Postgres con JPA**: `snapshot`, `issues` y `ai_result` como `jsonb`
   mapeados con `@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 7, sin librerías
   extra). La entidad JPA vive en infraestructura y se traduce al agregado.
6. **Frontend servido por nginx** en Docker, con proxy `/api` al backend: un
   solo origen en producción, así que CORS solo hace falta en desarrollo
   (`http://localhost:5173`).

## Paquete raíz

`io.github.raulramalf.browseme` (groupId `io.github.raulramalf`), confirmado el 2026-10-15.
