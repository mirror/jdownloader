package jd.gui.swing.jdgui.menu;

import java.util.ArrayList;

import jd.gui.swing.jdgui.actions.ToolBarAction;

public class MenuAction extends ToolBarAction {

    private ArrayList<MenuAction> items;

    public MenuAction(String pluginID, int i) {
        super(pluginID, i);
    }

    public MenuAction(Types separator) {
       super();
        this.setType(separator);
    }

    @Override
    public void init() {
        // TODO Auto-generated method stub

    }

    @Override
    public void initDefaults() {
        // TODO Auto-generated method stub

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
        // TODO Auto-generated method stub
        return getItems().size();
    }

    public MenuAction get(int i) {
        // TODO Auto-generated method stub
        return getItems().get(i);
    }

    public void addMenuItem(MenuAction m) {
        getItems().add(m);
        this.setType(Types.CONTAINER);

    }

}
