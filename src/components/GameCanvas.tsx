import React, { useEffect, useRef } from 'react';
import * as THREE from 'three';
import { RoomEnvironment } from 'three/addons/environments/RoomEnvironment.js';
import type {
  FloatingText,
  GroundDrop,
  HeroSave,
  LanBossState,
  LanPlayer,
  LiveMonster,
  MapId,
  SkillVfx,
} from '../types/game';
import { createContactShadowTexture, createFloatingTextSprite, updateLabelSprite } from '../three/muTextures';
import {
  buildMapEnvironment,
  createDropMesh,
  createHeroRig,
  createMonsterRig,
  LORENCIA_NPCS,
  sceneToWorldX,
  sceneToWorldY,
  worldToSceneX,
  worldToSceneZ,
  type HeroRig,
  type LorenciaNpcDef,
  type MapEnvironmentResult,
  type MonsterRig,
} from '../three/muModels';

interface GameCanvasProps {
  hero: HeroSave;
  monsters: LiveMonster[];
  drops: GroundDrop[];
  floatingTexts: FloatingText[];
  vfxList: SkillVfx[];
  lanPlayers: LanPlayer[];
  lanBosses: LanBossState[];
  joystickRef: React.MutableRefObject<{ x: number; y: number }>;
  onTapWorld: (worldX: number, worldY: number) => void;
  onInteractNpc?: (npc: LorenciaNpcDef) => void;
  onMoveDelta: (dx: number, dy: number) => void;
  onGamepadUpdate: (name: string | null, attackPressed: boolean, skillIdx: number | null) => void;
}

interface Active3DVfx {
  id: string;
  skillId: string;
  group: THREE.Group;
  createdAt: number;
  durationMs: number;
  ring?: THREE.Mesh;
  orbiters?: THREE.Mesh[];
}

interface Active3DFloating {
  id: string;
  sprite: THREE.Sprite;
  baseY: number;
  createdAt: number;
}

const _raycaster = new THREE.Raycaster();
const _ndc = new THREE.Vector2();
const _groundPlane = new THREE.Plane(new THREE.Vector3(0, 1, 0), 0);
const _hitPoint = new THREE.Vector3();

const MAP_SKY_COLORS: Record<MapId, number> = {
  lorencia: 0x0f151d,
  dungeon: 0x090712,
  devias: 0x162436,
  noria: 0x0b1c17,
  custom: 0x1a0b10,
};

function detectSoftwareWebGL(): boolean {
  try {
    const probe = document.createElement('canvas');
    const gl = probe.getContext('webgl2') || probe.getContext('webgl');
    if (!gl) return true;
    const ext = gl.getExtension('WEBGL_debug_renderer_info');
    const param = ext ? ext.UNMASKED_RENDERER_WEBGL : gl.RENDERER;
    const gpu = String(gl.getParameter(param) || '');
    gl.getExtension('WEBGL_lose_context')?.loseContext();
    return /SwiftShader|llvmpipe|softpipe|Software|Basic Render/i.test(gpu);
  } catch {
    return true;
  }
}

export const GameCanvas: React.FC<GameCanvasProps> = ({
  hero,
  monsters,
  drops,
  floatingTexts,
  vfxList,
  lanPlayers,
  lanBosses,
  joystickRef,
  onTapWorld,
  onInteractNpc,
  onMoveDelta,
  onGamepadUpdate,
}) => {
  const mountRef = useRef<HTMLDivElement | null>(null);
  const keysRef = useRef<Set<string>>(new Set());
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null);
  const heroPrevPosRef = useRef({ x: hero.x, y: hero.y, angle: 0, moving: false });

  const callbacksRef = useRef({ onTapWorld, onInteractNpc, onMoveDelta, onGamepadUpdate });
  callbacksRef.current = { onTapWorld, onInteractNpc, onMoveDelta, onGamepadUpdate };

  const stateRef = useRef({
    hero,
    monsters,
    drops,
    floatingTexts,
    vfxList,
    lanPlayers,
    lanBosses,
  });
  stateRef.current = {
    hero,
    monsters,
    drops,
    floatingTexts,
    vfxList,
    lanPlayers,
    lanBosses,
  };

  useEffect(() => {
    const down = (e: KeyboardEvent) => {
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName || '')) return;
      keysRef.current.add(e.key.toLowerCase());
    };
    const up = (e: KeyboardEvent) => {
      keysRef.current.delete(e.key.toLowerCase());
    };
    window.addEventListener('keydown', down);
    window.addEventListener('keyup', up);
    return () => {
      window.removeEventListener('keydown', down);
      window.removeEventListener('keyup', up);
    };
  }, []);

  useEffect(() => {
    const container = mountRef.current;
    if (!container) return;

    const isSoftwareGl = detectSoftwareWebGL();
    const renderer = new THREE.WebGLRenderer({
      antialias: !isSoftwareGl,
      powerPreference: 'high-performance',
    });
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.15;
    renderer.setPixelRatio(isSoftwareGl ? 1 : Math.min(window.devicePixelRatio || 1, 2));
    renderer.shadowMap.enabled = !isSoftwareGl;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;

    const rect = container.getBoundingClientRect();
    const initW = Math.max(320, rect.width || window.innerWidth);
    const initH = Math.max(320, rect.height || window.innerHeight);
    renderer.setSize(initW, initH);
    renderer.domElement.style.width = '100%';
    renderer.domElement.style.height = '100%';
    renderer.domElement.style.display = 'block';
    container.appendChild(renderer.domElement);

    const scene = new THREE.Scene();
    const pmremGenerator = new THREE.PMREMGenerator(renderer);
    const envTex = pmremGenerator.fromScene(new RoomEnvironment(), 0.04).texture;
    scene.environment = envTex;
    pmremGenerator.dispose();

    const camera = new THREE.PerspectiveCamera(initW < initH ? 52 : 39, initW / initH, 0.5, 240);
    cameraRef.current = camera;

    // Warm/cool MU 3-point lighting rig
    const hemiLight = new THREE.HemisphereLight(0x93c5fd, 0x292524, 0.85);
    scene.add(hemiLight);

    const keyLight = new THREE.DirectionalLight(0xffedd5, 1.85);
    keyLight.castShadow = !isSoftwareGl;
    keyLight.shadow.mapSize.set(1024, 1024);
    keyLight.shadow.camera.near = 2;
    keyLight.shadow.camera.far = 90;
    const d = 28;
    keyLight.shadow.camera.left = -d;
    keyLight.shadow.camera.right = d;
    keyLight.shadow.camera.top = d;
    keyLight.shadow.camera.bottom = -d;
    keyLight.shadow.bias = -0.0005;
    keyLight.shadow.normalBias = 0.02;
    scene.add(keyLight);
    scene.add(keyLight.target);

    const rimLight = new THREE.DirectionalLight(0x60a5fa, 0.55);
    rimLight.position.set(-18, 16, -18);
    scene.add(rimLight);

    const maxAnisotropy = renderer.capabilities.getMaxAnisotropy();
    const shadowTex = createContactShadowTexture();

    let currentMapId: MapId | null = null;
    let mapEnv: MapEnvironmentResult | null = null;

    const rebuildMapIfNeeded = (mapId: MapId) => {
      if (currentMapId === mapId && mapEnv) return;
      if (mapEnv) {
        scene.remove(mapEnv.group);
        mapEnv.disposables.forEach((item) => item.dispose());
      }
      currentMapId = mapId;
      const skyHex = MAP_SKY_COLORS[mapId];
      scene.background = new THREE.Color(skyHex);
      scene.fog = new THREE.FogExp2(skyHex, 0.0115);
      mapEnv = buildMapEnvironment(mapId, maxAnisotropy);
      scene.add(mapEnv.group);
    };

    let localHeroRig: HeroRig | null = null;
    let localHeroSig = '';
    const monsterRigs = new Map<string, MonsterRig>();
    const lanBossRigs = new Map<string, MonsterRig>();
    const lanPlayerRigs = new Map<string, HeroRig>();
    const dropMeshes = new Map<string, THREE.Group>();
    const activeVfx = new Map<string, Active3DVfx>();
    const activeFloats = new Map<string, Active3DFloating>();

    const disposeObjectGroup = (obj: THREE.Object3D) => {
      obj.traverse((child) => {
        if ((child as THREE.Mesh).geometry) {
          (child as THREE.Mesh).geometry.dispose();
        }
        const mat = (child as THREE.Mesh).material;
        if (Array.isArray(mat)) {
          mat.forEach((m) => m.dispose());
        } else if (mat) {
          mat.dispose();
        }
      });
    };

    const spawn3DVfx = (vfx: SkillVfx): Active3DVfx => {
      const group = new THREE.Group();
      const sx = worldToSceneX(vfx.targetX);
      const sz = worldToSceneZ(vfx.targetY);
      group.position.set(sx, 0.15, sz);

      const radius3d = Math.max(1.2, vfx.radius / 14);
      const col = new THREE.Color(vfx.color).getHex();

      const ring = new THREE.Mesh(
        new THREE.RingGeometry(radius3d * 0.45, radius3d * 0.85, 28),
        new THREE.MeshBasicMaterial({
          color: col,
          transparent: true,
          opacity: 0.85,
          side: THREE.DoubleSide,
          depthWrite: false,
        })
      );
      ring.rotation.x = -Math.PI / 2;
      group.add(ring);

      const orbiters: THREE.Mesh[] = [];
      if (vfx.skillId === 'evil-spirit' || vfx.skillId === 'twisting-slash' || vfx.skillId === 'ice-storm') {
        const count = vfx.skillId === 'evil-spirit' ? 6 : 5;
        const orbMat = new THREE.MeshStandardMaterial({
          color: vfx.skillId === 'evil-spirit' ? 0x1e1b4b : col,
          emissive: col,
          emissiveIntensity: 1.5,
          roughness: 0.2,
        });
        const orbGeo =
          vfx.skillId === 'twisting-slash'
            ? new THREE.BoxGeometry(1.1, 0.14, 0.28)
            : new THREE.OctahedronGeometry(0.35, 0);
        for (let i = 0; i < count; i++) {
          const m = new THREE.Mesh(orbGeo, orbMat);
          const ang = (i / count) * Math.PI * 2;
          m.position.set(Math.cos(ang) * radius3d * 0.7, 1.0, Math.sin(ang) * radius3d * 0.7);
          group.add(m);
          orbiters.push(m);
        }
      } else if (vfx.skillId === 'meteorite' || vfx.skillId === 'rageful-blow') {
        const core = new THREE.Mesh(
          new THREE.DodecahedronGeometry(radius3d * 0.42, 0),
          new THREE.MeshStandardMaterial({
            color: col,
            emissive: col,
            emissiveIntensity: 1.6,
            transparent: true,
            opacity: 0.9,
          })
        );
        core.position.y = 2.4;
        group.add(core);
        orbiters.push(core);
      } else if (vfx.skillId === 'triple-shot' || vfx.skillId === 'ice-arrow') {
        const hx = worldToSceneX(vfx.x);
        const hz = worldToSceneZ(vfx.y);
        const arrowMat = new THREE.MeshBasicMaterial({ color: col });
        for (let i = -1; i <= 1; i++) {
          const arrow = new THREE.Mesh(new THREE.ConeGeometry(0.18, 1.2, 5), arrowMat);
          arrow.position.set((hx - sx) * 0.5 + i * 0.65, 1.2, (hz - sz) * 0.5 + i * 0.45);
          arrow.rotation.x = Math.PI / 2;
          group.add(arrow);
          orbiters.push(arrow);
        }
      }

      scene.add(group);
      return {
        id: vfx.id,
        skillId: vfx.skillId,
        group,
        createdAt: vfx.createdAt,
        durationMs: vfx.durationMs,
        ring,
        orbiters,
      };
    };

    let camInitialized = false;
    const handleResize = () => {
      if (!container) return;
      const r = container.getBoundingClientRect();
      const w = Math.max(320, r.width);
      const h = Math.max(320, r.height);
      camera.fov = w < h ? 52 : 39;
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      renderer.setSize(w, h);
    };
    window.addEventListener('resize', handleResize);

    if (!isSoftwareGl) {
      renderer.compile(scene, camera);
    }

    let frameId = 0;
    let disposed = false;
    let lastTime = performance.now();
    let lastGamepadAttack = 0;

    function afterDraw(cb: () => void) {
      const gl = renderer.getContext() as WebGL2RenderingContext;
      const sync = gl.fenceSync ? gl.fenceSync(gl.SYNC_GPU_COMMANDS_COMPLETE, 0) : null;
      if (!sync) {
        frameId = window.setTimeout(cb, 80);
        return;
      }
      gl.flush();
      const poll = () => {
        if (disposed) {
          gl.deleteSync(sync);
          return;
        }
        const st = gl.getSyncParameter(sync, gl.SYNC_STATUS);
        if (st === gl.UNSIGNALED) {
          frameId = window.setTimeout(poll, 5);
        } else {
          gl.deleteSync(sync);
          frameId = window.setTimeout(cb, 80);
        }
      };
      frameId = window.setTimeout(poll, 5);
    }

    const tick = () => {
      if (disposed) return;
      const now = performance.now();
      const dt = Math.min(0.05, (now - lastTime) / 1000);
      lastTime = now;

      // 1. Movement & Gamepad Inputs
      let vx = joystickRef.current.x;
      let vy = joystickRef.current.y;
      const keys = keysRef.current;
      if (keys.has('w') || keys.has('arrowup')) vy -= 1;
      if (keys.has('s') || keys.has('arrowdown')) vy += 1;
      if (keys.has('a') || keys.has('arrowleft')) vx -= 1;
      if (keys.has('d') || keys.has('arrowright')) vx += 1;

      const pads = navigator.getGamepads ? navigator.getGamepads() : [];
      const activePad = pads?.[0];
      if (activePad) {
        if (Math.abs(activePad.axes[0] || 0) > 0.2) vx += activePad.axes[0];
        if (Math.abs(activePad.axes[1] || 0) > 0.2) vy += activePad.axes[1];
        let skillIdx: number | null = null;
        if (activePad.buttons[1]?.pressed) skillIdx = 0;
        else if (activePad.buttons[2]?.pressed) skillIdx = 1;
        else if (activePad.buttons[3]?.pressed) skillIdx = 2;
        const atk = !!activePad.buttons[0]?.pressed && now - lastGamepadAttack > 260;
        if (atk) lastGamepadAttack = now;
        callbacksRef.current.onGamepadUpdate(activePad.id, atk, skillIdx);
      } else {
        callbacksRef.current.onGamepadUpdate(null, false, null);
      }

      const mag = Math.hypot(vx, vy);
      if (mag > 0.08) {
        const clamped = Math.min(1, mag);
        callbacksRef.current.onMoveDelta((vx / mag) * clamped * dt, (vy / mag) * clamped * dt);
      }

      const {
        hero: curHero,
        monsters: curMonsters,
        drops: curDrops,
        floatingTexts: curTexts,
        vfxList: curVfx,
        lanPlayers: curLanPlayers,
        lanBosses: curLanBosses,
      } = stateRef.current;

      // 2. Map environment & Torch flickering
      rebuildMapIfNeeded(curHero.mapId);
      if (mapEnv) {
        mapEnv.torchFlames.forEach((flame, idx) => {
          const flick = 0.9 + Math.sin(now * 0.012 + idx * 1.7) * 0.18;
          flame.scale.set(flick, flick * 1.25, flick);
          flame.rotation.y += dt * 2.2;
        });
        mapEnv.torchLights.forEach((pl, idx) => {
          pl.intensity = 17 + Math.sin(now * 0.014 + idx * 2.1) * 3.5;
        });
        if (mapEnv.fountainWater) {
          mapEnv.fountainWater.position.y = 0.72 + Math.sin(now * 0.004) * 0.04;
        }
      }

      // 3. Sync Local 3D Hero
      const wingsTier = curHero.equipment.wings ? (curHero.equipment.wings.rarity === 'mythic' ? 2 : 1) : 0;
      const weaponPlus = curHero.equipment.weapon?.plus || 0;
      const heroSig = `${curHero.id}-${curHero.classId}-${wingsTier}-${weaponPlus}-${curHero.level}`;
      if (!localHeroRig || localHeroSig !== heroSig) {
        if (localHeroRig) {
          localHeroRig.disposed = true;
          scene.remove(localHeroRig.root);
          disposeObjectGroup(localHeroRig.root);
        }
        localHeroRig = createHeroRig(
          curHero.name,
          curHero.classId,
          curHero.level,
          wingsTier,
          weaponPlus,
          false,
          shadowTex,
          maxAnisotropy
        );
        scene.add(localHeroRig.root);
        localHeroSig = heroSig;
      }

      const hx = worldToSceneX(curHero.x);
      const hz = worldToSceneZ(curHero.y);
      const prevH = heroPrevPosRef.current;
      const dx = curHero.x - prevH.x;
      const dy = curHero.y - prevH.y;
      const movedDist = Math.hypot(dx, dy);
      if (movedDist > 0.25) {
        prevH.angle = Math.atan2(dx, dy);
        prevH.moving = true;
      } else {
        prevH.moving = false;
      }
      prevH.x = curHero.x;
      prevH.y = curHero.y;

      localHeroRig.root.position.set(hx, 0, hz);
      localHeroRig.bodyGroup.rotation.y = prevH.angle;

      // Hero walk / wings / +13 aura animation
      if (localHeroRig.bmdLoadedGroup) {
        localHeroRig.bodyGroup.position.y = prevH.moving ? Math.abs(Math.sin(now * 0.015)) * 0.1 : Math.sin(now * 0.003) * 0.02;
        localHeroRig.bodyGroup.rotation.z = prevH.moving ? Math.sin(now * 0.015) * 0.05 : 0;
      } else {
        const walkPhase = prevH.moving ? Math.sin(now * 0.016) * 0.6 : 0;
        localHeroRig.leftLeg.rotation.x = walkPhase;
        localHeroRig.rightLeg.rotation.x = -walkPhase;
        localHeroRig.leftArm.rotation.x = -walkPhase * 0.7;
        localHeroRig.rightArm.rotation.x = walkPhase * 0.7;
      }
      if (localHeroRig.wingsGroup.visible) {
        const flap = Math.sin(now * 0.008) * 0.24;
        localHeroRig.leftWing.rotation.y = 0.25 + flap;
        localHeroRig.rightWing.rotation.y = -0.25 - flap;
      }
      if (weaponPlus >= 7) {
        localHeroRig.auraRing.rotation.z += dt * 1.4;
      }

      // 4. Classic MU Camera + Shadow light follow
      const targetCamX = hx;
      const targetCamY = 19.5;
      const targetCamZ = hz + 18.5;
      if (!camInitialized) {
        camera.position.set(targetCamX, targetCamY, targetCamZ);
        camInitialized = true;
      } else {
        camera.position.x = THREE.MathUtils.damp(camera.position.x, targetCamX, 10, dt);
        camera.position.y = THREE.MathUtils.damp(camera.position.y, targetCamY, 10, dt);
        camera.position.z = THREE.MathUtils.damp(camera.position.z, targetCamZ, 10, dt);
      }
      camera.lookAt(
        THREE.MathUtils.damp(camera.position.x, hx, 14, dt),
        1.1,
        THREE.MathUtils.damp(camera.position.z - 18.5, hz, 14, dt)
      );

      keyLight.position.set(hx + 16, 28, hz + 14);
      keyLight.target.position.set(hx, 0, hz);

      // 5. Sync Remote LAN Players
      const activeLanIds = new Set<string>();
      for (const peer of curLanPlayers) {
        if (peer.id === curHero.id || peer.mapId !== curHero.mapId) continue;
        activeLanIds.add(peer.id);
        let rig = lanPlayerRigs.get(peer.id);
        if (!rig || rig.wingsTier !== peer.wingsTier || rig.weaponPlus !== peer.weaponPlus) {
          if (rig) {
            rig.disposed = true;
            scene.remove(rig.root);
            disposeObjectGroup(rig.root);
          }
          rig = createHeroRig(
            peer.name,
            peer.classId,
            peer.level,
            peer.wingsTier,
            peer.weaponPlus,
            true,
            shadowTex,
            maxAnisotropy
          );
          scene.add(rig.root);
          lanPlayerRigs.set(peer.id, rig);
        }
        rig.root.position.set(worldToSceneX(peer.x), 0, worldToSceneZ(peer.y));
        rig.bodyGroup.rotation.y = peer.dir;
        if (rig.wingsGroup.visible) {
          const flap = Math.sin(now * 0.008) * 0.24;
          rig.leftWing.rotation.y = 0.25 + flap;
          rig.rightWing.rotation.y = -0.25 - flap;
        }
      }
      for (const [id, rig] of lanPlayerRigs.entries()) {
        if (!activeLanIds.has(id)) {
          rig.disposed = true;
          scene.remove(rig.root);
          disposeObjectGroup(rig.root);
          lanPlayerRigs.delete(id);
        }
      }

      // 6. Sync Monsters
      const activeMobIds = new Set<string>();
      for (const m of curMonsters) {
        if (m.hp <= 0) continue;
        activeMobIds.add(m.uid);
        let rig = monsterRigs.get(m.uid);
        if (!rig) {
          rig = createMonsterRig(m.templateId, m.name, m.level, m.color, m.hp, m.maxHp, m.isBoss, shadowTex, maxAnisotropy);
          scene.add(rig.root);
          monsterRigs.set(m.uid, rig);
        }
        const mx = worldToSceneX(m.x);
        const mz = worldToSceneZ(m.y);
        rig.root.position.set(mx, 0, mz);
        rig.bodyGroup.rotation.y = Math.atan2(hx - mx, hz - mz);
        rig.bodyGroup.position.y = Math.sin(now * 0.005 + m.x) * 0.06;

        if (rig.leftWing && rig.rightWing) {
          const flap = Math.sin(now * 0.01) * 0.35;
          rig.leftWing.rotation.z = flap;
          rig.rightWing.rotation.z = -flap;
        }

        const isFlashing = m.hitFlashUntil > now;
        for (const hMat of rig.hitFlashMaterials) {
          if (isFlashing) {
            hMat.emissive.setHex(0xffffff);
            hMat.emissiveIntensity = 1.1;
          } else if (rig.bmdLoadedGroup) {
            hMat.emissive.setHex(0x000000);
            hMat.emissiveIntensity = 0;
          } else {
            hMat.emissive.setHex(rig.baseColor);
            hMat.emissiveIntensity = rig.isBoss ? 0.3 : 0.08;
          }
        }

        updateLabelSprite(
          rig.label,
          `${m.name} Nv${m.level}`,
          m.isBoss ? '#fde68a' : '#f1f5f9',
          m.hp / Math.max(1, m.maxHp),
          m.isBoss
        );
      }
      for (const [uid, rig] of monsterRigs.entries()) {
        if (!activeMobIds.has(uid)) {
          rig.disposed = true;
          scene.remove(rig.root);
          disposeObjectGroup(rig.root);
          monsterRigs.delete(uid);
        }
      }

      // 7. Sync Shared LAN Bosses
      const activeLanBossIds = new Set<string>();
      for (const lb of curLanBosses) {
        if (lb.mapId !== curHero.mapId || lb.hp <= 0) continue;
        activeLanBossIds.add(lb.id);
        let rig = lanBossRigs.get(lb.id);
        if (!rig) {
          rig = createMonsterRig(lb.id, `[LAN] ${lb.name}`, 99, '#f59e0b', lb.hp, lb.maxHp, true, shadowTex, maxAnisotropy);
          scene.add(rig.root);
          lanBossRigs.set(lb.id, rig);
        }
        rig.root.position.set(worldToSceneX(lb.x), 0, worldToSceneZ(lb.y));
        updateLabelSprite(rig.label, `[LAN] ${lb.name}`, '#7dd3fc', lb.hp / Math.max(1, lb.maxHp), true);
      }
      for (const [id, rig] of lanBossRigs.entries()) {
        if (!activeLanBossIds.has(id)) {
          rig.disposed = true;
          scene.remove(rig.root);
          disposeObjectGroup(rig.root);
          lanBossRigs.delete(id);
        }
      }

      // 8. Sync Ground Drops
      const activeDropIds = new Set<string>();
      for (const drop of curDrops) {
        activeDropIds.add(drop.uid);
        let mesh = dropMeshes.get(drop.uid);
        if (!mesh) {
          mesh = createDropMesh(drop);
          scene.add(mesh);
          dropMeshes.set(drop.uid, mesh);
        }
        mesh.position.set(worldToSceneX(drop.x), Math.sin(now * 0.005 + drop.x) * 0.12, worldToSceneZ(drop.y));
        mesh.children[0].rotation.y += dt * 1.8;
      }
      for (const [uid, mesh] of dropMeshes.entries()) {
        if (!activeDropIds.has(uid)) {
          scene.remove(mesh);
          disposeObjectGroup(mesh);
          dropMeshes.delete(uid);
        }
      }

      // 9. Sync 3D Skill VFX
      for (const vfx of curVfx) {
        const age = now - vfx.createdAt;
        if (age < 0 || age > vfx.durationMs) continue;
        if (!activeVfx.has(vfx.id)) {
          activeVfx.set(vfx.id, spawn3DVfx(vfx));
        }
      }
      for (const [id, fx] of activeVfx.entries()) {
        const age = now - fx.createdAt;
        if (age > fx.durationMs) {
          scene.remove(fx.group);
          disposeObjectGroup(fx.group);
          activeVfx.delete(id);
          continue;
        }
        const p = age / fx.durationMs;
        if (fx.ring) {
          const s = 0.4 + p * 0.9;
          fx.ring.scale.set(s, s, 1);
          (fx.ring.material as THREE.MeshBasicMaterial).opacity = (1 - p) * 0.9;
        }
        fx.group.rotation.y += dt * (fx.skillId === 'twisting-slash' ? 14 : 7);
        if (fx.skillId === 'meteorite' || fx.skillId === 'rageful-blow') {
          fx.orbiters?.forEach((m) => {
            m.position.y = Math.max(0.4, 2.6 * (1 - p));
          });
        }
      }

      // 10. Sync Floating Damage / Loot Texts
      for (const ft of curTexts) {
        const age = now - ft.createdAt;
        if (age < 0 || age > 950) continue;
        if (!activeFloats.has(ft.id)) {
          const sprite = createFloatingTextSprite(ft.text, ft.color, !!ft.isCrit);
          sprite.position.set(worldToSceneX(ft.x), 2.8, worldToSceneZ(ft.y));
          scene.add(sprite);
          activeFloats.set(ft.id, { id: ft.id, sprite, baseY: 2.8, createdAt: ft.createdAt });
        }
      }
      for (const [id, fl] of activeFloats.entries()) {
        const age = now - fl.createdAt;
        if (age > 950) {
          scene.remove(fl.sprite);
          disposeObjectGroup(fl.sprite);
          activeFloats.delete(id);
          continue;
        }
        const p = age / 950;
        fl.sprite.position.y = fl.baseY + p * 2.4;
        (fl.sprite.material as THREE.SpriteMaterial).opacity = 1 - p;
      }

      renderer.render(scene, camera);

      if (isSoftwareGl) {
        afterDraw(tick);
      } else {
        frameId = requestAnimationFrame(tick);
      }
    };

    if (isSoftwareGl) {
      afterDraw(tick);
    } else {
      frameId = requestAnimationFrame(tick);
    }

    return () => {
      disposed = true;
      cancelAnimationFrame(frameId);
      clearTimeout(frameId);
      window.removeEventListener('resize', handleResize);
      if (localHeroRig) {
        localHeroRig.disposed = true;
        disposeObjectGroup(localHeroRig.root);
      }
      monsterRigs.forEach((r) => {
        r.disposed = true;
        disposeObjectGroup(r.root);
      });
      lanBossRigs.forEach((r) => {
        r.disposed = true;
        disposeObjectGroup(r.root);
      });
      lanPlayerRigs.forEach((r) => {
        r.disposed = true;
        disposeObjectGroup(r.root);
      });
      dropMeshes.forEach((dMesh) => disposeObjectGroup(dMesh));
      activeVfx.forEach((v) => disposeObjectGroup(v.group));
      activeFloats.forEach((f) => disposeObjectGroup(f.sprite));
      if (mapEnv) mapEnv.disposables.forEach((dItem) => dItem.dispose());
      shadowTex.dispose();
      envTex.dispose();
      renderer.dispose();
      if (renderer.domElement.parentNode === container) {
        container.removeChild(renderer.domElement);
      }
    };
  }, [joystickRef]);

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    const container = mountRef.current;
    const camera = cameraRef.current;
    if (!container || !camera) return;
    const rect = container.getBoundingClientRect();
    _ndc.x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
    _ndc.y = -((e.clientY - rect.top) / rect.height) * 2 + 1;
    _raycaster.setFromCamera(_ndc, camera);
    if (_raycaster.ray.intersectPlane(_groundPlane, _hitPoint)) {
      const wx = sceneToWorldX(_hitPoint.x);
      const wy = sceneToWorldY(_hitPoint.z);
      const curMap = stateRef.current.hero.mapId;
      const availableNpcs =
        curMap === 'lorencia'
          ? LORENCIA_NPCS
          : LORENCIA_NPCS.filter((n) => n.id === 'chaos' || n.id === 'barmaid');
      const clickedNpc = availableNpcs.find((n) => Math.hypot(wx - n.wx, wy - n.wy) < 32);
      if (clickedNpc && callbacksRef.current.onInteractNpc) {
        callbacksRef.current.onInteractNpc(clickedNpc);
        return;
      }
      callbacksRef.current.onTapWorld(wx, wy);
    }
  };

  return (
    <div
      ref={mountRef}
      onPointerDown={handlePointerDown}
      className="w-full h-full touch-none-game cursor-crosshair select-none"
    />
  );
};
