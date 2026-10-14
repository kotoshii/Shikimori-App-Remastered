package com.gnoemes.shikimori.presentation.presenter.forum.converter

import android.content.Context
import com.gnoemes.shikimori.entity.forum.domain.Forum
import com.gnoemes.shikimori.entity.forum.domain.ForumType
import com.gnoemes.shikimori.utils.nameRes
import javax.inject.Inject

class ForumConverterImpl @Inject constructor(
        private val context: Context
) : ForumConverter {

    override fun apply(t: List<Forum>): List<Forum> {
        val list = t.filterNot { it.type in UNLISTABLE }.toMutableList()
        list.add(0, Forum(0, "", ForumType.ALL, ""))
        return list.map { convertForum(it) }
    }

    private fun convertForum(it: Forum): Forum = it.copy(name = context.getString(it.type.nameRes))

    companion object {
        /**
         * Forums `/api/forums` returns but the forum screen must not offer:
         * - hidden and premoderation are moderation forums, absent from shikimori.io's own menu;
         * - tierlists and reviews cannot be opened: `/api/topics` answers 422 for those `forum`
         *   values (checked 2026-10-01). Their topics still turn up in "all", which is why the
         *   types themselves stay in [ForumType].
         */
        private val UNLISTABLE = setOf(
                ForumType.HIDDEN,
                ForumType.PREMODERATION,
                ForumType.TIERLISTS,
                ForumType.REVIEWS
        )
    }
}