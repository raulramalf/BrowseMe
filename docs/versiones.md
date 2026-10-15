# Versiones verificadas (Fase 2, paso 2)

Consultadas el 2026-10-15 directamente en los registros oficiales (Maven Central,
npm, Adoptium, nodejs.org, GitHub releases, Docker Hub, start.spring.io). Se
eligen siempre versiones **estables** (sin alpha/beta/RC/milestone).

## Backend

| Componente | Versión | Fuente | Notas |
|---|---|---|---|
| Java (LTS) | **25** | api.adoptium.net (`most_recent_lts: 25`) | Temurin 25 |
| Spring Boot | **4.1.1** (2026-08-20) | Maven Central, GitHub releases | 4.2 solo en milestones |
| Spring Framework | 7.0.9 | gestionada por Boot 4.1.1 | |
| Spring AI | **2.0.1** (2026-08-21) | Maven Central; start.spring.io: `2.0.1` → Boot `>=4.0.0 <4.2.0-M1` | README: 2.x ↔ Boot 4.x. Starter `spring-ai-starter-model-ollama` |
| Jsoup | **1.23.2** | Maven Central | Solo para parsear |
| OkHttp | **5.5.0** | Maven Central | No la gestiona Boot. Necesita kotlin-stdlib ≥ 2.1.21; Boot fija 2.3.21 (compatible) |
| Flyway | **12.4.0** | gestionada por Boot 4.1.1 | Hay 13.10.0 en Central, pero usamos la versión de Boot para no salirnos de lo que se ha probado juntos. Requiere `flyway-database-postgresql` |
| Driver PostgreSQL | 42.7.13 | gestionada por Boot | |
| Testcontainers | **2.0.5** | gestionada por Boot 4.1.1 | Coincide con la última de Central |
| WireMock | **3.13.2** (`wiremock-standalone`) | Maven Central | 4.x sigue en beta (4.0.0-beta.40). El artefacto standalone lleva Jetty sombreado y evita conflictos con Boot 4 |
| ArchUnit | **1.5.1** (2026-09-25) | Maven Central, GitHub release notes | Soporta ficheros de clase de Java 25 |
| Bucket4j | **8.21.0** (`com.bucket4j:bucket4j_jdk17-core`) | Maven Central | Es el artefacto vigente; `bucket4j-core` dejó de publicarse en la 8.10.1 |
| JUnit Jupiter | 6.0.3 | gestionada por Boot | |
| Mockito / AssertJ | 5.23.0 / 3.27.7 | gestionadas por Boot | |

## Frontend

| Componente | Versión | Fuente | Notas |
|---|---|---|---|
| Node.js | **24 LTS** (v24.22.0 «Krypton») | nodejs.org/dist/index.json | Vite pide `^20.19.0 \|\| >=22.12.0` |
| React / React DOM | **19.3.1** | npm | |
| Vite | **8.3.4** | npm | |
| @vitejs/plugin-react | **6.1.2** | npm | Requiere vite ^8 |
| TypeScript | **6.0.3** | npm; plantilla oficial `create-vite` react-ts usa `~6.0.2` | 7.0.2 es `latest` (compilador nativo), pero la plantilla oficial de Vite aún fija 6.0; seguimos a la plantilla |
| Vitest | **5.0.4** | npm | Peer: vite ^6 \|\| ^7 \|\| ^8 |
| @testing-library/react | **16.3.3** | npm | Peer: React 18/19, `@testing-library/dom` ^10 |
| React Router | **7.18.4** (`react-router`) | npm | Confirmada. En la v7 `react-router-dom` solo reexporta `react-router`; se usa este directamente |

## Infraestructura

| Componente | Versión | Fuente |
|---|---|---|
| Ollama | **0.40.4** | GitHub releases (latest) |
| Modelo | `llama3.2:3b` | decisión confirmada |
| PostgreSQL (imagen) | **18.6-alpine** | Docker Hub |

## Herramientas de build

| Componente | Versión | Fuente |
|---|---|---|
| Maven (vía wrapper) | 3.9.16 | Maven Central (`maven-core`) |
| maven-wrapper-plugin | 3.3.4 | Maven Central |
| Surefire / Failsafe | 3.5.6 | gestionadas por Boot 4.1.1 |
