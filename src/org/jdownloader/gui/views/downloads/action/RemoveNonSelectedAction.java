package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class RemoveNonSelectedAction extends AppAction {

    /**
     * 
     */
    private static final long            serialVersionUID = 6855083561629297363L;
    private java.util.List<AbstractNode> selection;

    public RemoveNonSelectedAction(java.util.List<AbstractNode> selection) {
        this.selection = selection;

        setName(_GUI._.RemoveNonSelectedAction_RemoveNonSelectedAction_object_());
        setIconKey("ok");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;

        // LinkCollector.getInstance().getChildrenByFilter(filter)

        final SelectionInfo<FilePackage, DownloadLink> selectionInfo = new SelectionInfo<FilePackage, DownloadLink>(selection);
        final HashSet<DownloadLink> set = new HashSet<DownloadLink>();
        set.addAll(selectionInfo.getChildren());
        List<DownloadLink> nodesToDelete = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

            @Override
            public int returnMaxResults() {
                return 0;
            }

            @Override
            public boolean acceptNode(DownloadLink node) {
                return !set.contains(node);
            }
        });

        DownloadController.deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete), _GUI._.RemoveNonSelectedAction_actionPerformed());
    }

}
