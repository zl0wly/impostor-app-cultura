# 🕵️ El Impostor — Cultura de Venezuela

Juego del impostor para jugar con varios teléfonos Android. Cada jugador recibe una
palabra de una categoría (bailes, gastronomía, mitos, etc.); a uno solo le toca ser el
**impostor**. La app reparte los roles y las palabras; luego los jugadores discuten en
persona y al final votan si repetir la ronda o terminar.

> Proyecto de tarea. No está publicado en ninguna tienda.

## ✨ Características

- Sala con **3–8 jugadores** (varios teléfonos).
- **Dos modos de conexión**:
  - **Nearby Connections** (recomendado): usa Bluetooth + Wi-Fi automáticamente. Sin
    hotspot, sin misma red, sin emparejar. Requiere Google Play Services.
  - **Hotspot / misma Wi-Fi** (alternativo): pensado para teléfonos **sin Nearby / sin
    Play Services**. El anfitrión comparte su red y los jugadores se conectan por IP.
- Categorías de cultura venezolana: bailes típicos, instrumentos musicales, fiestas
  religiosas, gastronomía, dichos, mitos y leyendas.
- **Dos modos de impostor**: *palabra falsa* (otra palabra al azar de la misma
  categoría) o *sin palabra*.
- La palabra permanece oculta hasta pulsar **Mostrar** (para que nadie más la vea).
- Votación **Repetir / Terminar** al final de cada ronda.
- Palabras fáciles de ampliar mediante CSV.

## 📱 Requisitos

- Android **8.0 (API 26)** o superior.
- Modo Nearby: teléfonos con **Google Play Services**.
- Modo Hotspot: todos **en la misma red** (hotspot del anfitrión o el mismo Wi-Fi).

## 🔧 Compilar la APK

Con JDK 17 y el SDK de Android instalados:

```powershell
$env:JAVA_HOME='C:\Users\Christian\AppData\Local\AndroidBuild\jdk-17'
.\gradlew.bat assembleDebug
```

La APK queda en:

```
app\build\outputs\apk\debug\app-debug.apk
```

Instalar en un teléfono:

```
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
```

(o copiar el APK al teléfono, abrirlo y activar "instalar de orígenes desconocidos").

> La primera compilación necesita internet (descarga dependencias de Gradle). Después la
> app funciona 100% local.

## 🎮 Cómo se juega

1. Instala la app en 3–8 teléfonos (Android 8+; con Play Services si usas Nearby).
2. Abre la app y **concede los permisos** (Bluetooth / Dispositivos cercanos / Ubicación).
3. Elige el **modo de conexión** en el menú principal.
4. **Anfitrión**: escribe su nombre → **Crear sala**.
5. **Jugadores**: **Unirse a una sala** → tocan la sala detectada (o escriben la IP en
   modo Hotspot) → quedan conectados.
6. El anfitrión elige **categoría** y **modo de impostor** → **¡Preparados!**.
7. Cada jugador pulsa **Mostrar palabra** (en privado) y la ve.
8. Discuten en persona describiendo su palabra sin decirla.
9. Al final, cada jugador vota **🔄 Repetir** o **⛔ Terminar**. El anfitrión ve el
   recuento y pulsa **Siguiente ronda** o **Terminar juego**.

## 🔌 Modo Nearby (recomendado)

- Todos los teléfonos con Google Play Services.
- No hace falta hotspot ni estar en la misma red.
- El anfitrión crea la sala y los jugadores la ven automáticamente al pulsar "Unirse".

## 📶 Modo Hotspot / misma Wi-Fi (alternativo)

Para teléfonos **sin Nearby**. Todos deben estar en la **misma red**:

- Opción A: el anfitrión activa el **punto de acceso (hotspot)** del teléfono y los demás
  se conectan a él.
- Opción B: todos conectados al **mismo Wi-Fi**.

Pasos:
1. **Anfitrión**: menú → modo **Hotspot** → **Crear sala** → aparece
   **"IP anfitrión: x.x.x.x:8988"**.
2. **Jugadores**: menú → modo **Hotspot** → **Unirse**. Verán la sala automáticamente
   (descubrimiento por UDP) o podrán **escribir la IP** del anfitrión y pulsar **Conectar**.

> Puertos: TCP **8988** (datos) y UDP **8989** (descubrimiento). Algunos routers/hotspots
> bloquean el broadcast; en ese caso usa la **IP manual**.

## 🗂️ Categorías y palabras (CSV)

Las palabras están en `app/src/main/assets/palabras.csv` con formato:

```
categoria,palabra
Bailes típicos,Joropo
Gastronomía,Arepa
```

- Una fila por palabra; se agrupan por categoría.
- Para agregar o cambiar palabras, edita el CSV y recompila.
- El modo "palabra falsa" necesita **mínimo 2 palabras** por categoría (para sortear una
  distinta a la real).

## 🎭 Modos de impostor

- **Palabra falsa**: al impostor le toca otra palabra **al azar de la misma categoría**.
- **Sin palabra**: el impostor no recibe palabra y debe adivinar la de los demás.

## 🧱 Estructura del proyecto

```
app/src/main/java/com/impostor/cultura/
├── MainActivity.kt        · menú (nombre + modo de conexión)
├── HostActivity.kt        · lobby y control del anfitrión
├── JoinActivity.kt        · descubrir/unirse + ver rol
├── GameSession.kt         · lógica del juego (roles, votos) + LiveData
├── PermissionHelper.kt    · permisos según versión de Android
├── model/                 · Category, Player, RoleAssignment, SessionInfo
├── data/                  · WordData, CsvWordLoader
└── net/
    ├── Transport.kt       · interfaz de red (abstracción)
    ├── NearbyTransport.kt · implementación con Nearby Connections
    ├── HotspotTransport.kt· implementación TCP/UDP sobre la LAN
    └── Protocol.kt        · mensajes JSON
app/src/main/assets/palabras.csv
```

`GameSession` no sabe si por debajo hay Nearby o sockets: usa la interfaz `Transport`.
Así **ambos modos comparten la misma lógica de juego**.

## 🔐 Permisos

- Nearby (Android 12+): `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`.
- Android 13+: `NEARBY_WIFI_DEVICES`.
- Android ≤ 11: `ACCESS_FINE_LOCATION`.
- `INTERNET` (para el modo Hotspot / TCP).

## ❓ Preguntas frecuentes

**¿Funciona en iPhone / iOS?** Con Android nativo, no. Compilar para iOS requiere un Mac
con Xcode (aunque sea para una tarea). No hay versión iOS sin un Mac.

**No aparece la sala en modo Hotspot.** Asegúrate de estar todos en la misma red y que el
anfitrión ya creó la sala. Si el broadcast está bloqueado, usa la **IP manual**.

**No conecta por Nearby.** Verifica Google Play Services y que los permisos estén concedidos.

**Se pierde la conexión al cambiar de app.** Mantén la app en primer plano durante la partida.

## 🧪 Verificado

La app fue compilada y probada en un emulador Android 11: instalación, arranque del menú,
creación de sala (Nearby y Hotspot con IP mostrada), reparto de rol/impostor, mostrar
palabra y votación. La conexión **real entre varios teléfonos** conviene probarla en
dispositivos físicos.

