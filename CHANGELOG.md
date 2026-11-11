# Changelog

## v1.0.1

### Corregido
- Los permisos de Bluetooth/Ubicación solo se piden en el modo **Nearby**; el modo
  **Hotspot** ya no los exige (clave para teléfonos sin Nearby).
- La sala se limita a **8 jugadores**: se rechazan las conexiones extra (Nearby y Hotspot).
- Se impide iniciar la ronda con un solo jugador y se avisa cuando hay menos de 3.

## v1.0.0 — Primer release

### Añadido
- Juego del impostor con categorías de cultura venezolana (bailes típicos, instrumentos
  musicales, fiestas religiosas, gastronomía, dichos, mitos y leyendas).
- Conexión por **Google Nearby Connections** (sin hotspot ni misma Wi-Fi).
- Conexión alternativa por **Hotspot / misma Wi-Fi** (TCP/UDP sobre la LAN) para equipos
  sin Nearby / sin Google Play Services.
- Arquitectura con interfaz `Transport` (Nearby + Hotspot) y **lógica de juego compartida**.
- Reparto de roles con dos modos: **palabra falsa** (al azar de la misma categoría) y
  **sin palabra**.
- Palabra oculta con botón **Mostrar / Ocultar**.
- Votación **Repetir / Terminar** y control de rondas por parte del anfitrión.
- Palabras cargables desde `app/src/main/assets/palabras.csv`.
- Documentación en `README.md`.
- APK compilada en `dist/impostor-v1.0.0.apk`.

### Notas
- Android 8.0+ (API 26). El modo Nearby requiere Google Play Services.
- Probado en emulador Android 11 (menú, salas Nearby/Hotspot, reparto de rol, votación).
  La conexión real entre varios teléfonos debe probarse en dispositivos físicos.
