package org.jdownloader.extensions.schedulerV2.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DateEditor;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;

import org.appwork.storage.JSonStorage;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.uio.CloseReason;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.extensions.schedulerV2.actions.AbstractScheduleAction;
import org.jdownloader.extensions.schedulerV2.actions.IScheduleActionConfig;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper.TIME_OPTIONS;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper.WEEKDAY;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntry;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntryStorable;
import org.jdownloader.extensions.schedulerV2.translate.T;
import org.jdownloader.gui.translate._GUI;

public class AddScheduleEntryDialog extends AbstractDialog<ScheduleEntry> {
    private JPanel                                                  content;
    private ExtTextField                                            scheduleName;
    private LinkedList<JComponent>                                  timeOptionPaneOnlyOnce;
    private JSpinner                                                timeSpinnerOnce;
    private JSpinner                                                dateSpinnerOnce;
    private JSpinner                                                minuteSpinnerHourly;
    private LinkedList<JComponent>                                  timeOptionPaneHourly;
    private JSpinner                                                timeSpinnerDaily;
    private JSpinner                                                timeSpinnerWeekly;
    private JSpinner                                                hourSpinnerInterval;
    private LinkedList<JComponent>                                  timeOptionPaneInterval;
    private JSpinner                                                minuteSpinnerInterval;
    private ComboBox<TIME_OPTIONS>                                  intervalBox;
    private ComboBox<AbstractScheduleAction<IScheduleActionConfig>> actionBox;
    private ScheduleEntry                                           editEntry  = null;
    private LinkedList<JComponent>                                  timeOptionPaneSpecificDays;
    private JCheckBox                                               specificDaysMon;
    private JCheckBox                                               specificDaysTue;
    private JCheckBox                                               specificDaysWed;
    private JCheckBox                                               specificDaysThu;
    private JCheckBox                                               specificDaysFri;
    private JCheckBox                                               specificDaysSat;
    private JCheckBox                                               specificDaysSun;
    private JSpinner                                                timeSpinnerSpecificDays;
    private final JLabel                                            emptyLabel = new JLabel() {
        {
            setOpaque(false);
        }
    };

    public AddScheduleEntryDialog() {
        super(UserIO.NO_ICON, T.T.addScheduleEntryDialog_title(), null, _GUI.T.lit_save(), null);
    }

    public AddScheduleEntryDialog(ScheduleEntry entry) {
        super(UserIO.NO_ICON, T.T.addScheduleEntryDialog_title_edit(), null, _GUI.T.lit_save(), null);
        this.editEntry = entry;
    }

    @Override
    protected ScheduleEntry createReturnValue() {
        if (!getCloseReason().equals(CloseReason.OK)) {
            return null;
        }
        ScheduleEntryStorable actionStorable = new ScheduleEntryStorable();
        actionStorable.setEnabled(true);
        actionStorable.setName(scheduleName.getText());
        AbstractScheduleAction<IScheduleActionConfig> action = actionBox.getSelectedItem();
        actionStorable.setActionID(action.getActionID());
        actionStorable.setActionConfig(JSonStorage.toString(action.getConfig()));
        TIME_OPTIONS timeType = intervalBox.getSelectedItem();
        switch (timeType) {
        case HOURLY: {
            Date d = (Date) minuteSpinnerHourly.getValue();
            Calendar timestamp = Calendar.getInstance();
            timestamp.set(Calendar.SECOND, 0);
            timestamp.set(Calendar.MINUTE, d.getMinutes());
            actionStorable.setTimestamp(timestamp.getTimeInMillis() / 1000l);
        }
        break;
        case SPECIFICDAYS: {
            Date d = (Date) timeSpinnerSpecificDays.getValue();
            Calendar timestamp = Calendar.getInstance();
            timestamp.set(Calendar.SECOND, 0);
            timestamp.set(Calendar.MINUTE, d.getMinutes());
            timestamp.set(Calendar.HOUR_OF_DAY, d.getHours());
            actionStorable.setTimestamp(timestamp.getTimeInMillis() / 1000l);
            LinkedList<WEEKDAY> days = new LinkedList<ActionHelper.WEEKDAY>();
            if (specificDaysMon.isSelected()) {
                days.add(WEEKDAY.MONDAY);
            }
            if (specificDaysTue.isSelected()) {
                days.add(WEEKDAY.TUESDAY);
            }
            if (specificDaysWed.isSelected()) {
                days.add(WEEKDAY.WEDNESDAY);
            }
            if (specificDaysThu.isSelected()) {
                days.add(WEEKDAY.THURSDAY);
            }
            if (specificDaysFri.isSelected()) {
                days.add(WEEKDAY.FRIDAY);
            }
            if (specificDaysSat.isSelected()) {
                days.add(WEEKDAY.SATURDAY);
            }
            if (specificDaysSun.isSelected()) {
                days.add(WEEKDAY.SUNDAY);
            }
            actionStorable._setSelectedDays(days);
        }
        break;
        case CHOOSEINTERVAL: {
            actionStorable.setTimestamp(Calendar.getInstance().getTimeInMillis() / 1000l);
            actionStorable.setIntervalHour((Integer) hourSpinnerInterval.getValue());
            actionStorable.setIntervalMin((Integer) minuteSpinnerInterval.getValue());
        }
        break;
        case ONLYONCE:
        default: {
            Date time = (Date) timeSpinnerOnce.getValue();
            Date date = (Date) dateSpinnerOnce.getValue();
            Calendar timestamp = Calendar.getInstance();
            timestamp.set(date.getYear() + 1900, date.getMonth(), date.getDate(), time.getHours(), time.getMinutes());
            timestamp.set(Calendar.SECOND, 0);
            actionStorable.setTimestamp(timestamp.getTimeInMillis() / 1000l);
        }
        break;
        }
        actionStorable._setTimeType(timeType);
        try {
            return new ScheduleEntry(actionStorable);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel migPanel = new MigPanel("ins 0 0 5 0, wrap 2", "[95::][grow,fill]", "[sg name][sg header][sg repeat][sg parameter][sg parameter][sg header][sg action][sg parameter2, 26]");
        migPanel.setOpaque(false);
        migPanel.add(new JLabel(T.T.scheduleTable_column_name() + ":"));
        scheduleName = new ExtTextField();
        scheduleName.setText(editEntry != null ? editEntry.getName() : T.T.addScheduleEntryDialog_defaultScheduleName());
        migPanel.add(scheduleName, "");
        migPanel.add(header(T.T.addScheduleEntryDialog_header_time()), "spanx, growx,pushx,newline 15");
        migPanel.add(new JLabel(T.T.addScheduleEntryDialog_repeat() + ":"), "gapleft 10");
        intervalBox = new ComboBox<TIME_OPTIONS>(ActionHelper.TIME_OPTIONS.values()) {
            @Override
            protected String getLabel(int index, TIME_OPTIONS value) {
                return value.getReadableName();
            }
        };
        intervalBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ComboBox cb = (ComboBox) e.getSource();
                Object selected = cb.getSelectedItem();
                selectTimeOptionPane((TIME_OPTIONS) selected);
            }
        });
        if (editEntry != null) {
            intervalBox.setSelectedItem(editEntry.getTimeType());
        } else {
            intervalBox.setSelectedIndex(0);
        }
        migPanel.add(intervalBox, "");
        // Begin time subpanels
        setupTimeOptionPanes(migPanel);
        selectTimeOptionPane(editEntry != null ? editEntry.getTimeType() : ActionHelper.TIME_OPTIONS.ONLYONCE);
        // Begin action area
        migPanel.add(header(T.T.addScheduleEntryDialog_actionParameters()), "spanx, growx,newline 15");
        migPanel.add(new JLabel(T.T.scheduleTable_column_action() + ":"), "gapleft 10");
        final AbstractScheduleAction<IScheduleActionConfig>[] actionArray = (AbstractScheduleAction<IScheduleActionConfig>[]) Array.newInstance(AbstractScheduleAction.class, ActionHelper.ACTIONS.size());
        AbstractScheduleAction<IScheduleActionConfig> selectedItem = ActionHelper.ACTIONS.get(0);
        for (int i = 0; i < ActionHelper.ACTIONS.size(); i++) {
            if (editEntry != null && editEntry.getAction() != null && editEntry.getAction().getActionID().equals(ActionHelper.ACTIONS.get(i).getActionID())) {
                selectedItem = editEntry.getAction();
                actionArray[i] = selectedItem;
            } else {
                actionArray[i] = ActionHelper.ACTIONS.get(i);
            }
        }
        actionBox = new ComboBox<AbstractScheduleAction<IScheduleActionConfig>>(actionArray) {
            @Override
            protected String getLabel(int index, AbstractScheduleAction<IScheduleActionConfig> value) {
                return value.getReadableName();
            }
        };
        actionBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AbstractScheduleAction<IScheduleActionConfig> selected = actionBox.getSelectedItem();
                for (int i = 0; i < actionArray.length; i++) {
                    actionArray[i].setVisible(actionArray[i].equals(selected));
                }
            }
        });
        migPanel.add(actionBox);
        for (int i = 0; i < actionArray.length; i++) {
            actionArray[i].drawOnPanel(migPanel);
        }
        actionBox.setSelectedItem(selectedItem);
        content = migPanel;
        updatePanel();
        loadEntry(editEntry);
        return content;
    }

    private void loadEntry(ScheduleEntry editEntry) {
        if (editEntry == null) {
            scheduleName.setText(T.T.addScheduleEntryDialog_defaultScheduleName());
        } else {
            scheduleName.setText(editEntry.getName());
        }
    }

    private void setupTimeOptionPanes(MigPanel panel) {
        HashMap<JComponent, String> constraints = new HashMap<JComponent, String>();
        /* Only Once */
        timeOptionPaneOnlyOnce = new LinkedList<JComponent>();
        dateSpinnerOnce = new JSpinner(new SpinnerDateModel());
        dateSpinnerOnce.setEditor(new DateEditor(dateSpinnerOnce, ((SimpleDateFormat) SimpleDateFormat.getDateInstance()).toPattern()));
        dateSpinnerOnce.setValue(editEntry != null && editEntry.getTimeType().equals(TIME_OPTIONS.ONLYONCE) ? new Date(editEntry.getTimestamp() * 1000l) : new Date());
        timeSpinnerOnce = new JSpinner(new SpinnerDateModel());
        timeSpinnerOnce.setEditor(new DateEditor(timeSpinnerOnce, ((SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.SHORT)).toPattern().replaceAll("h", "H").replaceAll("a", "")));
        timeSpinnerOnce.setValue(editEntry != null && editEntry.getTimeType().equals(TIME_OPTIONS.ONLYONCE) ? new Date(editEntry.getTimestamp() * 1000l) : new Date());
        JLabel dateOnlyOnceLabel;
        timeOptionPaneOnlyOnce.add(dateOnlyOnceLabel = new JLabel(T.T.addScheduleEntryDialog_date() + ":"));
        constraints.put(dateOnlyOnceLabel, "gapleft 10,");
        timeOptionPaneOnlyOnce.add(dateSpinnerOnce);
        JLabel timeOnlyOnceLabel;
        timeOptionPaneOnlyOnce.add(timeOnlyOnceLabel = new JLabel(T.T.addScheduleEntryDialog_time() + ":"));
        constraints.put(timeOnlyOnceLabel, "gapleft 10,");
        timeOptionPaneOnlyOnce.add(timeSpinnerOnce);
        for (JComponent component : timeOptionPaneOnlyOnce) {
            panel.add(component, getOrDefault(constraints, component, "") + "hidemode 3");
            component.setVisible(false);
        }
        /* Hourly */
        timeOptionPaneHourly = new LinkedList<JComponent>();
        minuteSpinnerHourly = new JSpinner(new SpinnerDateModel());
        minuteSpinnerHourly.setEditor(new DateEditor(minuteSpinnerHourly, "mm"));
        minuteSpinnerHourly.setValue(editEntry != null && editEntry.getTimeType().equals(TIME_OPTIONS.HOURLY) ? new Date(editEntry.getTimestamp() * 1000l) : new Date());
        JLabel timeHourlyLabel;
        timeOptionPaneHourly.add(timeHourlyLabel = new JLabel(T.T.addScheduleEntryDialog_minute() + ":"));
        constraints.put(timeHourlyLabel, "gapleft 10,");
        timeOptionPaneHourly.add(minuteSpinnerHourly);
        for (JComponent component : timeOptionPaneHourly) {
            panel.add(component, getOrDefault(constraints, component, "") + "hidemode 3");
            component.setVisible(false);
        }
        timeOptionPaneHourly.add(emptyLabel);
        /* Specific Days */
        timeOptionPaneSpecificDays = new LinkedList<JComponent>();
        JLabel timeSpecificDaysLabel = new JLabel(T.T.addScheduleEntryDialog_time() + ":");
        timeOptionPaneSpecificDays.add(timeSpecificDaysLabel);
        constraints.put(timeSpecificDaysLabel, "gapleft 10,");
        timeSpinnerSpecificDays = new JSpinner(new SpinnerDateModel());
        timeSpinnerSpecificDays.setEditor(new DateEditor(timeSpinnerSpecificDays, ((SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.SHORT)).toPattern().replaceAll("h", "H").replaceAll("a", "")));
        timeSpinnerSpecificDays.setValue(editEntry != null && editEntry.getTimeType().equals(TIME_OPTIONS.SPECIFICDAYS) ? new Date(editEntry.getTimestamp() * 1000l) : new Date());
        timeOptionPaneSpecificDays.add(timeSpinnerSpecificDays);
        JLabel daySpecificDaysLabel = new JLabel(T.T.addScheduleEntryDialog_days() + ":");
        timeOptionPaneSpecificDays.add(daySpecificDaysLabel);
        constraints.put(daySpecificDaysLabel, "gapleft 10,");
        MigPanel specificDaysSubPanel = new MigPanel("ins 0, wrap 7", "[grow]", "");
        List<WEEKDAY> days = (editEntry == null) ? null : editEntry.getSelectedDays();
        specificDaysSubPanel.add(specificDaysMon = new JCheckBox(WEEKDAY.MONDAY.getReadableName(), days == null ? true : days.contains(WEEKDAY.MONDAY)));
        specificDaysSubPanel.add(specificDaysTue = new JCheckBox(WEEKDAY.TUESDAY.getReadableName(), days == null ? true : days.contains(WEEKDAY.TUESDAY)));
        specificDaysSubPanel.add(specificDaysWed = new JCheckBox(WEEKDAY.WEDNESDAY.getReadableName(), days == null ? true : days.contains(WEEKDAY.WEDNESDAY)));
        specificDaysSubPanel.add(specificDaysThu = new JCheckBox(WEEKDAY.THURSDAY.getReadableName(), days == null ? true : days.contains(WEEKDAY.THURSDAY)));
        specificDaysSubPanel.add(specificDaysFri = new JCheckBox(WEEKDAY.FRIDAY.getReadableName(), days == null ? true : days.contains(WEEKDAY.FRIDAY)));
        specificDaysSubPanel.add(specificDaysSat = new JCheckBox(WEEKDAY.SATURDAY.getReadableName(), days == null ? true : days.contains(WEEKDAY.SATURDAY)));
        specificDaysSubPanel.add(specificDaysSun = new JCheckBox(WEEKDAY.SUNDAY.getReadableName(), days == null ? true : days.contains(WEEKDAY.SUNDAY)));
        timeOptionPaneSpecificDays.add(specificDaysSubPanel);
        for (JComponent component : timeOptionPaneSpecificDays) {
            panel.add(component, getOrDefault(constraints, component, "") + "hidemode 3");
            component.setVisible(false);
        }
        /* Interval */
        timeOptionPaneInterval = new LinkedList<JComponent>();
        hourSpinnerInterval = new JSpinner(new SpinnerNumberModel(editEntry != null && editEntry.getTimeType().equals(TIME_OPTIONS.CHOOSEINTERVAL) ? editEntry.getIntervalHour() : 1, 0, 365 * 24, 1));
        JLabel hoursIntervalLabel;
        timeOptionPaneInterval.add(hoursIntervalLabel = new JLabel(T.T.addScheduleEntryDialog_hours() + ":"));
        constraints.put(hoursIntervalLabel, "gapleft 10,");
        timeOptionPaneInterval.add(hourSpinnerInterval);
        minuteSpinnerInterval = new JSpinner(new SpinnerNumberModel(editEntry != null && editEntry.getTimeType().equals(TIME_OPTIONS.CHOOSEINTERVAL) ? editEntry.getIntervalMinunte() : 0, 0, 59, 1));
        JLabel minutesIntervalLabel;
        timeOptionPaneInterval.add(minutesIntervalLabel = new JLabel(T.T.addScheduleEntryDialog_minutes() + ":"));
        constraints.put(minutesIntervalLabel, "gapleft 10,");
        timeOptionPaneInterval.add(minuteSpinnerInterval);
        for (JComponent component : timeOptionPaneInterval) {
            panel.add(component, getOrDefault(constraints, component, "") + "hidemode 3");
            component.setVisible(false);
        }
        panel.add(emptyLabel, "spanx, hidemode 3");
        emptyLabel.setVisible(false);
    }

    private String getOrDefault(HashMap<JComponent, String> constraints, JComponent component, String defaultValue) {
        final String ret = constraints.get(component);
        if (ret == null) {
            return defaultValue;
        } else {
            return ret;
        }
    }

    private void selectTimeOptionPane(TIME_OPTIONS interval) {
        if (timeOptionPaneOnlyOnce == null) {
            return;
        }
        for (JComponent component : timeOptionPaneHourly) {
            component.setVisible(interval.equals(TIME_OPTIONS.HOURLY));
        }
        for (JComponent component : timeOptionPaneInterval) {
            component.setVisible(interval.equals(TIME_OPTIONS.CHOOSEINTERVAL));
        }
        for (JComponent component : timeOptionPaneOnlyOnce) {
            component.setVisible(interval.equals(TIME_OPTIONS.ONLYONCE));
        }
        for (JComponent component : timeOptionPaneSpecificDays) {
            component.setVisible(interval.equals(TIME_OPTIONS.SPECIFICDAYS));
        }
        getDialog().pack();
    }

    @Override
    protected int getPreferredWidth() {
        if (content == null) {
            return 455;
        } else {
            return Math.max((int) content.getMinimumSize().getWidth() + 50, 455);
        }
    }

    protected void updatePanel() {
        if (content != null) {
            getDialog().pack();
        }
    }

    private JComponent header(String caption) {
        JLabel ret = SwingUtils.toBold(new JLabel(caption));
        ret.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ret.getForeground()));
        return ret;
    }

    @Override
    protected void packed() {
        super.packed();
    }
}