package com.gnoemes.shikimori.di.series

import com.gnoemes.shikimori.data.repository.series.shikimori.converter.*
import com.gnoemes.shikimori.data.repository.series.shikimori.parser.*
import com.gnoemes.shikimori.presentation.presenter.series.episodes.converter.EpisodeViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.series.episodes.converter.EpisodeViewModelConverterImpl
import com.gnoemes.shikimori.presentation.presenter.series.translations.converter.TranslationsViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.series.translations.converter.TranslationsViewModelConverterImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.multibindings.IntoSet

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

    //one line per hosting the app can play, see HostingParser. SovetRomanticaParser is left out on
    //purpose, see its own comment.
    @Binds
    @IntoSet
    fun bindVkParser(parser: VkParser): HostingParser

    @Binds
    @IntoSet
    fun bindSibnetParser(parser: SibnetParser): HostingParser

    @Binds
    @IntoSet
    fun bindOkParser(parser: OkParser): HostingParser

    @Binds
    @IntoSet
    fun bindMailRuParser(parser: MailRuParser): HostingParser

    @Binds
    @IntoSet
    fun bindAllVideoParser(parser: AllVideoParser): HostingParser

    @Binds
    @IntoSet
    fun bindAnimeJoyParser(parser: AnimeJoyParser): HostingParser

    @Binds
    @IntoSet
    fun bindDzenParser(parser: DzenParser): HostingParser

    @Binds
    @IntoSet
    fun bindCdaParser(parser: CdaParser): HostingParser

    @Binds
    @IntoSet
    fun bindKodikParser(parser: KodikParser): HostingParser

    @Binds
    @IntoSet
    fun bindAnime365Parser(parser: Anime365Parser): HostingParser

    @Binds
    @IntoSet
    fun bindMatreshkaParser(parser: MatreshkaParser): HostingParser

}