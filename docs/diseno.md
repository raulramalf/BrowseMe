# Sistema visual del frontend

Inspirado en una guía de estilo tipo «consola de ingeniería»: lienzo oscuro
(obsidiana `#132322`), tarjetas claras (menta `#edf7f5`) y un único verde de
acción (`#3ddc91`). Los tokens viven en `frontend/src/styles/index.css`.

## Reglas
- El verde se reserva a acciones (botón, navegación activa), cifras destacadas
  y el estado en curso del progreso. No es decoración.
- Botones y pestañas en píldora (56 px); tarjetas grandes con 60 px de radio
  (20 px en móvil); sin degradados; sombra máxima `rgba(0,0,0,.04)`.
- Ritmo oscuro/claro: en el informe, los hechos verificados van en tarjeta
  clara y la interpretación de IA en tarjeta oscura con distintivo amarillo
  «No verificada», para separarlos también visualmente.
- Pesos tipográficos: 400 en texto, 500 en titulares.

## Desviaciones conscientes de la guía
- **Fuentes**: Aeonik/AeonikFono son comerciales. Se usa Inter si está
  instalada y, si no, la fuente del sistema. No se cargan fuentes remotas
  (CSP `font-src 'self'` y sin peticiones a terceros).
- **Severidad**: se añade un rojo/coral para la severidad alta; la severidad
  no puede depender solo del texto. Media usa el amarillo de la paleta y baja
  el verde menta suave.
- **Texto atenuado**: `#828786` no llega a contraste AA sobre menta; se usa
  `#424f4f` sobre claro y blanco al 72 % sobre oscuro.
- **Foco**: anillo amarillo sobre oscuro y obsidiana sobre claro, visible
  también sobre los botones verdes.
- Las cifras de la portada describen el propio servicio (1 página, 0 cookies
  de seguimiento, 7 días de conservación, código abierto), no métricas
  inventadas.
