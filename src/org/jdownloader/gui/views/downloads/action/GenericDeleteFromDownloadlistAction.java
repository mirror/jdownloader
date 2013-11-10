package org.jdownloader.gui.views.downloads.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;
import jd.plugins.LinkStatusProperty;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.exttable.ExtTableEvent;
import org.appwork.swing.exttable.ExtTableListener;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.plugins.FinalLinkState;

public class GenericDeleteFromDownloadlistAction extends CustomizableAppAction implements ExtTableListener, ActionContext, DownloadControllerListener {

    /**
     * 
     */
    private static final long                        serialVersionUID  = 1L;
    public static final String                       DELETE_ALL        = "deleteAll";
    public static final String                       DELETE_DISABLED   = "deleteDisabled";
    public static final String                       DELETE_FAILED     = "deleteFailed";
    public static final String                       DELETE_FINISHED   = "deleteFinished";
    public static final String                       DELETE_OFFLINE    = "deleteOffline";

    private boolean                                  deleteDisabled    = false;
    private boolean                                  onlySelectedItems = false;

    private boolean                                  deleteAll         = false;

    private boolean                                  ignoreFiltered    = true;

    private boolean                                  deleteFailed      = false;

    private boolean                                  deleteFinished    = false;

    private boolean                                  deleteOffline     = false;

    private SelectionInfo<FilePackage, DownloadLink> selection;
    private DelayedRunnable                          delayer;

    @Customizer(name = "Include All Links")
    public boolean isDeleteAll() {
        return deleteAll;
    }

    @Customizer(name = "Include disabled Links")
    public boolean isDeleteDisabled() {
        return deleteDisabled;
    }

    @Customizer(name = "Include failed")
    public boolean isDeleteFailed() {
        return deleteFailed;
    }

    @Customizer(name = "Include finished Links")
    public boolean isDeleteFinished() {
        return deleteFinished;
    }

    @Customizer(name = "Include Offline Links")
    public boolean isDeleteOffline() {
        return deleteOffline;
    }

    @Customizer(name = "Exclude filtered Links")
    public boolean isIgnoreFiltered() {
        return ignoreFiltered;
    }

    @Customizer(name = "Only Selected Links")
    public boolean isOnlySelectedItems() {
        return onlySelectedItems;
    }

    public void setDeleteAll(final boolean deleteIdle) {
        GenericDeleteFromDownloadlistAction.this.deleteAll = deleteIdle;
        updateName();
        delayer.resetAndStart();
    }

    public void setDeleteDisabled(final boolean deleteDisabled) {
        GenericDeleteFromDownloadlistAction.this.deleteDisabled = deleteDisabled;
        updateName();
        delayer.resetAndStart();
    }

    public void setDeleteFailed(final boolean deleteFailed) {
        GenericDeleteFromDownloadlistAction.this.deleteFailed = deleteFailed;
        updateName();
        delayer.resetAndStart();
    }

    public void setDeleteFinished(final boolean deleteFinished) {
        GenericDeleteFromDownloadlistAction.this.deleteFinished = deleteFinished;
        updateName();
        delayer.resetAndStart();
    }

    public void setDeleteOffline(final boolean deleteOffline) {
        GenericDeleteFromDownloadlistAction.this.deleteOffline = deleteOffline;
        updateName();
        delayer.resetAndStart();
    }

    public void setIgnoreFiltered(final boolean ignoreFiltered) {
        GenericDeleteFromDownloadlistAction.this.ignoreFiltered = ignoreFiltered;
        updateName();
        delayer.resetAndStart();
    }

    public void setOnlySelectedItems(final boolean onlySelectedItems) {
        GenericDeleteFromDownloadlistAction.this.onlySelectedItems = onlySelectedItems;
        if (onlySelectedItems) {
            getTable().getEventSender().addListener(GenericDeleteFromDownloadlistAction.this, true);
            DownloadController.getInstance().getEventSender().removeListener(GenericDeleteFromDownloadlistAction.this);
        } else {
            getTable().getEventSender().removeListener(GenericDeleteFromDownloadlistAction.this);
            DownloadController.getInstance().getEventSender().addListener(GenericDeleteFromDownloadlistAction.this, true);
        }
        updateName();
        if (delayer != null) delayer.resetAndStart();
    }

    public GenericDeleteFromDownloadlistAction() {
        super();

        setIconKey(IconKey.ICON_DELETE);
        delayer = new DelayedRunnable(200, 500) {

            @Override
            public void delayedrun() {
                update();
            }
        };
        DownloadController.getInstance().getEventSender().addListener(this, true);
        update();

    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        update();
    }

    private DownloadLink lastLink;

    protected void update() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (isOnlySelectedItems()) {
                    selection = (getTable().getSelectionInfo(true, true));

                } else {
                    if (isIgnoreFiltered()) {
                        selection = getTable().getSelectionInfo(false, true);

                    } else {

                        selection = getTable().getSelectionInfo(false, false);
                    }

                }
                // we remember the last link and try it first
                if (lastLink != null && selection.contains(lastLink)) {
                    if (checkLink(lastLink)) {
                        //
                        setEnabled(true);
                        return;
                    }
                }
                if (isDeleteAll() && !selection.isEmpty()) {
                    setEnabled(true);
                    return;

                }
                for (DownloadLink link : selection.getChildren()) {
                    if (checkLink(link)) {
                        lastLink = link;
                        setEnabled(true);
                        return;
                    }
                }
                setEnabled(false);
            }

            /**
             * @param link
             * @return
             */

        };

    }

    @Override
    public void setEnabled(boolean newValue) {

        super.setEnabled(newValue);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final List<DownloadLink> nodesToDelete = new ArrayList<DownloadLink>();
        for (final DownloadLink dl : selection.getChildren()) {
            if (checkLink(dl)) {
                nodesToDelete.add(dl);
            }
        }
        if (nodesToDelete.size() > 0) {
            final SelectionInfo<FilePackage, DownloadLink> si = new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete, null, null, e, getTable());
            if (si.getChildren().size() > 0) {
                DownloadTabActionUtils.deleteLinksRequest(si, _GUI._.GenericDeleteFromDownloadlistAction_actionPerformed_ask_(createName()));
                return;
            }
        }

        Toolkit.getDefaultToolkit().beep();
        Dialog.getInstance().showErrorDialog(_GUI._.GenericDeleteSelectedToolbarAction_actionPerformed_nothing_to_delete_());
    }

    private DownloadsTable getTable() {
        return (DownloadsTable) DownloadsTableModel.getInstance().getTable();
    }

    public boolean checkLink(DownloadLink link) {
        if (isDeleteAll()) { return true; }

        if (isDeleteDisabled() && !link.isEnabled()) {

        return true; }
        if (isDeleteFailed() && FinalLinkState.CheckFailed(link.getFinalLinkState())) {

        return true; }
        if (isDeleteFinished() && FinalLinkState.CheckFinished(link.getFinalLinkState())) {

        return true; }
        if (isDeleteOffline() && link.getFinalLinkState() == FinalLinkState.OFFLINE) {

        return true; }
        return false;
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled();
    }

    private void updateName() {

        // if (isDeleteFailed() && isDeleteDisabled() && isDeleteFinished() && isDeleteOffline()) {
        // setName(_GUI._.ContextMenuFactory_createPopup_cleanup_only());
        // } else {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setName(createName());
            }
        };

        // }

    }

    private String createName() {
        final StringBuilder sb = new StringBuilder();

        if (isDeleteAll()) {
            if (isOnlySelectedItems()) {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_selected_all());
            } else {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_all());
            }
        } else {
            if (isOnlySelectedItems()) {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_selected());
            } else {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object());
            }
            boolean first = true;

            if (isDeleteDisabled()) {
                if (!first) {
                    sb.append(" & ");
                }
                sb.append(_GUI._.lit_disabled());
                first = false;
            }
            if (isDeleteFailed()) {
                if (!first) {
                    sb.append(" & ");
                }
                first = false;
                sb.append(_GUI._.lit_failed());
            }
            if (isDeleteFinished()) {
                if (!first) {
                    sb.append(" & ");
                }
                first = false;
                sb.append(_GUI._.lit_finished());
            }
            if (isDeleteOffline()) {
                if (!first) {
                    sb.append(" & ");
                }
                first = false;
                sb.append(_GUI._.lit_offline());
            }
        }
        return sb.toString();
    }

    @Override
    public void onExtTableEvent(ExtTableEvent<?> event) {
        if (event.getType() == ExtTableEvent.Types.SELECTION_CHANGED) {
            update();
        }
    }

    @Override
    public void onDownloadControllerAddedPackage(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerStructureRefresh(FilePackage pkg) {
        delayer.resetAndStart();
    }

    @Override
    public void onDownloadControllerStructureRefresh() {
        delayer.resetAndStart();
    }

    @Override
    public void onDownloadControllerStructureRefresh(AbstractNode node, Object param) {
        delayer.resetAndStart();
    }

    @Override
    public void onDownloadControllerRemovedPackage(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerRemovedLinklist(List<DownloadLink> list) {
        delayer.resetAndStart();
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, DownloadLinkProperty property) {
        delayer.resetAndStart();
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, LinkStatusProperty property) {
        delayer.resetAndStart();
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {
        delayer.resetAndStart();
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg) {
    }

}
