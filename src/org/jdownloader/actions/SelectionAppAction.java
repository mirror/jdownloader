package org.jdownloader.actions;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.jdownloader.gui.views.SelectionInfo;

public abstract class SelectionAppAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AppAction implements CachableInterface {
    /**
     * 
     */
    private static final long                        serialVersionUID = 1L;
    private SelectionInfo<PackageType, ChildrenType> selection;
    private String                                   data;

    public String getData() {
        return data;
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

    public boolean isSuperEnabled() {
        return super.isEnabled();
    }

    @Override
    public boolean isEnabled() {

        return hasSelection() && super.isEnabled();
    }

    public boolean hasSelection() {
        return getSelection() != null && !getSelection().isEmpty();
    }

    public void setSelection(SelectionInfo<PackageType, ChildrenType> selection) {
        this.selection = selection;
    }

}
