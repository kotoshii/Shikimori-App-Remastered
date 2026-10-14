package com.gnoemes.shikimori.presentation.presenter.details

import android.content.Context
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.preference.secondTitleName
import com.gnoemes.shikimori.data.local.preference.titleName
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import com.gnoemes.shikimori.entity.common.domain.Status
import com.gnoemes.shikimori.entity.common.domain.TitleDetails
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.*
import com.gnoemes.shikimori.entity.roles.domain.Person
import com.gnoemes.shikimori.entity.user.domain.Statistic
import com.gnoemes.shikimori.entity.user.presentation.UserStatisticItem
import com.gnoemes.shikimori.presentation.presenter.common.converter.BBCodesTextProcessor
import com.gnoemes.shikimori.presentation.presenter.common.converter.labelRes
import com.gnoemes.shikimori.utils.color
import com.gnoemes.shikimori.utils.colorSpan
import com.gnoemes.shikimori.utils.date.DateTimeConverter
import org.joda.time.DateTime

/**
 * What the anime and the manga page convert the same way: the head, the description, the actions,
 * the statistics, and the pieces of the info block both have (second name, genre tags, the
 * ongoing/anons line, the anons date, creator rows). A subclass builds its own info block from them.
 */
abstract class BaseTitleDetailsViewModelConverter<D : TitleDetails>(
        protected val context: Context,
        protected val settings: SettingsSource,
        protected val dateTimeConverter: DateTimeConverter,
        private val textProcessor: BBCodesTextProcessor
) : TitleDetailsViewModelConverter<D> {

    /** The type the head shows. */
    protected abstract val headType: Type

    /** The statistics' status rows, in order: the api's names (english|russian) and the label. */
    protected abstract val statusLabels: List<Pair<String, Int>>

    override fun convertHead(it: D, isGuest: Boolean): DetailsHeadItem = DetailsHeadItem(
            headType,
            settings.titleName(it.name, it.nameRu),
            it.image,
            it.score,
            it.userRate?.status,
            isGuest
    )

    override fun convertDescriptionItem(description: String?): DetailsDescriptionItem {
        val processedText = description?.let { textProcessor.process(it) }
        return DetailsDescriptionItem(processedText)
    }

    override fun getActions(status: Status): DetailsActionItem {
        val actions = DetailsActionType.values().toMutableList()

        if (status == Status.ANONS) actions.remove(DetailsActionType.SIMILAR)

        return DetailsActionItem(actions)
    }

    override fun convertScores(t: List<Statistic>): List<UserStatisticItem> {
        val sum = t.sumBy { it.value }

        return t
                .asSequence()
                .map { UserStatisticItem(it.name, it.value, it.value / sum.toFloat()) }
                .sortedByDescending { it.category.toInt() }
                .toMutableList()
    }

    override fun convertStatuses(t: List<Statistic>): List<UserStatisticItem> {
        val sum = t.sumBy { it.value }

        return statusLabels.mapNotNull { (names, label) ->
            val item = t.find { names.split("|").contains(it.name) }
            if (item != null) UserStatisticItem(context.getString(label), item.value, item.value / sum.toFloat())
            else null
        }
    }

    protected fun secondName(it: D): String = settings.secondTitleName(it.name, it.nameRu)

    //demographic first, then genres, then the long tail of themes - GenreV2.Kind is declared in that
    //order, and sortedBy is stable so shikimori's own order survives inside each kind
    protected fun genreTags(genres: List<GenreV2>): List<DetailsTagItem> =
            genres
                    .sortedBy { genre -> genre.kind.ordinal }
                    .map { genre -> DetailsTagItem(genre.id, DetailsTagItem.TagType.GENRE, genre.russianName, genre) }

    /** «Анонс» or «Онгоинг» in its colour; null for any other status. */
    protected fun statusItem(status: Status): InfoItem? {
        if (status != Status.ANONS && status != Status.ONGOING) return null

        val color = if (status == Status.ONGOING) R.color.status_ongoing else R.color.status_anons
        val description = context.getString(status.labelRes).colorSpan(context.color(color))
        return InfoItem(description, context.getString(R.string.common_status))
    }

    protected fun anonsDateItem(dateAired: DateTime): InfoItem =
            InfoItem(dateAired.toTitleDate(), context.getString(R.string.details_release_date))

    /**
     * The people with one of [roles] (api name → label), as clickable rows: those with [first] come
     * first, and each shows the first of its roles that is in [roles].
     */
    protected fun creatorItems(creators: List<Pair<Person, List<String>>>, roles: Map<String, Int>, first: String): List<InfoClickableItem> =
            creators
                    .asSequence()
                    .filter { (_, personRoles) -> personRoles.any { it in roles } }
                    .sortedByDescending { (_, personRoles) -> personRoles.contains(first) }
                    .mapNotNull { (person, personRoles) ->
                        val role = personRoles.firstOrNull { it in roles } ?: return@mapNotNull null
                        InfoClickableItem(person.id, person.linkedType, settings.titleName(person.name, person.nameRu), person.image, context.getString(roles.getValue(role)))
                    }
                    .toList()

    protected fun DateTime.toTitleDate() = this.toString("dd ${dateTimeConverter.convertShortMonth(this)} yyyy")
}
