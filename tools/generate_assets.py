#!/usr/bin/env python3
"""Deterministically author original pixel textures and five Structurize v1 gothic blueprints.

No third-party art is copied. NBT layout follows the pinned Structurize reader.
"""
import gzip
from collections import deque
import hashlib
import json
import math
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ART = RES / 'assets/minecolonies_cult'
PACK = RES / 'blueprints/minecolonies_cult/kingdoms_gate'

def s(value): return (8, value)
def i(value): return (3, value)
def compound(value): return (10, value)
def listing(kind, value): return (9, (kind, value))
def position(x, y, z): return compound(dict(x=i(x), y=i(y), z=i(z)))
def string(value):
    encoded = value.encode('utf-8')
    return struct.pack('>H', len(encoded)) + encoded

def payload(kind, value):
    if kind in (1, 2, 3): return struct.pack({1:'>b', 2:'>h', 3:'>i'}[kind], value)
    if kind == 8: return string(value)
    if kind == 9:
        subtype, items = value
        return bytes([subtype]) + struct.pack('>i', len(items)) + b''.join(payload(subtype, item) for item in items)
    if kind == 10: return b''.join(bytes([tag[0]]) + string(key) + payload(*tag) for key, tag in value.items()) + b'\0'
    if kind == 11: return struct.pack('>i', len(value)) + b''.join(struct.pack('>i', x if x < 2**31 else x - 2**32) for x in value)
    raise ValueError(kind)

def nbt(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(gzip.compress(b'\x0a\x00\x00' + payload(10, value), mtime=0))

def png(path, width, height, pixels):
    def chunk(kind, data): return struct.pack('>I',len(data)) + kind + data + struct.pack('>I',zlib.crc32(kind + data))
    scan = b''.join(b'\0' + bytes(sum((list(c) for c in row), [])) for row in pixels)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR',struct.pack('>IIBBBBB',width,height,8,6,0,0,0)) + chunk(b'IDAT',zlib.compress(scan,9)) + chunk(b'IEND',b''))

def textures():
    # Gate emblem: a violet void inside a gilded, pointed arch.
    icon = [[(0,0,0,0) for _ in range(32)] for _ in range(32)]
    for y in range(32):
        for x in range(32):
            distance = abs(x-15.5)
            arch = 6 + distance * .85
            if 5 <= x <= 26 and arch <= y <= 28:
                edge = distance > 8 or y < arch + 2 or y > 25
                icon[y][x] = (198,157,90,255) if edge else (53+(x*y)%19,22,88+(x*3+y)%30,255)
    png(ART/'textures/gui/cult_icon.png',32,32,icon)
    png(PACK/'kingdoms_gate.png',32,32,icon)
    tile = [[(38+(x*11+y*17)%12,30+(x*3+y)%8,45+(x+y*2)%10,255) for x in range(32)] for y in range(32)]
    for y in range(32):
        for x in range(32):
            if icon[y][x][3]: tile[y][x] = icon[y][x]
    png(ART/'textures/block/kingdoms_gate.png',32,32,tile)
    # Native citizen UV is 128x64. Unused hat and beard UV regions remain transparent.
    skin = [[(0,0,0,0) for _ in range(128)] for _ in range(64)]
    def fill(x0,y0,x1,y1,color):
        for y in range(y0,y1):
            for x in range(x0,x1):
                shade = ((x*7+y*11)%5)-2
                skin[y][x] = tuple(max(0,min(255,c+shade)) for c in color[:3])+(color[3],)
    purple=(49,25,68,255); trim=(176,139,78,255)
    fill(0,0,32,16,purple)
    fill(8,8,16,16,(24,19,32,255))
    fill(10,11,12,12,(203,167,231,255)); fill(13,11,15,12,(203,167,231,255))
    for box in [(0,16,16,32),(16,16,40,32),(40,16,56,32),(16,48,32,64),(32,48,48,64)]: fill(*box,purple)
    for box in [(20,20,22,30),(26,20,28,30),(16,29,40,31),(0,29,16,31),(16,61,32,63),(40,25,56,27),(32,57,48,59)]: fill(*box,trim)
    fill(23,22,25,26,(153,87,186,255))
    png(ART/'textures/entity/cult_leader.png',128,64,skin)

def blueprint(level):
    sx, sy, sz = 29, 24, 35
    anchor=(14,1,3)
    blocks={}
    def put(x,y,z,name,**properties):
        assert 0<=x<sx and 0<=y<sy and 0<=z<sz
        blocks[(x,y,z)] = {'Name':s(name if ':' in name else 'minecraft:'+name)}
        if properties: blocks[(x,y,z)]['Properties']=compound({key:s(str(value)) for key,value in properties.items()})
    def line(x0,y0,z0,x1,y1,z1,name,**props):
        for y in range(y0,y1+1):
            for z in range(z0,z1+1):
                for x in range(x0,x1+1): put(x,y,z,name,**props)
    # Constant footprint and anchor permit safe upgrades without moving the congregation area.
    for x in range(1,28):
        for z in range(1,34):
            put(x,0,z,'stone_bricks' if (x+z)%5 else 'cracked_stone_bricks')
            if 13<=x<=15:put(x,0,z,'purple_wool')
            radius=math.hypot(x-14,z-17)
            if 5<radius<6:put(x,0,z,'chiseled_stone_bricks' if level<4 else 'polished_deepslate')
    left={1:7,2:5,3:4,4:3,5:3}[level]; right=28-left
    front=10 if level==1 else 7; back=29; wall=3+level
    masonry='dark_oak_planks' if level==1 else ('stone_bricks' if level<4 else 'deepslate_bricks')
    for x in range(left,right+1):
        for z in range(front,back+1):
            if x in (left,right) or z in (front,back):
                for y in range(1,wall+1):
                    if z==front and 12<=x<=16 and y<=3:continue
                    material='dark_oak_log' if (x in (left,right) and z%5==0) else masonry
                    if x in (left,right) and z%5 in (2,3) and 2<=y<=wall-1:material='purple_stained_glass'
                    if z==back and 12<=x<=16 and 3<=y<=wall-1:material='purple_stained_glass'
                    put(x,y,z,material)
    # Steep gable roof, overhanging eaves, stone front gable and repeated exterior buttresses.
    for x in range(left-1,right+2):
        roof_y=wall+1+min(x-(left-1),(right+1)-x)
        for z in range(front-1,back+2):put(x,roof_y,z,'dark_oak_planks' if level<3 else 'deepslate_tiles')
        if front<=back:
            for y in range(wall+1,roof_y):
                put(x,y,front,masonry); put(x,y,back,masonry)
    if level>=2:
        for x in (left-1,right+1):
            for z in range(front+2,back,5):
                line(x,1,z,x,wall-1,z,'stone_bricks' if level<4 else 'polished_deepslate')
    # A free-standing ceremonial arch faces the congregation; focus remains clear of collision.
    arch_z=25; arch_top=5+level
    for x in (11,17):line(x,1,arch_z,x,arch_top-2,arch_z,'chiseled_stone_bricks' if level<3 else 'polished_deepslate')
    for x in range(11,18):put(x,arch_top-abs(x-14)//2,arch_z,'stone_bricks' if level<3 else 'amethyst_block')
    for x in (10,18):
        put(x,1,24,'purple_candle',candles='3',lit='true',waterlogged='false')
    for z in range(front+3,back,5):
        for x in (left+1,right-1):put(x,1,z,'lantern',hanging='false',waterlogged='false')
    if level>=3:
        for x in (left+1,right-1):
            for z in (front+1,back-1):line(x,1,z,x,2,z,'chiseled_stone_bricks')
    if level==5:
        for cx in (4,24):
            for y in range(1,19):
                for x in range(cx-2,cx+3):
                    for z in range(2,7):
                        if x in (cx-2,cx+2) or z in (2,6):put(x,y,z,'deepslate_bricks' if y%4 else 'polished_deepslate')
            for d in range(3):line(cx-2+d,19+d,2+d,cx+2-d,19+d,6-d,'deepslate_tiles')
    put(*anchor,'minecolonies_cult:kingdoms_gate',facing='north')
    # Every marked slot is a clear, supported standing position in the hall/courtyard.
    # Flood-fill from the public entrance; sealed tower interiors must never become gathering slots.
    reachable={(14,1)}; queue=deque(reachable)
    while queue:
        x,z=queue.popleft()
        for nx,nz in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)):
            if (nx,nz) not in reachable and (nx,0,nz) in blocks and (nx,1,nz) not in blocks and (nx,2,nz) not in blocks:
                reachable.add((nx,nz)); queue.append((nx,nz))
    slots=[]
    for z in range(5,24):
        for x in range(2,27):
            if (x,z) in reachable:slots.append((x,1,z))
    tags=[]
    for x,y,z in slots:
        tags.append({'tagPos':position(x-anchor[0],y-anchor[1],z-anchor[2]),'tagNameList':listing(10,[{'tagName':s('cult_gather')}])})
    tags.append({'tagPos':position(0,0,arch_z-anchor[2]),'tagNameList':listing(10,[{'tagName':s('cult_focus')}])})
    provider={'schematicName':s('kingdoms_gate'+str(level)), 'corner1':position(-14,-1,-3),
              'corner2':position(14,22,31), 'posTagMap':listing(10,tags), 'pack':s('Kingdom\'s Gate'),
              'path':s('mysticism/kingdoms_gate'+str(level)+'.blueprint')}
    tile={'id':s('minecolonies_cult:kingdoms_gate'),'x':i(anchor[0]),'y':i(anchor[1]),'z':i(anchor[2]),
          'blueprintDataProvider':compound(provider), 'registryName':s('minecolonies_cult:kingdoms_gate')}
    palette=[{'Name':s('minecraft:air')}]; indices={json.dumps(palette[0],sort_keys=True):0}; cells=[]
    for y in range(sy):
        for z in range(sz):
            for x in range(sx):
                block=blocks.get((x,y,z),palette[0]); key=json.dumps(block,sort_keys=True)
                if key not in indices:indices[key]=len(palette);palette.append(block)
                cells.append(indices[key])
    packed=[(cells[n]<<16) | (cells[n+1] if n+1<len(cells) else 0) for n in range(0,len(cells),2)]
    result={'version':(1,1),'size_x':(2,sx),'size_y':(2,sy),'size_z':(2,sz),'palette':listing(10,palette),
            'blocks':(11,packed),'tile_entities':listing(10,[tile]),'entities':listing(10,[]),
            'required_mods':listing(8,['minecolonies_cult','minecolonies','structurize']),
            'name':s('kingdoms_gate'+str(level)), 'architects':listing(8,['Kingdom\'s Gate contributors']),
            'mcversion':i(3955), 'optional_data':compound({'structurize':compound({'primary_offset':position(*anchor)})})}
    path=PACK/'mysticism'/f'kingdoms_gate{level}.blueprint'; nbt(path,result)
    return {'level':level,'dimensions':[sx,sy,sz],'anchor':anchor,'gatheringSlots':len(slots),'blocks':len(blocks),
            'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}

def main():
    textures()
    PACK.mkdir(parents=True,exist_ok=True)
    (PACK/'pack.json').write_text(json.dumps({'version':1,'pack-format':1,'name':"Kingdom's Gate",'desc':'A gothic congregation hall, growing from a timber chapel into a twin-spired sanctuary.',
        'authors':["Kingdom's Gate contributors"],'mods':['minecolonies','structurize','minecolonies_cult'],'icon':'kingdoms_gate.png'},indent=2)+'\n')
    proof=[blueprint(level) for level in range(1,6)]
    (ROOT/'assets').mkdir(exist_ok=True)
    (ROOT/'assets/provenance.json').write_text(json.dumps({'generator':'tools/generate_assets.py','art':'Original deterministic pixel art and block architecture; no copied textures or blueprints.','blueprints':proof},indent=2)+'\n')
    nbt(RES/'data/minecolonies_cult/structure/empty.nbt',{'DataVersion':i(3955),'size':listing(3,[5,5,5]),'palette':listing(10,[{'Name':s('minecraft:air')}]),'blocks':listing(10,[]),'entities':listing(10,[])})
    print(json.dumps(proof,indent=2))

if __name__=='__main__': main()
