package com.gnoemes.shikimori.entity.comment.presentation

import com.gnoemes.shikimori.entity.common.presentation.richtext.LinkTarget

/** What the comment sheet shows (P8-T4, Q11). */
sealed class CommentSheetState {

    object Loading : CommentSheetState()

    /**
     * The comment, and where «Открыть обсуждение» leads: its topic, or the profile whose wall it is on;
     * null when it is in the topic the screen under the sheet shows.
     */
    data class Shown(val comment: CommentViewModel, val open: LinkTarget.Screen?) : CommentSheetState()

    /** Deleted: the api answers 404. */
    object Deleted : CommentSheetState()

    /** Could not load; «Повторить» tries again. */
    object Failed : CommentSheetState()
}
