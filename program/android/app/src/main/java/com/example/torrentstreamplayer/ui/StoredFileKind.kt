package com.example.torrentstreamplayer.ui

import androidx.annotation.DrawableRes
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.meta.VideoFormat

/** 저장 파일 행의 형식 아이콘. 영상 판단은 [VideoFormat] 표를 그대로 따른다. */
internal enum class StoredFileKind(@DrawableRes val icon: Int) {
    VIDEO(R.drawable.ic_movie),
    OTHER(R.drawable.ic_file);

    companion object {
        fun of(name: String): StoredFileKind = if (VideoFormat.of(name) != null) VIDEO else OTHER
    }
}
