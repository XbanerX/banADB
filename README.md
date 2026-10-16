# banADB

<p align="center">
  <img src="app/src/main/res/drawable/ic_banadb_logo.png" alt="banADB Logo" width="128" height="128" />
</p>

<p align="center">
  <b>Control remoto ADB inalámbrico y gestor integral de Android a Android.</b><br>
  Sin necesidad de PC. Sin necesidad de root.
</p>

<p align="center">
  <a href="#descarga-e-instalación">Descargas</a> •
  <a href="#características">Características</a> •
  <a href="USAGE.md">Guía de Uso</a> •
  <a href="#compilación-y-desarrollo">Desarrollo</a> •
  <a href="LICENSE">Licencia</a>
</p>

---

## Capturas de Pantalla

| Dispositivos y Home | Emparejamiento Inalámbrico | Control Remoto (D-Pad) |
|:---:|:---:|:---:|
| <img src="docs/screenshots/01_home.png" width="240" /> | <img src="docs/screenshots/02_pairing.png" width="240" /> | <img src="docs/screenshots/03_remote_dpad.png" width="240" /> |

| Modo Trackpad | Panel de Herramientas | Galería Remota con Miniaturas |
|:---:|:---:|:---:|
| <img src="docs/screenshots/04_remote_touchpad.png" width="240" /> | <img src="docs/screenshots/05_remote_tools.png" width="240" /> | <img src="docs/screenshots/06_gallery.png" width="240" /> |

| Gestor de Archivos | Administrador de Apps | Espejo de Notificaciones |
|:---:|:---:|:---:|
| <img src="docs/screenshots/07_files.png" width="240" /> | <img src="docs/screenshots/08_apps.png" width="240" /> | <img src="docs/screenshots/09_notifications.png" width="240" /> |

---

## ⚡ Características

- **Emparejamiento Inalámbrico (Android 11+):**
  - Detección automática por mDNS de teléfonos con Depuración Inalámbrica activa.
  - Emparejamiento en un paso con código de 6 dígitos.
  - Soporte para emparejamiento manual ingresando IP y puerto asignado.
- **Conexión Clásica (Android 10 o inferior):**
  - Conexión directa al puerto ADB `5555` (para dispositivos configurados previamente con `adb tcpip 5555` o dispositivos rooteados).
  - Conexión local al propio dispositivo (`127.0.0.1:5555`).
- **Control Remoto Interactivo:**
  - **Modo D-Pad:** Cruceta táctil de cuatro direcciones y botón central OK para navegación rápida en Android TV, cajas inteligentes o teléfonos.
  - **Modo Trackpad:** Superficie táctil que traduce deslizamientos y toques en eventos de puntero reales sobre la pantalla remota (con opción de invertir eje).
  - **Modo Numpad:** Teclado numérico y barra de inyección de texto directo.
  - **Botones de sistema:** Atrás, Inicio, Recientes, Encendido, Volumen (+ / - / Silencio) y Controles Multimedia.
- **Galería Remota con Miniaturas:**
  - Explora fotos y videos remotos en `DCIM/` y `Pictures/` sin necesidad de descargarlos manualmente.
  - Miniaturas cargadas en streaming ligero para no saturar la red Wi-Fi.
  - Vista previa a pantalla completa y visor de detalles (resolución, tamaño, fecha).
- **Gestor de Archivos Remoto:**
  - Navegación rápida por el almacenamiento interno (`/sdcard`).
  - Creación de carpetas, borrado de archivos e inspección de tamaños y fechas.
  - Descarga y visualización directa de archivos en el teléfono local.
  - Detección de archivos `.apk` para instalarlos remotamente con un toque.
- **Administrador de Aplicaciones:**
  - Listado de apps instaladas con filtro rápido (todas o solo de usuario).
  - Lanzar apps, forzar detención (*Force Stop*) y limpiar datos/caché remotamente.
  - **Extracción de APK:** Descarga el archivo APK de cualquier aplicación del teléfono remoto a tu almacenamiento local.
  - **Instalación de APK:** Selecciona un APK de tu teléfono y envíalo a instalar en el remoto.
- **Espejo de Notificaciones Remotas:**
  - Lee las notificaciones activas del teléfono objetivo clasificadas por categoría (mensajería, redes, sistema).
  - Modo Auto-Sync con actualización periódica en vivo.
  - Botón para copiar el texto de la notificación o descartarla remotamente.
- **Puente de Portapapeles (Clipboard):**
  - Lee lo que hay actualmente en el portapapeles del teléfono remoto.
  - Envía textos largos directamente al portapapeles remoto sin complicaciones.
- **Terminal ADB y Scripts Rápidos:**
  - Consola interactiva para ejecutar comandos `sh` y `adb shell`.
  - Atajos rápidos para comandos frecuentes (`pm list packages`, `dumpsys battery`, `getprop`, `top`, `logcat`, etc.).
  - Gestor de scripts para automatizar tareas repetitivas.
- **Diagnóstico y Estado del Sistema:**
  - Lectura en tiempo real de batería (temperatura en °C, voltaje, porcentaje y estado de salud).
  - Especificaciones del dispositivo (fabricante, modelo, versión de Android, API, resolución de pantalla, uptime).
- **Servicio en Segundo Plano:**
  - Mantiene la sesión ADB activa con una notificación persistente, evitando que el sistema cierre la conexión al cambiar de app.

Para ver instrucciones detalladas de cada función, consulta el [Manual de Uso (USAGE.md)](USAGE.md).

---

## 📥 Descarga e Instalación

### Desde GitHub Releases
La forma recomendada de instalar banADB es descargar el paquete APK firmado más reciente directamente desde la página de versiones:

👉 **[Descargar banADB en GitHub Releases](https://github.com/baner/banadb/releases)**

1. Descarga el archivo `banadb.apk` en tu dispositivo Android.
2. Abre el archivo descargado y autoriza la instalación desde tu navegador o explorador de archivos.
3. ¡Listo! Abre banADB y comienza a emparejar tus dispositivos.

### F-Droid
> ⏳ **Próximamente disponible en F-Droid:** Se está preparando la inclusión en el repositorio oficial de F-Droid para permitir instalaciones y actualizaciones automáticas con código 100% abierto.

---

## 🛠️ Compilación y Desarrollo

Si deseas compilar la aplicación tú mismo o contribuir al código fuente, sigue estos pasos:

### Requisitos Previos
- **Git** instalado en tu sistema.
- **JDK 17** (OpenJDK 17 recomendado).
- **Android SDK** con herramientas de compilación (`build-tools 34.0.0` o superior).
- Acceso a internet para que Gradle descargue las dependencias.

### Pasos para Compilar

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/baner/banadb.git
   cd banadb
   ```

2. **Configurar el entorno Java:**
   Asegúrate de que `JAVA_HOME` apunte a Java 17:
   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
   ```

3. **Compilar el APK de depuración:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Ubicación del APK resultante:**
   Una vez concluida la compilación exitosamente, encontrarás el archivo APK en:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

5. **Instalar en un teléfono conectado:**
   Puedes instalarlo directamente en tu dispositivo conectado por cable USB o Wi-Fi:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📄 Licencia y Créditos

- **Proyecto:** banADB
- **Versión:** 1.0.0 (Build 1)
- **Autor:** baner
- **Contacto:** [bandroid@banerxus.com](mailto:bandroid@banerxus.com)
- **Licencia:** Distribuido bajo los términos de la [GNU Affero General Public License v3.0 (AGPL-3.0)](LICENSE).
