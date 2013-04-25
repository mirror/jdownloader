package org.jdownloader.gui.views.downloads.contextmenumanager;

import java.util.ArrayList;
import java.util.HashMap;

import org.appwork.storage.Storable;

public class MenuContainerRoot extends MenuContainer implements Storable {
    private int version;

    public MenuContainerRoot(/* Storable */) {

    }

    public void setSource(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void validate() {
        validate(this);
    }

    /**
     * Validates the items, and removes invalid entries. replaces generic entries with an actual class instance
     * 
     * @param menuContainerRoot
     */
    private void validate(MenuItemData menuContainerRoot) {
        if (menuContainerRoot.getItems() != null) {

            ArrayList<MenuItemData> itemsToRemove = null;
            HashMap<MenuItemData, MenuItemData> replaceMap = new HashMap<MenuItemData, MenuItemData>();
            for (MenuItemData mid : menuContainerRoot.getItems()) {
                try {
                    MenuItemData lr = mid.lazyReal();
                    if (lr != mid) {
                        // let's replace
                        replaceMap.put(mid, lr);

                    }
                    if (lr.getActionData() != null) {
                        lr.getActionData()._getClazz();

                    }

                } catch (Exception e) {
                    if (itemsToRemove == null) itemsToRemove = new ArrayList<MenuItemData>();
                    itemsToRemove.add(mid);
                    e.printStackTrace();

                }

            }
            if (itemsToRemove != null) {
                menuContainerRoot.getItems().removeAll(itemsToRemove);
            }

            for (int i = 0; i < menuContainerRoot.getItems().size(); i++) {
                MenuItemData mid = menuContainerRoot.getItems().get(i);
                MenuItemData rep = replaceMap.remove(mid);
                if (rep != null) {
                    menuContainerRoot.getItems().set(i, rep);
                    mid = rep;
                }
                validate(mid);

            }
        }

    }

}
