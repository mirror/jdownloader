package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class DeleteSelectionAction extends AbstractDeleteCrawledLinksAppAction {

    private static final long serialVersionUID = -5721724901676405104L;

    public DeleteSelectionAction(SelectionInfo<CrawledPackage, CrawledLink> si) {
        super(si);
        setIconKey("delete");
        setName(_GUI._.gui_table_contextmenu_deletelist2());
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        deleteLinksRequest(getSelection(), _GUI._.RemoveSelectionAction_actionPerformed_());
    }

}