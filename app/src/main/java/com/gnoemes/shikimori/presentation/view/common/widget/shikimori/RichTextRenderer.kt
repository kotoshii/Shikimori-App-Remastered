package com.gnoemes.shikimori.presentation.view.common.widget.shikimori

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.common.presentation.richtext.Block
import com.gnoemes.shikimori.entity.common.presentation.richtext.Image
import com.gnoemes.shikimori.entity.common.presentation.richtext.Inline
import com.gnoemes.shikimori.entity.common.presentation.richtext.LinkTarget
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichText
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextListener
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextState
import com.gnoemes.shikimori.entity.common.presentation.richtext.Style
import com.gnoemes.shikimori.entity.common.presentation.richtext.Video
import com.gnoemes.shikimori.utils.colorAttr
import com.gnoemes.shikimori.utils.dimenAttr
import com.gnoemes.shikimori.utils.dp
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout

/**
 * [RichText] → views, on the main thread, once per text: a text view for each paragraph (its formatting,
 * links, smileys and `||spoilers||` as spans), and a view for each block (quotes, spoilers, images). The
 * parser's model is in RichText.kt; how each element should look is in P8-T4's plan, §2.
 *
 * What is opened — `||spoilers||`, `[spoiler=label]` inside a sentence, block spoilers — is kept in [state],
 * so views built again from the same state show them as the user left them (a list that reuses its rows).
 * A spoiler's content is built when it is first opened, so a closed one costs nothing and its images load
 * only once seen.
 */
class RichTextRenderer(
        private val context: Context,
        private val imageLoader: ImageLoader?,
        private val listener: RichTextListener?,
        val state: RichTextState
) {

    private val text: RichText = state.text

    private val gallery: List<Image> by lazy { text.zoomableImages() }

    /** The space between two top-level parts of a text. */
    val blockGap = context.dp(6)
    private val spoilerColor = context.colorAttr(R.attr.colorTextSpoiler)
    private val blockBackground = context.colorAttr(R.attr.colorTextBlockBackground)
    private val quoteBarColor = context.colorAttr(R.attr.colorTextQuoteBar)
    private val codeColor = context.colorAttr(R.attr.colorTextCode)
    private val codeBackground = context.colorAttr(R.attr.colorTextCodeBackground)
    private val secondaryColor = context.colorAttr(R.attr.colorOnPrimarySecondary)
    private val openSpoilerTint = context.colorAttr(R.attr.colorTextSpoilerOpen)
    private val borderWidth = context.dimenAttr(R.attr.themeStrokeWidth)
    private val borderColor = context.colorAttr(R.attr.colorDivider)

    //the text's own size: smileys and avatars are drawn at about its height
    private val textSize: Float by lazy { TextView(context).textSize }
    private val smileySize: Int by lazy { (textSize * SMILEY_SCALE).toInt() }
    private val avatarSize: Int by lazy { (textSize * AVATAR_SCALE).toInt() }
    //the HTML puts the nickname right after the avatar; the site leaves about 4 px (`b-user16.sass`: the 16 px
    //avatar sits 20 px left of the name), and the user asked for a gap
    private val avatarSpace: Int by lazy { (textSize * AVATAR_SPACE_SCALE).toInt() }

    //what is opened: `||spoilers||` by the parser's numbers; `[spoiler=label]`s and spoiler blocks by identity
    private val revealed: MutableSet<Int> get() = state.revealed
    private val opened: MutableSet<Any> get() = state.opened

    fun render(into: LinearLayout) = addBlocks(text.blocks, into)

    /**
     * Builds only the top-level part at [index] into [into], for a list that shows a long text part by part
     * (the topic page's post, P8-T4 Q29); false when that part shows nothing. Every part built by one renderer
     * shares what spans them: the zoom gallery, what is opened.
     */
    fun renderBlock(index: Int, into: LinearLayout): Boolean {
        val view = blockView(text.blocks[index]) ?: return false
        add(into, view)
        return true
    }

    private fun addBlocks(blocks: List<Block>, into: LinearLayout) {
        for (block in blocks) blockView(block)?.let { add(into, it) }
    }

    private fun add(into: LinearLayout, view: View) {
        val params = view.layoutParams as? LinearLayout.LayoutParams ?: matchWidth()
        if (into.childCount > 0) params.topMargin = blockGap
        into.addView(view, params)
    }

    private fun blockView(block: Block): View? = when (block) {
        is Block.Paragraph -> paragraph(block.inlines)
        is Block.Quote -> quote(block)
        is Block.Spoiler -> spoiler(block)
        is Block.Images -> images(block.images)
        is Block.Replies -> replies(block)
        //P8-T4 stage 4 gives these their own look; until then their words are shown plainly
        is Block.Heading -> paragraph(block.inlines)
        is Block.Ban -> paragraph(block.inlines)
        is Block.Aligned -> column(block.blocks)
        is Block.BulletList -> column(block.items.flatten())
        is Block.Code -> paragraph(listOf(Inline.Text(block.text, Style(isCode = true))))
        is Block.VideoCard -> paragraph(videoLink(block.video))
        is Block.Wall -> column(listOfNotNull(block.images.takeIf { it.isNotEmpty() }?.let { Block.Images(it) }) +
                block.videos.map { Block.Paragraph(videoLink(it)) })
        is Block.Cards -> column(block.cards.filter { it.title.isNotBlank() }.map { link(it.title, it.target) } +
                listOfNotNull(block.more?.let { link(it.text, it.target) }))
        is Block.Ratings -> paragraph(block.lines.flatMapIndexed { index, rating ->
            listOfNotNull(Inline.LineBreak.takeIf { index > 0 },
                    Inline.Text(listOf(rating.title, rating.score).filter { it.isNotEmpty() }.joinToString(" — ") +
                            rating.notice.takeIf { it.isNotEmpty() }?.let { ", $it" }.orEmpty(), Style.PLAIN))
        })
        is Block.Divider, is Block.Poll -> null
    }

    // Blocks

    private fun column(blocks: List<Block>): View = column().also { addBlocks(blocks, it) }

    private fun column(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = matchWidth()
    }

    /** A quote: an author line when there is one, then its text, beside a bar. */
    private fun quote(quote: Block.Quote): View {
        val content = column().apply { setPadding(context.dp(10), context.dp(6), context.dp(8), context.dp(6)) }
        if (quote.author.isNotEmpty()) add(content, paragraph(quote.author))
        addBlocks(quote.blocks, content)
        return barBox(quoteBarColor, content)
    }

    /** A spoiler shown as a button with its label; a tap opens and closes the box below it. */
    private fun spoiler(spoiler: Block.Spoiler): View {
        val column = column()
        val button = AppCompatTextView(context).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(spoilerColor)
            setPadding(context.dp(10), context.dp(4), context.dp(10), context.dp(4))
            gravity = if (spoiler.isCentered) Gravity.CENTER else Gravity.START or Gravity.CENTER_VERTICAL
            if (spoiler.isFullWidth) setTypeface(typeface, Typeface.BOLD)
        }
        column.addView(button, LinearLayout.LayoutParams(if (spoiler.isFullWidth) MATCH else WRAP, WRAP))
        var box: View? = null
        fun show(open: Boolean) {
            val shown = box
            if (open && shown == null) {
                val content = column().apply { setPadding(context.dp(10), context.dp(6), context.dp(8), context.dp(6)) }
                addBlocks(spoiler.blocks, content)
                box = barBox(spoilerColor, content).also { column.addView(it) }
            } else shown?.visibility = if (open) View.VISIBLE else View.GONE
            button.text = spoilerButtonText(spoiler, open)
        }
        show(spoiler in opened)
        button.setOnClickListener {
            val open = spoiler !in opened
            if (open) opened.add(spoiler) else opened.remove(spoiler)
            show(open)
        }
        return column
    }

    private fun spoilerButtonText(spoiler: Block.Spoiler, open: Boolean): CharSequence =
            spans(spoiler.label.ifEmpty { defaultLabel() }, null, links = false)
                    .append(if (open) "  ▴" else "  ▾")

    /**
     * A box with a coloured bar on its left: quotes, open spoilers. On Amoled it is black like the screen,
     * so a 1 dp line in the divider colour sets it apart, as Amoled's sheets and menus (CODE_CONVENTIONS §7,
     * `themeStrokeWidth`: 0 on the other themes, where the box is lighter than the screen).
     */
    private fun barBox(barColor: Int, content: View): View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        background = GradientDrawable().apply {
            setColor(blockBackground)
            setStroke(borderWidth, borderColor)
        }
        setPadding(borderWidth, borderWidth, borderWidth, borderWidth)
        layoutParams = matchWidth()
        addView(View(context).apply { setBackgroundColor(barColor) }, LinearLayout.LayoutParams(context.dp(3), MATCH))
        addView(content, LinearLayout.LayoutParams(0, WRAP, 1f))
    }

    /** Images side by side while they fit, as on the site, else on the next line. */
    private fun images(images: List<Image>): View {
        if (images.size == 1) return imageView(images[0]).apply { layoutParams = LinearLayout.LayoutParams(WRAP, WRAP) }
        return FlexboxLayout(context).apply {
            flexWrap = FlexWrap.WRAP
            //each image keeps its own height; the default stretches them to the row's tallest
            alignItems = AlignItems.FLEX_START
            layoutParams = matchWidth()
            val gap = context.dp(4)
            images.forEach { addView(imageView(it), FlexboxLayout.LayoutParams(WRAP, WRAP).apply { setMargins(0, 0, gap, gap) }) }
        }
    }

    private fun imageView(image: Image): View = RichTextImageView(context, image, imageLoader).apply {
        val link = image.link
        when {
            //an image inside a link: the tap follows the link, as on the site
            link != null -> setOnClickListener { listener?.onLinkClicked(link) }
            image.zoomUrl != null -> setOnClickListener {
                val index = gallery.indexOfFirst { it === image }
                if (index >= 0) listener?.onImageClicked(gallery, index)
            }
        }
    }

    /** «Ответ:» or «Ответы:» and the comments that answer this one: small, grey, at the end, as on the site. */
    private fun replies(replies: Block.Replies): View =
            paragraph(listOf(Inline.Text(replies.label + " ", Style.PLAIN)) + replies.inlines) { view ->
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, REPLIES_TEXT_SP)
                view.setTextColor(secondaryColor)
                view.gravity = Gravity.END
            }

    private fun videoLink(video: Video): List<Inline> =
            listOf(Inline.Text(video.title ?: video.host, Style(link = LinkTarget.Web(video.url))))

    private fun link(text: String, target: LinkTarget?): Block =
            Block.Paragraph(listOf(Inline.Text(text, Style(link = target))))

    private fun defaultLabel(): List<Inline> = listOf(Inline.Text(context.getString(R.string.spoiler_label), Style.PLAIN))

    // Paragraphs

    private fun paragraph(inlines: List<Inline>, look: ((ShikimoriTextView) -> Unit)? = null): View =
            Paragraph(inlines, look).also { it.rebuild() }.root

    private fun textView(look: ((ShikimoriTextView) -> Unit)?): ShikimoriTextView = ShikimoriTextView(context).apply {
        imageLoader = this@RichTextRenderer.imageLoader
        layoutParams = matchWidth()
        look?.invoke(this)
    }

    /**
     * One paragraph, built again when something in it opens or closes. A `[spoiler=label]` that holds only
     * text opens in place; one that holds images or blocks splits the paragraph there and shows its content
     * in a box between the two halves (the blocks inside break the line on the site too). Either way a small
     * «спойлер ▴» mark stays where the label was, and closes it.
     */
    private inner class Paragraph(
            private val inlines: List<Inline>,
            private val look: ((ShikimoriTextView) -> Unit)?
    ) {
        private val hasLabelled = inlines.any { it is Inline.LabelledSpoiler }

        val root: View = if (hasLabelled) column() else textView(look)

        fun rebuild() {
            if (!hasLabelled) {
                (root as ShikimoriTextView).setRichText(spans(inlines, this))
                return
            }
            val column = root as LinearLayout
            column.removeAllViews()
            var run = mutableListOf<Inline>()
            fun closeRun() {
                val trimmed = run.dropWhile { it == Inline.LineBreak }.dropLastWhile { it == Inline.LineBreak }
                if (trimmed.isNotEmpty()) add(column, textView(look).also { it.setRichText(spans(trimmed, this)) })
                run = mutableListOf()
            }
            for (inline in flatten(inlines)) {
                run.add(inline)
                if (inline is Inline.LabelledSpoiler && inline in opened && !inline.holdsOnlyText()) {
                    //its mark ends the line above the box
                    closeRun()
                    val content = column().apply { setPadding(context.dp(10), context.dp(6), context.dp(8), context.dp(6)) }
                    addBlocks(inline.blocks, content)
                    add(column, barBox(spoilerColor, content))
                }
            }
            closeRun()
        }
    }

    /**
     * The open `[spoiler=label]`s that hold only text followed by their text, at any depth; each stays in
     * the list as its own mark ([spans]).
     */
    private fun flatten(inlines: List<Inline>): List<Inline> = inlines.flatMap { inline ->
        if (inline is Inline.LabelledSpoiler && inline in opened && inline.holdsOnlyText()) {
            listOf(inline) + flatten(inline.blocks.flatMapIndexed { index, block ->
                (if (index > 0) listOf(Inline.LineBreak) else emptyList()) + (block as Block.Paragraph).inlines
            })
        } else listOf(inline)
    }

    private fun Inline.LabelledSpoiler.holdsOnlyText(): Boolean = blocks.all { it is Block.Paragraph }

    // Text

    private fun spans(inlines: List<Inline>, paragraph: Paragraph?, links: Boolean = true): SpannableStringBuilder {
        val text = SpannableStringBuilder()
        for (inline in inlines) {
            when (inline) {
                is Inline.Text -> appendText(text, inline.text, inline.style, paragraph, links)
                is Inline.Smiley -> appendPicture(text, inline.url, smileySize, 0, inline.style, paragraph, links)
                is Inline.Avatar -> appendPicture(text, inline.url, avatarSize, avatarSpace, inline.style, paragraph, links)
                is Inline.LineBreak -> text.append('\n')
                is Inline.LabelledSpoiler ->
                    if (inline in opened) appendCloseMark(text, inline, paragraph) else appendLabel(text, inline, paragraph)
            }
        }
        return text
    }

    private fun appendText(text: SpannableStringBuilder, words: String, style: Style, paragraph: Paragraph?, links: Boolean) {
        val start = text.length
        text.append(words)
        val end = text.length
        fun set(span: Any) = text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        //the size and the weight first: a closed spoiler's bar is as wide as its words
        when {
            style.isBold && style.isItalic -> set(StyleSpan(Typeface.BOLD_ITALIC))
            style.isBold -> set(StyleSpan(Typeface.BOLD))
            style.isItalic -> set(StyleSpan(Typeface.ITALIC))
        }
        style.sizePx?.let { set(RelativeSizeSpan(it / SITE_TEXT_PX)) }
        if (style.isCode) set(TypefaceSpan("monospace"))

        val hiddenBy = style.spoilers.firstOrNull { it !in revealed }
        if (hiddenBy != null) {
            set(HiddenSpoilerSpan(spoilerColor) { reveal(hiddenBy, paragraph) })
            return
        }
        //open: a tap closes the innermost spoiler the words are in, as the site's click does
        style.spoilers.lastOrNull()?.let { id -> set(OpenSpoilerSpan(openSpoilerTint) { hide(id, paragraph) }) }
        if (style.isUnderline) set(UnderlineSpan())
        if (style.isStrike || style.isDeleted) set(StrikethroughSpan())
        if (style.isCode) {
            set(BackgroundColorSpan(codeBackground))
            set(ForegroundColorSpan(codeColor))
        }
        val color = when {
            style.isDeleted -> secondaryColor
            style.isCode -> null
            else -> style.color
        }
        color?.let { set(ForegroundColorSpan(it)) }
        val link = style.link
        if (links && link != null) set(LinkSpan(keepColor = color != null || style.isCode) { listener?.onLinkClicked(link) })
    }

    private fun appendPicture(text: SpannableStringBuilder, url: String, size: Int, spaceAfter: Int, style: Style, paragraph: Paragraph?, links: Boolean) {
        val start = text.length
        text.append(OBJECT)
        val end = text.length
        val hiddenBy = style.spoilers.firstOrNull { it !in revealed }
        text.setSpan(PictureSpan(url, size, hiddenBy != null, spoilerColor, spaceAfter), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val link = style.link
        val openIn = style.spoilers.lastOrNull()
        val tap = when {
            hiddenBy != null -> HiddenSpoilerSpan(spoilerColor) { reveal(hiddenBy, paragraph) }
            links && link != null -> LinkSpan(keepColor = true) { listener?.onLinkClicked(link) }
            openIn != null -> OpenSpoilerSpan(openSpoilerTint) { hide(openIn, paragraph) }
            else -> null
        }
        tap?.let { text.setSpan(it, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
    }

    /** Where an open `[spoiler=label]`'s label was: a small «спойлер ▴» that closes it. */
    private fun appendCloseMark(text: SpannableStringBuilder, spoiler: Inline.LabelledSpoiler, paragraph: Paragraph?) {
        val start = text.length
        text.append(context.getString(R.string.spoiler_label)).append(" ▴")
        val end = text.length
        text.setSpan(SpoilerLabelSpan {
            opened.remove(spoiler)
            paragraph?.rebuild()
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(RelativeSizeSpan(LABEL_PX / SITE_TEXT_PX), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.append(' ')
    }

    private fun appendLabel(text: SpannableStringBuilder, spoiler: Inline.LabelledSpoiler, paragraph: Paragraph?) {
        val start = text.length
        text.append(spans(spoiler.label.ifEmpty { defaultLabel() }, null, links = false))
        val end = text.length
        text.setSpan(SpoilerLabelSpan {
            opened.add(spoiler)
            paragraph?.rebuild()
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(RelativeSizeSpan(LABEL_PX / SITE_TEXT_PX), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun reveal(id: Int, paragraph: Paragraph?) {
        revealed.add(id)
        paragraph?.rebuild()
    }

    private fun hide(id: Int, paragraph: Paragraph?) {
        revealed.remove(id)
        paragraph?.rebuild()
    }

    private fun matchWidth() = LinearLayout.LayoutParams(MATCH, WRAP)

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

        const val OBJECT = "￼"

        //the site's text is 13 px (`--font-size-base-mobile`, globals.scss): `[size=N]` is N/13 of the app's
        const val SITE_TEXT_PX = 13f
        //a spoiler's label is 12 px on the site (`b-spoiler.sass`)
        const val LABEL_PX = 12f
        const val REPLIES_TEXT_SP = 12f

        //the site draws smileys 32 px high beside 13 px text; here a little over the line, to keep lines even
        const val SMILEY_SCALE = 1.6f
        const val AVATAR_SCALE = 1.2f
        const val AVATAR_SPACE_SCALE = 0.3f
    }
}
