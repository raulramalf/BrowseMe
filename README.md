# BrowseMe

Análisis SEO de una página web, explicado en lenguaje sencillo para pequeñas
empresas, autónomos y profesionales.

El usuario introduce la URL pública de su negocio. BrowseMe descarga la página
de forma segura, ejecuta **comprobaciones SEO deterministas** y usa un **modelo
de IA local** (Ollama) solo para explicar y priorizar lo que ya han detectado
las reglas. El informe separa siempre lo **verificado por reglas** de la
**interpretación de IA**, y declara lo que no se puede afirmar.

> Proyecto open source (Apache 2.0) y pieza de portfolio: arquitectura
> hexagonal, seguridad anti-SSRF, validación de la salida de IA y pruebas.

## Autoría y uso de IA

**Autor:** Raúl Ramírez Alfaro ([@raulramalf](https://github.com/raulramalf)),
creador y responsable de este repositorio.

**Uso de IA en el desarrollo:** se han utilizado herramientas de IA únicamente
como **herramienta de apoyo** (por ejemplo, sugerencias de código, revisión,
documentación o borradores). No han sido el único recurso: las decisiones de
diseño y arquitectura, la revisión y la responsabilidad del resultado son del
autor. No debe confundirse con la IA local (Ollama) que usa la aplicación: esa
es una funcionalidad del producto, descrita más abajo, que solo interpreta
hallazgos ya detectados por reglas deterministas.

## Qué hace (y qué no)

| Analiza | No analiza |
|---|---|
| **SEO**: título, metadescripción, idioma, encabezados, HTTPS, redirecciones, `noindex`, canonical, robots.txt, imágenes sin `alt`, viewport, texto visible | Velocidad real de carga |
| **Captación y fidelización**: contacto (teléfono, email, WhatsApp, formularios), llamadas a la acción, suscripción por email, opiniones, redes sociales, datos del negocio (schema.org), vista previa al compartir | Visitas, ventas ni clientes que repiten (solo señales en la página) |
| **Diseño**: menú, enlaces con texto genérico, botones sin nombre, formularios sin etiqueta, imágenes sin tamaño, favicon; y, con Chrome/Edge/Chromium instalado, la página dibujada: desbordes en móvil, contraste, tamaño de botones y texto, tipografías | Calidad del contenido ni posicionamiento en Google |
| **Nota de 0 a 10** con fórmula pública y desglose por área | Otras páginas del sitio |

**Nota 0-10.** La calcula el código (`ScoreCalculator`), nunca la IA, así que la
misma página siempre recibe la misma nota. Cada área (SEO 40 %, captación y
fidelización 30 %, diseño 30 %) empieza en 10 y cada hallazgo resta según su
severidad (alta 3, media 1,5, baja 0,5, informativa 0); la nota final es la
media ponderada. El informe muestra el desglose y el método. Si no hubo
análisis visual, la parte de diseño solo cuenta el HTML y se marca como parcial.

Cada hallazgo lleva su evidencia y su limitación. La IA (Ollama local) explica y
prioriza lo detectado, incluida la mejora de captación y de diseño, pero no
detecta nada ni cambia la nota. Catálogo de reglas en
[docs/PROMPT_DESARROLLO.md](docs/PROMPT_DESARROLLO.md#5-catálogo-de-reglas-seo)
(37 reglas: 18 de SEO, 7 de captación, 6 de diseño del HTML y 6 de diseño visual).

## Arquitectura

Monorepo con `backend/` (Java 25, Spring Boot 4.1, Spring AI 2.0) y `frontend/`
(React 19, TypeScript, Vite 8). El backend sigue una arquitectura hexagonal:

- **domain**: Java puro (análisis, snapshot de la página, reglas SEO, política de URLs).
- **application**: casos de uso y puertos (`PageFetcher`, `AiInterpreter`, `AnalysisRepository`).
- **infrastructure**: adaptadores (OkHttp + Jsoup, Spring AI + Ollama, JPA + PostgreSQL, REST).

Un test de ArchUnit falla si el dominio importa Spring, JPA, Jsoup, OkHttp,
Spring AI o Jackson. Diagramas y flujo en [docs/architecture.md](docs/architecture.md).

## Puesta en marcha con Docker Compose

Requisitos: Docker con Compose v2 y unos 6 GB de RAM libres (el modelo
`llama3.2:3b` ocupa unos 2-3 GB en memoria).

```bash
cp .env.example .env
```

Edita `.env` y cambia `POSTGRES_PASSWORD`. Después:

```bash
docker compose up --build
```

Abre <http://localhost:8080>. La primera vez, el servicio `ollama-pull`
descarga el modelo (unos 2 GB); hasta que termine, los informes salen sin IA.

**Con poca RAM**: pon `BROWSEME_AI_MODE=disabled` en `.env` y arranca sin Ollama:

```bash
docker compose up --build postgres backend frontend
```

## App de escritorio (descargable)

BrowseMe se distribuye como una app para Windows, macOS (Apple Silicon) y Linux
que incluye Java, el backend y el frontend. Se ejecuta en el ordenador de cada
persona con el perfil `local`:

- Escucha solo en `http://127.0.0.1:8765` (cambia el puerto con `BROWSEME_PORT`)
  y abre el navegador al arrancar (`BROWSEME_OPEN_BROWSER=false` para evitarlo).
  Si el puerto está ocupado (normalmente por otra copia que sigue abierta, quizá
  una versión anterior) avisa y usa el siguiente libre (8766…), en lugar de fallar.
- La versión se ve en el pie de la web de la app; las páginas se sirven con
  `Cache-Control: no-cache` para que una actualización no muestre la pantalla vieja
  desde la caché del navegador.
- Informes en memoria mientras la app está abierta; sin PostgreSQL.
- IA con el Ollama local (`ollama pull llama3.2:3b`). Si Ollama no está, el
  informe sale con los hallazgos verificados y sin interpretación.
- Misma protección SSRF y mismas reglas que el resto de despliegues.

Los ejecutables los genera [.github/workflows/release.yml](.github/workflows/release.yml)
con `jpackage`:

- **Publicar una versión**: cambia el número del archivo [VERSION](VERSION)
  (p. ej. a `1.1.0`) y súbelo a `main`. El workflow crea la etiqueta `v1.1.0` y
  la release de GitHub con `BrowseMe-windows.zip`, `BrowseMe-macos.zip` y
  `BrowseMe-linux.zip`. También funciona subiendo una etiqueta `vX.Y.Z`.
- **Probar sin publicar**: ejecuta el workflow *Release* a mano en la pestaña
  *Actions*; los zips quedan como artefactos.

La app no está firmada digitalmente: Windows (SmartScreen) y macOS (Gatekeeper)
avisan la primera vez.

## Web informativa (Vercel)

La web pública solo informa y enlaza a la descarga; no tiene backend. En Vercel:
*Root Directory* `frontend`. [frontend/vercel.json](frontend/vercel.json) ya fija
el comando de build (`npm run build:site`, que compila con `VITE_PUBLIC_SITE=true`)
y la reescritura de rutas del SPA.

## Desarrollo local

Requisitos: JDK 25 y Node 24. No necesitas PostgreSQL ni Ollama: el perfil
`dev` usa un repositorio en memoria y una IA simulada (marcada como tal).

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

```bash
cd frontend && npm ci && npm run dev
```

Abre <http://localhost:5173>. Vite reenvía `/api` al backend en el puerto 8080.
Para probar con un Ollama local en modo `dev`: `BROWSEME_AI_MODE=ollama`.

## Pruebas

```bash
cd backend && ./mvnw verify
```

```bash
cd frontend && npm test && npm run build
```

- **Backend**: reglas con casos límite, `UrlPolicy` y batería SSRF (`127.1`,
  `0x7f000001`, `[::ffff:127.0.0.1]`, `169.254.169.254`, dominios que resuelven a
  IP privada, redirección a IP privada en el segundo salto…), fetcher contra
  WireMock (límites, timeouts, bomba gzip), IA con un `ChatModel` falso (JSON
  roto, `ruleId` inventado, Ollama caído, timeout), API completa y ArchUnit.
- **Integración con PostgreSQL** (`*IT`, Testcontainers): necesita Docker; sin
  él se omiten. En CI se ejecutan.
- **Frontend**: formulario, estados del informe (cargando, error, parcial,
  completo) y render inerte de texto hostil.

## Configuración

| Variable | Por defecto | Descripción |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/browseme` | Conexión a PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `browseme` / vacío | Credenciales de PostgreSQL |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | URL de Ollama |
| `BROWSEME_AI_MODE` | `ollama` (`fake` en `dev`) | `ollama`, `fake` o `disabled` |
| `BROWSEME_AI_MODEL` | `llama3.2:3b` | Modelo de Ollama |
| `BROWSEME_AI_TIMEOUT` | `120s` | Plazo máximo de la IA; si se supera, informe sin IA |
| `BROWSEME_ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes CORS permitidos |
| `BROWSEME_HSTS` | `false` | Envía `Strict-Transport-Security` (solo tras HTTPS) |
| `BROWSEME_RETENTION` | `P7D` | Tiempo que se conservan los análisis |
| `BROWSEME_EXECUTOR_THREADS` / `_QUEUE_CAPACITY` | `2` / `20` | Análisis simultáneos y cola |
| `BROWSEME_WEB_SUBMITS_PER_MINUTE` | `5` | Análisis nuevos por IP y minuto |
| `BROWSEME_WEB_READS_PER_MINUTE` | `120` | Consultas por IP y minuto |
| `BROWSEME_VISUAL_MODE` | `auto` (`disabled` en `dev`) | `auto`: dibuja la página con Chrome, Edge o Chromium si están instalados; `disabled`: sin análisis visual |
| `BROWSEME_BROWSER_PATH` | vacío | Ruta al navegador; vacío = buscarlo en las rutas habituales |
| `BROWSEME_NO_SANDBOX` | no definida | `true` desactiva el sandbox del navegador (solo para CI con páginas propias) |
| `BROWSEME_PORT` | `8765` | Puerto de la app de escritorio (perfil `local`) |
| `BROWSEME_OPEN_BROWSER` | `true` en `local` | Abre el navegador al arrancar la app de escritorio |
| `VITE_PUBLIC_SITE` (frontend, al compilar) | vacío | `true` = web informativa sin analizador, con descarga |
| `BROWSEME_FETCH_USER_AGENT` | `BrowseMeBot/0.1 (+[COMPLETAR: URL informativa])` | User-Agent del rastreador |

En Docker Compose, las de PostgreSQL e IA se toman de `.env` (ver
[.env.example](.env.example)).

## API

| Método | Ruta | Respuesta |
|---|---|---|
| `POST` | `/api/v1/analyses` con `{"url": "..."}` | `202` + `Location`; `400`, `422`, `429`, `503` |
| `GET` | `/api/v1/analyses/{id}` | `200` con el informe; `400`, `404`, `429` |

Errores en formato RFC 9457 (Problem Details) con un `code` estable
(`INVALID_URL`, `URL_REJECTED`, `RATE_LIMITED`, `SERVICE_BUSY`,
`ANALYSIS_NOT_FOUND`…). Contratos en [docs/contratos.md](docs/contratos.md).

## Seguridad

El servidor descarga URLs de terceros, así que el riesgo principal es SSRF. Se
normaliza el host, se valida **cada IP resuelta** y se conecta a esa misma IP,
las redirecciones se siguen a mano revalidando cada salto, y la descarga tiene
límites de tiempo y tamaño. El contenido de las páginas es dato no confiable:
nunca se interpreta como instrucciones para la IA ni se pinta como HTML.

Detalles, medidas y **riesgos que no se eliminan del todo** en
[docs/seguridad.md](docs/seguridad.md).

## Revisión de dependencias

```bash
cd frontend && npm audit
```

```bash
cd backend && ./mvnw org.owasp:dependency-check-maven:check
```

El segundo descarga la base de datos NVD la primera vez (tarda y conviene
configurar una API key de NVD); no se ejecuta en CI.

## Limitaciones conocidas

- Una sola página, sin ejecutar JavaScript.
- Interpretación de robots.txt simplificada (RFC 9309, evaluada para Googlebot).
- Rate limit en memoria y por instancia.
- Un modelo pequeño en CPU es lento y puede equivocarse; su salida se valida,
  pero se presenta siempre como interpretación no verificada.
- Los textos legales son plantillas con marcadores `[COMPLETAR: ...]` y **no son
  asesoramiento jurídico**.

## Contribuir

1. Abre una issue describiendo el cambio.
2. Trabaja en una rama, con tests para lo que añadas.
3. Antes del PR: `./mvnw verify` en `backend/` y `npm test && npm run build` en `frontend/`.
4. Respeta las reglas de [CLAUDE.md](CLAUDE.md): el dominio sin frameworks, los
   hechos los producen reglas y no la IA, y el frontend no usa `dangerouslySetInnerHTML`.

## Licencia

[Apache 2.0](LICENSE).
