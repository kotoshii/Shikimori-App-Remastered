package com.gnoemes.shikimori.entity.common.presentation.richtext

/**
 * What the user opened in each text of a list, by the item's id, for as long as the list's adapter lives (its
 * fragment keeps it while the view is rebuilt): a row that leaves the screen is reused for another item, and
 * its text is built again from this. A text read again (another instance, after a reload) starts closed.
 * The topic page keeps one for its comments, the forum for its rows.
 */
class RichTextStates {

    private val states = HashMap<Long, RichTextState>()

    fun of(id: Long, text: RichText): RichTextState =
            states[id]?.takeIf { it.text === text } ?: RichTextState(text).also { states[id] = it }
}
