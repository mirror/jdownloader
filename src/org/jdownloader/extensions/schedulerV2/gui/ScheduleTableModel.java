package org.jdownloader.extensions.schedulerV2.gui;

import java.awt.Component;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.extensions.schedulerV2.SchedulerExtension;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper.TIME_OPTIONS;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntry;
import org.jdownloader.extensions.schedulerV2.translate.T;
import org.jdownloader.images.NewTheme;

public class ScheduleTableModel extends ExtTableModel<ScheduleEntry> {

    private final SchedulerExtension extension;

    public ScheduleTableModel(SchedulerExtension extension) {
        super("ScheduleTableModel");
        this.extension = extension;
        // List<ScheduleEntry> lst = CFG_SCHEDULER.CFG.getEntryList();
        updateDataModel();

    }

    public void updateDataModel() {
        _fireTableStructureChanged(new ArrayList<ScheduleEntry>(extension.getScheduleEntries()), true);
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
            }

            @Override
            public boolean isEditable(ScheduleEntry obj) {
                return true;
            }
        });

        this.addColumn(new ExtTextColumn<ScheduleEntry>(T._.scheduleTable_column_action()) {

            @Override
            public String getStringValue(ScheduleEntry value) {
                if (value.getAction() == null) {
                    return "UNKNOWN";
                }
                return value.getAction().getReadableName();

            }
        });

        this.addColumn(new ExtTextColumn<ScheduleEntry>(T._.scheduleTable_column_actionparameter()) {

            @Override
            public String getStringValue(ScheduleEntry value) {
                if (value.getAction() == null) {
                    return "UNKNOWN";
                }
                return value.getAction().getReadableParameter();
            }
        });

        this.addColumn(new ExtTextColumn<ScheduleEntry>(T._.scheduleTable_column_next_execution()) {

            @Override
            public String getStringValue(ScheduleEntry value) {
                TIME_OPTIONS timeType = value.getTimeType();
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(value.getTimestamp() * 1000l);
                switch (timeType) {
                case ONLYONCE:
                    return SimpleDateFormat.getInstance().format(c.getTime());
                case HOURLY: {
                    Calendar next = Calendar.getInstance();
                    next.set(Calendar.MINUTE, c.get(Calendar.MINUTE));
                    if (Calendar.getInstance().getTimeInMillis() > next.getTimeInMillis()) {
                        // in past
                        next.add(Calendar.HOUR_OF_DAY, 1);
                    }
                    return SimpleDateFormat.getInstance().format(next.getTime());
                }
                case SPECIFICDAYS: {
                    if (value.getSelectedDays().size() == 0) {
                        return "";
                    }
                    Calendar next = Calendar.getInstance();
                    next.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY));
                    next.set(Calendar.MINUTE, c.get(Calendar.MINUTE));
                    Calendar now = Calendar.getInstance();
                    if (next.get(Calendar.HOUR_OF_DAY) * 60 + next.get(Calendar.MINUTE) <= now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)) {
                        // in past
                        do {
                            next.add(Calendar.DAY_OF_WEEK, 1);
                        } while (!value.getSelectedDays().contains(ActionHelper.dayMap.get(next.get(Calendar.DAY_OF_WEEK))));
                    }
                    return SimpleDateFormat.getInstance().format(next.getTime());
                }
                case DAILY: {// TODO remove me
                    Calendar next = Calendar.getInstance();
                    next.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY));
                    next.set(Calendar.MINUTE, c.get(Calendar.MINUTE));
                    if (Calendar.getInstance().getTimeInMillis() > next.getTimeInMillis()) {
                        // in past
                        next.add(Calendar.DAY_OF_YEAR, 1);
                    }
                    return SimpleDateFormat.getInstance().format(next.getTime());
                }
                case WEEKLY:// TODO remove me
                    Calendar next = Calendar.getInstance();
                    next.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY));
                    next.set(Calendar.MINUTE, c.get(Calendar.MINUTE));
                    next.set(Calendar.DAY_OF_WEEK, c.get(Calendar.DAY_OF_WEEK));
                    if (Calendar.getInstance().getTimeInMillis() > next.getTimeInMillis()) {
                        // in past
                        next.add(Calendar.WEEK_OF_YEAR, 1);
                    }
                    return SimpleDateFormat.getInstance().format(next.getTime());
                case CHOOSEINTERVAL:
                    long nowS = Calendar.getInstance().getTimeInMillis() / 1000l;
                    long startS = value.getTimestamp();
                    long intervalS = 60 * 60 * value.getIntervalHour() + 60 * value.getIntervalMinunte();
                    long offsetS = (nowS - startS) % intervalS;
                    return SimpleDateFormat.getInstance().format((nowS - offsetS + intervalS) * 1000l);
                default:
                    return "?";
                }

            }
        });

        this.addColumn(new ExtTextColumn<ScheduleEntry>(T._.scheduleTable_column_repeats()) {
            Calendar c = Calendar.getInstance();

            @Override
            public String getStringValue(ScheduleEntry value) {
                TIME_OPTIONS timeType = value.getTimeType();
                switch (timeType) {
                case HOURLY: {
                    c.setTimeInMillis(value.getTimestamp() * 1000l);
                    String minute = String.valueOf(c.get(Calendar.MINUTE));
                    return T._.timeformat_repeats_hourly(minute);
                }
                case DAILY: {
                    c.setTimeInMillis(value.getTimestamp() * 1000l);
                    String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(c.getTime());
                    return T._.timeformat_repeats_daily(time);
                }
                case SPECIFICDAYS: {
                    if (value.getSelectedDays().size() == 0) {
                        // Never
                        return T._.lit_never();
                    }
                    c.setTimeInMillis(value.getTimestamp() * 1000l);
                    String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(c.getTime());

                    if (value.getSelectedDays().size() == 7) {
                        // Never
                        return T._.timeformat_repeats_daily(time);
                    }

                    String days = "";
                    for (int i = 0; i < value.getSelectedDays().size(); i++) {
                        if (i != 0) {
                            days += ", ";
                        }
                        days += value.getSelectedDays().get(i).getReadableName();
                    }

                    if (value.getSelectedDays().size() == 1) {
                        // Weekly
                        c.setTimeInMillis(value.getTimestamp() * 1000l);
                        return T._.timeformat_repeats_weekly(days, time);
                    }
                    return T._.timeformat_repeats_specificDays(days, time);
                }
                case WEEKLY: {
                    c.setTimeInMillis(value.getTimestamp() * 1000l);
                    String day = (new SimpleDateFormat("EEEE")).format(c.getTime());
                    String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(c.getTime());
                    return T._.timeformat_repeats_weekly(day, time);
                }
                case CHOOSEINTERVAL: {
                    String hour = String.valueOf(value.getIntervalHour());
                    String minute = String.valueOf(value.getIntervalMinunte());
                    return T._.timeformat_repeats_interval(hour, minute);
                }
                default:
                    return timeType.getReadableName();
                }
            }
        });
    }
}
