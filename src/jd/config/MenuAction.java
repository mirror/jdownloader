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

package jd.config;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import jd.controlling.JDLogger;
import jd.gui.action.JDAction;
import jd.plugins.Plugin;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class MenuAction extends JDAction {
    public static final int CONTAINER = 0;
    public static final int NORMAL = 1;
    public static final int SEPARATOR = 3;

    private static final long serialVersionUID = 9205555751462125274L;
    public static final int TOGGLE = 2;
    private static final String MENUITEMS = "MENUITEMS";

    private int id = NORMAL;
    private ArrayList<MenuAction> items;
    private Plugin plugin;

    public MenuAction(int id) {
        this(id, null, -1);
    }

    /**
     * this is just a delegate to adjust return Type
     * 
     * @see JDAction#setActionListener(ActionListener)
     */
    public MenuAction setActionListener(ActionListener actionListener) {
        return (MenuAction) super.setActionListener(actionListener);
    }

    public MenuAction(int id, String title, int actionID) {
        super(title, actionID);
        this.id = id;
    }

    public int getId() {
        
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public MenuAction(String title, int actionID) {
        this(NORMAL, title, actionID);
    }

    public MenuAction(String menukey, String iconkey) {
        super(JDL.L("gui.menu." + menukey + ".name", menukey), JDTheme.II(iconkey, 16, 16));

        setMnemonic(JDL.L("gui.menu." + menukey + ".mnem", "-"));
        setAccelerator(JDL.L("gui.menu." + menukey + ".accel", "-"));

    }

    public void addMenuItem(MenuAction m) {
        if (id != CONTAINER) {
            JDLogger.getLogger().severe("I am not a Container MenuAction!!");
        }
        if (items == null) {
            items = new ArrayList<MenuAction>();
        }
        items.add(m);
        this.firePropertyChange(MENUITEMS, null, items);

    }

    public MenuAction get(int i) {
        if (items == null) { return null; }
        return items.get(i);
    }

    public int getType() {
        return id;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public int getSize() {
        if (items == null) { return 0; }
        return items.size();
    }

    public MenuAction setItems(ArrayList<MenuAction> createMenuitems) {
        items = createMenuitems;
        return this;
    }

    public MenuAction setPlugin(Plugin plugin) {
        this.plugin = plugin;
        return this;
    }

    public void actionPerformed(ActionEvent e) {
        if (getActionListener() == null) {
            JDLogger.getLogger().warning("no Actionlistener for " + getTitle());
            return;
        }
        getActionListener().actionPerformed(new ActionEvent(this, getActionID(), getTitle()));
    }

}