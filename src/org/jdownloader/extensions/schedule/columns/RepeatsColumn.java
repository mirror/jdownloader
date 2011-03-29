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

import org.jdownloader.extensions.schedule.Actions;

import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextTableColumn;
import jd.utils.locale.JDL;

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
            return JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.add.once", "Only once");
        case 60:
            return JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.add.hourly", "Hourly");
        case 1440:
            return JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.add.daily", "Daily");
        case 10080:
            return JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.add.weekly", "Weekly");
        }
        int hour = ((Actions) value).getRepeat() / 60;
        return JDL.LF("jd.plugins.optional.schedule.MainGui.MyTableModel.add.interval", "Interval: %sh %sm", hour, ((Actions) value).getRepeat() - (hour * 60));

    }

}
