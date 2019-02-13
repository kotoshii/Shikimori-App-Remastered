package com.gnoemes.shikimori.presentation.presenter.series.episodes.converter

import com.gnoemes.shikimori.entity.series.domain.Episode
import com.gnoemes.shikimori.entity.series.presentation.EpisodeViewModel
import io.reactivex.functions.Function

interface EpisodeViewModelConverter : Function<List<Episode>, List<EpisodeViewModel>>