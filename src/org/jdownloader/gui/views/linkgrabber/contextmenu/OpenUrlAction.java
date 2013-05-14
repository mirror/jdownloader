package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.plugins.DownloadLink;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class OpenUrlAction extends AppAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1299668511027392364L;
    private CrawledLink       link;

    public OpenUrlAction(SelectionInfo<CrawledPackage, CrawledLink> link) {

        setName(_GUI._.OpenUrlAction_OpenUrlAction_());
        setIconKey("browse");
        if (link != null) {
            this.link = link.getContextLink();
        }
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
