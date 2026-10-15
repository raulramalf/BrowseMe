# Decisiones confirmadas (Fase 2, paso 1)

Fecha: 2026-10-15

| # | Decisión | Valor confirmado |
|---|---|---|
| 1 | Descarga HTTP | OkHttp con resolución DNS propia validada; Jsoup solo para parsear |
| 3 | Persistencia | PostgreSQL + Flyway; repositorio en memoria para dev/tests unitarios; Testcontainers en integración |
| 4 | Retención | 7 días |
| 6 | Modelo de Ollama | `llama3.2:3b` por defecto (configurable por variable de entorno) |
| 7 | Licencia | Apache 2.0 |
| 11 | Despliegue | Local con Docker Compose; además, publicación gratuita opcional (Vercel + Render, perfil `cloud`), pedida el 2026-10-15 |
| — | Plazo | 1 semana (objetivo: 2026-10-22) |

## Entorno de desarrollo

- 7,7 GB RAM, CPU de 4 hilos (Intel Haswell), GeForce 920M (Kepler, no soportada
  por la aceleración CUDA de Ollama): **inferencia solo en CPU**.
- Consecuencia: la interpretación con IA tardará decenas de segundos por informe.
  El diseño asíncrono lo absorbe; el timeout de IA debe ser generoso (≈120 s) y
  configurable, y el fallback `COMPLETED_WITHOUT_AI` es el camino normal si se excede.
- Toolchain sin sudo en `~/.local/opt`: Temurin JDK 25.0.4.1+1 (Adoptium, SHA-256
  verificado) y Maven 3.9.16 (Apache, SHA-512 verificado). El proyecto usa el
  Maven Wrapper 3.3.4 (`backend/mvnw`), fijado a Maven 3.9.16.
- Memoria justa con Postgres + backend + Ollama + navegador: limitar heap de la JVM
  (`-Xmx512m`) y no ejecutar Testcontainers con Ollama levantado a la vez.

## Ajuste de alcance por plazo de 1 semana

El plan original es de 4 semanas. Para una semana se recorta, en este orden y
sin tocar lo innegociable (SSRF, validación de salida de IA, tests del núcleo):

- Fuera: PageSpeed, exportación PDF, `sitemap.xml`, modo oscuro, `DELETE`,
  E2E con Playwright, despliegue público.
- Se mantiene: catálogo de reglas completo, robots.txt, IA con validación,
  API asíncrona, Postgres + Flyway, frontend con estados, páginas legales,
  Docker Compose, README, CI básico.

## robots.txt (implementado en la funcionalidad del fetcher)

- El estado que se muestra en el informe (`ROBOTS_TXT_BLOCKS`) se calcula para
  **Googlebot** (con `*` como alternativa), porque es lo que importa para el SEO.
- **Respeto**: si robots.txt nombra expresamente a `BrowseMeBot` y le prohíbe la
  URL, no se descarga la página y el análisis termina con `ROBOTS_DISALLOWED`.
  Una prohibición genérica (`User-agent: *`) se **informa** pero no detiene el
  análisis: es una comprobación puntual pedida por el usuario, no un rastreo
  (el mismo criterio que siguen las herramientas de prueba bajo demanda de los
  buscadores). Confirmado el 2026-10-15.
- 4xx en robots.txt equivale a «sin restricciones» (`NOT_FOUND`); 5xx, errores o
  destinos prohibidos, a `UNAVAILABLE` (RFC 9309).

## Publicación gratuita (2026-10-15)

- Pedido: que cualquiera pueda usar la web publicada y que el servidor sea gratis.
- Frontend en Vercel; backend en el plan gratuito de Render (Docker, 512 MB,
  se duerme tras 15 min). Su PostgreSQL gratuito caduca a los 30 días, así que
  el perfil `cloud` usa el repositorio en memoria y desactiva la IA (Ollama
  necesita varios GB de RAM). La seguridad SSRF y las reglas no cambian.
- El frontend llama al backend por CORS (`VITE_API_BASE_URL`), no mediante un
  reenvío de Vercel, para que el límite por IP vea la IP real del cliente y no
  la de Vercel.
