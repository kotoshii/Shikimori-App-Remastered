package com.gnoemes.shikimori.presentation.presenter.details

import com.gnoemes.shikimori.entity.common.domain.Status
import com.gnoemes.shikimori.entity.common.domain.TitleDetails
import com.gnoemes.shikimori.entity.common.presentation.DetailsActionItem
import com.gnoemes.shikimori.entity.common.presentation.DetailsDescriptionItem
import com.gnoemes.shikimori.entity.common.presentation.DetailsHeadItem
import com.gnoemes.shikimori.entity.common.presentation.DetailsInfoItem
import com.gnoemes.shikimori.entity.roles.domain.Person
import com.gnoemes.shikimori.entity.user.domain.Statistic
import com.gnoemes.shikimori.entity.user.presentation.UserStatisticItem

/** The anime and the manga page's view models; see [BaseTitleDetailsViewModelConverter]. */
interface TitleDetailsViewModelConverter<D : TitleDetails> {

    fun convertHead(it: D, isGuest: Boolean): DetailsHeadItem

    fun convertInfo(it: D, creators: List<Pair<Person, List<String>>>): DetailsInfoItem

    fun convertDescriptionItem(description: String?): DetailsDescriptionItem

    fun getActions(status: Status): DetailsActionItem

    fun convertScores(t: List<Statistic>): List<UserStatisticItem>

    fun convertStatuses(t: List<Statistic>): List<UserStatisticItem>
}
