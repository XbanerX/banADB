# banADB - Wireless & Autonomous ADB Cockpit

Una aplicación nativa moderna para Android construida con **Kotlin**, **Jetpack Compose** y el protocolo puro en Kotlin **KADB** (`com.flyfishxu:kadb`). Diseñada con un tema visual de alto rendimiento **Cyber Cyan** y fondo **Deep Slate**.

## 📱 Características Principales

1. **Emparejamiento Inalámbrico Android 11+ (Wireless Debugging):**
   - Descubrimiento automático mDNS (`_adb-tls-pairing._tcp` y `_adb-tls-connect._tcp`).
   - Diálogo de emparejamiento con código de 6 dígitos.
   - Conexión directa a puertos conocidos (ej. 5555 para dispositivos rooteados o activados previamente con `adb tcpip 5555`).

2. **Control Remoto Interactivo (D-Pad, Trackpad & Teclado):**
   - **Modo D-Pad:** Pad direccional con botón central OK.
   - **Modo Trackpad:** Superficie táctil para enviar gestos de deslizamiento (*swipe*) y clics mapeados a la resolución remota.
   - **Modo Teclado / Numpad:** Teclado numérico e inyección directa de cadenas de texto (`input text`).
   - **Barra de navegación del sistema:** Botones Atrás (*Back*), Inicio (*Home*), Aplicaciones recientes (*Recents*), Encendido (*Power*), Volumen (+ / - / Mute).

3. **Terminal ADB Interactiva:**
   - Salida en consola de ancho fijo estilo hacker con tema de colores.
   - Sugerencias rápidas (*chips*) para comandos comunes (`pm list packages`, `dumpsys battery`, `getprop`, `top`, `logcat`, `ip addr`, etc.).
   - Entrada de comandos con autodesplazamiento al final.

4. **Administrador de Aplicaciones:**
   - Búsqueda en tiempo real por nombre o paquete.
   - Filtro para alternar entre aplicaciones de usuario y de sistema.
   - Acciones remotas con un solo toque: Lanzar app, Forzar detención (*Force Stop*), Limpiar datos (*Clear Data*), y Desinstalar con confirmación.

5. **Explorador de Archivos Remoto:**
   - Navegación por carpetas con ruta de navegación (*breadcrumbs*).
   - Crear carpetas remotas (`mkdir`).
   - Eliminar archivos y carpetas.
   - Formateo de tamaños y fechas.

6. **Panel de Diagnósticos (Dashboard):**
   - Métricas de batería en tiempo real: Porcentaje, estado de carga, temperatura (°C), voltaje (mV), estado de salud.
   - Especificaciones de hardware y OS: Fabricante, modelo, versión de Android, API SDK, resolución de pantalla, IP conectada, tiempo activo (*uptime*).
   - Sincronización bidireccional del portapapeles remoto (*Remote Clipboard*).
   - Acciones de energía: Reinicio normal, reinicio a modo Recovery y reinicio a Bootloader.

7. **Administrador de Procesos:**
   - Lista dinámica de procesos en ejecución (`ps -A`) con PID y usuario.
   - Búsqueda y filtrado rápido.
   - Terminación forzada de procesos (*kill -9 PID*) con confirmación.

8. **Automatización y Scripts:**
   - Scripts predefinidos para tareas comunes del sistema.
   - Posibilidad de crear, ejecutar y guardar scripts personalizados.

9. **Screen Mirror / Captura en Vivo:**
   - Visualización de la pantalla del dispositivo remoto.
   - Mapeo directo de toques y deslizamientos en la imagen capturada para interactuar directamente como si estuvieras tocando la pantalla remota.
   - Modo de refresco manual o automático en vivo.

10. **Servicio en Primer Plano (Foreground Service):**
    - Mantiene la conexión ADB y la sesión activa en segundo plano con notificación persistente.

---

## 🛠️ Compilación e Instalación

### Compilar desde la línea de comandos:
```bash
cd /home/baner/Projects/banadb
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew assembleDebug
```

### Ubicación del APK generado:
```
/home/baner/Projects/banadb/app/build/outputs/apk/debug/app-debug.apk
```

### Instalar en un teléfono conectado por USB o Wi-Fi:
```bash
adb install -r banadb.apk
```

---

## 📄 Licencia y Créditos

- **Versión:** 1.0.0 (Build 1)
- **Autor:** baner
- **Contacto:** [bandroid@banerxus.com](mailto:bandroid@banerxus.com)
- **Licencia:** [GNU Affero General Public License v3.0 (AGPL-3.0)](LICENSE)

