package org.jdownloader.actions;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.views.SelectionInfo;

public abstract class SelectionAppAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AppAction implements CachableInterface {
    /**
     * 
     */
    private static final long                          serialVersionUID = 1L;
    protected SelectionInfo<PackageType, ChildrenType> selection;
    private String                                     data;

    public String getData() {
        return data;
    }

    private boolean itemVisibleForSelections = true;

    @Customizer(name = "Item is visible for selected Links")
    public boolean isItemVisibleForSelections() {
        return itemVisibleForSelections;
    }

    @Customizer(name = "Item is visible for selected Links")
    public void setItemVisibleForSelections(boolean clearListAfterConfirm) {
        this.itemVisibleForSelections = clearListAfterConfirm;
    }

    private boolean itemVisibleForEmptySelection = false;

    @Customizer(name = "Item is visible for empty selections")
    public boolean isItemVisibleForEmptySelection() {
        return itemVisibleForEmptySelection;
    }

    @Customizer(name = "Item is visible for empty selections")
    public void setItemVisibleForEmptySelection(boolean clearListAfterConfirm) {
        this.itemVisibleForEmptySelection = clearListAfterConfirm;
    }

    public void setData(String data) {
        this.data = data;
    }

    public SelectionAppAction(SelectionInfo<PackageType, ChildrenType> si) {
        setSelection(si);
    }

    public SelectionInfo<PackageType, ChildrenType> getSelection() {
        return selection;
    }

    public boolean hasSelection() {
        return getSelection() != null && !getSelection().isEmpty();
    }

    public void setSelection(SelectionInfo<PackageType, ChildrenType> selection) {
        this.selection = selection;
        if (!isItemVisibleForEmptySelection() && !hasSelection()) {
            setVisible(false);
            setEnabled(false);
        } else if (!isItemVisibleForSelections() && hasSelection()) {
            setVisible(false);
            setEnabled(false);
        } else {
            setVisible(true);
            setEnabled(true);
        }

    }

}
