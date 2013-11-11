package org.jdownloader.controlling.contextmenu;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

public abstract class CustomizableTableContextAppAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends CustomizableSelectionAppAction<PackageType, ChildrenType> {
    private TableContext tableContext;

    public CustomizableTableContextAppAction(boolean empty, boolean selection) {
        super();
        initTableContext(empty, selection);

    }

    public CustomizableTableContextAppAction() {
        super();
    }

    protected void initTableContext(boolean empty, boolean selection) {
        tableContext = new TableContext(empty, selection);
        addContextSetup(tableContext);
    }

    protected void removeTableContext() {
        removeContextSetup(tableContext);
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        requestTableContextUpdate();

    }

    protected void requestTableContextUpdate() {
        boolean has = !isEmptyContext();
        if (tableContext != null) {

            if (has) {
                if (tableContext.isItemVisibleForSelections()) {
                    setVisible(true);
                } else {
                    setVisible(false);
                    setEnabled(false);
                }

            } else {
                if (tableContext.isItemVisibleForEmptySelection()) {
                    setVisible(true);
                    setEnabled(true);
                } else {
                    setVisible(false);
                    setEnabled(false);
                }

            }
        } else if (!has) {
            setVisible(false);
            setEnabled(false);
        } else if (has) {
            setVisible(true);
        }
    }

    protected boolean isEmptyContext() {
        return !hasSelection(getSelection());
    }

    public TableContext getTableContext() {
        return tableContext;
    }

}
