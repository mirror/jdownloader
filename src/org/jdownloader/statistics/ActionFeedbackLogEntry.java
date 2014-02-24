package org.jdownloader.statistics;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.jdownloader.controlling.contextmenu.MenuItemData;

public class ActionFeedbackLogEntry extends AbstractFeedbackLogEntry implements Storable {
    private MenuItemData menuItemData;

    public MenuItemData getMenuItemData() {
        return menuItemData;
    }

    public void setMenuItemData(MenuItemData menuItemData) {
        this.menuItemData = menuItemData;
    }

    protected ActionFeedbackLogEntry(/* storable */) {
        super();
    }

    public ActionFeedbackLogEntry(boolean positive, MenuItemData menuItemData) {
        super(positive);
        this.menuItemData = menuItemData;
    }

    @Override
    public String toString() {
        return JSonStorage.toString(this);
    }

    private int counter;

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public int getCounter() {
        return counter;
    }
}
