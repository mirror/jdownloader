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

package jd.gui.swing.jdgui.menu;

import java.util.ArrayList;

import jd.gui.swing.jdgui.actions.ToolBarAction;

public class MenuAction extends ToolBarAction {

    private static final long serialVersionUID = 2731508542740902624L;
    private ArrayList<MenuAction> items;

    public MenuAction(String pluginID, int i) {
        super(pluginID, i);
    }

    public MenuAction(String pluginID, String icon) {
        super(pluginID, icon);
    }

    public MenuAction(Types separator) {
        super();
        this.setType(separator);
    }

    @Override
    public void init() {

    }

    @Override
    public void initDefaults() {

    }

    public void setItems(ArrayList<MenuAction> mis) {
        if (mis != null && mis.size() > 0) this.setType(Types.CONTAINER);
        this.items = mis;
    }

    public ArrayList<MenuAction> getItems() {
        if (items == null) items = new ArrayList<MenuAction>();
        return items;
    }

    public int getSize() {
        return getItems().size();
    }

    public MenuAction get(int i) {
        return getItems().get(i);
    }

    public void addMenuItem(MenuAction m) {
        getItems().add(m);
        this.setType(Types.CONTAINER);
    }

}
