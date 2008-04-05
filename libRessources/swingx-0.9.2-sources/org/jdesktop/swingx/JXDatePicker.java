/*
 * $Id: JXDatePicker.java,v 1.81 2008/02/26 14:22:05 kleopatra Exp $
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
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.text.DateFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EventListener;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.text.DefaultFormatterFactory;

import org.jdesktop.swingx.calendar.DatePickerFormatter;
import org.jdesktop.swingx.event.EventListenerMap;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.plaf.DatePickerAddon;
import org.jdesktop.swingx.plaf.DatePickerUI;
import org.jdesktop.swingx.plaf.LookAndFeelAddons;
import org.jdesktop.swingx.plaf.UIManagerExt;
import org.jdesktop.swingx.util.Contract;

/**
 * A component for entering dates with a user interaction similar to a
 * JComboBox. The dates can be typed into a text field or selected from a
 * JXMonthView which opens in a JXPopupMenu on user's request.
 * <p>
 * 
 * The date selection is controlled by the JXMonthView's DateSelectionModel.
 * This allows the use of all its functionality in the JXDatePicker as well.
 * F.i. restrict the selection to a date in the current or next week:
 * <p>
 * 
 * <pre><code>
 * Appointment appointment = new Appointment(director,
 *         &quot;Be sure to have polished shoes!&quot;);
 * JXDatePicker picker = new JXDatePicker();
 * Calendar calendar = picker.getMonthView().getCalendar();
 * // starting today if we are in a hurry
 * calendar.setTime(new Date());
 * picker.getMonthView().setLowerBound(calendar.getTime());
 * // end of next week
 * CalendarUtils.endOfWeek(calendar);
 * calendar.add(Calendar.WEEK_OF_YEAR);
 * picker.getMonthView().setUpperBound(calendar.getTime());
 * </code></pre>
 * 
 * Similar to a JXMonthView, the JXDatePicker fire an ActionEvent when the user
 * actively commits or cancels a selection. Interested client code can add a
 * ActionListener to be notified by the user action.
 * 
 * <pre><code>
 * JXDatePicker picker = new JXDatePicker(new Date());
 * ActionListener l = new ActionListener() {
 *     public void actionPerformed(ActionEvent e) {
 *         if (JXDatePicker.COMMIT_KEY.equals(e.getActionCommand)) {
 *             safeDate(picker.getDate());
 *         }
 *     }
 * };
 * picker.addActionListener(l);
 * </code></pre>
 * 
 * <p>
 * The DateFormats used in the JXDatePicker's are initialized to the default
 * formats of the DatePickerFormatter, as defined by the picker's resourceBundle
 * DatePicker.properties. Application code can overwrite the picker's default
 * 
 * <pre><code>
 * picker.setDateFormats(myCustomFormat, myAlternativeCustomFormat);
 * </code></pre>
 * 
 * PENDING JW: explain what the alternatives are for (after understanding it
 * myself ;-)
 * <p>
 * 
 * The selected Date is a bound property of the JXDatePicker. This allows easy
 * binding to a property of a custom bean when using a binding framework.
 * <p>
 * 
 * @author Joshua Outwater
 * @author Jeanette Winzenburg
 * 
 * @see JXMonthView
 * @see DateSelectionModel
 * @see DatePickerFormatter
 * 
 */
public class JXDatePicker extends JComponent {
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(JXDatePicker.class
            .getName());
    static {
        LookAndFeelAddons.contribute(new DatePickerAddon());
    }

    /**
     * UI Class ID
     */
    public static final String uiClassID = "DatePickerUI";

    public static final String EDITOR = "editor";
    public static final String MONTH_VIEW = "monthView";
    public static final String DATE_IN_MILLIS = "dateInMillis";
    public static final String LINK_PANEL = "linkPanel";

    /** action command used for commit actionEvent. */
    public static final String COMMIT_KEY = "datePickerCommit";
    /** action command used for cancel actionEvent. */
    public static final String CANCEL_KEY = "datePickerCancel";
    /** action key for navigate home action */
    public static final String HOME_NAVIGATE_KEY = "navigateHome";
    /** action key for commit home action */
    public static final String HOME_COMMIT_KEY = "commitHome";

    private static final DateFormat[] EMPTY_DATE_FORMATS = new DateFormat[0];

    /**
     * The editable date field that displays the date
     */
    private JFormattedTextField _dateField;

    /**
     * Popup that displays the month view with controls for
     * traversing/selecting dates.
     */
    private JPanel _linkPanel;
    private MessageFormat _linkFormat;
    private Date linkDate;
    
    private JXMonthView _monthView;
    private String _actionCommand = "selectionChanged";
    private boolean editable = true;
    private EventListenerMap listenerMap;
    protected boolean lightWeightPopupEnabled = JPopupMenu.getDefaultLightWeightPopupEnabled();

    private Date date;

    private PropertyChangeListener monthViewListener;


    /**
     * Create a new date picker using the current date as the initial
     * selection and the default abstract formatter
     * <code>JXDatePickerFormatter</code>.
     * <p/>
     * The date picker is configured with the default time zone and locale
     *
     * @see #setTimeZone
     * @see #getTimeZone
     */
    public JXDatePicker() {
        this(null, null);
    }

    

    /**
     * Create a new date picker using the specified time as the initial
     * selection and the default abstract formatter
     * <code>JXDatePickerFormatter</code>.
     * <p/>
     * The date picker is configured with the default time zone and locale
     *
     * @param selected the initially selected date
     * @see #setTimeZone
     * @see #getTimeZone
     */
    public JXDatePicker(Date selected) {
        this(selected, null);
    }
    
    /**
     * Create a new date picker using the current date as the initial
     * selection and the default abstract formatter
     * <code>JXDatePickerFormatter</code>.
     * <p/>
     * The date picker is configured with the default time zone and specified 
     * locale
     *
     * @param locale    initial Locale
     * @see #setTimeZone
     * @see #getTimeZone
     */
    public JXDatePicker(Locale locale) {
        this(null, locale);
    }

    /**
     * Create a new date picker using the specified time as the initial
     * selection and the default abstract formatter
     * <code>JXDatePickerFormatter</code>.
     * <p/>
     * The date picker is configured with the default time zone and specified locale
     *
     * @param selection initially selected Date
     * @param locale initial Locale
     * @see #setTimeZone
     * @see #getTimeZone
     */
    public JXDatePicker(Date selection, Locale locale) {
        init();
        if (locale != null) {
            setLocale(locale);
        }
        // install the controller before setting the date
        updateUI();
        setDate(selection);
    }

    /**
     * Sets the date property. <p>
     * 
     * Does nothing if the ui vetos the new date - as might happen if
     * the code tries to set a date which is unselectable in the 
     * monthView's context. The actual value of the new Date might
     * be changed by the ui, the default implementation cleans
     * the Date by zeroing all time components. <p>
     * 
     * At all "stable" (= not editing in date input field nor 
     * in the monthView) times the date is the same in the 
     * JXMonthView, this JXDatePicker and the editor. If a new Date
     * is set, this invariant is enforce by the DatePickerUI.<p>
     * 
     * A not null default value is set on instantiation.
     * 
     * This is a bound property. 
     * 
     *  
     * @param date the new date to set.
     * @see #getDate()
     */
    public void setDate(Date date) {
        /*
         * JW: 
         * this is a poor woman's constraint property.
         * Introduces explicit coupling to the ui. 
         * Which is unusual at this place in code.
         * 
         * If needed the date can be made a formal
         * constraint property and let the ui add a
         * VetoablePropertyListener.
         */
        try {
            date = getUI().getSelectableDate(date);
        } catch (PropertyVetoException e) {
            return;
        }
        Date old = getDate();
        long oldMillis = getDateInMillis();
        this.date = date;
        firePropertyChange("date", old, getDate());
        // JW: remove when removing the deprecated method
        firePropertyChange("dateInMillis", oldMillis, getDateInMillis());
    }

 

    /**
     * Returns the currently selected date.
     *
     * @return Date
     */
    public Date getDate() {
        return date; 
    }
    
    /**
     * 
     */
    private void init() {
        listenerMap = new EventListenerMap();
        initMonthView();

        updateLinkFormat();
        linkDate = _monthView.getToday();
//        _linkDate = System.currentTimeMillis();
        _linkPanel = new TodayPanel();
    }

    private void initMonthView() {
        _monthView = new JXMonthView();
//        _monthView.setSelectionModel(new SingleDaySelectionModel());
        _monthView.setTraversable(true);
        _monthView.addPropertyChangeListener(getMonthViewListener());
    }

    /**
     * Lazily creates and returns the PropertyChangeListener which listens
     * for model's calendar properties.
     * 
     * @return a PropertyChangeListener for monthView's property change notification.
     */
    private PropertyChangeListener getMonthViewListener() {
        if (monthViewListener == null) {
            monthViewListener = new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {
                    if ("timeZone".equals(evt.getPropertyName())) {
                        updateTimeZone((TimeZone) evt.getOldValue(), (TimeZone) evt.getNewValue());
                    }
                    
                }
                
            };
        }
        return monthViewListener;
    }

    /**
     * Callback from monthView timezone changes. <p>
     * 
     * NOTE: as timeZone is a bound property of this class we need to 
     * guarantee the propertyChangeNotification. As this class doesn't 
     * own this property it must listen to the owner (monthView) and 
     * re-fire the change.
     * 
     * @param oldValue the old timezone.
     * @param newValue the new timezone.
     */
    protected void updateTimeZone(TimeZone oldValue, TimeZone newValue) {
        firePropertyChange("timeZone", oldValue, newValue);
    }

    /**
     * Returns the look and feel (L&F) object that renders this component.
     *
     * @return the DatePickerUI object that renders this component
     */
    public DatePickerUI getUI() {
        return (DatePickerUI) ui;
    }

    /**
     * Sets the L&F object that renders this component.
     *
     * @param ui UI to use for this {@code JXDatePicker}
     */
    public void setUI(DatePickerUI ui) {
        super.setUI(ui);
    }

    /**
     * Resets the UI property with the value from the current look and feel.
     *
     * @see UIManager#getUI
     */
    @Override
    public void updateUI() {
        setUI((DatePickerUI) LookAndFeelAddons.getUI(this, DatePickerUI.class));
        // JW: quick hack around #706-swingx - monthView not updated
        // not sure if this here is the correct place nor 
        // the correct method - SwingUtilities.updateComponentTree might be better
        getMonthView().updateUI();
        invalidate();
    }

    /**
     * @inheritDoc
     */
    @Override
    public String getUIClassID() {
        return uiClassID;
    }

    /**
     * Replaces the currently installed formatter and factory used by the
     * editor. These string formats are defined by the
     * <code>java.text.SimpleDateFormat</code> class. <p>
     * 
     * Note: The given formats are internally synched to the picker's current
     *    TimeZone. 
     * 
     * @param formats zero or more not null string formats to use. Note that a 
     *    null array is allowed and resets the formatter to use the 
     *    localized default formats.
     * @throws NullPointerException any array element is null.
     * @see java.text.SimpleDateFormat
     */
    public void setFormats(String... formats) {
        DateFormat[] dateFormats = null;
        if (formats !=  null) {
            Contract.asNotNull(formats,
                    "the array of format strings must not "
                            + "must not contain null elements");
            dateFormats = new DateFormat[formats.length];
            for (int counter = formats.length - 1; counter >= 0; counter--) {
                dateFormats[counter] = new SimpleDateFormat(formats[counter], getLocale());
            }
        }
        setFormats(dateFormats);
    }

    /**
     * Replaces the currently installed formatter and factory used by the
     * editor.<p>
     *
     * Note: The given formats are internally synched to the picker's current
     *    TimeZone. 
     * 
     * @param formats zero or more not null formats to use. Note that a 
     *    null array is allowed and resets the formatter to use the 
     *    localized default formats.
     * @throws NullPointerException any of its elements is null.
     */
    public void setFormats(DateFormat... formats) {
        if (formats != null) {
            Contract.asNotNull(formats, "the array of formats " + "must not contain null elements");
        }
        
        DateFormat[] old = getFormats();
        _dateField.setFormatterFactory(new DefaultFormatterFactory(
                new DatePickerFormatter(formats, getLocale())));
        firePropertyChange("formats", old, getFormats());
    }

    /**
     * Returns an array of the formats used by the installed formatter
     * if it is a subclass of <code>JXDatePickerFormatter<code>.
     * <code>javax.swing.JFormattedTextField.AbstractFormatter</code>
     * and <code>javax.swing.text.DefaultFormatter</code> do not have
     * support for accessing the formats used.
     *
     * @return array of formats guaranteed to be not null, but might be empty.
     */
    public DateFormat[] getFormats() {
        // Dig this out from the factory, if possible, otherwise return null.
        AbstractFormatterFactory factory = _dateField.getFormatterFactory();
        if (factory != null) {
            AbstractFormatter formatter = factory.getFormatter(_dateField);
            if (formatter instanceof DatePickerFormatter) {
                return ((DatePickerFormatter) formatter).getFormats();
            }
        }
        return EMPTY_DATE_FORMATS;
    }

    /**
     * Return the <code>JXMonthView</code> used in the popup to
     * select dates from.
     *
     * @return the month view component
     */
    public JXMonthView getMonthView() {
        return _monthView;
    }

    /**
     * Set the component to use the specified JXMonthView.  If the new JXMonthView
     * is configured to a different time zone it will affect the time zone of this
     * component.
     *
     * @param monthView month view comopnent.
     * @throws NullPointerException if view component is null
     * 
     * @see #setTimeZone
     * @see #getTimeZone
     */
    public void setMonthView(JXMonthView monthView) {
        Contract.asNotNull(monthView, "monthView must not be null");
        JXMonthView oldMonthView = getMonthView();
        TimeZone oldTZ = getTimeZone();
        oldMonthView.removePropertyChangeListener(getMonthViewListener());
        _monthView = monthView;
        getMonthView().addPropertyChangeListener(getMonthViewListener());
        firePropertyChange(MONTH_VIEW, oldMonthView, getMonthView());
        firePropertyChange("timeZone", oldTZ, getTimeZone());
    }

    /**
     * Gets the time zone.  This is a convenience method which returns the time zone
     * of the JXMonthView being used.
     *
     * @return The <code>TimeZone</code> used by the <code>JXMonthView</code>.
     */
    public TimeZone getTimeZone() {
        return _monthView.getTimeZone();
    }

    /**
     * Sets the time zone with the given time zone value.    This is a convenience
     * method which returns the time zone of the JXMonthView being used.<p>
     * 
     * PENDING JW: currently this property is the only property of the monthView 
     * which is exposed in this api. Not sure why it is here at all.
     * It's asymetric (to the other properties) and as such should be either removed
     * or the others which might be relevant to a datePicker exposed as well (probably 
     * hiding the monthView itself as an implementation detail of the ui delegate). 
     *
     * @param tz The <code>TimeZone</code>.
     */
    public void setTimeZone(TimeZone tz) {
        _monthView.setTimeZone(tz);
    }


    /**
     * Returns the date shown in the LinkPanel.
     * <p>
     * PENDING JW: the property should be named linkDate - but that's held by the 
     * deprecated long returning method. Maybe revisit if we actually remove the other.
     * 
     * @return the date shown in the LinkPanel.
     */
    public Date getLinkDay() {
        return linkDate;
    }

    /**
     * Set the date the link will use and the string defining a MessageFormat
     * to format the link.  If no valid date is in the editor when the popup
     * is displayed the popup will focus on the month the linkDate is in.  Calling
     * this method will replace the currently installed linkPanel and install
     * a new one with the requested date and format.
     * 
     * 
     * @param linkDay         Date in milliseconds
     * @param linkFormatString String used to format the link
     * @see java.text.MessageFormat
     */
    public void setLinkDay(Date linkDay, String linkFormatString) {
        setLinkFormat(new MessageFormat(linkFormatString));
        setLinkDay(linkDay);
    }
    
    /**
     * Sets the date shown in the TodayPanel. 
     * 
     * PENDING JW ... quick api hack for testing. Don't recreate the panel if
     * it had been used 
     * 
     * @param linkDay the date used in the TodayPanel
     */
    public void setLinkDay(Date linkDay) {
        this.linkDate = linkDay;
        Format[] formats = getLinkFormat().getFormatsByArgumentIndex();
        for (Format format : formats) {
            if (format instanceof DateFormat) {
                ((DateFormat) format).setTimeZone(getTimeZone());
            }
        }
        setLinkPanel(new TodayPanel());
    }
    

    /**
     * @param _linkFormat the _linkFormat to set
     */
    protected void setLinkFormat(MessageFormat _linkFormat) {
        this._linkFormat = _linkFormat;
    }

    /**
     * @return the _linkFormat
     */
    protected MessageFormat getLinkFormat() {
        return _linkFormat;
    }

    /**
     * Update text on the link panel.
     * 
     */
    private void updateLinkFormat() {
        // PENDING JW: move to ui
        String linkFormat = UIManagerExt.getString(
                "JXDatePicker.linkFormat", getLocale());
        
        if (linkFormat != null) {
            setLinkFormat(new MessageFormat(linkFormat));
        } else {
            setLinkFormat(new MessageFormat("{0,date, dd MMMM yyyy}"));
        }
    }

    /**
     * Return the panel that is used at the bottom of the popup.  The default
     * implementation shows a link that displays the current month.
     *
     * @return The currently installed link panel
     */
    public JPanel getLinkPanel() {
        return _linkPanel;
    }

    /**
     * Set the panel that will be used at the bottom of the popup.
     * PENDING JW: why insist on JPanel? JComponent would be enough?
     *  
     * @param linkPanel The new panel to install in the popup
     */
    public void setLinkPanel(JPanel linkPanel) {
        JPanel oldLinkPanel = _linkPanel;
        _linkPanel = linkPanel;
        firePropertyChange(LINK_PANEL, oldLinkPanel, _linkPanel);
    }

    /**
     * Returns the formatted text field used to edit the date selection.
     * <p>
     * Clients should NOT use this method. It is provided to temporarily support
     * the PLAF code.
     * 
     * @return the formatted text field
     */
//    @Deprecated
    public JFormattedTextField getEditor() {
        return _dateField;
    }

    /**
     * Sets the editor. <p>
     * 
     * The default is created and set by the UI delegate.
     * <p>
     * Clients should NOT use this method. It is provided to temporarily support
     * the PLAF code.
     * 
     * @param editor the formatted input.
     * @throws NullPointerException if editor is null.
     * 
     * @see #getEditor
     */
//    @Deprecated
    public void setEditor(JFormattedTextField editor) {
        Contract.asNotNull(editor, "editor must not be null");
        JFormattedTextField oldEditor = _dateField;
        _dateField = editor;
        firePropertyChange(EDITOR, oldEditor, _dateField);
    }

    @Override
    public void setComponentOrientation(ComponentOrientation orientation) {
        super.setComponentOrientation(orientation);
        _monthView.setComponentOrientation(orientation);
    }

    /**
     * Returns true if the current value being edited is valid.
     *
     * @return true if the current value being edited is valid.
     */
    public boolean isEditValid() {
        return _dateField.isEditValid();
    }

    /**
     * Commits the editor's changes and notifies ActionListeners.
     * 
     * Forces the current value to be taken from the AbstractFormatter and
     * set as the current value. This has no effect if there is no current
     * AbstractFormatter installed.
     *
     * @throws java.text.ParseException Throws parse exception if the date
     *                                  can not be parsed.
     */
    public void commitEdit() throws ParseException {
        try {
            _dateField.commitEdit();
            fireActionPerformed(COMMIT_KEY);
        } catch (ParseException e) {
            // re-throw
            throw e;
        } 
    }

    /**
     * Cancels the editor's changes and notifies ActionListeners.
     * 
     */
    public void cancelEdit() {
        // hmmm... no direct api?
         _dateField.setValue(_dateField.getValue());
         fireActionPerformed(CANCEL_KEY);
    }

    /**
     * Sets the editable property. If false, ...?
     * 
     * The default value is true.
     * 
     * @param value
     * @see #isEditable()
     */
    public void setEditable(boolean value) {
        boolean oldEditable = isEditable();
        editable = value;
        firePropertyChange("editable", oldEditable, editable);
        if (editable != oldEditable) {
            repaint();
        }
    }

    /**
     * Returns the editable property. 
     * 
     * @return {@code true} if the picker is editable; {@code false} otherwise
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Returns the font that is associated with the editor of this date picker.
     */
    @Override
    public Font getFont() {
        return getEditor().getFont();
    }

    /**
     * Set the font for the editor associated with this date picker.
     */
    @Override
    public void setFont(final Font font) {
        getEditor().setFont(font);
    }

    /**
     * Sets the <code>lightWeightPopupEnabled</code> property, which
     * provides a hint as to whether or not a lightweight
     * <code>Component</code> should be used to contain the
     * <code>JXDatePicker</code>, versus a heavyweight
     * <code>Component</code> such as a <code>Panel</code>
     * or a <code>Window</code>.  The decision of lightweight
     * versus heavyweight is ultimately up to the
     * <code>JXDatePicker</code>.  Lightweight windows are more
     * efficient than heavyweight windows, but lightweight
     * and heavyweight components do not mix well in a GUI.
     * If your application mixes lightweight and heavyweight
     * components, you should disable lightweight popups.
     * The default value for the <code>lightWeightPopupEnabled</code>
     * property is <code>true</code>, unless otherwise specified
     * by the look and feel.  Some look and feels always use
     * heavyweight popups, no matter what the value of this property.
     * <p/>
     * See the article <a href="http://java.sun.com/products/jfc/tsc/articles/mixing/index.html">Mixing Heavy and Light Components</a>
     * on <a href="http://java.sun.com/products/jfc/tsc">
     * <em>The Swing Connection</em></a>
     * This method fires a property changed event.
     *
     * @param aFlag if <code>true</code>, lightweight popups are desired
     * @beaninfo bound: true
     * expert: true
     * description: Set to <code>false</code> to require heavyweight popups.
     */
    public void setLightWeightPopupEnabled(boolean aFlag) {
        boolean oldFlag = lightWeightPopupEnabled;
        lightWeightPopupEnabled = aFlag;
        firePropertyChange("lightWeightPopupEnabled", oldFlag, lightWeightPopupEnabled);
    }

    /**
     * Gets the value of the <code>lightWeightPopupEnabled</code>
     * property.
     *
     * @return the value of the <code>lightWeightPopupEnabled</code>
     *         property
     * @see #setLightWeightPopupEnabled
     */
    public boolean isLightWeightPopupEnabled() {
        return lightWeightPopupEnabled;
    }

    /**
     * Get the baseline for the specified component, or a value less
     * than 0 if the baseline can not be determined.  The baseline is measured
     * from the top of the component.
     *
     * @param width  Width of the component to determine baseline for.
     * @param height Height of the component to determine baseline for.
     * @return baseline for the specified component
     */
    public int getBaseline(int width, int height) {
        return ((DatePickerUI) ui).getBaseline(width, height);
    }

    /**
     * Returns the string currently used to identiy fired ActionEvents.
     *
     * @return String The string used for identifying ActionEvents.
     * 
     * @deprecated no longer used. The command is internally determined and
     * either datePickerCommit or datePickerCancel, depending on the user
     * gesture which triggered the action.
     */
    @Deprecated
    public String getActionCommand() {
        return _actionCommand;
    }

    /**
     * Sets the string used to identify fired ActionEvents.
     *
     * @param actionCommand The string used for identifying ActionEvents.
     * 
     * @deprecated no longer used. The command is internally determined and
     * either datePickerCommit or datePickerCancel, depending on the user
     * gesture which triggered the action.
     */
    @Deprecated
    public void setActionCommand(String actionCommand) {
        _actionCommand = actionCommand;
    }

    /**
     * Fires an ActionEvent with this picker's actionCommand
     * to all listeners.
     * 
     * @deprecated no longer used. The command is internally determined and
     * either datePickerCommit or datePickerCancel, depending on the user
     * gesture which triggered the action.
     */
    @Deprecated
    protected void fireActionPerformed() {
        fireActionPerformed(getActionCommand());
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
     * Fires an ActionEvent with the given actionCommand
     * to all listeners.
     */
    protected void fireActionPerformed(String actionCommand) {
        ActionListener[] listeners = getListeners(ActionListener.class);
        ActionEvent e = null;

        for (ActionListener listener : listeners) {
            if (e == null) {
                e = new ActionEvent(this,
                        ActionEvent.ACTION_PERFORMED,
                        actionCommand);
            }
            listener.actionPerformed(e);
        }
    }
    /*
    public void setLocale(Locale l) {
        super.setLocale(l);
        _monthView.setLocale(l);
    }*/
    
 
    /**
     * Pes: added setLocale method to refresh link text on locale changes
     */
    private final class TodayPanel extends JXPanel {
        private TodayAction todayAction;
        private JXHyperlink todayLink;

        TodayPanel() {
            super(new FlowLayout());
            setBackgroundPainter(new MattePainter(new GradientPaint(0, 0, new Color(238, 238, 238), 0, 1, Color.WHITE)));
            todayAction = new TodayAction();
            todayLink = new JXHyperlink(todayAction);
            todayLink.addMouseListener(createDoubleClickListener());
            Color textColor = new Color(16, 66, 104);
            todayLink.setUnclickedColor(textColor);
            todayLink.setClickedColor(textColor);
            add(todayLink);
        }

        /**
         * @return
         */
        private MouseListener createDoubleClickListener() {
            MouseAdapter adapter = new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getClickCount() != 2) return;
                    todayAction.select = true;
                }
                
            };
            return adapter;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(187, 187, 187));
            g.drawLine(0, 0, getWidth(), 0);
            g.setColor(new Color(221, 221, 221));
            g.drawLine(0, 1, getWidth(), 1);
        }

        @Override
        public void setLocale(Locale l) {
            super.setLocale(l);
            /* FIXME: PeS: Uncomment this after updateLinkFormat works correctly
             * 
             * and then delete following line
             */
            updateLinkFormat();
            todayLink.setText(getLinkFormat().format(new Object[]{getLinkDay()}));
//            todayLink.setText(DateFormat.getDateInstance(DateFormat.MEDIUM, l).format(new Date(_linkDate)));
        }
        
        private final class TodayAction extends AbstractAction {
            boolean select;
            TodayAction() {
                super(getLinkFormat().format(new Object[]{getLinkDay()}));
                Calendar cal = _monthView.getCalendar();
                cal.setTime(getLinkDay());
                putValue(NAME, getLinkFormat().format(new Object[] {cal.getTime()}));
            }

            public void actionPerformed(ActionEvent ae) {
                String key = select ? JXDatePicker.HOME_COMMIT_KEY : JXDatePicker.HOME_NAVIGATE_KEY;
                select = false;
                Action delegate = getActionMap().get(key);
                if (delegate !=  null) {
                    delegate.actionPerformed(null);
                }
                //                Date span = new DateSpan(_linkDate, _linkDate).getStartAsDate();
//                if (select) {
//                    _monthView.setSelectedDate(span);
//                    _monthView.commitSelection();
//                    select = false;
//                } else {
//                    _monthView.ensureDateVisible(_linkDate);
//                }
                
            }
        }
    }

//----------------------- deprecated
    
    /**
     * Set the currently selected date.
     *
     * @param millis milliseconds
     * 
     * @deprecated use {@link #setDate(Date)}
     */
    @Deprecated
    public void setDateInMillis(long millis) {
        setDate(new Date(millis));
    }

    /**
     * Returns the currently selected date in milliseconds.
     *
     * @return the date in milliseconds, -1 if there is no selection.
     * 
     * @deprecated use {@link #getDate()}
     */
    @Deprecated
    public long getDateInMillis() {
        long result = -1;
        Date selection = getDate();
        if (selection != null) {
            result = selection.getTime();
        }
        return result;
    }

    /**
     * Returns the date shown in the LinkPanel as millis.
     * 
     * @return the date shown in the TodayPanel in millis
     * 
     * @deprecated use {@link #getLinkDay()}
     */
    @Deprecated
    public long getLinkDate() {
        return getLinkDay().getTime();
    }

    /**
     * Set the date the link will use and the string defining a MessageFormat to
     * format the link. If no valid date is in the editor when the popup is
     * displayed the popup will focus on the month the linkDate is in. Calling
     * this method will replace the currently installed linkPanel and install a
     * new one with the requested date and format.
     * 
     * 
     * @param linkDate Date in milliseconds
     * @param linkFormatString String used to format the link
     * @see java.text.MessageFormat
     * 
     * @deprecated use {@link #setLinkDay(Date, String)}
     */
    @Deprecated
    public void setLinkDate(long linkDate, String linkFormatString) {
        setLinkDay(new Date(linkDate), linkFormatString);
    }
    
    /**
     * Sets the date used in the TodayPanel.<p>
     * 
     * PENDING JW ... quick api hack for testing.
     * 
     * @param linkDate the date in millis to use in the TodayPanel.
     * 
     * @deprecated use {@link #setLinkDay(Date)}
     */
    @Deprecated
    public void setLinkDate(long linkDate) {
        setLinkDay(new Date(linkDate));
    }
    /**
     * Returns the date shown in the LinkPanel.
     * 
     * @return the date shown in the LinkPanel.
     * 
     * @deprecated use {@link #getLinkDay()}
     */
    @Deprecated
    public Date getToday() {
        return getLinkDay();
    }
    
    /**
     * PENDING JW ... quick api hack for testing.
     * 
     * @param linkDate
     * 
     * @deprecated use {@link #setLinkDay(Date)}
     */
    @Deprecated
    public void setToday(Date linkDate) {
        setLinkDay(linkDate);
    }

    /**
     * Create a new date picker using the specified time as the initial
     * selection and the default abstract formatter
     * <code>JXDatePickerFormatter</code>.
     * <p/>
     * The date picker is configured with the default time zone and specified locale
     *
     * @param millis initial time in milliseconds
     * @param locale initial Locale
     * @see #setTimeZone
     * @see #getTimeZone
     * 
     * @deprecated use {@link #JXDatePicker(Date, Locale)}
     */
    @Deprecated
    public JXDatePicker(long millis, Locale locale) {
        this(new Date(millis), locale);
//        init();
//        if (locale != null) {
//            setLocale(locale);
//        }
//        // install the controller before setting the date
//        updateUI();
//        setDate(new Date(millis));
    }
    
    /**
     * Create a new date picker using the specified time as the initial
     * selection and the default abstract formatter
     * <code>JXDatePickerFormatter</code>.
     * <p/>
     * The date picker is configured with the default time zone and locale
     *
     * @param millis initial time in milliseconds
     * @see #setTimeZone
     * @see #getTimeZone
     * 
     * @deprecated use {@link #JXDatePicker(Date)}
     */
    @Deprecated
    public JXDatePicker(long millis) {
        this(millis, null);
    }

}    
 