/*
 * $Id: CalendarUtils.java,v 1.8 2008/02/27 15:31:26 kleopatra Exp $
 *
 * Copyright 2007 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package org.jdesktop.swingx.calendar;

import java.util.Calendar;
import java.util.Date;

/**
 * Calendar manipulation.
 * 
 * PENDING: replace by something tested - as is c&p'ed dateUtils 
 * to work on a calendar instead of using long
 * 
 * @author Jeanette Winzenburg
 */
public class CalendarUtils {

    // Constants used internally; unit is milliseconds
    @SuppressWarnings("unused")
    public static final int ONE_MINUTE = 60*1000;
    @SuppressWarnings("unused")
    public static final int ONE_HOUR   = 60*ONE_MINUTE;
    @SuppressWarnings("unused")
    public static final int THREE_HOURS = 3 * ONE_HOUR;
    @SuppressWarnings("unused")
    public static final int ONE_DAY    = 24*ONE_HOUR;
    
    /**
     * Adjusts the Calendar to the end of the day of the last day in DST in the
     * current year or unchanged if  not using DST. Returns the calendar's date or null, if not 
     * using DST.<p>
     * 
     * 
     * @param calendar the calendar to adjust
     * @return the end of day of the last day in DST, or null if not using DST.
     */
    public static Date getEndOfDST(Calendar calendar) {
        if (!calendar.getTimeZone().useDaylightTime()) return null;
        long old = calendar.getTimeInMillis();
        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        endOfMonth(calendar);
        startOfDay(calendar);
        for (int i = 0; i < 366; i++) {
           calendar.add(Calendar.DATE, -1);
           if (calendar.getTimeZone().inDaylightTime(calendar.getTime())) {
               endOfDay(calendar);
               return calendar.getTime();
           }
        }
        calendar.setTimeInMillis(old);
        return null;
    }

    
    /**
     * Adjusts the Calendar to the end of the day of the first day in DST in the
     * current year or unchanged if  not using DST. Returns the calendar's date or null, if not 
     * using DST.<p>
     * 
     * Note: the start of the day of the first day in DST is ill-defined!
     * 
     * @param calendar the calendar to adjust
     * @return the start of day of the first day in DST, or null if not using DST.
     */
    public static Date getStartOfDST(Calendar calendar) {
        if (!calendar.getTimeZone().useDaylightTime()) return null;
        long old = calendar.getTimeInMillis();
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        startOfMonth(calendar);
        endOfDay(calendar);
        for (int i = 0; i < 366; i++) {
           calendar.add(Calendar.DATE, 1);
           if (calendar.getTimeZone().inDaylightTime(calendar.getTime())) {
               endOfDay(calendar);
               return calendar.getTime();
           }
        }
        calendar.setTimeInMillis(old);
        return null;
    }
    /**
     * Returns a boolean indicating if the given calendar represents the 
     * start of a day (in the calendar's time zone). The calendar is unchanged.
     * 
     * @param calendar the calendar to check.
     * 
     * @return true if the calendar's time is the start of the day,
     *   false otherwise.
     */
    public static boolean isStartOfDay(Calendar calendar) {
        Calendar temp = (Calendar) calendar.clone();
        temp.add(Calendar.MILLISECOND, -1);
        return temp.get(Calendar.DATE) != calendar.get(Calendar.DATE);
    }

    /**
     * Returns a boolean indicating if the given calendar represents the 
     * end of a day (in the calendar's time zone). The calendar is unchanged.
     * 
     * @param calendar the calendar to check.
     * 
     * @return true if the calendar's time is the end of the day,
     *   false otherwise.
     */
    public static boolean isEndOfDay(Calendar calendar) {
        Calendar temp = (Calendar) calendar.clone();
        temp.add(Calendar.MILLISECOND, 1);
        return temp.get(Calendar.DATE) != calendar.get(Calendar.DATE);
    }
    
    /**
     * Returns a boolean indicating if the given calendar represents the 
     * start of a month (in the calendar's time zone). Returns true, if the time is 
     * the start of the first day of the month, false otherwise. The calendar is unchanged.
     * 
     * @param calendar the calendar to check.
     * 
     * @return true if the calendar's time is the start of the first day of the month,
     *   false otherwise.
     */
    public static boolean isStartOfMonth(Calendar calendar) {
        Calendar temp = (Calendar) calendar.clone();
        temp.add(Calendar.MILLISECOND, -1);
        return temp.get(Calendar.MONTH) != calendar.get(Calendar.MONTH);
    }

    /**
     * Returns a boolean indicating if the given calendar represents the 
     * end of a month (in the calendar's time zone). Returns true, if the time is 
     * the end of the last day of the month, false otherwise. The calendar is unchanged.
     * 
     * @param calendar the calendar to check.
     * 
     * @return true if the calendar's time is the end of the last day of the month,
     *   false otherwise.
     */
    public static boolean isEndOfMonth(Calendar calendar) {
        Calendar temp = (Calendar) calendar.clone();
        temp.add(Calendar.MILLISECOND, 1);
        return temp.get(Calendar.MONTH) != calendar.get(Calendar.MONTH);
    }
    
    /**
     * Returns a boolean indicating if the given calendar represents the 
     * start of a month (in the calendar's time zone). Returns true, if the time is 
     * the start of the first day of the month, false otherwise. The calendar is unchanged.
     * 
     * @param calendar the calendar to check.
     * 
     * @return true if the calendar's time is the start of the first day of the month,
     *   false otherwise.
     */
    public static boolean isStartOfWeek(Calendar calendar) {
        Calendar temp = (Calendar) calendar.clone();
        temp.add(Calendar.MILLISECOND, -1);
        return temp.get(Calendar.WEEK_OF_YEAR) != calendar.get(Calendar.WEEK_OF_YEAR);
    }
    
    /**
     * Returns a boolean indicating if the given calendar represents the 
     * end of a week (in the calendar's time zone). Returns true, if the time is 
     * the end of the last day of the week, false otherwise. The calendar is unchanged.
     * 
     * @param calendar the calendar to check.
     * 
     * @return true if the calendar's time is the end of the last day of the week,
     *   false otherwise.
     */
    public static boolean isEndOfWeek(Calendar calendar) {
        Calendar temp = (Calendar) calendar.clone();
        temp.add(Calendar.MILLISECOND, 1);
        return temp.get(Calendar.WEEK_OF_YEAR) != calendar.get(Calendar.WEEK_OF_YEAR);
    }
    
    /**
     * Adjusts the calendar to the start of the current week.
     * That is, first day of the week with all time fields cleared.
     * @param calendar the calendar to adjust.
     */
    public static void startOfWeek(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        startOfDay(calendar);
    }

    /**
     * Adjusts the calendar to the end of the current week.
     * That is, last day of the week with all time fields at max.
     * @param calendar the calendar to adjust.
     */
    public static void endOfWeek(Calendar calendar) {
        startOfWeek(calendar);
        calendar.add(Calendar.DATE, 7);
        calendar.add(Calendar.MILLISECOND, -1);
    }
    
    /**
     * Adjusts the calendar to the end of the current week.
     * That is, last day of the week with all time fields at max.
     * The Date of the adjusted Calendar is
     * returned. 
     * 
     * @param calendar calendar to adjust.
     * @param date the Date to use.
     * @return the end of the week of the given date
     */
    public static Date endOfWeek(Calendar calendar, Date date) {
        calendar.setTime(date);
        endOfWeek(calendar);
        return calendar.getTime();
    }
    
    /**
     * Adjusts the calendar to the start of the current week.
     * That is, last day of the week with all time fields at max.
     * The Date of the adjusted Calendar is
     * returned. 
     * 
     * @param calendar calendar to adjust.
     * @param date the Date to use.
     * @return the start of the week of the given date
     */
    public static Date startOfWeek(Calendar calendar, Date date) {
        calendar.setTime(date);
        startOfWeek(calendar);
        return calendar.getTime();
    }

    /**
     * Adjusts the calendar to the start of the current month.
     * That is, first day of the month with all time fields cleared.
     * @param calendar
     */
    public static void startOfMonth(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        startOfDay(calendar);
    }

    /**
     * Adjusts the calendar to the end of the current month.
     * That is the last day of the month with all time-fields
     * at max.
     * 
     * @param calendar
     */
    public static void endOfMonth(Calendar calendar) {
        // start of next month
        calendar.add(Calendar.MONTH, 1);
        startOfMonth(calendar);
        // one millisecond back
        calendar.add(Calendar.MILLISECOND, -1);
    }



    /**
     * Adjust the given calendar to the first millisecond of the given date.
     * that is all time fields cleared. The Date of the adjusted Calendar is
     * returned. 
     * 
     * @param calendar calendar to adjust.
     * @param date the Date to use.
     * @return the start of the day of the given date
     */
    public static Date startOfDay(Calendar calendar, Date date) {
        calendar.setTime(date);
        startOfDay(calendar);
        return calendar.getTime();
    }


    /**
     * Adjust the given calendar to the last millisecond of the given date.
     * that is all time fields cleared. The Date of the adjusted Calendar is
     * returned. 
     * 
     * @param calendar calendar to adjust.
     * @param date the Date to use.
     * @return the end of the day of the given date
     */
    public static Date endOfDay(Calendar calendar, Date date) {
        calendar.setTime(date);
        endOfDay(calendar);
        return calendar.getTime();
    }


    /**
     * Adjust the given calendar to the first millisecond of the current day.
     * that is all time fields cleared.
     * 
     * @param calendar calendar to adjust.
     */
    public static void startOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
    }
    /**
     * Adjust the given calendar to the last millisecond of the specified date.
     * 
     * @param calendar calendar to adjust.
     */
    public static void endOfDay(Calendar calendar) {
        calendar.add(Calendar.DATE, 1);
        startOfDay(calendar);
        calendar.add(Calendar.MILLISECOND, -1);
    }

    /**
     * Checks the given dates for being equal.
     * 
     * @param current one of the dates to compare
     * @param date the otherr of the dates to compare
     * @return true if the two given dates both are null or both are not null and equal, 
     *  false otherwise.
     */
    public static boolean areEqual(Date current, Date date) {
        if ((date == null) && (current == null)) {
            return true;
        }
        if (date != null) {
           return date.equals(current);
        }
        return false;
    }

    /**
     * Returns a boolean indicating whether the given Date is the same day as
     * the day in the calendar. Calendar and date are unchanged by the check.
     *
     * @param today the Calendar representing a date, must not be null.
     * @param now the date to compare to, must not be null
     * @return true if the calendar and date represent the same day in the
     *   given calendar.
     */
    public static boolean isSameDay(Calendar today, Date now) {
        Calendar temp = (Calendar) today.clone();
        startOfDay(temp);
        Date start = temp.getTime();
        temp.setTime(now);
        startOfDay(temp);
        return start.equals(temp.getTime());
    }


}
