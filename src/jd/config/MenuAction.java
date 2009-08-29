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
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.plugins.Plugin;

public class MenuAction extends ToolBarAction {

    private static final long serialVersionUID = 9205555751462125274L;

    private static final String MENUITEMS = "MENUITEMS";

    private static final String PLUGIN = "PLUGIN";

    private static final ArrayList<MenuAction> EMPTY_ITEMS = new ArrayList<MenuAction>();

    /**
     * this is just a delegate to adjust return Type
     * 
     * @see JDAction#setActionListener(ActionListener)
     */
    public MenuAction setActionListener(ActionListener actionListener) {
        return (MenuAction) super.setActionListener(actionListener);
    }

    public MenuAction(String menukey, String iconkey) {
        super(menukey, iconkey);

        this.setToolTipText(this.getTitle());

    }

    public MenuAction(Types container, String name, int actionID) {
        super(name);
        this.type = container;
        this.setToolTipText(this.getTitle());
        this.setId(name);
        this.putValue(VISIBLE, SubConfiguration.getConfig("Toolbar").getBooleanProperty("VISIBLE_" + this.getID(), true));
        this.setActionID(actionID);
        initDefaults();
        ActionController.register(this);
    }

    public MenuAction(Types separator) {
        super("-");
        this.setId("separator");
        this.type = separator;
    }

    public MenuAction(String name, int i) {
        super(name);
        this.setId(name);
        this.setActionID(i);

    }

    @SuppressWarnings("unchecked")
    public void addMenuItem(MenuAction m) {

        ArrayList<MenuAction> items = null;
        try {
            items = (ArrayList<MenuAction>) getValue(MENUITEMS);
        } catch (Exception e) {
        }

        if (items == null) {
            items = new ArrayList<MenuAction>();
        }
        items.add(m);
        this.putValue(MENUITEMS, items);

    }

    @SuppressWarnings("unchecked")
    private ArrayList<MenuAction> getItems() {
        if (getValue(MENUITEMS) == null) { return EMPTY_ITEMS; }
        return ((ArrayList<MenuAction>) getValue(MENUITEMS));
    }

    @SuppressWarnings("unchecked")
    public MenuAction get(int i) {
        if (getValue(MENUITEMS) == null) { return null; }
        return ((ArrayList<MenuAction>) getValue(MENUITEMS)).get(i);
    }

    public Plugin getPlugin() {
        if (getValue(PLUGIN) == null) { return null; }
        return (Plugin) getValue(PLUGIN);
    }

    public int getSize() {
        if (getItems() == null) { return 0; }
        return getItems().size();
    }

    public MenuAction setItems(ArrayList<MenuAction> createMenuitems) {
        putValue(MENUITEMS, createMenuitems);
        return this;
    }

    public MenuAction setPlugin(Plugin plugin) {
        putValue(PLUGIN, plugin);
        return this;
    }

    public void actionPerformed(ActionEvent e) {
        if (getActionListener() == null) {
            JDLogger.getLogger().warning("no Actionlistener for " + getTitle());
            return;
        }
        getActionListener().actionPerformed(new ActionEvent(this, getActionID(), getTitle()));
    }

    @Override
    public void init() {
        // TODO Auto-generated method stub

    }

    @Override
    public void initDefaults() {
        // TODO Auto-generated method stub

    }

}