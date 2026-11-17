import re, json, os, sys
SRC=sys.argv[1]; OUT=sys.argv[2]
os.makedirs(OUT,exist_ok=True)
# items
lines=[l.rstrip('\r\n') for l in open(f'{SRC}/tools/Item.txt',encoding='utf-8',errors='replace')]
cols=None; group=None; items=[]
for l in lines:
    s=l.strip()
    if s.startswith('//Index'): cols=re.split(r'\s+',s[2:]) ; continue
    if not s or s.startswith('//'): continue
    if re.fullmatch(r'\d+',s): group=int(s); continue
    if s=='end': group=None; continue
    if group is None: continue
    m=re.search(r'"([^"]*)"',s)
    if not m: continue
    pre=s[:m.start()].split(); post=s[m.end():].split()
    if len(pre)<8: continue
    nums=[int(x) if re.fullmatch(r'-?\d+',x) else x for x in post]
    items.append({"group":group,"index":int(pre[0]),"slot":int(pre[1]),"skill":int(pre[2]),
      "w":int(pre[3]),"h":int(pre[4]),"name":m.group(1),"stats":nums})
json.dump({"columnsAfterName":"itemLvl,dmgMin,dmgMax,speed,durability,magicDur,magicPwr,reqLvl,str,agi,ene,vit,cmd,type,DW,DK,Elf,MG,DL,SUM(approx; some groups differ)","items":items},open(f'{OUT}/items.json','w'),ensure_ascii=False,separators=(',',':'))
print('items',len(items))
# monsters
mons=[]; d=f'{SRC}/versions/shared/monsters'
for n in sorted(os.listdir(d)):
    p=f'{d}/{n}/index.ts'
    if not os.path.exists(p): continue
    t=open(p,encoding='utf-8').read()
    g=lambda k:(re.search(k,t) or [None,None])[1]
    numb=g(r'Numb:\s*(\d+)'); name=g(r"Name:\s*'((?:[^'\\]|\\.)*)'"); mt=g(r'type:\s*(\d+)')
    if numb is None or name is None: continue
    st={k:int(v) for k,v in re.findall(r'\b(Level|HP|MinDmg|MaxDmg|Def|AttackSuccess|Move|Range|View|MoveSpeed|AttSpeed|ItemR|MoneyR|MaxI):\s*(-?\d+)',t)}
    sc=g(r'scale:\s*([\d.]+)')
    mons.append({"id":int(numb),"name":name.replace("\\'","'"),"modelType":int(mt) if mt else None,
      "scale":float(sc) if sc else 1.0,"model":f"Monster/Monster{int(mt)+1:02d}.glb" if mt else None,"stats":st})
mons.sort(key=lambda m:m['id'])
json.dump(mons,open(f'{OUT}/monsters.json','w'),ensure_ascii=False,separators=(',',':'))
print('monsters',len(mons), sum(1 for m in mons if m['model']))
