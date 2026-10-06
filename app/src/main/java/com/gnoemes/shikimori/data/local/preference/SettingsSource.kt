package com.gnoemes.shikimori.data.local.preference

import com.gnoemes.shikimori.entity.chronology.ChronologyType
import com.gnoemes.shikimori.entity.rates.domain.RateSwipeAction
import com.gnoemes.shikimori.entity.series.domain.PlayerType
import com.gnoemes.shikimori.entity.series.domain.TranslationType

interface SettingsSource {

    val isAutoStatus : Boolean

    val isAutoIncrement : Boolean

    val isRussianNaming: Boolean

    val allowR18Content: Boolean

    val altSourceByDefault: Boolean

    val isAskForPlayer : Boolean

    val translationType : TranslationType

    val playerType : PlayerType

    val useLocalTranslationSettings : Boolean

    val downloadFolder : String

    val isExternalBestQuality : Boolean

    val rateSwipeToLeftAction : RateSwipeAction

    val rateSwipeToRightAction : RateSwipeAction

    var chronologyType : ChronologyType

    val hideAnime365: Boolean

    /** Every hosting the app has displayed, so the filter screen has something to offer. */
    var seenHostings: Set<String>

    /** Hostings the user has chosen to hide, matched by domain and subdomain. */
    val hiddenHostings: Set<String>

    /**
     * The v2 genre vocabularies, encoded one genre per entry. Written only by
     * `GenreVocabularySource`, which merges into them and never removes - see its documentation
     * for why that matters.
     */
    var animeGenres: Set<String>

    var mangaGenres: Set<String>
}