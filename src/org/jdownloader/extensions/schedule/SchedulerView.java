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

import javax.swing.Icon;

import jd.gui.swing.jdgui.views.ClosableView;
import jd.utils.JDTheme;

import org.jdownloader.extensions.schedule.translate.T;

public class SchedulerView extends ClosableView {
    private static final long serialVersionUID = -7876057076125402969L;

    public SchedulerView() {
        super();

        init();
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.config.eventmanager", 16, 16);
    }

    @Override
    public String getTitle() {
        return T._.jd_plugins_optional_schedule_SchedulerView_title();
    }

    @Override
    public String getTooltip() {
        return T._.jd_plugins_optional_schedule_SchedulerView_tooltip();
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
    }

    @Override
    public String getID() {
        return "schedulerview";
    }
}