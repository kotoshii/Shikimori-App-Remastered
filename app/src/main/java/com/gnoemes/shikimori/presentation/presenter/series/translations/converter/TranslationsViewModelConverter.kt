package com.gnoemes.shikimori.presentation.presenter.series.translations.converter

import com.gnoemes.shikimori.entity.series.domain.Translation
import com.gnoemes.shikimori.entity.series.domain.TranslationSetting
import com.gnoemes.shikimori.entity.series.presentation.TranslationViewModel

interface TranslationsViewModelConverter {
    fun convertTranslations(translations: List<Translation>, setting: TranslationSetting?): List<TranslationViewModel>
}