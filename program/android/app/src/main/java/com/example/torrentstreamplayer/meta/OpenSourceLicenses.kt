package com.example.torrentstreamplayer.meta

import androidx.annotation.RawRes
import com.example.torrentstreamplayer.R

/**
 * 앱에 들어 있는 라이선스 전문(`res/raw/license_*.txt`). 같은 라이선스는 파일 하나를 여러 구성요소가 같이 쓴다.
 * 파일 이름은 [fileName]과 같아야 한다(`OpenSourceLicensesTest`가 확인).
 */
internal enum class LicenseDocument(@RawRes val resource: Int, val fileName: String) {
    LGPL_2_1(R.raw.license_lgpl_2_1, "license_lgpl_2_1"),
    GPL_2_0(R.raw.license_gpl_2_0, "license_gpl_2_0"),
    MIT_LIBTORRENT4J(R.raw.license_mit_libtorrent4j, "license_mit_libtorrent4j"),
    BSD3_LIBTORRENT(R.raw.license_bsd3_libtorrent, "license_bsd3_libtorrent"),
    BSL_1_0(R.raw.license_bsl_1_0, "license_bsl_1_0"),
    APACHE_2_0(R.raw.license_apache_2_0, "license_apache_2_0"),
    MPL_2_0(R.raw.license_mpl_2_0, "license_mpl_2_0"),
    BSD3_USRSCTP(R.raw.license_bsd3_usrsctp, "license_bsd3_usrsctp"),
    MIT_PLOG(R.raw.license_mit_plog, "license_mit_plog"),
    BSD3_NANOHTTPD(R.raw.license_bsd3_nanohttpd, "license_bsd3_nanohttpd"),
}

/**
 * 오픈소스 라이선스 화면의 목록(`design/specs/open-source-licenses.md` 2절). 이름·버전·라이선스 이름·주소는
 * 고유명사라 번역하지 않는다. 의존성(`gradle/libs.versions.toml`, `app/build.gradle.kts`)을 바꾸면 이 목록도 같이 바꾼다.
 *
 * 포함 여부는 실제 산출물에서 확인했다: libvlc-all 3.7.5 AAR의 `libvlc.so`, libtorrent4j 2.1.0-39의 `libtorrent4j.so`.
 * 순서는 고지 의무가 큰 것(GPL·LGPL) → 앱의 핵심(토렌트·서버) → 플랫폼 라이브러리다.
 *
 * @property sourceOffer 라이선스가 소스 제공 안내를 요구한다(GPL·LGPL·MPL). 목록에 "소스 코드를 받을 수 있습니다."를 보인다.
 * @property lgplNotice 전문 보기에 LibVLC LGPL 안내 문구를 보인다.
 */
internal enum class OpenSourceComponent(
    val displayName: String,
    val version: String,
    val licenseName: String,
    val url: String,
    val copyright: String,
    val document: LicenseDocument,
    val sourceOffer: Boolean = false,
    val lgplNotice: Boolean = false,
) {
    LIBVLC("LibVLC", "3.7.5", "LGPL 2.1 이상", "https://code.videolan.org/videolan/libvlcjni",
        "Copyright © VideoLAN and VLC authors", LicenseDocument.LGPL_2_1, sourceOffer = true, lgplNotice = true),

    /** libvlc.so에 정적으로 들어 있는 GPL 구성요소. */
    LIBVLC_GPL_COMPONENTS("x264, libdvdnav, libdvdread, libpostproc", "LibVLC 3.7.5 포함", "GPL 2.0 이상",
        "https://code.videolan.org/videolan/vlc", "Copyright © x264 project, VideoLAN, FFmpeg developers",
        LicenseDocument.GPL_2_0, sourceOffer = true),

    /** libvlc.so에 들어 있는 FFmpeg(libavcodec 62.28.102, libavformat 62.12.102). */
    FFMPEG("FFmpeg", "libavcodec 62.28.102", "LGPL 2.1 이상", "https://ffmpeg.org",
        "Copyright © FFmpeg developers", LicenseDocument.LGPL_2_1, sourceOffer = true),

    LIBTORRENT4J("libtorrent4j", "2.1.0-39", "MIT", "https://github.com/aldenml/libtorrent4j",
        "Copyright © 2018-2025 Alden Torres, © 2016 FrostWire, LLC.", LicenseDocument.MIT_LIBTORRENT4J),
    LIBTORRENT("libtorrent", "2.1.0.0", "BSD 3-Clause", "https://github.com/arvidn/libtorrent",
        "Copyright © 2003-2020, Arvid Norberg", LicenseDocument.BSD3_LIBTORRENT),
    BOOST("Boost", "libtorrent4j 포함", "Boost Software License 1.0", "https://www.boost.org",
        "Copyright © Boost authors", LicenseDocument.BSL_1_0),
    OPENSSL("OpenSSL", "3.5.5", "Apache 2.0", "https://www.openssl.org",
        "Copyright © The OpenSSL Project Authors", LicenseDocument.APACHE_2_0),
    LIBDATACHANNEL("libdatachannel, libjuice", "libtorrent4j 포함", "MPL 2.0",
        "https://github.com/paullouisageneau/libdatachannel", "Copyright © Paul-Louis Ageneau",
        LicenseDocument.MPL_2_0, sourceOffer = true),
    USRSCTP("usrsctp", "libtorrent4j 포함", "BSD 3-Clause", "https://github.com/sctplab/usrsctp",
        "Copyright © 2015, Randall Stewart and Michael Tuexen", LicenseDocument.BSD3_USRSCTP),
    PLOG("plog", "libtorrent4j 포함", "MIT", "https://github.com/SergiusTheBest/plog",
        "Copyright © 2022 Sergey Podobry", LicenseDocument.MIT_PLOG),
    NANOHTTPD("NanoHTTPD", "2.3.1", "BSD 3-Clause", "https://github.com/NanoHttpd/nanohttpd",
        "Copyright © 2012-2013 Paul S. Hawke, 2001,2005-2013 Jarno Elonen, 2010 Konstantinos Togias",
        LicenseDocument.BSD3_NANOHTTPD),
    ANDROIDX("AndroidX, Jetpack Compose, Material 3", "Compose BOM 2026.02.01", "Apache 2.0",
        "https://developer.android.com/jetpack", "Copyright © The Android Open Source Project",
        LicenseDocument.APACHE_2_0),
    KOTLIN("Kotlin 표준 라이브러리, kotlinx.coroutines", "2.2.10 등", "Apache 2.0", "https://kotlinlang.org",
        "Copyright © JetBrains s.r.o. and Kotlin Programming Language contributors", LicenseDocument.APACHE_2_0),
    MATERIAL_ICONS("Material 아이콘", "—", "Apache 2.0", "https://fonts.google.com/icons",
        "Copyright © Google LLC", LicenseDocument.APACHE_2_0),
}
