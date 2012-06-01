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

package org.jdownloader.extensions.schedule.columns;

import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextTableColumn;

import org.jdownloader.extensions.schedule.Actions;
import org.jdownloader.extensions.schedule.translate.T;

public class RepeatsColumn extends JDTextTableColumn {

    private static final long serialVersionUID = 5634248167278175584L;

    public RepeatsColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    public boolean isEnabled(Object obj) {
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    @Override
    protected String getStringValue(Object value) {
        switch (((Actions) value).getRepeat()) {
        case 0:
            return T._.jd_plugins_optional_schedule_MainGui_MyTableModel_add_once();
        case 60:
            return T._.jd_plugins_optional_schedule_MainGui_MyTableModel_add_hourly();
        case 1440:
            return T._.jd_plugins_optional_schedule_MainGui_MyTableModel_add_daily();
        case 10080:
            return T._.jd_plugins_optional_schedule_MainGui_MyTableModel_add_weekly();
        }
        int hour = ((Actions) value).getRepeat() / 60;
        return T._.jd_plugins_optional_schedule_MainGui_MyTableModel_add_interval(hour, ((Actions) value).getRepeat() - (hour * 60));

    }

}