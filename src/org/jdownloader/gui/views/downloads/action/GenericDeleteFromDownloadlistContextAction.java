package org.jdownloader.gui.views.downloads.action;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.controlling.contextmenu.TableContext;
import org.jdownloader.gui.views.SelectionInfo;

public class GenericDeleteFromDownloadlistContextAction extends GenericDeleteFromDownloadlistAction {
    private TableContext tableContext;

    public GenericDeleteFromDownloadlistContextAction() {
        super();
        addContextSetup(tableContext = new TableContext(false, true));

    }

    @Override
    protected void updateListeners() {

    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        if (tableContext != null) {
            SelectionInfo<FilePackage, DownloadLink> actualSelection = getTable().getSelectionInfo();
            boolean has = actualSelection != null && !actualSelection.isEmpty();
            if (tableContext != null) {

                if (has) {
                    if (tableContext.isItemVisibleForSelections()) {
                        setVisible(true);
                    } else {
                        setVisible(false);
                        setEnabled(false);
                    }

                } else {
                    if (tableContext.isItemVisibleForEmptySelection() && !isOnlySelectedItems()) {
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

    }

    @Override
    protected void initContextDefaults() {
        setOnlySelectedItems(true);
    }

}
