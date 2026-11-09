package com.gnoemes.shikimori.utils.widgets;


import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import com.gnoemes.shikimori.presentation.view.common.widget.shikimori.HiddenSpoilerSpan;
import com.gnoemes.shikimori.presentation.view.common.widget.shikimori.OpenSpoilerSpan;

/**
 * Taps in one text view of user text: the tappable part under the finger runs its own action - a link
 * opens its target, a closed spoiler opens, an open one closes (see RichTextSpans.kt). A closed spoiler wins
 * over a link inside it, and a link over the open spoiler around it, as on the site. Until P8-T4 only
 * URLSpans were run and every other tap was lost. One per text view (CODE_CONVENTIONS.md §12): it keeps
 * the part pressed now, and a shared one used to hold a listener, and leaked it.
 */
public class ShikimoriLinkMovementMethod extends LinkMovementMethod {

    //marked by a selection while pressed
    private boolean pressed = false;

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {

        int action = event.getAction();

        //the finger moved on and the page scrolls: the pressed mark must not stay, a selection the page
        //would scroll back to (ShikimoriTextView.onTouchEvent)
        if (action == MotionEvent.ACTION_CANCEL && pressed) {
            pressed = false;
            Selection.removeSelection(buffer);
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            ClickableSpan span = spanAt(widget, buffer, event);

            if (span != null) {
                if (action == MotionEvent.ACTION_UP) {
                    //before the action, which may give the view new text
                    pressed = false;
                    Selection.removeSelection(buffer);
                    span.onClick(widget);
                } else {
                    pressed = true;
                    Selection.setSelection(buffer, buffer.getSpanStart(span), buffer.getSpanEnd(span));
                }
                return true;
            } else {
                pressed = false;
                Selection.removeSelection(buffer);
            }
        }

        return super.onTouchEvent(widget, buffer, event);
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
