# Seguridad de BrowseMe

BrowseMe descarga URLs que escribe cualquiera desde su propio servidor. El
riesgo principal es **SSRF**: usar el servidor para llegar a redes internas o a
los metadatos del proveedor cloud. Que los tests pasen no significa que la
aplicación sea segura; este documento explica qué se hace y qué queda abierto.

## Anti-SSRF

| Medida | Dónde | Test |
|---|---|---|
| Solo `http`/`https`, puertos 80 y 443, máximo 2048 caracteres, sin credenciales | `UrlPolicy.parse` | `UrlPolicyTest` |
| Host normalizado antes de validar: IDN a punycode, IPv4 en formatos alternativos (`127.1`, `0x7f000001`, `2130706433`, octal, dígitos de ancho completo), IPv6 | `UrlPolicy`, `Ipv4Literal` | `UrlPolicyTest`, `Ipv4LiteralTest` |
| Destinos prohibidos: loopback, RFC 1918, link-local (`169.254.169.254`), CGNAT, multicast, no especificada, documentación y reservadas. En IPv6 solo se admite unicast global (`2000::/3`), sin 6to4, Teredo ni prefijos de documentación | `UrlPolicy.isForbidden` | `UrlPolicyTest` |
| Nombres locales rechazados (`localhost`, `.local`, `.internal`, `.home.arpa`, `.lan`, una sola etiqueta) | `UrlPolicy` | `UrlPolicyTest` |
| DNS resuelto una vez; se rechaza si **cualquier** IP está prohibida, y se conecta a esas IPs sin volver a resolver (contra DNS rebinding) | `ValidatingDns` | `SafeHttpPageFetcherTest$Dns` |
| La dirección realmente conectada se vuelve a comprobar antes de enviar nada (cubre hosts que son IPs literales) | interceptor de red en `SafeHttpPageFetcher` | prueba de mutación manual |
| Redirecciones manuales, máximo 3; cada salto repite toda la validación | `SafeHttpPageFetcher` | `...$Redirects` |
| Conexiones nunca reutilizadas (`Connection: close`, pool sin conexiones libres) | `SafeHttpPageFetcher` | `never_reuses_a_connection_between_requests` |
| Timeouts: conexión 5 s, lectura 10 s, total 20 s para toda la cadena | `FetchLimits` | `...$Limits` |
| Cuerpo máximo 2 MB contando bytes **descomprimidos** (sin fiarse de `Content-Length`, con freno a bombas gzip) | `BoundedBodyReader` | `...$Limits` |
| Solo `text/html`; sin cookies, caché, proxy ni cabeceras del usuario; User-Agent identificable | `SafeHttpPageFetcher` | `...$Extraction` |
| robots.txt con las mismas protecciones; si lo prohíbe expresamente a BrowseMeBot, no se descarga la página | `SafeHttpPageFetcher` | `...$Robots` |
| La validación del `POST` es solo una respuesta rápida; la real se repite al descargar | `AnalysisProcessor` | `AnalysisProcessorTest` |
| Jsoup solo parsea; `Jsoup.connect()` no se usa | `JsoupSnapshotExtractor` | revisión de código |

## IA y datos no confiables

- El modelo recibe un **resumen acotado** (unos 2000 caracteres) y los hallazgos, nunca el HTML (`AiInputFactory`).
- El contenido de la página va entre `<datos_pagina>` y `</datos_pagina>`, con `<` y `>` neutralizados para que no pueda cerrar el bloque. La instrucción de sistema prohíbe obedecerlo.
- El modelo no tiene herramientas ni red: solo recibe texto y devuelve JSON con un esquema fijo (formato estructurado de Ollama).
- `AiOutputValidator` descarta recomendaciones que citan `ruleId` no detectados o inventados, exige marcar las sugerencias generales y acota longitudes y número de elementos. JSON roto, sin resumen o fuera de plazo: el informe sale sin IA (`COMPLETED_WITHOUT_AI`).
- El frontend pinta todo como texto: no se usa `dangerouslySetInnerHTML` (hay un test con `<script>` y `onerror`).

## Abuso y recursos

- Límite por IP con Bucket4j: 5 análisis nuevos y 120 consultas por minuto (configurable). Las IPs solo viven en memoria, en un mapa acotado, y no se escriben en logs.
- Ejecutor acotado (2 hilos y cola de 20 por defecto); con la cola llena la API responde 503 con `Retry-After`.
- Un análisis por URL en curso: repetir la misma URL devuelve el análisis existente.

## Web

- CORS restrictivo: solo el origen del frontend, solo `GET`/`POST`, sin credenciales. Con Docker Compose el frontend y la API comparten origen a través de nginx.
- Cabeceras: CSP estricta (`default-src 'none'` en la API; solo `'self'` en la SPA), `X-Content-Type-Options`, `Referrer-Policy: no-referrer`, `X-Frame-Options`; HSTS activable con `BROWSEME_HSTS=true` cuando hay HTTPS.
- Errores RFC 9457 genéricos: sin trazas, clases ni mensajes internos.
- Actuator: solo `health`, sin detalles; nginx no lo publica.
- Secretos solo por variables de entorno (`.env` ignorado por git, `.env.example` versionado). El backend corre como usuario no root en su contenedor y no publica puertos.

## Análisis visual con un navegador real (v1.1)

La app puede dibujar la página con el Chrome, Edge o Chromium **del equipo** (no se incluye ninguno) para medir contraste, tamaños y desbordes en móvil. Un navegador es una superficie de ataque nueva, así que se aísla de la red y la regla anti-SSRF no se relaja:

| Medida | Dónde | Test |
|---|---|---|
| El navegador no resuelve ningún nombre (`--host-resolver-rules=MAP * ~NOTFOUND`) y todo su tráfico va a un proxy inexistente (`127.0.0.1:1`): no tiene salida propia, ni hacia IPs literales | `ChromeRenderer.launch` | `the_browser_never_reaches_the_network_on_its_own` |
| Cada petición de la página se intercepta (`Fetch.requestPaused`) y la hace `SafeResourceFetcher`, con la misma política de URLs, DNS validado, comprobación de la IP conectada, sin proxy ni cookies | `RequestBroker`, `SafeResourceFetcher` | mismo test (IP literal, host privado y metadatos no llegan) |
| Solo GET y tipos necesarios para dibujar; máximo de peticiones, bytes totales y por recurso; redirecciones devueltas al navegador, que vuelve a pedirlas por el mismo filtro | `RequestBroker` | `ChromeRendererTest` |
| WebRTC sin UDP directo; diálogos de la página descartados; cookies del recurso no se reenvían | `ChromeRenderer` | revisión de código |
| Las métricas se calculan en un **mundo aislado** de JavaScript que el código de la página no puede alterar, y se validan igualmente como dato no confiable (rangos, tamaños, listas acotadas) | `metrics.js`, `ChromeRenderer.metrics` | `ChromeRendererTest` |
| Perfil temporal propio que se borra al terminar; el proceso y sus hijos se matan; plazo total configurable | `ChromeRenderer` | `leaves_no_temporary_profile_behind` |
| Chrome puede arrancar con su sandbox activado. Solo se desactiva (`--no-sandbox`) si se ejecuta como root en Linux (contenedores) o si `BROWSEME_NO_SANDBOX=true`, que fija el CI para sus páginas de prueba propias | `ChromeRenderer.launch` | — |
| Las capturas son JPEG de una página no confiable: se sirven como imagen (`image/jpeg`, `nosniff`, CSP de la API), con tope de 600 KB | `AnalysisController`, `VisualCheck` | `AnalysisApiTest` |
| Si falla o no hay navegador, el análisis sigue con las reglas del HTML y lo dice en las limitaciones | `AnalysisProcessor` | `AnalysisProcessorTest` |

## Riesgos que no se eliminan del todo

1. **Sin aislamiento de red a nivel de infraestructura.** La protección SSRF está en la aplicación. Como defensa en profundidad, en producción conviene que el contenedor del backend solo tenga salida a Internet (egress restringido: sin acceso a rangos privados, metadatos cloud ni otros servicios), por ejemplo con reglas de red del host o del proveedor.
2. **Rate limit en memoria y por instancia.** Se pierde al reiniciar y se puede evadir rotando IPs.
3. **Prompt injection mitigada, no eliminada.** Un texto bien diseñado podría sesgar la redacción de la IA. La validación impide que aparezcan hallazgos inventados, pero no garantiza que el tono o la priorización sean correctos.
4. **Alucinaciones de modelos pequeños.** Se reducen con el esquema y la validación, pero no desaparecen. Por eso la interpretación se marca siempre como «no verificada».
5. **El parser de URLs de Java y el de OkHttp no son idénticos.** Se mitiga revalidando con la URL ya normalizada y comprobando la IP conectada, pero cualquier diferencia futura entre parsers es un punto a vigilar.
6. **Navegador del equipo.** Chrome, Edge o Chromium pueden tener vulnerabilidades propias; el aislamiento de red y el sandbox reducen el riesgo, pero el navegador se actualiza con el sistema, no con BrowseMe. Quien no quiera usarlo puede desactivar el análisis visual (`BROWSEME_VISUAL_MODE=disabled`).
7. **Dependencias.** `npm audit` se ejecuta en CI. El análisis OWASP de dependencias Java (`dependency-check`) necesita descargar la base de datos NVD (y conviene una API key), así que no está automatizado; ver «Revisión de dependencias» en el README.
