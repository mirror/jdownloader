package org.jdownloader.controlling.contextmenu;

public class TableContext implements ActionContext {
    public static final String ITEM_VISIBLE_FOR_SELECTIONS      = "itemVisibleForSelections";
    public static final String ITEM_VISIBLE_FOR_EMPTY_SELECTION = "itemVisibleForEmptySelection";

    private boolean            itemVisibleForSelections         = true;
    private boolean            itemVisibleForEmptySelection     = false;

    @Customizer(name = "Item is visible for selected Links")
    public boolean isItemVisibleForSelections() {
        return itemVisibleForSelections;
    }

    @Customizer(name = "Item is visible for selected Links")
    public void setItemVisibleForSelections(boolean clearListAfterConfirm) {
        this.itemVisibleForSelections = clearListAfterConfirm;
    }

    @Customizer(name = "Item is visible for empty selections")
    public boolean isItemVisibleForEmptySelection() {
        return itemVisibleForEmptySelection;
    }

    @Customizer(name = "Item is visible for empty selections")
    public void setItemVisibleForEmptySelection(boolean clearListAfterConfirm) {
        this.itemVisibleForEmptySelection = clearListAfterConfirm;
    }

    public TableContext(boolean setItemVisibleForEmptySelection, boolean setItemVisibleForSelections) {

        setItemVisibleForEmptySelection(setItemVisibleForEmptySelection);
        setItemVisibleForSelections(setItemVisibleForSelections);
    }

}
