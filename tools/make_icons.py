from PIL import Image, ImageChops, ImageDraw
import os
src = Image.open("assets-src/icon-v2.jpg").convert("RGB")
# trim white border
bg = Image.new("RGB", src.size, (255,255,255))
diff = ImageChops.difference(src, bg).convert("L").point(lambda v: 255 if v > 40 else 0)
box = diff.getbbox(); tile = src.crop(box)
for xy in [(0,0),(tile.width-1,0),(0,tile.height-1),(tile.width-1,tile.height-1)]:
    ImageDraw.floodfill(tile, xy, (11,42,107), thresh=60)
s = max(tile.size); sq = Image.new("RGB", (s,s), (11,42,107)); sq.paste(tile, ((s-tile.width)//2, (s-tile.height)//2)); tile = sq
# rounded-tile alpha mask so leftover white corners disappear
def rounded(img, r):
    m = Image.new("L", img.size, 0); ImageDraw.Draw(m).rounded_rectangle([0,0,img.width-1,img.height-1], r, fill=255)
    o = img.convert("RGBA"); o.putalpha(m); return o
tile_r = rounded(tile, int(s*0.2))
res = "app/src/main/res"
dens = {"mdpi":1,"hdpi":1.5,"xhdpi":2,"xxhdpi":3,"xxxhdpi":4}
for d,f in dens.items():
    os.makedirs(f"{res}/mipmap-{d}", exist_ok=True)
    n = int(48*f); full = tile_r.resize((n,n), Image.LANCZOS)
    full.save(f"{res}/mipmap-{d}/ic_launcher.png")
    c = Image.new("L",(n,n),0); ImageDraw.Draw(c).ellipse([0,0,n-1,n-1],fill=255)
    rr = tile.resize((n,n), Image.LANCZOS).convert("RGBA"); rr.putalpha(c); rr.save(f"{res}/mipmap-{d}/ic_launcher_round.png")
    # adaptive foreground 108dp, artwork in central 72dp safe zone (~66%)
    fg_n = int(108*f); art = int(fg_n*0.70)
    fg = Image.new("RGBA",(fg_n,fg_n),(0,0,0,0)); fg.paste(tile.resize((art,art),Image.LANCZOS),((fg_n-art)//2,)*2)
    fg.save(f"{res}/mipmap-{d}/ic_launcher_foreground.png")
tile.resize((512,512), Image.LANCZOS).save("output/SafeSpot-play-icon-512.png")
print("ok", box)
