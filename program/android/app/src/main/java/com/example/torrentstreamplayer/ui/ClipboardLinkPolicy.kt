package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.meta.NetworkSpec

/**
 * [붙여넣기] 버튼이 입력란에 넣을 글자. 메인 입력란과 링크 추가 창이 같이 쓴다([PasteButton]).
 *
 * 클립보드 글자에서 링크를 찾으면([IncomingLinkParser.firstLink]) 그 링크만 넣는다("보세요 magnet:?…"처럼 앞뒤 말이
 * 붙어 있어도). 링크가 아니면 다듬은 글자를 그대로 넣어, 누른 뒤 [다운로드]·[바로 재생]에서 기존 검증 문구가
 * 나오게 한다. 링크 하나보다 긴 글자([NetworkSpec.MAX_LINK_CHARS] 초과)는 넣지 않는다. 클립보드 글자는 로그에 남기지
 * 않는다.
 */
internal object ClipboardLinkPolicy {
    /** 입력란에 넣을 글자, 또는 입력란을 그대로 둘 때 null. [clip]은 클립보드의 글자(텍스트가 아니면 null). */
    fun pasteText(clip: CharSequence?): String? {
        if (clip.isNullOrBlank()) return null
        // 대화 전체가 복사돼 있어도 찾는 범위만 읽는다(한 글자 더 읽어 잘린 링크를 알아본다).
        val end = minOf(clip.length, IncomingLinkParser.MAX_SHARED_CHARS + 1)
        val text = clip.subSequence(0, end).toString()
        return when (val found = IncomingLinkParser.firstLink(text)) {
            is IncomingLinkResult.Valid -> found.link.value
            is IncomingLinkResult.Invalid -> text.trim().takeIf { it.length <= NetworkSpec.MAX_LINK_CHARS }
        }
    }
}
