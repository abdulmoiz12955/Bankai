#!/usr/bin/env python3
"""Generates every binary/model asset of the Absolute Cutter mod (original, procedural).
Run from anywhere:  python3 tools/generate_assets.py     (needs Pillow + numpy; ffmpeg optional for .ogg)
"""
import json, math, random, subprocess, wave, tempfile, os
from pathlib import Path
import numpy as np
from PIL import Image

MODID = "absolute_cutter"
ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources/assets" / MODID
random.seed(7); np.random.seed(7)
for d in ("textures/item", "textures/entity", "textures/particle", "models/item", "particles", "sounds", "lang"):
    (RES / d).mkdir(parents=True, exist_ok=True)

# ------------------------------------------------------------------ TEXTURE ATLAS (64x64, 4x4 cells of 16px)
CELLS = {"blade": (0, 0), "groove": (1, 0), "edge": (2, 0), "guard": (3, 0),
         "gold": (0, 1), "wrap": (1, 1), "grip": (2, 1), "metal": (3, 1),
         "core": (0, 2), "marks": (1, 2)}

def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def add(c, n):
    return tuple(max(0, min(255, v + n)) for v in c)

img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = img.load()

def paint(name, fn):
    cx, cy = CELLS[name]
    for y in range(16):
        for x in range(16):
            px[cx * 16 + x, cy * 16 + y] = fn(x, y) + (255,)

def n(a=4):
    return random.randint(-a, a)

def f_blade(x, y):
    c = lerp((9, 9, 15), (36, 36, 50), y / 15)
    if (x + y * 2) % 11 == 0: c = add(c, 38)
    if x in (0, 15): c = add(c, 14)
    return add(c, n(3))
def f_groove(x, y):
    v = 0.5 + 0.5 * math.sin(y * 0.95 + x * 0.55)
    c = lerp((140, 165, 205), (255, 255, 255), v)
    if x in (7, 8): c = lerp(c, (255, 255, 255), 0.7)
    return add(c, n(5))
def f_edge(x, y):
    c = (226, 233, 246)
    if x % 4 == 0: c = (255, 255, 255)
    if y % 5 == 0: c = (196, 206, 226)
    return add(c, n(3))
def f_guard(x, y):
    c = add((16, 16, 21), n(3))
    if y in (2, 13) or (x + y) % 9 == 0 and 4 < y < 11: c = (214, 176, 58)
    return c
def f_gold(x, y):
    c = lerp((244, 204, 84), (176, 130, 32), y / 15)
    if x in (0, 15) or y in (0, 15): c = lerp(c, (90, 60, 10), 0.5)
    return add(c, n(4))
def f_wrap(x, y):
    c = (98, 10, 22)
    if (x + y) % 6 < 2: c = (44, 4, 10)
    if (x + y) % 12 == 0: c = (160, 160, 172)
    return add(c, n(3))
def f_grip(x, y):
    c = add((22, 22, 27), n(3))
    if y % 4 == 0: c = (120, 14, 28)
    return c
def f_metal(x, y):
    c = lerp((44, 46, 58), (98, 101, 116), 1 - abs(y - 7.5) / 7.5)
    if y in (2, 13): c = add(c, 30)
    return add(c, n(3))
def f_core(x, y):
    d = math.hypot(x - 7.5, y - 7.5) / 10.6
    return lerp((255, 255, 255), (140, 180, 235), d)
GLYPH = {(3,3),(3,4),(3,5),(4,5),(5,5),(5,4),(5,3),(7,3),(7,4),(7,5),(7,6),(9,3),(10,4),(11,5),(9,7),(9,8),(9,9),
         (3,9),(4,9),(5,9),(5,10),(5,11),(3,11),(7,10),(8,10),(9,11),(11,9),(11,10),(11,11),(12,11)}
def f_marks(x, y):
    return (232, 192, 72) if (x, y) in GLYPH else add((10, 10, 14), n(2))

for name, fn in (("blade", f_blade), ("groove", f_groove), ("edge", f_edge), ("guard", f_guard), ("gold", f_gold),
                 ("wrap", f_wrap), ("grip", f_grip), ("metal", f_metal), ("core", f_core), ("marks", f_marks)):
    paint(name, fn)
img.save(RES / "textures/item/absolute_cutter.png")

# entity white texture + particle glows
Image.new("RGBA", (8, 8), (255, 255, 255, 255)).save(RES / "textures/entity/getsuga.png")
def glow(path, tint):
    g = Image.new("RGBA", (16, 16)); p = g.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5) / 7.5
            a = max(0.0, 1 - d) ** 1.6
            core = max(0.0, 1 - d * 2.2)
            c = lerp(tint, (255, 255, 255), core)
            p[x, y] = (*c, int(255 * min(1, a * 1.2)))
    g.save(path)
glow(RES / "textures/particle/silver_mote.png", (205, 222, 255))
glow(RES / "textures/particle/getsuga_ember.png", (255, 200, 70))
for pname in ("silver_mote", "getsuga_ember"):
    (RES / f"particles/{pname}.json").write_text(json.dumps({"textures": [f"{MODID}:{pname}"]}, indent=2))

# ------------------------------------------------------------------ 3D ITEM MODEL (vanilla JSON cuboid model)
OY = -1.3
els = []
def uvc(m):
    cx, cy = CELLS[m]; return [cx * 4, cy * 4, cx * 4 + 4, cy * 4 + 4]
def r3(v): return round(v, 3)
def box(x1, y1, x2, y2, z1, z2, mat, rot=None):
    e = {"from": [r3(x1), r3(y1 + OY), r3(z1)], "to": [r3(x2), r3(y2 + OY), r3(z2)],
         "faces": {f: {"uv": uvc(mat), "texture": "#0"} for f in ("north", "east", "south", "west", "up", "down")}}
    if rot:
        o, axis, ang = rot
        e["rotation"] = {"origin": [o[0], r3(o[1] + OY), o[2]], "axis": axis, "angle": ang}
    els.append(e)

# blade: curved by stepping each segment sideways (vanilla models only allow 22.5 deg rotations)
SEG = 9
def curve(y): return 0.0055 * (y - 6.4) ** 2
for k in range(SEG):
    y0 = 6.4 + k * 2.3; y1 = y0 + 2.4
    w = 1.5 * (1 - 0.5 * (k / (SEG - 1)) ** 2); cx = 8 + curve(y0)
    box(cx - w, y0, cx - 0.3, y1, 7.45, 8.55, "blade")          # dark spine
    box(cx - 0.3, y0, cx + 0.3, y1, 7.6, 8.4, "groove")         # recessed silver-energy groove
    box(cx + 0.3, y0, cx + w - 0.35, y1, 7.5, 8.5, "blade")     # dark body
    box(cx + w - 0.35, y0, cx + w, y1, 7.7, 8.3, "edge")        # luminous cutting edge
for (a, b, w) in ((27.2, 28.2, 0.75), (28.2, 29.0, 0.5), (29.0, 29.5, 0.25)):   # kissaki tip
    cx = 8 + curve(a); box(cx - w, a, cx + w, b, 7.65, 8.35, "edge")
for zz in ((8.56, 8.62), (7.38, 7.44)):                           # black & gold markings by the guard
    box(6.6, 6.9, 7.2, 7.5, zz[0], zz[1], "marks"); box(6.6, 7.9, 7.2, 8.4, zz[0], zz[1], "marks")
    box(8.8, 7.0, 9.3, 7.4, zz[0], zz[1], "gold"); box(8.8, 7.9, 9.2, 8.1, zz[0], zz[1], "gold")

# angular broken-crescent crossguard
box(5.8, 5.0, 10.2, 6.5, 6.9, 9.1, "guard"); box(5.9, 5.85, 10.1, 6.05, 6.85, 9.15, "gold")
box(7.2, 5.3, 8.8, 6.2, 9.1, 9.16, "marks"); box(7.2, 5.3, 8.8, 6.2, 6.84, 6.9, "marks")
box(10.2, 5.2, 11.4, 6.6, 7.1, 8.9, "guard"); box(11.2, 5.9, 12.4, 7.4, 7.3, 8.7, "guard")
box(12.0, 6.9, 13.0, 8.5, 7.5, 8.5, "guard"); box(12.6, 8.1, 13.4, 9.7, 7.65, 8.35, "guard")
box(12.9, 9.5, 13.3, 10.2, 7.8, 8.2, "gold"); box(10.2, 6.5, 11.4, 6.66, 7.2, 8.8, "gold")
box(4.2, 4.6, 5.8, 6.2, 7.1, 8.9, "guard"); box(3.0, 3.9, 4.4, 5.4, 7.3, 8.7, "guard")
box(2.9, 5.3, 4.5, 5.45, 7.4, 8.6, "gold")
box(1.4, 3.0, 2.4, 4.6, 7.6, 8.4, "guard", ((1.9, 3.8, 8), "z", 22.5))      # detached shard (the "break")
box(1.6, 4.4, 2.2, 4.7, 7.8, 8.2, "gold", ((1.9, 3.8, 8), "z", 22.5))

# black grip with crimson + metallic wrap (octagonal via 45deg duplicate)
box(7.2, 0.9, 8.8, 5.0, 7.2, 8.8, "grip"); box(7.2, 0.9, 8.8, 5.0, 7.2, 8.8, "grip", ((8, 3, 8), "y", 45))
for i in range(4):
    y = 1.2 + i * 0.95
    box(7.1, y, 8.9, y + 0.5, 7.1, 8.9, "wrap"); box(7.05, y, 8.95, y + 0.5, 7.05, 8.95, "wrap", ((8, y, 8), "y", 45))

# pommel with pulsing silver core
box(6.5, -0.5, 9.5, 0.9, 6.5, 9.5, "metal"); box(6.5, -0.5, 9.5, 0.9, 6.5, 9.5, "metal", ((8, 0, 8), "y", 45))
box(6.9, -1.3, 9.1, -0.5, 6.9, 9.1, "guard"); box(7.3, -1.8, 8.7, -1.3, 7.3, 8.7, "core")
for (a, b, c, d) in ((9.5, 9.7, 7.4, 8.6), (6.3, 6.5, 7.4, 8.6)): box(a, -0.3, b, 0.7, c, d, "core")
for (c, d) in ((9.5, 9.7), (6.3, 6.5)): box(7.4, -0.3, 8.6, 0.7, c, d, "core")

def disp(rot, tr, sc): return {"rotation": rot, "translation": tr, "scale": [sc, sc, sc]}
model = {
    "credit": "Original procedural model - Absolute Cutter",
    "texture_size": [64, 64],
    "textures": {"0": f"{MODID}:item/absolute_cutter", "particle": f"{MODID}:item/absolute_cutter"},
    "elements": els,
    "display": {   # vanilla handheld values with the z-rotation reduced by 45deg because the blade is modelled upright
        "thirdperson_righthand": disp([0, -90, 10], [0, 4, 0.5], 0.72),
        "thirdperson_lefthand": disp([0, 90, -10], [0, 4, 0.5], 0.72),
        "firstperson_righthand": disp([0, -90, -20], [1.13, 3.2, 1.13], 0.58),
        "firstperson_lefthand": disp([0, 90, 20], [1.13, 3.2, 1.13], 0.58),
        "ground": disp([0, 0, 0], [0, 3, 0], 0.45),
        "gui": disp([0, 0, -45], [-1.7, -1.7, 0], 0.52),
        "fixed": disp([0, 180, -45], [-1.7, -1.7, 0], 0.52),
    },
    "gui_light": "front",
}
(RES / "models/item/absolute_cutter.json").write_text(json.dumps(model, indent=1))
print("model elements:", len(els))

# ------------------------------------------------------------------ SOUNDS (synthesised, original; .ogg via ffmpeg)
SR = 44100
def T(d): return np.linspace(0, d, int(SR * d), endpoint=False)
def band(x, lo, hi):
    X = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1 / SR)
    m = np.convolve(((f >= lo) & (f <= hi)).astype(float), np.ones(41) / 41, "same")
    return np.fft.irfft(X * m, len(x))
def noise(t): return np.random.randn(len(t))
def fade(x, fi=0.01, fo=0.06):
    a = int(SR * fi); b = int(SR * fo)
    x[:a] *= np.linspace(0, 1, a); x[-b:] *= np.linspace(1, 0, b); return x
def norm(x, p=0.85): return x / (np.max(np.abs(x)) + 1e-9) * p
S = {}
t = T(2.0)
S["sword_hum"] = fade((0.5 * np.sin(2 * np.pi * 110 * t) + 0.3 * np.sin(2 * np.pi * 165.4 * t) + 0.22 * np.sin(2 * np.pi * 220.7 * t)
        + 0.08 * np.sin(2 * np.pi * 440 * t * (1 + 0.004 * np.sin(2 * np.pi * 3 * t)))) * (0.8 + 0.2 * np.sin(2 * np.pi * 1.5 * t)), 0.25, 0.4)
t = T(0.5); e = np.exp(-t * 9) * (1 - np.exp(-t * 200))
S["slash"] = fade((band(noise(t), 1500, 9000) * 0.9 + 0.35 * np.sin(2 * np.pi * (2400 * t - 1800 * t * t))) * e, 0.002, 0.05)
t = T(1.0); ph = 2 * np.pi * (90 * t + 40 * t * t)
S["charge"] = fade((np.sin(ph) + 0.5 * np.sin(2 * ph) + 0.3 * np.sin(3 * ph) + 0.35 * band(noise(t), 300, 1400)) * np.sin(np.pi * t) ** 0.6, 0.02, 0.1)
t = T(1.4)
chime = sum(a * np.sin(2 * np.pi * f * t) for f, a in ((880, 1), (1318.5, .7), (1760, .5), (2637, .3))) * np.exp(-t * 3.2)
S["charge_complete"] = fade(0.6 * chime + np.sin(2 * np.pi * 70 * t) * np.exp(-t * 8) + 0.4 * band(noise(t), 3000, 9000) * np.exp(-t * 14), 0.002, 0.1)
t = T(1.8); sw = (t / 0.5) * np.exp(1 - t / 0.5)
S["getsuga_launch"] = fade(np.sin(2 * np.pi * (80 * t - 25 * t * t)) * np.exp(-t * 2.2) + 0.6 * band(noise(t), 200, 3000) * sw
        + 0.2 * np.sin(2 * np.pi * 1800 * t) * np.exp(-t * 5), 0.003, 0.15)
t = T(1.0)
S["getsuga_travel"] = fade(0.8 * band(noise(t), 40, 300) + 0.4 * band(noise(t), 400, 2500) * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * t)), 0.1, 0.1)
t = T(1.8)
S["getsuga_impact"] = fade(band(noise(t), 2000, 12000) * np.exp(-t * 25) + band(noise(t), 60, 900) * np.exp(-t * 4) * 0.9
        + np.sin(2 * np.pi * 55 * t) * np.exp(-t * 3.5), 0.001, 0.2)

def encode(name, data):
    pcm = (norm(data) * 32767).astype(np.int16)
    with tempfile.TemporaryDirectory() as td:
        wav = os.path.join(td, name + ".wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
        out = RES / "sounds" / (name + ".ogg")
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", str(out)], check=True)

VANILLA = {"sword_hum": "block.beacon.ambient", "slash": "entity.player.attack.sweep", "charge": "block.respawn_anchor.charge",
           "charge_complete": "block.beacon.activate", "getsuga_launch": "entity.warden.sonic_boom",
           "getsuga_travel": "entity.phantom.swoop", "getsuga_impact": "entity.lightning_bolt.impact"}
ok = True
try:
    for k, v in S.items(): encode(k, v)
except Exception as ex:
    ok = False; print("ffmpeg/vorbis unavailable -> vanilla sound fallback:", ex)
sj = {}
for k in S:
    entry = {"name": f"{MODID}:{k}"} if ok else {"name": "minecraft:" + VANILLA[k], "type": "event"}
    sj[k] = {"subtitle": f"subtitles.{MODID}.{k}", "sounds": [entry]}
(RES / "sounds.json").write_text(json.dumps(sj, indent=2))
print("sounds:", "custom .ogg" if ok else "vanilla fallback")
print("done")
