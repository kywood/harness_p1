package com.example.torrentstreamplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.engine.MagnetHistory

/**
 * 재생 목록 항목의 보이는 제목. 이름 없는 마그넷에 저장된 자리 표시 제목([MagnetHistory.placeholderId])은
 * 기기 언어의 `playlist_untitled` 문구로 바꿔 보이고, 받은 이름은 그대로 보인다.
 */
@Composable
internal fun playlistTitle(magnet: String, title: String): String =
    MagnetHistory.placeholderId(magnet, title)?.let { stringResource(R.string.playlist_untitled, it) } ?: title
