import * as THREE from 'three';
import type { MapId } from '../types/game';

export interface MapTextureSet {
  ground: THREE.CanvasTexture;
  safeStone: THREE.CanvasTexture;
}

function makeCanvas(size = 256): [HTMLCanvasElement, CanvasRenderingContext2D] {
  const canvas = document.createElement('canvas');
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext('2d')!;
  return [canvas, ctx];
}

function finalizeColorTexture(
  canvas: HTMLCanvasElement,
  maxAnisotropy: number,
  repeatX = 1,
  repeatY = 1
): THREE.CanvasTexture {
  const tex = new THREE.CanvasTexture(canvas);
  tex.colorSpace = THREE.SRGBColorSpace;
  tex.wrapS = THREE.RepeatWrapping;
  tex.wrapT = THREE.RepeatWrapping;
  tex.repeat.set(repeatX, repeatY);
  tex.anisotropy = maxAnisotropy;
  tex.needsUpdate = true;
  return tex;
}

export function createMapTextures(mapId: MapId, maxAnisotropy: number): MapTextureSet {
  // Ground outer terrain texture
  const [gCanvas, gCtx] = makeCanvas(256);
  const palettes: Record<MapId, { base: string; alt: string; speck: string; vein: string }> = {
    lorencia: { base: '#212b1b', alt: '#2a3721', speck: '#161d12', vein: '#3c4a2b' },
    dungeon: { base: '#16141e', alt: '#1f1b2c', speck: '#0d0b12', vein: '#312945' },
    devias: { base: '#8ba4bc', alt: '#a7bfd4', speck: '#647d96', vein: '#d4e6f5' },
    noria: { base: '#152e24', alt: '#1c3d2f', speck: '#0c1f17', vein: '#2c5a46' },
    custom: { base: '#271418', alt: '#351b21', speck: '#170a0d', vein: '#4d242d' },
  };
  const pal = palettes[mapId];

  gCtx.fillStyle = pal.base;
  gCtx.fillRect(0, 0, 256, 256);

  // Subtle deterministic patches
  for (let i = 0; i < 420; i++) {
    const x = ((i * 73) % 256);
    const y = ((i * 137) % 256);
    const r = 4 + (i % 14);
    gCtx.fillStyle = i % 3 === 0 ? pal.alt : i % 3 === 1 ? pal.speck : pal.vein;
    gCtx.globalAlpha = 0.28;
    gCtx.beginPath();
    gCtx.arc(x, y, r, 0, Math.PI * 2);
    gCtx.fill();
  }
  gCtx.globalAlpha = 1;

  // Cobblestone grid hint for Dungeon / Arena
  if (mapId === 'dungeon' || mapId === 'custom') {
    gCtx.strokeStyle = 'rgba(0,0,0,0.35)';
    gCtx.lineWidth = 2;
    for (let x = 0; x <= 256; x += 64) {
      gCtx.beginPath();
      gCtx.moveTo(x, 0);
      gCtx.lineTo(x, 256);
      gCtx.stroke();
      gCtx.beginPath();
      gCtx.moveTo(0, x);
      gCtx.lineTo(256, x);
      gCtx.stroke();
    }
  }

  // Safe Zone MU Stone Cobblestone
  const [sCanvas, sCtx] = makeCanvas(256);
  const safeBase =
    mapId === 'devias' ? '#46586e' : mapId === 'noria' ? '#364d42' : mapId === 'dungeon' ? '#2c273a' : '#423c34';
  const safeDark =
    mapId === 'devias' ? '#2c394a' : mapId === 'noria' ? '#203029' : mapId === 'dungeon' ? '#1c1826' : '#29241f';
  const safeTrim =
    mapId === 'devias' ? '#7dd3fc' : mapId === 'noria' ? '#34d399' : mapId === 'dungeon' ? '#c084fc' : '#d97706';

  sCtx.fillStyle = safeBase;
  sCtx.fillRect(0, 0, 256, 256);

  const brickW = 64;
  const brickH = 32;
  for (let row = 0; row < 8; row++) {
    const offset = row % 2 === 0 ? 0 : brickW / 2;
    for (let col = -1; col < 5; col++) {
      const bx = col * brickW + offset;
      const by = row * brickH;
      sCtx.fillStyle = (row + col) % 2 === 0 ? safeBase : safeDark;
      sCtx.fillRect(bx + 2, by + 2, brickW - 4, brickH - 4);
      sCtx.strokeStyle = 'rgba(0,0,0,0.5)';
      sCtx.lineWidth = 2;
      sCtx.strokeRect(bx + 1, by + 1, brickW - 2, brickH - 2);
      sCtx.strokeStyle = 'rgba(255,255,255,0.08)';
      sCtx.lineWidth = 1;
      sCtx.beginPath();
      sCtx.moveTo(bx + 3, by + 3);
      sCtx.lineTo(bx + brickW - 3, by + 3);
      sCtx.stroke();
    }
  }
  // Subtle golden rune accent on stone
  sCtx.strokeStyle = safeTrim;
  sCtx.globalAlpha = 0.14;
  sCtx.lineWidth = 2;
  sCtx.strokeRect(8, 8, 240, 240);
  sCtx.globalAlpha = 1;

  const groundTex = finalizeColorTexture(gCanvas, maxAnisotropy, 38, 38);
  const safeStoneTex = finalizeColorTexture(sCanvas, maxAnisotropy, 6, 6);

  const realGroundMap: Record<MapId, string> = {
    lorencia: '/mu-data/World1/TileGrass01.jpg',
    dungeon: '/mu-data/World2/TileGround01.jpg',
    devias: '/mu-data/World3/TileGrass01.jpg',
    noria: '/mu-data/World4/TileGrass01.jpg',
    custom: '/mu-data/World2/TileGround01.jpg',
  };

  const groundImg = new Image();
  groundImg.onload = () => {
    gCtx.drawImage(groundImg, 0, 0, 256, 256);
    if (mapId === 'custom') {
      gCtx.fillStyle = 'rgba(136, 19, 55, 0.35)';
      gCtx.fillRect(0, 0, 256, 256);
    }
    groundTex.needsUpdate = true;
  };
  groundImg.src = realGroundMap[mapId];

  if (mapId === 'lorencia' || mapId === 'dungeon') {
    const stoneImg = new Image();
    stoneImg.onload = () => {
      sCtx.drawImage(stoneImg, 0, 0, 256, 256);
      sCtx.fillStyle = 'rgba(15, 12, 10, 0.16)';
      sCtx.fillRect(0, 0, 256, 256);
      safeStoneTex.needsUpdate = true;
    };
    stoneImg.src = mapId === 'lorencia' ? '/mu-data/World1/TileGround01.jpg' : '/mu-data/World2/TileGround01.jpg';
  }

  return {
    ground: groundTex,
    safeStone: safeStoneTex,
  };
}

export function createContactShadowTexture(): THREE.CanvasTexture {
  const [canvas, ctx] = makeCanvas(128);
  const grad = ctx.createRadialGradient(64, 64, 6, 64, 64, 60);
  grad.addColorStop(0, 'rgba(0, 0, 0, 0.72)');
  grad.addColorStop(0.55, 'rgba(0, 0, 0, 0.35)');
  grad.addColorStop(1, 'rgba(0, 0, 0, 0)');
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, 128, 128);
  const tex = new THREE.CanvasTexture(canvas);
  tex.needsUpdate = true;
  return tex;
}

export function createMetalArmorTexture(baseHex: string, accentHex: string, maxAnisotropy: number): THREE.CanvasTexture {
  const [canvas, ctx] = makeCanvas(128);
  const grad = ctx.createLinearGradient(0, 0, 128, 128);
  grad.addColorStop(0, baseHex);
  grad.addColorStop(0.5, accentHex);
  grad.addColorStop(1, baseHex);
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, 128, 128);

  // Armor plate seams
  ctx.strokeStyle = 'rgba(0,0,0,0.42)';
  ctx.lineWidth = 3;
  for (let y = 16; y < 128; y += 32) {
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(128, y);
    ctx.stroke();
  }
  ctx.strokeStyle = 'rgba(255,255,255,0.25)';
  ctx.lineWidth = 1.5;
  ctx.strokeRect(4, 4, 120, 120);

  return finalizeColorTexture(canvas, maxAnisotropy, 1, 1);
}

export function createLabelSprite(
  text: string,
  color: string,
  hpRatio?: number,
  isBoss = false
): THREE.Sprite {
  const canvas = document.createElement('canvas');
  canvas.width = 256;
  canvas.height = 64;
  const ctx = canvas.getContext('2d')!;

  ctx.clearRect(0, 0, 256, 64);

  // Crisp dark pill/plate behind nameplate
  ctx.font = isBoss ? 'bold 20px "Plus Jakarta Sans", sans-serif' : 'bold 18px "Plus Jakarta Sans", sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';

  ctx.fillStyle = 'rgba(7, 9, 15, 0.72)';
  ctx.fillRect(24, 6, 208, 28);
  ctx.strokeStyle = isBoss ? 'rgba(251, 191, 36, 0.8)' : 'rgba(255, 255, 255, 0.18)';
  ctx.lineWidth = 1.5;
  ctx.strokeRect(24, 6, 208, 28);

  ctx.fillStyle = color;
  ctx.fillText(text.slice(0, 22), 128, 20);

  if (typeof hpRatio === 'number') {
    const barW = isBoss ? 176 : 136;
    const barX = (256 - barW) / 2;
    ctx.fillStyle = '#090d16';
    ctx.fillRect(barX, 40, barW, 10);
    ctx.fillStyle = isBoss ? '#f59e0b' : '#ef4444';
    ctx.fillRect(barX + 1, 41, Math.max(0, (barW - 2) * Math.max(0, Math.min(1, hpRatio))), 8);
    ctx.strokeStyle = 'rgba(255,255,255,0.3)';
    ctx.lineWidth = 1;
    ctx.strokeRect(barX, 40, barW, 10);
  }

  const tex = new THREE.CanvasTexture(canvas);
  tex.colorSpace = THREE.SRGBColorSpace;
  tex.needsUpdate = true;

  const mat = new THREE.SpriteMaterial({
    map: tex,
    transparent: true,
    depthTest: false,
    depthWrite: false,
  });
  const sprite = new THREE.Sprite(mat);
  sprite.scale.set(isBoss ? 4.4 : 3.4, isBoss ? 1.1 : 0.85, 1);
  sprite.renderOrder = 20;
  return sprite;
}

export function updateLabelSprite(
  sprite: THREE.Sprite,
  text: string,
  color: string,
  hpRatio?: number,
  isBoss = false
) {
  const mat = sprite.material as THREE.SpriteMaterial;
  const oldTex = mat.map;
  const canvas = oldTex?.image as HTMLCanvasElement | undefined;
  if (!canvas) return;
  const ctx = canvas.getContext('2d')!;
  ctx.clearRect(0, 0, 256, 64);

  ctx.font = isBoss ? 'bold 20px "Plus Jakarta Sans", sans-serif' : 'bold 18px "Plus Jakarta Sans", sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';

  ctx.fillStyle = 'rgba(7, 9, 15, 0.72)';
  ctx.fillRect(24, 6, 208, 28);
  ctx.strokeStyle = isBoss ? 'rgba(251, 191, 36, 0.8)' : 'rgba(255, 255, 255, 0.18)';
  ctx.lineWidth = 1.5;
  ctx.strokeRect(24, 6, 208, 28);

  ctx.fillStyle = color;
  ctx.fillText(text.slice(0, 22), 128, 20);

  if (typeof hpRatio === 'number') {
    const barW = isBoss ? 176 : 136;
    const barX = (256 - barW) / 2;
    ctx.fillStyle = '#090d16';
    ctx.fillRect(barX, 40, barW, 10);
    ctx.fillStyle = isBoss ? '#f59e0b' : '#ef4444';
    ctx.fillRect(barX + 1, 41, Math.max(0, (barW - 2) * Math.max(0, Math.min(1, hpRatio))), 8);
    ctx.strokeStyle = 'rgba(255,255,255,0.3)';
    ctx.lineWidth = 1;
    ctx.strokeRect(barX, 40, barW, 10);
  }

  if (oldTex) oldTex.needsUpdate = true;
}

export function createFloatingTextSprite(text: string, color: string, isCrit: boolean): THREE.Sprite {
  const canvas = document.createElement('canvas');
  canvas.width = 256;
  canvas.height = 64;
  const ctx = canvas.getContext('2d')!;
  ctx.font = isCrit ? '900 32px "JetBrains Mono", monospace' : '700 26px "JetBrains Mono", monospace';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.lineWidth = 5;
  ctx.strokeStyle = 'rgba(5, 7, 12, 0.92)';
  ctx.strokeText(text, 128, 32);
  ctx.fillStyle = color;
  ctx.fillText(text, 128, 32);

  const tex = new THREE.CanvasTexture(canvas);
  tex.colorSpace = THREE.SRGBColorSpace;
  tex.needsUpdate = true;

  const mat = new THREE.SpriteMaterial({
    map: tex,
    transparent: true,
    depthTest: false,
    depthWrite: false,
  });
  const sprite = new THREE.Sprite(mat);
  sprite.scale.set(isCrit ? 3.8 : 3.0, isCrit ? 0.95 : 0.75, 1);
  sprite.renderOrder = 30;
  return sprite;
}
