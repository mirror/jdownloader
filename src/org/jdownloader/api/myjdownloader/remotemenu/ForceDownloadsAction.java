package org.jdownloader.api.myjdownloader.remotemenu;

import java.awt.event.ActionEvent;

import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.action.ForceDownloadAction;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class ForceDownloadsAction extends AbstractMyJDSelectionAction {
    @Override
    public String getID() {
        return "forcedls";
    }

    private ForceDownloadAction delegate;

    public ForceDownloadsAction() {
        delegate = new ForceDownloadAction() {
            @Override
            protected SelectionInfo<FilePackage, DownloadLink> getSelection() {
                return (SelectionInfo<FilePackage, DownloadLink>) ForceDownloadsAction.this.getSelection();
            }

        };
        setIconKey(delegate.getIconKey());
        setName(delegate.getName());

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        delegate.actionPerformed(e);
    }

}
