package com.gnoemes.shikimori.utils.widgets;


import android.content.res.ColorStateList;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.gnoemes.shikimori.presentation.view.common.widget.shikimori.HiddenSpoilerSpan;
import com.gnoemes.shikimori.presentation.view.common.widget.shikimori.LinkSpan;
import com.gnoemes.shikimori.presentation.view.common.widget.shikimori.OpenSpoilerSpan;
import com.gnoemes.shikimori.presentation.view.common.widget.shikimori.PressedSpan;
import com.gnoemes.shikimori.presentation.view.common.widget.shikimori.SpoilerLabelSpan;

/**
 * Taps in one text view of user text: the tappable part under the finger runs its own action - a link
 * opens its target, a closed spoiler opens, an open one closes (see RichTextSpans.kt). A closed spoiler wins
 * over a link inside it, and a link over the open spoiler around it, as on the site. Until P8-T4 only
 * URLSpans were run and every other tap was lost. One per text view (CODE_CONVENTIONS.md §12): it keeps
 * the part pressed now, and a shared one used to hold a listener, and leaked it.
 *
 * A pressed link or spoiler label gets a tint ({@link PressedSpan}) from the touch until a moment after the finger
 * lifts, so among many links the one tapped is seen (P8-T4 Q42); it goes at once when the touch turns into a
 * scroll. Until then the press was marked with a selection, removed as the finger lifted, so a quick tap showed
 * nothing. The tint changes no sizes, so nothing is laid out again and the page cannot jump.
 */
public class ShikimoriLinkMovementMethod extends LinkMovementMethod {

    //how long the tint stays after the finger lifts: about as long as a button's ripple
    private static final long PRESS_AFTER_UP_MS = 300;
    //the tint is the links' colour at 30%: it follows every theme and accent colour, and shows behind text (the
    //theme's ripple colour, 6%, hardly does; P8-T4 Q44)
    private static final int PRESS_ALPHA = (int) (0.3f * 255);

    //the tint on the part pressed now, and the text it is in
    private PressedSpan pressed = null;
    private Spannable pressedText = null;

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {

        int action = event.getAction();

        //the finger moved on and the page scrolls: the tint goes at once
        if (action == MotionEvent.ACTION_CANCEL) clearPressed();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            ClickableSpan span = spanAt(widget, buffer, event);

            if (span != null) {
                if (action == MotionEvent.ACTION_UP) {
                    //the tint leaves a moment later; taken before the action, which may give the view new text
                    releasePressed(widget);
                    span.onClick(widget);
                } else {
                    clearPressed();
                    if (showsPress(span)) press(widget, buffer, span);
                }
                return true;
            } else {
                clearPressed();
                //a text view that is not clickable takes only the taps on its parts (a forum row's post,
                //ShikimoriTextView.passPlainTaps): the rest go to the view under it. The base class would take
                //the press, to scroll the text by dragging.
                if (action == MotionEvent.ACTION_DOWN && !widget.isClickable()) return false;
            }
        }

        return super.onTouchEvent(widget, buffer, event);
    }

    private void press(TextView widget, Spannable buffer, ClickableSpan span) {
        pressed = new PressedSpan(pressTint(widget));
        pressedText = buffer;
        buffer.setSpan(pressed, buffer.getSpanStart(span), buffer.getSpanEnd(span), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    //a view off its window runs it when it comes back, so a tint never stays
    private void releasePressed(TextView widget) {
        PressedSpan tint = pressed;
        Spannable text = pressedText;
        pressed = null;
        pressedText = null;
        if (tint != null) widget.postDelayed(() -> text.removeSpan(tint), PRESS_AFTER_UP_MS);
    }

    private void clearPressed() {
        if (pressed != null) pressedText.removeSpan(pressed);
        pressed = null;
        pressedText = null;
    }

    private static int pressTint(TextView widget) {
        ColorStateList links = widget.getLinkTextColors();
        int color = links != null ? links.getColorForState(widget.getDrawableState(), links.getDefaultColor()) : widget.getCurrentTextColor();
        return ColorUtils.setAlphaComponent(color, PRESS_ALPHA);
    }

    //a link or a spoiler's label; a spoiler's bar shows the tap by opening
    private static boolean showsPress(ClickableSpan span) {
        return span instanceof LinkSpan || span instanceof SpoilerLabelSpan;
    }

    /**
     * The tappable part under the finger; none beside the end of a line, where the nearest letter is a link.
     * A closed spoiler first, an open spoiler last: the parts inside it are its links.
     */
    private static ClickableSpan spanAt(TextView widget, Spannable buffer, MotionEvent event) {
        Layout layout = widget.getLayout();
        if (layout == null) return null;

        int x = (int) event.getX() - widget.getTotalPaddingLeft() + widget.getScrollX();
        int y = (int) event.getY() - widget.getTotalPaddingTop() + widget.getScrollY();

        int line = layout.getLineForVertical(y);
        if (x < layout.getLineLeft(line) || x > layout.getLineRight(line)) return null;
        int offset = layout.getOffsetForHorizontal(line, x);

        ClickableSpan[] spans = buffer.getSpans(offset, offset, ClickableSpan.class);
        ClickableSpan open = null;
        ClickableSpan other = null;
        for (ClickableSpan span : spans) {
            if (span instanceof HiddenSpoilerSpan) return span;
            if (span instanceof OpenSpoilerSpan) {
                if (open == null) open = span;
            } else if (other == null) {
                other = span;
            }
        }
        return other != null ? other : open;
    }
}
