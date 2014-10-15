package org.jdownloader.controlling.contextmenu;

import org.jdownloader.translate._JDT;

public class TableContext implements ActionContext {
    public static final String ITEM_VISIBLE_FOR_SELECTIONS      = "itemVisibleForSelections";
    public static final String ITEM_VISIBLE_FOR_EMPTY_SELECTION = "itemVisibleForEmptySelection";

    private boolean            itemVisibleForSelections         = true;
    private boolean            itemVisibleForEmptySelection     = false;

    public static String getTranslationItemVisibleForSelections() {
        return _JDT._.TableContext_getTranslationItemVisibleForSelections_();
    }

    public static String getTranslationItemVisibleForEmptySelection() {
        return _JDT._.TableContext_getTranslationItemVisibleForEmptySelection();
    }

    @Customizer(link = "#getTranslationItemVisibleForSelections")
    public boolean isItemVisibleForSelections() {
        return itemVisibleForSelections;
    }

    public void setItemVisibleForSelections(boolean clearListAfterConfirm) {
        this.itemVisibleForSelections = clearListAfterConfirm;
    }

    @Customizer(link = "#getTranslationItemVisibleForEmptySelection")
    public boolean isItemVisibleForEmptySelection() {
        return itemVisibleForEmptySelection;
    }

    public void setItemVisibleForEmptySelection(boolean clearListAfterConfirm) {
        this.itemVisibleForEmptySelection = clearListAfterConfirm;
    }

    public TableContext(boolean setItemVisibleForEmptySelection, boolean setItemVisibleForSelections) {

        setItemVisibleForEmptySelection(setItemVisibleForEmptySelection);
        setItemVisibleForSelections(setItemVisibleForSelections);
    }

}
