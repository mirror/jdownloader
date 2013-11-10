package org.jdownloader.gui.views.downloads.action;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.controlling.contextmenu.CustomizableSelectionAppAction;
import org.jdownloader.gui.KeyObserver;

public abstract class AbstractDeleteSelectionFromDownloadlistAction extends CustomizableSelectionAppAction<FilePackage, DownloadLink> {

    public AbstractDeleteSelectionFromDownloadlistAction() {
        super();
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1117751339878672160L;

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        if (hasSelection()) {

            if (KeyObserver.getInstance().isAltDown() || KeyObserver.getInstance().isAltGraphDown()) {
                setEnabled(false);
            } else {
                setEnabled(true);
            }
        } else {
            setEnabled(false);
        }
    }

}
