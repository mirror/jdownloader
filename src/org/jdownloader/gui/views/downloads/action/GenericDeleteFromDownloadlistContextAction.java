package org.jdownloader.gui.views.downloads.action;

import org.jdownloader.controlling.contextmenu.TableContext;

public class GenericDeleteFromDownloadlistContextAction extends GenericDeleteFromDownloadlistAction {
    public GenericDeleteFromDownloadlistContextAction() {
        super();
        addContextSetup(new TableContext(false, true));

    }

    @Override
    protected void initContextDefaults() {
        setOnlySelectedItems(true);
    }

}
