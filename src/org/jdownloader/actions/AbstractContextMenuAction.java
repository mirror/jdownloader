package org.jdownloader.actions;

import org.jdownloader.controlling.contextmenu.Customizer;


public abstract class AbstractContextMenuAction extends AppAction implements CachableInterface {

    public static final String                         ITEM_VISIBLE_FOR_SELECTIONS      = "itemVisibleForSelections";
    public static final String                         ITEM_VISIBLE_FOR_EMPTY_SELECTION = "itemVisibleForEmptySelection";
    private boolean itemVisibleForSelections = true;
    private boolean itemVisibleForEmptySelection = false;

    @Customizer(name = "Item is visible for selected Links")
    public boolean isItemVisibleForSelections() {
        return itemVisibleForSelections;
    }

    @Customizer(name = "Item is visible for selected Links")
    public void setItemVisibleForSelections(boolean clearListAfterConfirm) {
        this.itemVisibleForSelections = clearListAfterConfirm;
    }

    @Override
    public void setData(String data) {
    }

    @Customizer(name = "Item is visible for empty selections")
    public boolean isItemVisibleForEmptySelection() {
        return itemVisibleForEmptySelection;
    }

    @Customizer(name = "Item is visible for empty selections")
    public void setItemVisibleForEmptySelection(boolean clearListAfterConfirm) {
        this.itemVisibleForEmptySelection = clearListAfterConfirm;
    }

}
