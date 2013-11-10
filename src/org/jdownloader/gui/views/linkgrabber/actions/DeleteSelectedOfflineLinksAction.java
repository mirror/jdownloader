package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;

import org.jdownloader.gui.translate._GUI;

public class DeleteSelectedOfflineLinksAction extends AbstractDeleteCrawledLinksAppAction {

    public DeleteSelectedOfflineLinksAction() {

        setName(_GUI._.DeleteOfflineAction_DeleteOfflineAction_object_());
        setIconKey("remove_offline");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        List<CrawledLink> nodesToDelete = new ArrayList<CrawledLink>();
        for (CrawledLink cl : getSelection().getChildren()) {

            DownloadLink dl = cl.getDownloadLink();
            if (dl != null) {
                AvailableStatus status = dl.getAvailableStatus();
                if (status != null) {
                    if (status == DownloadLink.AvailableStatus.FALSE) {
                        nodesToDelete.add(cl);
                    }
                }

            }
        }
        // deleteLinksRequest(new SelectionInfo<CrawledPackage, CrawledLink>(null, nodesToDelete, null, null, e,
        // DownloadsTableModel.getInstance().getTable()), _GUI._.DeleteSelectedOfflineLinksAction_actionPerformed());

    }

    @Override
    public boolean isEnabled() {
        if (super.isEnabled()) {
            for (CrawledLink cl : getSelection().getChildren()) {

                DownloadLink dl = cl.getDownloadLink();
                if (dl != null) {
                    AvailableStatus status = dl.getAvailableStatus();
                    if (status != null) {
                        if (status == DownloadLink.AvailableStatus.FALSE) { return true; }
                    }

                }

            }
        }
        return false;
    }

}
