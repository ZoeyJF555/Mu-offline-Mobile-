# Guía de Administración y Parches JSON (Mu Offline Mobile)

Este proyecto utiliza **persistencia en JSON plano (cero SQL)** tanto para los datos locales de jugadores (`accounts.json`, `heroes_<cuenta>.json`, `vault_<cuenta>.json`, `settings.json`, `mods.json`) como para la configuración global del servidor offline (`manifest.json`, `mapas.json`, `npcs.json`, `clases.json`, `items.json`, `eventos.json`).

---

## 1. Dónde viven los archivos de configuración

- **Base incluida dentro del APK:** `/app/src/main/assets/data/`
  - `manifest.json`: versión actual del parche y lista de archivos sincronizables.
  - `clases.json`: las 5 clases (`DK`, `DW`, `ELF`, `MG`, `DL`), puntos por nivel y evoluciones Nv.150 / Nv.400.
  - `mapas.json`: los 15 mapas clásicos + Arena Mod con `minLevel`, `warpLevel`, `expRate` (por defecto `0.35`), `dropZenRate` y `dropItemRate`.
  - `items.json`: alas S1/S2/S3, 5 joyas (`Bless`, `Soul`, `Chaos`, `Life`, `Creation`), mascotas DL (`Dark Raven`, `Dark Horse`) y talismanes.
  - `npcs.json`: NPCs por mapa (`Lorencia`, `Noria` Chaos Goblin, `Devias` Golden Archer, Baúl, Quest de Marlon).
  - `eventos.json`: Blood Castle, Devil Square, Chaos Castle e Invasión Dorada.
- **Copia editable en el teléfono:** `<filesDir>/mu_data/`

---

## 2. Cómo modificar tasas y NPCs directamente desde el APK

1. Abre el botón **Admin** desde el Menú de Personajes o el icono de escudo en el HUD dentro del juego.
2. En la pestaña **Rates Mapas**, selecciona cualquier mapa (`Lorencia`, `Atlans`, `Stadium`, etc.), ajusta `EXP Rate`, `Zen Rate` y `Drop (0..1)` y presiona **Guardar Rates en mapas.json**.
3. En la pestaña **Editor JSON**, puedes alternar entre `mapas.json` y `npcs.json`, editar el texto directamente y tocar **Aplicar JSON** o **Exportar .json** para compartirlo.

---

## 3. Cómo publicar parches remotos sin recompilar el APK

Cuando los jugadores tengan conexión a internet pueden descargar tus actualizaciones JSON desde un repositorio estático (por ejemplo, **GitHub Raw** o **GitHub Pages**):

1. Sube o actualiza los archivos de `/app/src/main/assets/data/` (`manifest.json`, `mapas.json`, `npcs.json`) en tu repositorio de GitHub.
2. Asegúrate de incrementar el campo `"version"` en `manifest.json`:
   ```json
   {
     "version": "6.3.2",
     "updatedAt": "2026-11-17",
     "notes": "Evento EXP x2 en Atlans y Lost Tower",
     "files": ["mapas.json", "npcs.json"]
   }
   ```
3. Copia el enlace **Raw** de `manifest.json`, por ejemplo:
   `https://raw.githubusercontent.com/<tu-usuario>/Mu-Offline-Mobile/main/app/src/main/assets/data/manifest.json`
4. Dentro de la app ve a **Admin -> Sync Nube**, pega la URL de `manifest.json` y toca **Descargar Actualización JSON**. La app descargará `mapas.json` y `npcs.json` desde la misma carpeta base y los guardará para seguir jugando offline.

---

## 4. Multijugador Local (Wi-Fi LAN hasta 12 jugadores + Bluetooth)

- **Wi-Fi LAN / Hotspot:** Abre el panel **LAN / BT**, crea una sala (Host) o conéctate por detección automática NSD / IP:Puerto (`47777`). Soporta hasta **12 jugadores simultáneos**, intercambio P2P, chat y eventos cooperativos (`Blood Castle`, `Devil Square`, `Chaos Castle`, `Invasión Dorada`).
- **Bluetooth:** Desde el mismo panel **LAN / BT** puedes otorgar los permisos de Bluetooth de Android 12+ (`BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN`) y revisar dispositivos vinculados cercanos para partidas locales o gamepads Bluetooth.
