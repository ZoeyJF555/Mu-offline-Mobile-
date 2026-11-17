# Mu Offline Mobile (Season 6 Ep 3 — Android APK + Versión Web)

RPG de acción estilo clásico **MU Online Season 6 Episodio 3** con **persistencia 100% en JSON plano (cero SQL)**, jugable tanto en **Android (APK nativo)** como en **Web (`/web/index.html`)**.

---

## Cómo jugar en Web (PC o Móvil en Navegador)

1. Abre `/web/index.html` directamente en Chrome / Edge / Firefox, o habilita **GitHub Pages** en tu repositorio (`Settings -> Pages -> Source: GitHub Actions`).
2. Cada push a `main` ejecuta `.github/workflows/deploy-web.yml` para publicar la versión Web automáticamente.
3. Controles Web: `WASD` / Flechas o clic para moverte y atacar, `Espacio` ataque básico, `1 / 2 / 3` habilidades, `Q / E` pociones HP/MP y `F` hablar con NPCs.

---

## Descargar el APK para jugar en Android

Cada actualización enviada al repositorio compila automáticamente el instalador `.apk` mediante `.github/workflows/build-apk.yml` y lo deja disponible en:

1. **Carpeta del repositorio:** `apk/Mu-Offline-Mobile.apk`
2. **Releases de GitHub:** Release **`latest`** → `Mu-Offline-Mobile.apk`
3. **Pestaña Actions:** Artifact `Mu-Offline-Mobile-APK`

---

## Características Principales (Season 6 Episodio 3)

- **Persistencia JSON Plana (Sin SQL):** Cuentas, 5 slots de personaje, baúl de 40 espacios, ajustes, mods y configuración global en `mapas.json`, `npcs.json`, `clases.json`, `items.json` y `manifest.json`. Ver `README_ADMIN.md`.
- **5 Clases + Evoluciones Nv.150 y Nv.400:** Dark Knight (`Blade Knight` / `Blade Master`), Dark Wizard (`Soul Master` / `Grand Master`), Fairy Elf (`Muse Elf` / `High Elf`), Magic Gladiator (`Duel Master` / `Magic Knight`, 7 pts/nv, sin casco, desbloqueo Nv.220+) y Dark Lord (`Lord Emperor` / `Empire Lord`, Dark Raven/Horse, desbloqueo Nv.300+).
- **15 Mapas Clásicos + Arena Mod:** `Lorencia`, `Noria`, `Devias`, `Dungeon 1-3`, `Atlans`, `Lost Tower 1-7`, `Kalima`, `Tarkan`, `Icarus` (requiere Alas), `Kanturu Ruins`, `Aida`, `CryWolf`, `Stadium` y `Arena Mod` con minimapa y coordenadas `(X, Y)`.
- **NPCs, Chaos Machine, Joyas y Eventos:** Herrero, Armaduras, Mago, Barmaid, Baúl Compartido, Quest de Marlon, Chaos Goblin (Alas S1/S2/S3 y `+10..+13`), Golden Archer (3 Talismanes -> Alas S2) y 5 Joyas (`Bless`, `Soul`, `Chaos`, `Life`, `Creation`).
- **Mods Offline & Panel Admin:** Activación/creación/exportación `.json` de mods y editor de tasas/JSON + sincronización remota de `manifest.json`.
- **Multijugador Local (Android APK):** Salas Wi-Fi LAN TCP/NSD hasta **12 jugadores**, intercambio P2P, eventos (`Blood Castle`, `Devil Square`, `Chaos Castle`, `Invasión Dorada`) y panel Bluetooth.
