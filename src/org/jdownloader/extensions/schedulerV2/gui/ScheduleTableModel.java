package org.jdownloader.extensions.schedulerV2.gui;

import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.extensions.schedulerV2.CFG_SCHEDULER;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper;
import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntry;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntryStorable;
import org.jdownloader.extensions.schedulerV2.translate.T;
import org.jdownloader.images.NewTheme;

public class ScheduleTableModel extends ExtTableModel<ScheduleEntry> implements GenericConfigEventListener<Object> {

    public ScheduleTableModel() {
        super("ScheduleTableModel");

        // List<ScheduleEntry> lst = CFG_SCHEDULER.CFG.getEntryList();
        CFG_SCHEDULER.ENTRY_LIST.getEventSender().addListener(this, true);
        updateDataModel();

    }

    private void updateDataModel() {

        ArrayList<ScheduleEntry> elements = new ArrayList<ScheduleEntry>();
        for (ScheduleEntryStorable storableEl : CFG_SCHEDULER.CFG.getEntryList()) {
            elements.add(new ScheduleEntry(storableEl));
        }
        _fireTableStructureChanged(elements, true);
    }

    /**
     * 
     */
    private static final long serialVersionUID = -4395270044662213519L;

    @Override
    protected void initColumns() {
        this.addColumn(new ExtCheckColumn<ScheduleEntry>(T._.scheduleTable_column_enable()) {

            private static final long serialVersionUID = 1515656228974789237L;

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    private static final long serialVersionUID = 3224931991570756349L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("ok", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public boolean isEditable(ScheduleEntry obj) {
                return true;
            }

            @Override
            protected boolean getBooleanValue(ScheduleEntry value) {
                return value.isEnabled();
            }

            @Override
            protected void setBooleanValue(boolean value, ScheduleEntry object) {
                object.setEnabled(value);
                save();
            }
        });

        this.addColumn(new ExtTextColumn<ScheduleEntry>(T._.scheduleTable_column_name()) {

            @Override
            public String getStringValue(ScheduleEntry value) {
                return value.getName();
            }

            @Override
            protected void setStringValue(String value, ScheduleEntry object) {
                object.setName(value);
                save();
            }

            @Override
            public boolean isEditable(ScheduleEntry obj) {
                return true;
            }
        });

        this.addColumn(new ExtTextColumn<ScheduleEntry>(T._.scheduleTable_column_action()) {

            @Override
            public String getStringValue(ScheduleEntry value) {
                return value.getAction().getReadableName();
            }
        });

        this.addColumn(new ExtTextColumn<ScheduleEntry>(T._.scheduleTable_column_actionparameter()) {

            @Override
            public String getStringValue(ScheduleEntry value) {
                if (value.getActionParameter() == null) {
                    return "";
                } else if (value.getAction().getParameterType().equals(ActionParameter.SPEED)) {
                    return SizeFormatter.formatBytes(Long.valueOf(value.getActionParameter())) + "/s";
                }
                return value.getActionParameter();
            }
        });

        this.addColumn(new ExtTextColumn<ScheduleEntry>(T._.scheduleTable_column_next_execution()) {

            @Override
            public String getStringValue(ScheduleEntry value) {
                String timeType = value.getTimeType();
                if (timeType.equals("ONLYONCE")) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(value.getTimestamp() * 1000l);
                    return SimpleDateFormat.getInstance().format(c.getTime());
                } else if (timeType.equals("HOURLY")) {

                } else if (timeType.equals("DAILY")) {

                } else if (timeType.equals("WEEKLY")) {

                } else if (timeType.equals("CHOOSEINTERVAL")) {

                }
                return "?";

            }
        });

        this.addColumn(new ExtTextColumn<ScheduleEntry>(T._.scheduleTable_column_repeats()) {

            @Override
            public String getStringValue(ScheduleEntry value) {
                String timeType = value.getTimeType();
                if (timeType.equals("HOURLY")) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(value.getTimestamp() * 1000l);
                    String minute = String.valueOf(c.get(Calendar.MINUTE));
                    return T._.timeformat_repeats_hourly(minute);
                } else if (timeType.equals("DAILY")) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(value.getTimestamp() * 1000l);
                    String time = SimpleDateFormat.getTimeInstance().format(c.getTime());
                    return T._.timeformat_repeats_daily(time);
                } else if (timeType.equals("WEEKLY")) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(value.getTimestamp() * 1000l);
                    String day = (new SimpleDateFormat("EEEE")).format(c.getTime());
                    String time = SimpleDateFormat.getTimeInstance().format(c.getTime());
                    return T._.timeformat_repeats_weekly(day, time);
                } else if (timeType.equals("CHOOSEINTERVAL")) {
                    String hour = String.valueOf(value.getIntervalHour());
                    String minute = String.valueOf(value.getIntervalMin());
                    return T._.timeformat_repeats_interval(hour, minute);
                }
                return ActionHelper.getPrettyTimeOption(value.getTimeType());
            }
        });
    }

    protected void save() {
        ArrayList<ScheduleEntryStorable> storables = new ArrayList<ScheduleEntryStorable>(getTableData().size());
        for (ScheduleEntry entry : getTableData()) {
            storables.add(entry.getStorable());
        }
        CFG_SCHEDULER.CFG.setEntryList(storables);
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        updateDataModel();
    }

}
