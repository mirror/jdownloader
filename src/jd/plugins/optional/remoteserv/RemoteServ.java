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

package jd.plugins.optional.remoteserv;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.event.ControlEvent;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;

@OptionalPlugin(rev = "$Revision: 11760 $", id = "remoteserv", hasGui = true, interfaceversion = 7, minJVM = 1.6, linux = true, windows = true, mac = true)
public class RemoteServ extends PluginOptional {

    public RemoteServ(final PluginWrapper wrapper) {
        super(wrapper);
        this.initConfigEntries();

    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        // add main menu items.. this item is used to show/hide GUi
        final ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        return menu;
    }

    @Override
    public String getIconKey() {
        return "gui.images.chat";
    }

    @Override
    public boolean initAddon() {
        // this method is called ones after the addon has been loaded

        return true;
    }

    private void initConfigEntries() {
    }

    @Override
    public void onControlEvent(final ControlEvent event) {
        switch (event.getEventID()) {
        case ControlEvent.CONTROL_INIT_COMPLETE:

        }
    }

    @Override
    public void onExit() {

    }

    @Override
    public void setGuiEnable(final boolean b) {

    }

}
