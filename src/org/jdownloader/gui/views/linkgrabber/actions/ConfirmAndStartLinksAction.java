package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.views.SelectionInfo;

public class ConfirmAndStartLinksAction extends AppAction {

    /**
     * 
     */

    private ConfirmAutoAction delegate;

    public ConfirmAndStartLinksAction(SelectionInfo<CrawledPackage, CrawledLink> selectionInfo) {

        delegate = new ConfirmAutoAction(selectionInfo);
        delegate.setAutostart(true);
        setName(delegate.getName());
        setIconKey(delegate.getIconKey());
        setTooltipText(delegate.getTooltipText());

    }

    public void actionPerformed(ActionEvent e) {
        delegate.actionPerformed(e);
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

}
