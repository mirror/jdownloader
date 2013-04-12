package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class RemoveOfflineAction extends AppAction {

    /**
     * 
     */
    private static final long serialVersionUID = -6341297356888158708L;

    public RemoveOfflineAction() {
        setName(_GUI._.RemoveOfflineAction_RemoveOfflineAction_object_());
        setIconKey("remove_offline");
    }

    public void actionPerformed(ActionEvent e) {

        final List<DownloadLink> nodesToDelete = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

            @Override
            public int returnMaxResults() {
                return 0;
            }

            @Override
            public boolean acceptNode(DownloadLink node) {
                if (node.getLinkStatus().hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) return true;
                return node.getAvailableStatus() == AvailableStatus.FALSE;
            }
        });

        DownloadController.deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete), _GUI._.RemoveOfflineAction_actionPerformed());
    }

}
