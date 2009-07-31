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
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import jd.controlling.JDLogger;
import jd.plugins.Plugin;

public class MenuItem extends AbstractAction {
    public static final int CONTAINER = 0;
    public static final int NORMAL = 1;
    public static final int SEPARATOR = 3;
    /**
     * 
     */
    private static final long serialVersionUID = 9205555751462125274L;
    public static final int TOGGLE = 2;
    private static final String MENUITEMS = "MENUITEMS";
    private int actionID;
    private ActionListener actionListener;

    private int id = NORMAL;
    private ArrayList<MenuItem> items;
    private Plugin plugin;

    private Logger logger;
    private Property properties;

    public MenuItem(int id) {
        this(id, null, -1);
    }

    public MenuItem(int id, String title, int actionID) {
        this.id = id;
        this.actionID = actionID;

        this.putValue(MenuItem.NAME, title);
        logger = JDLogger.getLogger();
    }

    public MenuItem(String title, int actionID) {
        this(NORMAL, title, actionID);
    }

    public void setAccelerator(String accelerator) {
        this.putValue(MenuItem.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelerator));
    }

    public String getAccelerator() {
        KeyStroke stroke = ((KeyStroke) getValue(ACCELERATOR_KEY));
        if (stroke == null) return null;
        return stroke.getKeyChar() + "";
    }

    public void addMenuItem(MenuItem m) {
        if (id != CONTAINER) {
            logger.severe("I am not a Container MenuItem!!");
        }
        if (items == null) {
            items = new ArrayList<MenuItem>();
        }
        items.add(m);
        this.firePropertyChange(MENUITEMS, null, items);

    }

    public MenuItem get(int i) {
        if (items == null) { return null; }
        return items.get(i);
    }

    public int getActionID() {

        return actionID;
    }

    public ActionListener getActionListener() {
        return actionListener;
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

    public String getTitle() {
        return (String) getValue(NAME);
    }

    public boolean isSelected() {
        Object value = getValue(SELECTED_KEY);
        if (value == null) {
            putValue(SELECTED_KEY, false);
            return false;
        }
        return (Boolean) value;

    }

    public MenuItem setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;

        return this;
    }

    public MenuItem setItems(ArrayList<MenuItem> createMenuitems) {
        items = createMenuitems;
        return this;

    }

    public MenuItem setPlugin(Plugin plugin) {
        this.plugin = plugin;
        return this;
    }

    public void setSelected(boolean selected) {
        putValue(SELECTED_KEY, selected);
    }

    public MenuItem setTitle(String title) {
        putValue(NAME, title);
        return this;
    }

    public MenuItem setIcon(ImageIcon ii) {
        putValue(SMALL_ICON, ii);
        return this;

    }

    public ImageIcon getIcon() {
        return (ImageIcon) getValue(SMALL_ICON);
    }

    public void actionPerformed(ActionEvent e) {
        if (getActionListener() == null) {
            JDLogger.getLogger().warning("no Actionlistener for " + getTitle());
            return;
        }
        getActionListener().actionPerformed(new ActionEvent(this, getActionID(), getTitle()));

    }

    public void setProperty(String string, Object value) {
        if (properties == null) properties = new Property();
        this.firePropertyChange(string, getProperty(string), value);
        properties.setProperty(string, value);

    }

    public Object getProperty(String string) {
        if (properties == null) properties = new Property();
        return properties.getProperty(string);
    }

}