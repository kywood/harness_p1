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
    USAGE_GUIDE(R.string.action_usage_guide),
    PRIVACY_POLICY(R.string.action_privacy_policy, url = R.string.url_privacy_policy),
    OPEN_SOURCE_LICENSES(R.string.action_open_source_licenses),
    AD_PRIVACY(R.string.action_ad_privacy, needsConsentRegion = true);

    /** 외부 브라우저로 나가는 링크. 글자 뒤에 `ic_open_in_new`와 "브라우저에서 열림"을 붙인다. */
    val external: Boolean get() = url != null
}

/**
 * 법적 링크가 부르는 동작. 메인 하단 묶음과 설정의 "정보" 묶음이 같은 연결을 쓴다(링크 ↔ 동작 연결은 [actionFor] 한 곳).
 *
 * @param openExternal 외부 브라우저로 주소를 연다(실패 안내는 부르는 화면이 맡는다).
 * @param privacyOptions 광고 개인정보 설정. 동의가 필요한 지역에서만 있다.
 */
internal class LegalLinkActions(
    val openUsageGuide: () -> Unit,
    val openExternal: (String) -> Unit,
    val openLicenses: () -> Unit,
    val privacyOptions: (() -> Unit)?,
) {
    /** 보일 링크를 순서대로([LegalFooterPolicy.links]). */
    val links: List<LegalLink> get() = LegalFooterPolicy.links(consentOptionsAvailable = privacyOptions != null)

    /** [link]를 눌렀을 때의 동작. [url]은 [LegalLink.url]을 읽은 주소다. 동작이 없으면(동의 불필요 지역) null. */
    fun actionFor(link: LegalLink, url: String?): (() -> Unit)? = when (link) {
        LegalLink.USAGE_GUIDE -> openUsageGuide
        LegalLink.PRIVACY_POLICY -> url?.let { address -> { openExternal(address) } }
        LegalLink.OPEN_SOURCE_LICENSES -> openLicenses
        LegalLink.AD_PRIVACY -> privacyOptions
    }
}

/** 메인 하단 묶음에 무엇을 보일지 정한다. */
internal object LegalFooterPolicy {
    /** 보일 링크를 순서대로. 광고 개인정보 설정은 UMP 옵션 창이 있을 때(동의 필요 지역)만. */
    fun links(consentOptionsAvailable: Boolean): List<LegalLink> =
        LegalLink.entries.filter { consentOptionsAvailable || !it.needsConsentRegion }

    /** 버전명을 읽지 못하면 "버전 " 글자만 남지 않게 줄을 숨긴다. */
    fun versionLabel(versionName: String?): String? = versionName?.trim()?.takeIf { it.isNotEmpty() }
}
