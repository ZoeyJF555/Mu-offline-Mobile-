#!/usr/bin/env python3
"""Decode a MU world (EncTerrainN.*) from an Ignies clone into compact JSON for the web viewer.
usage: build_world.py <ignies_clone> <world_number> <out_dir> <ObjectDirNumber> <tile names comma sep>"""
import sys, os, json, base64, struct, shutil
src, wn, out, objdir, tiles = sys.argv[1], int(sys.argv[2]), sys.argv[3], int(sys.argv[4]), sys.argv[5].split(',')
G = f'{src}/public/game-assets'
KEY = bytes([0xD1,0x73,0x52,0xF6,0xD2,0x9A,0xCB,0x27,0x3E,0xAF,0x59,0x31,0x37,0xB3,0xE7,0xA2])
BUX = bytes([0xFC,0xCF,0xAB])
def dec(b):
    o = bytearray(len(b)); k = 0x5E
    for i, c in enumerate(b):
        o[i] = ((c ^ KEY[i % 16]) - k) & 0xFF
        k = (c + 0x3D) & 0xFF
    return bytes(o)
N = 256 * 256
W = f'{G}/World{wn}'
hb = open(f'{W}/TerrainHeight.OZB', 'rb').read()
heights = hb[1084:1084 + N]
m = dec(open(f'{W}/EncTerrain{wn}.map', 'rb').read())
l1, l2, al = m[2:2+N], m[2+N:2+2*N], m[2+2*N:2+3*N]
a = bytearray(dec(open(f'{W}/EncTerrain{wn}.att', 'rb').read()))
for i in range(len(a)): a[i] ^= BUX[i % 3]
att = bytes(a[4:4+N]) if len(a) == N + 4 else bytes((a[4+i*2] for i in range(N)))
o = dec(open(f'{W}/EncTerrain{wn}.obj', 'rb').read())
cnt = struct.unpack_from('<h', o, 2)[0]
objs = []
for j in range(cnt):
    id_, x, y, z, rx, ry, rz, s = struct.unpack_from('<h7f', o, 4 + j * 30)
    objs.append([id_, round(x, 1), round(y, 1), round(z, 1), round(rx, 2), round(ry, 2), round(rz, 2), round(s, 3)])
b64 = lambda b: base64.b64encode(b).decode()
os.makedirs(f'{out}/tiles', exist_ok=True)
for t in tiles:
    p = f'{W}/{t}.jpg'
    if os.path.exists(p): shutil.copy(p, f'{out}/tiles/')
lj = f'{W}/TerrainLight.jpg'
if os.path.exists(lj): shutil.copy(lj, f'{out}/TerrainLight.jpg')
json.dump({'world': wn, 'objectDir': f'Object{objdir}', 'tiles': tiles, 'size': 256,
           'height': b64(heights), 'layer1': b64(l1), 'layer2': b64(l2), 'alpha': b64(al), 'att': b64(att),
           'objects': objs}, open(f'{out}/world{wn}.json', 'w'), separators=(',', ':'))
print('objects', len(objs), 'distinct types', len({x[0] for x in objs}), 'heights max', max(heights))
