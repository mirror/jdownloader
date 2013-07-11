package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.plugins.DownloadLink;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class OpenUrlAction extends SelectionAppAction<CrawledPackage, CrawledLink> {

    /**
     * 
     */
    private static final long serialVersionUID = 1299668511027392364L;
    private CrawledLink       link;

    @Override
    public void setSelection(SelectionInfo<CrawledPackage, CrawledLink> selection) {
        super.setSelection(selection);
        if (getSelection() != null) {
            this.link = getSelection().getContextLink();
        }
    }

    public OpenUrlAction(SelectionInfo<CrawledPackage, CrawledLink> link) {
        super(link);
        setName(_GUI._.OpenUrlAction_OpenUrlAction_());
        setIconKey("browse");

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        CrossSystem.openURL(link.getDownloadLink().getBrowserUrl());
    }

    @Override
    public boolean isEnabled() {
        return CrossSystem.isOpenBrowserSupported() && link != null && link.getDownloadLink() != null && (link.getDownloadLink().getLinkType() == DownloadLink.LINKTYPE_NORMAL || link.getDownloadLink().gotBrowserUrl());
    }

}
