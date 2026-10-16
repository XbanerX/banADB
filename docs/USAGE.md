# Manual de Uso de banADB

Esta guía detalla el funcionamiento de cada una de las herramientas integradas en **banADB**, para qué sirve cada función y el procedimiento paso a paso para utilizarlas.

---

## Índice

1. [Preparación Inicial del Dispositivo Objetivo](#1-preparación-inicial-del-dispositivo-objetivo)
2. [Conexión y Emparejamiento](#2-conexión-y-emparejamiento)
   - [Emparejamiento en Android 11 o superior (Código de 6 dígitos)](#emparejamiento-en-android-11-o-superior-código-de-6-dígitos)
   - [Conexión directa en Android 10 o inferior (Puerto 5555)](#conexión-directa-en-android-10-o-inferior-puerto-5555)
   - [Conexión al propio dispositivo local](#conexión-al-propio-dispositivo-local)
3. [Control Remoto en Vivo (Cockpit)](#3-control-remoto-en-vivo-cockpit)
   - [Modo D-Pad](#modo-d-pad)
   - [Modo Trackpad](#modo-trackpad)
   - [Modo Numpad y Entrada de Texto](#modo-numpad-y-entrada-de-texto)
   - [Botones de Sistema y Control de Volumen](#botones-de-sistema-y-control-de-volumen)
4. [Galería Remota (Remote Gallery)](#4-galería-remota-remote-gallery)
5. [Explorador de Archivos (File Manager)](#5-explorador-de-archivos-file-manager)
6. [Gestor de Aplicaciones (App Manager)](#6-gestor-de-aplicaciones-app-manager)
   - [Extraer APKs hacia tu teléfono](#extraer-apks-hacia-tu-teléfono)
   - [Instalar APKs remotamente](#instalar-apks-remotamente)
   - [Control de ciclo de vida (Lanzar, Detener, Limpiar, Desinstalar)](#control-de-ciclo-de-vida)
7. [Espejo de Notificaciones (Notification Mirror)](#7-espejo-de-notificaciones-notification-mirror)
8. [Puente de Portapapeles (Remote Clipboard)](#8-puente-de-portapapeles-remote-clipboard)
9. [Terminal Interactiva y Scripts](#9-terminal-interactiva-y-scripts)
10. [Diagnóstico de Batería y Hardware](#10-diagnóstico-de-batería-y-hardware)
11. [Comportamiento en Segundo Plano y Desconexión](#11-comportamiento-en-segundo-plano-y-desconexión)

---

## 1. Preparación Inicial del Dispositivo Objetivo

Para poder controlar un dispositivo Android de forma inalámbrica, ambos dispositivos deben estar conectados a la **misma red Wi-Fi** o compartir un punto de acceso (zona Wi-Fi móvil).

En el teléfono o televisor que deseas controlar:
1. Ve a **Ajustes** -> **Información del teléfono**.
2. Toca 7 veces consecutivas sobre el **Número de compilación** hasta que aparezca el aviso de que las opciones de desarrollador están activas.
3. Entra a **Ajustes** -> **Sistema** (o Ajustes adicionales) -> **Opciones de desarrollador**.
4. Activa la casilla **Depuración por Wi-Fi** (o *Wireless Debugging*).
   - En dispositivos de marcas como Xiaomi, Oppo o Realme, activa también la opción llamada **Depuración USB (ajustes de seguridad)** para permitir que los comandos de entrada (toques y teclas) sean aceptados.

---

## 2. Conexión y Emparejamiento

Al abrir banADB, la pantalla de inicio te mostrará tus dispositivos previamente guardados y el botón **+ Pair New Device**.

### Emparejamiento en Android 11 o superior (Código de 6 dígitos)
Android 11 en adelante utiliza TLS y puertos dinámicos para emparejar:

1. En el teléfono objetivo, entra a la sección de **Depuración inalámbrica** y pulsa en **Emparejar dispositivo con código de vinculación**.
2. Verás en pantalla una dirección IP, un puerto de emparejamiento (ej. `192.168.1.45:41235`) y un código numérico de 6 dígitos.
3. En banADB, pulsa **+ Pair New Device**:
   - **Detección mDNS:** Si tu red permite multidifusión, el teléfono aparecerá automáticamente en la lista. Toca sobre él, introduce el código de 6 dígitos y pulsa **Pair Code**.
   - **Ingreso manual:** Si no aparece automáticamente, despliega la sección **Manual IP & Port Entry**, escribe la IP, el puerto de emparejamiento que muestra la pantalla y los 6 dígitos del código.
4. Pulsa **Pair**. Una vez emparejado, banADB guardará la clave criptográfica y se conectará automáticamente.

### Conexión directa en Android 10 o inferior (Puerto 5555)
Dispositivos antiguos o teléfonos donde ya se ejecutó una vez el comando `adb tcpip 5555` escuchan permanentemente en el puerto 5555:

1. Pulsa **+ Pair New Device** y ve a la pestaña **Android 10**.
2. Escribe la IP del dispositivo y el puerto (por defecto `5555`).
3. Pulsa **Connect**. En la pantalla del dispositivo objetivo aparecerá una ventana preguntando si permites la depuración desde esa clave; pulsa **Permitir siempre**.

### Conexión al propio dispositivo local
banADB puede conectarse a sí mismo (localhost) para tareas de automatización interna:
1. En **+ Pair New Device**, selecciona la pestaña **Local (Host)**.
2. Si tu teléfono tiene activa la depuración inalámbrica en el puerto 5555, presiona **Connect to 127.0.0.1:5555**.

---

## 3. Control Remoto en Vivo (Cockpit)

Al conectar un dispositivo entrarás a la pantalla de control principal. Esta interfaz incluye una cruceta de navegación, un trackpad táctil y controles multimedia.

### Modo D-Pad
Ideal para Android TV, reproductores multimedia o navegar menús sin tocar la pantalla directamente:
- **Flechas Direccionales:** Envían eventos `DPAD_UP`, `DPAD_DOWN`, `DPAD_LEFT` y `DPAD_RIGHT`.
- **Botón OK Central:** Ejecuta la acción de selección (`DPAD_CENTER` o `ENTER`).
- **Respuesta táctil:** Cada pulsación genera vibración hápitca para confirmar el comando.

### Modo Trackpad
Convierte la pantalla de tu móvil en un ratón táctil para el dispositivo remoto:
- **Tocar una vez:** Envía un toque en la posición relativa actual.
- **Arrastrar el dedo:** Envía un deslizamiento suave (*swipe*) en la pantalla remota.
- **Interruptor Invert (Invertir eje):** Por defecto el movimiento sigue el desplazamiento natural del dedo. Si prefieres que funcione como el desplazamiento inverso de una rueda de ratón, activa este interruptor.

### Modo Numpad y Entrada de Texto
- **Teclas numéricas (0-9):** Para introducir contraseñas, pines o canales en televisores.
- **Campo de texto inferior:** Escribe cualquier texto o URL en la barra y presiona enviar para inyectarlo en el campo de texto activo del teléfono remoto mediante `input text`.

### Botones de Sistema y Control de Volumen
- **Fila de navegación rápida:**
  - **Back:** Botón Atrás del sistema.
  - **Home:** Va a la pantalla de inicio.
  - **Recents:** Abre la vista de aplicaciones recientes o multitarea.
- **Barra multimedia inferior:**
  - **Bajar Volumen (Vol -)**
  - **Silencio (Mute)**
  - **Subir Volumen (Vol +)**
  - **Reproducir / Pausar:** Envía la señal multimedia global para pausar música o videos.

---

## 4. Galería Remota (Remote Gallery)

Permite visualizar el contenido multimedia del teléfono remoto sin necesidad de transferir archivos pesados:

- **Detección automática:** Escanea automáticamente las carpetas `DCIM/Camera`, `DCIM/Screenshots` y `Pictures/`.
- **Filtros por pestaña:** Organiza rápidamente los archivos en **All**, **Camera** o **Screenshots**.
- **Miniaturas optimizadas:** Genera vistas previas ligeras a través del canal ADB para que la galería cargue con fluidez.
- **Identificación de Video:** Los archivos de video (`.mp4`, `.mkv`) muestran un indicador de duración y el botón de reproducción.
- **Previsualización:** Al tocar cualquier foto, se abre un visor detallado donde puedes ver la imagen a mayor resolución, su peso exacto, fecha de captura y la opción de descargarla a tu teléfono.

---

## 5. Explorador de Archivos (File Manager)

Un administrador de archivos completo que opera directamente sobre el almacenamiento del dispositivo remoto (`/sdcard/`):

- **Navegación por carpetas:** Toca cualquier carpeta para entrar en ella. La barra superior muestra la ruta actual y permite volver atrás en el árbol de directorios.
- **Crear carpetas:** Toca el botón **+** en la esquina superior para crear un nuevo directorio remoto.
- **Eliminar archivos:** El icono de papelera al lado de cada elemento elimina el archivo o carpeta tras una confirmación para evitar pérdidas accidentales.
- **Visualización directa:** Al pulsar sobre una imagen, documento de texto o archivo compatible, banADB descarga una copia temporal en tu teléfono y la abre con tu visor predeterminado.
- **Instalación de APKs:** Si tocas un archivo con extensión `.apk` dentro del explorador, la app te preguntará si deseas instalarlo de inmediato en el dispositivo remoto mediante `pm install -r`.

---

## 6. Gestor de Aplicaciones (App Manager)

Permite supervisar y gestionar todos los paquetes instalados en el dispositivo remoto.

### Extraer APKs hacia tu teléfono
Si quieres hacer una copia de seguridad de una app que está instalada en el dispositivo remoto:
1. Localiza la aplicación en la lista (puedes usar el buscador por nombre o nombre de paquete).
2. Toca el menú de tres puntos al lado de la app.
3. Selecciona **Extract APK**.
4. banADB obtendrá la ruta base del paquete mediante `pm path`, descargará el archivo APK y lo guardará en tu carpeta local de descargas (`/sdcard/Download/banADB/`).

### Instalar APKs remotamente
1. Pulsa el botón flotante **Install APK** en la esquina inferior.
2. Selecciona un archivo `.apk` almacenado en tu teléfono local.
3. banADB transferirá el archivo al teléfono remoto y ejecutará la instalación de forma silenciosa. Al concluir, recibirás una notificación de confirmación.

### Control de ciclo de vida
Desde el menú contextual de cada aplicación puedes:
- **Lanzar App:** Abre la actividad principal de la app en la pantalla del dispositivo remoto.
- **Forzar Detención (Force Stop):** Cierra inmediatamente el proceso de la app (`am force-stop`).
- **Limpiar Datos (Clear Data):** Borra la memoria caché y datos de usuario de la app (`pm clear`), dejándola como recién instalada.
- **Desinstalar:** Desinstala por completo la aplicación del sistema.

---

## 7. Espejo de Notificaciones (Notification Mirror)

Esta función te permite leer las notificaciones que están llegando al teléfono remoto sin tener que mirar su pantalla:

- **Lectura directa:** Extrae el título, texto descriptivo, aplicación remitente y hora de llegada de cada notificación activa.
- **Filtros inteligentes:** Puedes filtrar las notificaciones por categorías:
  - **Messages:** SMS, WhatsApp, Telegram, etc.
  - **Social:** Redes sociales y avisos comunitarios.
  - **Email:** Correos de Gmail, Outlook, etc.
  - **System:** Notificaciones propias del sistema Android (batería, descargas, conexiones).
- **Auto-Sync:** Activa el interruptor en la parte superior para que la lista se refresque automáticamente cada 5 segundos.
- **Acciones:**
  - **Copy:** Copia el texto íntegro de la notificación al portapapeles de tu teléfono.
  - **Dismiss:** Descarta la notificación en el dispositivo remoto.

---

## 8. Puente de Portapapeles (Remote Clipboard)

Compartir textos largos, enlaces o contraseñas entre dos teléfonos suele ser incómodo. banADB incluye un puente bidireccional:

- **Escribir en el remoto:** Pega o escribe el texto que deseas enviar en el campo superior y pulsa **Send to Remote Clipboard**. El contenido quedará listo para pegarse en cualquier app del dispositivo objetivo.
- **Leer del remoto:** Pulsa **Read Remote Clipboard**. banADB consultará el contenido actual del portapapeles del teléfono remoto y te lo mostrará en pantalla con un botón para copiarlo a tu propio portapapeles.

---

## 9. Terminal Interactiva y Scripts

Para usuarios avanzados, administradores de sistemas y desarrolladores:

- **Consola Shell:** Ubicada en la pestaña **Terminal**, te permite enviar cualquier comando POSIX o ADB shell directamente (`ls`, `top`, `logcat`, `dumpsys`, `am`, etc.).
- **Atajos frecuentes:** Botones de un toque para comandos habituales:
  - `pm list packages -3`: Lista todas las apps de terceros instaladas.
  - `dumpsys battery`: Muestra el volcado completo del subsistema de energía.
  - `getprop`: Consulta propiedades de compilación y hardware.
- **Scripts Preconfigurados:** En el panel de herramientas, la sección **Scripts** te permite ejecutar secuencias de automatización almacenadas con un solo clic.

---

## 10. Diagnóstico de Batería y Hardware

En la sección **Battery** y **Status** del panel de herramientas:
- **Batería:** Monitorea en tiempo real el porcentaje de carga, estado (cargando / descargando), temperatura del sensor térmico (en grados Celsius), voltaje en milivoltios y estado de degradación de la celda.
- **Hardware y OS:** Informa la marca del dispositivo, modelo comercial, versión de Android instalada, nivel de API del SDK, resolución nativa del panel táctil y tiempo transcurrido desde el último encendido (*uptime*).

---

## 11. Comportamiento en Segundo Plano y Desconexión

banADB está diseñado para no interrumpir tu trabajo cuando cambias de aplicación:

- **Servicio Persistente:** Mientras estés conectado a un dispositivo, se mantendrá un servicio en primer plano con una notificación en la barra de estado. Esto evita que Android cierre la conexión TCP si abres otra app o bloqueas la pantalla.
- **Gesto Atrás:** Al pulsar el botón de volver atrás desde la pantalla de control, la app te presentará tres opciones:
  - **Desconectar:** Cierra la sesión ADB y regresa a la lista de dispositivos.
  - **Mantener conectado:** Vuelve al menú principal pero conserva la conexión activa en segundo plano para que puedas reconectar al instante.
  - **Cancelar:** Permanece en la pantalla de control remoto.
- **Salida de la Aplicación:** Al presionar Atrás en la pantalla de inicio, se te solicitará confirmación antes de cerrar la aplicación para evitar salidas accidentales.
