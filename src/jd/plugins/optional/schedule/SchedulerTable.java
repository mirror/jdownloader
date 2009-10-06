package jd.plugins.optional.schedule;

import jd.gui.swing.components.table.JDTable;

public class SchedulerTable extends JDTable {

    private static final long serialVersionUID = -9118598915864230546L;

    public SchedulerTable(Schedule schedule) {
        super(new SchedulerTableModel("schedulerview", schedule));
    }

    @Override
    public SchedulerTableModel getModel() {
        return (SchedulerTableModel) super.getModel();
    }

}
