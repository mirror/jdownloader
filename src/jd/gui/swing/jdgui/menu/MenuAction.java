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
