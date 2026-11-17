# Integración de datos y modelos de OpenMu-Client-Babylon (MIT, © Ignies)

## Qué se usó
| Origen (Ignies) | Destino aquí | Entidad |
|---|---|---|
| `tools/Item.txt` (tabla Item.bmd, 663 ítems) | `web/mu/data/items.json` | Ítems: grupo, índice, tamaño, nivel, daño, requisitos, clases |
| `versions/shared/monsters/*/index.ts` (542 monstruos) | `web/mu/data/monsters.json` | Monstruos/NPC: id, nombre, nivel, HP, daño, defensa, modelo y escala |
| `public/game-assets/Player/*.glb` | `web/mu/models/Player/` | Cuerpo del jugador, armaduras, botas, guantes, cascos, alas |
| `public/game-assets/Monster/MonsterNN.glb` (nivel <= 40) | `web/mu/models/Monster/` | Monstruos de Lorencia/Noria/Devias/Dungeon |
| `public/game-assets/NPC/*.glb` | `web/mu/models/NPC/` | Algunos NPC de tienda |

Regla de modelo de monstruo: `Monster{modelType+1:02}.glb` (igual que Ignies).
`tools/extract_ignies_data.py` regenera los JSON desde un clon de Ignies.

## Hecho en esta rama
- `tools/build_world.py` descifra el mapa de Lorencia (`EncTerrain1.map/.obj/.att`, altura, luz) a `web/mu/world1/world1.json` (2.870 objetos, 109 tipos).
- `web/mu/viewer.html` (Three.js local en `web/vendor/three`, sin internet) dibuja: terreno con los 9 tiles reales y su mapa de luz, 2.845 objetos reales de `Object1`, un Dark Knight armado con piezas GLB reales y animacion `player_action_1`, y 4 monstruos reales (Bull Fighter, Hound, Budge Dragon, Spider).
- Capturas: `web/mu/screens/`. Abrir con `web/mu/viewer.html` (parametros: `dist`, `pitch`, `yaw`, `x`, `z`, `anim`, `face`).

## Limitaciones conocidas
- No hay `items.json`, `Monsters.json` ni `Models.json` en Ignies; se generaron con el extractor.
- 149 monstruos no tienen `modelType` (placeholder).
- 3 tipos de objeto sin modelo en Object1 (130, 131, 132); sin colision ni agua animada; sin cielo ni efectos.
- HUD original (`Data/Interface`, formatos OZT/OZJ) sin integrar; falta pegarlo al visor y a la app Android.
- Android: la app sigue siendo Kotlin 2D; hay que mostrar este visor dentro de un WebView o portar el render.
- `Data/` (1,1 GB) + `public/game-assets` (633 MB) no caben en APK/GitHub: solo Lorencia, jugador y monstruos de nivel <= 40.
