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

package jd.gui.skins.simple.startmenu.actions;

import java.awt.event.ActionEvent;

public class AddonConfiguration extends StartAction {

    private static final long serialVersionUID = 5296731283280444433L;

    public AddonConfiguration() {
        super("action.addonconfig", "gui.images.taskpanes.addons");
    }

    public void actionPerformed(ActionEvent e) {
//        SimpleGuiConstants.GUI_CONFIG.setProperty("LAST_CONFIG_PANEL", ConfigTaskPane.ACTION_ADDONS);
        //TODO
//        SwingGui.getInstance().getTaskPane().switcher(SwingGui.getInstance().getCfgTskPane());
    }

}
