package org.jdownloader.gui.views.downloads.action;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.gui.views.SelectionInfo;

public abstract class AbstractDeleteSelectionFromDownloadlistAction extends AbstractSelectionContextAction<FilePackage, DownloadLink> {

    public AbstractDeleteSelectionFromDownloadlistAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1117751339878672160L;

    @Override
    public boolean isEnabled() {
        SelectionInfo<FilePackage, DownloadLink> si = getSelection();
        if (si != null && si.getChildren().size() > 0) {
            if (si.isAltDown() || si.isAltGraphDown()) return false;
            return true;
        }
        return false;
    }
}
