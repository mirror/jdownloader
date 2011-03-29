//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.schedule;

import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.schedule.columns.ActionColumn;
import jd.plugins.optional.schedule.columns.DateColumn;
import jd.plugins.optional.schedule.columns.EnabledColumn;
import jd.plugins.optional.schedule.columns.NameColumn;
import jd.plugins.optional.schedule.columns.NextExeColumn;
import jd.plugins.optional.schedule.columns.RepeatsColumn;
import jd.plugins.optional.schedule.columns.TimeColumn;
import jd.utils.locale.JDL;

public class SchedulerTableModel extends JDTableModel {

    private static final long serialVersionUID = 4878129559346795192L;
    private static final String JDL_PREFIX = "jd.plugins.optional.schedule.SchedulerTableModel.";

    private ScheduleExtension schedule = null;

    public SchedulerTableModel(String configname, ScheduleExtension schedule) {
        super(configname);
        this.schedule = schedule;
    }

    protected void initColumns() {
        this.addColumn(new EnabledColumn(JDL.L(JDL_PREFIX + "onoff", "On/Off"), this));
        this.addColumn(new NameColumn(JDL.L(JDL_PREFIX + "name", "Name"), this));
        this.addColumn(new ActionColumn(JDL.L(JDL_PREFIX + "action", "Action"), this));
        this.addColumn(new DateColumn(JDL.L(JDL_PREFIX + "date", "Date"), this));
        this.addColumn(new TimeColumn(JDL.L(JDL_PREFIX + "time", "Time"), this));
        this.addColumn(new NextExeColumn(JDL.L(JDL_PREFIX + "nextexecution", "Next Execution"), this));
        this.addColumn(new RepeatsColumn(JDL.L(JDL_PREFIX + "repeats", "Repeats"), this));
    }

    @Override
    public void refreshModel() {
        synchronized (list) {
            synchronized (ScheduleExtension.LOCK) {
                list.clear();
                list.addAll(schedule.getActions());
            }
        }
    }

}
