package org.jdownloader.gui.toolbar.action;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.action.DeleteDisabledSelectedLinks;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.contextmenu.RemoveDisabledAction;

public class DeleteDisabledSelectedLinksToolbarAction extends SelectionBasedToolbarAction {

    private List<AbstractNode> currentSelection;

    public DeleteDisabledSelectedLinksToolbarAction() {
        super();
        setIconKey(IconKey.ICON_REMOVE_DISABLED);
        // DeleteDisabledSelectedLinks
        setName(_GUI._.DeleteDisabledLinks_DeleteDisabledLinks_object_());

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (currentSelection != null) {
            if (getTable() != null && getTable() instanceof DownloadsTable) {
                SelectionInfo<FilePackage, DownloadLink> si = new SelectionInfo<FilePackage, DownloadLink>(null, currentSelection, null, null, e, (DownloadsTable) getTable());
                new DeleteDisabledSelectedLinks(si).actionPerformed(e);
            } else {

                SelectionInfo<CrawledPackage, CrawledLink> si = new SelectionInfo<CrawledPackage, CrawledLink>(null, currentSelection, null, null, e, (LinkGrabberTable) getTable());
                new RemoveDisabledAction(si).actionPerformed(e);

            }

        }

    }

    @Override
    protected String createTooltip() {
        return getName();
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    protected void onSelectionUpdate(List<AbstractNode> list) {
        currentSelection = list;
        if (list == null || list.size() == 0) {
            setEnabled(false);
        } else {
            setEnabled(true);
        }
    }

}
