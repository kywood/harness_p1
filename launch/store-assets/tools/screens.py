"""Play 스토어 스크린샷 생성 (휴대전화·7인치·10인치 태블릿).

사용: python screens.py <store-assets 폴더> <ko-KR|en-US|all> [phone|tab7|tab10 ...]
필요: Pillow, Windows Noto Sans KR 가변 글꼴(NotoSansKR-VF.ttf)

원본 캡처(raw/)를 브랜드 틀(배경 #0A0F1E, 그라데이션 #2F66F0 -> #2EE6B0)에 넣고
위에 제목·보조 문구를 얹는다. 캔버스는 9:16 또는 16:9(Play 대화면 규칙)이다.
문구 기준: marketing/aso/ko-KR.md 6절.
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

BG = (0x0A, 0x0F, 0x1E)
C1 = (0x2F, 0x66, 0xF0)
C2 = (0x2E, 0xE6, 0xB0)
ON = (0xE8, 0xEE, 0xFF)
SUB = (0xA7, 0xB3, 0xD1)
OUTL = (0x2A, 0x37, 0x57)
NOTO = 'C:/Windows/Fonts/NotoSansKR-VF.ttf'

# (제목, 보조) — 언어별, 화면 키별 캡션
CAPTIONS = {'ko-KR': {
    'main': ('링크를 넣으면 바로 재생', '마그넷 링크나 토렌트 파일 주소만 붙여넣으세요'),
    'preparing': ('앞부분부터 먼저 받아요', '다 받을 때까지 기다리지 않아도 돼요'),
    'player_noads': ('재생 중에는 광고 없이', '받은 조각과 재생 위치를 한눈에'),
    'player_seek': ('이동하면 그 위치부터 받아요', '10초 이동 · 확대 · 축소'),
    'playlist': ('보던 곳부터 이어보기', '최근 50개를 기기 안에 암호화해 보관'),
    'stored': ('업로드 없이, 기록은 기기 안에', '받은 파일은 형식별로 보고 바로 삭제'),
}, 'en-US': {
    'main': ('Paste a magnet link, play right away', 'Works with magnet links and .torrent file URLs'),
    'preparing': ('The beginning downloads first', 'No need to wait for the whole file'),
    'player_noads': ('No ads during playback', 'See downloaded pieces and your position at a glance'),
    'player_seek': ('Seek, and it downloads from there', '10-second skip · zoom in · zoom out'),
    'playlist': ('Pick up where you left off', 'Your last 50 links, encrypted on your device'),
    'stored': ('No uploads. History stays on device', 'Browse saved files by type and delete them'),
    'open_link': ('Open links from other apps', 'Play now or save for later'),
    'subtitles': ('Choose your subtitles', 'Pick subtitle files that come with the video'),
}}

# 언어별 원본 폴더 이름(대상 폴더 아래). 없으면 raw. en-US 는 영어 UI 로 다시 찍은 원본을 쓴다(#144).
RAW_DIRS = {'en-US': 'raw-en-US'}

# 언어별 추가 장면(기본 6장 뒤에 붙는다): 대상 -> [(출력 파일, 원본 파일, 캡션 키, 캔버스 W, H)]
EXTRA = {'en-US': {
    'phone': [('07_open_link.png', 'open-link.png', 'open_link', 1080, 1920),
              ('08_subtitles.png', 'subtitles.png', 'subtitles', 1080, 1920)],
    'tab7': [('07_open_link.png', 'open-link.png', 'open_link', 1080, 1920),
             ('08_subtitles.png', 'subtitles.png', 'subtitles', 1080, 1920)],
    'tab10': [('07_open_link.png', 'open-link.png', 'open_link', 2560, 1440),
              ('08_subtitles.png', 'subtitles.png', 'subtitles', 2560, 1440)],
}}

# 대상별: 출력 폴더, 원본 폴더, [(출력 파일, 원본 파일, 캡션 키, 캔버스 W, H)]
TARGETS = {
    'phone': ('4_Play_Store_Screenshots/{lang}', '4_Play_Store_Screenshots/raw', [
        ('01_main.png', 'main.png', 'main', 1080, 1920),
        ('02_preparing.png', 'preparing.png', 'preparing', 1080, 1920),
        ('03_player_landscape.png', 'player-landscape.png', 'player_noads', 1920, 1080),
        ('04_player.png', 'player.png', 'player_seek', 1080, 1920),
        ('05_playlist.png', 'playlist.png', 'playlist', 1080, 1920),
        ('06_stored_files.png', 'stored-files.png', 'stored', 1080, 1920),
    ]),
    'tab7': ('5_Tablet_7in/{lang}', '5_Tablet_7in/raw', [
        ('01_main.png', 'main.png', 'main', 1080, 1920),
        ('02_preparing.png', 'preparing.png', 'preparing', 1080, 1920),
        ('03_player_landscape.png', 'player-landscape.png', 'player_noads', 1920, 1080),
        ('04_player.png', 'player.png', 'player_seek', 1080, 1920),
        ('05_playlist.png', 'playlist.png', 'playlist', 1080, 1920),
        ('06_stored_files.png', 'stored-files.png', 'stored', 1080, 1920),
    ]),
    'tab10': ('6_Tablet_10in/{lang}', '6_Tablet_10in/raw', [
        ('01_main.png', 'main.png', 'main', 2560, 1440),
        ('02_preparing.png', 'preparing.png', 'preparing', 2560, 1440),
        ('03_player.png', 'player.png', 'player_noads', 2560, 1440),
        ('04_player_seek.png', 'player-seek.png', 'player_seek', 2560, 1440),
        ('05_playlist.png', 'playlist.png', 'playlist', 2560, 1440),
        ('06_stored_files.png', 'stored-files.png', 'stored', 2560, 1440),
    ]),
}


def font(size, weight):
    f = ImageFont.truetype(NOTO, size)
    f.set_variation_by_axes([weight])
    return f


def background(W, H):
    img = Image.new('RGB', (W, H), BG)
    glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    g = ImageDraw.Draw(glow)
    g.ellipse((-W * 0.3, -H * 0.15, W * 0.7, H * 0.35), fill=C1 + (80,))
    g.ellipse((W * 0.5, -H * 0.1, W * 1.3, H * 0.3), fill=C2 + (45,))
    glow = glow.filter(ImageFilter.GaussianBlur(min(W, H) // 6))
    img.paste(glow, (0, 0), glow)
    return img


def gradient_bar(d, x, y, w, h):
    for i in range(w):
        t = i / (w - 1)
        d.line([(x + i, y), (x + i, y + h)], fill=tuple(int(C1[k] + (C2[k] - C1[k]) * t) for k in range(3)))


def fitted_font(d, text, size, weight, max_w):
    while True:
        f = font(size, weight)
        b = d.textbbox((0, 0), text, font=f)
        if b[2] - b[0] <= max_w or size <= 12:
            return f
        size -= 2


def centered(d, W, y, text, size, weight, fill, max_w):
    f = fitted_font(d, text, size, weight, max_w)
    b = d.textbbox((0, 0), text, font=f)
    d.text(((W - (b[2] - b[0])) // 2 - b[0], y), text, font=f, fill=fill)


def paste_shot(img, src, box, radius):
    x, y, w, h = box
    s = Image.open(src).convert('RGB').resize((w, h), Image.LANCZOS)
    mask = Image.new('L', (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, h - 1), radius, fill=255)
    shadow = Image.new('RGBA', img.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((x - 6, y + 10, x + w + 6, y + h + 22), radius + 6, fill=(0, 0, 0, 150))
    shadow = shadow.filter(ImageFilter.GaussianBlur(18))
    img.paste(shadow, (0, 0), shadow)
    img.paste(s, (x, y), mask)
    ImageDraw.Draw(img).rounded_rectangle((x - 2, y - 2, x + w + 1, y + h + 1), radius + 2, outline=OUTL, width=3)


def compose(src, out, W, H, title, sub):
    k = min(W, H) / 1080
    portrait = H > W
    img = background(W, H)
    d = ImageDraw.Draw(img)
    max_w = W - int(120 * k)
    if portrait:
        ty, ts, sy, ss, by, top, side, bottom = 92, 68, 192, 36, 262, 320, 48, 40
    else:
        ty, ts, sy, ss, by, top, side, bottom = 56, 60, 146, 32, 206, 268, 120, 60
    centered(d, W, int(ty * k), title, int(ts * k), 700, ON, max_w)
    centered(d, W, int(sy * k), sub, int(ss * k), 400, SUB, max_w)
    gradient_bar(d, W // 2 - int(60 * k), int(by * k), int(120 * k), max(4, int(6 * k)))
    sw, sh = Image.open(src).size
    box_w, box_h = W - 2 * int(side * k), H - int(top * k) - int(bottom * k)
    scale = min(box_w / sw, box_h / sh)
    w, h = round(sw * scale), round(sh * scale)
    paste_shot(img, src, ((W - w) // 2, int(top * k), w, h), int((44 if portrait else 36) * k))
    img.save(out)


def main():
    root = sys.argv[1]
    langs = list(CAPTIONS) if sys.argv[2] == 'all' else [sys.argv[2]]
    names = sys.argv[3:] or list(TARGETS)
    for lang, name in [(l, n) for l in langs for n in names]:
        out_tpl, raw_dir, items = TARGETS[name]
        out_dir = out_tpl.format(lang=lang)
        os.makedirs(os.path.join(root, out_dir), exist_ok=True)
        raw_dir = os.path.join(os.path.dirname(raw_dir), RAW_DIRS.get(lang, 'raw'))
        for out, raw, key, W, H in items + EXTRA.get(lang, {}).get(name, []):
            src = os.path.join(root, raw_dir, raw)
            if not os.path.exists(src):
                print('skip (no raw):', src)
                continue
            title, sub = CAPTIONS[lang][key]
            compose(src, os.path.join(root, out_dir, out), W, H, title, sub)
            print('wrote', out_dir + '/' + out, W, 'x', H)


if __name__ == '__main__':
    main()
