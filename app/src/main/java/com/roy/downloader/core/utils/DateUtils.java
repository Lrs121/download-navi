package com.roy.downloader.core.utils;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.roy.downloader.R;

import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

public class DateUtils {
    public static final long SECOND_IN_MILLIS = 1000;

    private static final int secondsInDay = 86400;
    private static final int secondsInHour = 3600;
    private static final int secondsInMinute = 60;

    private static String elapsedFormatSS;
    private static String elapsedFormatMMSS;
    private static String elapsedFormatHMMSS;
    private static String elapsedFormatDHMM;
    private static final ReentrantLock lock = new ReentrantLock();

    public static String formatElapsedTime(Context context, long elapsedSeconds) {
        return formatElapsedTime(context, null, elapsedSeconds);
    }

    public static String formatElapsedTime(@NonNull Context context,
                                           StringBuilder recycle,
                                           long elapsedSeconds) {
        long days = 0;
        long hours = 0;
        long minutes = 0;
        long seconds;

        if (elapsedSeconds >= secondsInDay) {
            days = elapsedSeconds / secondsInDay;
            elapsedSeconds -= days * secondsInDay;
        }

        if (elapsedSeconds >= secondsInHour) {
            hours = elapsedSeconds / secondsInHour;
            elapsedSeconds -= hours * secondsInHour;
        }

        if (elapsedSeconds >= secondsInMinute) {
            minutes = elapsedSeconds / secondsInMinute;
            elapsedSeconds -= minutes * secondsInMinute;
        }

        seconds = elapsedSeconds;

        StringBuilder sb = recycle;
        if (sb == null)
            sb = new StringBuilder(8);
        else
            sb.setLength(0);

        Formatter f = new Formatter(sb, Locale.getDefault());

        initFormatStrings(context);

        if (days > 0)
            return f.format(elapsedFormatDHMM, days, hours, minutes).toString();
        else if (hours > 0)
            return f.format(elapsedFormatHMMSS, hours, minutes, seconds).toString();
        else if (minutes > 0)
            return f.format(elapsedFormatMMSS, minutes, seconds).toString();
        else
            return f.format(elapsedFormatSS, seconds).toString();
    }

    private static void initFormatStrings(@NonNull Context context) {
        lock.lock();

        try {
            elapsedFormatSS = context.getString(R.string.elapsed_time_format_ss);
            elapsedFormatMMSS = context.getString(R.string.elapsed_time_format_mm_ss);
            elapsedFormatHMMSS = context.getString(R.string.elapsed_time_format_h_mm_ss);
            elapsedFormatDHMM = context.getString(R.string.elapsed_time_format_d_h_mm);
        } finally {
            lock.unlock();
        }
    }

    public static long endOfToday(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);

        return calendar.getTimeInMillis();
    }

    public static long startOfToday(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    public static long endOfYesterday(long timeMillis) {
        return endOfToday(timeMillis) - secondsInDay * 1000;
    }

    public static long startOfYesterday(long timeMillis) {
        return startOfToday(timeMillis) - secondsInDay * 1000;
    }

    public static long endOfWeek(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.add(Calendar.WEEK_OF_YEAR, 1);

        return calendar.getTimeInMillis();
    }

    public static long startOfWeek(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 24);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());

        return calendar.getTimeInMillis();
    }

    public static long endOfMonth(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));

        return calendar.getTimeInMillis();
    }

    public static long startOfMonth(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        return calendar.getTimeInMillis();
    }

    public static long endOfYear(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR));

        return calendar.getTimeInMillis();
    }

    public static long startOfYear(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.DAY_OF_YEAR, 1);

        return calendar.getTimeInMillis();
    }

    public static long elapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }
}
