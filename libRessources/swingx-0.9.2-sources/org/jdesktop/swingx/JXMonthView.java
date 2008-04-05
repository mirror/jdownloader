/*
 * $Id: JXMonthView.java,v 1.52 2008/02/27 11:57:20 kleopatra Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
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
 */
package org.jdesktop.swingx;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.UIManager;

import org.jdesktop.swingx.calendar.CalendarUtils;
import org.jdesktop.swingx.calendar.DateSelectionModel;
import org.jdesktop.swingx.calendar.DaySelectionModel;
import org.jdesktop.swingx.calendar.DateSelectionModel.SelectionMode;
import org.jdesktop.swingx.event.DateSelectionEvent;
import org.jdesktop.swingx.event.DateSelectionListener;
import org.jdesktop.swingx.event.EventListenerMap;
import org.jdesktop.swingx.event.DateSelectionEvent.EventType;
import org.jdesktop.swingx.plaf.LookAndFeelAddons;
import org.jdesktop.swingx.plaf.MonthViewAddon;
import org.jdesktop.swingx.plaf.MonthViewUI;
import org.jdesktop.swingx.util.Contract;


/**
 * Component that displays a month calendar which can be used to select a day
 * or range of days.  By default the <code>JXMonthView</code> will display a
 * single calendar using the current month and year, using
 * <code>Calendar.SUNDAY</code> as the first day of the week.
 * <p>
 * The <code>JXMonthView</code> can be configured to display more than one
 * calendar at a time by calling
 * <code>setPreferredCalCols</code>/<code>setPreferredCalRows</code>.  These
 * methods will set the preferred number of calendars to use in each
 * column/row.  As these values change, the <code>Dimension</code> returned
 * from <code>getMinimumSize</code> and <code>getPreferredSize</code> will
 * be updated.  The following example shows how to create a 2x2 view which is
 * contained within a <code>JFrame</code>:
 * <pre>
 *     JXMonthView monthView = new JXMonthView();
 *     monthView.setPreferredCols(2);
 *     monthView.setPreferredRows(2);
 *
 *     JFrame frame = new JFrame();
 *     frame.getContentPane().add(monthView);
 *     frame.pack();
 *     frame.setVisible(true);
 * </pre>
 * <p>
 * <code>JXMonthView</code> can be further configured to allow any day of the
 * week to be considered the first day of the week.  Character
 * representation of those days may also be set by providing an array of
 * strings.
 * <pre>
 *    monthView.setFirstDayOfWeek(Calendar.MONDAY);
 *    monthView.setDaysOfTheWeek(
 *            new String[]{"S", "M", "T", "W", "Th", "F", "S"});
 * </pre>
 * <p>
 * This component supports flagging days.  These flagged days are displayed
 * in a bold font.  This can be used to inform the user of such things as
 * scheduled appointment.
 * <pre><code>
 *    // Create some dates that we want to flag as being important.
 *    Calendar cal1 = Calendar.getInstance();
 *    cal1.set(2004, 1, 1);
 *    Calendar cal2 = Calendar.getInstance();
 *    cal2.set(2004, 1, 5);
 *
 *    monthView.setFlaggedDates(cal1.getTime(), cal2.getTime(), new Date());
 * </code></pre>
 * Applications may have the need to allow users to select different ranges of
 * dates.  There are three modes of selection that are supported, single, single interval
 * and multiple interval selection.  Once a selection is made an DateSelectionEvent is
 * fired to inform listeners of the change.
 * <pre>
 *    // Change the selection mode to select full weeks.
 *    monthView.setSelectionMode(SelectionMode.SINGLE_INTERVAL_SELECTION);
 *
 *    // Register a date selection listener to get notified about
 *    // any changes in the date selection model.
 *    monthView.getSelectionModel().addDateSelectionListener(new DateSelectionListener {
 *        public void valueChanged(DateSelectionEvent e) {
 *            System.out.println(e.getSelection());
 *        }
 *    });
 * </pre>
 * 
 * NOTE (for users of earlier versions): as of version 1.19 control about selection 
 * dates is moved completely into the model. The default model used is of type 
 * DaySelectionModel, which handles dates in the same way the JXMonthView did earlier
 * (that is, normalize all to the start of the day, which means zeroing all time
 * fields).<p>
 * 
 * NOTE: all methods taking/returning millis are deprecated (or will be as soon as they
 * have equivalents taking/returning Date) and typically less-maintained then the Date methods.
 * We highlighly recommend to not use them, if there's an alternative - in SwingX, all
 * deprecated API will be removed before final!
 *  
 * @author Joshua Outwater
 * @author Jeanette Winzenburg
 * @version  $Revision: 1.52 $
 */
public class JXMonthView extends JComponent {
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(JXMonthView.class
            .getName());
    /*
     * moved from package calendar to swingx at version 1.51
     */

    /** action command used for commit actionEvent. */
    public static final String COMMIT_KEY = "monthViewCommit";
    /** action command used for cancel actionEvent. */
    public static final String CANCEL_KEY = "monthViewCancel";

    public static final String BOX_PADDING_X = "boxPaddingX";
    public static final String BOX_PADDING_Y = "boxPaddingY";
    public static final String DAYS_OF_THE_WEEK = "daysOfTheWeek";
    public static final String SELECTION_MODEL = "selectionModel";
    public static final String TRAVERSABLE = "traversable";
    public static final String FLAGGED_DATES = "flaggedDates";

    /** @deprecated use hardcoded property name - this violated naming convention #751.*/
    @Deprecated
    public static final String SHOW_LEADING_DATES = "showLeadingDates";
    /** @deprecated use hardcoded property name - this violated naming convention #751.*/
    @Deprecated
    public static final String SHOW_TRAILING_DATES = "showTrailingDates";
    /** @deprecated use hardcoded property name - this violated naming convention #751.*/
    @Deprecated
    public static final String WEEK_NUMBER = "weekNumber";
    /** @deprecated use hardcoded property name, changed to "firstDisplayedDay". */
    @Deprecated
    public static final String FIRST_DISPLAYED_DATE = "firstDisplayedDate";
    /** Return value used to identify when the month down button is pressed. 
     * @deprecated moved to ui
     */
    @Deprecated
    public static final int MONTH_DOWN = 1;
    /** Return value used to identify when the month up button is pressed. 
     * @deprecated moved to ui
     */
    @Deprecated
    public static final int MONTH_UP = 2;

    static {
        LookAndFeelAddons.contribute(new MonthViewAddon());
    }

     /**
     * UI Class ID
     */
    public static final String uiClassID = "MonthViewUI";

    public static final int DAYS_IN_WEEK = 7;
    public static final int MONTHS_IN_YEAR = 12;


    /**
     * Keeps track of the first date we are displaying.  We use this as a
     * restore point for the calendar. This is normalized to the start of the
     * first day of the month given in setFirstDisplayedDate.
     */
    private Date firstDisplayedDay;
    /** 
     * the calendar to base all selections, flagging upon. 
     * NOTE: the time of this calendar is undefined - before using, internal
     * code must explicitly set it.
     * PENDING JW: as of version 1.26 all calendar/properties are controlled by the model.
     * We keep a clone of the model's calendar here for notification reasons: 
     * model fires DateSelectionEvent of type CALENDAR_CHANGED which neiter carry the
     * oldvalue nor the property name needed to map into propertyChange notification.
     */
    private Calendar cal;
    /** calendar to store the real input of firstDisplayedDate. */
    private Calendar anchor;
    /** 
     * Start of the day which contains System.millis() in the current calendar.
     * Kept in synch via a timer started in addNotify.
     */
    private Date today;
    /**
     * The timer used to keep today in synch with system time.
     */
    private Timer todayTimer;
    // PENDING JW: why kept apart from cal? Why writable? - shouldn't the calendar have complete
    // control?
    private int firstDayOfWeek;
    //-------------- selection/flagging
    /** 
     * The DateSelectionModel driving this component. This model's calendar
     * is the reference for all dates.
     */
    private DateSelectionModel model;
    /**
     * Listener registered with the current model to keep Calendar dependent
     * state synched.
     */
    private DateSelectionListener modelListener;
    /** 
     * The manager of the flagged dates. Note
     * that the type of this is an implementation detail.  
     */
    private DaySelectionModel flaggedDates;
    /**
     * Storage of actionListeners registered with the monthView.
     */
    private EventListenerMap listenerMap;
    
    private boolean traversable;
    private boolean leadingDays;
    private boolean trailingDays;
    private boolean showWeekNumber;
    private boolean componentInputMapEnabled;
    
    //-------------------
    // PENDING JW: ??
    @SuppressWarnings({"FieldCanBeLocal"})
    protected Date modifiedStartDate;
    @SuppressWarnings({"FieldCanBeLocal"})
    protected Date modifiedEndDate;
    
    //------------- visuals
    
    /**
     * localizable day column headers. Default typically installed by the uidelegate.
     */
    private String[] _daysOfTheWeek;
    /**
     * Insets used in determining the rectangle for the month string
     * background.
     */
    private boolean antialiased;
    protected Insets _monthStringInsets = new Insets(0, 0, 0, 0);
    private int boxPaddingX;
    private int boxPaddingY;
    private int minCalCols = 1;
    private int minCalRows = 1;
    private Color todayBackgroundColor;
    private Color monthStringBackground;
    private Color monthStringForeground;
    private Color daysOfTheWeekForeground;
    private Color selectedBackground;
    private String actionCommand = "selectionChanged";
    private Hashtable<Integer, Color> dayToColorTable = new Hashtable<Integer, Color>();
    private Color flaggedDayForeground;

    /**
     * Create a new instance of the <code>JXMonthView</code> class using the
     * default Locale and the current system time as the first date to 
     * display.
     */
    public JXMonthView() {
        this(new Date(System.currentTimeMillis()), null, null);
    }

    /**
     * Create a new instance of the <code>JXMonthView</code> class using the 
     * default Locale and the current system time as the first date to 
     * display.
     * 
     * @param locale desired locale, if null the system default locale is used
     */
    public JXMonthView(final Locale locale) {
        this(new Date(System.currentTimeMillis()), null, locale);
    }

    /**
     * Create a new instance of the <code>JXMonthView</code> class using the
     * default Locale and the given time as the first date to 
     * display.
     *
     * @param firstDisplayedDate The first month to display.
     */
    public JXMonthView(Date firstDisplayedDate) {
        this(firstDisplayedDate, null, null);
    }

    /**
     * Create a new instance of the <code>JXMonthView</code> class using the
     * default Locale, the given time as the first date to 
     * display and the given selection model. 
     * 
     * @param firstDisplayedDate The first month to display.
     * @param model the selection model to use, if null a <code>DefaultSelectionModel</code> is
     *   created.
     */
    public JXMonthView(Date firstDisplayedDate, final DateSelectionModel model) {
        this(firstDisplayedDate, model, null);
    }


    /**
     * Create a new instance of the <code>JXMonthView</code> class using the
     * given Locale, the given time as the first date to 
     * display and the given selection model. 
     * 
     * @param firstDisplayedDay 
     * @param model the selection model to use, if null a <code>DefaultSelectionModel</code> is
     *   created.
     * @param locale desired locale, if null the system default locale is used
     */
    public JXMonthView(Date firstDisplayedDay, final DateSelectionModel model, final Locale locale) {
        super();
        listenerMap = new EventListenerMap();

        initModel(model, locale);
        superSetLocale(locale);
        setFirstDisplayedDay(firstDisplayedDay);
        // Keep track of today
        updateTodayFromCurrentTime();

        // install the controller
        updateUI();

        setFocusable(true);
        todayBackgroundColor = getForeground();

    }

    
//------------------ Calendar related properties
    
    /**
     * Sets locale and resets text and format used to display months and days. 
     * Also resets firstDayOfWeek. <p>
     * 
     * PENDING JW: the following warning should be obsolete (installCalendar
     * should take care) - check if it really is!
     * 
     * <p>
     * <b>Warning:</b> Since this resets any string labels that are cached in UI
     * (month and day names) and firstDayofWeek, use <code>setDaysOfTheWeek</code> and/or
     * setFirstDayOfWeek after (re)setting locale.
     * </p>
     * 
     * @param   locale new Locale to be used for formatting
     * @see     #setDaysOfTheWeek(String[])
     * @see     #setFirstDayOfWeek(int)
     */
    @Override
    public void setLocale(Locale locale) {
        model.setLocale(locale);
    }

    /**
     * 
     * @param locale
     */
    private void superSetLocale(Locale locale) {
        // PENDING JW: formally, a null value is allowed and must be passed on to super
        // I suspect this is not done here to keep the logic out off the constructor?
        // 
        if (locale != null) {
            super.setLocale(locale);
            repaint();
       }
    }
    
    /**
     * Returns a clone of the internal calendar, with it's time set to firstDisplayedDate.
     * 
     * PENDING: firstDisplayed useful as reference time? It's timezone dependent anyway. 
     * Think: could return with monthView's today instead? 
     * 
     * @return a clone of internal calendar, configured to the current firstDisplayedDate
     * @throws IllegalStateException if called before instantitation is completed
     */
    public Calendar getCalendar() {
        // JW: this is to guard against a regression of not-fully understood 
        // problems in constructor (UI used to call back into this before we were ready)
        if (cal == null) throw 
            new IllegalStateException("must not be called before instantiation is complete");
        Calendar calendar = (Calendar) cal.clone();
        calendar.setTime(firstDisplayedDay);
        return calendar;
    }

    /**
     * Gets the time zone.
     *
     * @return The <code>TimeZone</code> used by the <code>JXMonthView</code>.
     */
    public TimeZone getTimeZone() {
        return cal.getTimeZone();
    }

    /**
     * Sets the time zone with the given time zone value.
     * 
     * This is a bound property. 
     * 
     * @param tz The <code>TimeZone</code>.
     */
    public void setTimeZone(TimeZone tz) {
        model.setTimeZone(tz);
    }

    /**
     * Gets what the first day of the week is; e.g.,
     * <code>Calendar.SUNDAY</code> in the U.S., <code>Calendar.MONDAY</code>
     * in France.
     *
     * @return int The first day of the week.
     */
    public int getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    /**
     * Sets what the first day of the week is; e.g.,
     * <code>Calendar.SUNDAY</code> in US, <code>Calendar.MONDAY</code>
     * in France.
     *
     * @param firstDayOfWeek The first day of the week.
     * @see java.util.Calendar
     */
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        getSelectionModel().setFirstDayOfWeek(firstDayOfWeek);
    }



//---------------------- synch to model's calendar    

    /**
     * Initializes selection model related internals. If the Locale is
     * null, it falls back to JComponent.defaultLocale. If the model
     * is null it creates a default model with the locale.
     * 
     * PENDING JW: leave default locale fallback to model?
     * 
     * @param model the DateSelectionModel which should drive the monthView. 
     *    If null, a default model is created and initialized with the given locale.
     * @param locale the Locale to use with the selectionModel. If null,
     *   JComponent.getDefaultLocale is used.
     */
    private void initModel(DateSelectionModel model, Locale locale) {
        if (locale == null) {
            locale = JComponent.getDefaultLocale();
        }
        if (model == null) {
            model = new DaySelectionModel(locale);
        }
        this.model = model;
        // PENDING JW: do better to synchronize Calendar related 
        // properties of flaggedDates to those of the selection model.
        // plus: should use the same normalization?
        this.flaggedDates = new DaySelectionModel(locale);
        flaggedDates.setSelectionMode(SelectionMode.MULTIPLE_INTERVAL_SELECTION);
        
        installCalendar();
        model.addDateSelectionListener(getDateSelectionListener());
    }

    /**
     * Lazily creates and returns the DateSelectionListener which listens
     * for model's calendar properties.
     * 
     * @return a DateSelectionListener for model's CALENDAR_CHANGED notification.
     */
    private DateSelectionListener getDateSelectionListener() {
        if (modelListener == null) {
            modelListener = new DateSelectionListener() {

                public void valueChanged(DateSelectionEvent ev) {
                    if (EventType.CALENDAR_CHANGED.equals(ev.getEventType())) {
                        updateCalendar();
                    }
                    
                }
                
            };
        }
        return modelListener;
    }

    /**
     * Installs the internal calendars from the selection model.
     * 
     */
    private void installCalendar() {
        cal = model.getCalendar();
        firstDayOfWeek = cal.getFirstDayOfWeek();
        anchor = (Calendar) cal.clone();
    }

    /**
     * Returns the anchor date. Currently, this is the "uncleaned" input date 
     * of setFirstDisplayedDate. This is a quick hack for Issue #618-swingx, to
     * have some invariant for testing. Do not use in client code, may change
     * without notice!
     * 
     * @return the "uncleaned" first display date.
     */
    protected Date getAnchorDate() {
        return anchor.getTime();
    }

    /**
     * Callback from selection model calendar changes.
     */
    private void updateCalendar() {
       if (!getLocale().equals(model.getLocale())) {
           installCalendar();
           superSetLocale(model.getLocale());
       } else {
           if (!model.getTimeZone().equals(getTimeZone())) {
               updateTimeZone();
           }
           if (cal.getMinimalDaysInFirstWeek() != model.getMinimalDaysInFirstWeek()) {
               updateMinimalDaysOfFirstWeek();
           }
           if (cal.getFirstDayOfWeek() != model.getFirstDayOfWeek()) {
              updateFirstDayOfWeek(); 
           }
       }
    }


    /**
     * Callback from changing timezone in model.
     */
    private void updateTimeZone() {
        TimeZone old = getTimeZone();
        TimeZone tz = model.getTimeZone();
        cal.setTimeZone(tz);
        anchor.setTimeZone(tz);
        setFirstDisplayedDay(anchor.getTime());
        updateTodayFromCurrentTime();
        updateDatesAfterTimeZoneChange(old);
        firePropertyChange("timeZone", old, getTimeZone());
    }
    
    /**
     * All dates are "cleaned" relative to the timezone they had been set.
     * After changing the timezone, they need to be updated to the new.
     * 
     * Here: clear everything. 
     * 
     * @param oldTimeZone the timezone before the change
     */
    protected void updateDatesAfterTimeZoneChange(TimeZone oldTimeZone) {
        SortedSet<Date> flagged = getFlaggedDates();
        flaggedDates.setTimeZone(getTimeZone());
        firePropertyChange("flaggedDates", flagged, getFlaggedDates());
     }
    
    /**
     * Call back from listening to model firstDayOfWeek change.
     */
    private void updateFirstDayOfWeek() {
        int oldFirstDayOfWeek = this.firstDayOfWeek;

        firstDayOfWeek = getSelectionModel().getFirstDayOfWeek();
        cal.setFirstDayOfWeek(firstDayOfWeek);
        anchor.setFirstDayOfWeek(firstDayOfWeek);
        firePropertyChange("firstDayOfWeek", oldFirstDayOfWeek, firstDayOfWeek);
    }

    /**
     * Call back from listening to model firstDayOfWeek change.
     */
    private void updateMinimalDaysOfFirstWeek() {
        cal.setMinimalDaysInFirstWeek(model.getMinimalDaysInFirstWeek());
        anchor.setMinimalDaysInFirstWeek(model.getMinimalDaysInFirstWeek());
    }


    
//-------------------- scrolling
    /**
     * Returns the last date able to be displayed.  For example, if the last
     * visible month was April the time returned would be April 30, 23:59:59.
     *
     * @return long The last displayed date.
     */
    public Date getLastDisplayedDay() {
        return getUI().getLastDisplayedDay();
    }

    
    /**
     * Returns the first displayed date.
     *
     * @return long The first displayed date.
     */
    public Date getFirstDisplayedDay() {
        return firstDisplayedDay;
    }

    
    /**
     * Set the first displayed date.  We only use the month and year of
     * this date.  The <code>Calendar.DAY_OF_MONTH</code> field is reset to
     * 1 and all other fields, with exception of the year and month,
     * are reset to 0.
     *
     * @param date The first displayed date.
     */
    public void setFirstDisplayedDay(Date date) {
        anchor.setTime(date);
        Date oldDate = getFirstDisplayedDay();

        cal.setTime(anchor.getTime());
        CalendarUtils.startOfMonth(cal);
        firstDisplayedDay = cal.getTime();

        firePropertyChange("firstDisplayedDay", oldDate, getFirstDisplayedDay() );
        // JW: need to fire two events until the deprecated firstDisplayedDate is removed!
        long oldFirstDisplayedDate = oldDate != null ? oldDate.getTime() : 0;
        firePropertyChange(FIRST_DISPLAYED_DATE, 
                oldFirstDisplayedDate, 
                firstDisplayedDay.getTime());
    }



    /**
     * Moves the <code>date</code> into the visible region of the calendar. If
     * the date is greater than the last visible date it will become the last
     * visible date. While if it is less than the first visible date it will
     * become the first visible date. <p>
     * 
     * NOTE: this is the recommended method to scroll to a particular date, the
     * functionally equivalent method taking a long as parameter will most 
     * probably be deprecated.
     * 
     * @param date Date to make visible, must not be null.
     * @see #ensureDateVisible(long)
     */
    public void ensureDateVisible(Date date) {
        if (date.before(firstDisplayedDay)) {
            setFirstDisplayedDay(date);
        } else {
            Date lastDisplayedDate = getLastDisplayedDay();
            if (date.after(lastDisplayedDate)) {
                // extract to CalendarUtils!
                cal.setTime(date);
                int month = cal.get(Calendar.MONTH);
                int year = cal.get(Calendar.YEAR);

                cal.setTime(lastDisplayedDate);
                int lastMonth = cal.get(Calendar.MONTH);
                int lastYear = cal.get(Calendar.YEAR);

                int diffMonths = month - lastMonth
                        + ((year - lastYear) * MONTHS_IN_YEAR);

                cal.setTime(firstDisplayedDay);
                cal.add(Calendar.MONTH, diffMonths);
                setFirstDisplayedDay(cal.getTime());
            }
        }
    }


    /**
     * Returns the Date at the given location. May be null if the
     * coordinates don't map to a day in the month which contains the 
     * coordinates. Specifically: hitting leading/trailing dates returns null.
     * 
     * Mapping pixel to calendar day.
     *
     * @param x the x position of the location in pixel
     * @param y the y position of the location in pixel
     * @return the day at the given location or null if the location
     *   doesn't map to a day in the month which contains the coordinates.
     */ 
    public Date getDayAtLocation(int x, int y) {
        return getUI().getDayAtLocation(x, y);
    }
   
//------------------ today
    
    /**
     * Sets today from the current system time. 
     * 
     * temporary widened access for testing.
     */
    protected void updateTodayFromCurrentTime() {
        setToday(new Date(System.currentTimeMillis()));
    }

    /**
     * Increments today. This is used by the timer.
     * 
     * PENDING: is it safe? doesn't check if we are really tomorrow?
     * temporary widened access for testing.
     */
    protected void incrementToday() {
        cal.setTime(getToday());
        cal.add(Calendar.DAY_OF_MONTH, 1);
        setToday(cal.getTime());
    }

    /**
     * Sets the date which represents today. Internally 
     * modified to the start of the day which contains the
     * given date in this monthView's calendar coordinates.
     *  
     * temporary widened access for testing.
     * 
     * @param date the date which should be used as today.
     */
    protected void setToday(Date date) {
        Date oldToday = getToday();
        // PENDING JW: do we really want the start of today? 
        this.today = startOfDay(date);
        // PENDING JW: need notification for millis property until we
        // remove it!
        firePropertyChange("todayInMillis", 
                oldToday != null ? oldToday.getTime() : 0, getToday().getTime());
        firePropertyChange("today", oldToday, getToday());
    }

    /**
     * Returns the start of today in this monthviews calendar coordinates.
     * 
     * @return the start of today as Date.
     */
    public Date getToday() {
        // null only happens in the very first time ... 
        return today != null ? (Date) today.clone() : null;
    }

    
    
//----   internal date manipulation ("cleanup" == start of day in monthView's calendar)
    

    /**
     * Returns the start of the day as Date.
     * 
     * @param date the Date.
     * @return start of the given day as Date, relative to this
     *    monthView's calendar.
     *    
     */
    private Date startOfDay(Date date) {
        return CalendarUtils.startOfDay(cal, date);
    }

//------------------- ui delegate    
    /**
     * @inheritDoc
     */
    public MonthViewUI getUI() {
        return (MonthViewUI)ui;
    }

    /**
     * Sets the L&F object that renders this component.
     *
     * @param ui UI to use for this {@code JXMonthView}
     */
    public void setUI(MonthViewUI ui) {
        super.setUI(ui);
    }

    /**
     * Resets the UI property with the value from the current look and feel.
     *
     * @see UIManager#getUI(JComponent)
     */
    @Override
    public void updateUI() {
        setUI((MonthViewUI)LookAndFeelAddons.getUI(this, MonthViewUI.class));
        invalidate();
    }

    /**
     * @inheritDoc
     */
    @Override
    public String getUIClassID() {
        return uiClassID;
    }

    
//---------------- DateSelectionModel

    /**
     * Returns the date selection model which drives this
     * JXMonthView.
     * 
     * @return the date selection model
     */
    public DateSelectionModel getSelectionModel() {
        return model;
    }

    /**
     * Sets the date selection model to drive this monthView.
     * 
     * @param model the selection model to use, must not be null.
     * @throws NullPointerException if model is null
     */
    public void setSelectionModel(DateSelectionModel model) {
        Contract.asNotNull(model, "date selection model must not be null");
        DateSelectionModel oldModel = this.model;
        model.removeDateSelectionListener(getDateSelectionListener());
        this.model = model;
        installCalendar();
        if (!model.getLocale().equals(getLocale())) {
            super.setLocale(model.getLocale());
        }
        model.addDateSelectionListener(getDateSelectionListener());
        firePropertyChange(SELECTION_MODEL, oldModel, model);
    }

//-------------------- delegates to model
    
    /**
     * Clear any selection from the selection model
     */
    public void clearSelection() {
        getSelectionModel().clearSelection();
    }

    /**
     * Return true if the selection is empty, false otherwise
     *
     * @return true if the selection is empty, false otherwise
     */
    public boolean isSelectionEmpty() {
        return getSelectionModel().isSelectionEmpty();
    }

    /**
     * Get the current selection
     *
     * @return sorted set of selected dates
     */
   public SortedSet<Date> getSelection() {
        return getSelectionModel().getSelection();
    }

    /**
     * Adds the selection interval to the selection model. 
     * 
     * @param startDate Start of date range to add to the selection
     * @param endDate End of date range to add to the selection
     */
    public void addSelectionInterval(Date startDate, Date endDate) {
            getSelectionModel().addSelectionInterval(startDate, endDate);
    }

    /**
     * Sets the selection interval to the selection model.  
     *
     * @param startDate Start of date range to set the selection to
     * @param endDate End of date range to set the selection to
     */
    public void setSelectionInterval(final Date startDate, final Date endDate) {
            getSelectionModel().setSelectionInterval(startDate, endDate);
    }

    /**
     * Removes the selection interval from the selection model.  
     * 
     * @param startDate Start of the date range to remove from the selection
     * @param endDate End of the date range to remove from the selection
     */
    public void removeSelectionInterval(final Date startDate, final Date endDate) {
        getSelectionModel().removeSelectionInterval(startDate, endDate);
    }

    /**
     * Returns the current selection mode for this JXMonthView.
     *
     * @return int Selection mode.
     */
    public SelectionMode getSelectionMode() {
        return getSelectionModel().getSelectionMode();
    }

    /**
     * Set the selection mode for this JXMonthView.

     * @param selectionMode The selection mode to use for this {@code JXMonthView}
     */
    public void setSelectionMode(final SelectionMode selectionMode) {
        getSelectionModel().setSelectionMode(selectionMode);
    }

    /**
     * Returns the earliest selected date. 
     * 
     *   
     * @return the first Date in the selection or null if empty.
     */
    public Date getFirstSelectionDate() {
        return getSelectionModel().getFirstSelectionDate();    
     }
   

    /**
     * Returns the earliest selected date. 
     * 
     * @return the first Date in the selection or null if empty.
     */
    public Date getLastSelectionDate() {
        return getSelectionModel().getLastSelectionDate();    
     }

    /**
     * Returns the earliest selected date. 
     * 
     * PENDING JW: keep this? it was introduced before the first/last 
     *   in model. When delegating everything, we duplicate here.
     *   
     * @return the first Date in the selection or null if empty.
     */
    public Date getSelectionDate() {
        return getFirstSelectionDate();    
    }

    /**
     * Sets the model's selection to the given date or clears the selection if
     * null.
     * 
     * @param newDate the selection date to set
     */
    public void setSelectionDate(Date newDate) {
        if (newDate == null) {
            clearSelection();
        } else {
            setSelectionInterval(newDate, newDate);
        }
    }

    /**
     * Returns true if the specified date falls within the _startSelectedDate
     * and _endSelectedDate range.  
     *
     * @param date The date to check
     * @return true if the date is selected, false otherwise
     */
    public boolean isSelected(Date date) {
        return getSelectionModel().isSelected(date);
    }


    /**
     * Set the lower bound date that is allowed to be selected. <p>
     * 
     * 
     * @param lowerBound the lower bound, null means none.
     */
    public void setLowerBound(Date lowerBound) {
        getSelectionModel().setLowerBound(lowerBound);
    }

    /**
     * Set the upper bound date that is allowed to be selected. <p>
     * 
     * @param upperBound the upper bound, null means none.
     */
    public void setUpperBound(Date upperBound) {
        getSelectionModel().setUpperBound(upperBound);
    }


    /**
     * Return the lower bound date that is allowed to be selected for this
     * model.
     *
     * @return lower bound date or null if not set
     */
    public Date getLowerBound() {
        return getSelectionModel().getLowerBound();
    }

    /**
     * Return the upper bound date that is allowed to be selected for this
     * model.
     *
     * @return upper bound date or null if not set
     */
    public Date getUpperBound() {
        return getSelectionModel().getUpperBound();
    }

    /**
     * Identifies whether or not the date passed is an unselectable date.
     * <p>
     * 
     * @param date date which to test for unselectable status
     * @return true if the date is unselectable, false otherwise
     */
    public boolean isUnselectableDate(Date date) {
        return getSelectionModel().isUnselectableDate(date);
    }

    /**
     * Sets the dates that should be unselectable. This will replace the model's
     * current set of unselectable dates. The implication is that calling with
     * zero dates will remove all unselectable dates.
     * <p>
     * 
     * NOTE: neither the given array nor any of its elements must be null.
     * 
     * @param unselectableDates zero or more not-null dates that should be
     *        unselectable.
     * @throws NullPointerException if either the array or any of the elements
     *         are null
     */
    public void setUnselectableDates(Date... unselectableDates) {
        Contract.asNotNull(unselectableDates,
                "unselectable dates must not be null");
        SortedSet<Date> unselectableSet = new TreeSet<Date>();
        for (Date unselectableDate : unselectableDates) {
            unselectableSet.add(unselectableDate);
        }
        getSelectionModel().setUnselectableDates(unselectableSet);
        // PENDING JW: check that ui does the repaint!
        repaint();
    }

//--------------------- flagged dates
    /**
     * Identifies whether or not the date passed is a flagged date.  <b>All dates are modified to remove their hour of
     * day, minute, second, and millisecond before being added to the selection model</b>
     *
     * @param date date which to test for flagged status 
     * @return true if the date is flagged, false otherwise
     */
    public boolean isFlaggedDate(Date date) {
        if (date == null) return false;
        return flaggedDates.isSelected(date);
    }
    
    /**
     * Replace all flags with the given dates.<p>
     * 
     * NOTE: neither the given array nor any of its elements should be null.
     * Currently, a null array will be tolerated to ease migration. A null
     * has the same effect as clearFlaggedDates.
     * 
     *
     * @param flagged the dates to be flagged
     */
    public void setFlaggedDates(Date... flagged) {
//        Contract.asNotNull(flagged, "must not be null");
        SortedSet<Date> oldFlagged = flaggedDates.getSelection();
        flaggedDates.clearSelection();
        if (flagged != null) {
            for (Date date : flagged) {
                flaggedDates.addSelectionInterval(date, date);
            }
        }
        firePropertyChange("flaggedDates", oldFlagged, flaggedDates.getSelection());
   }
    /**
     * Adds the dates to the flags. 
     * 
     * NOTE: neither the given array nor any of its elements should be null.
     * Currently, a null array will be tolerated to ease migration. A null
     * does nothing.
     *
     * @param flagged the dates to be flagged
     */
    public void addFlaggedDates(Date... flagged) {
//        Contract.asNotNull(flagged, "must not be null");
        SortedSet<Date> oldFlagged = flaggedDates.getSelection();
        if (flagged != null) {
            for (Date date : flagged) {
                flaggedDates.addSelectionInterval(date, date);
            }
        }
        firePropertyChange("flaggedDates", oldFlagged, flaggedDates.getSelection());
    }
    
    /**
     * Unflags the given dates.
     * 
     * NOTE: neither the given array nor any of its elements should be null.
     * Currently, a null array will be tolerated to ease migration. 
     *
     * @param flagged the dates to be unflagged
     */
    public void removeFlaggedDates(Date... flagged) {
//        Contract.asNotNull(flagged, "must not be null");
        SortedSet<Date> oldFlagged = flaggedDates.getSelection();
        if (flagged != null) {
            for (Date date : flagged) {
                flaggedDates.removeSelectionInterval(date, date);
            }
        }
        firePropertyChange("flaggedDates", oldFlagged, flaggedDates.getSelection());
    }
    /**
     * Clears all flagged dates.
     * 
     */
    public void clearFlaggedDates() {
        SortedSet<Date> oldFlagged = flaggedDates.getSelection();
        flaggedDates.clearSelection();
        firePropertyChange("flaggedDates", oldFlagged, flaggedDates.getSelection());
    }
    
    /**
     * Returns a sorted set of flagged Dates. The returned set is guaranteed to
     * be not null, but may be empty.
     * 
     * @return a sorted set of flagged dates.
     */
    public SortedSet<Date> getFlaggedDates() {
        return flaggedDates.getSelection();
    }

    /**
     * Returns a boolean indicating if this monthView has flagged dates.
     * 
     * @return a boolean indicating if this monthView has flagged dates.
     */
    public boolean hasFlaggedDates() {
        return !flaggedDates.isSelectionEmpty();
    }


//------------------- visual properties    
    /**
     * Whether or not to show leading dates for a months displayed by this component.
     *
     * @param value true if leading dates should be displayed, false otherwise.
     */
    public void setShowingLeadingDays(boolean value) {
        if (leadingDays == value) {
            return;
        }

        leadingDays = value;
        firePropertyChange("showingLeadingDays", !leadingDays, leadingDays);
        // JW: fire the old event until the deprecated method is removed
        firePropertyChange("showLeadingDates", !leadingDays, leadingDays);
    }

    /**
     * Whether or not we're showing leading dates.
     *
     * @return true if leading dates are shown, false otherwise.
     */
    public boolean isShowingLeadingDays() {
        return leadingDays;
    }

    /**
     * Whether or not to show trailing dates for the months displayed by this component.
     *
     * @param value true if trailing dates should be displayed, false otherwise.
     */
    public void setShowingTrailingDays(boolean value) {
        if (trailingDays == value) {
            return;
        }

        trailingDays = value;
        firePropertyChange("showingTrailingDays", !trailingDays, trailingDays);
        // JW: fire the old event until the deprecated method is removed
        firePropertyChange("showTrailingDates", !trailingDays, trailingDays);
    }

    /**
     * Whether or not we're showing trailing dates.
     *
     * @return true if trailing dates are shown, false otherwise.
     */
    public boolean isShowingTrailingDays() {
        return trailingDays;
    }
    
    /**
     * Returns whether or not the month view supports traversing months.
     *
     * @return <code>true</code> if month traversing is enabled.
     */
    public boolean isTraversable() {
        return traversable;
    }

    /**
     * Set whether or not the month view will display buttons to allow the user
     * to traverse to previous or next months.
     * 
     * @param traversable set to true to enable month traversing, false
     *        otherwise.
     */
    public void setTraversable(boolean traversable) {
        if (traversable == this.traversable)
            return;
        this.traversable = traversable;
        firePropertyChange(TRAVERSABLE, !this.traversable, this.traversable);
    }

    /**
     * Returns whether or not this <code>JXMonthView</code> should display
     * week number.
     *
     * @return <code>true</code> if week numbers should be displayed
     */
    public boolean isShowingWeekNumber() {
        return showWeekNumber;
    }

    /**
     * Set whether or not this <code>JXMonthView</code> will display week
     * numbers or not.
     *
     * @param showWeekNumber true if week numbers should be displayed,
     *        false otherwise
     */
    public void setShowingWeekNumber(boolean showWeekNumber) {
        if (this.showWeekNumber == showWeekNumber) return;
        this.showWeekNumber = showWeekNumber;
        firePropertyChange("showingWeekNumber", !showWeekNumber, showWeekNumber);
    }

    /**
     * Sets the String representation for each day of the week as used
     * in the header of the day's grid. For
     * this method the first days of the week days[0] is assumed to be
     * <code>Calendar.SUNDAY</code>. If null, the representation provided
     * by the MonthViewUI is used.
     * 
     * The default value is the representation as 
     * returned from the MonthViewUI.
     * 
     * @param days Array of characters that represents each day
     * @throws IllegalArgumentException if not null and <code>days.length</code> !=
     *         DAYS_IN_WEEK
     */
    public void setDaysOfTheWeek(String[] days) {
        if ((days != null) && (days.length != DAYS_IN_WEEK)) {
            throw new IllegalArgumentException(
                    "Array of days is not of length " + DAYS_IN_WEEK
                            + " as expected.");
        }

        String[] oldValue = getDaysOfTheWeek();
        _daysOfTheWeek = days;
        firePropertyChange(DAYS_OF_THE_WEEK, oldValue, days);
    }

    /**
     * Returns the String representation for each day of the
     * week. 
     *
     * @return String representation for the days of the week, guaranteed to
     *   never be null.
     *   
     * @see #setDaysOfTheWeek(String[])
     * @see MonthViewUI  
     */
    public String[] getDaysOfTheWeek() {
        if (_daysOfTheWeek != null) {
            String[] days = new String[DAYS_IN_WEEK];
            System.arraycopy(_daysOfTheWeek, 0, days, 0, DAYS_IN_WEEK);
            return days;
        } 
        return getUI().getDaysOfTheWeek();
    }

    /**
     * Returns true if anti-aliased text is enabled for this component, false
     * otherwise.
     *
     * @return boolean <code>true</code> if anti-aliased text is enabled,
     * <code>false</code> otherwise.
     */
    public boolean isAntialiased() {
        return antialiased;
    }

    /**
     * Turns on/off anti-aliased text for this component.
     *
     * @param antiAlias <code>true</code> for anti-aliased text,
     * <code>false</code> to turn it off.
     */
    public void setAntialiased(boolean antiAlias) {
        if (this.antialiased == antiAlias) {
            return;
        }
        this.antialiased = antiAlias;
        firePropertyChange("antialiased", !this.antialiased, this.antialiased);
    }

    /**
     * Returns the padding used between days in the calendar.
     *
     * @return Padding used between days in the calendar
     */
    public int getBoxPaddingX() {
        return boxPaddingX;
    }

    /**
     * Sets the number of pixels used to pad the left and right side of a day.
     * The padding is applied to both sides of the days.  Therefore, if you
     * used the padding value of 3, the number of pixels between any two days
     * would be 6.
     *
     * @param boxPaddingX Number of pixels applied to both sides of a day
     */
    public void setBoxPaddingX(int boxPaddingX) {
        int oldBoxPadding = this.boxPaddingX;
        this.boxPaddingX = boxPaddingX;
        firePropertyChange(BOX_PADDING_X, oldBoxPadding, this.boxPaddingX);
    }

    /**
     * Returns the padding used above and below days in the calendar.
     *
     * @return Padding used between dats in the calendar
     */
    public int getBoxPaddingY() {
        return boxPaddingY;
    }

    /**
     * Sets the number of pixels used to pad the top and bottom of a day.
     * The padding is applied to both the top and bottom of a day.  Therefore,
     * if you used the padding value of 3, the number of pixels between any
     * two days would be 6.
     *
     * @param boxPaddingY Number of pixels applied to top and bottom of a day
     */
    public void setBoxPaddingY(int boxPaddingY) {
        int oldBoxPadding = this.boxPaddingY;
        this.boxPaddingY = boxPaddingY;
        firePropertyChange(BOX_PADDING_Y, oldBoxPadding, this.boxPaddingY);
    }


    /**
     * Returns the selected background color.
     *
     * @return the selected background color.
     */
    public Color getSelectedBackground() {
        return selectedBackground;
    }

    /**
     * Sets the selected background color to <code>c</code>.  The default color
     * is <code>138, 173, 209 (Blue-ish)</code>
     *
     * @param c Selected background.
     */
    public void setSelectedBackground(Color c) {
        selectedBackground = c;
        repaint();
    }

    /**
     * Returns the color used when painting the today background.
     *
     * @return Color Color
     */
    public Color getTodayBackground() {
        return todayBackgroundColor;
    }

    /**
     * Sets the color used to draw the bounding box around today.  The default
     * is the background of the <code>JXMonthView</code> component.
     *
     * @param c color to set
     */
    public void setTodayBackground(Color c) {
        todayBackgroundColor = c;
        repaint();
    }

    /**
     * Returns the color used to paint the month string background.
     *
     * @return Color Color.
     */
    public Color getMonthStringBackground() {
        return monthStringBackground;
    }

    /**
     * Sets the color used to draw the background of the month string.  The
     * default is <code>138, 173, 209 (Blue-ish)</code>.
     *
     * @param c color to set
     */
    public void setMonthStringBackground(Color c) {
        monthStringBackground = c;
        repaint();
    }

    /**
     * Returns the color used to paint the month string foreground.
     *
     * @return Color Color.
     */
    public Color getMonthStringForeground() {
        return monthStringForeground;
    }

    /**
     * Sets the color used to draw the foreground of the month string.  The
     * default is <code>Color.WHITE</code>.
     *
     * @param c color to set
     */
    public void setMonthStringForeground(Color c) {
        monthStringForeground = c;
        repaint();
    }

    /**
     * Sets the color used to draw the foreground of each day of the week. These
     * are the titles
     *
     * @param c color to set
     */
    public void setDaysOfTheWeekForeground(Color c) {
        daysOfTheWeekForeground = c;
        repaint();
    }

    /**
     * @return Color Color
     */
    public Color getDaysOfTheWeekForeground() {
        return daysOfTheWeekForeground;
    }

    /**
     * Set the color to be used for painting the specified day of the week.
     * Acceptable values are Calendar.SUNDAY - Calendar.SATURDAY.
     *
     * @param dayOfWeek constant value defining the day of the week.
     * @param c         The color to be used for painting the numeric day of the week.
     */
    public void setDayForeground(int dayOfWeek, Color c) {
        dayToColorTable.put(dayOfWeek, c);
        repaint();
    }

    /**
     * Return the color that should be used for painting the numerical day of the week.
     *
     * @param dayOfWeek The day of week to get the color for.
     * @return The color to be used for painting the numeric day of the week.
     *         If this was no color has yet been defined the component foreground color
     *         will be returned.
     */
    public Color getDayForeground(int dayOfWeek) {
        Color c;
        c = dayToColorTable.get(dayOfWeek);
        if (c == null) {
            c = getForeground();
        }
        return c;
    }

    /**
     * Set the color to be used for painting the foreground of a flagged day.
     *
     * @param c The color to be used for painting.
     */
    public void setFlaggedDayForeground(Color c) {
        flaggedDayForeground = c;
        repaint();
    }

    /**
     * Return the color that should be used for painting the foreground of the flagged day.
     *
     * @return The color to be used for painting
     */
    public Color getFlaggedDayForeground() {
        return flaggedDayForeground;
    }

    /**
     * Returns a copy of the insets used to paint the month string background.
     *
     * @return Insets Month string insets.
     */
    public Insets getMonthStringInsets() {
        return (Insets) _monthStringInsets.clone();
    }

    /**
     * Insets used to modify the width/height when painting the background
     * of the month string area.
     *
     * @param insets Insets
     */
    public void setMonthStringInsets(Insets insets) {
        if (insets == null) {
            _monthStringInsets.top = 0;
            _monthStringInsets.left = 0;
            _monthStringInsets.bottom = 0;
            _monthStringInsets.right = 0;
        } else {
            _monthStringInsets.top = insets.top;
            _monthStringInsets.left = insets.left;
            _monthStringInsets.bottom = insets.bottom;
            _monthStringInsets.right = insets.right;
        }
        repaint();
    }

    /**
     * Returns the preferred number of columns to paint calendars in. 
     * <p>
     * PENDING JW: rename to a "full" name preferredColumnCount
     * @return int Columns of calendars.
     */
    public int getPreferredCols() {
        return minCalCols;
    }

    /**
     * The preferred number of columns to paint calendars.
     * <p>
     * PENDING JW: rename to a "full" name preferredColumnCount
     *   and make bound property
     * @param cols The number of columns of calendars.
     */
    public void setPreferredCols(int cols) {
        if (cols <= 0) {
            return;
        }
        minCalCols = cols;
        revalidate();
        repaint();
    }

    /**
     * Returns the preferred number of rows to paint calendars in.
     * <p>
     * PENDING JW: rename to a "full" name preferredRowCount
     *  or maybe visibleRowCount to be consistent with JXTable/JXList 
     * @return int Rows of calendars.
     */
    public int getPreferredRows() {
        return minCalRows;
    }

    /**
     * Sets the preferred number of rows to paint calendars.
     * <p>
     * PENDING JW: rename to a "full" name preferredRowCount
     *   and make bound property
     *
     * @param rows The number of rows of calendars.
     */
    public void setPreferredRows(int rows) {
        if (rows <= 0) {
            return;
        }
        minCalRows = rows;
        revalidate();
        repaint();
    }


    /**
     * Moves and resizes this component to conform to the new bounding
     * rectangle r. This component's new position is specified by r.x and
     * r.y, and its new size is specified by r.width and r.height <p>
     * 
     * PENDING JW: why ovrridden? super is identical
     *
     * @param r The new bounding rectangle for this component
     */
    @Override
    public void setBounds(Rectangle r) {
        setBounds(r.x, r.y, r.width, r.height);
    }

    /**
     * Sets the font of this component.
     *
     * PENDING JW: why override?
     *  
     * @param font The font to become this component's font; if this parameter
     *             is null then this component will inherit the font of its parent.
     */
    @Override
    public void setFont(Font font) {
        Font old = getFont();
        super.setFont(font);
        firePropertyChange("font", old, font);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        todayTimer.stop();
        super.removeNotify();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNotify() {
        super.addNotify();

        // Setup timer to update the value of today.
        int secondsTillTomorrow = 86400;

        if (todayTimer == null) {
            todayTimer = new Timer(secondsTillTomorrow * 1000,
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            incrementToday();
                        }
                    });
        }

        // Modify the initial delay by the current time.
        cal.setTimeInMillis(System.currentTimeMillis());
        secondsTillTomorrow = secondsTillTomorrow -
                (cal.get(Calendar.HOUR_OF_DAY) * 3600) -
                (cal.get(Calendar.MINUTE) * 60) -
                cal.get(Calendar.SECOND);
        todayTimer.setInitialDelay(secondsTillTomorrow * 1000);
        todayTimer.start();
    }

//-------------------- action and listener
    

    /**
     * Commits the current selection. <p>
     * 
     * Resets the model's adjusting property to false
     * and fires an ActionEvent
     * with the COMMIT_KEY action command.
     * 
     * <p>PENDING: define what "commit selection" means ... currently
     * only fires (to keep the picker happy).
     * 
     * @see #cancelSelection()
     * @see org.jdesktop.swingx.calendar.DateSelectionModel#setAdjusting(boolean)
     */
    public void commitSelection() {
        getSelectionModel().setAdjusting(false);
        fireActionPerformed(COMMIT_KEY);
    }

    /**
     * Cancels the selection. <p>
     * 
     * Resets the model's adjusting to 
     * false and fires an ActionEvent with the CANCEL_KEY action command.
     * 
     * @see #commitSelection
     * @see org.jdesktop.swingx.calendar.DateSelectionModel#setAdjusting(boolean)
     */
    public void cancelSelection() {
        getSelectionModel().setAdjusting(false);
        fireActionPerformed(CANCEL_KEY);
        
    }

    /**
     * Sets the component input map enablement property.<p>
     * 
     * If enabled, the keybinding for WHEN_IN_FOCUSED_WINDOW are
     * installed, otherwise not. Changing this property will
     * install/clear the corresponding key bindings. Typically, clients 
     * which want to use the monthview in a popup, should enable these.<p>
     * 
     * The default value is false.
     * 
     * @param enabled boolean to indicate whether the component
     *   input map should be enabled.
     * @see #isComponentInputMapEnabled()  
     */
    public void setComponentInputMapEnabled(boolean enabled) {
        if (isComponentInputMapEnabled() == enabled) return;
        this.componentInputMapEnabled = enabled;
        firePropertyChange("componentInputMapEnabled", !enabled, isComponentInputMapEnabled());
    }

    /**
     * Returns the componentInputMapEnabled property.
     * 
     * @return a boolean indicating whether the component input map is 
     *   enabled.
     * @see #setComponentInputMapEnabled(boolean)  
     *   
     */
    public boolean isComponentInputMapEnabled() {
        return componentInputMapEnabled;
    }

    /**
     * Adds an ActionListener.
     * <p/>
     * The ActionListener will receive an ActionEvent when a selection has
     * been made.
     *
     * @param l The ActionListener that is to be notified
     */
    public void addActionListener(ActionListener l) {
        listenerMap.add(ActionListener.class, l);
    }

    /**
     * Removes an ActionListener.
     *
     * @param l The action listener to remove.
     */
    public void removeActionListener(ActionListener l) {
        listenerMap.remove(ActionListener.class, l);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        java.util.List<T> listeners = listenerMap.getListeners(listenerType);
        T[] result;
        if (!listeners.isEmpty()) {
            //noinspection unchecked
            result = (T[]) java.lang.reflect.Array.newInstance(listenerType, listeners.size());
            result = listeners.toArray(result);
        } else {
            result = super.getListeners(listenerType);
        }
        return result;
    }

    /**
     * Creates and fires an ActionEvent with the given action 
     * command to all listeners.
     * 
     * @param actionCommand the command for the created.
     */
    protected void fireActionPerformed(String actionCommand) {
        ActionListener[] listeners = getListeners(ActionListener.class);
        ActionEvent e = null;

        for (ActionListener listener : listeners) {
            if (e == null) {
                e = new ActionEvent(JXMonthView.this,
                        ActionEvent.ACTION_PERFORMED,
                        actionCommand);
            }
            listener.actionPerformed(e);
        }
    }


//--- deprecated code - NOTE: these methods will be removed soon! 
//--- they will definitely be removed in milestone 0.9.2!    

    /**
     * Returns the selected date. 
     * 
     * @return the first Date in the selection or null if empty.
     * 
     * @deprecated use {@link #getSelectionDate()} name change to 
     *   align with new DateSelectionModel api
     */
    @Deprecated
    public Date getSelectedDate() {
        return getSelectionDate();    
    }

    /**
     * Sets the model's selection to the given date or clears the selection if
     * null.
     * 
     * @param newDate the selection date to set
     * 
     * @deprecated use {@link #setSelectionDate(Date)} - name change to 
     *   align with new DateSelectionModel api
     */
    @Deprecated
    public void setSelectedDate(Date newDate) {
        setSelectionDate(newDate);
    }

    /**
     * Returns true if the specified date falls within the _startSelectedDate
     * and _endSelectedDate range.  
     *
     * @param date The date to check
     * @return true if the date is selected, false otherwise
     * 
     * @deprecated use {@link #isSelected(Date)}  - name change to 
     *   align with DateSelectionModel api
     */
    @Deprecated
    public boolean isSelectedDate(Date date) {
        return getSelectionModel().isSelected(date);
    }

    /**
     * Return a long representing the date at the specified x/y position.
     * The date returned will have a valid day, month and year.  Other fields
     * such as hour, minute, second and milli-second will be set to 0.
     *
     * @param x X position
     * @param y Y position
     * @return long The date, -1 if position does not contain a date.
     * @deprecated use {@link #getDayAtLocation(int, int)}
     */
    @Deprecated
    public long getDayAt(int x, int y) {
        Date day = getDayAtLocation(x, y);
        return day != null ? day.getTime() : -1;
    }

    /**
     * Whether or not to show leading dates for a months displayed by this component.
     *
     * @param value true if leading dates should be displayed, false otherwise.
     * @deprecated use {@link #setShowingLeadingDays(boolean)} 
     *  - name change to comply with property naming conventions and 
     *  consistently using "Day" instead of "Date". 
     */
    @Deprecated
    public void setShowLeadingDates(boolean value) {
        setShowingLeadingDays(value);
    }
    
    /**
     * Whether or not we're showing leading dates.
     *
     * @return true if leading dates are shown, false otherwise.
     * 
     * @deprecated use {@link #isShowingLeadingDays()}
     *  - name change to comply with property naming conventions and 
     *  consistently using "Day" instead of "Date". 
     */
    @Deprecated
    public boolean isShowingLeadingDates() {
        return leadingDays;
    }

    /**
     * Whether or not to show trailing dates for the months displayed by this component.
     *
     * @param value true if trailing dates should be displayed, false otherwise.
     * @deprecated use {@link #setShowingTrailingDays(boolean)} - 
     *  - name change to comply with property naming conventions and 
     *  consistently using "Day" instead of "Date". 
     */
    @Deprecated
    public void setShowTrailingDates(boolean value) {
        setShowingTrailingDays(value);
    }

    /**
     * 
     * @return true if the trailing dates in of a month are showing, false otherwise.
     * 
     * @deprecated use {@link #isShowingTrailingDays()} 
     *  - name change to comply with property naming conventions and 
     *  consistently using "Day" instead of "Date". 
     */
    @Deprecated
    public boolean isShowingTrailingDates() {
        return isShowingTrailingDays();
    }
  //--------------------- flagged dates (long) - deprecation pending!

    /**
     * Identifies whether or not the date passed is a flagged date.  <b>All dates are modified to remove their hour of
     * day, minute, second, and millisecond before being added to the selection model</b>
     *
     * @param date date which to test for flagged status
     * @return true if the date is flagged, false otherwise
     * 
     * @deprecated use {@link #isFlaggedDate(Date)}
     */
    @Deprecated
    public boolean isFlaggedDate(long date) {
        return isFlaggedDate(new Date(date));
    }

    /**
     * An array of longs defining days that should be flagged.
     *
     * @param flaggedDates the dates to be flagged
     * 
     * @deprecated use {@link #setFlaggedDates(Date[])}
     */
    @Deprecated
    public void setFlaggedDates(long[] flaggedDates) {
        Date[] flagged = null;
        if (flaggedDates != null) {
            flagged = new Date[flaggedDates.length];
            for (int i = 0; i < flaggedDates.length; i++) {
                flagged[i] = new Date(flaggedDates[i]);
            }
        }
        setFlaggedDates(flagged);
    }

  //---------------------- delegates to model: long param    
    /**
     * Returns true if the specified date falls within the _startSelectedDate
     * and _endSelectedDate range.  
     *
     * @param date The date to check
     * @return true if the date is selected, false otherwise
     * 
     * @deprecated use {@link #setSelectionDate(Date)}
     */
    @Deprecated
    public boolean isSelectedDate(long date) {
        return getSelectionModel().isSelected(new Date(date));
    }

    /**
     * Identifies whether or not the date passed is an unselectable date.  
     * 
     * @param date date which to test for unselectable status
     * @return true if the date is unselectable, false otherwise
     * 
     * @deprecated use {@link #isUnselectableDate(Date)}
     */
    @Deprecated
    public boolean isUnselectableDate(long date) {
        return getSelectionModel().isUnselectableDate(new Date(date));
    }

    /**
     * An array of longs defining days that should be unselectable.  
     *
     * @param unselectableDates the dates that should be unselectable
     * 
     * @deprecated use {@link #setUnselectableDates(Date...)}
     */
    @Deprecated
    public void setUnselectableDates(long[] unselectableDates) {
        SortedSet<Date> unselectableSet = new TreeSet<Date>();
        if (unselectableDates != null) {
            for (long unselectableDate : unselectableDates) {
                unselectableSet.add(new Date(unselectableDate));
            }
        }
        getSelectionModel().setUnselectableDates(unselectableSet);
        repaint();
    }

    /**
     * Sets the todayInMillis property to the start of the day which contains the
     * given millis in this monthView's calendar coordinates.
     *  
     * temporary widened access for testing.
     * 
     * @param millis the instance in millis which should be used as today.
     * @deprecated use {@link #setToday(Date)}
     */
    @Deprecated
    protected void setTodayInMillis(long millis) {
        setToday(new Date(millis));
    }

    /**
     * Returns the start of today in this monthviews calendar coordinates.
     * 
     * @return the start of today in millis.
     * 
     * @deprecated use {@link #getToday()}
     */
    @Deprecated
    public long getTodayInMillis() {
        return today.getTime();
    }

    @Deprecated
    protected void cleanupWeekSelectionDates(Date startDate, Date endDate) {
        int count = 1;
        cal.setTime(startDate);
        while (cal.getTimeInMillis() < endDate.getTime()) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            count++;
        }

        if (count > JXMonthView.DAYS_IN_WEEK) {
            // Move the start date to the first day of the week.
            cal.setTime(startDate);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int firstDayOfWeek = getFirstDayOfWeek();
            int daysFromStart = dayOfWeek - firstDayOfWeek;
            if (daysFromStart < 0) {
                daysFromStart += JXMonthView.DAYS_IN_WEEK;
            }
            cal.add(Calendar.DAY_OF_MONTH, -daysFromStart);

            modifiedStartDate = cal.getTime();

            // Move the end date to the last day of the week.
            cal.setTime(endDate);
            dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int lastDayOfWeek = firstDayOfWeek - 1;
            if (lastDayOfWeek == 0) {
                lastDayOfWeek = Calendar.SATURDAY;
            }
            int daysTillEnd = lastDayOfWeek - dayOfWeek;
            if (daysTillEnd < 0) {
                daysTillEnd += JXMonthView.DAYS_IN_WEEK;
            }
            cal.add(Calendar.DAY_OF_MONTH, daysTillEnd);
            modifiedEndDate = cal.getTime();
        }
    }

    /**
     * Moves the <code>date</code> into the visible region of the calendar. If
     * the date is greater than the last visible date it will become the last
     * visible date. While if it is less than the first visible date it will
     * become the first visible date. <p>
     * 
     * NOTE: this method will probably be deprecated, it's recommended
     * to use the functionally equivalent method taking a Date parameter.
     * 
     * @param date millis representing the date to make visible.
     * @see #ensureDateVisible(Date)
     * 
     * @deprecated use {@link #ensureDateVisible(Date)}
     */
    @Deprecated
    public void ensureDateVisible(long date) {
        if (date < firstDisplayedDay.getTime()) {
            setFirstDisplayedDate(date);
        } else {
            long lastDisplayedDate = getLastDisplayedDate();
            if (date > lastDisplayedDate) {

                cal.setTimeInMillis(date);
                int month = cal.get(Calendar.MONTH);
                int year = cal.get(Calendar.YEAR);

                cal.setTimeInMillis(lastDisplayedDate);
                int lastMonth = cal.get(Calendar.MONTH);
                int lastYear = cal.get(Calendar.YEAR);

                int diffMonths = month - lastMonth
                        + ((year - lastYear) * MONTHS_IN_YEAR);

                cal.setTime(firstDisplayedDay);
                cal.add(Calendar.MONTH, diffMonths);
                setFirstDisplayedDate(cal.getTimeInMillis());
            }
        }
    }


    /**
     * Returns the first displayed date.
     *
     * @return long The first displayed date.
     * @deprecated use {@link #getFirstDisplayedDay()}
     */
    @Deprecated
    public long getFirstDisplayedDate() {
        return firstDisplayedDay.getTime();
    }

    
    /**
     * Set the first displayed date.  We only use the month and year of
     * this date.  The <code>Calendar.DAY_OF_MONTH</code> field is reset to
     * 1 and all other fields, with exception of the year and month,
     * are reset to 0.
     *
     * @param date The first displayed date.
     * @deprecated use {@link #setFirstDisplayedDay(Date)}
     */
    @Deprecated
    public void setFirstDisplayedDate(long date) {
        setFirstDisplayedDay(new Date(date));
    }

 
    
    /**
     * Returns the last date able to be displayed.  For example, if the last
     * visible month was April the time returned would be April 30, 23:59:59.
     *
     * @return long The last displayed date.
     * @deprecated use {@link #getLastDisplayedDay()} instead
     */
    @Deprecated
    public long getLastDisplayedDate() {
        return getUI().getLastDisplayedDate();
    }

    /**
     * Create a new instance of the <code>JXMonthView</code> class using the
     * given Locale, the given time as the first date to 
     * display and the given selection model. 
     * 
     * @param firstDisplayedDate 
     * @param model the selection model to use, if null a <code>DefaultSelectionModel</code> is
     *   created.
     * @param locale desired locale, if null the system default locale is used
     * 
     * @deprecated use {@link #JXMonthView(Date, DateSelectionModel, Locale)}
     */
    @Deprecated
    public JXMonthView(long firstDisplayedDate, final DateSelectionModel model, final Locale locale) {
        this(new Date(firstDisplayedDate), model, locale);
    }

    /**
     * Create a new instance of the <code>JXMonthView</code> class using the
     * default Locale and the given time as the first date to 
     * display.
     *
     * @param firstDisplayedDate The first month to display.
     * @deprecated use {@link #JXMonthView(Date)}(
     */
    @Deprecated
    public JXMonthView(long firstDisplayedDate) {
        this(firstDisplayedDate, null, null);
    }

    /**
     * Create a new instance of the <code>JXMonthView</code> class using the
     * default Locale, the given time as the first date to 
     * display and the given selection model. 
     * 
     * @param firstDisplayedDate The first month to display.
     * @param model the selection model to use, if null a <code>DefaultSelectionModel</code> is
     *   created.
     *   
     * @deprecated use {@link #JXMonthView(Date, DateSelectionModel)}  
     */
    @Deprecated
    public JXMonthView(long firstDisplayedDate, final DateSelectionModel model) {
        this(firstDisplayedDate, model, null);
    }

    /**
     * Returns the string currently used to identiy fired ActionEvents.
     *
     * @return String The string used for identifying ActionEvents.
     * 
     * @deprecated no longer used. The command is internally determined and
     * either monthViewCommit or monthViewCancel, depending on the user
     * gesture which triggered the action.
     */
    @Deprecated
    public String getActionCommand() {
        return actionCommand;
    }

    /**
     * Sets the string used to identify fired ActionEvents.
     *
     * @param actionCommand The string used for identifying ActionEvents.
     * 
     * @deprecated no longer used. The command is internally determined and
     * either monthViewCommit or monthViewCancel, depending on the user
     * gesture which triggered the action.
     */
    @Deprecated
    public void setActionCommand(String actionCommand) {
        this.actionCommand = actionCommand;
    }



}
