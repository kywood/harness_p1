package com.example.torrentstreamplayer.ui

import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R

/**
 * 이용 안내 시트의 절(`design/specs/usage-guide.md` 2절). 선언 순서가 화면 순서다.
 * 저작권 안내는 절이 아니라 맨 위 강조 카드(`main_rights_notice`·`guide_rights_body`)에 있다.
 */
internal enum class UsageGuideSection(@StringRes val title: Int, @StringRes val body: Int) {
    WHAT(R.string.guide_what_title, R.string.guide_what_body),
    UPLOAD(R.string.guide_upload_title, R.string.guide_upload_body),
    STORAGE(R.string.guide_storage_title, R.string.guide_storage_body),
    DATA(R.string.guide_data_title, R.string.guide_data_body),
    ADS(R.string.guide_ads_title, R.string.guide_ads_body),
}
