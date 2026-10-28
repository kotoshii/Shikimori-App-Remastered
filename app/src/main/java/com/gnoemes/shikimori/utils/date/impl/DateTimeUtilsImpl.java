package com.gnoemes.shikimori.utils.date.impl;

import com.gnoemes.shikimori.utils.date.DateTimeUtils;

import org.joda.time.DateTime;

import javax.inject.Inject;

public class DateTimeUtilsImpl implements DateTimeUtils {

    @Inject
    public DateTimeUtilsImpl() {
    }

    @Override
    public DateTime getNowDateTime() {
        return DateTime.now();
    }

    @Override
    public boolean isToday(DateTime date) {
        return isSameDay(getNowDateTime(), date);
    }

    @Override
    public boolean isTomorrow(DateTime date) {
        return isSameDay(getNowDateTime().plusDays(1), date);
    }

    @Override
    public boolean isSameYear(DateTime time) {
        return getNowDateTime().toLocalDate().getYear() == time.toLocalDate().getYear();
    }

    @Override
    public boolean isSameYear(DateTime firstDate, DateTime secondDate) {
        return firstDate.toLocalDate().getYear() == secondDate.toLocalDate().getYear();
    }

    //the whole date: the day of the year alone took a date a year back for today, so it read
    //"8760 hours ago" (BUGS #26.1)
    @Override
    public boolean isSameDay(DateTime firstDate, DateTime secondDate) {
        return firstDate != null &&
                secondDate != null &&
                firstDate.toLocalDate().equals(secondDate.toLocalDate());
    }
}
