import express from 'express';
import http from 'http';
import os from 'os';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { WebSocketServer, WebSocket } from 'ws';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

interface LanPlayerState {
  id: string;
  name: string;
  classId: 'dk' | 'dw' | 'elf';
  level: number;
  resets: number;
  mapId: string;
  x: number;
  y: number;
  dir: number;
  wingsTier: number;
  weaponPlus: number;
  hp: number;
  maxHp: number;
  lastSeen: number;
}

interface LanRoomBoss {
  id: string;
  name: string;
  mapId: string;
  hp: number;
  maxHp: number;
  x: number;
  y: number;
  respawnAt: number;
}

interface LanEvent {
  id: string;
  name: string;
  mapId: string;
  active: boolean;
  endsAt: number;
  startedBy: string;
  rewardZen: number;
}

interface ServerTradeState {
  tradeId: string;
  playerA: string;
  playerB: string;
  itemsA: unknown[];
  zenA: number;
  confirmedA: boolean;
  itemsB: unknown[];
  zenB: number;
  confirmedB: boolean;
}

interface LanRoom {
  code: string;
  hostName: string;
  createdAt: number;
  players: Map<string, { ws: WebSocket; state: LanPlayerState }>;
  bosses: Map<string, LanRoomBoss>;
  activeEvent: LanEvent | null;
  trades: Map<string, ServerTradeState>;
}

const rooms = new Map<string, LanRoom>();

function createDefaultBosses(): Map<string, LanRoomBoss> {
  const map = new Map<string, LanRoomBoss>();
  const defaults: LanRoomBoss[] = [
    { id: 'lan-boss-lorencia', name: 'Red Dragon', mapId: 'lorencia', hp: 1800, maxHp: 1800, x: 780, y: 680, respawnAt: 0 },
    { id: 'lan-boss-dungeon', name: 'Gorgon', mapId: 'dungeon', hp: 4200, maxHp: 4200, x: 640, y: 520, respawnAt: 0 },
    { id: 'lan-boss-devias', name: 'Ice Queen', mapId: 'devias', hp: 6500, maxHp: 6500, x: 720, y: 460, respawnAt: 0 },
    { id: 'lan-boss-noria', name: 'Golden Tantallos', mapId: 'noria', hp: 9000, maxHp: 9000, x: 680, y: 640, respawnAt: 0 },
  ];
  for (const b of defaults) {
    map.set(b.id, b);
  }
  return map;
}

function getLocalLanUrls(port: number): string[] {
  const interfaces = os.networkInterfaces();
  const urls: string[] = [];
  for (const name of Object.keys(interfaces)) {
    const entries = interfaces[name];
    if (!entries) continue;
    for (const entry of entries) {
      if (entry.family === 'IPv4' && !entry.internal) {
        urls.push(`http://${entry.address}:${port}`);
      }
    }
  }
  return urls;
}

function broadcastToRoom(room: LanRoom, payload: unknown, excludeId?: string) {
  const raw = JSON.stringify(payload);
  for (const [playerId, client] of room.players.entries()) {
    if (excludeId && playerId === excludeId) continue;
    if (client.ws.readyState === WebSocket.OPEN) {
      client.ws.send(raw);
    }
  }
}

async function startServer() {
  const app = express();
  const server = http.createServer(app);
  const port = Number(process.env.PORT || 3000);

  app.use(express.json());

  app.get('/api/lan/info', (_req, res) => {
    const lanUrls = getLocalLanUrls(port);
    res.json({
      port,
      lanUrls,
      activeRoomCount: rooms.size,
      appUrl: process.env.APP_URL || null,
    });
  });

  app.get('/api/lan/rooms', (_req, res) => {
    const list = Array.from(rooms.values()).map((room) => ({
      code: room.code,
      hostName: room.hostName,
      playerCount: room.players.size,
      createdAt: room.createdAt,
      players: Array.from(room.players.values()).map((p) => ({
        name: p.state.name,
        classId: p.state.classId,
        level: p.state.level,
        mapId: p.state.mapId,
      })),
    }));
    res.json({ rooms: list });
  });

  const wss = new WebSocketServer({ noServer: true });

  server.on('upgrade', (request, socket, head) => {
    const pathname = new URL(request.url || '/', 'http://localhost').pathname;
    if (pathname === '/ws-lan') {
      wss.handleUpgrade(request, socket, head, (ws) => {
        wss.emit('connection', ws, request);
      });
    }
  });

  wss.on('connection', (ws) => {
    let currentRoomCode: string | null = null;
    let currentPlayerId: string | null = null;

    const leaveCurrentRoom = () => {
      if (!currentRoomCode || !currentPlayerId) return;
      const room = rooms.get(currentRoomCode);
      if (room) {
        room.players.delete(currentPlayerId);
        broadcastToRoom(room, {
          type: 'player:left',
          playerId: currentPlayerId,
        });
        if (room.players.size === 0) {
          rooms.delete(currentRoomCode);
        }
      }
      currentRoomCode = null;
      currentPlayerId = null;
    };

    ws.on('message', (data) => {
      let msg: Record<string, unknown>;
      try {
        msg = JSON.parse(data.toString());
      } catch {
        return;
      }

      const type = msg.type;
      if (type === 'room:join') {
        leaveCurrentRoom();
        const rawCode = String(msg.roomCode || 'LORENCIA-01').trim().toUpperCase().slice(0, 24);
        const roomCode = rawCode || 'LORENCIA-01';
        const p = (msg.player || {}) as Partial<LanPlayerState>;
        const playerId = String(p.id || `p-${Math.random().toString(36).slice(2, 10)}`);

        let room = rooms.get(roomCode);
        if (!room) {
          room = {
            code: roomCode,
            hostName: String(p.name || 'Guerrero'),
            createdAt: Date.now(),
            players: new Map(),
            bosses: createDefaultBosses(),
            activeEvent: null,
            trades: new Map(),
          };
          rooms.set(roomCode, room);
        }

        const state: LanPlayerState = {
          id: playerId,
          name: String(p.name || 'Guerrero').slice(0, 20),
          classId: p.classId === 'dw' || p.classId === 'elf' ? p.classId : 'dk',
          level: Number(p.level || 1),
          resets: Number(p.resets || 0),
          mapId: String(p.mapId || 'lorencia'),
          x: Number(p.x ?? 480),
          y: Number(p.y ?? 480),
          dir: Number(p.dir ?? 0),
          wingsTier: Number(p.wingsTier ?? 0),
          weaponPlus: Number(p.weaponPlus ?? 0),
          hp: Number(p.hp ?? 180),
          maxHp: Number(p.maxHp ?? 180),
          lastSeen: Date.now(),
        };

        currentRoomCode = roomCode;
        currentPlayerId = playerId;
        room.players.set(playerId, { ws, state });

        ws.send(JSON.stringify({
          type: 'room:state',
          roomCode: room.code,
          hostName: room.hostName,
          players: Array.from(room.players.values()).map((c) => c.state),
          bosses: Array.from(room.bosses.values()),
          activeEvent: room.activeEvent,
        }));

        broadcastToRoom(room, {
          type: 'player:joined',
          player: state,
        }, playerId);
        return;
      }

      if (!currentRoomCode || !currentPlayerId) return;
      const room = rooms.get(currentRoomCode);
      if (!room) return;
      const entry = room.players.get(currentPlayerId);
      if (!entry) return;

      if (type === 'player:move') {
        entry.state.x = Number(msg.x ?? entry.state.x);
        entry.state.y = Number(msg.y ?? entry.state.y);
        entry.state.dir = Number(msg.dir ?? entry.state.dir);
        entry.state.mapId = String(msg.mapId ?? entry.state.mapId);
        entry.state.level = Number(msg.level ?? entry.state.level);
        entry.state.resets = Number(msg.resets ?? entry.state.resets);
        entry.state.wingsTier = Number(msg.wingsTier ?? entry.state.wingsTier);
        entry.state.weaponPlus = Number(msg.weaponPlus ?? entry.state.weaponPlus);
        entry.state.hp = Number(msg.hp ?? entry.state.hp);
        entry.state.maxHp = Number(msg.maxHp ?? entry.state.maxHp);
        entry.state.lastSeen = Date.now();

        broadcastToRoom(room, {
          type: 'player:updated',
          player: entry.state,
        }, currentPlayerId);
      } else if (type === 'player:skill') {
        broadcastToRoom(room, {
          type: 'player:cast',
          playerId: currentPlayerId,
          skillId: String(msg.skillId || 'slash'),
          mapId: entry.state.mapId,
          x: Number(msg.x ?? entry.state.x),
          y: Number(msg.y ?? entry.state.y),
          targetX: Number(msg.targetX ?? entry.state.x),
          targetY: Number(msg.targetY ?? entry.state.y),
        }, currentPlayerId);
      } else if (type === 'boss:hit') {
        const bossId = String(msg.bossId || '');
        const dmg = Math.max(1, Math.min(50000, Number(msg.damage || 0)));
        const boss = room.bosses.get(bossId);
        if (!boss) return;
        const now = Date.now();
        if (boss.respawnAt > now) return;
        boss.hp = Math.max(0, boss.hp - dmg);
        if (boss.hp === 0) {
          boss.respawnAt = now + 20000;
          broadcastToRoom(room, {
            type: 'boss:killed',
            bossId: boss.id,
            killerName: entry.state.name,
            respawnAt: boss.respawnAt,
          });
          setTimeout(() => {
            const r = rooms.get(currentRoomCode || '');
            const b = r?.bosses.get(bossId);
            if (r && b) {
              b.hp = b.maxHp;
              b.respawnAt = 0;
              broadcastToRoom(r, { type: 'boss:respawned', boss: b });
            }
          }, 20000);
        } else {
          broadcastToRoom(room, {
            type: 'boss:updated',
            bossId: boss.id,
            hp: boss.hp,
          });
        }
      } else if (type === 'event:start') {
        const kind = msg.eventType === 'blood-castle' ? 'blood-castle' : 'golden-invasion';
        const ev: LanEvent = kind === 'blood-castle'
          ? {
              id: `ev-${Date.now()}`,
              name: 'Blood Castle Cooperativo',
              mapId: 'dungeon',
              active: true,
              endsAt: Date.now() + 90000,
              startedBy: entry.state.name,
              rewardZen: 12000,
            }
          : {
              id: `ev-${Date.now()}`,
              name: 'Invasión Dorada de Lorencia',
              mapId: 'lorencia',
              active: true,
              endsAt: Date.now() + 90000,
              startedBy: entry.state.name,
              rewardZen: 8000,
            };
        room.activeEvent = ev;
        const bossId = kind === 'blood-castle' ? 'lan-boss-dungeon' : 'lan-boss-lorencia';
        const boss = room.bosses.get(bossId);
        if (boss) {
          boss.hp = boss.maxHp;
          boss.respawnAt = 0;
          broadcastToRoom(room, { type: 'boss:respawned', boss });
        }
        broadcastToRoom(room, { type: 'event:started', event: ev });
      } else if (type === 'trade:request') {
        const targetId = String(msg.targetPlayerId || '');
        const target = room.players.get(targetId);
        if (!target || targetId === currentPlayerId) return;
        const tradeId = `tr-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
        room.trades.set(tradeId, {
          tradeId,
          playerA: currentPlayerId,
          playerB: targetId,
          itemsA: [],
          zenA: 0,
          confirmedA: false,
          itemsB: [],
          zenB: 0,
          confirmedB: false,
        });
        if (target.ws.readyState === WebSocket.OPEN) {
          target.ws.send(JSON.stringify({
            type: 'trade:incoming',
            tradeId,
            fromPlayerId: currentPlayerId,
            fromPlayerName: entry.state.name,
          }));
        }
      } else if (type === 'trade:accept') {
        const tradeId = String(msg.tradeId || '');
        const tr = room.trades.get(tradeId);
        if (!tr || tr.playerB !== currentPlayerId) return;
        const pA = room.players.get(tr.playerA);
        const pB = room.players.get(tr.playerB);
        if (!pA || !pB) return;
        if (pA.ws.readyState === WebSocket.OPEN) {
          pA.ws.send(JSON.stringify({
            type: 'trade:started',
            tradeId,
            partnerId: pB.state.id,
            partnerName: pB.state.name,
          }));
        }
        if (pB.ws.readyState === WebSocket.OPEN) {
          pB.ws.send(JSON.stringify({
            type: 'trade:started',
            tradeId,
            partnerId: pA.state.id,
            partnerName: pA.state.name,
          }));
        }
      } else if (type === 'trade:decline' || type === 'trade:cancel') {
        const tradeId = String(msg.tradeId || '');
        const tr = room.trades.get(tradeId);
        if (!tr) return;
        room.trades.delete(tradeId);
        const pA = room.players.get(tr.playerA);
        const pB = room.players.get(tr.playerB);
        for (const p of [pA, pB]) {
          if (p && p.ws.readyState === WebSocket.OPEN) {
            p.ws.send(JSON.stringify({ type: 'trade:cancelled', tradeId }));
          }
        }
      } else if (type === 'trade:update') {
        const tradeId = String(msg.tradeId || '');
        const tr = room.trades.get(tradeId);
        if (!tr) return;
        const items = Array.isArray(msg.items) ? msg.items : [];
        const zen = Math.max(0, Number(msg.zen || 0));
        if (tr.playerA === currentPlayerId) {
          tr.itemsA = items;
          tr.zenA = zen;
          tr.confirmedA = false;
          tr.confirmedB = false;
          const pB = room.players.get(tr.playerB);
          pB?.ws.send(JSON.stringify({ type: 'trade:partner_updated', tradeId, items, zen }));
        } else if (tr.playerB === currentPlayerId) {
          tr.itemsB = items;
          tr.zenB = zen;
          tr.confirmedA = false;
          tr.confirmedB = false;
          const pA = room.players.get(tr.playerA);
          pA?.ws.send(JSON.stringify({ type: 'trade:partner_updated', tradeId, items, zen }));
        }
      } else if (type === 'trade:confirm') {
        const tradeId = String(msg.tradeId || '');
        const tr = room.trades.get(tradeId);
        if (!tr) return;
        if (tr.playerA === currentPlayerId) tr.confirmedA = true;
        else if (tr.playerB === currentPlayerId) tr.confirmedB = true;

        const pA = room.players.get(tr.playerA);
        const pB = room.players.get(tr.playerB);
        if (tr.confirmedA && tr.confirmedB) {
          room.trades.delete(tradeId);
          pA?.ws.send(JSON.stringify({
            type: 'trade:completed',
            tradeId,
            receivedItems: tr.itemsB,
            receivedZen: tr.zenB,
            givenItems: tr.itemsA,
            givenZen: tr.zenA,
          }));
          pB?.ws.send(JSON.stringify({
            type: 'trade:completed',
            tradeId,
            receivedItems: tr.itemsA,
            receivedZen: tr.zenA,
            givenItems: tr.itemsB,
            givenZen: tr.zenB,
          }));
        } else {
          if (tr.playerA === currentPlayerId) {
            pB?.ws.send(JSON.stringify({ type: 'trade:partner_confirmed', tradeId, confirmed: true }));
          } else {
            pA?.ws.send(JSON.stringify({ type: 'trade:partner_confirmed', tradeId, confirmed: true }));
          }
        }
      } else if (type === 'chat:send') {
        const text = String(msg.text || '').trim().slice(0, 180);
        if (!text) return;
        broadcastToRoom(room, {
          type: 'chat:message',
          id: `${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
          senderId: currentPlayerId,
          senderName: entry.state.name,
          classId: entry.state.classId,
          text,
          timestamp: Date.now(),
        });
      } else if (type === 'room:leave') {
        leaveCurrentRoom();
      }
    });

    ws.on('close', () => {
      leaveCurrentRoom();
    });
  });

  await new Promise<void>((resolve) => {
    server.listen(port, '0.0.0.0', () => {
      console.log(`Mu Offline Mobile server listening on http://0.0.0.0:${port}`);
      resolve();
    });
  });

  if (process.env.NODE_ENV !== 'production') {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: {
        middlewareMode: true,
        hmr: { server },
      },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distDir = path.resolve(__dirname, 'dist');
    app.use(express.static(distDir));
    app.get('*', (_req, res) => {
      const indexFile = path.join(distDir, 'index.html');
      if (fs.existsSync(indexFile)) {
        res.sendFile(indexFile);
      } else {
        res.status(404).send('Build not found');
      }
    });
  }
}

startServer();
