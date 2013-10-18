package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;

public class AddContainerContextMenuAction extends AbstractSelectionContextAction<CrawledPackage, CrawledLink> {

    private static final long serialVersionUID = 1901008532686173167L;

    public AddContainerContextMenuAction(final SelectionInfo<CrawledPackage, CrawledLink> si) {
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