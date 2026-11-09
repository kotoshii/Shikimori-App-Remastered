package com.gnoemes.shikimori.presentation.presenter.common.converter

import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.preference.titleName
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.richtext.Alignment
import com.gnoemes.shikimori.entity.common.presentation.richtext.Block
import com.gnoemes.shikimori.entity.common.presentation.richtext.Card
import com.gnoemes.shikimori.entity.common.presentation.richtext.Image
import com.gnoemes.shikimori.entity.common.presentation.richtext.Inline
import com.gnoemes.shikimori.entity.common.presentation.richtext.LinkTarget
import com.gnoemes.shikimori.entity.common.presentation.richtext.MoreLink
import com.gnoemes.shikimori.entity.common.presentation.richtext.Rating
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichText
import com.gnoemes.shikimori.entity.common.presentation.richtext.Style
import com.gnoemes.shikimori.entity.common.presentation.richtext.Video
import com.gnoemes.shikimori.utils.appendHostIfNeed
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.HttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import javax.inject.Inject

/**
 * The api's rendered HTML of user-written text (`description_html`, `html_body`, `html_footer`) → [RichText].
 * The server has already turned BB codes and the site's markdown into HTML, so the app reads only that
 * (BB_CODES.md §1, §8). Slow on a very large text (a 1.6 MB topic exists): call it on a background thread.
 * Names in links follow Settings → «Русские названия» at the time of the call.
 */
class HtmlContentParser @Inject constructor(
        private val settings: SettingsSource
) {
    /** [postUrl]: the page of the post that holds the text, on the site; a poll in it links there. */
    fun parse(html: String?, postUrl: String? = null): RichText =
            HtmlContentReader(postUrl) { name, nameRu -> settings.titleName(name, nameRu) }.read(html)
}

/**
 * One reading of one text; [pickName] is the naming rule (`SettingsSource.titleName`), [postUrl] as in
 * [HtmlContentParser.parse]. Kept apart from [HtmlContentParser] so the unit tests can run it without the
 * settings.
 */
internal class HtmlContentReader(
        private val postUrl: String? = null,
        private val pickName: (name: String, nameRu: String?) -> String
) {

    private var nextSpoilerId = 0

    fun read(html: String?): RichText {
        if (html.isNullOrBlank()) return RichText.EMPTY
        val sink = Sink()
        children(Jsoup.parseBodyFragment(withLeftoverDivs(html)).body(), sink, Style.PLAIN)
        return RichText(sink.finish())
    }

    /**
     * Users' `[div=…]…[/div]` that the api left as text. The Rails renderer the api still uses leaves every
     * `[div]` of a text as text when its openings and closings do not pair up (`bb_codes/tags/div_tag.rb`) - one
     * `[/div]` missing out of 82 is enough - while the live site's pages render them (BB_CODES.md §7). This makes
     * the `<div … data-div>` the site makes, by the site's own patterns: the line break after an opening tag and
     * around a closing one goes with the tag, and closing tags right after a closing one stay after it. Code and
     * tags' attributes are left alone. An unclosed one ends where its parent ends, as in a browser.
     */
    private fun withLeftoverDivs(html: String): String {
        if (!html.contains("[/div]")) return html
        return LEFTOVER_DIV.replace(html) { match ->
            val groups = match.groups
            when {
                groups[1] != null -> match.value
                groups[2] != null -> {
                    val classes = groups[3]?.value.orEmpty().split(' ')
                            .filter { it.isNotEmpty() && it.lowercase() !in FORBIDDEN_DIV_CLASSES }
                    val classAttr = if (classes.isEmpty()) "" else " class=\"${classes.joinToString(" ")}\""
                    "<div$classAttr${groups[4]?.value.orEmpty()} data-div>"
                }
                else -> "</div>" + groups[5]!!.value
            }
        }
    }

    // Walking the tree

    private fun walk(node: Node, sink: Sink, style: Style) {
        when (node) {
            is TextNode -> sink.text(node.wholeText, style)
            is Element -> element(node, sink, style)
        }
    }

    /** The children of [parent]; a pair of `name-en`/`name-ru` spans gives the one name the setting picks. */
    private fun children(parent: Element, sink: Sink, style: Style) {
        var nameShown = false
        for (child in parent.childNodes()) {
            if (child is Element && child.isNameSpan()) {
                if (!nameShown) sink.text(names(parent), style)
                nameShown = true
                continue
            }
            walk(child, sink, style)
        }
    }

    private fun element(el: Element, sink: Sink, style: Style) {
        if (el.isHidden()) return
        val tag = el.normalName()
        val headingLevel = headingLevel(el)
        when {
            tag == "br" -> sink.lineBreak()
            tag in SKIPPED_TAGS -> Unit
            tag == "img" -> img(el, sink, style)
            el.hasClass("b-replies") -> replies(el, sink, style)
            tag == "div" && el.hasClass("ban") -> sink.block(Block.Ban(inlines(el, style)))
            el.hasClass("b-spoiler_block") -> blockSpoiler(el, sink, style)
            el.hasClass("b-spoiler") -> labelledSpoiler(el, sink, style)
            el.hasClass("b-spoiler_inline") -> inlineSpoiler(el, sink, style)
            el.hasClass("b-quote") || el.hasClass("b-quote-v2") -> quote(el, sink, style)
            el.hasClass("b-image") -> image(el, style)?.let { sink.image(it) }
            el.hasClass("b-video") -> video(el)?.let { sink.block(Block.VideoCard(it)) }
            el.hasClass("b-shiki_wall") -> wall(el)?.let { sink.block(it) }
            tag == "article" && el.hasClass("b-catalog_entry") -> sink.block(Block.Cards(listOf(card(el)), null))
            tag == "a" && el.hasClass("see-more") -> more(el)?.let { sink.block(Block.Cards(emptyList(), it)) }
            el.hasClass("critique-stars") -> ratings(el)?.let { sink.block(it) }
            el.hasClass("poll-placeholder") -> poll(el)?.let { sink.block(it) }
            tag == "pre" -> sink.block(code(el))
            tag == "hr" -> sink.block(Block.Divider)
            tag == "ul" || tag == "ol" -> list(el, sink, style)
            headingLevel != null -> heading(el, sink, style, headingLevel)
            tag == "center" -> aligned(el, sink, style, Alignment.CENTER)
            el.hasClass("right-text") -> aligned(el, sink, style, Alignment.END)
            tag == "a" || el.hasAttr("data-href") -> link(el, sink, style)
            else -> generic(el, sink, style)
        }
    }

    /** Any other element: its style, its words from `data-text-ru`, and a line of its own when it is a block. */
    private fun generic(el: Element, sink: Sink, style: Style) {
        val isBlock = el.isBlockTag() && !el.hasClass("b-user16")
        if (isBlock) sink.boundary()
        //the crossed-out tag of a deleted target stands apart from the name before it, as on the site
        if (el.normalName() == "del" && el.parent()?.hasClass("b-entry-404") == true) {
            sink.text(" ", style.copy(isStrike = false, isDeleted = false))
        }
        content(el, sink, styleOf(el, style))
        if (isBlock) sink.boundary()
    }

    /** Words some elements keep only in attributes (contest status, a link's text), then the children. */
    private fun content(el: Element, sink: Sink, style: Style) {
        if (el.hasClass("translated-before")) sink.text(el.attr("data-text-ru"), style)
        children(el, sink, style)
        if (el.hasClass("translated-after")) sink.text(el.attr("data-text-ru"), style)
    }

    private fun styleOf(el: Element, style: Style): Style {
        var result = when (el.normalName()) {
            "strong", "b" -> style.copy(isBold = true)
            "em", "i" -> style.copy(isItalic = true)
            "u", "ins" -> style.copy(isUnderline = true)
            //`<s>` is only the @ of a mention, never strikethrough (BB_CODES.md §4)
            "del", "strike" -> style.copy(isStrike = true)
            "code", "kbd", "samp", "tt" -> style.copy(isCode = true)
            else -> style
        }
        val css = el.attr("style")
        if (css.isNotEmpty()) {
            for (declaration in css.split(';')) {
                val name = declaration.substringBefore(':').trim().lowercase()
                val value = declaration.substringAfter(':', "").trim()
                when (name) {
                    "color" -> CssColor.parse(value)?.let { result = result.copy(color = it) }
                    "font-size" -> PIXELS.find(value)?.groupValues?.get(1)?.toIntOrNull()
                            ?.let { result = result.copy(sizePx = it) }
                }
            }
        }
        if (el.hasClass("b-entry-404")) result = result.copy(isDeleted = true, link = null)
        return result
    }

    // Links

    private fun link(el: Element, sink: Sink, style: Style) {
        val linkStyle = when {
            el.hasClass("b-entry-404") -> style.copy(isDeleted = true, link = null)
            style.isDeleted -> style
            else -> linkTarget(el)?.let { style.copy(link = it) } ?: style
        }
        //`div.linkeable[data-href]` is a block
        val isBlock = el.isBlockTag()
        if (isBlock) sink.boundary()
        content(el, sink, styleOf(el, linkStyle))
        if (isBlock) sink.boundary()
    }

    private fun linkTarget(el: Element): LinkTarget? {
        val attrs = dataAttrs(el)
        if (attrs != null) {
            val id = attrs.long("id")
            when (val type = attrs.string("type")) {
                "anime", "manga", "ranobe", "character", "person", "user", "comment" ->
                    if (id != null) return LinkTarget.Screen(SCREEN_TYPES.getValue(type), id)
                //`entry` is the old name of a topic
                "topic", "entry" -> if (id != null) return LinkTarget.Screen(Type.TOPIC, id)
                //no messages screen
                "message" -> return null
            }
        }
        if (el.hasClass("b-user16") || el.parent()?.hasClass("b-user16") == true) {
            val nickname = el.attr("title").ifEmpty { el.text() }.trim()
            if (nickname.isNotEmpty()) return LinkTarget.UserByNickname(nickname)
        }
        return urlTarget(el.attr("href").ifEmpty { el.attr("data-href") }, el.text())
    }

    /**
     * A link by its address alone: some links carry no ids (BB_CODES.md §8), so a site page is read from its
     * path. [text] is the link's own text, which names a studio.
     */
    private fun urlTarget(href: String, text: String? = null): LinkTarget? {
        val trimmed = href.trim()
        if (trimmed.isEmpty()) return null
        //the site renders `[url=mailto:…]` as `http://mailto:…` (BB_CODES.md §6)
        if (trimmed.startsWith(BROKEN_MAILTO)) return LinkTarget.Web("mailto:" + trimmed.removePrefix(BROKEN_MAILTO))
        val url = trimmed.appendHostIfNeed()
        val parsed = HttpUrl.parse(url) ?: return LinkTarget.Web(url)
        if (!SHIKIMORI_HOST.matches(parsed.host())) return LinkTarget.Web(url)
        val path = parsed.encodedPath()
        ENTITY_PATH.matchEntire(path)?.let { match ->
            val id = match.groupValues[2].toLongOrNull()
            if (id != null) return LinkTarget.Screen(PATH_TYPES.getValue(match.groupValues[1]), id)
        }
        (TOPIC_PATH.matchEntire(path) ?: FORUM_TOPIC_PATH.matchEntire(path))?.let { match ->
            match.groupValues[1].toLongOrNull()?.let { return LinkTarget.Screen(Type.TOPIC, it) }
        }
        COMMENT_PATH.matchEntire(path)?.let { match ->
            match.groupValues[1].toLongOrNull()?.let { return LinkTarget.Screen(Type.COMMENT, it) }
        }
        STUDIO_PATH.matchEntire(path)?.let { match ->
            //a bare address shows itself as the text, which is no name
            val name = text?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("/") && !it.contains("://") }
            match.groupValues[1].toLongOrNull()?.let { return LinkTarget.Studio(it, name) }
        }
        return LinkTarget.Web(url)
    }

    // Images and video

    private fun img(el: Element, sink: Sink, style: Style) {
        val src = el.attr("src")
        when {
            src.isBlank() -> Unit
            el.hasClass("smiley") -> sink.inline(Inline.Smiley(el.attr("alt"), absolute(src.replace(" ", "%20")), style))
            el.parent()?.hasClass("b-user16") == true || el.parent()?.parent()?.hasClass("b-user16") == true ->
                sink.inline(Inline.Avatar(absolute(retina(el) ?: src), style))
            //a bare image: `[image=deleted]`'s placeholder, or an image the renderer left unwrapped
            else -> sink.image(Image(imageUrl(src, null), null, el.intAttr("width"), el.intAttr("height"),
                    el.intAttr("data-width"), el.intAttr("data-height"), false, style.link))
        }
    }

    /**
     * `a.b-image` (a tap enlarges it) or `span.b-image` (`no-zoom`, and posters). The site enlarges no
     * poster; the app does (the user, 2026-11-09, P8-T4 Q16): a poster is shown whole, its own image is the
     * zoom. An image the author marked `no-zoom` stays as it is.
     */
    private fun image(wrapper: Element, style: Style): Image? {
        val img = wrapper.selectFirst("img") ?: return null
        val src = img.attr("src").ifBlank { return null }
        val original = dataAttrs(wrapper)?.string("src")
        val isPoster = wrapper.hasClass("b-poster")
        val zoom = when {
            wrapper.normalName() == "a" -> wrapper.attr("data-href").ifEmpty { wrapper.attr("href") }
            isPoster -> src
            else -> ""
        }
        return Image(
                url = imageUrl(src, original),
                zoomUrl = zoom.takeIf { it.isNotBlank() }?.let { imageUrl(it, original) },
                width = img.intAttr("width"),
                height = img.intAttr("height"),
                realWidth = img.intAttr("data-width"),
                realHeight = img.intAttr("data-height"),
                isPoster = isPoster,
                link = style.link
        )
    }

    private fun video(el: Element): Video? {
        val host = el.selectFirst(".marker")?.text()?.trim().orEmpty()
                .ifEmpty { el.classNames().firstOrNull { it !in VIDEO_NON_HOST_CLASSES } ?: "video" }
        val title = el.selectFirst("span.name")?.let { it.attr("title").ifBlank { it.text() } }?.trim()?.ifEmpty { null }
        val link = el.selectFirst("a.video-link")
        if (link != null) {
            val url = link.attr("href").ifBlank { link.attr("data-href") }.ifBlank { return null }
            val thumbnail = link.selectFirst("img")?.attr("src")?.ifBlank { null }
            return Video(absolute(url), thumbnail?.let { imageUrl(it, null) }, host, title)
        }
        //a `.mp4`/`.webm` link: the generated thumbnail is in data-src, src is a placeholder
        val img = el.selectFirst("img[data-video]") ?: return null
        val thumbnail = img.attr("data-src").ifBlank { img.attr("src") }.ifBlank { null }
        return Video(absolute(img.attr("data-video")), thumbnail?.let { imageUrl(it, null) }, host, title)
    }

    private fun wall(el: Element): Block.Wall? {
        val videos = el.select("div.b-video").mapNotNull { video(it) }
        val images = el.select(".b-image").mapNotNull { image(it, Style.PLAIN) }
        return if (videos.isEmpty() && images.isEmpty()) null else Block.Wall(videos, images)
    }

    // Blocks

    private fun replies(el: Element, sink: Sink, style: Style) {
        val label = (if (el.hasClass("single")) el.attr("data-text-alt-ru") else el.attr("data-text-ru")).trim()
        val mentions = inlines(el, style)
        if (mentions.isNotEmpty()) sink.block(Block.Replies(label, mentions))
    }

    private fun blockSpoiler(el: Element, sink: Sink, style: Style) {
        val label = el.children().firstOrNull { it.normalName() == "span" }
        val box = el.children().firstOrNull { it.normalName() == "div" }
        sink.block(Block.Spoiler(
                label = label?.let { inlines(it, style) }.orEmpty(),
                blocks = box?.let { blocks(it, style) }.orEmpty(),
                isFullWidth = el.hasClass("is-fullwidth"),
                isCentered = el.hasClass("is-centered")
        ))
    }

    /** A `div`, but the site shows it inside the sentence (BB_CODES.md §8), so it stays in the paragraph. */
    private fun labelledSpoiler(el: Element, sink: Sink, style: Style) {
        val label = el.children().firstOrNull { it.normalName() == "label" }
        val inner = el.selectFirst("div.content > div.inner") ?: el.selectFirst("div.content")
        sink.inline(Inline.LabelledSpoiler(
                label = label?.let { inlines(it, style) }.orEmpty(),
                blocks = inner?.let { blocks(it, style) }.orEmpty()
        ))
    }

    /**
     * `||text||`: its text stays in the sentence, marked hidden. Images can sit inside one (P8-T4 research 3,
     * T41); that one becomes a labelled spoiler, which can open below the line.
     */
    private fun inlineSpoiler(el: Element, sink: Sink, style: Style) {
        val hidden = Sink()
        children(el, hidden, style.copy(spoilers = style.spoilers + nextSpoilerId++))
        val parts = hidden.finish()
        val paragraph = parts.singleOrNull() as? Block.Paragraph
        when {
            parts.isEmpty() -> Unit
            paragraph != null -> paragraph.inlines.forEach { sink.inline(it) }
            else -> sink.inline(Inline.LabelledSpoiler(emptyList(), blocks(el, style)))
        }
    }

    private fun quote(el: Element, sink: Sink, style: Style) {
        val author = el.children().firstOrNull { it.hasClass("quoteable") }
        val content = el.children().firstOrNull { it.hasClass("quote-content") }
        val blocks = if (content != null) blocks(content, style) else {
            val rest = Sink()
            el.childNodes().filter { it != author }.forEach { walk(it, rest, style) }
            rest.finish()
        }
        sink.block(Block.Quote(author?.let { inlines(it, style) }.orEmpty(), blocks))
    }

    private fun list(el: Element, sink: Sink, style: Style) {
        val items = mutableListOf<Sink>()
        for (child in el.childNodes()) {
            if (child is Element && child.normalName() == "li") {
                items += Sink().also { children(child, it, style) }
            } else {
                //text before the first `<li>` (`[list]text[*]…`) stays above the list
                walk(child, items.lastOrNull() ?: sink, style)
            }
        }
        val parts = items.map { it.finish() }.filter { it.isNotEmpty() }
        if (parts.isNotEmpty()) sink.block(Block.BulletList(parts))
    }

    private fun heading(el: Element, sink: Sink, style: Style, level: Int) {
        val parts = blocks(el, styleOf(el, style))
        val paragraph = parts.singleOrNull() as? Block.Paragraph
        when {
            paragraph != null -> sink.block(Block.Heading(level, paragraph.inlines))
            //a heading div that holds images or blocks: show them as they are
            else -> parts.forEach { sink.block(it) }
        }
    }

    private fun aligned(el: Element, sink: Sink, style: Style, alignment: Alignment) {
        var parts = blocks(el, style)
        //`[center][center]…` nests up to 3 deep on the site; one is enough
        while (parts.size == 1 && (parts[0] as? Block.Aligned)?.alignment == alignment) parts = (parts[0] as Block.Aligned).blocks
        if (parts.isNotEmpty()) sink.block(Block.Aligned(alignment, parts))
    }

    private fun code(el: Element): Block.Code {
        val code = el.selectFirst("code") ?: el
        return Block.Code(code.wholeText().removeSuffix("\n"), el.attr("data-language").ifBlank { null })
    }

    private fun card(el: Element): Card {
        val id = el.id().toLongOrNull() ?: el.attr("data-track_user_rate").substringAfterLast(':').toLongOrNull()
        val type = CARD_TYPES.entries.firstOrNull { el.hasClass(it.key) }?.value
        val cover = el.selectFirst("a.cover")
        val target = if (type != null && id != null) LinkTarget.Screen(type, id) else cover?.attr("href")?.let { urlTarget(it) }
        val img = el.selectFirst("img")
        val title = el.selectFirst(".title")?.let { if (it.hasNameSpans()) names(it) else it.text() }
                ?: img?.attr("alt").orEmpty()
        val isCensored = img?.hasClass("is-moderation_censored") == true
        val poster = if (isCensored || img == null) null else (retina(img) ?: img.attr("src")).ifBlank { null }
        return Card(target, title.trim(), poster?.let { imageUrl(it, null) }, isCensored)
    }

    private fun more(el: Element): MoreLink? {
        val target = urlTarget(el.attr("href")) ?: return null
        val text = el.selectFirst(".inner")?.ownText()?.trim().orEmpty().ifEmpty { el.text().trim() }
        return MoreLink(text, target)
    }

    private fun ratings(el: Element): Block.Ratings? {
        val lines = el.select(".star-line").map {
            Rating(
                    title = it.selectFirst(".title")?.text()?.trim().orEmpty(),
                    score = it.selectFirst(".score-value")?.text()?.trim().orEmpty(),
                    notice = it.selectFirst(".score-notice")?.text()?.trim().orEmpty()
            )
        }
        return if (lines.isEmpty()) null else Block.Ratings(lines)
    }

    private fun poll(el: Element): Block.Poll? =
            (el.id().toLongOrNull() ?: el.attr("data-track_poll").toLongOrNull())?.let { Block.Poll(it, postUrl) }

    // Helpers

    private fun blocks(el: Element, style: Style): List<Block> = Sink().also { children(el, it, style) }.finish()

    /** An element that is one line of text (a label, a quote's author, the replies): its paragraphs joined. */
    private fun inlines(el: Element, style: Style): List<Inline> {
        val result = mutableListOf<Inline>()
        for (block in blocks(el, style)) {
            if (block !is Block.Paragraph) continue
            if (result.isNotEmpty()) result += Inline.LineBreak
            result += block.inlines
        }
        return result
    }

    private fun names(parent: Element): String {
        val en = parent.children().firstOrNull { it.hasClass("name-en") }?.text()?.trim()
        val ru = parent.children().firstOrNull { it.hasClass("name-ru") }?.text()?.trim()
        return if (en != null) pickName(en, ru) else ru.orEmpty()
    }

    /** Jsoup's own `isBlock` counts `<del>` and `<ins>` as blocks, so a deleted mention took a line of its own. */
    private fun Element.isBlockTag() = normalName() in BLOCK_TAGS

    private fun Element.isNameSpan() = normalName() == "span" && (hasClass("name-en") || hasClass("name-ru"))

    private fun Element.hasNameSpans() = children().any { it.isNameSpan() }

    /** What the site's CSS never shows: users' `[div=hidden]` (Q8), `[source]`. */
    private fun Element.isHidden() = hasClass("hidden") || hasClass("b-source")

    private fun headingLevel(el: Element): Int? = when (el.normalName()) {
        "h1", "h2" -> 1
        "h3" -> 2
        "h4" -> 3
        "h5" -> 4
        "h6" -> 5
        "div" -> when {
            el.hasClass("headline") -> 4
            el.hasClass("midheadline") -> 5
            el.hasClass("subheadline") -> 6
            else -> null
        }
        else -> null
    }

    private fun dataAttrs(el: Element): JsonObject? {
        val value = el.attr("data-attrs").trim()
        if (!value.startsWith("{")) return null
        return try {
            GSON.fromJson(value, JsonObject::class.java)
        } catch (e: RuntimeException) {
            null
        }
    }

    private fun JsonObject.long(key: String): Long? =
            get(key)?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asLong }.getOrNull() }

    private fun JsonObject.string(key: String): String? =
            get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private fun Element.intAttr(name: String): Int? = attr(name).trim().toIntOrNull()?.takeIf { it > 0 }

    /** The `2x` address of a `srcset`, for the small pictures a phone shows sharper than the site. */
    private fun retina(img: Element): String? =
            img.attr("srcset").split(',').map { it.trim() }.firstOrNull { it.endsWith(" 2x") }?.removeSuffix(" 2x")?.trim()

    private fun absolute(url: String): String = url.trim().appendHostIfNeed()

    /**
     * An image address to load: whole, over https (comments get `http://` from the server's `//` rewrite,
     * BB_CODES.md §8), and off the old static hosts, which are dead while the same path on shikimori.io works.
     * Such an image comes through the image proxy, and its [original] address is in the wrapper's data.
     */
    private fun imageUrl(src: String, original: String?): String {
        val originalUrl = original?.let { absolute(it) }
        if (originalUrl != null && OLD_STATIC_HOST.containsMatchIn(originalUrl)) return OLD_STATIC_HOST.replaceFirst(originalUrl, MAIN_HOST)
        val url = OLD_STATIC_HOST.replaceFirst(absolute(src), MAIN_HOST)
        return if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url
    }

    /** The text being gathered: finished blocks, and the paragraph still open. */
    private class Sink {
        private val blocks = mutableListOf<Block>()
        private val inlines = mutableListOf<Inline>()
        private val pending = StringBuilder()
        private var pendingStyle = Style.PLAIN
        private var hadBreak = false

        /** Text as HTML has it: runs of spaces and newlines are one space, and none at the start of a line. */
        fun text(raw: String, style: Style) {
            if (raw.isEmpty()) return
            var text = WHITESPACE.replace(raw, " ")
            if (text.startsWith(' ') && atSpace()) text = text.substring(1)
            if (text.isEmpty()) return
            if (pending.isNotEmpty() && pendingStyle != style) closeText()
            pendingStyle = style
            pending.append(text)
        }

        fun inline(inline: Inline) {
            when (inline) {
                is Inline.Text -> text(inline.text, inline.style)
                is Inline.LineBreak -> lineBreak()
                else -> {
                    closeText()
                    inlines += inline
                }
            }
        }

        fun lineBreak() {
            trimTrailingSpace()
            closeText()
            inlines += Inline.LineBreak
            hadBreak = true
        }

        /** The edge of a block element with nothing special about it: a new line, unless one has just begun. */
        fun boundary() {
            closeText()
            if (inlines.isNotEmpty() && inlines.last() != Inline.LineBreak) lineBreak()
        }

        fun block(block: Block) {
            val joins = paragraphIsBlank()
            flush()
            val last = blocks.lastOrNull()
            when {
                joins && block is Block.Cards && last is Block.Cards ->
                    blocks[blocks.lastIndex] = Block.Cards(last.cards + block.cards, block.more ?: last.more)
                else -> blocks += block
            }
        }

        /** Images with nothing but spaces between them stand in one row, as on the site. */
        fun image(image: Image) {
            val joins = paragraphIsBlank() && !hadBreak
            flush()
            val last = blocks.lastOrNull()
            if (joins && last is Block.Images) blocks[blocks.lastIndex] = Block.Images(last.images + image)
            else blocks += Block.Images(listOf(image))
        }

        fun finish(): List<Block> {
            flush()
            return blocks.toList()
        }

        private fun flush() {
            closeText()
            val trimmed = trim(inlines)
            if (trimmed.isNotEmpty()) blocks += Block.Paragraph(trimmed)
            inlines.clear()
            hadBreak = false
        }

        private fun paragraphIsBlank(): Boolean =
                pending.isBlank() && inlines.all { it == Inline.LineBreak || (it is Inline.Text && it.text.isBlank()) }

        private fun atSpace(): Boolean {
            if (pending.isNotEmpty()) return pending.last() == ' '
            return when (val last = inlines.lastOrNull()) {
                null, Inline.LineBreak -> true
                is Inline.Text -> last.text.endsWith(' ')
                else -> false
            }
        }

        private fun trimTrailingSpace() {
            if (pending.isNotEmpty() && pending.last() == ' ') pending.setLength(pending.length - 1)
        }

        private fun closeText() {
            if (pending.isEmpty()) return
            inlines += Inline.Text(pending.toString(), pendingStyle)
            pending.setLength(0)
        }

        /** No line breaks or spaces at the edges of a paragraph: the blocks around it keep their own gaps. */
        private fun trim(list: List<Inline>): List<Inline> {
            val result = list.toMutableList()
            while (true) {
                val first = result.firstOrNull() ?: break
                when {
                    first == Inline.LineBreak -> result.removeAt(0)
                    first is Inline.Text && first.text.trimStart(' ').isEmpty() -> result.removeAt(0)
                    first is Inline.Text && first.text.startsWith(' ') -> {
                        result[0] = first.copy(text = first.text.trimStart(' ')); break
                    }
                    else -> break
                }
            }
            while (true) {
                val last = result.lastOrNull() ?: break
                when {
                    last == Inline.LineBreak -> result.removeAt(result.lastIndex)
                    last is Inline.Text && last.text.trimEnd(' ').isEmpty() -> result.removeAt(result.lastIndex)
                    last is Inline.Text && last.text.endsWith(' ') -> {
                        result[result.lastIndex] = last.copy(text = last.text.trimEnd(' ')); break
                    }
                    else -> break
                }
            }
            return result
        }
    }

    private companion object {
        val GSON = Gson()

        //not U+00A0: `&nbsp;` stays a space that does not break or collapse
        val WHITESPACE = Regex("[ \\t\\n\\r\\f]+")
        val PIXELS = Regex("(\\d+)\\s*px")

        val BLOCK_TAGS = setOf(
                "div", "p", "center", "blockquote", "ul", "ol", "li", "pre", "hr", "h1", "h2", "h3", "h4", "h5", "h6",
                "table", "caption", "thead", "tbody", "tfoot", "tr", "td", "th", "article", "section", "header",
                "footer", "nav", "aside", "main", "figure", "figcaption", "dl", "dt", "dd", "address", "details",
                "summary", "form", "fieldset"
        )
        val SKIPPED_TAGS = setOf("meta", "link", "script", "style", "source", "noscript", "template", "head", "title")

        //users' `[div]` left as text ([withLeftoverDivs]), one match per part: 1 what is left alone (code; a tag
        //with attributes, whose values could hold the same words); 2 an opening tag (3 its classes, 4 its data
        //attributes) with the line break after it; else a closing tag with the line breaks around it, 5 the
        //closing tags right after it. The site's patterns (`bb_codes/tags/div_tag.rb`) over the api's HTML, where
        //a line break is `<br>`, or `<br class="br">` in a run of them
        val LEFTOVER_DIV = Regex(
                """(<pre\b[\s\S]*?</pre>|<code\b[\s\S]*?</code>|<(?!br(?: class="br")?>|/)[^>]*>)""" +
                        """|(\[div(?:=((?:[\w \-](?!data-\w))+))?((?: data-[\w\-]+(?:=[\w\-]+)?)+)?\])(?:<br(?: class="br")?>)?""" +
                        """|(?:<br(?: class="br")?>)?\[/div\]((?:[\[<]/(?!div)\w+[\]>])*)(?:<br(?: class="br")?>)?""",
                RegexOption.IGNORE_CASE)
        //the classes of the site's forbidden list (`bb_codes/cleanup_css_class.rb`) that this parser acts on: a
        //user's `[div=ban]` is not a ban note
        val FORBIDDEN_DIV_CLASSES = setOf("ban", "b-video")

        val SCREEN_TYPES = mapOf(
                "anime" to Type.ANIME, "manga" to Type.MANGA, "ranobe" to Type.RANOBE,
                "character" to Type.CHARACTER, "person" to Type.PERSON, "user" to Type.USER, "comment" to Type.COMMENT
        )
        val PATH_TYPES = mapOf(
                "animes" to Type.ANIME, "mangas" to Type.MANGA, "ranobe" to Type.RANOBE,
                "characters" to Type.CHARACTER, "people" to Type.PERSON
        )
        val CARD_TYPES = linkedMapOf(
                "c-anime" to Type.ANIME, "c-manga" to Type.MANGA, "c-ranobe" to Type.RANOBE,
                "c-character" to Type.CHARACTER, "c-person" to Type.PERSON
        )
        val VIDEO_NON_HOST_CLASSES = setOf("b-video", "unprocessed", "to-process")

        val SHIKIMORI_HOST = Regex("(www\\.)?shikimori\\.(io|one|me|org)")
        //a title's or a person's own page only (a letter before the id is a DMCA prefix); its sub-pages,
        //such as a critique, open in the browser
        val ENTITY_PATH = Regex("/(animes|mangas|ranobe|characters|people)/[a-z]?(\\d+)(-[^/]*)?/?")
        val FORUM_TOPIC_PATH = Regex("/forum/(?:[^/]+/)+(\\d+)(-[^/]*)?/?")
        val TOPIC_PATH = Regex("/topics/(\\d+)(-[^/]*)?/?")
        val COMMENT_PATH = Regex("/comments/(\\d+)/?")
        val STUDIO_PATH = Regex("/animes/studio/(\\d+)(-[^/]*)?/?")

        const val BROKEN_MAILTO = "http://mailto:"
        const val MAIN_HOST = "https://shikimori.io/"
        val OLD_STATIC_HOST = Regex("^https?://(desu|dere|moe|kawai|nyaa)\\.shikimori\\.(me|one|org)/(?=system/)")
    }
}
