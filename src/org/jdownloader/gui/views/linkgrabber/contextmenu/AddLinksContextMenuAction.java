package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.jdgui.menu.actions.AddContainerAction;

import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class AddLinksContextMenuAction extends AbstractSelectionContextAction<CrawledPackage, CrawledLink> {

    private static final long serialVersionUID = 1901008532686173167L;

    public AddLinksContextMenuAction(final SelectionInfo<CrawledPackage, CrawledLink> si) {
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