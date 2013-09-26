package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.menu.actions.AddContainerAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class AddLinksContextMenuAction extends SelectionAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 1901008532686173167L;

    public AddLinksContextMenuAction(final SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setItemVisibleForEmptySelection(true);
        setItemVisibleForSelections(false);
        setName(_GUI._.action_addcontainer());
        setTooltipText(_GUI._.action_addcontainer_tooltip());
        setIconKey("load");

    }

    public void actionPerformed(ActionEvent e) {
        new AddContainerAction().actionPerformed(e);
    }

}