package jd.config;

import java.awt.event.ActionListener;
import java.util.ArrayList;

import jd.plugins.Plugin;

public class MenuItem extends Property {
    public static final int CONTAINER = 0;
    public static final int NORMAL = 1;
    public static final int SEPARATOR = 3;
    /**
     * 
     */
    private static final long serialVersionUID = 9205555751462125274L;
    public static final int TOGGLE = 2;
    private int actionID;
    private ActionListener actionListener;
    private boolean enabled=true;
    private int id = NORMAL;
    private ArrayList<MenuItem> items;
    private Plugin plugin;
    private boolean selected;
    private String title;
    public MenuItem(int id) {
        this(id, null, -1);
    }
    public MenuItem(int id, String title, int actionID) {
        this.id = id;
        this.actionID = actionID;
        this.title = title;
    }

    public MenuItem(String title, int actionID) {
        this(NORMAL, title, actionID);
    }

    public void addMenuItem(MenuItem m) {
        if (id != CONTAINER) {
            logger.severe("I am not a Container MenuItem!!");
        }
        if (this.items == null) this.items = new ArrayList<MenuItem>();
        items.add(m);

    }

    public MenuItem get(int i) {
        if (items == null) return null;
        return items.get(i);
    }

    public int getActionID() {

        return this.actionID;
    }

    public ActionListener getActionListener() {
        return actionListener;
    }

    public int getID() {

        return id;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public int getSize() {

        if (items == null) return 0;
        return items.size();
    }

    public String getTitle() {
        return title;
    }

    public boolean isEnabled() {
      
        return enabled;
    }

    public boolean isSelected() {

        return selected;
    }

    public MenuItem setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
        return this;
    }

    public MenuItem setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public MenuItem setItems(ArrayList<MenuItem> createMenuitems) {
        this.items = createMenuitems;
        return this;

    }

    public MenuItem setPlugin(Plugin plugin) {
        this.plugin = plugin;
        return this;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public MenuItem setTitle(String title) {
        this.title = title;
        return this;
    }

}