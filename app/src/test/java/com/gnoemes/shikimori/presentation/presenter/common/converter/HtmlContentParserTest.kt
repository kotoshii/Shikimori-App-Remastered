package com.gnoemes.shikimori.presentation.presenter.common.converter

import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.richtext.Alignment
import com.gnoemes.shikimori.entity.common.presentation.richtext.Block
import com.gnoemes.shikimori.entity.common.presentation.richtext.Inline
import com.gnoemes.shikimori.entity.common.presentation.richtext.LinkTarget
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichText
import com.gnoemes.shikimori.entity.common.presentation.richtext.Style
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The parser on real api answers: `src/test/resources/richtext/` holds the HTML field of each text in the
 * P8-T4 test set (DOCS roadmap/tasks/phase-8/P8-T4/P8-T4-research/3-test-texts.md, fetched 2026-11-08 with
 * DOCS knowledge/tools/bb_html_features.py; `comment_<id>` is `/api/comments/<id>` → `html_body`,
 * `topic_<id>_body`/`_footer` is `/api/topics/<id>`, `<kind>_<id>_description` its `description_html`).
 * `preview_s1` is that file's synthetic text S1, rendered by the site's preview call, for what no live text
 * has. Short HTML written here comes from the markup in BB_CODES.md §8.
 */
class HtmlContentParserTest {

    private fun parse(html: String?, russianNames: Boolean = true): RichText =
            HtmlContentReader { name, nameRu -> if (russianNames) nameRu?.takeIf { it.isNotEmpty() } ?: name else name }.read(html)

    private fun fixture(name: String, russianNames: Boolean = true): RichText {
        val stream = javaClass.getResourceAsStream("/richtext/$name.html") ?: error("no test file $name")
        return parse(stream.bufferedReader(Charsets.UTF_8).readText(), russianNames)
    }

    // Walking the result

    private fun blocks(list: List<Block>): List<Block> = list.flatMap { block ->
        listOf(block) + when (block) {
            is Block.Quote -> blocks(block.blocks)
            is Block.Spoiler -> blocks(block.blocks)
            is Block.Aligned -> blocks(block.blocks)
            is Block.BulletList -> blocks(block.items.flatten())
            is Block.Paragraph -> blocks(block.inlines.filterIsInstance<Inline.LabelledSpoiler>().flatMap { it.blocks })
            else -> emptyList()
        }
    }

    private fun RichText.allBlocks() = blocks(blocks)

    private fun Block.inlines(): List<Inline> = when (this) {
        is Block.Paragraph -> inlines
        is Block.Heading -> inlines
        is Block.Quote -> author
        is Block.Spoiler -> label
        is Block.Ban -> inlines
        is Block.Replies -> inlines
        else -> emptyList()
    }

    private fun RichText.allInlines(): List<Inline> = allBlocks().flatMap { it.inlines() }.flatMap {
        if (it is Inline.LabelledSpoiler) listOf(it) + it.label else listOf(it)
    }

    private fun RichText.texts() = allInlines().filterIsInstance<Inline.Text>()

    private fun RichText.textWith(words: String) = texts().first { it.text.contains(words) }

    private fun List<Inline>.plain() = joinToString("") {
        when (it) {
            is Inline.Text -> it.text
            is Inline.LineBreak -> "\n"
            else -> ""
        }
    }

    private inline fun <reified T : Block> RichText.all(): List<T> = allBlocks().filterIsInstance<T>()

    // Empty texts

    @Test
    fun `no html and an empty description are empty`() {
        assertTrue(parse(null).isEmpty)
        assertTrue(parse("").isEmpty)
        //what the api sends for a title or character with no description (live 2026-11-08)
        assertTrue(parse("<div class=\"b-text_with_paragraphs\"></div>").isEmpty)
    }

    @Test
    fun `the same html gives an equal result, so a list skips an unchanged row`() {
        assertEquals(fixture("comment_14159216"), fixture("comment_14159216"))
    }

    // Text, whitespace, formatting

    @Test
    fun `whitespace collapses as in a browser, but a no-break space stays`() {
        val text = parse("a   b\n c&nbsp;&nbsp;d")
        assertEquals(listOf(Block.Paragraph(listOf(Inline.Text("a b c  d", Style.PLAIN)))), text.blocks)
    }

    @Test
    fun `escaped characters and markdown the site leaves alone stay literal`() {
        val text = fixture("preview_s1").texts().joinToString("") { it.text }
        assertTrue(text.contains("A & B, \"кавычки\", <тег>"))
        assertTrue(text.contains("$100"))
        assertTrue(text.contains("**жирный** __двойное подчёркивание__"))
        assertTrue(text.contains("---"))
        assertTrue(text.contains("1. нумерованный"))
    }

    @Test
    fun `bold, italic, underline, strike and inline code`() {
        val text = parse("<strong>b</strong> <em>i</em> <u>u</u> <del>s</del> <code class=\"b-code_inline\">c</code> <s>@</s>")
        assertTrue(text.textWith("b").style.isBold)
        assertTrue(text.textWith("i").style.isItalic)
        assertTrue(text.textWith("u").style.isUnderline)
        assertTrue(text.textWith("s").style.isStrike)
        assertTrue(text.textWith("c").style.isCode)
        //`<s>` is only a mention's @, never strikethrough
        assertFalse(text.textWith("@").style.isStrike)
    }

    @Test
    fun `colours in short and long hex and by name, and sizes`() {
        val s1 = fixture("preview_s1")
        assertEquals(0xFFFF0000.toInt(), s1.textWith("короткий hex").style.color)
        assertEquals(0xFFFF0000.toInt(), s1.textWith("имя цвета").style.color)
        assertEquals(0xFFFF4136.toInt(), s1.textWith("длинный hex").style.color)
        assertEquals(35, s1.textWith("размер 50").style.sizePx)

        val music = fixture("topic_581152_body").textWith("Кидайте сюда музыку")
        assertEquals(0xFFBF7237.toInt(), music.style.color)
        assertEquals(18, music.style.sizePx)
    }

    @Test
    fun `line breaks stay, but not at the edges of a paragraph`() {
        val news = fixture("topic_649866_body").blocks.first() as Block.Paragraph
        //`br.br br.br` between paragraphs: two breaks, one empty line, as on the site
        assertTrue(news.inlines.plain().contains("».\n\nТрансляция"))
        assertFalse(news.inlines.first() is Inline.LineBreak)
        assertFalse(news.inlines.last() is Inline.LineBreak)
        //a comment's `<br class="br"><br class="br"><br>` before the ban leaves no empty paragraph
        assertTrue(fixture("comment_14159216").all<Block.Paragraph>().none { it.inlines.isEmpty() })
    }

    @Test
    fun `a block inside an inline element keeps the inline style, and the text around it too`() {
        val text = parse("<span style=\"font-size: 16px;\">a<center>b</center>c</span>")
        assertEquals(3, text.blocks.size)
        assertEquals(16, (text.blocks[0] as Block.Paragraph).inlines.filterIsInstance<Inline.Text>().single().style.sizePx)
        val centered = text.blocks[1] as Block.Aligned
        assertEquals(Alignment.CENTER, centered.alignment)
        assertEquals(16, (centered.blocks.single() as Block.Paragraph).inlines.filterIsInstance<Inline.Text>().single().style.sizePx)
        assertEquals(16, (text.blocks[2] as Block.Paragraph).inlines.filterIsInstance<Inline.Text>().single().style.sizePx)
    }

    @Test
    fun `single-quoted attributes of the site's own templates are read`() {
        //ContestStatusTopic's `div.b-entry-info` writes `class='line-container'`
        val text = fixture("topic_648263_body")
        assertTrue(text.texts().any { it.text.contains("Название:") })
        assertTrue(text.texts().any { it.text.contains("04.10.2026 – 19.10.2026") })
    }

    // Headings, lists, code, layout

    @Test
    fun `markdown headings and users' headline blocks`() {
        val headings = fixture("preview_s1").all<Block.Heading>()
        assertEquals(listOf(1, 2, 3, 4, 5), headings.map { it.level })
        assertEquals("Заголовок h2", headings[0].inlines.plain())
        //`[div=headline m20]`
        assertEquals("Комменты", fixture("topic_642713_body").all<Block.Heading>().first().inlines.plain())
    }

    @Test
    fun `lists, one per marker kind, and the text before one stays above it`() {
        assertEquals(3, fixture("preview_s1").all<Block.BulletList>().size)
        val news = fixture("topic_643368_body")
        val list = news.all<Block.BulletList>().single()
        assertEquals(2, list.items.size)
        val before = news.blocks[news.blocks.indexOf(list) - 1] as Block.Paragraph
        assertEquals("Главных героев озвучили популярные актеры игровой индустрии и аниме:", before.inlines.plain())
    }

    @Test
    fun `a code block keeps its lines and its language`() {
        val code = fixture("topic_621224_body").all<Block.Code>().first()
        assertEquals("css", code.language)
        assertEquals("body{\n  background: url(ссылка на картинку)\n}", code.text)
    }

    @Test
    fun `centred and right-aligned blocks, and a divider`() {
        assertTrue(fixture("topic_642713_body").all<Block.Aligned>().all { it.alignment == Alignment.CENTER })
        assertTrue(fixture("topic_218886_body").all<Block.Aligned>().any { it.alignment == Alignment.END })
        assertEquals(1, fixture("comment_13208946").all<Block.Divider>().size)
    }

    @Test
    fun `hidden blocks are not shown`() {
        val text = parse("a<div class=\"hidden\" data-div=\"\">secret</div><div class=\"b-source hidden\"><span class=\"linkeable\" data-href=\"https://example.com/src\">example.com</span></div>b")
        //a box the CSS does not draw does not break the line either
        assertEquals("ab", text.allInlines().plain())
    }

    // Links

    @Test
    fun `a description's entity link opens the character, with the author's own text`() {
        val tatsuya = fixture("anime_48375_description").textWith("Тацуи")
        assertEquals("Тацуи", tatsuya.text)
        assertEquals(LinkTarget.Screen(Type.CHARACTER, 55743), tatsuya.style.link)
    }

    @Test
    fun `a bare entity link shows the one name the setting picks`() {
        val meruem = LinkTarget.Screen(Type.CHARACTER, 23277)
        assertEquals("Меруэм", fixture("comment_14161975").texts().first { it.style.link == meruem }.text)
        assertEquals("Meruem", fixture("comment_14161975", russianNames = false).texts().first { it.style.link == meruem }.text)
    }

    @Test
    fun `links with no ids are read from the address`() {
        //`[url]` to an entity page gives a relative link with no data-attrs
        assertEquals(LinkTarget.Screen(Type.CHARACTER, 101635),
                fixture("comment_14159016").textWith("/characters/101635-bad-end-happy").style.link)
        //ContestStatusTopic's winners have no data-attrs either
        assertEquals(LinkTarget.Screen(Type.ANIME, 57555), fixture("topic_648263_body").textWith("Резе").style.link)
        //a DMCA letter before the id
        assertEquals(LinkTarget.Screen(Type.ANIME, 16201),
                parse("<a class=\"b-link\" href=\"https://shikimori.io/animes/z16201-aku-no-hana\">x</a>").textWith("x").style.link)
        //a forum topic
        assertEquals(LinkTarget.Screen(Type.TOPIC, 588641), fixture("comment_14165453").textWith("правил сайта").style.link)
    }

    @Test
    fun `other site pages and outside links open the browser`() {
        assertEquals(LinkTarget.Web("https://shikimori.io/contests/110-luchshiy-anime-film-2017-goda"),
                fixture("comment_14131573").textWith("/contests/110").style.link)
        assertEquals(LinkTarget.Web("https://shikimori.io/animes/studio/56"), fixture("topic_649866_body").textWith("A-1 Pictures").style.link)
        assertEquals(LinkTarget.Web("https://kujico.jp/detail/158"), fixture("comment_14156778").textWith("kujico.jp").style.link)
        //a critique is a sub-page of a title, not the title
        assertEquals(LinkTarget.Web("https://shikimori.io/animes/z16201-aku-no-hana/critiques/2382"),
                parse("<a class=\"b-link\" href=\"https://shikimori.io/animes/z16201-aku-no-hana/critiques/2382\">cr</a>").textWith("cr").style.link)
        //the site renders `[url=mailto:…]` as `http://mailto:…`
        assertEquals(LinkTarget.Web("mailto:a@b.c"),
                parse("<a class=\"b-link\" href=\"http://mailto:a@b.c\" rel=\"noopener noreferrer nofollow\">mail</a>").textWith("mail").style.link)
    }

    @Test
    fun `a deleted target is crossed out and opens nothing`() {
        val entity = parse("a <span class=\"b-entry-404\"><del>[anime=999999999]</del></span>").textWith("[anime=999999999]")
        assertTrue(entity.style.isDeleted)
        assertNull(entity.style.link)

        val mention = parse("<a href=\"https://shikimori.io/comments/14164991\" class=\"b-mention b-entry-404 bubbled\" data-attrs=\"{&quot;id&quot;:14164991,&quot;type&quot;:&quot;comment&quot;,&quot;userId&quot;:1,&quot;text&quot;:&quot;nick&quot;}\"><s>@</s><span>nick</span><del>[comment=14164991]</del></a>")
        assertTrue(mention.texts().all { it.style.link == null })
        assertEquals("@nick [comment=14164991]", mention.allInlines().plain())
    }

    // Mentions and replies

    @Test
    fun `the reply prefix is a mention of the comment`() {
        val first = (fixture("comment_14100487").blocks.first() as Block.Paragraph).inlines.first() as Inline.Text
        assertEquals("@kalich411", first.text)
        assertEquals(LinkTarget.Screen(Type.COMMENT, 14099661), first.style.link)
    }

    @Test
    fun `user and topic mentions`() {
        assertEquals(LinkTarget.Screen(Type.USER, 1153241), fixture("comment_11694247").textWith("@saxarnik").style.link)
        assertEquals(LinkTarget.Screen(Type.USER, 890017), fixture("comment_14006183").textWith("@Toxexex").style.link)
        assertEquals(LinkTarget.Screen(Type.TOPIC, 306918), fixture("comment_13247766").textWith("@Animoon4444").style.link)
        //`[entry]`, the old name of a topic
        assertEquals(LinkTarget.Screen(Type.TOPIC, 170638), fixture("comment_1095323").textWith("@Кураноскэ-кун").style.link)
    }

    @Test
    fun `the replies line, with one and with two comments`() {
        val one = fixture("comment_14161975").all<Block.Replies>().single()
        assertEquals("Ответ:", one.label)
        assertEquals("@Cat12", one.inlines.plain())

        val two = fixture("comment_14159216").all<Block.Replies>().single()
        assertEquals("Ответы:", two.label)
        assertEquals("@Неслышный Наблюдатель, @ꓝantom°", two.inlines.plain())
        assertEquals(LinkTarget.Screen(Type.COMMENT, 14163025), (two.inlines.last() as Inline.Text).style.link)
    }

    @Test
    fun `a topic's html has no replies line`() {
        //raw `:very sad2:[replies=13091273]`
        val text = fixture("topic_604041_body")
        assertTrue(text.all<Block.Replies>().isEmpty())
        assertEquals(1, text.allInlines().filterIsInstance<Inline.Smiley>().size)
    }

    // Quotes

    @Test
    fun `a quote of a comment has the author's avatar and name, which open the comment`() {
        val quote = fixture("comment_14164825").all<Block.Quote>().first()
        val target = LinkTarget.Screen(Type.COMMENT, 14164364)
        assertEquals("https://shikimori.io/system/users/x32/1164584.png?1662131484", (quote.author[0] as Inline.Avatar).url)
        assertEquals(target, (quote.author[0] as Inline.Avatar).style.link)
        assertEquals(Inline.Text("ArkMur", Style(link = target)), quote.author[1])
        assertEquals("Фансервис и \"красота\" в ней - половина успеха тайтла.", quote.blocks.single().inlines().plain())
    }

    @Test
    fun `quote authors of a topic, a nickname and a label`() {
        assertEquals(LinkTarget.Screen(Type.TOPIC, 634144),
                fixture("comment_13852792").all<Block.Quote>().single().author.filterIsInstance<Inline.Text>().single().style.link)
        assertEquals(LinkTarget.UserByNickname("Tml"),
                fixture("comment_14163749").all<Block.Quote>().first().author.filterIsInstance<Inline.Text>().single().style.link)
        val label = fixture("comment_13004443").all<Block.Quote>().single().author
        assertEquals("Описание на английском языке", label.plain())
        assertTrue(label.filterIsInstance<Inline.Text>().all { it.style.link == null })
    }

    @Test
    fun `a quote of a deleted comment shows the author and the crossed-out tag`() {
        val author = fixture("comment_14159191").all<Block.Quote>()[1].author
        assertEquals("Кусь 🐾 [comment=19769709]", author.plain())
        assertTrue(author.filterIsInstance<Inline.Text>().last().style.isDeleted)
    }

    @Test
    fun `quotes nest`() {
        //the first quote of the text is S1's markdown `> цитата`
        val outer = fixture("preview_s1").blocks.filterIsInstance<Block.Quote>().last()
        assertEquals("внешняя", outer.blocks[0].inlines().plain())
        assertEquals("внутренняя", (outer.blocks[1] as Block.Quote).blocks.single().inlines().plain())
    }

    // Spoilers

    @Test
    fun `an inline spoiler hides its text in the sentence`() {
        val hidden = fixture("comment_14100487").textWith("мертвые")
        assertEquals(1, hidden.style.spoilers.size)
        assertTrue(fixture("comment_14100487").textWith("я думаю").style.spoilers.isEmpty())
    }

    @Test
    fun `a labelled spoiler stays in the sentence, links inside`() {
        val spoiler = fixture("comment_14156778").allInlines().filterIsInstance<Inline.LabelledSpoiler>().single()
        assertEquals("шок контент", spoiler.label.plain())
        val links = spoiler.blocks.flatMap { it.inlines() }.filterIsInstance<Inline.Text>().mapNotNull { it.style.link }
        assertEquals(LinkTarget.Web("https://kujico.jp/detail/158"), links[1])
    }

    @Test
    fun `a block spoiler, a moderator's one, and what stays outside it`() {
        val block = fixture("comment_14093733").blocks.single() as Block.Spoiler
        assertEquals("спойлер", block.label.plain())
        assertTrue(block.blocks.single() is Block.Images)

        val hidden = fixture("comment_14159216")
        assertEquals("Скрыто модератором", (hidden.blocks[0] as Block.Spoiler).label.plain())
        assertTrue(hidden.blocks[1] is Block.Replies)
        assertTrue(hidden.blocks[2] is Block.Ban)
    }

    @Test
    fun `spoilers nest, and an inline one holding images opens below the line`() {
        val outer = fixture("preview_s1").blocks.filterIsInstance<Block.Spoiler>().single()
        assertEquals("внешний", outer.label.plain())
        val inner = outer.blocks.single().inlines().filterIsInstance<Inline.LabelledSpoiler>().single()
        assertEquals("внутренний", inner.label.plain())

        val withImages = fixture("topic_367213_body").allInlines().filterIsInstance<Inline.LabelledSpoiler>()
        assertTrue(withImages.isNotEmpty())
        assertTrue(withImages.all { it.label.isEmpty() && it.blocks.single() is Block.Images })
    }

    // Images, smileys, video

    @Test
    fun `an upload shows its thumbnail, enlarges the original, and knows its real size`() {
        val image = fixture("comment_14163764").all<Block.Images>().single().images.single()
        assertTrue(image.url.contains("/user_images_h/thumbnail/"))
        assertTrue(image.zoomUrl!!.contains("/user_images_h/original/"))
        assertEquals(918, image.realWidth)
        assertEquals(472, image.realHeight)

        val sized = fixture("topic_581152_body").all<Block.Images>().single().images.single()
        assertEquals(400, sized.width)
        assertEquals(283, sized.height)
        assertEquals(735, sized.realWidth)
    }

    @Test
    fun `outside images come through the proxy, no-zoom ones and posters cannot be enlarged`() {
        val camo = fixture("comment_14147875").all<Block.Images>().single().images.single()
        assertTrue(camo.url.startsWith("https://camo-v3.shikimori.io/"))
        assertTrue(camo.zoomUrl!!.startsWith("https://camo-v3.shikimori.io/"))

        val noZoom = fixture("comment_14102268").all<Block.Images>().single().images.single()
        assertNull(noZoom.zoomUrl)
        assertEquals(110, noZoom.width)

        val poster = fixture("comment_14091351").all<Block.Images>().single().images.single()
        assertTrue(poster.isPoster)
        assertNull(poster.zoomUrl)
    }

    @Test
    fun `old static hosts move to shikimori io, and http images to https`() {
        val old = parse("<a href=\"https://desu.shikimori.me/system/images/original/136572.png\" data-href=\"https://camo-v3.shikimori.io/abc?url=https%3A%2F%2Fdesu.shikimori.me%2Fsystem%2Fimages%2Foriginal%2F136572.png\" class=\"b-image unprocessed\" data-attrs=\"{&quot;src&quot;:&quot;https://desu.shikimori.me/system/images/original/136572.png&quot;}\"><img src=\"https://camo-v3.shikimori.io/abc?url=https%3A%2F%2Fdesu.shikimori.me%2Fsystem%2Fimages%2Foriginal%2F136572.png\" class=\"check-width\"></a>")
                .all<Block.Images>().single().images.single()
        assertEquals("https://shikimori.io/system/images/original/136572.png", old.url)
        assertEquals("https://shikimori.io/system/images/original/136572.png", old.zoomUrl)

        val tenor = parse("<span class=\"b-image no-zoom\" data-attrs=\"{&quot;src&quot;:&quot;https://media.tenor.com/a.gif&quot;}\"><img src=\"http://media.tenor.com/a.gif\" class=\"check-width\" width=\"80\"></span>")
                .all<Block.Images>().single().images.single()
        assertEquals("https://media.tenor.com/a.gif", tenor.url)
    }

    @Test
    fun `images side by side form one row, a line break starts another`() {
        val img = "<span class=\"b-image no-zoom\"><img src=\"https://i.imgur.com/a.png\"></span>"
        assertEquals(listOf(2), parse("$img $img").all<Block.Images>().map { it.images.size })
        assertEquals(listOf(1, 1), parse("$img<br>$img").all<Block.Images>().map { it.images.size })
    }

    @Test
    fun `an image inside a link follows the link`() {
        val image = parse("<a class=\"b-link\" href=\"/clubs/4266-x/pages/3929\"><span class=\"b-image no-zoom\"><img src=\"https://i.imgur.com/a.png\"></span></a>")
                .all<Block.Images>().single().images.single()
        assertEquals(LinkTarget.Web("https://shikimori.io/clubs/4266-x/pages/3929"), image.link)
    }

    @Test
    fun `the zoomable images of a text form one gallery`() {
        assertEquals(7, fixture("topic_642713_body").zoomableImages().size)
    }

    @Test
    fun `smileys, a code with a space among them`() {
        val tea = fixture("comment_14161713").allInlines().filterIsInstance<Inline.Smiley>().single()
        assertEquals(":tea shock:", tea.code)
        assertEquals("https://shikimori.io/images/smileys/:tea%20shock:.gif", tea.url)
        //one alone, then three in a row, which stay three: the server collapses only four or more
        val ololo = (fixture("comment_14163092").blocks.first() as Block.Paragraph).inlines
        assertEquals(4, ololo.count { it is Inline.Smiley })
        assertTrue(ololo.takeLast(3).all { it is Inline.Smiley })
    }

    @Test
    fun `a video card, a direct mp4 link, and the news wall`() {
        val youtube = fixture("comment_14164008").all<Block.VideoCard>().single().video
        assertEquals("https://youtu.be/jdLVdUHVa_w", youtube.url)
        assertEquals("https://img.youtube.com/vi/jdLVdUHVa_w/hqdefault.jpg", youtube.thumbnail)
        assertEquals("youtube", youtube.host)

        val mp4 = fixture("comment_13311271").all<Block.VideoCard>().single().video
        assertEquals("https://i.imgur.com/qHAy7xs.mp4", mp4.url)
        assertEquals("https://shikimori.io/system/webm_videos/normal/13400.jpg?1777640262", mp4.thumbnail)
        assertEquals("html5", mp4.host)

        val wall = fixture("topic_649866_footer").blocks.single() as Block.Wall
        assertEquals("PV1", wall.videos.single().title)
        assertEquals(1, wall.images.size)
    }

    // Rare and system parts

    @Test
    fun `poster cards of a collection, by the naming setting`() {
        val cards = fixture("topic_649710_body").all<Block.Cards>().single()
        assertEquals(133, cards.cards.size)
        assertEquals(LinkTarget.Screen(Type.CHARACTER, 13020), cards.cards[0].target)
        assertEquals("Аскеладд", cards.cards[0].title)
        assertEquals("Askeladd", fixture("topic_649710_body", russianNames = false).all<Block.Cards>().single().cards[0].title)
    }

    @Test
    fun `a censored card has no poster, and the more tile opens the topic`() {
        val text = parse("<article class=\"c-column b-catalog_entry c-anime entry-1 \" id=\"1\"><a class=\"cover\" href=\"https://shikimori.io/animes/1-x\"><img alt=\"Икс\" class=\"is-moderation_censored\" src=\"/assets/globals/missing/preview_animanga.png\"><span class=\"title\"><span class='name-en'>X</span><span class='name-ru'>Икс</span></span></a></article>" +
                "<a class=\"image-cutter see-more animanga\" href=\"https://shikimori.io/forum/collections/c/644376-obsuzhdenie-kollektsii\"><div class=\"text\"><div class=\"inner\"><div class=\"dots\">...</div>ещё +36</div></div></a>")
        val cards = text.blocks.single() as Block.Cards
        assertTrue(cards.cards.single().isCensored)
        assertNull(cards.cards.single().poster)
        assertEquals("ещё +36", cards.more!!.text)
        assertEquals(LinkTarget.Screen(Type.TOPIC, 644376), cards.more!!.target)
    }

    @Test
    fun `a critique's ratings`() {
        val ratings = fixture("topic_634144_body").blocks.first() as Block.Ratings
        assertEquals(4, ratings.lines.size)
        assertEquals("Сюжет", ratings.lines[0].title)
        assertEquals("3", ratings.lines[0].score)
        assertEquals("Очень плохо", ratings.lines[0].notice)
    }

    @Test
    fun `a ban under a comment names the moderator and the reason`() {
        val ban = fixture("comment_14165453").all<Block.Ban>().single()
        assertEquals("Аясэ-тян: бан на 2 часа. п.3 правил сайта.", ban.inlines.plain())
        assertEquals(LinkTarget.UserByNickname("Аясэ-тян"), (ban.inlines[0] as Inline.Avatar).style.link)
    }

    @Test
    fun `a poll is a poll, not its literal tag`() {
        val text = fixture("topic_578956_body")
        assertEquals(Block.Poll(276040), text.blocks.last())
        assertTrue(text.texts().none { it.text.contains("[poll") })
    }

    @Test
    fun `a contest status takes its words from the attributes`() {
        val text = fixture("comment_14098157")
        assertEquals("Турнир Лучший аниме-фильм 2024-2025 гг завершён.", text.allInlines().plain())
        assertNotNull(text.textWith("Лучший").style.link)
    }

    @Test
    fun `a tag the site leaves as text stays text`() {
        assertEquals("Топик обсуждения [tierlist=2308]тирлиста[/tierlist].", fixture("topic_643254_body").allInlines().plain())
    }

    // Size

    @Test
    fun `a very large text is read in reasonable time`() {
        //a topic of 1.64 M characters exists (research 3, T52); nine copies of a 179 K collection come close
        val one = javaClass.getResourceAsStream("/richtext/topic_649710_body.html")!!.bufferedReader(Charsets.UTF_8).readText()
        val html = one.repeat(9)
        parse(one)
        val start = System.nanoTime()
        val text = parse(html)
        val millis = (System.nanoTime() - start) / 1_000_000
        assertEquals(9 * 133, text.all<Block.Cards>().sumOf { it.cards.size })
        assertTrue("took $millis ms", millis < 5_000)
    }
}
