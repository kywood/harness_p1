"""Play 그래픽 이미지(1024x500, 알파 없음) 생성.

사용: python feature.py <적응형 아이콘 전경 png> <출력 png> [ko-KR|en-US]
"""
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

TEXT = {
    'ko-KR': ('받으면서 바로 재생', '다 받기 전에 재생하는 토렌트 플레이어'),
    'en-US': ('Play while it downloads', 'Paste a magnet link and play right away'),
}
fg_path, out = sys.argv[1], sys.argv[2]
lang = sys.argv[3] if len(sys.argv) > 3 else 'ko-KR'
line1, line2 = TEXT[lang]
NOTO = 'C:/Windows/Fonts/NotoSansKR-VF.ttf'


def font(size, weight):
    f = ImageFont.truetype(NOTO, size)
    f.set_variation_by_axes([weight])
    return f


def fitted(d, text, size, weight, max_w):
    while True:
        f = font(size, weight)
        b = d.textbbox((0, 0), text, font=f)
        if b[2] - b[0] <= max_w or size <= 12:
            return f
        size -= 1


W, H = 1024, 500
BG = (0x0A, 0x0F, 0x1E); C1 = (0x2F, 0x66, 0xF0); C2 = (0x2E, 0xE6, 0xB0)
img = Image.new('RGB', (W, H), BG)
# soft brand glow behind logo
glow = Image.new('RGBA', (W, H), (0, 0, 0, 0)); g = ImageDraw.Draw(glow)
g.ellipse((60, 40, 520, 460), fill=C1 + (70,)); g.ellipse((200, 110, 520, 390), fill=C2 + (40,))
glow = glow.filter(ImageFilter.GaussianBlur(90)); img.paste(glow, (0, 0), glow)
# logo (adaptive foreground, transparent)
fg = Image.open(fg_path).convert('RGBA')
bbox = fg.getchannel('A').point(lambda a: 255 if a > 8 else 0).getbbox(); fg = fg.crop(bbox)
s = 300 / fg.height; fg = fg.resize((int(fg.width * s), 300), Image.LANCZOS)
img.paste(fg, (90, (H - 300) // 2), fg)
d = ImageDraw.Draw(img)
x = 520
max_w = W - x - 40
fb = font(88, 700)
fr = fitted(d, line1, 38, 400, max_w)
fs = fitted(d, line2, 26, 400, max_w)
# wordmark with gradient fill
title = 'PiecePlay'; tb = d.textbbox((0, 0), title, font=fb); tw, th = tb[2] - tb[0], tb[3] - tb[1]
mask = Image.new('L', (tw, th + 30), 0); ImageDraw.Draw(mask).text((-tb[0], -tb[1]), title, font=fb, fill=255)
grad = Image.new('RGB', (tw, th + 30))
for i in range(tw):
    t = i / max(1, tw - 1); col = tuple(int(C1[k] + (C2[k] - C1[k]) * t) for k in range(3))
    ImageDraw.Draw(grad).line([(i, 0), (i, th + 30)], fill=col)
ty = 150; img.paste(grad, (x, ty), mask)
d.text((x, ty + th + 34), line1, font=fr, fill=(0xE8, 0xEE, 0xFF))
d.text((x, ty + th + 96), line2, font=fs, fill=(0xA7, 0xB3, 0xD1))
img.save(out)
