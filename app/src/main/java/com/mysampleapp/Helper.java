package com.mysampleapp;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Vishaan on 8/22/2016.
 */
public class Helper {
    public static final Date filterDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
