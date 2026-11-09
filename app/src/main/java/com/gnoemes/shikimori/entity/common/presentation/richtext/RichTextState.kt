package com.gnoemes.shikimori.entity.common.presentation.richtext

import java.util.Collections
import java.util.IdentityHashMap

/**
 * What the user opened in one [text]: whether it is cut at 80dp and opened in full, and which spoilers are
 * open. The content view and the renderer keep this here, not in their own fields and views, so a list that
 * reuses a row for another item can show a text again as the user left it: the topic page keeps one per
 * comment and one for its post (P8-T4, the one-list step). A screen that passes none gets one per text from
 * the content view, which lives as long as its views.
 */
class RichTextState(val text: RichText) {

    /** Whether the text is cut at 80dp: decided once, at its first layout with a width; null until then. */
    var isCut: Boolean? = null

    /** A cut text opened in full. */
    var isExpanded: Boolean = false

    /** The `||spoilers||` opened, by the parser's numbers (once per text). */
    val revealed: MutableSet<Int> = mutableSetOf()

    /** The `[spoiler=label]`s and the spoiler blocks opened, by identity: two alike in one text are still two. */
    val opened: MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())
}
