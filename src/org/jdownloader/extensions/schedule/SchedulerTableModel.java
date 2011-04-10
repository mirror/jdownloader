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

package org.jdownloader.extensions.schedule;


 import org.jdownloader.extensions.schedule.translate.*;
import org.jdownloader.extensions.schedule.columns.ActionColumn;
import org.jdownloader.extensions.schedule.columns.DateColumn;
import org.jdownloader.extensions.schedule.columns.EnabledColumn;
import org.jdownloader.extensions.schedule.columns.NameColumn;
import org.jdownloader.extensions.schedule.columns.NextExeColumn;
import org.jdownloader.extensions.schedule.columns.RepeatsColumn;
import org.jdownloader.extensions.schedule.columns.TimeColumn;

import jd.gui.swing.components.table.JDTableModel;
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
        this.addColumn(new EnabledColumn(T._.jd_plugins_optional_schedule_SchedulerTableModel_onoff(), this));
        this.addColumn(new NameColumn(T._.jd_plugins_optional_schedule_SchedulerTableModel_name(), this));
        this.addColumn(new ActionColumn(T._.jd_plugins_optional_schedule_SchedulerTableModel_action(), this));
        this.addColumn(new DateColumn(T._.jd_plugins_optional_schedule_SchedulerTableModel_date(), this));
        this.addColumn(new TimeColumn(T._.jd_plugins_optional_schedule_SchedulerTableModel_time(), this));
        this.addColumn(new NextExeColumn(T._.jd_plugins_optional_schedule_SchedulerTableModel_nextexecution(), this));
        this.addColumn(new RepeatsColumn(T._.jd_plugins_optional_schedule_SchedulerTableModel_repeats(), this));
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