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

package jd.plugins.optional.jdpremserver;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.event.ControlEvent;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;

/**
 * order : # isJPfmUsable # prepare --> if mount location not set or problematic
 * --> ignore --> end # User selected to mount a file -> if file already present
 * -> ignore else watch() invoked # volumes automatically unmounted and all
 * kernel resources freed on exit.
 * 
 * @author Shashank Tulsyan
 */
@OptionalPlugin(rev = "$Revision: 11760 $", id = "neembuu", hasGui = true, interfaceversion = 5, minJVM = 1.7, linux = false, windows = true, mac = false)
public class JDPremServer extends PluginOptional {

    public JDPremServer(PluginWrapper wrapper) {
        super(wrapper);
        initConfigEntries();

    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @SuppressWarnings("unchecked")
    @Override
    public void onControlEvent(ControlEvent event) {
        // receiver all control events. reconnects,
        // downloadstarts/end,pluginstart/pluginend

        DownloadLink link;
        switch (event.getID()) {

        case ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU:

            break;
        }
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    @Override
    public boolean initAddon() {
        // this method is called ones after the addon has been loaded

        return true;
    }

    private void initConfigEntries() {

    }

    @Override
    public void onExit() {
        // addon disabled/tabe closed
    }

    @Override
    public String getIconKey() {
        // should use an own icon later
        return "gui.images.chat";
    }

}
