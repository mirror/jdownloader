package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class DeleteSelectedLinks extends AbstractDeleteCrawledLinksAppAction {

    public DeleteSelectedLinks(SelectionInfo<CrawledPackage, CrawledLink> si) {
        super(si);
        setName(_GUI._.DeleteAllAction_DeleteAllAction_object_());
        setIconKey("remove");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        deleteLinksRequest(getSelection(), _GUI._.RemoveSelectionAction_actionPerformed_());
    }
}
