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

## Lo que NO existe como se pedía
- No hay `items.json`, `Monsters.json` ni `Models.json` en Ignies; los generé con el extractor.
- 149 monstruos no tienen `modelType` (usan clases propias); quedan con `model: null` -> placeholder.
- El terreno de Lorencia está cifrado (`EncTerrain1.map/.obj/.att`); falta un decodificador.
- `Data/` (1,1 GB) + `public/game-assets` (633 MB) no caben en un APK ni en GitHub; solo se empaquetó un subconjunto.

## Pendiente
Visor 3D (el juego actual es Kotlin 2D / canvas web), terreno de Lorencia, HUD de `Data/Interface`, efectos de habilidades.
