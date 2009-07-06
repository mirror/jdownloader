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

package jd.plugins.optional;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", id = "draganddroptarget", interfaceversion = 4)
public class DragAndDropTarget extends PluginOptional {
    private Object gui;

    public DragAndDropTarget(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public boolean initAddon() {
        return true;
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();

        MenuItem menuItem = new MenuItem(MenuItem.TOGGLE, JDL.L("jd.plugins.optional.draganddroptarget.menu.enable", "Enable"), 0).setActionListener(this);
        menuItem.setSelected(this.gui != null);
        menu.add(menuItem);

        return menu;
    }

    @Override
    public void onExit() {
        // TODO Auto-generated method stub

    }

}