package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.IOEQ;
import jd.controlling.linkchecker.LinkChecker;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class CheckStatusAction extends AppAction {

    private static final long             serialVersionUID = 6821943398259956694L;

    private final ArrayList<DownloadLink> links;

    public CheckStatusAction(ArrayList<DownloadLink> links) {
        this.links = links;
        setIconKey("ok");
        setName(_GUI._.gui_table_contextmenu_check());

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        IOEQ.add(new Runnable() {

            public void run() {
                final LinkChecker<DownloadLink> lc = new LinkChecker<DownloadLink>(true);
                for (DownloadLink l : links) {
                    l.setAvailableStatus(AvailableStatus.UNCHECKED);
                }
                lc.check(links);
            }

        }, true);
    }

    @Override
    public boolean isEnabled() {
        return links != null && links.size() > 0;
    }

}