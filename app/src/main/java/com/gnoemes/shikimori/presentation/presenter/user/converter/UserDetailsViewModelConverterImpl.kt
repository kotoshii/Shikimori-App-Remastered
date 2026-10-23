package com.gnoemes.shikimori.presentation.presenter.user.converter

import com.gnoemes.shikimori.entity.club.domain.Club
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.user.domain.*
import com.gnoemes.shikimori.entity.user.presentation.*
import java.math.MathContext
import java.math.RoundingMode
import javax.inject.Inject

class UserDetailsViewModelConverterImpl @Inject constructor() : UserDetailsViewModelConverter {

    companion object {
        const val FRIENDS_INITIAL = 5
        const val CLUBS_INITIAL = 5
        const val FAVORITES_INITIAL = 3
    }

    override fun convertHead(t: UserDetails): UserHeadViewModel =
            UserHeadViewModel(
                    t.nickname,
                    t.lastOnline,
                    t.image
            )

    override fun convertInfo(t: UserDetails): UserInfoViewModel =
            UserInfoViewModel(
                    t.commonInfo.joinToString(" / "),
                    t.isFriend,
                    t.isIgnored,
                    t.isMe
            )

    override fun convertFriends(t: List<UserBrief>, mayHaveMore: Boolean): UserContentViewModel {
        val friends = t.map { UserContentItem(it.id, Type.USER, it.image.x160) }
        return convertContentViewModel(UserContentType.FRIENDS, friends, mayHaveMore)
    }

    override fun convertFavorites(t: FavoriteList): UserContentViewModel {
        val items = t
                .all
                .map { UserContentItem(it.id, convertFavoriteType(it.type), it.image) }
        return convertContentViewModel(UserContentType.FAVORITES, items)
    }

    override fun convertClubs(t: List<Club>): UserContentViewModel {
        val clubs = t.map { UserContentItem(it.id, it.linkedType, it.image.original) }
        return convertContentViewModel(UserContentType.CLUBS, clubs)
    }

    override fun convertAnimeRate(stats: UserStats): UserRateViewModel = UserRateViewModel(
            true,
            convertRate(stats.animeStatuses),
            countAvgScore(stats.scores.anime),
            convertScores(stats.scores.anime),
            convertTypes(stats.types.anime),
            convertRatings(stats.ratings.anime)
    )

    override fun convertMangaRate(stats: UserStats): UserRateViewModel = UserRateViewModel(
            false,
            convertRate(stats.mangaStatuses),
            countAvgScore(stats.scores.manga ?: emptyList()),
            convertScores(stats.scores.manga ?: emptyList()),
            convertTypes(stats.types.manga ?: emptyList()),
            emptyList()
    )

    private fun countAvgScore(scores: List<Statistic>): Float {
        if (scores.isEmpty()) return 0f

        val sum = scores.sumBy { it.value }
        return scores
                .sumBy { it.name.toInt() * it.value }
                .toFloat()
                .div(sum)
                .toBigDecimal(MathContext(3, RoundingMode.UP))
                .toFloat()
    }

    private fun convertRatings(ratings: List<Statistic>): List<UserStatisticItem> {
        val items = mutableListOf(
                UserStatisticItem("G", 0, 0f),
                UserStatisticItem("PG", 0, 0f),
                UserStatisticItem("PG-13", 0, 0f),
                UserStatisticItem("R-17", 0, 0f),
                UserStatisticItem("R+", 0, 0f),
                UserStatisticItem("Rx", 0, 0f)
        )
        val sum = ratings.sumBy { it.value }

        return ratings
                .mapNotNull { stat ->
                    val item = items.find { it.category == stat.name }
                    item?.copy(count = stat.value, progress = stat.value / sum.toFloat())
                }
    }

    //every kind the api counts, in its own words and order, as the site's profile shows them: a list of
    //the app's own dropped the kinds it had no word for («TV Спешл»), though they counted in the total (BUGS #76)
    private fun convertTypes(types: List<Statistic>): List<UserStatisticItem> {
        val sum = types.sumBy { it.value }
        return types.map { UserStatisticItem(it.name, it.value, if (sum > 0) it.value / sum.toFloat() else 0f) }
    }


    private fun convertScores(scores: List<Statistic>): List<UserStatisticItem> {
        val sum = scores.sumBy { it.value }

        return scores
                .asSequence()
                .map { UserStatisticItem(it.name, it.value, it.value / sum.toFloat()) }
                .sortedByDescending { it.category.toInt() }
                .toMutableList()
    }

    private fun convertRate(statuses: List<Status>?): Map<RateProgressStatus, Int> {
        if (statuses.isNullOrEmpty()) return emptyMap()

        return mapOf(
                Pair(RateProgressStatus.PLANNED, statuses.countRate { it.name == RateStatus.PLANNED }),
                Pair(RateProgressStatus.COMPLETED, statuses.countRate { it.name == RateStatus.COMPLETED }),
                Pair(RateProgressStatus.IN_PROGRESS, statuses.countRate { it.name == RateStatus.WATCHING }),
                Pair(RateProgressStatus.DROPPED, statuses.countRate { it.name == RateStatus.DROPPED })
        )
    }

    private inline fun List<Status>.countRate(crossinline block: (Status) -> Boolean): Int = sumBy { if (block.invoke(it)) it.size else 0 }

    private fun convertContentViewModel(type: UserContentType, items: List<UserContentItem>, mayHaveMore: Boolean = false): UserContentViewModel {
        val initialCount = getInitial(type)

        val isNeedMore = isNeedMoreItem(type, items.size)

        return UserContentViewModel(
                type,
                if (isNeedMore) items.take(initialCount) else items,
                isNeedMore,
                items.size - initialCount,
                mayHaveMore
        )
    }

    private fun getInitial(type: UserContentType): Int =
            when (type) {
                UserContentType.CLUBS -> CLUBS_INITIAL
                UserContentType.FAVORITES -> FAVORITES_INITIAL
                UserContentType.FRIENDS -> FRIENDS_INITIAL
            }

    private fun isNeedMoreItem(type: UserContentType, size: Int): Boolean =
            when (type) {
                UserContentType.FAVORITES -> size > 12
                else -> size > 15
            }

    private fun convertFavoriteType(type: FavoriteType): Type =
            when (type) {
                FavoriteType.ANIME -> Type.ANIME
                FavoriteType.CHARACTERS -> Type.CHARACTER
                FavoriteType.MANGA -> Type.MANGA
                FavoriteType.MANGAKAS -> Type.PERSON
                FavoriteType.PEOPLE -> Type.PERSON
                FavoriteType.PRODUCERS -> Type.PERSON
                FavoriteType.SEYU -> Type.PERSON
            }
}