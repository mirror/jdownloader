package org.jdownloader.gui.views.downloads.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.plugins.FinalLinkState;

public class GenericDeleteFromDownloadlistAction extends AppAction implements CachableInterface {
    /**
     * 
     */
    private static final long  serialVersionUID  = 1L;
    public static final String DELETE_ALL        = "deleteAll";
    public static final String DELETE_DISABLED   = "deleteDisabled";
    public static final String DELETE_FAILED     = "deleteFailed";
    public static final String DELETE_FINISHED   = "deleteFinished";
    public static final String DELETE_OFFLINE    = "deleteOffline";
    private List<AbstractNode> currentSelection;

    private boolean            deleteDisabled    = false;
    private boolean            onlySelectedItems = false;

    private boolean            deleteAll         = false;

    private boolean            ignoreFiltered    = true;

    private boolean            deleteFailed      = false;

    private boolean            deleteFinished    = false;

    private boolean            deleteOffline     = false;

    private List<DownloadLink> filteredDownloadLinks;

    public GenericDeleteFromDownloadlistAction() {
        super();
        this.setIconKey(IconKey.ICON_DELETE);

    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        this.update();
        final List<AbstractNode> lcurrentSelection = this.currentSelection;
        final List<DownloadLink> lfilteredDownloadLinks = this.filteredDownloadLinks;

        if (lcurrentSelection != null) {
            if (lfilteredDownloadLinks != null) {
                final SelectionInfo<FilePackage, DownloadLink> si = new SelectionInfo<FilePackage, DownloadLink>(null, lfilteredDownloadLinks, null, null, e, getTable());
                if (si.getChildren().size() > 0) {
                    DownloadTabActionUtils.deleteLinksRequest(si, _GUI._.GenericDeleteFromDownloadlistAction_actionPerformed_ask_(this.createName()));
                    return;
                }
            }
        }
        Toolkit.getDefaultToolkit().beep();
        Dialog.getInstance().showErrorDialog(_GUI._.GenericDeleteSelectedToolbarAction_actionPerformed_nothing_to_delete_());
    }

    private DownloadsTable getTable() {
        return (DownloadsTable) DownloadsTableModel.getInstance().getTable();
    }

    @Customizer(name = "Include All Links")
    public boolean isDeleteAll() {
        return this.deleteAll;
    }

    @Customizer(name = "Include disabled Links")
    public boolean isDeleteDisabled() {
        return this.deleteDisabled;
    }

    @Customizer(name = "Include failed")
    public boolean isDeleteFailed() {
        return this.deleteFailed;
    }

    @Customizer(name = "Include finished Links")
    public boolean isDeleteFinished() {
        return this.deleteFinished;
    }

    @Customizer(name = "Include Offline Links")
    public boolean isDeleteOffline() {
        return this.deleteOffline;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Customizer(name = "Exclude filtered Links")
    public boolean isIgnoreFiltered() {
        return this.ignoreFiltered;
    }

    @Customizer(name = "Only Selected Links")
    public boolean isOnlySelectedItems() {
        return this.onlySelectedItems;
    }

    @Override
    public void setData(final String data) {
    }

    public void setDeleteAll(final boolean deleteIdle) {
        this.deleteAll = deleteIdle;
        this.updateName();
    }

    public void setDeleteDisabled(final boolean deleteDisabled) {
        this.deleteDisabled = deleteDisabled;
        this.updateName();
    }

    public void setDeleteFailed(final boolean deleteFailed) {
        this.deleteFailed = deleteFailed;
        this.updateName();
    }

    public void setDeleteFinished(final boolean deleteFinished) {
        this.deleteFinished = deleteFinished;
        this.updateName();
    }

    public void setDeleteOffline(final boolean deleteOffline) {
        this.deleteOffline = deleteOffline;
        this.updateName();
    }

    public void setIgnoreFiltered(final boolean ignoreFiltered) {
        this.ignoreFiltered = ignoreFiltered;
    }

    public void setOnlySelectedItems(final boolean onlySelectedItems) {
        this.onlySelectedItems = onlySelectedItems;
    }

    private void update() {

        if (this.isOnlySelectedItems()) {
            this.currentSelection = getTable().getModel().getSelectedObjects();
        } else {
            if (this.isIgnoreFiltered()) {
                this.currentSelection = getTable().getModel().getElements();
            } else {

                this.currentSelection = new ArrayList<AbstractNode>(getTable().getModel().getController().getAllChildren());
            }

        }
        this.filteredDownloadLinks = null;

        if (this.currentSelection != null && this.currentSelection.size() > 0) {

            final SelectionInfo<FilePackage, DownloadLink> si = new SelectionInfo<FilePackage, DownloadLink>(null, this.currentSelection, null, null, null, getTable());

            final List<DownloadLink> nodesToDelete = new ArrayList<DownloadLink>();
            for (final DownloadLink dl : si.getChildren()) {
                if (this.isDeleteAll()) {
                    nodesToDelete.add(dl);
                    continue;
                }
                if (this.isDeleteDisabled() && !dl.isEnabled()) {
                    nodesToDelete.add(dl);
                    continue;
                }
                if (this.isDeleteFailed() && FinalLinkState.CheckFailed(dl.getFinalLinkState())) {
                    nodesToDelete.add(dl);
                    continue;
                }
                if (this.isDeleteFinished() && FinalLinkState.CheckFinished(dl.getFinalLinkState())) {
                    nodesToDelete.add(dl);
                    continue;
                }
                if (this.isDeleteOffline() && dl.getFinalLinkState() == FinalLinkState.OFFLINE) {
                    nodesToDelete.add(dl);
                    continue;
                }
            }
            this.filteredDownloadLinks = nodesToDelete;

        }

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

        if (this.isDeleteAll()) {
            if (this.isOnlySelectedItems()) {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_selected_all());
            } else {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_all());
            }
        } else {
            if (this.isOnlySelectedItems()) {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_selected());
            } else {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object());
            }
            boolean first = true;

            if (this.isDeleteDisabled()) {
                if (!first) {
                    sb.append(" & ");
                }
                sb.append(_GUI._.lit_disabled());
                first = false;
            }
            if (this.isDeleteFailed()) {
                if (!first) {
                    sb.append(" & ");
                }
                first = false;
                sb.append(_GUI._.lit_failed());
            }
            if (this.isDeleteFinished()) {
                if (!first) {
                    sb.append(" & ");
                }
                first = false;
                sb.append(_GUI._.lit_finished());
            }
            if (this.isDeleteOffline()) {
                if (!first) {
                    sb.append(" & ");
                }
                first = false;
                sb.append(_GUI._.lit_offline());
            }
        }
        return sb.toString();
    }

}
