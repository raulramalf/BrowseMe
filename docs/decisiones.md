# Decisiones confirmadas (Fase 2, paso 1)

Fecha: 2026-10-15

| # | Decisión | Valor confirmado |
|---|---|---|
| 1 | Descarga HTTP | OkHttp con resolución DNS propia validada; Jsoup solo para parsear |
| 3 | Persistencia | PostgreSQL + Flyway; repositorio en memoria para dev/tests unitarios; Testcontainers en integración |
| 4 | Retención | 7 días |
| 6 | Modelo de Ollama | `llama3.2:3b` por defecto (configurable por variable de entorno) |
| 7 | Licencia | Apache 2.0 |
| 11 | Despliegue | Local: app de escritorio descargable (perfil `local`) o Docker Compose; web informativa en Vercel (2026-10-15) |
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

## Web informativa y app de escritorio (2026-10-15)

- Pedido: web pública solo informativa y una app descargable que analice en el
  ordenador de cada persona con IA local. Se descartó alojar el backend (Render,
  Koyeb…) porque el usuario no quiere depender de un servidor.
- La app se empaqueta con `jpackage` (Java incluido) para Windows, macOS
  (Apple Silicon, runner `macos-latest`) y Linux x86-64. Perfil `local`: escucha
  solo en 127.0.0.1, informes en memoria, IA con Ollama local y caída limpia a
  `COMPLETED_WITHOUT_AI` si no está instalado.
- El backend sirve el frontend compilado y las rutas del SPA solo en el perfil
  `local`; las páginas reciben la misma CSP que pone nginx.
- La web de Vercel se compila con `VITE_PUBLIC_SITE=true`: el analizador redirige
  a `/descargar`, que enlaza a `releases/latest/download/…` de GitHub.

## Versión 1.1: nota 0-10, captación y diseño (2026-10-15)

Pedido por el propietario; respuestas a las preguntas de diseño:

- **Nota 0-10: fórmula pública** (no la IA). Cambia el principio 4 del prompt de
  desarrollo («no hay puntuación global arbitraria»): la nota no es arbitraria
  porque es determinista, está desglosada y se explica en el informe. Pesos: SEO
  40 %, captación y fidelización 30 %, diseño 30 %. Penalizaciones por severidad
  en décimas con enteros (alta 30, media 15, baja 5, informativa 0).
- **Retención: señales en la página.** La app no ve datos reales de clientes;
  siete reglas detectan contacto, llamadas a la acción, suscripción, opiniones,
  redes, datos del negocio y vista previa al compartir, y la IA explica cómo mejorar.
- **Diseño: con navegador real.** Elegido frente a «solo estructura del HTML». Se usa
  el Chrome, Edge o Chromium **ya instalado** (controlado por el DevTools Protocol
  con `java.net.http.WebSocket`, sin dependencias nuevas), así que la descarga no crece
  ~180 MB. Si no hay navegador, el análisis sigue con las reglas del HTML y la nota de
  diseño se marca parcial. El aislamiento de red y la regla SSRF están descritos en
  docs/seguridad.md.
- Las capturas (JPEG, tope 600 KB) se guardan con el análisis; PostgreSQL las guarda
  en el JSON del snapshot (Base64). Si fuese un problema, se moverían a una tabla
  aparte.
- Los análisis guardados antes de la 1.1 se siguen leyendo: los campos nuevos
  toman valores por defecto.

## Versión 1.1.1: consejos que no dependen de la IA (2026-10-15)

- Motivo: con la 1.1, sin Ollama instalado el informe solo decía «falta X» y el
  consejo de cómo captar clientes dependía de la IA. Ahora cada hallazgo lleva una
  **guía fija** (`RuleGuides`: por qué importa y pasos concretos) y los **puntos
  exactos** que sumaría corregirlo (`scoreGain`, recalculando la nota sin ese hallazgo).
- La guía es conocimiento general escrito en el código, no un dato de la página ni
  salida de la IA; los hechos siguen saliendo de las reglas. La IA solo añade una
  explicación y prioriza.
- El informe tiene una sección «Cómo conseguir más clientes» con los hallazgos que
  influyen en captar clientes (`affectsClients`), ordenados por puntos ganables, y
  «Ya lo estás haciendo bien» con las señales detectadas. La portada muestra la nota.

## Versión 1.1.2: actualizar la app de escritorio sin sorpresas (2026-10-15)

- Problema: tras actualizar, el usuario seguía viendo la versión anterior. Causas
  posibles: una copia vieja aún abierta en el puerto 8765 (la nueva no podía arrancar
  y no avisaba) y el navegador mostrando `index.html` desde su caché.
- Solución: `PortSelector` elige el siguiente puerto libre y avisa por consola;
  las páginas llevan `Cache-Control: no-cache` (los `/assets/` con hash, `immutable`);
  el pie muestra la versión (`VERSION`, fijada al compilar con `__APP_VERSION__`).
