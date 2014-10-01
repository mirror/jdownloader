package org.jdownloader.extensions.schedulerV2.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DateEditor;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.schedulerV2.CFG_SCHEDULER;
import org.jdownloader.extensions.schedulerV2.actions.IScheduleAction;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper;
import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntry;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntryStorable;
import org.jdownloader.extensions.schedulerV2.translate.T;
import org.jdownloader.gui.translate._GUI;

public class AddScheduleEntryDialog extends AbstractDialog<ScheduleEntry> {

    public static void showDialog() {
        final AddScheduleEntryDialog dialog = new AddScheduleEntryDialog();

        try {
            ScheduleEntry result = Dialog.getInstance().showDialog(dialog);
            if (result != null) {
                ArrayList<ScheduleEntryStorable> lst = CFG_SCHEDULER.CFG.getEntryList();
                lst.add(result.getStorable());
                CFG_SCHEDULER.CFG.setEntryList(lst);
            }
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

    }

    private Object getScheduleRule() {
        return null;
    }

    private JPanel       content;
    private TextInput    scheduleName;
    private MigPanel     timePane;
    private MigPanel     timeOptionPaneOnlyOnce;
    private JSpinner     timeSpinnerOnce;
    private JSpinner     dateSpinnerOnce;
    private JSpinner     minuteSpinnerHourly;
    private MigPanel     timeOptionPaneHourly;
    private MigPanel     timeOptionPaneDaily;
    private JSpinner     timeSpinnerDaily;
    private MigPanel     timeOptionPaneWeekly;
    private JSpinner     timeSpinnerWeekly;
    private JSpinner     hourSpinnerInterval;
    private MigPanel     timeOptionPaneInterval;
    private JSpinner     minuteSpinnerInterval;
    private ComboBox     intervalBox;
    private ComboBox     actionBox;
    private MigPanel     actionParameterPanelNone;
    private MigPanel     actionParameterPanelSpeed;
    private MigPanel     actionParameterPanel;
    private MigPanel     actionParameterPanelInt;
    private JSpinner     intParameterSpinner;
    private SpeedSpinner downloadspeedSpinner;

    private AddScheduleEntryDialog() {
        super(UserIO.NO_ICON, T._.addScheduleEntryDialog_title(), null, _GUI._.lit_save(), null);
    }

    @Override
    protected ScheduleEntry createReturnValue() {
        ScheduleEntry entry = new ScheduleEntry();

        entry.setEnabled(true);
        entry.setName(scheduleName.getText());

        IScheduleAction action = ActionHelper.ACTIONS.get(actionBox.getSelectedIndex());
        try {
            String parameter = null;
            if (action.getParameterType().equals(ActionParameter.SPEED)) {
                parameter = String.valueOf(downloadspeedSpinner.getBytes());
            } else if (action.getParameterType().equals(ActionParameter.INT)) {
                parameter = String.valueOf(intParameterSpinner.getValue());
            }
            entry.setAction(action.getClass().newInstance(), parameter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String timeType = ActionHelper.TIME_OPTIONS[intervalBox.getSelectedIndex()];
        entry.setTimeType(timeType);
        if (timeType.equals("ONLYONCE")) {
            Date time = (Date) timeSpinnerOnce.getValue();
            Date date = (Date) dateSpinnerOnce.getValue();
            Calendar timestamp = Calendar.getInstance();
            timestamp.set(date.getYear() + 1900, date.getMonth(), date.getDate(), time.getHours(), time.getMinutes());
            entry.setTimestamp(timestamp.getTimeInMillis() / 1000l);
        } else if (timeType.equals("HOURLY")) {
            Date d = (Date) minuteSpinnerHourly.getValue();
            Calendar timestamp = Calendar.getInstance();
            timestamp.set(Calendar.SECOND, 0);
            timestamp.set(Calendar.MINUTE, d.getMinutes());
            entry.setTimestamp(timestamp.getTimeInMillis() / 1000l);
        } else if (timeType.equals("DAILY")) {
            Date d = (Date) timeSpinnerDaily.getValue();
            Calendar timestamp = Calendar.getInstance();
            timestamp.set(Calendar.SECOND, 0);
            timestamp.set(Calendar.MINUTE, d.getMinutes());
            timestamp.set(Calendar.HOUR_OF_DAY, d.getHours());
            entry.setTimestamp(timestamp.getTimeInMillis() / 1000l);
        } else if (timeType.equals("WEEKLY")) {
            Date d = (Date) timeSpinnerWeekly.getValue();
            Calendar timestamp = Calendar.getInstance();
            timestamp.set(Calendar.SECOND, 0);
            timestamp.set(Calendar.MINUTE, d.getMinutes());
            timestamp.set(Calendar.HOUR_OF_DAY, d.getHours());
            timestamp.set(Calendar.DAY_OF_WEEK, d.getDay()); // TODO
            entry.setTimestamp(timestamp.getTimeInMillis() / 1000l);
        } else if (timeType.equals("CHOOSEINTERVAL")) {
            entry.setTimestamp(Calendar.getInstance().getTimeInMillis() / 1000l);
            entry.setIntervalHour((Integer) hourSpinnerInterval.getValue());
            entry.setIntervalMin((Integer) minuteSpinnerInterval.getValue());
        }

        return entry;

    }

    @Override
    public JComponent layoutDialogContent() {
        // content = new JPanel();

        MigPanel migPanel = new MigPanel("ins 0 10 5 10, wrap 2", "[][grow]", "");

        migPanel.setOpaque(false);

        migPanel.add(new JLabel("Name:"), "growx");
        scheduleName = new TextInput("Schedule");
        scheduleName.setColumns(1000);
        migPanel.add(scheduleName, "growx");

        migPanel.add(header("Time / Interval"), "spanx, growx,newline 15");
        migPanel.add(new JLabel("Repeat:"));

        ArrayList<String> options = new ArrayList<String>();
        for (int i = 0; i < ActionHelper.TIME_OPTIONS.length; i++) {
            options.add(ActionHelper.getPrettyTimeOption(ActionHelper.TIME_OPTIONS[i]));
        }

        intervalBox = new ComboBox(options.toArray());
        intervalBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ComboBox cb = (ComboBox) e.getSource();
                String selected = ActionHelper.TIME_OPTIONS[cb.getSelectedIndex()];
                selectTimeOptionPane(selected);
            }
        });
        intervalBox.setSelectedIndex(0);
        migPanel.add(intervalBox, "alignx right");

        // Begin time subpanels
        timePane = new MigPanel("", "", "");
        setupTimeOptionPanes();
        selectTimeOptionPane(ActionHelper.TIME_OPTIONS[0]);
        migPanel.add(timePane, "spanx, growx");
        // timeSpinner = new JSpinner(new SpinnerDateModel());
        // DateEditor timeEditorWeekly = new DateEditor(timeSpinner, "E, HH:mm");
        //
        // timeSpinner.setEditor(timeEditorWeekly);
        // timeSpinner.setValue(new Date());
        // migPanel.add(timeSpinner, "alignx right");

        // Begin action area
        migPanel.add(header("Action & Parameters"), "spanx, growx,newline 15");
        migPanel.add(new JLabel("Action:"));

        LinkedList<String> actionOptions = new LinkedList<String>();
        for (IScheduleAction action : ActionHelper.ACTIONS) {
            actionOptions.add(action.getReadableName());
        }
        actionBox = new ComboBox(actionOptions.toArray());
        actionBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ComboBox<String> cb = (ComboBox<String>) e.getSource();
                selectActionParameterPane(ActionHelper.ACTIONS.get(cb.getSelectedIndex()).getParameterType());
                ;
            }
        });
        migPanel.add(actionBox, "alignx right");

        actionParameterPanel = new MigPanel("ins 0", "", "");
        setupActionParameterPanes();
        selectActionParameterPane(ActionHelper.ACTIONS.get(actionBox.getSelectedIndex()).getParameterType());
        migPanel.add(actionParameterPanel, "spanx, growx, pushx");

        content = migPanel;
        updatePanel();

        return content;
    }

    private void selectActionParameterPane(ActionParameter actionParameter) {

        actionParameterPanel.removeAll();
        actionParameterPanel.setLayout(new MigLayout("ins 0 15 0 0", "", ""));
        switch (actionParameter) {
        case INT:
            actionParameterPanel.add(actionParameterPanelInt, "growx, pushx");
            actionParameterPanelInt.setVisible(true);
            break;
        case SPEED:
            actionParameterPanel.add(actionParameterPanelSpeed, "growx, pushx");
            actionParameterPanelSpeed.setVisible(true);
            break;
        default:
            actionParameterPanel.add(actionParameterPanelNone, "growx,pushx");
            actionParameterPanelNone.setVisible(true);
            break;
        }
        actionParameterPanel.repaint();
    }

    private void setupActionParameterPanes() {

        actionParameterPanelNone = new MigPanel("ins 6 0 0 6", "", "");
        JLabel lbl = new JLabel("No parameters to set.");
        lbl.setEnabled(false);

        actionParameterPanelNone.add(lbl);
        actionParameterPanelNone.setVisible(false);

        actionParameterPanelSpeed = new MigPanel("ins 0,wrap 2", "", "");

        actionParameterPanelSpeed.add(new JLabel("Speed:"), "width 18%");

        downloadspeedSpinner = new SpeedSpinner(0l, 100 * 1024 * 1024 * 1024l, 1l);
        downloadspeedSpinner.setValue(1 * 1024 * 1024);
        actionParameterPanelSpeed.add(downloadspeedSpinner, "width 30%");
        actionParameterPanelSpeed.setVisible(false);

        actionParameterPanelInt = new MigPanel("ins 0,wrap 2", "", "");
        actionParameterPanelInt.add(new JLabel("Number:"), "growx, width 18%");
        intParameterSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 25, 1));
        actionParameterPanelInt.add(intParameterSpinner, "growx, width 30%");
        actionParameterPanelInt.setVisible(false);
    }

    private void setupTimeOptionPanes() {

        timeOptionPaneOnlyOnce = new MigPanel(new MigLayout("ins 0,wrap 4", "", ""));
        dateSpinnerOnce = new JSpinner(new SpinnerDateModel());
        dateSpinnerOnce.setEditor(new DateEditor(dateSpinnerOnce, "dd.MM.yyyy"));
        dateSpinnerOnce.setValue(new Date());
        timeSpinnerOnce = new JSpinner(new SpinnerDateModel());
        timeSpinnerOnce.setEditor(new DateEditor(timeSpinnerOnce, "HH:mm"));
        timeSpinnerOnce.setValue(new Date());
        timeOptionPaneOnlyOnce.add(new JLabel("Date:"), "growx, width 18%");
        timeOptionPaneOnlyOnce.add(dateSpinnerOnce, "growx, width 30%");
        timeOptionPaneOnlyOnce.add(new JLabel("Time:"), "growx, width 18%,gapleft 4%");
        timeOptionPaneOnlyOnce.add(timeSpinnerOnce, "growx, width 30%");

        timeOptionPaneHourly = new MigPanel(new MigLayout("ins 0,wrap 2", "", ""));
        minuteSpinnerHourly = new JSpinner(new SpinnerDateModel());
        minuteSpinnerHourly.setEditor(new DateEditor(minuteSpinnerHourly, "mm"));
        minuteSpinnerHourly.setValue(new Date());
        timeOptionPaneHourly.add(new JLabel("Minute:"), "growx, width 18%");
        timeOptionPaneHourly.add(minuteSpinnerHourly, "growx, width 30%");
        timeOptionPaneHourly.setVisible(false);

        timeOptionPaneDaily = new MigPanel(new MigLayout("ins 0,wrap 2", "", ""));
        timeSpinnerDaily = new JSpinner(new SpinnerDateModel());
        timeSpinnerDaily.setEditor(new DateEditor(timeSpinnerDaily, "HH:mm"));
        timeSpinnerDaily.setValue(new Date());
        timeOptionPaneDaily.add(new JLabel("Time:"), "growx, width 18%");
        timeOptionPaneDaily.add(timeSpinnerDaily, "growx, width 30%");
        timeOptionPaneDaily.setVisible(false);

        timeOptionPaneWeekly = new MigPanel(new MigLayout("ins 0,wrap 2", "", ""));
        timeSpinnerWeekly = new JSpinner(new SpinnerDateModel());
        timeSpinnerWeekly.setEditor(new DateEditor(timeSpinnerWeekly, "E, HH:mm"));
        timeSpinnerWeekly.setValue(new Date());
        timeOptionPaneWeekly.add(new JLabel("Time:"), "growx, width 18%");
        timeOptionPaneWeekly.add(timeSpinnerWeekly, "growx, width 30%");
        timeOptionPaneWeekly.setVisible(false);

        timeOptionPaneInterval = new MigPanel(new MigLayout("ins 0,wrap 4", "", ""));

        hourSpinnerInterval = new JSpinner(new SpinnerNumberModel(1, 0, 365 * 24, 1));
        timeOptionPaneInterval.add(new JLabel("Hours:"), "growx, width 18%");
        timeOptionPaneInterval.add(hourSpinnerInterval, "growx, width 30%");
        minuteSpinnerInterval = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        timeOptionPaneInterval.add(new JLabel("Minutes:"), "growx, width 18%");
        timeOptionPaneInterval.add(minuteSpinnerInterval, "growx, width 30%, gapleft 4px");

        timeOptionPaneInterval.setVisible(false);
    }

    private void selectTimeOptionPane(String inteval) {
        if (timeOptionPaneOnlyOnce == null) {
            return;
        }
        timePane.removeAll();
        timePane.setLayout(new MigLayout("ins 0 15 0 0", "", ""));
        if (inteval.equals("ONLYONCE")) {
            timePane.add(timeOptionPaneOnlyOnce, "pushx, growx");
        } else if (inteval.equals("HOURLY")) {
            timePane.add(timeOptionPaneHourly, "pushx, growx");
            timeOptionPaneHourly.setVisible(true);
        } else if (inteval.equals("DAILY")) {
            timePane.add(timeOptionPaneDaily, "pushx, growx");
            timeOptionPaneDaily.setVisible(true);
        } else if (inteval.equals("WEEKLY")) {
            timePane.add(timeOptionPaneWeekly, "pushx, growx");
            timeOptionPaneWeekly.setVisible(true);
        } else if (inteval.equals("CHOOSEINTERVAL")) {
            timePane.add(timeOptionPaneInterval, "pushx, growx");
            timeOptionPaneInterval.setVisible(true);
        }
        timePane.repaint();
    }

    protected int getPreferredWidth() {
        return 400;
    }

    protected void updatePanel() {
        // try {
        if (content == null) {
            return;
        }
        // PluginForHost plg = plugin;
        // if (plg == null) {
        // LazyHostPlugin p = HostPluginController.getInstance().get(getPreselectedHoster());
        // if (p == null) {
        // Iterator<LazyHostPlugin> it = HostPluginController.getInstance().list().iterator();
        // if (it.hasNext()) {
        // p = it.next();
        // }
        // }
        // hoster.setSelectedItem(p);
        // plg = p.newInstance(cl);
        // }
        //
        // AccountFactory accountFactory = plg.getAccountFactory();
        // if (editAccountPanel != null) {
        // defaultAccount = editAccountPanel.getAccount();
        // content.remove(editAccountPanel.getComponent());
        // }
        // editAccountPanel = accountFactory.getPanel();
        // content.add(editAccountPanel.getComponent(), "gapleft 32,spanx");
        // editAccountPanel.setAccount(defaultAccount);
        // editAccountPanel.setNotifyCallBack(new Notifier() {
        //
        // @Override
        // public void onNotify() {
        // checkOK();
        // }
        //
        // });

        // scheduleName.addActionListener(new ActionListener() {
        //
        // @Override
        // public void actionPerformed(ActionEvent e) {
        // checkOK();
        // }
        // });
        //
        // checkOK();
        getDialog().pack();

        // } catch (UpdateRequiredClassNotFoundException e) {
        // e.printStackTrace();
        // }
    }

    private void checkOK() {
        this.okButton.setEnabled(scheduleName.getText().length() > 0 || true); // todo -> conditions
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