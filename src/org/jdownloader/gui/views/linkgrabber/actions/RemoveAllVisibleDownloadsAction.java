package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class RemoveAllVisibleDownloadsAction extends AbstractDeleteCrawledLinksAppAction {
    /**
     * 
     */
    private static final long serialVersionUID = 841782078416257540L;

    public RemoveAllVisibleDownloadsAction(SelectionInfo<CrawledPackage, CrawledLink> si) {
        super(si);
        setName(_GUI._.RemoveAllAction_RemoveAllAction_object());
        setIconKey("clear");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        deleteLinksRequest(getSelection(), _GUI._.RemoveSelectionAction_actionPerformed_());
    }

}
