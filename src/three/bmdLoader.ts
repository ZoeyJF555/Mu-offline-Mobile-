import * as THREE from 'three';
import type { ClassId } from '../types/game';

const XOR_KEY = new Uint8Array([
  0xd1, 0x73, 0x52, 0xf6, 0xd2, 0x9a, 0xcb, 0x27,
  0x3e, 0xaf, 0x59, 0x31, 0x37, 0xb3, 0xe7, 0xa2,
]);

export interface BmdVertex {
  node: number;
  position: THREE.Vector3;
}

export interface BmdNormal {
  node: number;
  normal: THREE.Vector3;
}

export interface BmdTriangle {
  polygon: number;
  vertexIndex: number[];
  normalIndex: number[];
  texCoordIndex: number[];
}

export interface BmdMesh {
  vertices: BmdVertex[];
  normals: BmdNormal[];
  texCoords: { u: number; v: number }[];
  triangles: BmdTriangle[];
  texturePath: string;
}

export interface BmdBoneMatrix {
  positions: THREE.Vector3[];
  quaternions: THREE.Quaternion[];
}

export interface BmdBone {
  dummy: boolean;
  name: string;
  parent: number;
  matrices: BmdBoneMatrix[];
}

export interface BmdAction {
  numKeys: number;
  lockPositions: boolean;
}

export interface BmdAsset {
  name: string;
  meshes: BmdMesh[];
  bones: BmdBone[];
  actions: BmdAction[];
}

function readAscii(dv: DataView, start: number, len: number): string {
  let out = '';
  for (let i = 0; i < len; i++) {
    const c = dv.getUint8(start + i);
    if (c === 0) break;
    out += String.fromCharCode(c);
  }
  return out;
}

function decryptBmdIfNeeded(raw: Uint8Array): Uint8Array {
  const version = raw[3];
  if (version !== 12) return raw;
  const enc = raw.slice(8);
  const dec = new Uint8Array(enc.length);
  let wMapKey = 0x5e;
  for (let i = 0; i < enc.length; i++) {
    dec[i] = ((enc[i] ^ XOR_KEY[i % 16]) - wMapKey) & 0xff;
    wMapKey = (enc[i] + 0x3d) & 0xff;
  }
  const out = new Uint8Array(4 + dec.length);
  out.set(raw.slice(0, 4), 0);
  out.set(dec, 4);
  return out;
}

function angleToQuat(rx: number, ry: number, rz: number): THREE.Quaternion {
  const az = rz * 0.5;
  const ay = ry * 0.5;
  const ax = rx * 0.5;
  const sz = Math.sin(az);
  const cz = Math.cos(az);
  const sy = Math.sin(ay);
  const cy = Math.cos(ay);
  const sx = Math.sin(ax);
  const cx = Math.cos(ax);
  return new THREE.Quaternion(
    sx * cy * cz - cx * sy * sz,
    cx * sy * cz + sx * cy * sz,
    cx * cy * sz - sx * sy * cz,
    cx * cy * cz + sx * sy * sz
  );
}

export function parseBmd(rawBytes: Uint8Array): BmdAsset {
  const header = String.fromCharCode(rawBytes[0], rawBytes[1], rawBytes[2]);
  if (header !== 'BMD') {
    throw new Error(`Formato BMD inválido: ${header}`);
  }
  const buf = decryptBmdIfNeeded(rawBytes);
  const dv = new DataView(buf.buffer, buf.byteOffset, buf.byteLength);
  let ptr = 4;
  const name = readAscii(dv, ptr, 32);
  ptr += 32;
  const numMeshes = dv.getUint16(ptr, true);
  ptr += 2;
  const numBones = dv.getUint16(ptr, true);
  ptr += 2;
  const numActions = dv.getUint16(ptr, true);
  ptr += 2;

  const meshes: BmdMesh[] = [];
  for (let m = 0; m < numMeshes; m++) {
    const numVertices = dv.getInt16(ptr, true);
    ptr += 2;
    const numNormals = dv.getInt16(ptr, true);
    ptr += 2;
    const numTexCoords = dv.getInt16(ptr, true);
    ptr += 2;
    const numTriangles = dv.getInt16(ptr, true);
    ptr += 2;
    ptr += 2; // texture index

    const vertices: BmdVertex[] = [];
    for (let i = 0; i < numVertices; i++) {
      const node = dv.getInt16(ptr, true);
      const x = dv.getFloat32(ptr + 4, true);
      const y = dv.getFloat32(ptr + 8, true);
      const z = dv.getFloat32(ptr + 12, true);
      vertices.push({ node, position: new THREE.Vector3(x, y, z) });
      ptr += 16;
    }

    const normals: BmdNormal[] = [];
    for (let i = 0; i < numNormals; i++) {
      const node = dv.getInt16(ptr, true);
      const x = dv.getFloat32(ptr + 4, true);
      const y = dv.getFloat32(ptr + 8, true);
      const z = dv.getFloat32(ptr + 12, true);
      normals.push({ node, normal: new THREE.Vector3(x, y, z) });
      ptr += 20;
    }

    const texCoords: { u: number; v: number }[] = [];
    for (let i = 0; i < numTexCoords; i++) {
      const u = dv.getFloat32(ptr, true);
      const v = dv.getFloat32(ptr + 4, true);
      texCoords.push({ u, v });
      ptr += 8;
    }

    const triangles: BmdTriangle[] = [];
    for (let i = 0; i < numTriangles; i++) {
      const polygon = dv.getUint8(ptr);
      const vertexIndex = [
        dv.getInt16(ptr + 2, true),
        dv.getInt16(ptr + 4, true),
        dv.getInt16(ptr + 6, true),
        dv.getInt16(ptr + 8, true),
      ];
      const normalIndex = [
        dv.getInt16(ptr + 10, true),
        dv.getInt16(ptr + 12, true),
        dv.getInt16(ptr + 14, true),
        dv.getInt16(ptr + 16, true),
      ];
      const texCoordIndex = [
        dv.getInt16(ptr + 18, true),
        dv.getInt16(ptr + 20, true),
        dv.getInt16(ptr + 22, true),
        dv.getInt16(ptr + 24, true),
      ];
      triangles.push({ polygon, vertexIndex, normalIndex, texCoordIndex });
      ptr += 64;
    }

    const texturePath = readAscii(dv, ptr, 32);
    ptr += 32;
    meshes.push({ vertices, normals, texCoords, triangles, texturePath });
  }

  const actions: BmdAction[] = [];
  for (let i = 0; i < numActions; i++) {
    const numKeys = dv.getInt16(ptr, true);
    ptr += 2;
    const lockPositions = dv.getUint8(ptr) === 1;
    ptr += 1;
    if (lockPositions) {
      ptr += numKeys * 12;
    }
    actions.push({ numKeys, lockPositions });
  }

  const bones: BmdBone[] = [];
  for (let i = 0; i < numBones; i++) {
    const dummy = dv.getUint8(ptr) === 1;
    ptr += 1;
    if (dummy) {
      bones.push({ dummy: true, name: 'Dummy', parent: -1, matrices: [] });
    } else {
      const bName = readAscii(dv, ptr, 32);
      ptr += 32;
      const parent = dv.getInt16(ptr, true);
      ptr += 2;
      const matrices: BmdBoneMatrix[] = [];
      for (let a = 0; a < numActions; a++) {
        const numKeys = actions[a].numKeys;
        const positions: THREE.Vector3[] = [];
        for (let k = 0; k < numKeys; k++) {
          positions.push(
            new THREE.Vector3(
              dv.getFloat32(ptr, true),
              dv.getFloat32(ptr + 4, true),
              dv.getFloat32(ptr + 8, true)
            )
          );
          ptr += 12;
        }
        const quaternions: THREE.Quaternion[] = [];
        for (let k = 0; k < numKeys; k++) {
          const rx = dv.getFloat32(ptr, true);
          const ry = dv.getFloat32(ptr + 4, true);
          const rz = dv.getFloat32(ptr + 8, true);
          quaternions.push(angleToQuat(rx, ry, rz));
          ptr += 12;
        }
        matrices.push({ positions, quaternions });
      }
      bones.push({ dummy: false, name: bName, parent, matrices });
    }
  }

  return { name, meshes, bones, actions };
}

const _localMat = new THREE.Matrix4();
const _scaleOne = new THREE.Vector3(1, 1, 1);

export function computeBoneWorldMatrices(
  bones: BmdBone[],
  actionIdx: number,
  keyFrame: number
): THREE.Matrix4[] {
  const out: THREE.Matrix4[] = new Array(bones.length);
  for (let i = 0; i < bones.length; i++) {
    const bone = bones[i];
    if (bone.dummy || !bone.matrices[actionIdx] || bone.matrices[actionIdx].positions.length === 0) {
      out[i] = new THREE.Matrix4();
      continue;
    }
    const matData = bone.matrices[actionIdx];
    const frame = Math.max(0, Math.min(matData.positions.length - 1, keyFrame));
    const pos = matData.positions[frame];
    const quat = matData.quaternions[frame];
    _localMat.compose(pos, quat, _scaleOne);

    if (bone.parent >= 0 && bone.parent < i) {
      out[i] = new THREE.Matrix4().multiplyMatrices(out[bone.parent], _localMat);
    } else {
      out[i] = _localMat.clone();
    }
  }
  return out;
}

const bmdCache = new Map<string, Promise<BmdAsset | null>>();
const texCache = new Map<string, Promise<THREE.Texture | null>>();
let texManifestCache: Promise<Record<string, string>> | null = null;

async function getTextureManifest(): Promise<Record<string, string>> {
  if (!texManifestCache) {
    texManifestCache = fetch('/mu-data/manifest.json')
      .then((r) => (r.ok ? r.json() : {}))
      .catch(() => ({}));
  }
  return texManifestCache;
}

export function loadBmdAsset(relPath: string): Promise<BmdAsset | null> {
  if (!bmdCache.has(relPath)) {
    const p = fetch(`/mu-data/${relPath}`)
      .then((r) => {
        if (!r.ok) return null;
        return r.arrayBuffer();
      })
      .then((ab) => (ab ? parseBmd(new Uint8Array(ab)) : null))
      .catch(() => null);
    bmdCache.set(relPath, p);
  }
  return bmdCache.get(relPath)!;
}

function decodeOztToTexture(bytes: Uint8Array, maxAnisotropy: number): THREE.DataTexture | null {
  // OZT = 4-byte header + TGA
  if (bytes.length < 22) return null;
  const tga = bytes.slice(4);
  const idLen = tga[0];
  const imgType = tga[2];
  const width = tga[12] | (tga[13] << 8);
  const height = tga[14] | (tga[15] << 8);
  const bpp = tga[16];
  const desc = tga[17];
  if (width <= 0 || height <= 0 || width > 2048 || height > 2048) return null;
  const bytesPerPixel = bpp / 8;
  if (bytesPerPixel !== 3 && bytesPerPixel !== 4) return null;

  const rgba = new Uint8Array(width * height * 4);
  let src = 18 + idLen;
  const topOrigin = (desc & 0x20) !== 0;

  const writePixel = (idx: number, b: number, g: number, r: number, a: number) => {
    const row = Math.floor(idx / width);
    const col = idx % width;
    const dstRow = topOrigin ? row : height - 1 - row;
    const o = (dstRow * width + col) * 4;
    rgba[o] = r;
    rgba[o + 1] = g;
    rgba[o + 2] = b;
    rgba[o + 3] = a;
  };

  if (imgType === 2) {
    for (let i = 0; i < width * height && src + bytesPerPixel <= tga.length; i++) {
      writePixel(
        i,
        tga[src],
        tga[src + 1],
        tga[src + 2],
        bytesPerPixel === 4 ? tga[src + 3] : 255
      );
      src += bytesPerPixel;
    }
  } else if (imgType === 10) {
    let i = 0;
    while (i < width * height && src < tga.length) {
      const header = tga[src++];
      const count = (header & 0x7f) + 1;
      if (header & 0x80) {
        const b = tga[src];
        const g = tga[src + 1];
        const r = tga[src + 2];
        const a = bytesPerPixel === 4 ? tga[src + 3] : 255;
        src += bytesPerPixel;
        for (let k = 0; k < count && i < width * height; k++) {
          writePixel(i++, b, g, r, a);
        }
      } else {
        for (let k = 0; k < count && i < width * height && src + bytesPerPixel <= tga.length; k++) {
          writePixel(
            i++,
            tga[src],
            tga[src + 1],
            tga[src + 2],
            bytesPerPixel === 4 ? tga[src + 3] : 255
          );
          src += bytesPerPixel;
        }
      }
    }
  } else {
    return null;
  }

  const tex = new THREE.DataTexture(rgba, width, height, THREE.RGBAFormat);
  tex.colorSpace = THREE.SRGBColorSpace;
  tex.wrapS = THREE.RepeatWrapping;
  tex.wrapT = THREE.RepeatWrapping;
  tex.anisotropy = maxAnisotropy;
  tex.flipY = false;
  tex.needsUpdate = true;
  return tex;
}

export async function loadMuTexture(
  folder: string,
  texName: string,
  maxAnisotropy: number
): Promise<THREE.Texture | null> {
  if (!texName || texName.toLowerCase().startsWith('hide')) return null;
  const manifest = await getTextureManifest();
  const cleanName = texName.split(/[\\/]/).pop() || texName;
  const key = `${folder}/${cleanName}`.toLowerCase();
  const resolvedRel = manifest[key] || manifest[cleanName.toLowerCase()];
  if (!resolvedRel) return null;

  if (!texCache.has(resolvedRel)) {
    const p = (async () => {
      try {
        if (resolvedRel.toLowerCase().endsWith('.ozt')) {
          const res = await fetch(`/mu-data/${resolvedRel}`);
          if (!res.ok) return null;
          const bytes = new Uint8Array(await res.arrayBuffer());
          return decodeOztToTexture(bytes, maxAnisotropy);
        }
        return await new Promise<THREE.Texture | null>((resolve) => {
          new THREE.TextureLoader().load(
            `/mu-data/${resolvedRel}`,
            (tex) => {
              tex.colorSpace = THREE.SRGBColorSpace;
              tex.wrapS = THREE.RepeatWrapping;
              tex.wrapT = THREE.RepeatWrapping;
              tex.anisotropy = maxAnisotropy;
              tex.flipY = false;
              tex.needsUpdate = true;
              resolve(tex);
            },
            undefined,
            () => resolve(null)
          );
        });
      } catch {
        return null;
      }
    })();
    texCache.set(resolvedRel, p);
  }
  return texCache.get(resolvedRel)!;
}

const _tmpV = new THREE.Vector3();
const _tmpN = new THREE.Vector3();
const _normMat = new THREE.Matrix3();

async function buildMeshesIntoParent(
  bmd: BmdAsset,
  folder: string,
  boneWorlds: THREE.Matrix4[],
  parent: THREE.Object3D,
  options: {
    tintColor?: number;
    emissiveColor?: number;
    emissiveIntensity?: number;
    maxAnisotropy?: number;
    doubleSided?: boolean;
    overrideTransform?: THREE.Matrix4;
  },
  materialsOut?: THREE.MeshStandardMaterial[]
) {
  const maxAnisotropy = options.maxAnisotropy ?? 4;

  for (const bMesh of bmd.meshes) {
    if (bMesh.texturePath.toLowerCase().startsWith('hide')) continue;

    const positions: number[] = [];
    const normals: number[] = [];
    const uvs: number[] = [];

    for (const tri of bMesh.triangles) {
      const polyTris = tri.polygon === 4 ? [[0, 1, 2], [0, 2, 3]] : [[0, 1, 2]];
      for (const [a, b, c] of polyTris) {
        for (const corner of [a, b, c]) {
          const vObj = bMesh.vertices[tri.vertexIndex[corner]];
          const nObj = bMesh.normals[tri.normalIndex[corner]];
          const uvObj = bMesh.texCoords[tri.texCoordIndex[corner]];
          if (!vObj || !nObj || !uvObj) continue;

          const bMat = boneWorlds[vObj.node] || boneWorlds[0];
          if (bMat) {
            _tmpV.copy(vObj.position).applyMatrix4(bMat);
            _normMat.getNormalMatrix(bMat);
            _tmpN.copy(nObj.normal).applyMatrix3(_normMat).normalize();
          } else {
            _tmpV.copy(vObj.position);
            _tmpN.copy(nObj.normal);
          }

          if (options.overrideTransform) {
            _tmpV.applyMatrix4(options.overrideTransform);
            _normMat.getNormalMatrix(options.overrideTransform);
            _tmpN.applyMatrix3(_normMat).normalize();
          }

          positions.push(_tmpV.x, _tmpV.y, _tmpV.z);
          normals.push(_tmpN.x, _tmpN.y, _tmpN.z);
          uvs.push(uvObj.u, uvObj.v);
        }
      }
    }

    if (positions.length === 0) continue;
    const geo = new THREE.BufferGeometry();
    geo.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
    geo.setAttribute('normal', new THREE.Float32BufferAttribute(normals, 3));
    geo.setAttribute('uv', new THREE.Float32BufferAttribute(uvs, 2));
    geo.computeBoundingSphere();

    const tex = await loadMuTexture(folder, bMesh.texturePath, maxAnisotropy);
    const isAdditive = /_r\./i.test(bMesh.texturePath);
    const hasAlpha = /\.tga$/i.test(bMesh.texturePath) || isAdditive;

    const mat = new THREE.MeshStandardMaterial({
      map: tex || undefined,
      color: options.tintColor ?? 0xffffff,
      roughness: 0.48,
      metalness: 0.3,
      emissive: new THREE.Color(options.emissiveColor ?? (isAdditive ? 0xffffff : 0x000000)),
      emissiveIntensity: options.emissiveIntensity ?? (isAdditive ? 0.7 : 0),
      alphaTest: hasAlpha ? 0.25 : 0,
      transparent: isAdditive,
      blending: isAdditive ? THREE.AdditiveBlending : THREE.NormalBlending,
      side: options.doubleSided || hasAlpha ? THREE.DoubleSide : THREE.FrontSide,
    });

    const mesh = new THREE.Mesh(geo, mat);
    mesh.castShadow = !isAdditive;
    mesh.receiveShadow = true;
    parent.add(mesh);
    if (materialsOut) materialsOut.push(mat);
  }
}

export function normalizeGroupGround(root: THREE.Group, pivot: THREE.Group, targetHeight?: number) {
  root.updateMatrixWorld(true);
  const box = new THREE.Box3().setFromObject(root);
  if (Number.isFinite(box.min.y) && Number.isFinite(box.max.y)) {
    const h = box.max.y - box.min.y;
    if (targetHeight && h > 0.05) {
      const factor = targetHeight / h;
      pivot.scale.multiplyScalar(factor);
      root.updateMatrixWorld(true);
      const box2 = new THREE.Box3().setFromObject(root);
      if (Number.isFinite(box2.min.y)) {
        pivot.position.y -= box2.min.y;
      }
    } else {
      pivot.position.y -= box.min.y;
    }
  }
}

export async function buildGroupFromBmd(
  bmdRelPath: string,
  options: {
    scale?: number;
    targetHeight?: number;
    skeletonBmd?: BmdAsset | null;
    actionIdx?: number;
    keyFrame?: number;
    tintColor?: number;
    emissiveColor?: number;
    emissiveIntensity?: number;
    maxAnisotropy?: number;
    doubleSided?: boolean;
    materialsOut?: THREE.MeshStandardMaterial[];
  } = {}
): Promise<THREE.Group | null> {
  const bmd = await loadBmdAsset(bmdRelPath);
  if (!bmd) return null;

  const folder = bmdRelPath.split('/')[0];
  const scale = (options.scale ?? 1) * 0.011;
  const actionIdx = options.actionIdx ?? 0;
  const keyFrame = options.keyFrame ?? 0;

  const boneSource =
    options.skeletonBmd && options.skeletonBmd.bones.length > 0 ? options.skeletonBmd.bones : bmd.bones;
  const maxAction = Math.max(0, (boneSource[0]?.matrices.length || 1) - 1);
  const boneWorlds = computeBoneWorldMatrices(boneSource, Math.min(actionIdx, maxAction), keyFrame);

  const root = new THREE.Group();
  const pivot = new THREE.Group();
  pivot.rotation.x = -Math.PI / 2;
  pivot.scale.setScalar(scale);
  root.add(pivot);

  await buildMeshesIntoParent(bmd, folder, boneWorlds, pivot, options, options.materialsOut);
  normalizeGroupGround(root, pivot, options.targetHeight);
  return root;
}

export async function loadPlayerHeroBmdGroup(
  classId: ClassId,
  wingsTier: number,
  weaponPlus: number,
  maxAnisotropy: number
): Promise<THREE.Group | null> {
  const skeleton = await loadBmdAsset('Player/player.bmd');
  if (!skeleton) return null;

  // Player/player.bmd action 1 = StopMale standing pose
  const boneWorlds = computeBoneWorldMatrices(skeleton.bones, 1, 0);

  const partFiles =
    classId === 'dk'
      ? [
          'Player/HelmClass01.bmd',
          'Player/ArmorClass01.bmd',
          'Player/PantClass01.bmd',
          'Player/GloveClass01.bmd',
          'Player/BootClass01.bmd',
        ]
      : classId === 'dw'
      ? [
          'Player/HelmClass02.bmd',
          'Player/ArmorClass02.bmd',
          'Player/PantClass02.bmd',
          'Player/GloveClass02.bmd',
          'Player/BootClass02.bmd',
        ]
      : [
          'Player/HelmElf01.bmd',
          'Player/ArmorElf01.bmd',
          'Player/PantElf01.bmd',
          'Player/GloveElf01.bmd',
          'Player/BootElf01.bmd',
        ];

  const plusEmissiveColor = weaponPlus >= 11 ? 0xfbbf24 : weaponPlus >= 7 ? 0x38bdf8 : 0x000000;
  const plusEmissiveInt = weaponPlus >= 7 ? 0.22 + (weaponPlus - 7) * 0.05 : 0;

  const root = new THREE.Group();
  const pivot = new THREE.Group();
  pivot.rotation.x = -Math.PI / 2;
  pivot.scale.setScalar(0.013);
  root.add(pivot);

  for (const rel of partFiles) {
    const partBmd = await loadBmdAsset(rel);
    if (!partBmd) continue;
    await buildMeshesIntoParent(partBmd, 'Player', boneWorlds, pivot, {
      emissiveColor: plusEmissiveColor,
      emissiveIntensity: plusEmissiveInt,
      maxAnisotropy,
    });
  }

  // Right Hand Weapon (bone 33 = knife_gdf) or Left Hand Bow (bone 42 = hand_bofdgne01)
  const weaponRel =
    classId === 'dk'
      ? weaponPlus >= 5
        ? 'Item/Sword06.bmd'
        : 'Item/Sword01.bmd'
      : classId === 'dw'
      ? 'Item/Staff01.bmd'
      : 'Item/Bow01.bmd';

  const weaponBmd = await loadBmdAsset(weaponRel);
  if (weaponBmd) {
    const wBoneIdx = classId === 'elf' ? 42 : 33;
    const wBoneMat = boneWorlds[wBoneIdx] || boneWorlds[28];
    const wWorlds = computeBoneWorldMatrices(weaponBmd.bones, 0, 0);
    await buildMeshesIntoParent(weaponBmd, 'Item', wWorlds, pivot, {
      emissiveColor: weaponPlus >= 7 ? plusEmissiveColor : 0x1e293b,
      emissiveIntensity: weaponPlus >= 7 ? plusEmissiveInt + 0.15 : 0.08,
      maxAnisotropy,
      overrideTransform: wBoneMat,
    });
  }

  // Dark Knight Shield in Left Hand (bone 42)
  if (classId === 'dk') {
    const shieldBmd = await loadBmdAsset('Item/Shield01.bmd');
    if (shieldBmd) {
      const sBoneMat = boneWorlds[42] || boneWorlds[37];
      const sWorlds = computeBoneWorldMatrices(shieldBmd.bones, 0, 0);
      await buildMeshesIntoParent(shieldBmd, 'Item', sWorlds, pivot, {
        maxAnisotropy,
        overrideTransform: sBoneMat,
      });
    }
  }

  // Wings attached to Spine1/Back bone 47 (Bone05) or bone 18 (Bip01 Spine1)
  if (wingsTier > 0) {
    const wingRel =
      wingsTier >= 2
        ? 'Player/Wing04.bmd'
        : classId === 'elf'
        ? 'Player/Wing02.bmd'
        : classId === 'dw'
        ? 'Player/Wing03.bmd'
        : 'Player/Wing04.bmd';
    const wingBmd = await loadBmdAsset(wingRel);
    if (wingBmd) {
      const backMat = boneWorlds[18] || boneWorlds[47];
      const wWorlds = computeBoneWorldMatrices(wingBmd.bones, 0, 0);
      await buildMeshesIntoParent(wingBmd, 'Player', wWorlds, pivot, {
        emissiveColor: wingsTier >= 2 ? 0xfbbf24 : 0xf43f5e,
        emissiveIntensity: 0.25,
        maxAnisotropy,
        doubleSided: true,
        overrideTransform: backMat,
      });
    }
  }

  normalizeGroupGround(root, pivot, 2.35);
  return root;
}

export async function loadNpcBmdGroup(
  npcId: 'smith' | 'wizard' | 'barmaid' | 'chaos',
  maxAnisotropy: number
): Promise<THREE.Group | null> {
  if (npcId === 'barmaid') {
    const skel = await loadBmdAsset('NPC/Female01.bmd');
    if (!skel) return null;
    const boneWorlds = computeBoneWorldMatrices(skel.bones, 0, 0);
    const root = new THREE.Group();
    const pivot = new THREE.Group();
    pivot.rotation.x = -Math.PI / 2;
    pivot.scale.setScalar(0.013);
    root.add(pivot);
    for (const part of [
      'NPC/FemaleHead01.bmd',
      'NPC/FemaleUpper01.bmd',
      'NPC/FemaleLower01.bmd',
      'NPC/FemaleBoots01.bmd',
    ]) {
      const b = await loadBmdAsset(part);
      if (b) await buildMeshesIntoParent(b, 'NPC', boneWorlds, pivot, { maxAnisotropy, doubleSided: true });
    }
    normalizeGroupGround(root, pivot, 2.25);
    return root;
  }

  const fileMap = {
    smith: 'NPC/Smith01.bmd',
    wizard: 'NPC/Wizard01.bmd',
    chaos: 'NPC/MixNpc01.bmd',
  };
  return buildGroupFromBmd(fileMap[npcId], {
    targetHeight: npcId === 'chaos' ? 2.0 : 2.35,
    actionIdx: 0,
    maxAnisotropy,
    doubleSided: true,
  });
}

export function resolveMonsterBmdPath(templateId: string): { path: string; tint?: number; height: number } | null {
  const id = templateId.toLowerCase();
  if (id.includes('budge')) return { path: 'Monster/Monster03.bmd', tint: id.includes('golden') ? 0xfbbf24 : undefined, height: id.includes('golden') ? 2.4 : 1.7 };
  if (id.includes('bull')) return { path: 'Monster/Monster01.bmd', tint: id.includes('poison') ? 0x4ade80 : undefined, height: 2.65 };
  if (id.includes('hound')) return { path: 'Monster/Monster02.bmd', height: 1.85 };
  if (id.includes('skeleton')) return { path: 'Monster/Monster08.bmd', tint: 0xe2e8f0, height: 2.3 };
  if (id.includes('ghost')) return { path: 'Monster/Monster08.bmd', tint: 0xc084fc, height: 2.4 };
  if (id.includes('red-dragon') || id.includes('dragon')) return { path: 'Monster/Monster32.bmd', height: 4.1 };
  if (id.includes('gorgon')) return { path: 'Monster/Monster12.bmd', height: 3.7 };
  if (id.includes('ice-monster')) return { path: 'Monster/Monster16.bmd', height: 2.5 };
  if (id.includes('yeti')) return { path: 'Monster/Monster14.bmd', height: 2.75 };
  if (id.includes('ice-queen')) return { path: 'Monster/Monster19.bmd', height: 3.4 };
  if (id.includes('goblin')) return { path: 'Monster/Monster20.bmd', height: 2.15 };
  if (id.includes('golem')) return { path: 'Monster/Monster26.bmd', height: 2.95 };
  if (id.includes('bali')) return { path: 'Monster/Monster33.bmd', height: 3.7 };
  if (id.includes('tantallos')) return { path: 'Monster/Monster43.bmd', tint: 0xfbbf24, height: 3.8 };
  return null;
}

export async function loadMonsterBmdGroup(
  templateId: string,
  isBoss: boolean,
  maxAnisotropy: number,
  materialsOut: THREE.MeshStandardMaterial[]
): Promise<THREE.Group | null> {
  const spec = resolveMonsterBmdPath(templateId);
  if (!spec) return null;
  return buildGroupFromBmd(spec.path, {
    targetHeight: isBoss ? Math.max(3.4, spec.height) : spec.height,
    actionIdx: 0,
    tintColor: spec.tint,
    maxAnisotropy,
    doubleSided: true,
    materialsOut,
  });
}
