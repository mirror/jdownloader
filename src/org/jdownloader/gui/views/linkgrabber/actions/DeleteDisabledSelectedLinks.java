package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class DeleteDisabledSelectedLinks extends AbstractDeleteCrawledLinksAppAction {

    /**
     * 
     */
    private static final long serialVersionUID = 5081124258090555549L;

    public DeleteDisabledSelectedLinks(SelectionInfo<CrawledPackage, CrawledLink> si) {
        super(si);
        setName(_GUI._.DeleteDisabledLinks_DeleteDisabledLinks_object_());
        setIconKey("remove_disabled");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        List<CrawledLink> nodesToDelete = new ArrayList<CrawledLink>();
        for (CrawledLink dl : getSelection().getChildren()) {
            if (!dl.isEnabled()) {
                nodesToDelete.add(dl);
            }
        }

        // deleteLinksRequest(new SelectionInfo<CrawledPackage, CrawledLink>(null, nodesToDelete, null, null, e,
        // DownloadsTableModel.getInstance().getTable()), _GUI._.DeleteDisabledLinksFromListAndDiskAction_actionPerformed_object_());
    }

    @Override
    public boolean isEnabled() {
        if (super.isEnabled()) {
            for (CrawledLink dl : getSelection().getChildren()) {
                if (!dl.isEnabled()) return true;
            }
        }
        return false;
    }

}
