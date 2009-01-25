//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.utils.JDUtilities;

abstract public class PluginForPasswordProtectedHost extends PluginForHost implements ControlListener {

    public PluginForPasswordProtectedHost(PluginWrapper wrapper) {
        super(wrapper);
        JDUtilities.getController().addControlListener(this);
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU) {
            ArrayList<MenuItem> entries = (ArrayList<MenuItem>) event.getParameter();

            MenuItem m;
            // Als id was 100% eindeutiges nehmen ;-P
            entries.add(m = new MenuItem("Testitem", 11111));
            m.setActionListener(this);
        }

    }

    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (e.getID() == 1) {
            System.out.println("HUHU");

        }

    }

}
