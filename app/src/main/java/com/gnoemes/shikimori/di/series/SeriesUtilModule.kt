package com.gnoemes.shikimori.di.series

import com.gnoemes.shikimori.data.repository.series.shikimori.converter.*
import com.gnoemes.shikimori.presentation.presenter.series.episodes.converter.EpisodeViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.series.episodes.converter.EpisodeViewModelConverterImpl
import com.gnoemes.shikimori.presentation.presenter.series.translations.converter.TranslationsViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.series.translations.converter.TranslationsViewModelConverterImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable

@Module
interface SeriesUtilModule {

    @Binds
    @Reusable
    fun bindSeriesResponseConverter(converter: EpisodeResponseConverterImpl): EpisodeResponseConverter

    @Binds
    @Reusable
    fun bindTranslationResponseConverter(converter: TranslationResponseConverterImpl): TranslationResponseConverter

    @Binds
    @Reusable
    fun bindEpisodeViewModelConverter(conterter: EpisodeViewModelConverterImpl): EpisodeViewModelConverter

    @Binds
    @Reusable
    fun bindTranslationViewModelConverter(converter: TranslationsViewModelConverterImpl): TranslationsViewModelConverter

    @Binds
    @Reusable
    fun bindVideoResponseConverter(converter: VideoResponseConverterImpl): VideoResponseConverter

    @Binds
    @Reusable
    fun bindvkConverter(converter: VkVideoConverterImpl): VkVideoConverter

    @Binds
    @Reusable
    fun bindSovetRomanticaConverter(converter: SovetRomanticaVideoConverterImpl): SovetRomanticaVideoConverter

    @Binds
    @Reusable
    fun bindOkConverter(converter: OkVideoConverterImpl): OkVideoConverter

    @Binds
    @Reusable
    fun bindMailRuConverter(converter: MailRuVideoConverterImpl): MailRuVideoConverter

    @Binds
    @Reusable
    fun bindMyviConverter(converter: MyviVideoConverterImpl): MyviVideoConverter

    @Binds
    @Reusable
    fun bindAllVideoConverter(converter: AllVideoVideoConverterImpl): AllVideoVideoConverter

    @Binds
    @Reusable
    fun bindAnimeJoyConverter(converter: AnimeJoyVideoConverterImpl): AnimeJoyVideoConverter

    @Binds
    @Reusable
    fun bindParsingConverter(converter: DocumentParsingConverterImpl): DocumentParsingConverter

}