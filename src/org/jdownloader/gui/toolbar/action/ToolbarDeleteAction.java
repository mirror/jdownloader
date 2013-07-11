package org.jdownloader.gui.toolbar.action;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.action.DeleteSelectionAction;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.contextmenu.RemoveSelectionLinkgrabberAction;

public class ToolbarDeleteAction extends SelectionBasedToolbarAction {

    private List<AbstractNode> currentSelection;

    public ToolbarDeleteAction() {
        super();
        setIconKey("delete");
        setName(_GUI._.ToolbarDeleteAction_ToolbarDeleteAction_delete_Selection());

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (currentSelection != null) {
            if (getTable() != null && getTable() instanceof DownloadsTable) {
                SelectionInfo<FilePackage, DownloadLink> si = new SelectionInfo<FilePackage, DownloadLink>(null, currentSelection, null, null, e, (DownloadsTable) getTable());
                new DeleteSelectionAction(si).actionPerformed(e);
            } else {

                SelectionInfo<CrawledPackage, CrawledLink> si = new SelectionInfo<CrawledPackage, CrawledLink>(null, currentSelection, null, null, e, (LinkGrabberTable) getTable());
                new RemoveSelectionLinkgrabberAction(si).actionPerformed(e);

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
