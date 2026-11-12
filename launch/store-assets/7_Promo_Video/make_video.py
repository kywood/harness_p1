"""홍보 영상 편집 준비: 자막·배경·마무리 화면 PNG와 ffmpeg 명령(build.sh)을 만든다.

사용: python make_video.py <작업 폴더>
  작업 폴더에는 record.sh 결과(promo_a.mp4, promo_b.mp4)가 있어야 한다. 자를 구간은 cuts.txt 에 적는다.
  screenrecord 시작이 늦어져 events.txt 시각과 파일 시각이 몇 초씩 어긋나므로, 구간은 프레임을 보고 고른다.
  만든 뒤 작업 폴더에서 `bash build.sh` 를 실행한다(ffmpeg: pieceplay-ffmpeg 일회용 Docker 컨테이너).
필요: Pillow, Windows Noto Sans KR 가변 글꼴. 출력: PiecePlay_promo_ko-KR.mp4, PiecePlay_promo_en-US.mp4 (1920x1080, 30fps, 무음)
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = 1920, 1080
APP_W, APP_H, APP_X, APP_Y = 1536, 960, 192, 14  # 앱 화면(16:10) 자리
BG = (0x0A, 0x0F, 0x1E); C1 = (0x2F, 0x66, 0xF0); C2 = (0x2E, 0xE6, 0xB0)
ON = (0xE8, 0xEE, 0xFF); SUB = (0xA7, 0xB3, 0xD1)
NOTO = 'C:/Windows/Fonts/NotoSansKR-VF.ttf'
HERE = os.path.dirname(os.path.abspath(__file__))
LOGO = os.path.join(HERE, '..', '2_Android_Adaptive_Icon', 'ic_launcher_foreground.png')

CAPTIONS = {
    'ko-KR': ['링크를 붙여넣고 바로 재생', '앞부분부터 먼저 받아요', '받으면서 보고, 재생 중에는 광고 없이',
              '이동하면 그 위치부터 받아요', '보던 곳부터 이어보기'],
    'en-US': ['Paste a magnet link, play right away', 'The beginning downloads first',
              'Watch while it downloads, no ads during playback', 'Seek, and it downloads from there',
              'Pick up where you left off'],
}
END = {
    'ko-KR': ('PiecePlay - 토렌트 플레이어', '받으면서 바로 재생'),
    'en-US': ('PiecePlay - Torrent Player', 'Play while it downloads'),
}
CREDIT = 'Sintel · Cosmos Laundromat © Blender Foundation | blender.org (CC BY)'
END_SEC = 3.5


def font(size, weight):
    f = ImageFont.truetype(NOTO, size); f.set_variation_by_axes([weight]); return f


def fitted(d, text, size, weight, max_w):
    while True:
        f = font(size, weight)
        b = d.textbbox((0, 0), text, font=f)
        if b[2] - b[0] <= max_w or size <= 12:
            return f
        size -= 1


def centered(d, y, text, f, fill):
    b = d.textbbox((0, 0), text, font=f)
    d.text(((W - (b[2] - b[0])) // 2 - b[0], y - (b[3] - b[1]) // 2 - b[1]), text, font=f, fill=fill)


def background():
    img = Image.new('RGB', (W, H), BG)
    glow = Image.new('RGBA', (W, H), (0, 0, 0, 0)); g = ImageDraw.Draw(glow)
    g.ellipse((-W * 0.3, -H * 0.3, W * 0.6, H * 0.5), fill=C1 + (70,))
    g.ellipse((W * 0.5, H * 0.5, W * 1.3, H * 1.3), fill=C2 + (40,))
    glow = glow.filter(ImageFilter.GaussianBlur(220)); img.paste(glow, (0, 0), glow)
    return img


def caption_png(text, path):
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    centered(d, 1030, text, fitted(d, text, 46, 700, W - 200), ON + (255,))
    img.save(path)


def end_png(lang, path):
    img = background(); d = ImageDraw.Draw(img)
    fg = Image.open(LOGO).convert('RGBA'); fg = fg.crop(fg.getchannel('A').point(lambda a: 255 if a > 8 else 0).getbbox())
    s = 260 / fg.height; fg = fg.resize((int(fg.width * s), 260), Image.LANCZOS)
    img.paste(fg, ((W - fg.width) // 2, 230), fg)
    title, sub = END[lang]
    centered(d, 610, title, fitted(d, title, 84, 700, W - 200), ON)
    centered(d, 710, sub, font(44, 400), SUB)
    for i in range(160):  # 그라데이션 막대
        t = i / 159; d.line([(W // 2 - 80 + i, 772), (W // 2 - 80 + i, 778)], fill=tuple(int(C1[k] + (C2[k] - C1[k]) * t) for k in range(3)))
    centered(d, 1010, CREDIT, font(26, 400), SUB)
    img.save(path)


def main():
    work = sys.argv[1]
    segs = []  # (입력, 시작, 끝, 배속, 자막 번호) — 녹화 파일 시각
    for line in open(os.path.join(HERE, 'cuts.txt'), encoding='utf-8'):
        if line.strip() and not line.startswith('#'):
            src, st, en, sp, cap = line.split()
            segs.append((src, float(st), float(en), float(sp), int(cap)))
    background().save(os.path.join(work, 'bg.png'))
    filters, labels, t, windows = [], [], 0.0, []
    for i, (src, a, b, speed, cap) in enumerate(segs):
        idx = 0 if src == 'a' else 1
        dur = (b - a) / speed
        # screenrecord 는 화면이 바뀔 때만 프레임을 남겨(가변 프레임) 구간 길이가 늘어날 수 있다.
        # 30fps 로 고정한 뒤 모자라면 마지막 프레임을 늘리고(tpad) 정확한 길이로 자른다.
        filters.append(f'[{idx}:v]setpts=PTS-STARTPTS,trim=start={a:.3f}:end={b:.3f},setpts=(PTS-STARTPTS)/{speed},fps=30,'
                       f'tpad=stop_mode=clone:stop_duration={dur:.3f},trim=duration={dur:.3f},setpts=PTS-STARTPTS,'
                       f'scale={APP_W}:{APP_H}:flags=lanczos,setsar=1[s{i}]')
        labels.append(f'[s{i}]')
        windows.append((cap, t, t + dur)); t += dur
    app_dur = t
    filters.append(''.join(labels) + f'concat=n={len(segs)}:v=1:a=0[app]')
    # 같은 자막이 이어지는 구간은 합친다
    merged = []
    for cap, s, e in windows:
        if merged and merged[-1][0] == cap:
            merged[-1] = (cap, merged[-1][1], e)
        else:
            merged.append((cap, s, e))
    lines = ['#!/usr/bin/env bash', 'set -e', '# make_video.py 가 만든 파일. 작업 폴더에서 실행한다.',
             'FF="docker run --rm -v $PWD:/w pieceplay-ffmpeg -hide_banner -loglevel error -y"']
    for lang in CAPTIONS:
        for n, text in enumerate(CAPTIONS[lang]):
            caption_png(text, os.path.join(work, f'cap_{lang}_{n}.png'))
        end_png(lang, os.path.join(work, f'end_{lang}.png'))
        inputs = f'-i promo_a.mp4 -i promo_b.mp4 -loop 1 -t {app_dur:.3f} -i bg.png'
        f = list(filters)
        f.append(f'[2:v]fps=30,setsar=1[bg];[bg][app]overlay={APP_X}:{APP_Y}:shortest=1[v0]')
        last = 'v0'
        for k, (cap, s, e) in enumerate(merged):
            inputs += f' -i cap_{lang}_{cap}.png'
            n_in = 3 + k
            f.append(f'[{last}][{n_in}:v]overlay=0:0:enable=\'between(t,{s:.3f},{e:.3f})\'[v{k + 1}]')
            last = f'v{k + 1}'
        inputs += f' -loop 1 -t {END_SEC} -i end_{lang}.png'
        n_end = 3 + len(merged)
        f.append(f'[{n_end}:v]fps=30,setsar=1,fade=t=in:st=0:d=0.4[end]')
        f.append(f'[{last}][end]concat=n=2:v=1:a=0,format=yuv420p[out]')
        graph = ';'.join(f)
        lines.append(f'$FF {inputs} -filter_complex "{graph}" -map "[out]" -c:v libx264 -preset slow -crf 18 '
                     f'-r 30 -movflags +faststart PiecePlay_promo_{lang}.mp4')
    open(os.path.join(work, 'build.sh'), 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')
    print(f'app {app_dur:.1f}s + end {END_SEC}s = {app_dur + END_SEC:.1f}s; captions', [(c, round(s, 1), round(e, 1)) for c, s, e in merged])


if __name__ == '__main__':
    main()
