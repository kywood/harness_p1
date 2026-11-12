"""LibVLC 산출물에 GPL 구성요소 흔적이 남았는지 검사한다(program/libvlc-lgpl-plan.md 5절).

사용:
    python3 -I libvlc_gpl_scan.py <AAR|.so|플러그인 디렉터리> [...]

- AAR·APK·AAB: 안의 libvlc.so를 ABI별로 검사한다.
- .so: 그 파일을 검사한다.
- 디렉터리: 빌드 중 생성된 lib*_plugin.a 를 검사한다(모듈별 라이선스 문자열이 링크 전에는 남아 있다).

문자열 표식(MARKERS)은 휴리스틱이다. 하나라도 걸리면 종료 코드 1로 끝나며, 걸린 항목을 사람이 확인한다.
64비트 .so는 ELF LOAD 세그먼트 정렬이 16 KB 이상인지도 본다.
"""
import os
import struct
import sys
import zipfile

# VLC 모듈이 VLC_MODULE_LICENSE VLC_LICENSE_GPL_2_PLUS 로 선언하면 들어가는 문자열(include/vlc_plugin.h).
GPL_MODULE_LICENSE = b"Licensed under the terms of the GNU General Public License, version 2 or later."

# 3.7.5 libvlc-all 에서 실제로 보인 GPL 구성요소의 흔적. 값은 그 구성요소 고유의 심볼·문자열이다.
MARKERS = {
    # "x264 - core" 는 표식으로 쓰지 않는다. LGPL FFmpeg의 H.264 SEI 해석기(h2645_sei.c)가
    # x264로 인코딩된 스트림을 알아보려고 같은 문자열을 갖고 있어 GPL이 없어도 걸린다.
    "x264": [b"x264_encoder_open", b"H.264/MPEG-4 AVC codec - Copy"],
    "x265": [b"x265_encoder_open"],
    "libpostproc": [b"pp_postprocess", b"postproc_version"],
    "libdvdnav": [b"dvdnav_open"],
    "libdvdread": [b"DVDOpen"],
    "realrtsp": [b"access_realrtsp", b"Real RTSP"],
    "lua": [b"luaL_newstate"],
    "gpl-module-license": [GPL_MODULE_LICENSE],
}

PAGE_16K = 16384


def scan_bytes(label, data):
    hits = []
    for name, needles in MARKERS.items():
        count = sum(data.count(n) for n in needles)
        if count:
            hits.append(f"{name}={count}")
    align = elf_min_load_align(data)
    status = "FAIL" if hits else "ok"
    print(f"[{status}] {label} size={len(data)} gpl={','.join(hits) or '-'} load_align={align}")
    bad_align = align is not None and align < PAGE_16K and is_elf64(data)
    if bad_align:
        print(f"[FAIL] {label} 64-bit LOAD 정렬 {align} < {PAGE_16K}")
    return bool(hits) or bad_align


def is_elf64(data):
    return data[:4] == b"\x7fELF" and data[4] == 2


def elf_min_load_align(data):
    """ELF LOAD 세그먼트의 최소 p_align. ELF가 아니면 None."""
    if data[:4] != b"\x7fELF":
        return None
    is64 = data[4] == 2
    endian = "<" if data[5] == 1 else ">"
    if is64:
        phoff, = struct.unpack_from(endian + "Q", data, 0x20)
        phentsize, phnum = struct.unpack_from(endian + "HH", data, 0x36)
    else:
        phoff, = struct.unpack_from(endian + "I", data, 0x1C)
        phentsize, phnum = struct.unpack_from(endian + "HH", data, 0x2A)
    aligns = []
    for i in range(phnum):
        off = phoff + i * phentsize
        p_type, = struct.unpack_from(endian + "I", data, off)
        if p_type != 1:
            continue
        p_align, = struct.unpack_from(endian + "Q", data, off + 0x30) if is64 else \
            struct.unpack_from(endian + "I", data, off + 0x1C)
        aligns.append(p_align)
    return min(aligns) if aligns else None


def scan_path(path):
    failed = False
    if os.path.isdir(path):
        for root, _, files in os.walk(path):
            for f in sorted(files):
                if f.endswith("_plugin.a") or f.endswith(".so"):
                    full = os.path.join(root, f)
                    with open(full, "rb") as fh:
                        failed |= scan_bytes(full, fh.read())
    elif zipfile.is_zipfile(path):
        with zipfile.ZipFile(path) as z:
            for name in z.namelist():
                if name.endswith("libvlc.so"):
                    failed |= scan_bytes(f"{os.path.basename(path)}!{name}", z.read(name))
    else:
        with open(path, "rb") as fh:
            failed |= scan_bytes(path, fh.read())
    return failed


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    failed = False
    for p in argv[1:]:
        failed |= scan_path(p)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
