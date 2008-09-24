//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.event.ControlListener;
import jd.parser.Regex;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Schedule extends PluginOptional implements ControlListener {
    public Schedule(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static int getAddonInterfaceVersion() {
        return 2;
    }

    ScheduleControl sControl = new ScheduleControl();

    @Override
    public void actionPerformed(ActionEvent e) {
        sControl.status.start();
        sControl.status.setInitialDelay(1000);
        sControl.setVisible(true);
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        menu.add(new MenuItem(JDLocale.L("gui.btn_settings", "Settings"), 0).setActionListener(this));
        return menu;
    }

    @Override
    public String getCoder() {
        return "Tudels";
    }

    @Override
    public String getHost() {
        return JDLocale.L("addons.schedule.name", "Schedule");
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public boolean initAddon() {

        logger.info("Schedule OK");
        JDUtilities.getController().addControlListener(this);
        return true;

    }

    @Override
    public void onExit() {

    }

}
