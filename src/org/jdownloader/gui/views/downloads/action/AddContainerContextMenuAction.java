package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;

public class AddContainerContextMenuAction extends SelectionAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 1901008532686173167L;

    public AddContainerContextMenuAction(final SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setItemVisibleForEmptySelection(true);
        setItemVisibleForSelections(false);
        setName(_GUI._.AddLinksToLinkgrabberAction());
        setIconKey("add");
        setTooltipText(_GUI._.AddLinksAction_AddLinksAction_tt());

    }

    public void actionPerformed(ActionEvent e) {
        new AddLinksAction().actionPerformed(e);
    }

}