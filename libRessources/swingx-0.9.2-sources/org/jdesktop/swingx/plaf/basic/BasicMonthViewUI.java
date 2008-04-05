/*
 * $Id: BasicMonthViewUI.java,v 1.113 2008/02/27 15:31:27 kleopatra Exp $
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
package org.jdesktop.swingx.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;

import org.jdesktop.swingx.JXMonthView;
import org.jdesktop.swingx.calendar.CalendarUtils;
import org.jdesktop.swingx.calendar.DateSelectionModel;
import org.jdesktop.swingx.calendar.DateSelectionModel.SelectionMode;
import org.jdesktop.swingx.event.DateSelectionEvent;
import org.jdesktop.swingx.event.DateSelectionListener;
import org.jdesktop.swingx.plaf.MonthViewUI;

/**
 * Base implementation of the <code>JXMonthView</code> UI.<p>
 *
 * <b>Note</b>: The api changed considerably between releases 0.9.1 and 0.9.2. Most of 
 * the old methods are still available but deprecated. It's strongly recommended to 
 * update subclasses soon, because those methods will be removed before 0.9.3. <p>
 * 
 * The general drift of the change was
 * <ul>
 * <li> replace all methods which take/return a date in millis with equivalents taking/returning
 *  a Date object
 * <li> streamline the painting (to make it understandable for me ;-) See below.
 * <li> pass-around a calendar object to all painting methods. The general contract is that
 *   methods which receive the calendar must not change it in any way. It's up to the calling
 *   method to loop through the dates if appropriate. 
 * </ul>
 *   
 * Painting: defined coordinate systems.
 * 
 * <ul>
 * <li> Screen coordinates of months/days, accessible via the getXXBounds() methods. These
 * coordinates are absolute in the system of the monthView. 
 * <li> The grid of visible months with logical row/column coordinates. The logical 
 * coordinates are adjusted to ComponentOrientation. 
 * <li> The grid of days in a month with logical row/column coordinates. The logical 
 * coordinates are adjusted to ComponentOrientation. The columns 
 * are the days of the week, the rows are the weeks in a month. The column header shows
 * the localized names of the days and has the row coordinate -1. It is shown always.
 * The row header shows the week number in the year and has the column coordinate -1. It
 * is shown only if the showingWeekNumber property is true.  
 * </ul>
 * 
 *   
 *   
 * @author dmouse
 * @author rbair
 * @author rah003
 * @author Jeanette Winzenburg
 */
public class BasicMonthViewUI extends MonthViewUI {
    @SuppressWarnings("all")
    private static final Logger LOG = Logger.getLogger(BasicMonthViewUI.class
            .getName());
    /** @deprecated no longer used, no replacement. */
    @Deprecated
    protected static final int LEADING_DAY_OFFSET = 1;
    /** @deprecated no longer used, no replacement. */
    @Deprecated
    protected static final int NO_OFFSET = 0;
    /** @deprecated no longer used, no replacement. */
    @Deprecated
    protected static final int TRAILING_DAY_OFFSET = -1;

    
    private static final int WEEKS_IN_MONTH = 6;
    private static final int CALENDAR_SPACING = 10;
    private static final Point NO_SUCH_CALENDAR = new Point(-1, -1);

    
    /** Return value used to identify when the month down button is pressed. */
    public static final int MONTH_DOWN = 1;
    /** Return value used to identify when the month up button is pressed. */
    public static final int MONTH_UP = 2;

    /** Formatter used to format the day of the week to a numerical value. */
    protected final SimpleDateFormat dayOfMonthFormatter = new SimpleDateFormat("d");
    /** localized names of all months.
     * protected for testing only!
     * PENDING: JW - should be property on JXMonthView, for symmetry with
     *   daysOfTheWeek? 
     */
    protected String[] monthsOfTheYear;

    /** the component we are installed for. */
    protected JXMonthView monthView;
    // listeners
    private PropertyChangeListener propertyChangeListener;
    private MouseListener mouseListener;
    private MouseMotionListener mouseMotionListener;
    private Handler handler;

    // fields related to visible date range
    /** start of day of the first visible month. */
    private Date firstDisplayedDate;
    /** first visible month. */
    private int firstDisplayedMonth;
    /** first visible year. */
    private int firstDisplayedYear;
    /** end of day of the last visible month. */
    private Date lastDisplayedDate;
    /** 
    
    //---------- fields related to selection/navigation


    /** flag indicating keyboard navigation. */
    private boolean usingKeyboard = false;
    /** For interval selections we need to record the date we pivot around. */
    private Date pivotDate = null;
    /**
     * Date span used by the keyboard actions to track the original selection.
     */
    private SortedSet<Date> originalDateSpan;

    //------------------ visuals
    /** Used as the font for flagged days. */
    protected Font derivedFont;

    protected boolean isLeftToRight;
    protected Icon monthUpImage;
    protected Icon monthDownImage;
    private Color weekOfTheYearForeground;
    private Color unselectableDayForeground;
    private Color leadingDayForeground;
    private Color trailingDayForeground;

    private int arrowPaddingX = 3;
    private int arrowPaddingY = 3;
    // PENDING JW: use again? this was used as marker of the single
    // selected day box ... removed for simplification
//    private Rectangle dirtyRect = new Rectangle();
    private Rectangle bounds = new Rectangle();
    
    /**
     * Horizontal edge of the first column of displayed months. The edge is 
     * left/right depending on componentOrientation isLeftToRight or not.
     * 
     * PENDING: JW - really want to adjust here? Need to check in usage
     *   anyway.
     *   @deprecated only read in deprecated methods (but calculation still triggered
     *    in layout, because some of the deprecated are public/protected)
     */
    @Deprecated
    private int startX;
    /**
     * Top of first row of displayed months. 
     *   @deprecated only read in deprecated methods (but calculation still triggered
     *    in layout, because some of the deprecated are public/protected)
     */
    @Deprecated
    private int startY;

    /** 
     * height of month header of the view, that is the name and the arrows.
     * initially, it's the same as the day-box-height, adjusted to arrow icon height
     * and arrow padding if traversable
     * 
     */
    private int monthBoxHeight;
    /** height of month header including the monthView's box padding. */
    private int fullMonthBoxHeight;
    /** 
     * raw witdth of a "day" box calculated from fontMetrics and "widest" content.
     *  this is the same for days-of-the-week, weeks-of-the-year and days
     * 
     */
    private int boxWidth;
    /** 
     * raw height of a "day" box calculated from fontMetrics and "widest" content.
     *  this is the same for days-of-the-week, weeks-of-the-year and days
     * 
     */
    private int boxHeight;
    /** 
     * width of a "day" box including the monthView's box padding
     * this is the same for days-of-the-week, weeks-of-the-year and days
     */
    private int fullBoxWidth;
    /** 
     * height of a "day" box including the monthView's box padding
     * this is the same for days-of-the-week, weeks-of-the-year and days
     */
    private int fullBoxHeight;
    /** the width of a single month display. */
    private int calendarWidth;
    /** the height of a single month display. */
    private int calendarHeight;
    /** the height of a single month grid cell, including padding. */
    private int fullCalendarHeight;
    /** the width of a single month grid cell, including padding. */
    private int fullCalendarWidth;
    /** The number of calendars displayed vertically. */
    private int calendarRowCount = 1;
    /** The number of calendars displayed horizontally. */
    private int calendarColumnCount = 1;
    
    /**
     * The bounding box of the grid of visible months. 
     */
    protected Rectangle calendarGrid = new Rectangle();
    private Rectangle[] monthStringBounds = new Rectangle[12];
    private Rectangle[] yearStringBounds = new Rectangle[12];
    private String[] daysOfTheWeek;


    @SuppressWarnings({"UnusedDeclaration"})
    public static ComponentUI createUI(JComponent c) {
        return new BasicMonthViewUI();
    }

    @Override
    public void installUI(JComponent c) {
        monthView = (JXMonthView)c;
        monthView.setLayout(createLayoutManager());
        isLeftToRight = monthView.getComponentOrientation().isLeftToRight();
        LookAndFeel.installProperty(monthView, "opaque", Boolean.TRUE);

        installComponents();
        installDefaults();
        installKeyboardActions();
        installListeners();
    }

    @Override
    public void uninstallUI(JComponent c) {
        uninstallListeners();
        uninstallKeyboardActions();
        uninstallDefaults();
        uninstallComponents();
        monthView.setLayout(null);
        monthView = null;
    }

    protected void installComponents() {}

    protected void uninstallComponents() {}

    protected void installDefaults() {
        
        Color background = monthView.getBackground();
        if (background == null || background instanceof UIResource) {
            monthView.setBackground(UIManager.getColor("JXMonthView.background"));
        }
        monthView.setBoxPaddingX(UIManager.getInt("JXMonthView.boxPaddingX"));
        monthView.setBoxPaddingY(UIManager.getInt("JXMonthView.boxPaddingY"));
        monthView.setMonthStringBackground(UIManager.getColor("JXMonthView.monthStringBackground"));
        monthView.setMonthStringForeground(UIManager.getColor("JXMonthView.monthStringForeground"));
        monthView.setDaysOfTheWeekForeground(UIManager.getColor("JXMonthView.daysOfTheWeekForeground"));
        monthView.setSelectedBackground(UIManager.getColor("JXMonthView.selectedBackground"));
        monthView.setFlaggedDayForeground(UIManager.getColor("JXMonthView.flaggedDayForeground"));
        Font f = monthView.getFont();
        if (f == null || f instanceof UIResource) {
            monthView.setFont(UIManager.getFont("JXMonthView.font"));
        }
        monthDownImage = UIManager.getIcon("JXMonthView.monthDownFileName");
        monthUpImage = UIManager.getIcon("JXMonthView.monthUpFileName");
        weekOfTheYearForeground = UIManager.getColor("JXMonthView.weekOfTheYearForeground");
        leadingDayForeground = UIManager.getColor("JXMonthView.leadingDayForeground");
        trailingDayForeground = UIManager.getColor("JXMonthView.trailingDayForeground");
        unselectableDayForeground = UIManager.getColor("JXMonthView.unselectableDayForeground");
        derivedFont = createDerivedFont();
        
        // install date/locale related state
        setFirstDisplayedDay(monthView.getFirstDisplayedDay());
        updateLocale();
    }

    protected void uninstallDefaults() {}

    protected void installKeyboardActions() {
        // Setup the keyboard handler.
        // PENDING JW: change to when-ancestor? just to be on the safe side
        // if we make the title contain active comps
        installKeyBindings(JComponent.WHEN_FOCUSED);
        // JW: removed the automatic keybindings in WHEN_IN_FOCUSED
        // which caused #555-swingx (binding active if not focused)
        ActionMap actionMap = monthView.getActionMap();
        KeyboardAction acceptAction = new KeyboardAction(KeyboardAction.ACCEPT_SELECTION);
        actionMap.put("acceptSelection", acceptAction);
        KeyboardAction cancelAction = new KeyboardAction(KeyboardAction.CANCEL_SELECTION);
        actionMap.put("cancelSelection", cancelAction);

        actionMap.put("selectPreviousDay", new KeyboardAction(KeyboardAction.SELECT_PREVIOUS_DAY));
        actionMap.put("selectNextDay", new KeyboardAction(KeyboardAction.SELECT_NEXT_DAY));
        actionMap.put("selectDayInPreviousWeek", new KeyboardAction(KeyboardAction.SELECT_DAY_PREVIOUS_WEEK));
        actionMap.put("selectDayInNextWeek", new KeyboardAction(KeyboardAction.SELECT_DAY_NEXT_WEEK));

        actionMap.put("adjustSelectionPreviousDay", new KeyboardAction(KeyboardAction.ADJUST_SELECTION_PREVIOUS_DAY));
        actionMap.put("adjustSelectionNextDay", new KeyboardAction(KeyboardAction.ADJUST_SELECTION_NEXT_DAY));
        actionMap.put("adjustSelectionPreviousWeek", new KeyboardAction(KeyboardAction.ADJUST_SELECTION_PREVIOUS_WEEK));
        actionMap.put("adjustSelectionNextWeek", new KeyboardAction(KeyboardAction.ADJUST_SELECTION_NEXT_WEEK));

        actionMap.put(JXMonthView.COMMIT_KEY, acceptAction);
        actionMap.put(JXMonthView.CANCEL_KEY, cancelAction);
    }

    /**
     * @param inputMap
     */
    private void installKeyBindings(int type) {
        InputMap inputMap = monthView.getInputMap(type);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "acceptSelection");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "cancelSelection");

        // @KEEP quickly check #606-swingx: keybindings not working in internalframe
        // eaten somewhere
//        inputMap.put(KeyStroke.getKeyStroke("F1"), "selectPreviousDay");

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "selectPreviousDay");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "selectNextDay");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), "selectDayInPreviousWeek");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "selectDayInNextWeek");

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK, false), "adjustSelectionPreviousDay");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK, false), "adjustSelectionNextDay");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_MASK, false), "adjustSelectionPreviousWeek");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK, false), "adjustSelectionNextWeek");
    }

    /**
     * @param inputMap
     */
    private void uninstallKeyBindings(int type) {
        InputMap inputMap = monthView.getInputMap(type);
        inputMap.clear();
    }

    protected void uninstallKeyboardActions() {}

    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        mouseListener = createMouseListener();
        mouseMotionListener = createMouseMotionListener();
        
        
        monthView.addPropertyChangeListener(propertyChangeListener);
        monthView.addMouseListener(mouseListener);
        monthView.addMouseMotionListener(mouseMotionListener);

        monthView.getSelectionModel().addDateSelectionListener(getHandler());
    }

    protected void uninstallListeners() {
        monthView.getSelectionModel().removeDateSelectionListener(getHandler());
        monthView.removeMouseMotionListener(mouseMotionListener);
        monthView.removeMouseListener(mouseListener);
        monthView.removePropertyChangeListener(propertyChangeListener);

        mouseMotionListener = null;
        mouseListener = null;
        propertyChangeListener = null;
    }
//----------------------- controller
    
    /**
     * Binds/clears the keystrokes in the component input map, 
     * based on the monthView's componentInputMap enabled property.
     * 
     * @see org.jdesktop.swingx.JXMonthView#isComponentInputMapEnabled()
     */
    protected void updateComponentInputMap() {
        if (monthView.isComponentInputMapEnabled()) {
            installKeyBindings(JComponent.WHEN_IN_FOCUSED_WINDOW);
        } else {
            uninstallKeyBindings(JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
    }


   /**
    * Updates month and day names according to specified locale.
    */
   protected void updateLocale() {
        Locale locale = monthView.getLocale();
        monthsOfTheYear = new DateFormatSymbols(locale).getMonths();
        
        // fixed JW: respect property in UIManager if available
        // PENDING JW: what to do if weekdays had been set 
        // with JXMonthView method? how to detect?
        daysOfTheWeek =
          (String[])UIManager.get("JXMonthView.daysOfTheWeek");
        
        if (daysOfTheWeek == null) {
            daysOfTheWeek = new String[7];
            String[] dateFormatSymbols = new DateFormatSymbols(locale)
                    .getShortWeekdays();
            daysOfTheWeek = new String[JXMonthView.DAYS_IN_WEEK];
            for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
                daysOfTheWeek[i - 1] = dateFormatSymbols[i];
            }
        }
//        monthView.setDaysOfTheWeek(daysOfTheWeek);
        monthView.invalidate();
        monthView.validate();
    }

   @Override
   public String[] getDaysOfTheWeek() {
       String[] days = new String[daysOfTheWeek.length];
       System.arraycopy(daysOfTheWeek, 0, days, 0, days.length);
       return days;
   }
   
//---------------------- config
    

    /**
     * Create a derived font used to when painting various pieces of the
     * month view component.  This method will be called whenever
     * the font on the component is set so a new derived font can be created.
     */
    protected Font createDerivedFont() {
        return monthView.getFont().deriveFont(Font.BOLD);
    }
    

//---------------------- listener creation    
    protected PropertyChangeListener createPropertyChangeListener() {
        return getHandler();
    }

    protected LayoutManager createLayoutManager() {
        return getHandler();
    }

    protected MouseListener createMouseListener() {
        return getHandler();
    }

    protected MouseMotionListener createMouseMotionListener() {
        return getHandler();
    }

    private Handler getHandler() {
        if (handler == null) {
            handler = new Handler();
        }

        return handler;
    }

    public boolean isUsingKeyboard() {
        return usingKeyboard;
    }

    public void setUsingKeyboard(boolean val) {
        usingKeyboard = val;
    }



    // ----------------------- mapping day coordinates

    /**
     * Returns the bounds of the day in the grid of days which contains the
     * given location. The bounds are in monthView screen coordinate system.
     * <p>
     * 
     * Note: this is a pure geometric mapping. The returned rectangle need not
     * necessarily map to a date in the month which contains the location, it
     * can represent a week-number/column header or a leading/trailing date.
     * 
     * @param x the x position of the location in pixel
     * @param y the y position of the location in pixel
     * @return the bounds of the day which contains the location, or null if
     *         outside
     */
    protected Rectangle getDayBoundsAtLocation(int x, int y) {
        Rectangle days = getMonthDetailsBoundsAtLocation(x, y);
        if ((days == null) || (!days.contains(x, y)))
            return null;
        int calendarRow = (y - days.y) / fullBoxHeight;
        int calendarColumn = (x - days.x) / fullBoxWidth;
        return new Rectangle(days.x + calendarColumn * fullBoxWidth, days.y
                + calendarRow * fullBoxHeight, fullBoxWidth, fullBoxHeight);
    }
    
    /**
     * Returns the logical coordinates of the day which contains the given
     * location. The p.x of the returned value represents the day of week, the
     * p.y represents the week of the month. The transformation takes care of
     * ComponentOrientation.
     * <p>
     * 
     * Note: this is a pure geometric mapping. The returned grid position need not
     * necessarily map to a date in the month which contains the location, it
     * can represent a week-number/column header or a leading/trailing date.
     * 
     * @param x the x position of the location in pixel
     * @param y the y position of the location in pixel
     * @return the logical coordinates of the day in the grid of days in a month
     *         or null if outside.
     */
    protected Point getDayGridPositionAtLocation(int x, int y) {
        Rectangle days = getMonthDetailsBoundsAtLocation(x, y);
        if ((days == null) ||(!days.contains(x, y))) return null;
        int calendarRow = (y - days.y) / fullBoxHeight;
        int calendarColumn = (x - days.x) / fullBoxWidth;
        if (!isLeftToRight) {
            int start = days.x + days.width;
            calendarColumn = (start - x) / fullBoxWidth;
        }
        if (monthView.isShowingWeekNumber()) {
            calendarColumn -= 1;
        }
        return new Point(calendarColumn, calendarRow - 1);
    }

    /**
     * Returns the given date's position in the grid of the month it is contained in.
     * 
     * @param date the Date to get the logical position for, must not be null.
     * @return the logical coordinates of the day in the grid of days in a
     *   month or null if the Date is not visible. 
     */
    protected Point getDayGridPosition(Date date) {
        if (!isVisible(date)) return null;
        Calendar calendar = getCalendar(date);
        Date startOfDay = CalendarUtils.startOfDay(calendar, date);
        // there must be a less ugly way?
        // columns
        CalendarUtils.startOfWeek(calendar);
        int column = 0;
        while (calendar.getTime().before(startOfDay)) {
            column++;
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        Date startOfWeek = CalendarUtils.startOfWeek(calendar, date);
        calendar.setTime(date);
        CalendarUtils.startOfMonth(calendar);
        int row = 0;
        while (calendar.getTime().before(startOfWeek)) {
            row++;
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }
        return new Point(column, row);
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
    @Override
    public Date getDayAtLocation(int x, int y) {
        Point dayInGrid = getDayGridPositionAtLocation(x, y);
        if ((dayInGrid == null) || (dayInGrid.x < 0) || (dayInGrid.y < 0)) return null;
        Date month = getMonthAtLocation(x, y);
        return getDayInMonth(month, dayInGrid.y, dayInGrid.x);
    }
    
    /**
     * Returns the bounds of the given day.
     * The bounds are in monthView coordinate system.<p>
     * 
     * PENDING JW: this most probably should be public as it is the logical
     * reverse of getDayAtLocation <p>
     * 
     * @param date the Date to return the bounds for. Must not be null.
     * @return the bounds of the given date or null if not visible.
     */
    protected Rectangle getDayBounds(Date date) {
        if (!isVisible(date)) return null;
        Point position = getDayGridPosition(date);
        Rectangle monthBounds = getMonthBounds(date);
        monthBounds.y += getMonthHeaderHeight() + (position.y + 1) * fullBoxHeight;
        if (monthView.isShowingWeekNumber()) {
            position.x++;
        }
        if (isLeftToRight) {
           monthBounds.x += position.x * fullBoxWidth; 
        } else {
            int start = monthBounds.x + monthBounds.width - fullBoxWidth; 
            monthBounds.x = start - position.x * fullBoxWidth;
        }
        monthBounds.width = fullBoxWidth;
        monthBounds.height = fullBoxHeight;
        return monthBounds;
    }
    
    /**
     * Returns a boolean indicating if the given Date is visible. Trailing/leading
     * dates of the last/first displayed month are considered to be invisible.
     * 
     * @param date the Date to check for visibility. Must not be null.
     * @return true if the date is visible, false otherwise.
     */
    private boolean isVisible(Date date) {
        if (getFirstDisplayedDay().after(date) || getLastDisplayedDay().before(date)) return false;
        return true;
    }

    /**
     * Returns the Date defined by the logical 
     * grid coordinates relative to the given month. May be null if the
     * logical coordinates represent a header in the day grid or is outside of the
     * given month.
     * 
     * Mapping logical day grid coordinates to Date.<p>
     * 
     * PENDING JW: relax the startOfMonth pre? Why did I require it?
     * 
     * @param month a calendar representing the first day of the month, must not
     *   be null.
     * @param row the logical row index in the day grid of the month
     * @param column the logical column index in the day grid of the month
     * @return the day at the logical grid coordinates in the given month or null
     *    if the coordinates 
     * @throws IllegalStateException if the month is not the start of the month.   
     */
    protected Date getDayInMonth(Date month, int row, int column) {
        if ((row < 0) || (column < 0)) return null;
        Calendar calendar = getCalendar(month);
        int monthField = calendar.get(Calendar.MONTH);
        if (!CalendarUtils.isStartOfMonth(calendar))
            throw new IllegalStateException("calendar must be start of month but was: " + month.getTime());
        CalendarUtils.startOfWeek(calendar);
        calendar.add(Calendar.DAY_OF_MONTH, row * JXMonthView.DAYS_IN_WEEK + column);
        if (calendar.get(Calendar.MONTH) == monthField) {
            return calendar.getTime();
        } 
        return null;
        
    }
    
    
    // ------------------- mapping month parts 
 

    /**
     * Mapping pixel to bounds.<p>
     * 
     * PENDING JW: define the "action grid". Currently this replaces the old
     * version to remove all internal usage of deprecated methods.
     *  
     * @param x the x position of the location in pixel
     * @param y the y position of the location in pixel
     * @return the bounds of the active header area in containing the location
     *   or null if outside.
     */
    protected int getTraversableGridPositionAtLocation(int x, int y) {
        Rectangle headerBounds = getMonthHeaderBoundsAtLocation(x, y);
        if (headerBounds == null) return -1;
        if (y < headerBounds.y + arrowPaddingY) return -1;
        if (y > headerBounds.y + headerBounds.height - arrowPaddingY) return -1;
        headerBounds.setBounds(headerBounds.x + arrowPaddingX, y, 
                headerBounds.width - 2 * arrowPaddingX, headerBounds.height);
        if (!headerBounds.contains(x, y)) return -1;
        Rectangle hitArea = new Rectangle(headerBounds.x, headerBounds.y, monthUpImage.getIconWidth(), monthUpImage.getIconHeight());
        if (hitArea.contains(x, y)) {
            return isLeftToRight ? MONTH_DOWN : MONTH_UP;
        }
        hitArea.translate(headerBounds.width - monthUpImage.getIconWidth(), 0);
        if (hitArea.contains(x, y)) {
            return isLeftToRight ? MONTH_UP : MONTH_DOWN;
        } 
        return -1;
    }
    
    /**
     * Returns the bounds of the month header which contains the 
     * given location. The bounds are in monthView coordinate system.
     * 
     * <p>
     * 
     * @param x the x position of the location in pixel
     * @param y the y position of the location in pixel
     * @return the bounds of the month which contains the location, 
     *   or null if outside
     */
    protected Rectangle getMonthHeaderBoundsAtLocation(int x, int y) {
        Rectangle header = getMonthBoundsAtLocation(x, y);
        if (header == null) return null;
        header.height = getMonthHeaderHeight();
        return header;
    }
    
    /**
     * Returns the bounds of the month details which contains the 
     * given location. The bounds are in monthView coordinate system.
     * 
     * @param x the x position of the location in pixel
     * @param y the y position of the location in pixel
     * @return the bounds of the details grid in the month at
     *   location or null if outside.
     */
    private Rectangle getMonthDetailsBoundsAtLocation(int x, int y) {
        Rectangle month = getMonthBoundsAtLocation(x, y);
        if (month == null) return null;
        int startOfDaysY = month.y + getMonthHeaderHeight();
        if (y < startOfDaysY) return null;
        month.y = startOfDaysY;
        month.height = month.height - getMonthHeaderHeight();
        return month;
    }

    
    // ---------------------- mapping month coordinates    

    /**
      * Returns the bounds of the month which contains the 
     * given location. The bounds are in monthView coordinate system.
     * 
     * <p>
     * 
     * Mapping pixel to bounds.
     * 
     * @param x the x position of the location in pixel
     * @param y the y position of the location in pixel
     * @return the bounds of the month which contains the location, 
     *   or null if outside
     */
    protected Rectangle getMonthBoundsAtLocation(int x, int y) {
        if (!calendarGrid.contains(x, y)) return null;
        int calendarRow = (y - calendarGrid.y) / fullCalendarHeight;
        int calendarColumn = (x - calendarGrid.x) / fullCalendarWidth;
        return new Rectangle( 
                calendarGrid.x + calendarColumn * fullCalendarWidth,
                calendarGrid.y + calendarRow * fullCalendarHeight,
                calendarWidth, calendarHeight);
    }
    
    
    /**
     * 
     * Returns the logical coordinates of the month which contains
     * the given location. The p.x of the returned value represents the column, the
     * p.y represents the row the month is shown in. The transformation takes
     * care of ComponentOrientation. <p>
     * 
     * Mapping pixel to logical grid coordinates.
     * 
     * @param x the x position of the location in pixel
     * @param y the y position of the location in pixel
     * @return the logical coordinates of the month in the grid of month shown by
     *   this monthView or null if outside. 
     */
    protected Point getMonthGridPositionAtLocation(int x, int y) {
        if (!calendarGrid.contains(x, y)) return null;
        int calendarRow = (y - calendarGrid.y) / fullCalendarHeight;
        int calendarColumn = (x - calendarGrid.x) / fullCalendarWidth;
        if (!isLeftToRight) {
            int start = calendarGrid.x + calendarGrid.width;
            calendarColumn = (start - x) / fullCalendarWidth;
              
        }
        return new Point(calendarColumn, calendarRow);
    }

    /**
     * Returns the Date representing the start of the month which 
     * contains the given location.<p>
     * 
     * Mapping pixel to calendar day.
     *
     * @param x the x position of the location in pixel
     * @param y the y position of the location in pixel
     * @return the start of the month which contains the given location or 
     *    null if the location is outside the grid of months.
     */
    protected Date getMonthAtLocation(int x, int y) {
        Point month = getMonthGridPositionAtLocation(x, y);
        if (month ==  null) return null;
        return getMonth(month.y, month.x);
    }
    
    /**
     * Returns the Date representing the start of the month at the given 
     * logical position in the grid of months. <p>
     * 
     * Mapping logical grid coordinates to Calendar.
     * 
     * @param row the rowIndex in the grid of months.
     * @param column the columnIndex in the grid months.
     * @return a Date representing the start of the month at the given
     *   logical coordinates.
     *   
     * @see #getMonthGridPosition(Date)  
     */
    protected Date getMonth(int row, int column) {
        Calendar calendar = getCalendar();
        calendar.add(Calendar.MONTH, 
                row * calendarColumnCount + column);
        return calendar.getTime();
        
    }

    /**
     * Returns the logical grid position of the month containing the given date.
     * The Point's x value is the column in the grid of months, the y value
     * is the row in the grid of months.
     * 
     * Mapping Date to logical grid position, this is the reverse of getMonth(int, int).
     * 
     * @param date the Date to return the bounds for. Must not be null.
     * @return the postion of the month that contains the given date or null if not visible.
     * 
     * @see #getMonth(int, int)
     * @see #getMonthBounds(int, int)
     */
    protected Point getMonthGridPosition(Date date) {
        if (!isVisible(date)) return null;
        // start of grid
        Calendar calendar = getCalendar();
        int firstMonth = calendar.get(Calendar.MONTH);
        int firstYear = calendar.get(Calendar.YEAR);
        
        // 
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        
        int diffMonths = month - firstMonth
            + ((year - firstYear) * JXMonthView.MONTHS_IN_YEAR);
        
        int row = diffMonths / calendarColumnCount;
        int column = diffMonths % calendarColumnCount;

        return new Point(column, row);
    }

    /**
     * Returns the bounds of the month at the given logical coordinates
     * in the grid of visible months.<p>
     * 
     * Mapping logical grip position to pixel.
     * 
     * @param row the rowIndex in the grid of months.
     * @param column the columnIndex in the grid months.
     * @return the bounds of the month at the given logical logical position.
     * 
     * @see #getMonthGridPositionAtLocation(int, int)
     * @see #getMonthBoundsAtLocation(int, int)
     */
    protected Rectangle getMonthBounds(int row, int column) {
        int startY = calendarGrid.y + row * fullCalendarHeight;
        int startX = calendarGrid.x + column * fullCalendarWidth;
        if (!isLeftToRight) {
            startX = calendarGrid.x + (calendarColumnCount - 1 - column) * fullCalendarWidth;
        }
        return new Rectangle(startX, startY, calendarWidth, calendarHeight);
    }

    /**
     * Returns the bounds of the month containing the given date.
     * The bounds are in monthView coordinate system.<p>
     * 
     * Mapping Date to pixel.
     * 
     * @param date the Date to return the bounds for. Must not be null.
     * @return the bounds of the month that contains the given date or null if not visible.
     * 
     * @see #getMonthAtLocation(int, int)
     */
    protected Rectangle getMonthBounds(Date date) {
        Point position = getMonthGridPosition(date);
        return position != null ? getMonthBounds(position.y, position.x) : null;
    }
    


    //---------------- accessors for sizes
    
    /**
     * Returns the size of a month.
     * @return the size of a month.
     */
    protected Dimension getMonthSize() {
        return new Dimension(calendarWidth, calendarHeight);
    }
    
    /**
     * Returns the size of a day including the padding.
     * @return the size of a month.
     */
    protected Dimension getDaySize() {
        return new Dimension(fullBoxWidth, fullBoxHeight);
    }
    /**
     * Returns the height of the month header.
     * 
     * @return the height of the month header.
     */
    protected int getMonthHeaderHeight() {
        return fullMonthBoxHeight;
    }

    

    //-------------------  layout    
    
    /**
     * Called from layout: calculates properties
     * of grid of months.
     */
    private void calculateMonthGridLayoutProperties() {
        calculateMonthGridRowColumnCount();
        calculateMonthGridBounds();
    }
    
    /**
     * Calculates the bounds of the grid of months. 
     * 
     * CalendarRow/ColumnCount and calendarWidth/Height must be
     * initialized before calling this. 
     */
    private void calculateMonthGridBounds() {
        // PENDING JW: this is the "old way" - keep until the deprecated
        // methods are removed.
        calculateStartPositionUnused();
        calendarGrid.setBounds(calculateCalendarGridX(), 
                calculateCalendarGridY(), 
                calculateCalendarGridWidth(), 
                calculateCalendarGridHeight());
    }


    private int calculateCalendarGridY() {
        return (monthView.getHeight() - calculateCalendarGridHeight()) / 2;
    }

    private int calculateCalendarGridX() {
        return (monthView.getWidth() - calculateCalendarGridWidth()) / 2; 
    }
    
    private int calculateCalendarGridHeight() {
        return ((calendarHeight * calendarRowCount) +
                (CALENDAR_SPACING * (calendarRowCount - 1 )));
    }

    private int calculateCalendarGridWidth() {
        return ((calendarWidth * calendarColumnCount) +
                (CALENDAR_SPACING * (calendarColumnCount - 1)));
    }

    /**
     * Calculates and updates the numCalCols/numCalRows that determine the number of
     * calendars that can be displayed. Updates the last displayed date if 
     * appropriate.
     * 
     */
    private void calculateMonthGridRowColumnCount() {
        int oldNumCalCols = calendarColumnCount;
        int oldNumCalRows = calendarRowCount;

        // Determine how many columns of calendars we want to paint.
        calendarColumnCount = 1;
        calendarColumnCount += (monthView.getWidth() - calendarWidth) /
                (calendarWidth + CALENDAR_SPACING);

        // Determine how many rows of calendars we want to paint.
        calendarRowCount = 1;
        calendarRowCount += (monthView.getHeight() - calendarHeight) /
                (calendarHeight + CALENDAR_SPACING);

        if (oldNumCalCols != calendarColumnCount ||
                oldNumCalRows != calendarRowCount) {
            updateLastDisplayedDay(getFirstDisplayedDay());
        }
    }



//-------------------- painting

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);

        Object oldAAValue = null;
        Graphics2D g2 = (g instanceof Graphics2D) ? (Graphics2D)g : null;
        if (g2 != null && monthView.isAntialiased()) {
            oldAAValue = g2.getRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        Rectangle clip = g.getClipBounds();

        Graphics tmp = g.create();
        paintBackground(clip, tmp);
        tmp.dispose();
        
        g.setColor(monthView.getForeground());

        // Get a calender set to the first displayed date
        Calendar cal = getCalendar();
        // Center the calendars horizontally/vertically in the available space.
        for (int row = 0; row < calendarRowCount; row++) {
            // Check if this row falls in the clip region.

            for (int column = 0; column < calendarColumnCount; column++) {
                bounds = getMonthBounds(row, column);
                if (bounds.intersects(clip)) {
                    paintMonth(g, bounds.x, bounds.y, bounds.width, bounds.height, cal);
                }
                // JW: clarified contract for all paint methods:
                // called methods must not change the calendar, its the responsibility
                cal.add(Calendar.MONTH, 1);
            }
        }

        if (g2 != null && monthView.isAntialiased()) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                oldAAValue);
        }
    }

    /**
     * Paints a month.  It is assumed the given calendar is already set to the
     * first day of the month to be painted.<p>
     * 
     * Note: the given calendar must not be changed.
     *
     * @param g Graphics object.
     * @param x x location of month
     * @param y y location of month
     * @param width width of month
     * @param height height of month
     * @param calendar the calendar specifying the the first day of the month to paint, 
     *  must not be null
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void paintMonth(Graphics g, int x, int y, int width, int height, Calendar calendar) {
        // PEINDING JW: remove usage of deprecated api
        // use Date instead of millis

        // Paint month name background.
        paintMonthStringBackground(g, x, y,
                width, fullMonthBoxHeight, calendar);

        paintMonthStringForeground(g, x, y,
                width, fullMonthBoxHeight, calendar);

        // Paint arrow buttons for traversing months if enabled.
        if (monthView.isTraversable()) {
            //draw the icons
            monthDownImage.paintIcon(monthView, g, x + arrowPaddingX, y + ((fullMonthBoxHeight - monthDownImage.getIconHeight()) / 2));
            monthUpImage.paintIcon(monthView, g, x + width - arrowPaddingX - monthUpImage.getIconWidth(), y + ((fullMonthBoxHeight - monthDownImage.getIconHeight()) / 2));
        }

        // Paint background of the short names for the days of the week.
        boolean showingWeekNumber = monthView.isShowingWeekNumber();
        int tmpX = isLeftToRight ? x + (showingWeekNumber ? fullBoxWidth : 0) : x;
        int tmpY = y + fullMonthBoxHeight;
        int tmpWidth = width - (showingWeekNumber ? fullBoxWidth : 0);
        paintDayOfTheWeekBackground(g, tmpX, tmpY, tmpWidth, fullBoxHeight, calendar);

        // Paint short representation of day of the week.
        int dayIndex = monthView.getFirstDayOfWeek() - 1;
        Font oldFont = monthView.getFont();
        g.setFont(derivedFont);
        g.setColor(monthView.getDaysOfTheWeekForeground());
        FontMetrics fm = monthView.getFontMetrics(derivedFont);
        String[] daysOfTheWeek = monthView.getDaysOfTheWeek();
        for (int i = 0; i < JXMonthView.DAYS_IN_WEEK; i++) {
            tmpX = isLeftToRight ?
                    x + (i * fullBoxWidth) + monthView.getBoxPaddingX() +
                            (boxWidth / 2) -
                            (fm.stringWidth(daysOfTheWeek[dayIndex]) /
                                    2) :
                    x + width - (i * fullBoxWidth) - monthView.getBoxPaddingX() -
                            (boxWidth / 2) -
                            (fm.stringWidth(daysOfTheWeek[dayIndex]) /
                                    2);
            if (showingWeekNumber) {
                tmpX += isLeftToRight ? fullBoxWidth : -fullBoxWidth;
            }
            tmpY = y + fullMonthBoxHeight + monthView.getBoxPaddingY() + fm.getAscent();
            g.drawString(daysOfTheWeek[dayIndex], tmpX, tmpY);
            dayIndex++;
            if (dayIndex == JXMonthView.DAYS_IN_WEEK) {
                dayIndex = 0;
            }
        }
        g.setFont(oldFont);
        // new top is below monthBox and daysOfWeek header
        int yNew = y + fullMonthBoxHeight + fullBoxHeight;
        // paint the column of week numbers
        paintWeeksOfYear(g, x, yNew, width, calendar);

        int xOffset = 0;
        if (monthView.isShowingWeekNumber()) {
            xOffset = fullBoxWidth;
        }
        if (isLeftToRight) {
            paintDays(g, x + xOffset, yNew, width - xOffset, calendar);
        } else {
            paintDays(g, x , yNew, width - xOffset, calendar);
        }

    }
    /**
     * 
     * Paints all days in the days' grid, that is the month area below
     * the daysOfWeek and to the right/left (depending on 
     * the monthView's componentOrientation) of the weekOfYears. 
     * The calendar
     * represents the first day of the month to paint. <p>
     * 
     * Note: the calendar must not be changed.
     * 
     * @param g Graphics object.
     * @param left the left boundary of the day grid.
     * @param top the upper boundary of the day grid
     * @param width the width of the day grid.
     * @param cal the calendar specifying the the first day of the month to paint, 
     *   must not be null
     */
    protected void paintDays(Graphics g, int left, int top, int width, Calendar cal) {
        
        Calendar calendar = (Calendar) cal.clone();
        CalendarUtils.startOfMonth(calendar);
        Date startOfMonth = calendar.getTime();
        CalendarUtils.endOfMonth(calendar);
        Date endOfMonth = calendar.getTime();
        // reset the clone
        calendar.setTime(cal.getTime());
        // alwaysfill the day grid in the month completely
//        int weeks = getWeeks(calendar);
        // adjust to start of week 
        calendar.setTime(cal.getTime());
        CalendarUtils.startOfWeek(calendar);
        // painting a grid of day boxes, all with dimensions 
        // width == fullBoxWidth and height = fullBoxHeight.
        int topOfDay = top;
        // 
        for (int week = 0; week < WEEKS_IN_MONTH; week++) {
            // PENDING JW: further simplify - now that we have the reverse mapping
            // (from logical to bounds) - use it! That will keep the RToL logic
            // out of here (which was the whole point of introducing logical coords).
//            for (int week = 0; week <= weeks; week++) {
            int leftOfDay = isLeftToRight ? left : left + width - fullBoxWidth;
            
            for (int day = 0; day < 7; day++) {
                if (calendar.getTime().before(startOfMonth)) {
                    // leading
                    paintLeadingDay(g, leftOfDay, topOfDay, calendar);
                   
                } else if (calendar.getTime().after(endOfMonth)) {
                    paintTrailingDay(g, leftOfDay, topOfDay, calendar);
                    
                } else {
                    paintDay(g, leftOfDay, topOfDay, calendar);
                }
                leftOfDay = isLeftToRight ? leftOfDay + fullBoxWidth : leftOfDay - fullBoxWidth;
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
            // the assumption is not always true - fails if we had the turn-on of DST with
//            if (!CalendarUtils.isStartOfWeek(calendar)) {
//                throw new IllegalStateException("started painting at " + firstStartOfWeek + 
//                		" should still be on the start of a week instead of " + calendar.getTime());
//            }
            topOfDay += fullBoxHeight;
        }
    }

    /**
     * Paints a day the current month, represented by the calendar in a day-box
     * located at left/top. The size of the day-box is defined by
     * fullBoxWidth/Height. The appearance of the day depends on its state (like
     * unselectable, flagged, selected)
     * <p>
     * 
     * Note: the given calendar must not be changed.
     * 
     * @param g the Graphics to paint into.
     * @param left the left boundary of the day-box to paint.
     * @param top the upper boundary of the day-box to paint.
     * @param calendar the calendar specifying the the day to paint, must not be
     *        null
     */
    protected void paintDay(Graphics g, int left, int top, Calendar calendar) {
        if (monthView.isUnselectableDate(calendar.getTime())) {
            paintUnselectableDayBackground(g, left, top, fullBoxWidth, fullBoxHeight,
                    calendar);
            paintUnselectableDayForeground(g, left, top, fullBoxWidth, fullBoxHeight,
                    calendar);

        } else if (monthView.isFlaggedDate(calendar.getTime())) {
            paintFlaggedDayBackground(g, left, top, fullBoxWidth, fullBoxHeight,
                    calendar);
            paintFlaggedDayForeground(g, left, top, fullBoxWidth, fullBoxHeight,
                    calendar);

        } else {
            paintDayBackground(g, left, top, fullBoxWidth, fullBoxHeight,
                    calendar);
            paintDayForeground(g, left, top, fullBoxWidth, fullBoxHeight,
                    calendar);
        }
    }

    /**
     * Paints a trailing day of the current month, represented by the calendar,
     * in a day-box located at left/top. The size of the day-box is defined by
     * fullBoxWidth/Height. Does nothing if the monthView's
     * isShowingTrailingDates is false.
     * <p>
     * 
     * Note: the given calendar must not be changed.
     * 
     * @param g the Graphics to paint into.
     * @param left the left boundary of the day-box to paint.
     * @param top the upper boundary of the day-box to paint.
     * @param calendar the calendar specifying the the day to paint, must not be
     *        null
     */
    protected void paintTrailingDay(Graphics g, int left, int top,
            Calendar calendar) {
        if (!monthView.isShowingTrailingDays()) return;
        paintTrailingDayBackground(g, left, top, fullBoxWidth,
                    fullBoxHeight, calendar);
        paintTrailingDayForeground(g, left, top, fullBoxWidth,
                    fullBoxHeight, calendar);
            
        
    }

    /**
     * Paints a leading day of the current month, represented by the calendar,
     * in a day-box located at left/top. The size of the day-box is defined by
     * fullBoxWidth/Height. Does nothing if the monthView's
     * isShowingLeadingDates is false.
     * <p>
     * 
     * Note: the given calendar must not be changed.
     * 
     * @param g the Graphics to paint into.
     * @param left the left boundary of the day-box to paint.
     * @param top the upper boundary of the day-box to paint.
     * @param calendar the calendar specifying the the day to paint, must not be
     *        null
     */
    protected void paintLeadingDay(Graphics g, int left, int top,
            Calendar calendar) {
        if (!monthView.isShowingLeadingDays()) return;
        paintLeadingDayBackground(g, left, top, fullBoxWidth, fullBoxHeight,
                calendar);
        paintLeadingDayForeground(g, left, top, fullBoxWidth, fullBoxHeight,
                calendar);

    }

    /**
     * Returns the number of weeks to paint in the current month, as represented
     * by the given calendar.
     * 
     * Note: the given calendar must not be changed.
     * 
     * @param month the calendar specifying the the first day of the month to
     *        paint, must not be null
     * @return the number of weeks of this month.
     */
    protected int getWeeks(Calendar month) {
        Date old = month.getTime();
        CalendarUtils.startOfWeek(month);
        int firstWeek = month.get(Calendar.WEEK_OF_YEAR);
        month.setTime(old);
        CalendarUtils.endOfMonth(month);
        int lastWeek = month.get(Calendar.WEEK_OF_YEAR);
        if (lastWeek < firstWeek) {
            lastWeek = month.getActualMaximum(Calendar.WEEK_OF_YEAR) + 1;
        }
        month.setTime(old);
        return lastWeek - firstWeek;
    }

    /**
     * Paints the weeks of year if the showingWeek property is true. Does nothing
     * otherwise.
     * 
     * It is assumed the given calendar is already set to the
     * first day of the month. The calendar is unchanged when leaving this method. 
     * 
     * Note: the given calendar must not be changed.
     * 
     * PENDING JW: this implementation doesn't need the height - should it be given 
     *   anyway for symetry in case subclasses need it?
     *
     * @param g Graphics object.
     * @param x x location of month
     * @param initialY y the upper bound of the "weekNumbers-box"
     * @param width width of month
     * @param cal the calendar specifying the the first day of the month to paint, 
     *  must not be null
     */
    protected void paintWeeksOfYear(Graphics g, int x, int initialY, int width,
            Calendar cal) {
        if (!monthView.isShowingWeekNumber()) return;
        int tmpX = isLeftToRight ? x : x + width - fullBoxWidth;
        paintWeekOfYearBackground(g, tmpX, initialY, fullBoxWidth,
                calendarHeight - (fullMonthBoxHeight + fullBoxHeight), cal);

        Calendar calendar = (Calendar) cal.clone();
        int weeks = getWeeks(calendar);
        calendar.setTime(cal.getTime());
         for (int weekOfYear = 0; weekOfYear <= weeks; weekOfYear++) {
             paintWeekOfYearForeground(g, tmpX, initialY, fullBoxWidth, fullBoxHeight,  calendar);
             initialY += fullBoxHeight;
             calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }
    }

    protected void paintDayOfTheWeekBackground(Graphics g, int x, int y, int width, int height, Calendar cal) {
        int boxPaddingX = monthView.getBoxPaddingX();
        g.drawLine(x + boxPaddingX, y + height - 1, x + width - boxPaddingX, y + height - 1);
    }

    protected void paintWeekOfYearBackground(Graphics g, int x, int y, int width, int height, Calendar cal) {
        int boxPaddingY = monthView.getBoxPaddingY();
        x = isLeftToRight ? x + width - 1 : x;
        g.drawLine(x, y + boxPaddingY, x, y + height - boxPaddingY);
    }

    /**
     * Paints the week of the year of the week of the year represented by the
     * given calendar.
     * <p>
     * 
     * Note: the given calendar must not be changed.
     *
     * @param g Graphics object
     * @param x x-coordinate of upper left corner.
     * @param y y-coordinate of upper left corner.
     * @param width width of bounding box
     * @param height height of bounding box
     */
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    protected void paintWeekOfYearForeground(Graphics g, int x, int y, int width, int height, 
            Calendar cal) {
        String str = Integer.toString(cal.get(Calendar.WEEK_OF_YEAR));
        FontMetrics fm;

        g.setColor(weekOfTheYearForeground);

        int boxPaddingX = monthView.getBoxPaddingX();
        int boxPaddingY = monthView.getBoxPaddingY();

        fm = g.getFontMetrics();
        g.drawString(str,
                isLeftToRight ?
                        x + boxPaddingX +
                                boxWidth - fm.stringWidth(str) :
                        x + boxPaddingX +
                                boxWidth - fm.stringWidth(str) - 1,
                y + boxPaddingY + fm.getAscent());
    }

    /**
     * Paints the background of the month string.  The bounding box for this
     * background can be modified by setting its insets via
     * setMonthStringInsets.  The color of the background can be set via
     * setMonthStringBackground.
     *
     * Note: the given calendar must not be changed.
     *
     * @param g Graphics object to paint to.
     * @param x x-coordinate of upper left corner.
     * @param y y-coordinate of upper left corner.
     * @param width width of the bounding box.
     * @param height height of the bounding box.
     * 
     * @see org.jdesktop.swingx.JXMonthView#setMonthStringBackground
     * @see org.jdesktop.swingx.JXMonthView#setMonthStringInsets
     */
    protected void paintMonthStringBackground(Graphics g, int x, int y,
                                              int width, int height, Calendar cal) {
        // Modify bounds by the month string insets.
        Insets monthStringInsets = monthView.getMonthStringInsets();
        x = isLeftToRight ? x + monthStringInsets.left : x + monthStringInsets.right;
        y = y + monthStringInsets.top;
        width = width - monthStringInsets.left - monthStringInsets.right;
        height = height - monthStringInsets.top - monthStringInsets.bottom;

        g.setColor(monthView.getMonthStringBackground());
        g.fillRect(x, y, width, height);
    }

    /**
     * Note: the given calendar must not be changed.
     * 
     * @param g Graphics object to paint to.
     * @param x x-coordinate of upper left corner.
     * @param y y-coordinate of upper left corner.
     * @param width width of the bounding box.
     * @param height height of the bounding box.
     * @param cal the calendar specifying the day to use, must not be null
     */
    protected void paintMonthStringForeground(Graphics g, int x, int y,
                                              int width, int height, Calendar cal) {
        // Paint month name.
        // 
        Font oldFont = monthView.getFont();

        // TODO: Calculating the bounds of the text dynamically so we can invoke
        // a popup for selecting the month/year to view.
        g.setFont(derivedFont);
        FontMetrics fm = monthView.getFontMetrics(derivedFont);
        int month = cal.get(Calendar.MONTH);
        String monthName = monthsOfTheYear[month];
        String yearString = Integer.toString(cal.get(Calendar.YEAR));

        Rectangle2D rect = fm.getStringBounds(monthName, g);
        monthStringBounds[month] = new Rectangle((int) rect.getX(), (int) rect.getY(),
                (int) rect.getWidth(), (int) rect.getHeight());
        int spaceWidth = (int) fm.getStringBounds(" ", g).getWidth();
        rect = fm.getStringBounds(yearString, g);
        yearStringBounds[month] = new Rectangle((int) rect.getX(), (int) rect.getY(),
                (int) rect.getWidth(), (int) rect.getHeight());
        // END

        g.setColor(monthView.getMonthStringForeground());
        int tmpX =
                x + (calendarWidth / 2) -
                        ((monthStringBounds[month].width + yearStringBounds[month].width + spaceWidth) / 2);
        int tmpY = y + monthView.getBoxPaddingY() + ((monthBoxHeight - boxHeight) / 2) +
                fm.getAscent();
        monthStringBounds[month].x = tmpX;
        yearStringBounds[month].x = (monthStringBounds[month].x + monthStringBounds[month].width +
                spaceWidth);

        paintMonthStringForeground(g,monthName, monthStringBounds[month].x, tmpY, yearString, yearStringBounds[month].x, tmpY, cal);
        g.setFont(oldFont);
    }

    /**
     * Paints only text for month and year. No calculations made. Used by custom LAFs. 
     * <p>
     * 
     * Note: the given calendar must not be changed.
     * 
     * @param g Graphics to paint into.
     * @param monthName Name of the month.
     * @param monthX Month string x coordinate.
     * @param monthY Month string y coordinate.
     * @param yearName Name (number) of the year.
     * @param yearX Year string x coordinate.
     * @param yearY Year string y coordinate.
     */
    protected void paintMonthStringForeground(Graphics g, String monthName, int monthX, int monthY, 
            String yearName, int yearX, int yearY, Calendar cal) {
        g.drawString(monthName, monthX, monthY);
        g.drawString(yearName, yearX, yearY);
    }

    /**
     * Paint the background for the day specified by the given calendar.
     * <p>
     * 
     * Note: the given calendar must not be changed.
     * 
     *
     * @param g Graphics object to paint to
     * @param x x-coordinate of upper left corner
     * @param y y-coordinate of upper left corner
     * @param width width of bounding box for the day
     * @param height height of bounding box for the day
     * @param cal the calendar specifying the day to paint, must not be null
     * @see  org.jdesktop.swingx.JXMonthView#isSelectedDate
     * @see  #isToday
     */
    protected void paintDayBackground(Graphics g, int x, int y, int width, int height,
                                      Calendar cal) {
        Date date = cal.getTime(); //InMillis();
        
        if (monthView.isSelected(date)) {
            g.setColor(monthView.getSelectedBackground());
            g.fillRect(x, y, width, height);
        }

        // If the date is today make sure we draw it's background over the selected
        // background.
        if (isToday(date)) {
            g.setColor(monthView.getTodayBackground());
            g.drawRect(x, y, width - 1, height - 1);
        }
    }

    /**
     * Paint the foreground for the specified day.
     * <p>
     * 
     * Note: the given calendar must not be changed.
     *
     * @param g Graphics object to paint to
     * @param x x-coordinate of upper left corner
     * @param y y-coordinate of upper left corner
     * @param width width of bounding box for the day
     * @param height height of bounding box for the day
     * @param cal the calendar specifying the day to paint, must not be null
     */
    protected void paintDayForeground(Graphics g, int x, int y, int width, int height, Calendar cal) {
        String numericDay = dayOfMonthFormatter.format(cal.getTime());

        g.setColor(monthView.getDayForeground(cal.get(Calendar.DAY_OF_WEEK)));

        int boxPaddingX = monthView.getBoxPaddingX();
        int boxPaddingY = monthView.getBoxPaddingY();

        paintDayForeground(g, numericDay, isLeftToRight ? x + boxPaddingX + boxWidth : x + boxPaddingX + boxWidth - 1,
                y + boxPaddingY, cal);
    }

    /**
     * Paints string of the day. No calculations made. Used by LAFs.
     * <p>
     * 
     * Note: the given calendar must not be changed.
     * @param g Graphics to paint on.
     * @param numericDay Text representation of the day.
     * @param x X coordinate of the upper <b>right</b> corner.
     * @param y Y coordinate of the upper <b>right</b> corner.
     */
    protected void paintDayForeground(Graphics g, String numericDay, int x, int y, Calendar cal) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(numericDay, x - fm.stringWidth(numericDay), y + fm.getAscent());
    }

    /**
     * Paint the background for the specified flagged day. The default implementation just calls
     * <code>paintDayBackground</code>.
     * <p>
     * 
     * Note: the given calendar must not be changed.
     * 
     * @param g Graphics object to paint to
     * @param x x-coordinate of upper left corner
     * @param y y-coordinate of upper left corner
     * @param width width of bounding box for the day
     * @param height height of bounding box for the day
     * @param cal the calendar specifying the day to paint, must not be null
     */
    protected void paintFlaggedDayBackground(Graphics g, int x, int y, int width, int height, Calendar cal) {
        paintDayBackground(g, x, y, width, height, cal);
    }

    /**
     * Paint the foreground for the specified flagged day.
     * <p>
     * 
     * Note: the given calendar must not be changed.
     *
     * @param g Graphics object to paint to
     * @param x x-coordinate of upper left corner
     * @param y y-coordinate of upper left corner
     * @param width width of bounding box for the day
     * @param height height of bounding box for the day
     * @param cal the calendar specifying the day to paint, must not be null
     */
    protected void paintFlaggedDayForeground(Graphics g, int x, int y, int width, int height, Calendar cal) {
        Date date = cal.getTime();
        String numericDay = dayOfMonthFormatter.format(date);
        FontMetrics fm;

        int boxPaddingX = monthView.getBoxPaddingX();
        int boxPaddingY = monthView.getBoxPaddingY();

        Font oldFont = monthView.getFont();
        g.setColor(monthView.getFlaggedDayForeground());
        g.setFont(derivedFont);
        fm = monthView.getFontMetrics(derivedFont);
        g.drawString(numericDay,
                isLeftToRight ?
                        x + boxPaddingX +
                                boxWidth - fm.stringWidth(numericDay):
                        x + boxPaddingX +
                                boxWidth - fm.stringWidth(numericDay) - 1,
                y + boxPaddingY + fm.getAscent());
        g.setFont(oldFont);
    }

    /**
     * Paint the foreground for the specified unselectable day.
     * <p>
     * 
     * Note: the given calendar must not be changed.
     *
     * @param g Graphics object to paint to
     * @param x x-coordinate of upper left corner
     * @param y y-coordinate of upper left corner
     * @param width width of bounding box for the day
     * @param height height of bounding box for the day
     * @param cal the calendar specifying the day to paint, must not be null
     */
    protected void paintUnselectableDayBackground(Graphics g, int x, int y, int width, int height, Calendar cal) {
        paintDayBackground(g, x, y, width, height, cal);
    }

    /**
     * Paint the foreground for the specified unselectable day.
     * <p>
     * 
     * Note: the given calendar must not be changed.
     *
     * @param g Graphics object to paint to
     * @param x x-coordinate of upper left corner
     * @param y y-coordinate of upper left corner
     * @param width width of bounding box for the day
     * @param height height of bounding box for the day
     * @param cal the calendar specifying the day to paint, must not be null
     */
    protected void paintUnselectableDayForeground(Graphics g, int x, int y, int width, int height, Calendar cal) {
        paintDayForeground(g, x, y, width, height, cal);
        g.setColor(unselectableDayForeground);

        String numericDay = dayOfMonthFormatter.format(cal.getTime());
        FontMetrics fm = monthView.getFontMetrics(derivedFont);
        int boxPaddingX = monthView.getBoxPaddingX();
        int boxPaddingY = monthView.getBoxPaddingY();
        width = fm.stringWidth(numericDay);
        height = fm.getAscent();
        x = isLeftToRight ? x + boxPaddingX + boxWidth - fm.stringWidth(numericDay) :
                x + boxPaddingX +
                        boxWidth - fm.stringWidth(numericDay) - 1;
        y = y + boxPaddingY;

        g.drawLine(x, y, x + width, y + height);
        g.drawLine(x + 1, y, x + width + 1, y + height);
        g.drawLine(x + width, y, x, y + height);
        g.drawLine(x + width - 1, y, x - 1, y + height);
    }

    /**
     * Paint the background for the specified leading day.
     * <p>
     * 
     * Note: the given calendar must not be changed.
     *
     * @param g Graphics object to paint to
     * @param x x-coordinate of upper left corner
     * @param y y-coordinate of upper left corner
     * @param width width of bounding box for the day
     * @param height height of bounding box for the day
     * @param cal the calendar specifying the day to paint, must not be null
     */
    protected void paintLeadingDayBackground(Graphics g, int x, int y, int width, int height, Calendar cal) {
        paintDayBackground(g, x, y, width, height, cal);
    }

    /**
     * Paint the foreground for the specified leading day.
     * <p>
     * 
     * Note: the given calendar must not be changed.
     *
     * @param g Graphics object to paint to
     * @param x x-coordinate of upper left corner
     * @param y y-coordinate of upper left corner
     * @param width width of bounding box for the day
     * @param height height of bounding box for the day
     * @param cal the calendar specifying the day to paint, must not be null
     */
    protected void paintLeadingDayForeground(Graphics g, int x, int y, int width, int height, Calendar cal) {
        String numericDay = dayOfMonthFormatter.format(cal.getTime());
        FontMetrics fm;

        g.setColor(leadingDayForeground);

        int boxPaddingX = monthView.getBoxPaddingX();
        int boxPaddingY = monthView.getBoxPaddingY();

        fm = g.getFontMetrics();
        int ltorOffset = x + boxPaddingX +
                boxWidth - fm.stringWidth(numericDay);
        g.drawString(numericDay,
                isLeftToRight ?
                        ltorOffset :
                        ltorOffset - 1,
                y + boxPaddingY + fm.getAscent());
    }

    /**
     * Paint the background for the specified trailing day.
     * <p>
     * 
     * Note: the given calendar must not be changed.
     *
     * @param g Graphics object to paint to
     * @param x x-coordinate of upper left corner
     * @param y y-coordinate of upper left corner
     * @param width width of bounding box for the day
     * @param height height of bounding box for the day
     * @param cal the calendar specifying the day to paint, must not be null
     */
    protected void paintTrailingDayBackground(Graphics g, int x, int y, int width, int height, Calendar cal) {
        paintLeadingDayBackground(g, x, y, width, height, cal);
    }

    /**
     * Paint the foreground for the specified trailing day.
     * <p>
     * 
     * Note: the given calendar must not be changed.
     *
     * @param g Graphics object to paint to
     * @param x x-coordinate of upper left corner
     * @param y y-coordinate of upper left corner
     * @param width width of bounding box for the day
     * @param height height of bounding box for the day
     * @param cal the calendar specifying the day to paint, must not be null
     */
    protected void paintTrailingDayForeground(Graphics g, int x, int y, int width, int height, Calendar cal) {
        String numericDay = dayOfMonthFormatter.format(cal.getTime());
        FontMetrics fm;

        g.setColor(trailingDayForeground);

        int boxPaddingX = monthView.getBoxPaddingX();
        int boxPaddingY = monthView.getBoxPaddingY();

        fm = g.getFontMetrics();
        g.drawString(numericDay,
                isLeftToRight ?
                        x + boxPaddingX +
                                boxWidth - fm.stringWidth(numericDay) :
                        x + boxPaddingX +
                                boxWidth - fm.stringWidth(numericDay) - 1,
                y + boxPaddingY + fm.getAscent());
    }


    protected void paintBackground(final Rectangle clip, final Graphics g) {
        if (monthView.isOpaque()) {
            g.setColor(monthView.getBackground());
            g.fillRect(clip.x, clip.y, clip.width, clip.height);
        }
    }

//---------------------- end of painting
    
//---------------------- navigation support    
    
    private void traverseMonth(int arrowType) {
        if (arrowType == MONTH_DOWN) {
            previousMonth();
        } else if (arrowType == MONTH_UP) {
            nextMonth();
        }
    }

    private void nextMonth() {
        Date upperBound = monthView.getUpperBound();
        if (upperBound == null
                || upperBound.after(getLastDisplayedDay()) ){
            Calendar cal = getCalendar();
            cal.add(Calendar.MONTH, 1);
            monthView.setFirstDisplayedDay(cal.getTime());
        }
    }

    private void previousMonth() {
        Date lowerBound = monthView.getLowerBound();
        if (lowerBound == null
                || lowerBound.before(getFirstDisplayedDay())){
            Calendar cal = getCalendar();
            cal.add(Calendar.MONTH, -1);
            monthView.setFirstDisplayedDay(cal.getTime());
        }
    }

//--------------------------- displayed dates, calendar

    
    /**
     * Returns the monthViews calendar configured to the firstDisplayedDate.
     * 
     * NOTE: it's safe to change the calendar state without resetting because
     * it's JXMonthView's responsibility to protect itself.
     * 
     * @return the monthView's calendar, configured with the firstDisplayedDate.
     */
    protected Calendar getCalendar() {
        return getCalendar(getFirstDisplayedDay());
    }
    
    /**
     * Returns the monthViews calendar configured to the given time.
     * 
     * NOTE: it's safe to change the calendar state without resetting because
     * it's JXMonthView's responsibility to protect itself.
     * 
     * @param date the date to configure the calendar with
     * @return the monthView's calendar, configured with the given date.
     */
    protected Calendar getCalendar(Date date) {
        Calendar calendar = monthView.getCalendar();
        calendar.setTime(date);
        return calendar;
    }

    

    /**
     * Updates the lastDisplayedDate property based on the given first and 
     * visible # of months.
     * 
     * @param first the date of the first visible day.
     */
    private void updateLastDisplayedDay(Date first) {
        Calendar cal = getCalendar(first);
        cal.add(Calendar.MONTH, ((calendarColumnCount * calendarRowCount) - 1));
        CalendarUtils.endOfMonth(cal);
        lastDisplayedDate = cal.getTime();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Date getLastDisplayedDay() {
        return lastDisplayedDate;
    }

    /*-------------- refactored: encapsulate aliased fields
     */

    /**
     * Sets the firstDisplayedDate property to the given value. Must update
     * dependent state as well. 
     * 
     * Here: updated lastDisplayedDatefirstDisplayedMonth/Year accordingly.
     * 
     * 
     * @param firstDisplayedDate the firstDisplayedDate to set
     */
    protected void setFirstDisplayedDay(Date firstDisplayedDate) {
        Calendar calendar = getCalendar(firstDisplayedDate);
        this.firstDisplayedDate = firstDisplayedDate;
        this.firstDisplayedMonth = calendar.get(Calendar.MONTH);
        this.firstDisplayedYear = calendar.get(Calendar.YEAR);
        updateLastDisplayedDay(firstDisplayedDate);
        monthView.repaint();
    }
    /**
     * @return the firstDisplayedDate
     */
    protected Date getFirstDisplayedDay() {
        return firstDisplayedDate;
    }

    /**
     * @return the firstDisplayedMonth
     */
    protected int getFirstDisplayedMonth() {
        return firstDisplayedMonth;
    }


    /**
     * @return the firstDisplayedYear
     */
    protected int getFirstDisplayedYear() {
        return firstDisplayedYear;
    }


    /**
     * @return the selection
     */
    protected SortedSet<Date> getSelection() {
        return monthView.getSelection();
    }
    
    
    /**
     * @return the start of today.
     */
    protected Date getToday() {
        return monthView.getToday();
    }

    /**
     * Returns true if the date passed in is the same as today.
     *
     * PENDING JW: really want the exact test?
     * 
     * @param date long representing the date you want to compare to today.
     * @return true if the date passed is the same as today.
     */
    protected boolean isToday(Date date) {
        return date.equals(getToday());
    }
    

//-----------------------end encapsulation
 
    
//------------------ Handler implementation 
//  
    /**
     * temporary: removed SelectionMode.NO_SELECTION, replaced
     * all access by this method to enable easy re-adding, if we want it.
     * If not - remove.
     */
    private boolean canSelectByMode() {
        return true;
    }
    

    private class Handler implements  
        MouseListener, MouseMotionListener, LayoutManager,
            PropertyChangeListener, DateSelectionListener {
        private boolean armed;
        private Date startDate;
        private Date endDate;

        public void mouseClicked(MouseEvent e) {}

        public void mousePressed(MouseEvent e) {
            // If we were using the keyboard we aren't anymore.
            setUsingKeyboard(false);

            if (!monthView.isEnabled()) {
                return;
            }

            if (!monthView.hasFocus() && monthView.isFocusable()) {
                monthView.requestFocusInWindow();
            }

            // Check if one of the month traverse buttons was pushed.
            if (monthView.isTraversable()) {
                int arrowType = getTraversableGridPositionAtLocation(e.getX(), e.getY());
                if (arrowType != -1) {
                    traverseMonth(arrowType);
                    return;
                }
            }

            if (!canSelectByMode()) {
                return;
            }

            
//            long selected = monthView.getDayAt(e.getX(), e.getY());
            Date cal = getDayAtLocation(e.getX(), e.getY());
            if (cal == null) {
                return;
            }

            // Update the selected dates.
            startDate = cal;
            endDate = cal;

            if (monthView.getSelectionMode() == SelectionMode.SINGLE_INTERVAL_SELECTION ||
//                    selectionMode == SelectionMode.WEEK_INTERVAL_SELECTION ||
                    monthView.getSelectionMode() == SelectionMode.MULTIPLE_INTERVAL_SELECTION) {
                pivotDate = startDate;
            }

            monthView.getSelectionModel().setAdjusting(true);
            
            if (monthView.getSelectionMode() == SelectionMode.MULTIPLE_INTERVAL_SELECTION && e.isControlDown()) {
                monthView.addSelectionInterval(startDate, endDate);
            } else {
                monthView.setSelectionInterval(startDate, endDate);
            }

            // Arm so we fire action performed on mouse release.
            armed = true;
        }

        
        public void mouseReleased(MouseEvent e) {
            // If we were using the keyboard we aren't anymore.
            setUsingKeyboard(false);

            if (!monthView.isEnabled()) {
                return;
            }

            if (!monthView.hasFocus() && monthView.isFocusable()) {
                monthView.requestFocusInWindow();
            }
            
            if (armed) {
                monthView.commitSelection();
            }
            armed = false;
        }

        public void mouseEntered(MouseEvent e) {}

        public void mouseExited(MouseEvent e) {}

        public void mouseDragged(MouseEvent e) {
            // If we were using the keyboard we aren't anymore.
            setUsingKeyboard(false);
            if (!monthView.isEnabled() || !canSelectByMode()) {
                return;
            }

//            long selected = monthView.getDayAt(e.getX(), e.getY());

            Date cal = getDayAtLocation(e.getX(), e.getY());
            if (cal == null) {
                return;
            }

            Date selected = cal;
            Date oldStart = startDate;
            Date oldEnd = endDate;

            if (monthView.getSelectionMode() == SelectionMode.SINGLE_SELECTION) {
                if (selected.equals(oldStart)) {
                    return;
                }
                startDate = selected;
                endDate = selected;
            } else {
                if (selected.before(pivotDate)) {
                    startDate = selected;
                    endDate = pivotDate;
                } else if (selected.after(pivotDate)) {
                    startDate = pivotDate;
                    endDate = selected;
                }
            }

            if (startDate.equals(oldStart) && endDate.equals(oldEnd)) {
                return;
            }

            if (monthView.getSelectionMode() == SelectionMode.MULTIPLE_INTERVAL_SELECTION && e.isControlDown()) {
                monthView.addSelectionInterval(startDate, endDate);
            } else {
                monthView.setSelectionInterval(startDate, endDate);
            }

            // Set trigger.
            armed = true;
        }

        public void mouseMoved(MouseEvent e) {}

//------------------------ layout
        
        
        private Dimension preferredSize = new Dimension();

        public void addLayoutComponent(String name, Component comp) {}

        public void removeLayoutComponent(Component comp) {}

        public Dimension preferredLayoutSize(Container parent) {
            layoutContainer(parent);
            return new Dimension(preferredSize);
        }

        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        public void layoutContainer(Container parent) {
            // Loop through year and get largest representation of the month.
            // Keep track of the longest month so we can loop through it to
            // determine the width of a date box.
            int currDays;
            int longestMonth = 0;
            int daysInLongestMonth = 0;

            int currWidth;
            int longestMonthWidth = 0;

            // We use a bold font for figuring out size constraints since
            // it's larger and flaggedDates will be noted in this style.
            FontMetrics fm = monthView.getFontMetrics(derivedFont);
            // JW PENDING: relies on calendar being set at least to year?
            // No, just on the bare calendar - so don't care about actual time
            Calendar cal = getCalendar();
            cal.set(Calendar.MONTH, cal.getMinimum(Calendar.MONTH));
            cal.set(Calendar.DAY_OF_MONTH,
                    cal.getActualMinimum(Calendar.DAY_OF_MONTH));
            for (int i = 0; i < cal.getMaximum(Calendar.MONTH); i++) {
                currWidth = fm.stringWidth(monthsOfTheYear[i]);
                if (currWidth > longestMonthWidth) {
                    longestMonthWidth = currWidth;
                }
                currDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                if (currDays > daysInLongestMonth) {
                    longestMonth = cal.get(Calendar.MONTH);
                    daysInLongestMonth = currDays;
                }
                cal.add(Calendar.MONTH, 1);
            }

            // Loop through the days of the week and adjust the box width
            // accordingly.
            boxHeight = fm.getHeight();
            String[] daysOfTheWeek = monthView.getDaysOfTheWeek();
            for (String dayOfTheWeek : daysOfTheWeek) {
                currWidth = fm.stringWidth(dayOfTheWeek);
                if (currWidth > boxWidth) {
                    boxWidth = currWidth;
                }
            }

            // Loop through longest month and get largest representation of the day
            // of the month.
            cal.set(Calendar.MONTH, longestMonth);
            cal.set(Calendar.DAY_OF_MONTH,
                    cal.getActualMinimum(Calendar.DAY_OF_MONTH));
            for (int i = 0; i < daysInLongestMonth; i++) {
                currWidth = fm.stringWidth(
                        dayOfMonthFormatter.format(cal.getTime()));
                if (currWidth > boxWidth) {
                    boxWidth = currWidth;
                }
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }

            // If we are displaying week numbers find the largest displayed week number.
            boolean showingWeekNumber = monthView.isShowingWeekNumber();
            if (showingWeekNumber) {
                int val = cal.getActualMaximum(Calendar.WEEK_OF_YEAR);
                currWidth = fm.stringWidth(Integer.toString(val));
                if (currWidth > boxWidth) {
                    boxWidth = currWidth;
                }
            }

            // If the calendar is traversable, check the icon heights and
            // adjust the month box height accordingly.
            monthBoxHeight = boxHeight;
            if (monthView.isTraversable()) {
                int newHeight = monthDownImage.getIconHeight() +
                        arrowPaddingY + arrowPaddingY;
                if (newHeight > monthBoxHeight) {
                    monthBoxHeight = newHeight;
                }
            }

            // Modify boxWidth if month string is longer
            int boxPaddingX = monthView.getBoxPaddingX();
            int boxPaddingY = monthView.getBoxPaddingY();
            preferredSize.width = (boxWidth + (2 * boxPaddingX)) * JXMonthView.DAYS_IN_WEEK;
            if (preferredSize.width < longestMonthWidth) {
                double diff = longestMonthWidth - preferredSize.width;
                if (monthView.isTraversable()) {
                    diff += monthDownImage.getIconWidth() +
                            monthUpImage.getIconWidth() + (arrowPaddingX * 4);
                }
                boxWidth += Math.ceil(diff / (double)JXMonthView.DAYS_IN_WEEK);
            }


            // Keep track of a full box height/width and full month box height
            fullBoxWidth = boxWidth + boxPaddingX + boxPaddingX;
            fullBoxHeight = boxHeight + boxPaddingY + boxPaddingY;
            fullMonthBoxHeight = monthBoxHeight + boxPaddingY + boxPaddingY;

            // Keep track of calendar width and height for use later.
            calendarWidth = fullBoxWidth * JXMonthView.DAYS_IN_WEEK;
            if (showingWeekNumber) {
                calendarWidth += fullBoxWidth;
            }
            fullCalendarWidth = calendarWidth + CALENDAR_SPACING;
            
            calendarHeight = (fullBoxHeight * 7) + fullMonthBoxHeight;
            fullCalendarHeight = calendarHeight + CALENDAR_SPACING;
            // Calculate minimum width/height for the component.
            int prefRows = monthView.getPreferredRows();
            preferredSize.height = (calendarHeight * prefRows) +
                    (CALENDAR_SPACING * (prefRows - 1));

            int prefCols = monthView.getPreferredCols();
            preferredSize.width = (calendarWidth * prefCols) +
                    (CALENDAR_SPACING * (prefCols - 1));

            // Add insets to the dimensions.
            Insets insets = monthView.getInsets();
            preferredSize.width += insets.left + insets.right;
            preferredSize.height += insets.top + insets.bottom;
           
            calculateMonthGridLayoutProperties();

        }


        public void propertyChange(PropertyChangeEvent evt) {
            String property = evt.getPropertyName();

            if ("componentOrientation".equals(property)) {
                isLeftToRight = monthView.getComponentOrientation().isLeftToRight();
                monthView.revalidate();
            } else if (JXMonthView.SELECTION_MODEL.equals(property)) {
                DateSelectionModel selectionModel = (DateSelectionModel) evt.getOldValue();
                selectionModel.removeDateSelectionListener(getHandler());
                selectionModel = (DateSelectionModel) evt.getNewValue();
                selectionModel.addDateSelectionListener(getHandler());
            } else if ("firstDisplayedDay".equals(property)) {
                setFirstDisplayedDay(((Date) evt.getNewValue()));
            } else if (JXMonthView.BOX_PADDING_X.equals(property) 
                    || JXMonthView.BOX_PADDING_Y.equals(property) 
                    || JXMonthView.TRAVERSABLE.equals(property) 
                    || JXMonthView.DAYS_OF_THE_WEEK.equals(property) 
                    || "border".equals(property) 
                    || "showingWeekNumber".equals(property)
                    || "traversable".equals(property) 
                    
                    ) {
                monthView.revalidate();
                monthView.repaint();
            } else if ("font".equals(property)) {
                derivedFont = createDerivedFont();
                monthView.revalidate();
            } else if ("componentInputMapEnabled".equals(property)) {
                updateComponentInputMap();
            } else if ("locale".equals(property)) { // "locale" is bound property
                updateLocale();
            } else if ("timeZone".equals(property)) {
                dayOfMonthFormatter.setTimeZone((TimeZone) evt.getNewValue());
            } else if ("flaggedDates".equals(property)
                || "showingTrailingDays".equals(property)
                || "showingLeadingDays".equals(property)
                || "today".equals(property)
                || "antialiased".equals(property)
                ) {
                monthView.repaint();
            } else {
//                LOG.info("got propertyChange:" + property);
            }
        }

        public void valueChanged(DateSelectionEvent ev) {
            // repaint old dirty region
//            monthView.repaint(dirtyRect);
//            // calculate new dirty region based on selection
//            calculateDirtyRectForSelection();
//            // repaint new selection
//            monthView.repaint(dirtyRect);
            monthView.repaint();
        }


    }

    /**
     * Class that supports keyboard traversal of the JXMonthView component.
     */
    private class KeyboardAction extends AbstractAction {
        public static final int ACCEPT_SELECTION = 0;
        public static final int CANCEL_SELECTION = 1;
        public static final int SELECT_PREVIOUS_DAY = 2;
        public static final int SELECT_NEXT_DAY = 3;
        public static final int SELECT_DAY_PREVIOUS_WEEK = 4;
        public static final int SELECT_DAY_NEXT_WEEK = 5;
        public static final int ADJUST_SELECTION_PREVIOUS_DAY = 6;
        public static final int ADJUST_SELECTION_NEXT_DAY = 7;
        public static final int ADJUST_SELECTION_PREVIOUS_WEEK = 8;
        public static final int ADJUST_SELECTION_NEXT_WEEK = 9;

        private int action;

        public KeyboardAction(int action) {
            this.action = action;
        }

        public void actionPerformed(ActionEvent ev) {
            if (!canSelectByMode())
                return;
            if (!isUsingKeyboard()) {
                originalDateSpan = getSelection();
            }
            // JW: removed the isUsingKeyboard from the condition
            // need to fire always.
            if (action >= ACCEPT_SELECTION && action <= CANCEL_SELECTION) { 
                // refactor the logic ...
                if (action == CANCEL_SELECTION) {
                    // Restore the original selection.
                    if ((originalDateSpan != null)
                            && !originalDateSpan.isEmpty()) {
                        monthView.setSelectionInterval(
                                originalDateSpan.first(), originalDateSpan
                                        .last());
                    } else {
                        monthView.clearSelection();
                    }
                    monthView.cancelSelection();
                } else {
                    // Accept the keyboard selection.
                    monthView.commitSelection();
                }
                setUsingKeyboard(false);
            } else if (action >= SELECT_PREVIOUS_DAY
                    && action <= SELECT_DAY_NEXT_WEEK) {
                setUsingKeyboard(true);
                monthView.getSelectionModel().setAdjusting(true);
                pivotDate = null;
                traverse(action);
            } else if (monthView.getSelectionMode() == SelectionMode.SINGLE_INTERVAL_SELECTION
                    && action >= ADJUST_SELECTION_PREVIOUS_DAY
                    && action <= ADJUST_SELECTION_NEXT_WEEK) {
                setUsingKeyboard(true);
                monthView.getSelectionModel().setAdjusting(true);
                addToSelection(action);
            }
        }

        private void traverse(int action) {
            Date oldStart = monthView.isSelectionEmpty() ? 
                    monthView.getToday() : monthView.getFirstSelectionDate();
            Calendar cal = getCalendar(oldStart);
            switch (action) {
                case SELECT_PREVIOUS_DAY:
                    cal.add(Calendar.DAY_OF_MONTH, -1);
                    break;
                case SELECT_NEXT_DAY:
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    break;
                case SELECT_DAY_PREVIOUS_WEEK:
                    cal.add(Calendar.DAY_OF_MONTH, -JXMonthView.DAYS_IN_WEEK);
                    break;
                case SELECT_DAY_NEXT_WEEK:
                    cal.add(Calendar.DAY_OF_MONTH, JXMonthView.DAYS_IN_WEEK);
                    break;
            }

            Date newStartDate = cal.getTime();
            if (!newStartDate.equals(oldStart)) {
                monthView.setSelectionInterval(newStartDate, newStartDate);
                monthView.ensureDateVisible(newStartDate);
            }
        }

        /**
         * If we are in a mode that allows for range selection this method
         * will extend the currently selected range.
         *
         * NOTE: This may not be the expected behavior for the keyboard controls
         * and we ay need to update this code to act in a way that people expect.
         *
         * @param action action for adjusting selection
         */
        private void addToSelection(int action) {
            // PENDING JW: remove use of deprecated
            // use Date always!
            Date newStartDate;
            Date newEndDate;
            Date selectionStart;
            Date selectionEnd;
            if (!monthView.isSelectionEmpty()) {
                newStartDate = selectionStart = monthView.getFirstSelectionDate();
                newEndDate = selectionEnd = monthView.getLastSelectionDate();
            } else {
                newStartDate = selectionStart = monthView.getToday();
                newEndDate = selectionEnd = newStartDate;
            }

            if (pivotDate == null) {
                pivotDate = newStartDate;
            }

            boolean isForward = true;
            // want a copy to play with - each branch sets and reads the time
            // actually don't care about the pre-set time.
            Calendar cal = getCalendar();
            switch (action) {
                case ADJUST_SELECTION_PREVIOUS_DAY:
                    if (!newEndDate.after(pivotDate)) {
                        cal.setTime(newStartDate);
                        cal.add(Calendar.DAY_OF_MONTH, -1);
                        newStartDate = cal.getTime();
                    } else {
                        cal.setTime(newEndDate);
                        cal.add(Calendar.DAY_OF_MONTH, -1);
                        newEndDate = cal.getTime();
                    }
                    isForward = false;
                    break;
                case ADJUST_SELECTION_NEXT_DAY:
                    if (!newStartDate.before(pivotDate)) {
                        cal.setTime(newEndDate);
                        cal.add(Calendar.DAY_OF_MONTH, 1);
                        newStartDate = pivotDate;
                        newEndDate = cal.getTime();
                    } else {
                        cal.setTime(newStartDate);
                        cal.add(Calendar.DAY_OF_MONTH, 1);
                        newStartDate = cal.getTime();
                    }
                    break;
                case ADJUST_SELECTION_PREVIOUS_WEEK:
                    if (!newEndDate.after(pivotDate)) {
                        cal.setTime(newStartDate);
                        cal.add(Calendar.DAY_OF_MONTH, -JXMonthView.DAYS_IN_WEEK);
                        newStartDate = cal.getTime();
                    } else {
                        cal.setTime(newEndDate);
                        cal.add(Calendar.DAY_OF_MONTH, -JXMonthView.DAYS_IN_WEEK);
                        Date newTime = cal.getTime();
                        if (!newTime.after(pivotDate)) {
                            newStartDate = newTime;
                            newEndDate = pivotDate;
                        } else {
                            newEndDate = cal.getTime();
                        }

                    }
                    isForward = false;
                    break;
                case ADJUST_SELECTION_NEXT_WEEK:
                    if (!newStartDate.before(pivotDate)) {
                        cal.setTime(newEndDate);
                        cal.add(Calendar.DAY_OF_MONTH, JXMonthView.DAYS_IN_WEEK);
                        newEndDate = cal.getTime();
                    } else {
                        cal.setTime(newStartDate);
                        cal.add(Calendar.DAY_OF_MONTH, JXMonthView.DAYS_IN_WEEK);
                        Date newTime = cal.getTime();
                        if (!newTime.before(pivotDate)) {
                            newStartDate = pivotDate;
                            newEndDate = newTime;
                        } else {
                            newStartDate = cal.getTime();
                        }
                    }
                    break;
            }
            if (!newStartDate.equals(selectionStart) || !newEndDate.equals(selectionEnd)) {
                monthView.setSelectionInterval(newStartDate, newEndDate);
                monthView.ensureDateVisible(isForward ? newEndDate  : newStartDate);
            }

        }
        

    }

// -- deprecated methods, no longer used internally, kept a short while
    
    

    /**
     * Get the view index for the specified day of the week.  This value will range
     * from 0 to DAYS_IN_WEEK - 1.  For example if the first day of the week was set
     * to Calendar.MONDAY and we requested the view index for Calendar.TUESDAY the result
     * would be 1.
     *
     * @param dayOfWeek day of the week to calculate view index for, acceptable values are
     * <code>Calendar.MONDAY</code> - <code>Calendar.SUNDAY</code>
     * @return view index for the specified day of the week
     * @deprecated with revised location/date mapping no longer needed, 
     *     no longer used internally
     */
    @Deprecated
    @SuppressWarnings("unused")
    private int getDayOfWeekViewIndex(int dayOfWeek) {
        int result = dayOfWeek - monthView.getFirstDayOfWeek();
        if (result < 0) {
            result += JXMonthView.DAYS_IN_WEEK;
        }
        return result;
    }
    /**
     * Returns an index defining which, if any, of the buttons for
     * traversing the month was pressed.  This method should only be
     * called when <code>setTraversable</code> is set to true.
     *
     * @param x x position of the pointer
     * @param y y position of the pointer
     * @return MONTH_UP, MONTH_DOWN or -1 when no button is selected.
     * 
     * @deprecated use {@link #getTraversableGridPositionAtLocation(int, int)}
     */
    @Deprecated
    protected int getTraversableButtonAt(int x, int y) {
        return getTraversableGridPositionAtLocation(x, y);
    }

    /**
     * Get the row and column for the calendar at the specified coordinates
     *
     * @param x x location
     * @param y y location
     * @return a new <code>Point</code> object containing the row as the x value
     * and column as the y value
     * @deprecated use {@link #getMonthGridPositionAtLocation(int, int)} - this method is
     *   no longer used internally. Note that the coordinate mapping in the 
     *   returned Point of new method is the other way round 
     *   (p.x == column, p.y == row) as in this!
     */
    @Deprecated
    protected Point getCalRowColAt(int x, int y) {
        if (isLeftToRight ? (startX > x) : (startX < x) || startY > y) {
            return NO_SUCH_CALENDAR;
        }

        Point result = new Point();
        // Determine which row of calendars we're in.
        result.x = (y - startY) / (calendarHeight + CALENDAR_SPACING);

        // Determine which column of calendars we're in.
        result.y = (isLeftToRight ? (x - startX) : (startX - x)) /
                (calendarWidth + CALENDAR_SPACING);

        // Make sure the row and column of calendars calculated is being
        // managed.
        if (result.x > calendarRowCount - 1 || result.y > calendarColumnCount -1) {
            result = NO_SUCH_CALENDAR;
        }

        return result;
    }
    /**
     * old way of calculating the position of the grid of months.
     * This is no longer used in current code, only in deprecated.
     * 
     * @deprecated the result is no longer used except from deprecated methods.
     */
    @Deprecated
    private void calculateStartPositionUnused() {
        // Calculate offset in x-axis for centering calendars.
        int width = monthView.getWidth();
        startX = (width - calculateCalendarGridWidth()) / 2;
        if (!isLeftToRight) {
            startX = width - startX;
        }

        // Calculate offset in y-axis for centering calendars.
        startY = calculateCalendarGridY();
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated use {@link #getDayAtLocation(int, int)} This method is
     * no longer used internally
     */
    @Deprecated
    @Override
    public long getDayAt(int x, int y) {
        Date cal = getDayAtLocation(x, y);
        return cal != null ? cal.getTime() : -1;
    }

    /**
     * @return the start of today.
     * 
     * @deprecated use {@link #getToday()}
     */
    @Deprecated
    protected long getTodayInMillis() {
        return getToday().getTime();
    }
    
    
    /**
     * Returns true if the date passed in is the same as today.
     *
     * PENDING JW: really want the exact test?
     * 
     * @param date long representing the date you want to compare to today.
     * @return true if the date passed is the same as today.
     * 
     * @deprecated use {@link #isToday(Date)}
     */
    @Deprecated
    protected boolean isToday(long date) {
        return date == getToday().getTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastDisplayedDate() {
        return lastDisplayedDate.getTime();
    }

    /**
     * {@inheritDoc} <p>
     * 
     * This method will be hidden soon: the newer perspective is that the
     * ui is responsible to keep the value in a reasonable state to query from
     * the outside. 
     */
    @Override
    public long calculateLastDisplayedDate() {
        updateLastDisplayedDay(getFirstDisplayedDay());
        // NOTE JW: this is the only place (outside the getter/setter) 
        // where the field is accessed directly - no need to cleanup because
        // this will be removed before final.
        return lastDisplayedDate.getTime();
    }

    /**
     * Sets the firstDisplayedDate property to the given value. Must update
     * dependent state as well. 
     * 
     * Here: updated lastDisplayedDatefirstDisplayedMonth/Year accordingly.
     * 
     * 
     * @param firstDisplayedDate the firstDisplayedDate to set
     * 
     * @deprecated use {@link #setFirstDisplayedDay(Date)}
     */
    @Deprecated
    protected void setFirstDisplayedDate(long firstDisplayedDate) {
        setFirstDisplayedDay(new Date(firstDisplayedDate));
    }
    /**
     * @return the firstDisplayedDate
     * 
     * @deprecated use {@link #getFirstDisplayedDay()}
     */
    @Deprecated
    protected long getFirstDisplayedDate() {
        return firstDisplayedDate.getTime();
    }

    /**
     * Returns the monthViews calendar configured to the given time.
     * 
     * NOTE: it's safe to change the calendar state without resetting because
     * it's JXMonthView's responsibility to protect itself.
     * 
     * @param millis the date to configure the calendar with
     * @return the monthView's calendar, configured with the given date.
     * @deprecated use {@link #getCalendar(Date)}
     */
    @Deprecated
    protected Calendar getCalendar(long millis) {
        Calendar calendar = monthView.getCalendar();
        calendar.setTimeInMillis(millis);
        return calendar;
    }


    
}
