package com.gnoemes.shikimori.presentation.view.common.widget.shikimori

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
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
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.common.presentation.richtext.Alignment
import com.gnoemes.shikimori.entity.common.presentation.richtext.Block
import com.gnoemes.shikimori.entity.common.presentation.richtext.Card
import com.gnoemes.shikimori.entity.common.presentation.richtext.Image
import com.gnoemes.shikimori.entity.common.presentation.richtext.Inline
import com.gnoemes.shikimori.entity.common.presentation.richtext.LinkTarget
import com.gnoemes.shikimori.entity.common.presentation.richtext.MoreLink
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichText
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextListener
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextState
import com.gnoemes.shikimori.entity.common.presentation.richtext.Style
import com.gnoemes.shikimori.entity.common.presentation.richtext.Video
import com.gnoemes.shikimori.utils.attr
import com.gnoemes.shikimori.utils.colorAttr
import com.gnoemes.shikimori.utils.dimenAttr
import com.gnoemes.shikimori.utils.dp
import com.gnoemes.shikimori.utils.drawable
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent

/**
 * [RichText] → views, on the main thread, once per text: a text view for each paragraph (its formatting,
 * links, smileys and `||spoilers||` as spans), and a view for each block (quotes, spoilers, images, lists,
 * code, video cards, poster cards…). The parser's model is in RichText.kt; how each element should look is in
 * P8-T4's plan, §2.
 *
 * What is opened — `||spoilers||`, `[spoiler=label]` inside a sentence, block spoilers — is kept in [state],
 * so views built again from the same state show them as the user left them (a list that reuses its rows).
 * A spoiler's content is built when it is first opened, so a closed one costs nothing and its images load
 * only once seen.
 *
 * [selectable]: whether the text can be selected. A text that cannot (a forum row's post) takes only the taps
 * on its links, spoilers and pictures; a tap on its plain words reaches the view under it.
 */
class RichTextRenderer(
        private val context: Context,
        private val imageLoader: ImageLoader?,
        private val listener: RichTextListener?,
        val state: RichTextState,
        private val selectable: Boolean = true
) {

    private val text: RichText = state.text

    private val gallery: List<Image> by lazy { text.zoomableImages() }

    /** How many top-level parts the text has: [renderBlock] builds them one by one. */
    val blockCount: Int get() = text.blocks.size

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
    private val banColor = ContextCompat.getColor(context, R.color.text_ban)

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

    /**
     * Builds the top-level part at [index] into [into], the parts one by one: the content view builds a cut
     * text only as far as its cut, and the topic page shows its post part by part, a row each (P8-T4 Q29).
     * False when that part shows nothing. Every part built by one renderer shares what spans them: the zoom
     * gallery, what is opened.
     */
    fun renderBlock(index: Int, into: LinearLayout): Boolean {
        val view = blockView(text.blocks[index], Gravity.START) ?: return false
        add(into, view, Gravity.START)
        return true
    }

    private fun addBlocks(blocks: List<Block>, into: LinearLayout, align: Int) {
        for (block in blocks) blockView(block, align)?.let { add(into, it, align) }
    }

    /** [align] places a view narrower than the column: an image, a video, a spoiler's button. */
    private fun add(into: LinearLayout, view: View, align: Int, gap: Int = blockGap) {
        val params = view.layoutParams as? LinearLayout.LayoutParams ?: matchWidth()
        if (into.childCount > 0) params.topMargin = gap
        params.gravity = align
        into.addView(view, params)
    }

    /** [align]: `Gravity.START`, `CENTER_HORIZONTAL` or `END` — `[center]` and `[right]` reach everything inside. */
    private fun blockView(block: Block, align: Int): View? = when (block) {
        is Block.Paragraph -> paragraph(block.inlines, align)
        is Block.Heading -> heading(block, align)
        is Block.Quote -> quote(block, align)
        is Block.Spoiler -> spoiler(block, align)
        is Block.Aligned -> column(block.blocks, gravityOf(block.alignment))
        is Block.BulletList -> list(block, align)
        is Block.Code -> code(block)
        is Block.Divider -> divider()
        is Block.Images -> images(block.images, align)
        is Block.VideoCard -> videoCard(block.video)
        is Block.Wall -> wall(block, align)
        is Block.Cards -> cards(block)
        is Block.Ratings -> ratings(block)
        is Block.Ban -> ban(block)
        is Block.Replies -> replies(block)
        is Block.Poll -> poll(block, align)
    }

    private fun gravityOf(alignment: Alignment): Int = when (alignment) {
        Alignment.CENTER -> Gravity.CENTER_HORIZONTAL
        Alignment.END -> Gravity.END
    }

    // Blocks

    private fun column(blocks: List<Block>, align: Int): View = column().also { addBlocks(blocks, it, align) }

    private fun column(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = matchWidth()
    }

    private fun boxContent(): LinearLayout = column().apply { setPadding(context.dp(10), context.dp(6), context.dp(8), context.dp(6)) }

    /** `#`…`#####`: bigger bold text, at the site's sizes (`application.sass`, `headlines.sass`). */
    private fun heading(heading: Block.Heading, align: Int): View = paragraph(heading.inlines, align) { view ->
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize * headingPx(heading.level) / SITE_TEXT_PX)
        view.setTypeface(view.typeface, Typeface.BOLD)
    }

    private fun headingPx(level: Int): Float = when (level) {
        1 -> 25f
        2 -> 21f
        3, 4 -> 18f
        else -> 15f
    }

    /** A quote: an author line when there is one, then its text, beside a bar. */
    private fun quote(quote: Block.Quote, align: Int): View {
        val content = boxContent()
        if (quote.author.isNotEmpty()) add(content, paragraph(quote.author, Gravity.START), Gravity.START)
        addBlocks(quote.blocks, content, align)
        return barBox(quoteBarColor, content)
    }

    /** A spoiler shown as a button with its label; a tap opens and closes the box below it. */
    private fun spoiler(spoiler: Block.Spoiler, align: Int): View {
        val column = column()
        val button = AppCompatTextView(context).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(spoilerColor)
            setPadding(context.dp(10), context.dp(4), context.dp(10), context.dp(4))
            gravity = if (spoiler.isCentered) Gravity.CENTER else Gravity.START or Gravity.CENTER_VERTICAL
            if (spoiler.isFullWidth) setTypeface(typeface, Typeface.BOLD)
        }
        column.addView(button, LinearLayout.LayoutParams(if (spoiler.isFullWidth) MATCH else WRAP, WRAP).apply { gravity = align })
        var box: View? = null
        fun show(open: Boolean) {
            val shown = box
            if (open && shown == null) {
                val content = boxContent()
                addBlocks(spoiler.blocks, content, align)
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
     * A box with a coloured bar on its left: quotes, open spoilers, ban notes. On Amoled it is black like the
     * screen, so a 1 dp line in the divider colour sets it apart, as Amoled's sheets and menus
     * (CODE_CONVENTIONS §7, `themeStrokeWidth`: 0 on the other themes, where the box is lighter than the screen).
     */
    private fun barBox(barColor: Int, content: View, background: Int = blockBackground): View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        this.background = GradientDrawable().apply {
            setColor(background)
            setStroke(borderWidth, borderColor)
        }
        setPadding(borderWidth, borderWidth, borderWidth, borderWidth)
        layoutParams = matchWidth()
        addView(View(context).apply { setBackgroundColor(barColor) }, LinearLayout.LayoutParams(context.dp(3), MATCH))
        addView(content, LinearLayout.LayoutParams(0, WRAP, 1f))
    }

    /**
     * `- `, `[list]`: a bullet, and the item's text beside it with a hanging indent, as the site's `b-list`
     * (a 5 px disc 12 px left of the text). An item can hold a nested list, which indents again.
     */
    private fun list(list: Block.BulletList, align: Int): View {
        val column = column()
        for (item in list.items) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = matchWidth()
            }
            val bullet = AppCompatTextView(context).apply {
                text = BULLET
                gravity = Gravity.CENTER_HORIZONTAL
            }
            row.addView(bullet, LinearLayout.LayoutParams(context.dp(BULLET_WIDTH_DP), WRAP))
            row.addView(column(item, align), LinearLayout.LayoutParams(0, WRAP, 1f))
            add(column, row, Gravity.START, gap = context.dp(LIST_ITEM_GAP_DP))
        }
        return column
    }

    /**
     * A code block: its lines as written, in monospace, scrolling sideways when a line is long, light on dark as
     * on the site (`b-code-v2.sass`, `#263238`) on every theme. The site colours no syntax, nor does the app.
     */
    private fun code(code: Block.Code): View = HorizontalScrollView(context).apply {
        setBackgroundColor(ContextCompat.getColor(context, R.color.text_code_block_background))
        setPadding(context.dp(8), context.dp(6), context.dp(8), context.dp(6))
        isHorizontalScrollBarEnabled = false
        layoutParams = matchWidth()
        addView(AppCompatTextView(context).apply {
            text = code.text
            typeface = Typeface.MONOSPACE
            setTextColor(ContextCompat.getColor(context, R.color.text_code_block))
            setTextIsSelectable(selectable)
        }, FrameLayout.LayoutParams(WRAP, WRAP))
    }

    /** `[hr]`: a line in the divider colour. */
    private fun divider(): View = View(context).apply {
        setBackgroundColor(borderColor)
        layoutParams = LinearLayout.LayoutParams(MATCH, context.dp(1)).apply { bottomMargin = context.dp(2) }
    }

    /** Images side by side while they fit, as on the site, else on the next line. */
    private fun images(images: List<Image>, align: Int): View {
        if (images.size == 1) return imageView(images[0]).apply { layoutParams = LinearLayout.LayoutParams(WRAP, WRAP) }
        return flexRow(images.map { imageView(it) }, align)
    }

    /** Views in a row while they fit, else on the next line; each keeps its own size. */
    private fun flexRow(views: List<View>, align: Int): View = FlexboxLayout(context).apply {
        flexWrap = FlexWrap.WRAP
        //each image keeps its own height; the default stretches them to the row's tallest
        alignItems = AlignItems.FLEX_START
        justifyContent = when (align) {
            Gravity.CENTER_HORIZONTAL -> JustifyContent.CENTER
            Gravity.END -> JustifyContent.FLEX_END
            else -> JustifyContent.FLEX_START
        }
        layoutParams = matchWidth()
        val gap = context.dp(4)
        views.forEach { addView(it, FlexboxLayout.LayoutParams(WRAP, WRAP).apply { setMargins(0, 0, gap, gap) }) }
    }

    private fun imageView(image: Image): View = RichTextImageView(context, image, imageLoader).apply {
        val link = image.link
        when {
            //an image inside a link: the tap follows the link, as on the site
            link != null -> onTap { listener?.onLinkClicked(link) }
            image.zoomUrl != null -> onTap {
                val index = gallery.indexOfFirst { it === image }
                if (index >= 0) listener?.onImageClicked(gallery, index)
            }
        }
    }

    /**
     * A tappable image or card: its action, and the ripple the app's cards show on a tap (P8-T4 Q43: in a text with
     * many of them, the one tapped is seen).
     */
    private fun View.onTap(action: () -> Unit) {
        setOnClickListener { action() }
        foreground = context.drawable(context.attr(R.attr.selectableItemBackground).resourceId)
    }

    /**
     * A video: its thumbnail in the site's media box (235×132 px, `$media_width`/`$media_height`), a play mark,
     * and the host's name in the corner as the site's marker (`media_marker.sass`). A tap opens the video's
     * page outside the app — the YouTube app, or the browser (P8-T1's decision).
     */
    private fun videoCard(video: Video): View {
        val card = FrameLayout(context).apply { layoutParams = LinearLayout.LayoutParams(WRAP, WRAP) }
        val thumbnail = Image(video.thumbnail.orEmpty(), null, MEDIA_WIDTH, MEDIA_HEIGHT, null, null, false, null)
        card.addView(RichTextImageView(context, thumbnail, imageLoader).apply { scaleType = ImageView.ScaleType.CENTER_CROP },
                FrameLayout.LayoutParams(WRAP, WRAP))
        card.addView(AppCompatImageView(context).apply {
            setImageResource(R.drawable.ic_play)
            setColorFilter(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(MARKER_BACKGROUND)
            }
        }, FrameLayout.LayoutParams(context.dp(PLAY_SIZE_DP), context.dp(PLAY_SIZE_DP), Gravity.CENTER))
        card.addView(AppCompatTextView(context).apply {
            text = listOfNotNull(video.title, video.host.takeIf { it.isNotBlank() }).joinToString(" · ")
            setTextColor(MARKER_TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, MARKER_TEXT_SP)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            //within the thumbnail: a long title must not make the card wider
            maxWidth = context.dp(MEDIA_WIDTH - 8)
            setPadding(context.dp(8), 0, context.dp(8), 0)
            background = GradientDrawable().apply {
                cornerRadius = context.dp(2).toFloat()
                setColor(MARKER_BACKGROUND)
            }
        }, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.END).apply {
            setMargins(context.dp(4), context.dp(4), context.dp(4), context.dp(4))
        })
        card.onTap { listener?.onLinkClicked(LinkTarget.Web(video.url)) }
        return card
    }

    /** `[wall]` and a news topic's `html_footer`: its videos and images side by side while they fit. */
    private fun wall(wall: Block.Wall, align: Int): View =
            flexRow(wall.videos.map { videoCard(it) } + wall.images.map { imageView(it) }, align)

    /** Poster cards of titles, in as many equal columns as fit; the «ещё +N» tile of a shortened list last. */
    private fun cards(cards: Block.Cards): View = PosterGridLayout(context).apply {
        layoutParams = matchWidth()
        cards.cards.forEach { addView(card(it)) }
        cards.more?.let { addView(moreTile(it)) }
    }

    /** A poster and the title under it; a tap opens the title. The site hides the poster of a title it censors. */
    private fun card(card: Card): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val poster = Image(card.poster.orEmpty(), null, POSTER_WIDTH, POSTER_HEIGHT, null, null, false, null)
        addView(RichTextImageView(context, poster, imageLoader).apply { scaleType = ImageView.ScaleType.CENTER_CROP },
                LinearLayout.LayoutParams(MATCH, WRAP))
        addView(AppCompatTextView(context).apply {
            text = card.title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, CARD_TITLE_SP)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, context.dp(4), 0, 0)
        }, LinearLayout.LayoutParams(MATCH, WRAP))
        val target = card.target
        if (target != null) onTap { listener?.onLinkClicked(target) }
    }

    /** The «ещё +N» tile at the end of a shortened list: the size of a poster; a tap opens the whole list. */
    private fun moreTile(more: MoreLink): View = PosterGridLayout.PosterSizedFrame(context, POSTER_WIDTH, POSTER_HEIGHT).apply {
        setBackgroundColor(blockBackground)
        addView(AppCompatTextView(context).apply {
            text = more.text
            gravity = Gravity.CENTER
            //in the colour of the links in the text
            setTextColor(linkTextColors)
        }, FrameLayout.LayoutParams(MATCH, MATCH))
        onTap { listener?.onLinkClicked(more.target) }
    }

    /** A critique's ratings, a line each: «Сюжет — 10 · Эпик вин!», the score in bold. */
    private fun ratings(ratings: Block.Ratings): View = paragraph(ratings.lines.flatMapIndexed { index, rating ->
        listOfNotNull(
                Inline.LineBreak.takeIf { index > 0 },
                Inline.Text(rating.title + " — ", Style(color = secondaryColor)).takeIf { rating.title.isNotEmpty() },
                Inline.Text(rating.score, Style(isBold = true)).takeIf { rating.score.isNotEmpty() },
                Inline.Text(" · " + rating.notice, Style.PLAIN).takeIf { rating.notice.isNotEmpty() }
        )
    }, Gravity.START)

    /**
     * A moderator's ban under a comment: the moderator, then the reason, in a box with a red bar and a faint
     * red tint (the site writes the reason in red, `forum_entry.sass`, which is hard to read on the dark themes).
     */
    private fun ban(ban: Block.Ban): View = barBox(banColor, boxContent().also { add(it, paragraph(ban.inlines, Gravity.START), Gravity.START) },
            background = ColorUtils.compositeColors(ColorUtils.setAlphaComponent(banColor, BAN_TINT_ALPHA), blockBackground))

    /** «Ответ:» or «Ответы:» and the comments that answer this one: small, grey, at the end, as on the site. */
    private fun replies(replies: Block.Replies): View =
            paragraph(listOf(Inline.Text(replies.label + " ", Style.PLAIN)) + replies.inlines, Gravity.END) { view ->
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, REPLIES_TEXT_SP)
                view.setTextColor(secondaryColor)
            }

    /** `[poll]`: the app cannot show a poll, so a link to the post that holds it opens the site (Q9). */
    private fun poll(poll: Block.Poll, align: Int): View? {
        val url = poll.postUrl ?: return null
        return paragraph(listOf(Inline.Text(context.getString(R.string.text_poll_link), Style(link = LinkTarget.Web(url)))), align)
    }

    private fun defaultLabel(): List<Inline> = listOf(Inline.Text(context.getString(R.string.spoiler_label), Style.PLAIN))

    // Paragraphs

    private fun paragraph(inlines: List<Inline>, align: Int, look: ((ShikimoriTextView) -> Unit)? = null): View =
            Paragraph(inlines, align, look).also { it.rebuild() }.root

    private fun textView(align: Int, look: ((ShikimoriTextView) -> Unit)?): ShikimoriTextView = ShikimoriTextView(context).apply {
        if (!selectable) passPlainTaps()
        imageLoader = this@RichTextRenderer.imageLoader
        layoutParams = matchWidth()
        gravity = align
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
            private val align: Int,
            private val look: ((ShikimoriTextView) -> Unit)?
    ) {
        private val hasLabelled = inlines.any { it is Inline.LabelledSpoiler }

        val root: View = if (hasLabelled) column() else textView(align, look)

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
                if (trimmed.isNotEmpty()) add(column, textView(align, look).also { it.setRichText(spans(trimmed, this)) }, align)
                run = mutableListOf()
            }
            for (inline in flatten(inlines)) {
                run.add(inline)
                if (inline is Inline.LabelledSpoiler && inline in opened && !inline.holdsOnlyText()) {
                    //its mark ends the line above the box
                    closeRun()
                    val content = boxContent()
                    addBlocks(inline.blocks, content, align)
                    add(column, barBox(spoilerColor, content), align)
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
        styleLabel(text, start, text.length) {
            opened.remove(spoiler)
            paragraph?.rebuild()
        }
        text.append(' ')
    }

    private fun appendLabel(text: SpannableStringBuilder, spoiler: Inline.LabelledSpoiler, paragraph: Paragraph?) {
        val start = text.length
        text.append(spans(spoiler.label.ifEmpty { defaultLabel() }, null, links = false))
        styleLabel(text, start, text.length) {
            opened.add(spoiler)
            paragraph?.rebuild()
        }
    }

    /**
     * A `[spoiler=label]`'s label or its close mark, as the site draws a label (`b-spoiler.sass`): 12 px, a little
     * spaced, in the link colour, with a dashed line under it that the text view draws ([SpoilerLabelSpan]).
     */
    private fun styleLabel(text: SpannableStringBuilder, start: Int, end: Int, action: () -> Unit) {
        val scale = LABEL_PX / SITE_TEXT_PX
        text.setSpan(SpoilerLabelSpan(scale, action), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(RelativeSizeSpan(scale), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(LetterSpacingSpan(LABEL_SPACING_PX / LABEL_PX), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
        const val BULLET = "•"

        //the site's text is 13 px (`--font-size-base-mobile`, globals.scss): `[size=N]` is N/13 of the app's
        const val SITE_TEXT_PX = 13f
        //a spoiler's label is 12 px on the site, 0.5 px between its letters (`b-spoiler.sass`)
        const val LABEL_PX = 12f
        const val LABEL_SPACING_PX = 0.5f
        const val REPLIES_TEXT_SP = 12f

        //the site draws smileys 32 px high beside 13 px text; here a little over the line, to keep lines even
        const val SMILEY_SCALE = 1.6f
        const val AVATAR_SCALE = 1.2f
        const val AVATAR_SPACE_SCALE = 0.3f

        //`b-list.sass`: the text 17 px in, the disc 12 px left of it
        const val BULLET_WIDTH_DP = 14
        const val LIST_ITEM_GAP_DP = 2

        //the site's media box (`$media_width`, `$media_height`) and its marker (`media_marker.sass`)
        const val MEDIA_WIDTH = 235
        const val MEDIA_HEIGHT = 132
        const val MARKER_TEXT_SP = 11f
        const val MARKER_TEXT = 0xE6FFFFFF.toInt()
        const val MARKER_BACKGROUND = 0xBF000000.toInt()
        const val PLAY_SIZE_DP = 40

        //a shikimori poster's proportions (its 225×320 preview); a card is never wider than that
        const val POSTER_WIDTH = 225
        const val POSTER_HEIGHT = 320
        const val CARD_TITLE_SP = 12f

        const val BAN_TINT_ALPHA = 0x26
    }
}
