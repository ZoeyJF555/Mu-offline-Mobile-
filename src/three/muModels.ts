import * as THREE from 'three';
import type { ClassId, GroundDrop, MapId } from '../types/game';
import { createLabelSprite, createMapTextures, createMetalArmorTexture } from './muTextures';
import {
  buildGroupFromBmd,
  loadMonsterBmdGroup,
  loadNpcBmdGroup,
  loadPlayerHeroBmdGroup,
} from './bmdLoader';

export interface LorenciaNpcDef {
  id: 'smith' | 'wizard' | 'barmaid' | 'chaos';
  name: string;
  subtitle: string;
  wx: number;
  wy: number;
  rotY: number;
}

export const LORENCIA_NPCS: LorenciaNpcDef[] = [
  { id: 'smith', name: 'Hanzo, Blacksmith', subtitle: 'Tienda Armas', wx: 425, wy: 438, rotY: 0.72 },
  { id: 'wizard', name: 'Pasi Wizard', subtitle: 'Tienda Magia', wx: 536, wy: 438, rotY: -0.72 },
  { id: 'barmaid', name: 'Liaman Barmaid', subtitle: 'Pociones', wx: 425, wy: 526, rotY: 2.35 },
  { id: 'chaos', name: 'Chaos Goblin', subtitle: 'Chaos Machine', wx: 536, wy: 526, rotY: -2.35 },
];

export interface HeroRig {
  root: THREE.Group;
  bodyGroup: THREE.Group;
  leftArm: THREE.Group;
  rightArm: THREE.Group;
  leftLeg: THREE.Mesh;
  rightLeg: THREE.Mesh;
  wingsGroup: THREE.Group;
  leftWing: THREE.Group;
  rightWing: THREE.Group;
  auraRing: THREE.Mesh;
  weaponGlowMat: THREE.MeshStandardMaterial;
  armorMat: THREE.MeshStandardMaterial;
  label: THREE.Sprite;
  classId: ClassId;
  wingsTier: number;
  weaponPlus: number;
  bmdLoadedGroup?: THREE.Group;
  disposed?: boolean;
}

export interface MonsterRig {
  root: THREE.Group;
  bodyGroup: THREE.Group;
  leftWing?: THREE.Mesh;
  rightWing?: THREE.Mesh;
  primaryMat: THREE.MeshStandardMaterial;
  hitFlashMaterials: THREE.MeshStandardMaterial[];
  label: THREE.Sprite;
  baseColor: number;
  isBoss: boolean;
  bmdLoadedGroup?: THREE.Group;
  disposed?: boolean;
}

export interface MapEnvironmentResult {
  group: THREE.Group;
  torchLights: THREE.PointLight[];
  torchFlames: THREE.Mesh[];
  fountainWater?: THREE.Mesh;
  groundMesh: THREE.Mesh;
  disposables: { dispose: () => void }[];
}

export function worldToSceneX(wx: number): number {
  return (wx - 700) / 10;
}

export function worldToSceneZ(wy: number): number {
  return (wy - 700) / 10;
}

export function sceneToWorldX(sx: number): number {
  return sx * 10 + 700;
}

export function sceneToWorldY(sz: number): number {
  return sz * 10 + 700;
}

function disposeObjResources(obj: THREE.Object3D) {
  obj.traverse((child) => {
    const mesh = child as THREE.Mesh;
    if (mesh.geometry) mesh.geometry.dispose();
    const mat = mesh.material;
    if (Array.isArray(mat)) {
      mat.forEach((m) => m.dispose());
    } else if (mat) {
      mat.dispose();
    }
  });
}

export function createHeroRig(
  name: string,
  classId: ClassId,
  level: number,
  wingsTier: number,
  weaponPlus: number,
  isRemote: boolean,
  shadowTex: THREE.Texture,
  maxAnisotropy: number
): HeroRig {
  const root = new THREE.Group();
  const bodyGroup = new THREE.Group();
  root.add(bodyGroup);

  const shadow = new THREE.Mesh(
    new THREE.PlaneGeometry(2.3, 2.3),
    new THREE.MeshBasicMaterial({ map: shadowTex, transparent: true, depthWrite: false, opacity: 0.72 })
  );
  shadow.rotation.x = -Math.PI / 2;
  shadow.position.y = 0.03;
  root.add(shadow);

  const auraRing = new THREE.Mesh(
    new THREE.RingGeometry(0.95, 1.25, 32),
    new THREE.MeshBasicMaterial({
      color: weaponPlus >= 11 ? 0xfbbf24 : 0x38bdf8,
      transparent: true,
      opacity: weaponPlus >= 7 ? 0.65 : 0.0,
      side: THREE.DoubleSide,
      depthWrite: false,
    })
  );
  auraRing.rotation.x = -Math.PI / 2;
  auraRing.position.y = 0.05;
  root.add(auraRing);

  const classColors = {
    dk: { main: '#b91c1c', accent: '#f59e0b', metal: 0xdc2626, cloth: 0x450a0a },
    dw: { main: '#2563eb', accent: '#a855f7', metal: 0x3b82f6, cloth: 0x1e1b4b },
    elf: { main: '#059669', accent: '#fde047', metal: 0x10b981, cloth: 0x064e3b },
  }[classId];

  const armorTex = createMetalArmorTexture(classColors.main, classColors.accent, maxAnisotropy);
  const plusIntensity = weaponPlus >= 7 ? 0.32 + (weaponPlus - 7) * 0.08 : 0.04;

  const armorMat = new THREE.MeshStandardMaterial({
    map: armorTex,
    roughness: 0.28,
    metalness: 0.85,
    emissive: new THREE.Color(classColors.metal),
    emissiveIntensity: plusIntensity,
  });

  const clothMat = new THREE.MeshStandardMaterial({
    color: classColors.cloth,
    roughness: 0.86,
    metalness: 0.08,
  });

  const skinMat = new THREE.MeshStandardMaterial({
    color: 0xe6b89c,
    roughness: 0.68,
    metalness: 0.02,
  });

  const goldTrimMat = new THREE.MeshStandardMaterial({
    color: 0xf59e0b,
    roughness: 0.22,
    metalness: 0.92,
  });

  const weaponGlowMat = new THREE.MeshStandardMaterial({
    color: weaponPlus >= 10 ? 0xfef08a : 0xe2e8f0,
    roughness: 0.16,
    metalness: 0.95,
    emissive: new THREE.Color(weaponPlus >= 7 ? (weaponPlus >= 11 ? 0xf59e0b : 0x38bdf8) : 0x000000),
    emissiveIntensity: weaponPlus >= 7 ? 0.65 : 0.05,
  });

  // Instant procedural silhouette while real MU .bmd loads asynchronously
  const legGeo = new THREE.BoxGeometry(0.28, 0.82, 0.3);
  const leftLeg = new THREE.Mesh(legGeo, armorMat);
  leftLeg.position.set(-0.22, 0.42, 0);
  leftLeg.castShadow = true;
  bodyGroup.add(leftLeg);

  const rightLeg = new THREE.Mesh(legGeo, armorMat);
  rightLeg.position.set(0.22, 0.42, 0);
  rightLeg.castShadow = true;
  bodyGroup.add(rightLeg);

  const torso = new THREE.Mesh(new THREE.BoxGeometry(0.76, 0.88, 0.46), armorMat);
  torso.position.y = 1.28;
  torso.castShadow = true;
  bodyGroup.add(torso);

  const belt = new THREE.Mesh(new THREE.BoxGeometry(0.8, 0.16, 0.5), goldTrimMat);
  belt.position.y = 0.9;
  bodyGroup.add(belt);

  if (classId === 'dw') {
    const robe = new THREE.Mesh(new THREE.CylinderGeometry(0.36, 0.56, 0.85, 8), clothMat);
    robe.position.y = 0.68;
    bodyGroup.add(robe);
  } else if (classId === 'elf') {
    const tunic = new THREE.Mesh(new THREE.CylinderGeometry(0.32, 0.48, 0.55, 8), clothMat);
    tunic.position.y = 0.78;
    bodyGroup.add(tunic);
  }

  const head = new THREE.Mesh(new THREE.BoxGeometry(0.42, 0.44, 0.42), skinMat);
  head.position.y = 1.98;
  bodyGroup.add(head);

  const leftArm = new THREE.Group();
  leftArm.position.set(-0.52, 1.56, 0);
  const leftArmMesh = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.65, 0.24), armorMat);
  leftArmMesh.position.y = -0.28;
  leftArm.add(leftArmMesh);
  bodyGroup.add(leftArm);

  const rightArm = new THREE.Group();
  rightArm.position.set(0.52, 1.56, 0);
  const rightArmMesh = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.65, 0.24), armorMat);
  rightArmMesh.position.y = -0.28;
  rightArm.add(rightArmMesh);
  bodyGroup.add(rightArm);

  const wingsGroup = new THREE.Group();
  wingsGroup.position.set(0, 1.45, -0.26);
  const leftWing = new THREE.Group();
  const rightWing = new THREE.Group();
  wingsGroup.add(leftWing, rightWing);
  wingsGroup.visible = wingsTier > 0;
  bodyGroup.add(wingsGroup);

  const label = createLabelSprite(
    `${name} · Nv${level}`,
    isRemote ? '#7dd3fc' : '#fef3c7',
    undefined,
    false
  );
  label.position.set(0, 3.05, 0);
  root.add(label);

  const rig: HeroRig = {
    root,
    bodyGroup,
    leftArm,
    rightArm,
    leftLeg,
    rightLeg,
    wingsGroup,
    leftWing,
    rightWing,
    auraRing,
    weaponGlowMat,
    armorMat,
    label,
    classId,
    wingsTier,
    weaponPlus,
  };

  // Async load authentic MU Online .bmd character + armor + weapon + shield + wings
  loadPlayerHeroBmdGroup(classId, wingsTier, weaponPlus, maxAnisotropy).then((bmdGroup) => {
    if (!bmdGroup) return;
    if (rig.disposed) {
      disposeObjResources(bmdGroup);
      return;
    }
    while (bodyGroup.children.length > 0) {
      const child = bodyGroup.children[0];
      bodyGroup.remove(child);
      disposeObjResources(child);
    }
    bodyGroup.add(bmdGroup);
    rig.bmdLoadedGroup = bmdGroup;
  });

  return rig;
}

export function createMonsterRig(
  templateId: string,
  name: string,
  level: number,
  colorHex: string,
  hp: number,
  maxHp: number,
  isBoss: boolean,
  shadowTex: THREE.Texture,
  maxAnisotropy = 4
): MonsterRig {
  const root = new THREE.Group();
  const bodyGroup = new THREE.Group();
  root.add(bodyGroup);

  const scale = isBoss ? 1.55 : 1.0;
  const baseColor = new THREE.Color(colorHex).getHex();

  const shadow = new THREE.Mesh(
    new THREE.PlaneGeometry(2.2 * scale, 2.2 * scale),
    new THREE.MeshBasicMaterial({ map: shadowTex, transparent: true, depthWrite: false, opacity: 0.68 })
  );
  shadow.rotation.x = -Math.PI / 2;
  shadow.position.y = 0.03;
  root.add(shadow);

  const primaryMat = new THREE.MeshStandardMaterial({
    color: baseColor,
    roughness: isBoss ? 0.35 : 0.55,
    metalness: isBoss ? 0.55 : 0.2,
    emissive: new THREE.Color(baseColor),
    emissiveIntensity: isBoss ? 0.3 : 0.08,
  });

  const darkBoneMat = new THREE.MeshStandardMaterial({
    color: 0x1e293b,
    roughness: 0.78,
    metalness: 0.15,
  });

  let leftWing: THREE.Mesh | undefined;
  let rightWing: THREE.Mesh | undefined;

  if (templateId.includes('dragon')) {
    const torso = new THREE.Mesh(new THREE.BoxGeometry(0.95 * scale, 0.75 * scale, 1.4 * scale), primaryMat);
    torso.position.y = 0.9 * scale;
    bodyGroup.add(torso);
    const wingGeo = new THREE.BoxGeometry(1.35 * scale, 0.08 * scale, 0.85 * scale);
    leftWing = new THREE.Mesh(wingGeo, primaryMat);
    leftWing.position.set(-1.0 * scale, 1.2 * scale, 0);
    bodyGroup.add(leftWing);
    rightWing = new THREE.Mesh(wingGeo, primaryMat);
    rightWing.position.set(1.0 * scale, 1.2 * scale, 0);
    bodyGroup.add(rightWing);
  } else if (templateId.includes('hound')) {
    const body = new THREE.Mesh(new THREE.BoxGeometry(0.65, 0.55, 1.3), primaryMat);
    body.position.y = 0.65;
    bodyGroup.add(body);
  } else {
    const robeOrTorso = new THREE.Mesh(
      new THREE.CylinderGeometry(0.36 * scale, 0.54 * scale, 1.3 * scale, 7),
      primaryMat
    );
    robeOrTorso.position.y = 0.75 * scale;
    bodyGroup.add(robeOrTorso);
    const head = new THREE.Mesh(new THREE.DodecahedronGeometry(0.34 * scale, 0), darkBoneMat);
    head.position.y = 1.75 * scale;
    bodyGroup.add(head);
  }

  if (isBoss) {
    const bossRing = new THREE.Mesh(
      new THREE.RingGeometry(1.4, 1.75, 28),
      new THREE.MeshBasicMaterial({
        color: 0xf59e0b,
        transparent: true,
        opacity: 0.72,
        side: THREE.DoubleSide,
        depthWrite: false,
      })
    );
    bossRing.rotation.x = -Math.PI / 2;
    bossRing.position.y = 0.05;
    root.add(bossRing);
  }

  const label = createLabelSprite(
    `${name} Nv${level}`,
    isBoss ? '#fde68a' : '#f1f5f9',
    hp / Math.max(1, maxHp),
    isBoss
  );
  label.position.set(0, isBoss ? 3.9 : 2.65, 0);
  root.add(label);

  const rig: MonsterRig = {
    root,
    bodyGroup,
    leftWing,
    rightWing,
    primaryMat,
    hitFlashMaterials: [primaryMat],
    label,
    baseColor,
    isBoss,
  };

  // Async replace with real MU Online .bmd monster
  const bmdMaterials: THREE.MeshStandardMaterial[] = [];
  loadMonsterBmdGroup(templateId, isBoss, maxAnisotropy, bmdMaterials).then((bmdGroup) => {
    if (!bmdGroup) return;
    if (rig.disposed) {
      disposeObjResources(bmdGroup);
      return;
    }
    while (bodyGroup.children.length > 0) {
      const child = bodyGroup.children[0];
      bodyGroup.remove(child);
      disposeObjResources(child);
    }
    bodyGroup.add(bmdGroup);
    rig.leftWing = undefined;
    rig.rightWing = undefined;
    rig.bmdLoadedGroup = bmdGroup;
    if (bmdMaterials.length > 0) {
      rig.hitFlashMaterials = bmdMaterials;
    }
  });

  return rig;
}

export function createDropMesh(drop: GroundDrop): THREE.Group {
  const group = new THREE.Group();

  if (drop.zen) {
    const coinMat = new THREE.MeshStandardMaterial({
      color: 0xfbbf24,
      emissive: 0xd97706,
      emissiveIntensity: 0.45,
      metalness: 0.9,
      roughness: 0.2,
    });
    for (let i = 0; i < 3; i++) {
      const coin = new THREE.Mesh(new THREE.CylinderGeometry(0.28, 0.28, 0.1, 10), coinMat);
      coin.position.set((i - 1) * 0.18, 0.08 + i * 0.08, (i % 2) * 0.12);
      group.add(coin);
    }
    const label = createLabelSprite(`${drop.zen} Zen`, '#fde68a');
    label.position.set(0, 1.1, 0);
    label.scale.set(2.5, 0.65, 1);
    group.add(label);
  } else if (drop.item) {
    const col = new THREE.Color(drop.item.colorHex).getHex();
    const isJewel = drop.item.slot === 'jewel';
    const mat = new THREE.MeshStandardMaterial({
      color: col,
      emissive: col,
      emissiveIntensity: 0.85,
      roughness: 0.15,
      metalness: 0.8,
    });
    const gem = new THREE.Mesh(
      isJewel ? new THREE.OctahedronGeometry(0.35, 0) : new THREE.BoxGeometry(0.55, 0.35, 0.4),
      mat
    );
    gem.position.y = 0.45;
    group.add(gem);

    if (isJewel || drop.item.rarity === 'mythic' || drop.item.rarity === 'excellent') {
      const beam = new THREE.Mesh(
        new THREE.CylinderGeometry(0.08, 0.32, 3.8, 10, 1, true),
        new THREE.MeshBasicMaterial({
          color: col,
          transparent: true,
          opacity: 0.36,
          side: THREE.DoubleSide,
          depthWrite: false,
        })
      );
      beam.position.y = 1.9;
      group.add(beam);
    }

    const txt = `${drop.item.name}${drop.item.plus > 0 ? ` +${drop.item.plus}` : ''}`;
    const label = createLabelSprite(txt, drop.item.colorHex);
    label.position.set(0, 1.35, 0);
    label.scale.set(3.2, 0.78, 1);
    group.add(label);
  }

  return group;
}

export function buildMapEnvironment(mapId: MapId, maxAnisotropy: number): MapEnvironmentResult {
  const group = new THREE.Group();
  const torchLights: THREE.PointLight[] = [];
  const torchFlames: THREE.Mesh[] = [];
  const disposables: { dispose: () => void }[] = [];
  let aborted = false;
  disposables.push({
    dispose: () => {
      aborted = true;
    },
  });

  const textures = createMapTextures(mapId, maxAnisotropy);
  disposables.push(textures.ground, textures.safeStone);

  const outerGeo = new THREE.PlaneGeometry(340, 340);
  const outerMat = new THREE.MeshStandardMaterial({
    map: textures.ground,
    roughness: mapId === 'devias' ? 0.55 : 0.88,
    metalness: 0.05,
  });
  const groundMesh = new THREE.Mesh(outerGeo, outerMat);
  groundMesh.rotation.x = -Math.PI / 2;
  groundMesh.receiveShadow = true;
  group.add(groundMesh);
  disposables.push(outerGeo, outerMat);

  const safeX = worldToSceneX(480);
  const safeZ = worldToSceneZ(480);

  const safePlazaGeo = new THREE.CylinderGeometry(13.8, 14.4, 0.28, 32);
  const safePlazaMat = new THREE.MeshStandardMaterial({
    map: textures.safeStone,
    roughness: 0.72,
    metalness: 0.12,
  });
  const safePlaza = new THREE.Mesh(safePlazaGeo, safePlazaMat);
  safePlaza.position.set(safeX, 0.14, safeZ);
  safePlaza.receiveShadow = true;
  group.add(safePlaza);
  disposables.push(safePlazaGeo, safePlazaMat);

  // Safe Zone Fountain
  const stoneTrimMat = new THREE.MeshStandardMaterial({ color: 0x64748b, roughness: 0.75, metalness: 0.25 });
  const goldMat = new THREE.MeshStandardMaterial({ color: 0xd97706, roughness: 0.28, metalness: 0.88 });
  const fountainGroup = new THREE.Group();
  fountainGroup.position.set(safeX, 0, safeZ);
  group.add(fountainGroup);

  const fountainBaseGeo = new THREE.CylinderGeometry(2.6, 3.0, 0.65, 16);
  const fountainBase = new THREE.Mesh(fountainBaseGeo, stoneTrimMat);
  fountainBase.position.y = 0.45;
  fountainBase.castShadow = true;
  fountainBase.receiveShadow = true;
  fountainGroup.add(fountainBase);

  const waterGeo = new THREE.CylinderGeometry(2.2, 2.2, 0.2, 16);
  const waterMat = new THREE.MeshStandardMaterial({
    color: mapId === 'custom' ? 0xf43f5e : 0x38bdf8,
    emissive: mapId === 'custom' ? 0x9f1239 : 0x0284c7,
    emissiveIntensity: 0.75,
    roughness: 0.15,
    metalness: 0.4,
  });
  const fountainWater = new THREE.Mesh(waterGeo, waterMat);
  fountainWater.position.y = 0.72;
  fountainGroup.add(fountainWater);

  const spireGeo = new THREE.ConeGeometry(0.65, 2.2, 6);
  const spire = new THREE.Mesh(spireGeo, goldMat);
  spire.position.y = 1.85;
  fountainGroup.add(spire);
  disposables.push(stoneTrimMat, goldMat, fountainBaseGeo, waterGeo, waterMat, spireGeo);

  // Swap central spire with real MU Lorencia Well01.bmd
  buildGroupFromBmd('Object1/Well01.bmd', { targetHeight: 4.5, maxAnisotropy, doubleSided: true }).then((well) => {
    if (!well) return;
    if (aborted) {
      disposeObjResources(well);
      return;
    }
    fountainGroup.remove(fountainBase, spire);
    fountainWater.position.y = 0.55;
    fountainGroup.add(well);
    disposables.push({ dispose: () => disposeObjResources(well) });
  });

  // 4 Classic MU Stone Torch Pillars around Safe Zone
  const pillarGeo = new THREE.BoxGeometry(0.95, 3.4, 0.95);
  const flameGeo = new THREE.OctahedronGeometry(0.42, 0);
  const flameMat = new THREE.MeshBasicMaterial({
    color: mapId === 'devias' ? 0x7dd3fc : mapId === 'dungeon' ? 0xc084fc : 0xfb923c,
  });
  disposables.push(pillarGeo, flameGeo, flameMat);

  const pillarAngles = [Math.PI * 0.25, Math.PI * 0.75, Math.PI * 1.25, Math.PI * 1.75];
  pillarAngles.forEach((ang, idx) => {
    const px = safeX + Math.cos(ang) * 11.2;
    const pz = safeZ + Math.sin(ang) * 11.2;

    const pillar = new THREE.Mesh(pillarGeo, stoneTrimMat);
    pillar.position.set(px, 1.7, pz);
    pillar.castShadow = true;
    group.add(pillar);

    const flame = new THREE.Mesh(flameGeo, flameMat);
    flame.position.set(px, 3.75, pz);
    group.add(flame);
    torchFlames.push(flame);

    if (idx % 2 === 0) {
      const pLight = new THREE.PointLight(
        mapId === 'devias' ? 0x38bdf8 : mapId === 'dungeon' ? 0xa855f7 : 0xf97316,
        18,
        24,
        1.5
      );
      pLight.position.set(px, 4.1, pz);
      group.add(pLight);
      torchLights.push(pLight);
    }
  });

  // Safe Zone Archways
  const archPostGeo = new THREE.BoxGeometry(1.1, 4.4, 1.1);
  const archLintGeo = new THREE.BoxGeometry(5.6, 0.9, 1.3);
  disposables.push(archPostGeo, archLintGeo);
  const addArch = (ax: number, az: number, rotY: number) => {
    const arch = new THREE.Group();
    const p1 = new THREE.Mesh(archPostGeo, stoneTrimMat);
    p1.position.set(-2.1, 2.2, 0);
    const p2 = new THREE.Mesh(archPostGeo, stoneTrimMat);
    p2.position.set(2.1, 2.2, 0);
    const top = new THREE.Mesh(archLintGeo, stoneTrimMat);
    top.position.set(0, 4.5, 0);
    arch.add(p1, p2, top);
    arch.position.set(ax, 0, az);
    arch.rotation.y = rotY;
    group.add(arch);
  };
  addArch(safeX, safeZ + 13.5, 0);
  addArch(safeX + 13.5, safeZ, Math.PI / 2);

  // Real MU Lorencia Buildings + Statues + StreetLights (async from /mu-data/Object1)
  if (mapId === 'lorencia' || mapId === 'custom') {
    const propsToPlace: { rel: string; x: number; z: number; rotY: number; height: number }[] = [
      { rel: 'Object1/House01.bmd', x: safeX - 11.2, z: safeZ - 8.5, rotY: 0.4, height: 6.4 },
      { rel: 'Object1/House02.bmd', x: safeX + 11.2, z: safeZ - 8.5, rotY: -0.4, height: 6.0 },
      { rel: 'Object1/StoneStatue01.bmd', x: safeX - 8.2, z: safeZ + 8.5, rotY: 0.6, height: 4.2 },
      { rel: 'Object1/StoneStatue01.bmd', x: safeX + 8.2, z: safeZ + 8.5, rotY: -0.6, height: 4.2 },
      { rel: 'Object1/StreetLight01.bmd', x: safeX - 4.5, z: safeZ + 13.2, rotY: 0, height: 4.6 },
      { rel: 'Object1/StreetLight01.bmd', x: safeX + 4.5, z: safeZ + 13.2, rotY: Math.PI, height: 4.6 },
    ];
    for (const p of propsToPlace) {
      buildGroupFromBmd(p.rel, { targetHeight: p.height, maxAnisotropy, doubleSided: true }).then((meshGroup) => {
        if (!meshGroup) return;
        if (aborted) {
          disposeObjResources(meshGroup);
          return;
        }
        meshGroup.position.set(p.x, 0.25, p.z);
        meshGroup.rotation.y = p.rotY;
        group.add(meshGroup);
        disposables.push({ dispose: () => disposeObjResources(meshGroup) });
      });
    }
  }

  // Real MU Safe Zone NPCs (Hanzo Blacksmith, Pasi Wizard, Liaman Barmaid, Chaos Goblin)
  const npcsToSpawn = mapId === 'lorencia' ? LORENCIA_NPCS : LORENCIA_NPCS.filter((n) => n.id === 'chaos' || n.id === 'barmaid');
  for (const npc of npcsToSpawn) {
    loadNpcBmdGroup(npc.id, maxAnisotropy).then((npcMesh) => {
      if (!npcMesh) return;
      if (aborted) {
        disposeObjResources(npcMesh);
        return;
      }
      const npcWrap = new THREE.Group();
      npcWrap.position.set(worldToSceneX(npc.wx), 0.28, worldToSceneZ(npc.wy));
      npcMesh.rotation.y = npc.rotY;
      npcWrap.add(npcMesh);

      const plate = createLabelSprite(`${npc.name}`, npc.id === 'chaos' ? '#d8b4fe' : '#fde047');
      plate.position.set(0, 2.85, 0);
      npcWrap.add(plate);

      const sub = createLabelSprite(`[${npc.subtitle}]`, '#93c5fd');
      sub.position.set(0, 3.45, 0);
      sub.scale.set(2.4, 0.6, 1);
      npcWrap.add(sub);

      group.add(npcWrap);
      disposables.push({ dispose: () => disposeObjResources(npcWrap) });
    });
  }

  // Perimeter Trees / Crystals / Spires
  const propCount = 68;
  const trunkGeo = new THREE.CylinderGeometry(0.28, 0.42, 2.2, 6);
  const crownGeo =
    mapId === 'devias'
      ? new THREE.OctahedronGeometry(1.5, 0)
      : mapId === 'dungeon' || mapId === 'custom'
      ? new THREE.ConeGeometry(1.1, 4.2, 5)
      : new THREE.ConeGeometry(1.65, 3.8, 6);

  const trunkMat = new THREE.MeshStandardMaterial({
    color: mapId === 'devias' ? 0x475569 : 0x3f2e21,
    roughness: 0.9,
  });
  const crownMat = new THREE.MeshStandardMaterial({
    color:
      mapId === 'devias'
        ? 0xbae6fd
        : mapId === 'noria'
        ? 0x10b981
        : mapId === 'dungeon'
        ? 0x3b2f4a
        : mapId === 'custom'
        ? 0x7f1d1d
        : 0x2d4a22,
    roughness: mapId === 'devias' ? 0.25 : 0.8,
    metalness: mapId === 'devias' ? 0.3 : 0.05,
  });

  const trunks = new THREE.InstancedMesh(trunkGeo, trunkMat, propCount);
  const crowns = new THREE.InstancedMesh(crownGeo, crownMat, propCount);
  trunks.castShadow = true;
  crowns.castShadow = true;

  const dummy = new THREE.Object3D();
  const tint = new THREE.Color();

  for (let i = 0; i < propCount; i++) {
    const angle = (i / propCount) * Math.PI * 2 + (i % 3) * 0.07;
    const ring = i < 44 ? 72 + (i % 7) * 3.5 : 34 + (i % 11) * 3.2;
    let px = Math.cos(angle) * ring;
    let pz = Math.sin(angle) * ring;

    if (Math.hypot(px - safeX, pz - safeZ) < 19) {
      px += 26;
      pz += 26;
    }

    const s = 0.85 + ((i * 37) % 30) / 100;
    dummy.position.set(px, 1.1 * s, pz);
    dummy.scale.set(s, s, s);
    dummy.rotation.y = i * 1.1;
    dummy.updateMatrix();
    trunks.setMatrixAt(i, dummy.matrix);

    dummy.position.set(px, 3.1 * s, pz);
    dummy.updateMatrix();
    crowns.setMatrixAt(i, dummy.matrix);

    tint.setHSL(0, 0, 0.82 + ((i * 19) % 25) / 100);
    crowns.setColorAt(i, tint);
  }
  trunks.instanceMatrix.needsUpdate = true;
  crowns.instanceMatrix.needsUpdate = true;
  if (crowns.instanceColor) crowns.instanceColor.needsUpdate = true;
  trunks.computeBoundingSphere();
  crowns.computeBoundingSphere();

  group.add(trunks, crowns);
  disposables.push(trunkGeo, crownGeo, trunkMat, crownMat);

  return { group, torchLights, torchFlames, fountainWater, groundMesh, disposables };
}
