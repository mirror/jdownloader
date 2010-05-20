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

package jd.plugins.optional.improveddock;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.event.ControlEvent;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;

@OptionalPlugin(rev = "$Revision$", defaultEnabled = true, id = "improvedmacosxdock", interfaceversion = 5, minJVM = 1.6, windows = false, linux = false)
public class ImrovedMacOSXDock extends PluginOptional {

    private MacDockIconChanger updateThread;

    public ImrovedMacOSXDock(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public boolean initAddon() {
        return true;
    }

    @Override
    public void onExit() {
        if (updateThread != null) {
            updateThread.stopUpdating();
            updateThread = null;
        }
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    @Override
    public void onControlEvent(ControlEvent event) {
        switch (event.getID()) {
        case ControlEvent.CONTROL_DOWNLOAD_START:
            if (updateThread == null) {
                updateThread = new MacDockIconChanger();
                updateThread.start();
            }
            break;
        case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
        case ControlEvent.CONTROL_DOWNLOAD_STOP:
            if (updateThread != null) {
                updateThread.stopUpdating();
                updateThread = null;
            }
            break;
        }
    }

}
