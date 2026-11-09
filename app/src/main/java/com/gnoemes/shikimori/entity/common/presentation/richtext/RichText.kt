package com.gnoemes.shikimori.entity.common.presentation.richtext

/**
 * User-written text — a description, a topic, a comment — read from the api's rendered HTML
 * (`description_html`, `html_body`, `html_footer`) by `HtmlContentParser`, and turned into views by the
 * content view. No Android here, so the parser runs in unit tests on the PC.
 *
 * Data classes all the way down: two parses of the same HTML are equal, so a list's diff skips a row whose
 * text did not change. Which HTML gives which part: DOCS knowledge/BB_CODES.md §8.
 */
data class RichText(val blocks: List<Block>) {

    val isEmpty: Boolean get() = blocks.isEmpty()

    /** The images a tap can enlarge, in reading order: the site opens them as one gallery. */
    fun zoomableImages(): List<Image> = blocks.flatMap { it.zoomableImages() }

    companion object {
        val EMPTY = RichText(emptyList())
    }
}

sealed class Block {

    /** Text with its formatting, links, smileys and spoilers inside a sentence. */
    data class Paragraph(val inlines: List<Inline>) : Block()

    /** `#`…`#####`, `[h3]`, and users' `[div=headline]`: level 1 is the biggest, 6 the smallest. */
    data class Heading(val level: Int, val inlines: List<Inline>) : Block()

    /** `[quote]` and `> `. The author line is empty when the quote has none. */
    data class Quote(val author: List<Inline>, val blocks: List<Block>) : Block()

    /** A spoiler shown as a button with its label; a tap opens the box. «Скрыто модератором» is one. */
    data class Spoiler(
            val label: List<Inline>,
            val blocks: List<Block>,
            val isFullWidth: Boolean,
            val isCentered: Boolean
    ) : Block()

    /** `[center]`, `[right]`. */
    data class Aligned(val alignment: Alignment, val blocks: List<Block>) : Block()

    /** `- `, `* `, `+ `, `[list]`; an item can hold a nested list. */
    data class BulletList(val items: List<List<Block>>) : Block()

    /** A code block: its lines kept as written. */
    data class Code(val text: String, val language: String?) : Block()

    /** `[hr]`. */
    object Divider : Block()

    /** Images that stand next to each other in the text; the site shows them side by side when they fit. */
    data class Images(val images: List<Image>) : Block()

    data class VideoCard(val video: Video) : Block()

    /** `[wall]`: a grid of images and videos. A news topic has its own in `html_footer`. */
    data class Wall(val videos: List<Video>, val images: List<Image>) : Block()

    /** Poster cards of titles (`[animes ids=…]`, collections), and the «ещё +N» tile of a shortened list. */
    data class Cards(val cards: List<Card>, val more: MoreLink?) : Block()

    /** A critique's ratings: «Сюжет — 10, Эпик вин!». */
    data class Ratings(val lines: List<Rating>) : Block()

    /** A moderator's ban under a comment: the moderator and the reason. */
    data class Ban(val inlines: List<Inline>) : Block()

    /** The comments that answer this one, «Ответ:» or «Ответы:» and their mentions. */
    data class Replies(val label: String, val inlines: List<Inline>) : Block()

    /** `[poll]`: the app has no polls, so this stands for a link to the post on the site. */
    data class Poll(val id: Long) : Block()

    fun zoomableImages(): List<Image> = when (this) {
        is Paragraph -> inlines.flatMap { it.zoomableImages() }
        is Heading -> inlines.flatMap { it.zoomableImages() }
        is Quote -> blocks.flatMap { it.zoomableImages() }
        is Spoiler -> blocks.flatMap { it.zoomableImages() }
        is Aligned -> blocks.flatMap { it.zoomableImages() }
        is BulletList -> items.flatten().flatMap { it.zoomableImages() }
        is Images -> images.filter { it.zoomUrl != null }
        is Wall -> images.filter { it.zoomUrl != null }
        else -> emptyList()
    }
}

sealed class Inline {

    data class Text(val text: String, val style: Style) : Inline()

    /** A shikimori smiley, shown about the height of the text. */
    data class Smiley(val code: String, val url: String, val style: Style) : Inline()

    /** A user's 16 px avatar before the nickname. */
    data class Avatar(val url: String, val style: Style) : Inline()

    object LineBreak : Inline()

    /**
     * `[spoiler=label]` inside a sentence: a dashed label; a tap shows the content — in place when it is
     * only text, below the line when it holds images or blocks.
     */
    data class LabelledSpoiler(val label: List<Inline>, val blocks: List<Block>) : Inline()

    fun zoomableImages(): List<Image> =
            if (this is LabelledSpoiler) blocks.flatMap { it.zoomableImages() } else emptyList()
}

/**
 * How a piece of text looks and what a tap on it does. Nested tags add up: a run inside `[b][i]` is bold
 * and italic, and a link or a size reaches into the blocks inside it.
 */
data class Style(
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isUnderline: Boolean = false,
        val isStrike: Boolean = false,
        /** Inline code: monospace on a light background. */
        val isCode: Boolean = false,
        /** A link to something deleted: grey, crossed out, and nothing to tap. */
        val isDeleted: Boolean = false,
        /** As the author wrote it, `0xAARRGGBB`. */
        val color: Int? = null,
        /** As the author wrote it, in the site's pixels; the site caps it at 35. */
        val sizePx: Int? = null,
        val link: LinkTarget? = null,
        /** The `||spoilers||` this text is in, outermost first; each hides the text until tapped. */
        val spoilers: List<Int> = emptyList()
) {
    companion object {
        val PLAIN = Style()
    }
}

enum class Alignment { CENTER, END }

data class Image(
        /** What to show: a preview or the original. */
        val url: String,
        /** What a tap enlarges; null when the site does not enlarge it. */
        val zoomUrl: String?,
        /** The size the author asked for, in the site's pixels. */
        val width: Int?,
        val height: Int?,
        /** The image's real size, when the site knows it (uploads), to keep its place before it loads. */
        val realWidth: Int?,
        val realHeight: Int?,
        /** `[poster]`: the full width. */
        val isPoster: Boolean,
        /** An image inside a link: a tap follows the link instead. */
        val link: LinkTarget?
)

data class Video(
        /** What a tap opens: the video's page, or the file of a `.mp4`/`.webm` link. */
        val url: String,
        val thumbnail: String?,
        /** The host as the site names it: `youtube`, `vk`, `shorts`, `html5`… */
        val host: String,
        val title: String?
)

data class Card(
        val target: LinkTarget?,
        val title: String,
        val poster: String?,
        /** The site hides the poster of a title it censors, and so does the app. */
        val isCensored: Boolean
)

data class MoreLink(val text: String, val target: LinkTarget)

data class Rating(val title: String, val score: String, val notice: String)
