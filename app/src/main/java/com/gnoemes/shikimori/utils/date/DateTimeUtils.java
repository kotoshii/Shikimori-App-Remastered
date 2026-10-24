package com.gnoemes.shikimori.utils.date;

import org.joda.time.DateTime;

/**
 * Util class for date and time
 */
public interface DateTimeUtils {

    /**
     * Returns current date
     */
    DateTime getNowDateTime();

    boolean isToday(DateTime time);

    boolean isTomorrow(DateTime time);

    boolean isSameYear(DateTime time);

    boolean isSameYear(DateTime firstDate, DateTime secondDate);

    /**
     * The same calendar date: the one compare {@link #isToday} and {@link #isTomorrow} are built on.
     */
    boolean isSameDay(DateTime firstDate, DateTime secondDate);

}