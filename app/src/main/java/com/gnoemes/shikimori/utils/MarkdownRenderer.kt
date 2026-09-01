package com.gnoemes.shikimori.utils

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan

/**
 * Renders the small slice of markdown that this project's github release notes actually use, into a
 * `CharSequence` a `TextView` can show.
 *
 * A full markdown library was considered and rejected: Markwon is the obvious choice but lives on
 * Maven Central, which this project does not declare, and nothing about a new dependency can be
 * verified here because Gradle cannot run - a resolution failure on AGP 3.2.1 would only appear on
 * the developer's machine. The notes are written to a pinned house style, so the supported set is
 * known and small. **That set is documented in BUILD_AND_RELEASE.md** - keep the two in step.
 *
 * Supported: `#`/`##`/`###` headings, `-`/`*` bullets, `**bold**`, `` `code` ``, `[text](url)` and
 * bare urls. Anything else is passed through as plain text rather than swallowed, so an unsupported
 * construct looks untidy instead of losing its content.
 *
 * A `TextView` showing this needs `LinkMovementMethod` for the links to be tappable.
 */
object MarkdownRenderer {

    private val BOLD = Regex("\\*\\*(.+?)\\*\\*")
    private val CODE = Regex("`([^`]+)`")
    private val LINK = Regex("\\[([^\\]]+)]\\((\\S+?)\\)")
    //stops before a closing bracket or paren so a url inside a markdown link is not double matched
    private val BARE_URL = Regex("https?://[^\\s)\\]]+")

    private val URL_TRAILING = charArrayOf('.', ',', ';', ':', '!', '?')

    private const val BULLET_GAP = 24

    fun render(markdown: String?): CharSequence {
        if (markdown.isNullOrBlank()) return ""

        val out = SpannableStringBuilder()
        //github sends crlf
        val lines = markdown.replace("\r\n", "\n").replace("\r", "\n").split("\n")

        for (raw in lines) {
            val line = raw.trim()

            if (line.isEmpty()) {
                //one blank line between blocks, never a run of them
                if (out.isNotEmpty() && !endsWithBlankLine(out)) out.append("\n")
                continue
            }

            when {
                line.startsWith("### ") -> heading(out, line.substring(4), 1.05f)
                line.startsWith("## ") -> heading(out, line.substring(3), 1.15f)
                line.startsWith("# ") -> heading(out, line.substring(2), 1.25f)
                line.startsWith("- ") || line.startsWith("* ") -> bullet(out, line.substring(2))
                else -> {
                    inline(out, line)
                    out.append("\n")
                }
            }
        }

        while (out.isNotEmpty() && out[out.length - 1] == '\n') out.delete(out.length - 1, out.length)

        return out
    }

    private fun endsWithBlankLine(out: SpannableStringBuilder): Boolean =
            out.length >= 2 && out[out.length - 1] == '\n' && out[out.length - 2] == '\n'

    private fun heading(out: SpannableStringBuilder, text: String, scale: Float) {
        val start = out.length
        inline(out, text)
        out.append("\n")

        //the heading may already carry ** of its own; bolding the whole line is harmless either way
        out.setSpan(StyleSpan(Typeface.BOLD), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        out.setSpan(RelativeSizeSpan(scale), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    /**
     * The newline is appended **before** the span is set: `BulletSpan` is a paragraph span, and
     * Android only honours one that covers a whole line.
     */
    private fun bullet(out: SpannableStringBuilder, text: String) {
        val start = out.length
        inline(out, text)
        out.append("\n")

        out.setSpan(BulletSpan(BULLET_GAP), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    /**
     * Walks the line taking whichever construct starts earliest, so the markers cannot be applied in
     * the wrong order. Bold re-enters this for its own contents; the other markers take their text
     * literally, which is what `code` in particular has to do.
     */
    private fun inline(out: SpannableStringBuilder, text: String) {
        var rest = text

        while (rest.isNotEmpty()) {
            val bold = BOLD.find(rest)
            val code = CODE.find(rest)
            val link = LINK.find(rest)
            val url = BARE_URL.find(rest)

            val next = listOfNotNull(bold, code, link, url).minBy { it.range.first }
            if (next == null) {
                out.append(rest)
                return
            }

            out.append(rest.substring(0, next.range.first))
            val start = out.length
            var consumed = next.range.last + 1

            when {
                next === bold -> {
                    //bold is the one marker that wraps others in practice - **[text](url)** - so it
                    //re-enters rather than appending its contents raw. The inner text is always
                    //shorter than what matched, so the recursion cannot run away.
                    inline(out, next.groupValues[1])
                    out.setSpan(StyleSpan(Typeface.BOLD), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                next === code -> {
                    out.append(next.groupValues[1])
                    out.setSpan(TypefaceSpan("monospace"), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                next === link -> {
                    out.append(next.groupValues[1])
                    out.setSpan(URLSpan(next.groupValues[2]), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                else -> {
                    //a url that ends a sentence must not take the punctuation into its target
                    val url = next.value.trimEnd(*URL_TRAILING)
                    consumed = next.range.first + url.length
                    out.append(url)
                    out.setSpan(URLSpan(url), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            rest = rest.substring(consumed)
        }
    }
}
