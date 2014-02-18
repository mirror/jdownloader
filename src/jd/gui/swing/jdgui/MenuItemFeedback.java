package jd.gui.swing.jdgui;

import org.jdownloader.controlling.contextmenu.MenuItemData;

public class MenuItemFeedback extends DirectFeedback {

    private MenuItemData menuItemData;

    public MenuItemData getMenuItemData() {
        return menuItemData;
    }

    public void setMenuItemData(MenuItemData menuItemData) {
        this.menuItemData = menuItemData;
    }

    public MenuItemFeedback(boolean positive, MenuItemData menuItemData) {
        super(positive);
        this.menuItemData = menuItemData;
    }

}
