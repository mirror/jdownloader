package jd.config;

import java.awt.event.ActionListener;
import java.util.ArrayList;

import jd.plugins.Plugin;

public class MenuItem extends Property {
    /**
     * 
     */
    private static final long serialVersionUID = 9205555751462125274L;
    public static final int CONTAINER = 0;
    public static final int NORMAL = 1;
    public static final int TOGGLE = 2;
    private int id = NORMAL;
    private ArrayList<MenuItem> items;
    private String title;
    private ActionListener actionListener;
    private Plugin plugin;
    private boolean selected;
    private int actionID;
    private boolean enabled=true;

    public MenuItem(String title, int actionID) {
        this(NORMAL, title, actionID);
    }

    public MenuItem(int id, String title, int actionID) {
        this.id = id;
        this.actionID = actionID;
        this.title = title;
    }

    public void addMenuItem(MenuItem m) {
        if (id != CONTAINER) {
            logger.severe("I am not a Container MenuItem!!");
        }
        if (this.items == null) this.items = new ArrayList<MenuItem>();
        items.add(m);

    }

    public ActionListener getActionListener() {
        return actionListener;
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public int getID() {

        return id;
    }

    public boolean isSelected() {

        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public MenuItem get(int i) {
        if (items == null) return null;
        return items.get(i);
    }

    public int getSize() {

        if (items == null) return 0;
        return items.size();
    }

    public void setItems(ArrayList<MenuItem> createMenuitems) {
        this.items = createMenuitems;

    }

    public int getActionID() {

        return this.actionID;
    }

    public boolean isEnabled() {
      
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}