package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AbstractContextMenuAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;

public class AddLinksContextMenuAction extends AbstractContextMenuAction {

    private static final long serialVersionUID = 1901008532686173167L;

    public AddLinksContextMenuAction() {
        super();
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