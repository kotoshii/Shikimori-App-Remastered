package com.gnoemes.shikimori.utils

import androidx.annotation.StringRes
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.series.domain.TranslationType

/** «Озвучка», «Субтитры», «Оригинал»: the kind as the screens show it; null for «не выбрано». */
@get:StringRes
val TranslationType.labelRes: Int?
    get() = when (this) {
        TranslationType.VOICE_RU -> R.string.translation_voice
        TranslationType.SUB_RU -> R.string.translation_subtitles
        TranslationType.RAW -> R.string.translation_original
        TranslationType.ALL -> null
    }
