package com.gnoemes.shikimori.utils.widgets;


import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.webkit.URLUtil;
import android.widget.TextView;

import com.gnoemes.shikimori.entity.common.domain.Type;
import com.gnoemes.shikimori.entity.common.presentation.shikimori.ShikimoriViews;
import com.gnoemes.shikimori.presentation.view.common.widget.shikimori.ShikimoriTextView;

/**
 * Link taps in one text view: a web link opens as usual, one of the app's own links
 * ({@code <TYPE>_<id>}, see {@code ShikimoriTextView.appendLink}) goes to that view's listener, and
 * anything else - {@code mailto:}, a site-relative path - is left alone. One per text view: a shared
 * one held the listener of whichever view was made last, and that view with it.
 */
public class ShikimoriLinkMovementMethod extends LinkMovementMethod {

    private final ShikimoriTextView.LinkClickListener clickListener;

    public ShikimoriLinkMovementMethod(ShikimoriTextView.LinkClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {

        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            ClickableSpan[] links = buffer.getSpans(off, off, ClickableSpan.class);

            if (links.length != 0) {
                if (action == MotionEvent.ACTION_UP) {
                    String url = links[0] instanceof URLSpan ? ((URLSpan) links[0]).getURL() : null;
                    if (url != null && URLUtil.isNetworkUrl(url)) {
                        links[0].onClick(widget);
                    } else if (url != null) {
                        onAppLink(url);
                    }
                } else if (action == MotionEvent.ACTION_DOWN) {
                    Selection.setSelection(buffer,
                            buffer.getSpanStart(links[0]),
                            buffer.getSpanEnd(links[0]));
                }
                return true;
            } else {
                Selection.removeSelection(buffer);
            }
        }

        return super.onTouchEvent(widget, buffer, event);
    }

    //split at the last divider: a type's own name can hold one (CLUB_PAGE)
    private void onAppLink(String url) {
        int divider = url.lastIndexOf(ShikimoriViews.ACTION_DIVIDER);
        if (divider <= 0) return;

        Type type;
        long id;
        try {
            type = Type.valueOf(url.substring(0, divider));
            id = Long.parseLong(url.substring(divider + ShikimoriViews.ACTION_DIVIDER.length()));
        } catch (IllegalArgumentException notAppLink) {
            //NumberFormatException is one too
            return;
        }
        clickListener.onLinkClicked(type, id);
    }
}
