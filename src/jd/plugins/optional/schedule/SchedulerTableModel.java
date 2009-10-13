package jd.plugins.optional.schedule;

import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.schedule.columns.DateColumn;
import jd.plugins.optional.schedule.columns.EnabledColumn;
import jd.plugins.optional.schedule.columns.NameColumn;
import jd.plugins.optional.schedule.columns.NextExeColumn;
import jd.plugins.optional.schedule.columns.NumberColumn;
import jd.plugins.optional.schedule.columns.RepeatsColumn;
import jd.plugins.optional.schedule.columns.TimeColumn;
import jd.utils.locale.JDL;

public class SchedulerTableModel extends JDTableModel {

    private static final long serialVersionUID = 4878129559346795192L;
    private static final String JDL_PREFIX = "jd.plugins.optional.schedule.SchedulerTableModel.";

    private Schedule schedule;

    public SchedulerTableModel(String configname, Schedule schedule) {
        super(configname);

        this.schedule = schedule;
    }

    protected void initColumns() {
        this.addColumn(new EnabledColumn("", this, schedule));
        this.addColumn(new NameColumn(JDL.L(JDL_PREFIX + "name", "Name"), this));
        this.addColumn(new DateColumn(JDL.L(JDL_PREFIX + "date", "Date"), this));
        this.addColumn(new TimeColumn(JDL.L(JDL_PREFIX + "time", "Time"), this));
        this.addColumn(new NextExeColumn(JDL.L(JDL_PREFIX + "nextexecution", "Next Execution"), this));
        this.addColumn(new RepeatsColumn(JDL.L(JDL_PREFIX + "repeats", "Repeats"), this));
        this.addColumn(new NumberColumn(JDL.L(JDL_PREFIX + "number", "# of actions"), this));
    }

    @Override
    public void refreshModel() {
        synchronized (list) {
            list.clear();
            list.addAll(schedule.getActions());
        }
    }

}
