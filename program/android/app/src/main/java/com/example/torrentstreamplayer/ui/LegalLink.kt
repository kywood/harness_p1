package com.example.torrentstreamplayer.ui

import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R

/**
 * 메인 하단 법적 링크(`design/specs/main-legal-footer.md`). 선언 순서가 화면 순서다(시안: 고정, 최대 4개).
 *
 * @param label 링크 글자.
 * @param url 외부 브라우저로 여는 주소 리소스. `null`이면 앱 안 동작이다.
 * @param needsConsentRegion 광고 동의가 필요한 지역에서만 보인다.
 */
internal enum class LegalLink(
    @StringRes val label: Int,
    @StringRes val url: Int? = null,
    val needsConsentRegion: Boolean = false,
) {
    PRIVACY_POLICY(R.string.action_privacy_policy, url = R.string.url_privacy_policy),
    OPEN_SOURCE_LICENSES(R.string.action_open_source_licenses),
    AD_PRIVACY(R.string.action_ad_privacy, needsConsentRegion = true);

    /** 외부 브라우저로 나가는 링크. 글자 뒤에 `ic_open_in_new`와 "브라우저에서 열림"을 붙인다. */
    val external: Boolean get() = url != null
}

/** 메인 하단 묶음에 무엇을 보일지 정한다. */
internal object LegalFooterPolicy {
    /** 보일 링크를 순서대로. 광고 개인정보 설정은 UMP 옵션 창이 있을 때(동의 필요 지역)만. */
    fun links(consentOptionsAvailable: Boolean): List<LegalLink> =
        LegalLink.entries.filter { consentOptionsAvailable || !it.needsConsentRegion }

    /** 버전명을 읽지 못하면 "버전 " 글자만 남지 않게 줄을 숨긴다. */
    fun versionLabel(versionName: String?): String? = versionName?.trim()?.takeIf { it.isNotEmpty() }
}
