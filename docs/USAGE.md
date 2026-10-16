# banADB User Guide

A complete, practical reference manual explaining how each feature in **banADB** works, what it does, and how to use it step by step.

---

## Table of Contents

1. [Initial Target Device Setup](#1-initial-target-device-setup)
2. [Connection & Pairing](#2-connection--pairing)
   - [Android 11+ Wireless Pairing (6-Digit Code)](#android-11-wireless-pairing-6-digit-code)
   - [Android 10 & Below Direct Connection (Port 5555)](#android-10--below-direct-connection-port-5555)
   - [Local Host Loopback Connection](#local-host-loopback-connection)
3. [Remote Cockpit (Live Control)](#3-remote-cockpit-live-control)
   - [D-Pad Mode](#d-pad-mode)
   - [Trackpad Mode](#trackpad-mode)
   - [Numpad & Text Injection](#numpad--text-injection)
   - [System Keys & Volume Bar](#system-keys--volume-bar)
4. [Remote Gallery](#4-remote-gallery)
5. [Remote File Manager](#5-remote-file-manager)
6. [Application Manager](#6-application-manager)
   - [Extracting APKs to Your Phone](#extracting-apks-to-your-phone)
   - [Installing APKs Remotely](#installing-apks-remotely)
   - [Lifecycle Controls (Launch, Force Stop, Clear Data, Uninstall)](#lifecycle-controls)
7. [Notification Mirror](#7-notification-mirror)
8. [Remote Clipboard Bridge](#8-remote-clipboard-bridge)
9. [Interactive Terminal & Scripts](#9-interactive-terminal--scripts)
10. [Battery & Device Diagnostics](#10-battery--device-diagnostics)
11. [Background Service & Disconnection](#11-background-service--disconnection)

---

## 1. Initial Target Device Setup

To control another Android device over Wi-Fi, both devices must be connected to the **same local network** (or one connected to the other's portable Wi-Fi hotspot).

On the device you want to control (the target):
1. Open **Settings** -> **About Phone**.
2. Tap the **Build Number** 7 times until you see a message confirming Developer Options are enabled.
3. Go to **Settings** -> **System** (or Additional Settings) -> **Developer Options**.
4. Enable **Wireless Debugging** (Depuración por Wi-Fi).
   - On devices running MIUI/HyperOS (Xiaomi), ColorOS (Oppo), or Realme UI, also toggle on **USB Debugging (Security Settings)** so the system allows incoming synthetic touch and key events.

---

## 2. Connection & Pairing

When opening banADB, the Home screen displays previously connected devices and the floating **+ Pair New Device** button.

### Android 11+ Wireless Pairing (6-Digit Code)
Android 11 and later enforces TLS encryption and dynamic pairing ports:

1. On the target device, open **Wireless Debugging** settings and tap **Pair device with pairing code**.
2. A popup will display an IP address, a pairing port (e.g. `192.168.1.45:41235`), and a 6-digit numeric code.
3. In banADB, tap **+ Pair New Device**:
   - **mDNS Auto-Discovery:** If your Wi-Fi router allows local multicast, the target phone will show up automatically in the list. Tap it, enter the 6-digit code, and tap **Pair Code**.
   - **Manual Entry:** If it does not show up automatically, expand **Manual IP & Port Entry**, enter the target IP, the specific pairing port shown on the screen, and the 6-digit code.
4. Tap **Pair**. Once paired, banADB stores the generated cryptographic keys and establishes the permanent ADB connection.

### Android 10 & Below Direct Connection (Port 5555)
Legacy Android versions or devices that have already been put into TCP/IP mode via `adb tcpip 5555` listen on port 5555:

1. Tap **+ Pair New Device** and switch to the **Android 10** tab.
2. Enter the device IP and port (defaults to `5555`).
3. Tap **Connect**. On the target phone's screen, an "Allow USB debugging?" dialog will appear; check **Always allow from this computer** and accept.

### Local Host Loopback Connection
banADB can connect to its own device to run scripts or automate local tasks:
1. In **+ Pair New Device**, switch to the **Local (Host)** tab.
2. Make sure Wireless Debugging is running on port 5555 on the phone.
3. Tap **Connect to 127.0.0.1:5555**.

---

## 3. Remote Cockpit (Live Control)

Connecting to a device opens the Remote Control dashboard. This screen provides tactile input tools, multimedia controls, and access to all remote utilities.

### D-Pad Mode
Ideal for Android TV boxes, media players, or navigating interface menus without a mouse cursor:
- **Directional Arrows:** Send `DPAD_UP`, `DPAD_DOWN`, `DPAD_LEFT`, and `DPAD_RIGHT` key events.
- **Center OK Button:** Triggers selection (`DPAD_CENTER` / `ENTER`).
- **Haptic Feedback:** Every button press emits subtle vibration to confirm the event was sent.

### Trackpad Mode
Converts your phone screen into a multi-touch touchpad for the target device:
- **Tap:** Sends a click event at the corresponding relative coordinates on the remote display.
- **Drag / Swipe:** Emits a continuous touch swipe across the remote screen.
- **Invert Toggle:** By default, dragging follows direct finger movement. Toggle "Invert" if you prefer inverted scrolling (like a laptop touchpad scroll wheel).

### Numpad & Text Injection
- **Numeric Keys (0-9):** Quick entry for PINs, passwords, and channels.
- **Text Bar:** Type or paste any string or URL and tap Send. banADB encodes the text and injects it into whatever input field is currently focused on the remote device via `input text`.

### System Keys & Volume Bar
- **System Navigation:**
  - **Back:** Sends the Android Back button event.
  - **Home:** Returns to the launcher home screen.
  - **Recents:** Opens the recent apps / multitasking switcher.
- **Bottom Bar:**
  - **Volume Down (Vol -)**
  - **Mute / Unmute**
  - **Volume Up (Vol +)**
  - **Play / Pause:** Global media playback toggle for audio and video apps.

---

## 4. Remote Gallery

Allows you to inspect photos, screenshots, and videos on the remote phone without transferring multi-gigabyte folders:

- **Automatic Directory Scanning:** Scans standard media paths including `DCIM/Camera`, `DCIM/Screenshots`, and `Pictures/`.
- **Category Filters:** Switch quickly between **All**, **Camera**, and **Screenshots**.
- **Lightweight Thumbnails:** Streams compressed image thumbnails over ADB so the gallery loads quickly without choking your Wi-Fi bandwidth.
- **Video Indicator:** Video files (`.mp4`, `.mkv`) display file size, timestamp, and a play overlay.
- **Fullscreen Preview:** Tap any picture to open a high-res inspection dialog showing exact resolution, file size, date, and a download button to save a copy locally.

---

## 5. Remote File Manager

A full-featured file manager operating on the remote filesystem (`/sdcard/`):

- **Directory Tree:** Tap folders to navigate downward; use the path breadcrumb bar at the top to navigate back up.
- **Folder Creation:** Tap the **+** folder icon in the top bar to create a new directory remotely.
- **Deletion:** Tap the trash can icon next to any file or directory. A safety confirmation dialog prevents accidental deletion.
- **File Preview:** Tapping images, text files, or logs downloads a temporary copy to your local device and opens it using your system's default viewer.
- **APK Installation:** Tapping any `.apk` file inside the file manager prompts you to install it immediately on the remote device via `pm install -r`.

---

## 6. Application Manager

Inspects and controls all packages installed on the remote device.

### Extracting APKs to Your Phone
If you want to backup or copy an app installed on the remote phone:
1. Find the app in the list (use the search bar to filter by app name or package identifier).
2. Tap the three-dot menu next to the application.
3. Select **Extract APK**.
4. banADB resolves the base path using `pm path`, pulls the APK file over ADB, and saves it in your local storage at `/sdcard/Download/banADB/`.

### Installing APKs Remotely
1. Tap the floating **Install APK** button in the lower-right corner.
2. Select any `.apk` file stored on your local phone.
3. banADB streams the package to the remote target's temporary directory and executes `pm install -r`. A status message confirms when installation finishes.

### Lifecycle Controls
From each app's contextual menu, you can also:
- **Launch App:** Starts the application's default launcher activity on the remote phone.
- **Force Stop:** Immediately kills all processes associated with the package (`am force-stop`).
- **Clear Data:** Wipes the app's cache and user databases (`pm clear`), resetting it to a clean state.
- **Uninstall:** Completely uninstalls the package from the device.

---

## 7. Notification Mirror

Monitors incoming active notifications from the remote phone without needing physical access to its display:

- **Notification Inspection:** Reads active notifications from the Android notification subsystem, extracting title, message text, originating app, and post time.
- **Category Tabs:** Filter by message type:
  - **Messages:** SMS, WhatsApp, Telegram, Signal, etc.
  - **Social:** Social media platforms.
  - **Email:** Gmail, Outlook, and mail clients.
  - **System:** Android OS status notifications (charging, downloads, connectivity).
- **Auto-Sync:** Toggle the switch at the top to automatically refresh active notifications every 5 seconds.
- **Actions:**
  - **Copy:** Copies the full notification content to your phone's clipboard.
  - **Dismiss:** Cancels the notification on the remote device.

---

## 8. Remote Clipboard Bridge

Share text, clipboard data, and long strings between devices:

- **Send to Remote:** Type or paste text into the input field and tap **Send to Remote Clipboard**. The text is immediately placed into the target device's clipboard, ready to paste in any app.
- **Read from Remote:** Tap **Read Remote Clipboard**. banADB queries the remote clipboard manager and displays the current string with a button to copy it locally.

---

## 9. Interactive Terminal & Scripts

Designed for advanced users, system administrators, and power users:

- **Shell Console:** Located in the **Terminal** tab, this lets you execute POSIX shell commands directly (`ls`, `top`, `logcat`, `dumpsys`, `am`, etc.).
- **Quick Action Chips:** One-tap chips for common diagnostic tasks:
  - `pm list packages -3`: Lists third-party installed packages.
  - `dumpsys battery`: Prints detailed power subsystem metrics.
  - `getprop`: Dumps system hardware and build properties.
- **Custom Scripts:** The **Scripts** tool lets you store and execute multi-command batch workflows with a single press.

---

## 10. Battery & Device Diagnostics

In the **Battery** and **Status** panels under Remote Feature Tools:
- **Battery:** Displays real-time battery level, charging/discharging state, thermal temperature in °C, voltage in millivolts, and battery health condition.
- **Hardware & OS:** Displays device manufacturer, marketing model, Android version, SDK API level, physical touchscreen resolution, and system uptime since last boot.

---

## 11. Background Service & Disconnection

banADB maintains persistent connections to avoid accidental drops:

- **Foreground Service:** While connected, a persistent notification runs in your phone's notification shade to ensure the Android system does not kill the TCP connection when you switch apps or lock your screen.
- **Back Navigation Handling:** Pressing the Back button from the Remote Control cockpit displays a confirmation dialog with three options:
  - **Disconnect:** Terminates the ADB session and returns to the home screen.
  - **Stay Connected:** Returns to the home device list while leaving the background connection active so you can re-enter immediately without re-authenticating.
  - **Cancel:** Dismisses the dialog and remains on the remote control screen.
- **Exit Confirmation:** Pressing Back on the Home screen prompts for confirmation before closing the app.
