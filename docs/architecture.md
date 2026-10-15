# Arquitectura de BrowseMe

Paquete raíz: `io.github.raulramalf.browseme`. Detalle de tipos en
[contratos.md](contratos.md) y de carpetas en [estructura.md](estructura.md).

## 1. Vista hexagonal

Las dependencias apuntan siempre hacia dentro: la infraestructura conoce la
aplicación y el dominio; el dominio no conoce a nadie. ArchUnit lo verifica.

```mermaid
flowchart LR
    user["Navegador"]

    subgraph frontend["Frontend React servido por nginx"]
        spa["SPA: formulario e informe"]
    end

    subgraph backend["Backend Spring Boot"]
        direction LR

        subgraph inbound["Infraestructura de entrada"]
            controller["AnalysisController"]
            ratelimit["RateLimitFilter"]
            scheduler["ExpiredAnalysisCleaner y StuckAnalysisRecovery"]
        end

        subgraph app["Aplicación"]
            submit["SubmitAnalysisService"]
            get["GetAnalysisService"]
            processor["AnalysisProcessor"]
            validator["AiOutputValidator"]
            portFetcher[["Puerto PageFetcher"]]
            portAi[["Puerto AiInterpreter"]]
            portRepo[["Puerto AnalysisRepository"]]
        end

        subgraph domain["Dominio Java puro"]
            aggregate["WebsiteAnalysis"]
            engine["SeoRuleEngine y reglas"]
            policy["UrlPolicy"]
            snapshot["PageSnapshot y SeoIssue"]
        end

        subgraph outbound["Infraestructura de salida"]
            fetcher["SafeHttpPageFetcher<br/>OkHttp + DNS validado + Jsoup"]
            ai["SpringAiOllamaInterpreter"]
            jpa["JpaAnalysisRepository"]
        end
    end

    web["Web analizada"]
    ollama["Ollama local"]
    db[("PostgreSQL")]

    user --> spa
    spa -- "HTTP /api/v1" --> ratelimit --> controller
    controller --> submit
    controller --> get
    submit -- "Executor acotado" --> processor
    processor --> validator
    submit --> policy
    processor --> engine
    processor --> aggregate
    engine --> snapshot

    processor --> portFetcher
    processor --> portAi
    processor --> portRepo
    get --> portRepo
    scheduler --> portRepo

    fetcher -. "implementa" .-> portFetcher
    ai -. "implementa" .-> portAi
    jpa -. "implementa" .-> portRepo

    fetcher --> policy
    fetcher -- "HTTPS sin cookies" --> web
    ai -- "resumen acotado" --> ollama
    jpa --> db
```

Cosas que el diagrama deja claras:

- **El dominio no tiene flechas hacia fuera.** `UrlPolicy` es lógica pura que
  usan dos actores: el servicio de envío (respuesta rápida al `POST`) y el
  fetcher, que la aplica a cada IP resuelta en cada salto. Esa segunda
  aplicación es la validación que cuenta.
- **El HTML crudo no cruza el puerto.** `SafeHttpPageFetcher` descarga,
  parsea con Jsoup y devuelve solo un `PageSnapshot`.
- **La IA está al final y es prescindible.** Si Ollama falla, el análisis
  termina igual con los hechos de las reglas.
- En los perfiles `dev` y `test` se sustituyen adaptadores (`FakeAiInterpreter`,
  `InMemoryAnalysisRepository`) desde `UseCaseConfig`, sin tocar la aplicación.

## 2. Secuencia de un análisis

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuario
    participant F as Frontend
    participant C as AnalysisController
    participant S as SubmitAnalysisService
    participant R as AnalysisRepository
    participant X as Executor acotado
    participant P as AnalysisProcessor
    participant PF as SafeHttpPageFetcher
    participant W as Web analizada
    participant E as SeoRuleEngine
    participant AI as SpringAiOllamaInterpreter
    participant O as Ollama

    U->>F: Introduce la URL
    F->>F: Validación en cliente
    F->>C: POST /api/v1/analyses
    C->>S: submit url
    S->>S: UrlPolicy valida sintaxis y destinos literales
    alt URL inválida o prohibida
        S-->>C: excepción
        C-->>F: 400 o 422 Problem Details
    else URL aceptada
        S->>R: save PENDING
        S->>X: encola process id
        alt Cola llena
            S-->>C: AnalysisCapacityExceeded
            C-->>F: 503 SERVICE_BUSY
        else Encolado
            C-->>F: 202 con id y Location
        end
    end

    X->>P: process id
    P->>R: save FETCHING
    P->>PF: fetch url
    loop Cada salto, máximo 3 redirecciones
        PF->>PF: Resuelve DNS y aplica UrlPolicy a todas las IPs
        PF->>W: GET a la IP validada con límites de tiempo y tamaño
        W-->>PF: Respuesta
    end
    PF->>W: GET robots.txt con las mismas protecciones
    PF->>PF: Jsoup extrae el PageSnapshot
    PF-->>P: PageSnapshot o FetchFailed

    alt Fallo de descarga o respuesta 4xx o 5xx
        P->>R: save FAILED con código de error
    else Snapshot obtenido
        P->>R: save ANALYZING
        P->>E: evaluate snapshot
        E-->>P: lista de SeoIssue
        P->>R: save INTERPRETING
        P->>AI: interpret resumen acotado
        AI->>O: prompt con datos delimitados
        alt Respuesta válida
            O-->>AI: JSON
            AI-->>P: AiInterpretation
            P->>P: AiOutputValidator descarta ruleIds no detectados
            P->>R: save COMPLETED
        else Caída, timeout o JSON inválido
            AI-->>P: excepción
            P->>R: save COMPLETED_WITHOUT_AI
        end
    end

    loop Sondeo cada pocos segundos hasta estado final
        F->>C: GET /api/v1/analyses/id
        C->>R: findById
        R-->>C: WebsiteAnalysis
        C-->>F: 200 AnalysisResponse
    end
    F-->>U: Informe con hechos, IA separada, limitaciones y fecha
```

## 3. Explicación del flujo

1. **Envío.** El frontend valida la URL y hace `POST`. El backend aplica límite
   por IP, valida la sintaxis con `UrlPolicy` (incluidas IPs literales en
   formatos alternativos), guarda el análisis como `PENDING` y lo encola. La
   respuesta es inmediata: `202` con el `id`. Si la cola está llena, `503`.
2. **Descarga segura.** Un hilo del ejecutor acotado toma el trabajo. El
   fetcher resuelve DNS por su cuenta, rechaza el destino si **cualquiera** de
   las IPs está prohibida y conecta a la IP ya validada (sin segunda
   resolución, contra DNS rebinding). Sigue las redirecciones a mano y repite
   las comprobaciones en cada salto. Corta el cuerpo a 2 MB y el tiempo total a
   20 s. Lee `robots.txt` con las mismas protecciones.
3. **Hechos.** Jsoup convierte el HTML en un `PageSnapshot` sin HTML crudo. El
   motor de reglas, que es puro y determinista, produce los `SeoIssue` con su
   evidencia y limitación.
4. **Interpretación.** A Ollama solo le llega un resumen acotado, etiquetado
   como datos no confiables. La respuesta se valida contra el esquema y contra
   los `ruleId` realmente detectados. Si algo falla, el resultado se guarda
   como `COMPLETED_WITHOUT_AI`, que es un resultado válido, no un error.
5. **Consulta.** El frontend sondea `GET` hasta un estado final y pinta el
   informe, que separa «verificado por reglas» de «interpretación de IA».
6. **Mantenimiento.** Cada hora se borran los análisis caducados (7 días). Al
   arrancar, los que quedaron en un estado intermedio pasan a `FAILED` con
   `INTERRUPTED`.
