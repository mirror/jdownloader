package jd.gui.swing.jdgui.menu.actions.sendlogs;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;

public class LogModel extends ExtTableModel<LogFolder> {

    public LogModel(ArrayList<LogFolder> folders) {
        super("LogModel");
        this.tableData = folders;
        Collections.sort(folders, new Comparator<LogFolder>() {

            @Override
            public int compare(LogFolder o1, LogFolder o2) {
                return new Long(o2.getCreated()).compareTo(new Long(o1.getCreated()));
            }
        });
    }

    @Override
    protected void initColumns() {
        addColumn(new ExtCheckColumn<LogFolder>(_GUI._.LogModel_initColumns_x_()) {

            @Override
            protected boolean getBooleanValue(LogFolder value) {
                return value.isSelected();
            }

            @Override
            public boolean isSortable(final LogFolder obj) {
                return false;
            }

            @Override
            public boolean isEditable(LogFolder obj) {

                return true;
            }

            @Override
            protected void setBooleanValue(boolean value, LogFolder object) {
                object.setSelected(value);
            }
        });

        ExtTextColumn<LogFolder> sort;
        addColumn(sort = new ExtTextColumn<LogFolder>(_GUI._.LogModel_initColumns_time_()) {
            {
                this.setRowSorter(new ExtDefaultRowSorter<LogFolder>() {

                    @Override
                    public int compare(final LogFolder o1, final LogFolder o2) {
                        if (this.getSortOrderIdentifier() != ExtColumn.SORT_ASC) {
                            return new Long(o1.getCreated()).compareTo(new Long(o2.getCreated()));
                        } else {
                            return new Long(o2.getCreated()).compareTo(new Long(o1.getCreated()));
                        }

                    }

                });
            }

            @Override
            public boolean isSortable(final LogFolder obj) {
                return false;
            }

            @Override
            public String getStringValue(LogFolder value) {

                String from = DateFormat.getInstance().format(new Date(value.getCreated()));
                String to = DateFormat.getInstance().format(new Date(value.getLastModified()));
                return _GUI._.LogModel_getStringValue_between_(from, to);
            }
        });

    }

}
