package org.jdownloader.extensions.schedulerV2.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import net.miginfocom.swing.MigLayout;

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
    private MigPanel                                                timePane;
    private LinkedList<JComponent>                                  timeOptionPaneOnlyOnce;
    private JSpinner                                                timeSpinnerOnce;
    private JSpinner                                                dateSpinnerOnce;
    private JSpinner                                                minuteSpinnerHourly;
    private LinkedList<JComponent>                                  timeOptionPaneHourly;
    private MigPanel                                                timeOptionPaneDaily;
    private JSpinner                                                timeSpinnerDaily;
    private MigPanel                                                timeOptionPaneWeekly;
    private JSpinner                                                timeSpinnerWeekly;
    private JSpinner                                                hourSpinnerInterval;
    private LinkedList<JComponent>                                  timeOptionPaneInterval;
    private JSpinner                                                minuteSpinnerInterval;
    private ComboBox<TIME_OPTIONS>                                  intervalBox;
    private ComboBox<AbstractScheduleAction<IScheduleActionConfig>> actionBox;
    private MigPanel                                                actionParameterPanel;

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
        super(UserIO.NO_ICON, T._.addScheduleEntryDialog_title(), null, _GUI._.lit_save(), null);
    }

    public AddScheduleEntryDialog(ScheduleEntry entry) {
        super(UserIO.NO_ICON, T._.addScheduleEntryDialog_title_edit(), null, _GUI._.lit_save(), null);
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
        case DAILY: {
            Date d = (Date) timeSpinnerDaily.getValue();
            Calendar timestamp = Calendar.getInstance();
            timestamp.set(Calendar.SECOND, 0);
            timestamp.set(Calendar.MINUTE, d.getMinutes());
            timestamp.set(Calendar.HOUR_OF_DAY, d.getHours());
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
        case WEEKLY: {
            Date d = (Date) timeSpinnerWeekly.getValue();
            Calendar timestamp = Calendar.getInstance();
            timestamp.set(Calendar.SECOND, 0);
            timestamp.set(Calendar.MINUTE, d.getMinutes());
            timestamp.set(Calendar.HOUR_OF_DAY, d.getHours());
            int dayOfWeek;

            timestamp.set(Calendar.DAY_OF_WEEK, d.getDay() + 1);
            actionStorable.setTimestamp(timestamp.getTimeInMillis() / 1000l);
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
        MigPanel migPanel = new MigPanel("ins 0 0 5 0, wrap 2", "[][grow,fill]", "[sg name][sg header][sg repeat][sg parameter][sg parameter][sg header][sg action][sg parameter2, 26]");
        migPanel.setOpaque(false);

        migPanel.add(new JLabel(T._.scheduleTable_column_name() + ":"));
        scheduleName = new ExtTextField();
        scheduleName.setText(editEntry != null ? editEntry.getName() : T._.addScheduleEntryDialog_defaultScheduleName());

        migPanel.add(scheduleName, "");

        migPanel.add(header(T._.addScheduleEntryDialog_header_time()), "spanx, growx,pushx,newline 15");
        migPanel.add(new JLabel(T._.addScheduleEntryDialog_repeat() + ":"), "gapleft 10");

        ArrayList<TIME_OPTIONS> values = new ArrayList<ActionHelper.TIME_OPTIONS>(Arrays.asList(ActionHelper.TIME_OPTIONS.values()));
        values.remove(ActionHelper.TIME_OPTIONS.DAILY);
        values.remove(ActionHelper.TIME_OPTIONS.WEEKLY);
        intervalBox = new ComboBox<TIME_OPTIONS>(values.toArray(new ActionHelper.TIME_OPTIONS[values.size()])) {

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
        migPanel.add(header(T._.addScheduleEntryDialog_actionParameters()), "spanx, growx,newline 15");
        migPanel.add(new JLabel(T._.scheduleTable_column_action() + ":"), "gapleft 10");

        AbstractScheduleAction<IScheduleActionConfig>[] array = (AbstractScheduleAction<IScheduleActionConfig>[]) Array.newInstance(AbstractScheduleAction.class, ActionHelper.ACTIONS.size());
        AbstractScheduleAction<IScheduleActionConfig> selectedItem = ActionHelper.ACTIONS.get(0);
        for (int i = 0; i < ActionHelper.ACTIONS.size(); i++) {
            if (editEntry != null && editEntry.getAction() != null && editEntry.getAction().getActionID().equals(ActionHelper.ACTIONS.get(i).getActionID())) {
                selectedItem = editEntry.getAction();
                array[i] = selectedItem;
            } else {
                array[i] = ActionHelper.ACTIONS.get(i);
            }

        }
        actionBox = new ComboBox<AbstractScheduleAction<IScheduleActionConfig>>(array) {
            @Override
            protected String getLabel(int index, AbstractScheduleAction<IScheduleActionConfig> value) {
                return value.getReadableName();
            }
        };
        actionBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                selectActionConfigPanel(actionBox.getSelectedItem().getConfigPanel());
            }
        });

        migPanel.add(actionBox);
        actionParameterPanel = new MigPanel("ins 0", "", "");
        migPanel.add(actionParameterPanel, "spanx, growx, pushx");

        actionBox.setSelectedItem(selectedItem);

        content = migPanel;
        updatePanel();
        loadEntry(editEntry);
        return content;
    }

    private void loadEntry(ScheduleEntry editEntry) {
        if (editEntry == null) {
            scheduleName.setText(T._.addScheduleEntryDialog_defaultScheduleName());

        } else {
            scheduleName.setText(editEntry.getName());
        }
    }

    private void selectActionConfigPanel(JPanel configPanel) {

        if (actionParameterPanel == null) {
            return;
        }
        actionParameterPanel.removeAll();
        actionParameterPanel.setLayout(new MigLayout("ins 0 10 0 0", "", ""));
        if (configPanel != null) {
            actionParameterPanel.add(configPanel, "growx,pushx");
        }
        actionParameterPanel.repaint();
        pack();
    }

    private void setupTimeOptionPanes(MigPanel panel) {
        HashMap<JComponent, String> constraints = new HashMap<JComponent, String>();

        /* Only Once */
        timeOptionPaneOnlyOnce = new LinkedList<JComponent>();
        dateSpinnerOnce = new JSpinner(new SpinnerDateModel());
        dateSpinnerOnce.setEditor(new DateEditor(dateSpinnerOnce, ((SimpleDateFormat) SimpleDateFormat.getDateInstance()).toPattern()));
        dateSpinnerOnce.setValue(editEntry != null && editEntry.getTimeType().equals(TIME_OPTIONS.ONLYONCE) ? new Date(editEntry.getTimestamp() * 1000l) : new Date());
        timeSpinnerOnce = new JSpinner(new SpinnerDateModel());
        timeSpinnerOnce.setEditor(new DateEditor(timeSpinnerOnce, ((SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.SHORT)).toPattern()));
        timeSpinnerOnce.setValue(editEntry != null && editEntry.getTimeType().equals(TIME_OPTIONS.ONLYONCE) ? new Date(editEntry.getTimestamp() * 1000l) : new Date());
        JLabel dateOnlyOnceLabel;
        timeOptionPaneOnlyOnce.add(dateOnlyOnceLabel = new JLabel(T._.addScheduleEntryDialog_date() + ":"));
        constraints.put(dateOnlyOnceLabel, "gapleft 10,");
        timeOptionPaneOnlyOnce.add(dateSpinnerOnce);
        JLabel timeOnlyOnceLabel;
        timeOptionPaneOnlyOnce.add(timeOnlyOnceLabel = new JLabel(T._.addScheduleEntryDialog_time() + ":"));
        constraints.put(timeOnlyOnceLabel, "gapleft 10,");
        timeOptionPaneOnlyOnce.add(timeSpinnerOnce);
        for (JComponent component : timeOptionPaneOnlyOnce) {
            panel.add(component, constraints.getOrDefault(component, "") + "hidemode 3");
            component.setVisible(false);
        }
        /* Hourly */
        timeOptionPaneHourly = new LinkedList<JComponent>();
        minuteSpinnerHourly = new JSpinner(new SpinnerDateModel());
        minuteSpinnerHourly.setEditor(new DateEditor(minuteSpinnerHourly, "mm"));
        minuteSpinnerHourly.setValue(editEntry != null && editEntry.getTimeType().equals(TIME_OPTIONS.HOURLY) ? new Date(editEntry.getTimestamp() * 1000l) : new Date());
        JLabel timeHourlyLabel;
        timeOptionPaneHourly.add(timeHourlyLabel = new JLabel(T._.addScheduleEntryDialog_minute() + ":"));
        constraints.put(timeHourlyLabel, "gapleft 10,");
        timeOptionPaneHourly.add(minuteSpinnerHourly);
        for (JComponent component : timeOptionPaneHourly) {
            panel.add(component, constraints.getOrDefault(component, "") + "hidemode 3");
            component.setVisible(false);
        }
        timeOptionPaneHourly.add(emptyLabel);

        /* Specific Days */
        timeOptionPaneSpecificDays = new LinkedList<JComponent>();

        JLabel timeSpecificDaysLabel;
        timeOptionPaneSpecificDays.add(timeSpecificDaysLabel = new JLabel(T._.addScheduleEntryDialog_time() + ":"));
        constraints.put(timeSpecificDaysLabel, "gapleft 10,");
        timeSpinnerSpecificDays = new JSpinner(new SpinnerDateModel());
        timeSpinnerSpecificDays.setEditor(new DateEditor(timeSpinnerSpecificDays, ((SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.SHORT)).toPattern()));
        timeSpinnerSpecificDays.setValue(editEntry != null && editEntry.getTimeType().equals(TIME_OPTIONS.SPECIFICDAYS) ? new Date(editEntry.getTimestamp() * 1000l) : new Date());
        timeOptionPaneSpecificDays.add(timeSpinnerSpecificDays);

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
        constraints.put(specificDaysSubPanel, "gapleft 10,spanx, growx, pushx,");

        for (JComponent component : timeOptionPaneSpecificDays) {
            panel.add(component, constraints.getOrDefault(component, "") + "hidemode 3");
            component.setVisible(false);
        }

        /* Interval */
        timeOptionPaneInterval = new LinkedList<JComponent>();
        hourSpinnerInterval = new JSpinner(new SpinnerNumberModel(editEntry != null && editEntry.getTimeType().equals(TIME_OPTIONS.CHOOSEINTERVAL) ? editEntry.getIntervalHour() : 1, 0, 365 * 24, 1));
        JLabel hoursIntervalLabel;
        timeOptionPaneInterval.add(hoursIntervalLabel = new JLabel(T._.addScheduleEntryDialog_hours() + ":"));
        constraints.put(hoursIntervalLabel, "gapleft 10,");
        timeOptionPaneInterval.add(hourSpinnerInterval);
        minuteSpinnerInterval = new JSpinner(new SpinnerNumberModel(editEntry != null && editEntry.getTimeType().equals(TIME_OPTIONS.CHOOSEINTERVAL) ? editEntry.getIntervalMinunte() : 0, 0, 59, 1));
        JLabel minutesIntervalLabel;
        timeOptionPaneInterval.add(minutesIntervalLabel = new JLabel(T._.addScheduleEntryDialog_minutes() + ":"));
        constraints.put(minutesIntervalLabel, "gapleft 10,");
        timeOptionPaneInterval.add(minuteSpinnerInterval);
        for (JComponent component : timeOptionPaneInterval) {
            panel.add(component, constraints.getOrDefault(component, "") + "hidemode 3");
            component.setVisible(false);
        }
        panel.add(emptyLabel, "spanx, hidemode 3");
        emptyLabel.setVisible(false);
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
    }

    @Override
    protected int getPreferredWidth() {
        if (content == null) {
            return 430;
        }
        return (int) content.getMinimumSize().getWidth() + 20;
    }

    protected void updatePanel() {
        if (content == null) {
            return;
        }
        // TODO check enable "ok" button
        getDialog().pack();
    }

    private void checkOK() {
        this.okButton.setEnabled(scheduleName.getText().length() > 0 || true); // TODO -> conditions
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