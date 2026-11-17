# Mu Offline Mobile (Android Nativo)

RPG de acción estilo clásico inspirado en **MU Online**, desarrollado de forma nativa para Android con **Kotlin**, **Jetpack Compose**, **SQLite local** y **Multijugador Wi-Fi LAN** sin internet ni servidores externos.

## Descargar el APK para jugar en Android

Cada actualización enviada al repositorio compila automáticamente el instalador `.apk` mediante GitHub Actions (`.github/workflows/build-apk.yml`) y lo deja disponible en tres lugares dentro del mismo repositorio:

1. **Carpeta directa del repositorio:** `apk/Mu-Offline-Mobile.apk` (tócalo y pulsa **Raw / Descargar**).
2. **Sección Releases (lado derecho del repositorio en GitHub):** Release **`latest`** → `Mu-Offline-Mobile.apk`.
3. **Pestaña Actions:** Última ejecución de **Build Android APK** → Artifact `Mu-Offline-Mobile-APK`.

> **Instalación en Android:** Descarga `Mu-Offline-Mobile.apk` en tu teléfono, ábrelo desde tu gestor de archivos y habilita *Instalar aplicaciones desconocidas* si Android te lo solicita la primera vez.

## Características Principales
- **Portal Offline SQLite:** Cuentas locales con hash SHA-256 + salt, guardado automático de múltiples héroes, inventario de 24 espacios, joyas y resets.
- **3 Clases Jugables:** Dark Knight, Dark Wizard y Fairy Elf con habilidades activas, tiempos de enfriamiento y efectos visuales.
- **5 Mapas & Lorencia Segura:** Lorencia (con 4 NPCs y fuente central), Dungeon, Devias, Noria y Arena Mod.
- **Chaos Machine & Joyas:** Mejoras con Jewel of Bless / Soul y combinaciones `+10` a `+13` y creación de Alas Nivel 2.
- **Gestor de Mods Offline:** Activación de mods integrados, creador personalizado en el móvil e importación/exportación `.json`.
- **Wi-Fi LAN Multijugador:** Host TCP integrado + descubrimiento NSD (`_muoffline._tcp.`), eventos cooperativos e intercambio P2P.
- **Controles:** Joystick táctil virtual, tocar para mover/atacar, **MU Helper** (auto-caza), modo horizontal y soporte para **Gamepad físico**.
