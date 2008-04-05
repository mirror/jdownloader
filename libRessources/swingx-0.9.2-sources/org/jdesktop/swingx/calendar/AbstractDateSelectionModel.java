/*
 * $Id: AbstractDateSelectionModel.java,v 1.5 2008/01/29 16:04:46 kleopatra Exp $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import org.jdesktop.swingx.event.DateSelectionEvent;
import org.jdesktop.swingx.event.DateSelectionListener;
import org.jdesktop.swingx.event.EventListenerMap;
import org.jdesktop.swingx.event.DateSelectionEvent.EventType;

/**
 * Abstract base implementation of DateSelectionModel. Implements
 * notification and Calendar related properties.
 * 
 * @author Jeanette Winzenburg
 */
public abstract class AbstractDateSelectionModel implements DateSelectionModel {
    public static final SortedSet<Date> EMPTY_DATES = Collections.unmodifiableSortedSet(new TreeSet<Date>());
    
    protected EventListenerMap listenerMap;
    protected boolean adjusting;
    protected Calendar calendar;
    /** 
     * the locale used by the calendar. <p>
     * NOTE: need to keep separately as a Calendar has no getter.
     */
    protected Locale locale;

    /**
     * Instantiates a DateSelectionModel with default locale.
     */
    public AbstractDateSelectionModel() {
        this(null);
    }
    
    /**
     * Instantiates a DateSelectionModel with the given locale. If the locale is
     * null, the Locale's default is used.
     * 
     * PENDING JW: fall back to JComponent.getDefaultLocale instead? We use this
     *   with components anyway?
     * 
     * @param locale the Locale to use with this model, defaults to Locale.default()
     *    if null.
     */
    public AbstractDateSelectionModel(Locale locale) {
        this.listenerMap = new EventListenerMap();
        setLocale(locale);
    }

    /**
     * {@inheritDoc}
     */
    public Calendar getCalendar() {
        return (Calendar) calendar.clone();
    }

    /**
     * {@inheritDoc}
     */
    public int getFirstDayOfWeek() {
        return calendar.getFirstDayOfWeek();
    }

    /**
     * {@inheritDoc}
     */
    public void setFirstDayOfWeek(final int firstDayOfWeek) {
        if (firstDayOfWeek == getFirstDayOfWeek()) return;
        calendar.setFirstDayOfWeek(firstDayOfWeek);
        fireValueChanged(EventType.CALENDAR_CHANGED);
    }

    /**
     * {@inheritDoc}
     */
    public int getMinimalDaysInFirstWeek() {
        return calendar.getMinimalDaysInFirstWeek();
    }

    /**
     * {@inheritDoc}
     */
    public void setMinimalDaysInFirstWeek(int minimalDays) {
        if (minimalDays == getMinimalDaysInFirstWeek()) return;
        calendar.setMinimalDaysInFirstWeek(minimalDays);
        fireValueChanged(EventType.CALENDAR_CHANGED);
    }

    
    /**
     * {@inheritDoc}
     */
    public TimeZone getTimeZone() {
        return calendar.getTimeZone();
    }

    /**
     * {@inheritDoc}
     */
    public void setTimeZone(TimeZone timeZone) {
        if (getTimeZone().equals(timeZone)) return;
        TimeZone oldTimeZone = getTimeZone();
        calendar.setTimeZone(timeZone);
        adjustDatesToTimeZone(oldTimeZone);
        fireValueChanged(EventType.CALENDAR_CHANGED);
    }

    /**
     * Adjusts all stored dates to a new time zone.
     * This method is called after the change had been made. <p>
     * 
     * This implementation resets all dates to null, clears everything. 
     * Subclasses may override to really map to the new time zone.
     *
     * @param oldTimeZone the old time zone
     * 
     */
    protected void adjustDatesToTimeZone(TimeZone oldTimeZone) {
        clearSelection();
        setLowerBound(null);
        setUpperBound(null);
        setUnselectableDates(EMPTY_DATES);
    }
    
    /**
     * {@inheritDoc}
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * {@inheritDoc}
     */
    public void setLocale(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        if (locale.equals(getLocale())) return;
        this.locale = locale;
        if (calendar != null) {
            calendar = Calendar.getInstance(calendar.getTimeZone(), locale);
        } else {
            calendar = Calendar.getInstance(locale);
        }
        fireValueChanged(EventType.CALENDAR_CHANGED);
    }

//------------------- utility methods
    
    /**
     * Returns the start of the day of the given date in this model's calendar.
     * NOTE: the calendar is changed by this operation.
     * 
     * @param date the Date to get the start for.
     * @return the Date representing the start of the day of the input date.
     */
    protected Date startOfDay(Date date) {
        return CalendarUtils.startOfDay(calendar, date);
    }

    /**
     * Returns the end of the day of the given date in this model's calendar.
     * NOTE: the calendar is changed by this operation.
     * 
     * @param date the Date to get the start for.
     * @return the Date representing the end of the day of the input date.
     */
    protected Date endOfDay(Date date) {
        return CalendarUtils.endOfDay(calendar, date);
    }

    /**
     * Returns a boolean indicating whether the given dates are on the same day in
     * the coordinates of the model's calendar.
     * 
     * @param selected one of the dates to check, must not be null.
     * @param compare the other of the dates to check, must not be null.
     * @return true if both dates represent the same day in this model's calendar.
     */
    protected boolean isSameDay(Date selected, Date compare) {
        return startOfDay(selected).equals(startOfDay(compare));
    }

//------------------- listeners
    
    /**
     * {@inheritDoc}
     */
    public boolean isAdjusting() {
        return adjusting;
    }

    /**
     * {@inheritDoc}
     */
    public void setAdjusting(boolean adjusting) {
        if (adjusting == isAdjusting()) return;
        this.adjusting = adjusting;
       fireValueChanged(adjusting ? EventType.ADJUSTING_STARTED : EventType.ADJUSTING_STOPPED);
        
    }

//----------------- notification    
    /**
     * {@inheritDoc}
     */
    public void addDateSelectionListener(DateSelectionListener l) {
        listenerMap.add(DateSelectionListener.class, l);
    }

    /**
     * {@inheritDoc}
     */
    public void removeDateSelectionListener(DateSelectionListener l) {
        listenerMap.remove(DateSelectionListener.class, l);
    }

    public List<DateSelectionListener> getDateSelectionListeners() {
        return listenerMap.getListeners(DateSelectionListener.class);
    }

    protected void fireValueChanged(DateSelectionEvent.EventType eventType) {
        List<DateSelectionListener> listeners = getDateSelectionListeners();
        DateSelectionEvent e = null;

        for (DateSelectionListener listener : listeners) {
            if (e == null) {
                e = new DateSelectionEvent(this, eventType, isAdjusting());
            }
            listener.valueChanged(e);
        }
    }



}
