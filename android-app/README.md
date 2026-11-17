# SpeedBot — Aplicación Android Nativa (Kotlin + Jetpack Compose + HC-05 SPP)

Aplicación nativa para Android desarrollada en **Kotlin** y **Jetpack Compose**, diseñada para controlar un auto robótico con **Arduino Nano** y módulo **Bluetooth Clásico HC-05 (SPP / RFCOMM)** a **9600 baudios**.

**Colegio Juan XXIII** — Desarrollado por:
- Luca Fazio
- Gonzalo Fernandez
- Santiago Garcia Higa
- Joaquin Gimenez

---

## Características Principales

- **Bluetooth Clásico SPP Nativo (`00001101-0000-1000-8000-00805F9B34FB`):**
  - Detección automática de dispositivos previamente vinculados en Android (`bondedDevices`) y reconexión directa al último HC-05 usado.
  - Búsqueda de dispositivos Bluetooth cercanos (`startDiscovery()`) y acceso directo a Ajustes Bluetooth de Android.
  - Manejo completo de permisos para Android 12+ (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`) y Android 11 o inferior (`BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION`).
  - Todos los mensajes de estado y error 100% en español.
- **Comandos exactos para Arduino Nano (`9600 baud`):**
  - `F` (Adelante 90°), `B` (Atrás 270°), `L` (Izquierda 180°), `R` (Derecha 0°).
  - Diagonales: `I` (45°), `G` (135°), `H` (225°), `J` (315°).
  - `S` (Detener / Freno Activo al soltar el joystick o finalizar tiempo).
  - `Z` (Bocina continua mientras se mantiene presionado el botón).
  - `E` / `D` (Encender / Apagar luces LED).
- **Joystick Analógico 360° nativo en Canvas** con cálculo de rumbo (`0°–359°`), pedal (`0%–100%`) y freno automático al soltar.
- **Temporizador de sesión de 2 minutos (`02:00` máx.):** pausa, reanudación, botón `+30s` con límite estricto de 2:00 y desconexión automática segura al agotarse el tiempo.
- **Audio sintetizado nativo (`AudioTrack`):** bocina bitonal continua (`420/435 Hz`), tonos de motor proporcionales al pedal y alarma descendente.

---

## Cómo generar e instalar el APK

### Opción 1: Automático con GitHub Actions (sin instalar Android Studio)
1. Sube este repositorio a GitHub.
2. Ingresa a la pestaña **Actions** en tu repositorio de GitHub.
3. Ejecuta el flujo **Build SpeedBot Android APK** (o se ejecutará automáticamente al hacer `push` a `main` / `master`).
4. Al finalizar, descarga el artefacto **SpeedBot-APK**, descomprime el `.zip` y pasa `SpeedBot-release.apk` (o `SpeedBot-debug.apk`) a tu celular Android para instalarlo.

### Opción 2: Localmente con Android Studio
1. Abre la carpeta `android-app/` desde **Android Studio**.
2. Espera a que Gradle sincronice las dependencias.
3. Ve a **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. El archivo `.apk` se generará en `android-app/app/build/outputs/apk/debug/app-debug.apk`.
