package org.jdownloader.gui.views.downloads.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.KeyStroke;

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
import org.appwork.swing.exttable.ExtTableModelEventWrapper;
import org.appwork.swing.exttable.ExtTableModelListener;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.KeyObserver;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.bottombar.IncludedSelectionSetup;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.DeleteFileOptions;

public class GenericDeleteFromDownloadlistAction extends CustomizableAppAction implements ExtTableListener, ActionContext, DownloadControllerListener, ExtTableModelListener {

    public static final String                         DELETE_ALL                = "deleteAll";
    public static final String                         DELETE_DISABLED           = "deleteDisabled";
    public static final String                         DELETE_FAILED             = "deleteFailed";
    public static final String                         DELETE_FINISHED           = "deleteFinished";
    public static final String                         DELETE_OFFLINE            = "deleteOffline";
    /**
     * 
     */
    private static final long                          serialVersionUID          = 1L;

    private DelayedRunnable                            delayer;
    private boolean                                    deleteAll                 = false;

    private boolean                                    deleteDisabled            = false;

    private boolean                                    deleteFailed              = false;

    private boolean                                    deleteFinished            = false;

    private boolean                                    deleteOffline             = false;

    private boolean                                    ignoreFiltered            = true;

    private DownloadLink                               lastLink;

    private Modifier                                   deleteFilesToggleModifier = null;

    protected SelectionInfo<FilePackage, DownloadLink> selection;

    @Customizer(name = "Key Modifier to toggle 'Delete Files'")
    public Modifier getDeleteFilesToggleModifier() {
        return deleteFilesToggleModifier;
    }

    public void setDeleteFilesToggleModifier(Modifier deleteFilesToggleModifier) {
        this.deleteFilesToggleModifier = deleteFilesToggleModifier;
    }

    @Override
    protected void initContextDefaults() {
        super.initContextDefaults();
        includedSelection.setIncludeSelectedLinks(true);
        includedSelection.setIncludeUnselectedLinks(true);
    }

    @Customizer(name = "Delete Mode")
    public DeleteFileOptions getDeleteMode() {

        // Modifier byPassDialog = getByPassDialogToggleModifier();
        Modifier deletToggle = getDeleteFilesToggleModifier();
        if (deleteMode == null) deleteMode = DeleteFileOptions.REMOVE_LINKS_ONLY;
        if (deletToggle != null && KeyObserver.getInstance().isModifierPressed(deletToggle.getModifier(), false)) {
            switch (deleteMode) {
            case REMOVE_LINKS_ONLY:
                return DeleteFileOptions.REMOVE_LINKS_AND_RECYCLE_FILES;
            case REMOVE_LINKS_AND_DELETE_FILES:
            case REMOVE_LINKS_AND_RECYCLE_FILES:
                return DeleteFileOptions.REMOVE_LINKS_ONLY;
            }
        }

        return deleteMode;
    }

    public void setDeleteMode(DeleteFileOptions deleteMode) {
        this.deleteMode = deleteMode;
    }

    private DeleteFileOptions        deleteMode;
    private ByPassDialogSetup        byPassDialog;
    protected IncludedSelectionSetup includedSelection;

    @Override
    public void loadContextSetups() {
        super.loadContextSetups();
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setName(createName());
            }
        }.getReturnValue();

    }

    public GenericDeleteFromDownloadlistAction() {
        super();
        addContextSetup(byPassDialog = new ByPassDialogSetup());
        initIncludedSelectionSupport();
        setIconKey(IconKey.ICON_DELETE);
        delayer = new DelayedRunnable(500, 1500) {

            @Override
            public void delayedrun() {
                update();
            }
        };

    }

    protected void initIncludedSelectionSupport() {
        addContextSetup(includedSelection = new IncludedSelectionSetup(DownloadsTable.getInstance(), this, this) {
            @Override
            public void updateListeners() {
                super.updateListeners();
                switch (getSelectionType()) {
                case ALL:
                    DownloadController.getInstance().getEventSender().addListener(GenericDeleteFromDownloadlistAction.this, true);
                    break;
                case SELECTED:
                case UNSELECTED:
                    DownloadController.getInstance().getEventSender().removeListener(GenericDeleteFromDownloadlistAction.this);
                    break;

                case NONE:
                    DownloadController.getInstance().getEventSender().removeListener(GenericDeleteFromDownloadlistAction.this);
                }

            }
        });
    }

    public List<KeyStroke> getAdditionalShortcuts(KeyStroke keystroke) {
        if (keystroke == null) return null;

        ArrayList<KeyStroke> ret = new ArrayList<KeyStroke>();
        Modifier mod1 = byPassDialog.getByPassDialogToggleModifier();
        if (mod1 != null) {
            ret.add(KeyStroke.getKeyStroke(keystroke.getKeyCode(), keystroke.getModifiers() | mod1.getModifier()));
        }

        Modifier mod2 = getDeleteFilesToggleModifier();
        if (mod2 != null) {
            ret.add(KeyStroke.getKeyStroke(keystroke.getKeyCode(), keystroke.getModifiers() | mod2.getModifier()));
        }

        if (mod2 != null && mod1 != null) {
            ret.add(KeyStroke.getKeyStroke(keystroke.getKeyCode(), keystroke.getModifiers() | mod2.getModifier() | mod1.getModifier()));
        }
        return ret;
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

                DownloadTabActionUtils.deleteLinksRequest(si, _GUI._.GenericDeleteFromDownloadlistAction_actionPerformed_ask_(createName()), getDeleteMode(), byPassDialog.isBypassDialog());
                return;
            }
        }

        Toolkit.getDefaultToolkit().beep();
        Dialog.getInstance().showErrorDialog(_GUI._.GenericDeleteSelectedToolbarAction_actionPerformed_nothing_to_delete_());
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

    private String createName() {
        final StringBuilder sb = new StringBuilder();

        if (isDeleteAll()) {
            switch (includedSelection.getSelectionType()) {
            case SELECTED:
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_selected_all());
                break;

            case UNSELECTED:
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_keep_selected());
                break;
            default:
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_all());

            }

        } else {

            switch (includedSelection.getSelectionType()) {
            case SELECTED:
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_selected());
                break;

            case UNSELECTED:
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_keep_unselected());
                break;
            default:
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

    protected DownloadsTable getTable() {
        return (DownloadsTable) DownloadsTableModel.getInstance().getTable();
    }

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

    @Override
    public boolean isEnabled() {
        return super.isEnabled();
    }

    @Customizer(name = "Exclude filtered Links")
    public boolean isIgnoreFiltered() {
        return ignoreFiltered;
    }

    @Override
    public void onDownloadControllerAddedPackage(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerRemovedLinklist(List<DownloadLink> list) {
        delayer.resetAndStart();
    }

    @Override
    public void onDownloadControllerRemovedPackage(FilePackage pkg) {
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
    public void onDownloadControllerStructureRefresh(FilePackage pkg) {
        delayer.resetAndStart();
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {
        delayer.resetAndStart();
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, DownloadLinkProperty property) {
        delayer.resetAndStart();
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, LinkStatusProperty property) {
        delayer.resetAndStart();
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {
    }

    @Override
    public void onExtTableEvent(ExtTableEvent<?> event) {
        if (event.getType() == ExtTableEvent.Types.SELECTION_CHANGED) {
            update();
        }
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        includedSelection.updateListeners();
        update();
    }

    public void setDeleteAll(final boolean deleteIdle) {
        GenericDeleteFromDownloadlistAction.this.deleteAll = deleteIdle;

    }

    public void setDeleteDisabled(final boolean deleteDisabled) {
        GenericDeleteFromDownloadlistAction.this.deleteDisabled = deleteDisabled;

    }

    public void setDeleteFailed(final boolean deleteFailed) {
        GenericDeleteFromDownloadlistAction.this.deleteFailed = deleteFailed;

    }

    public void setDeleteFinished(final boolean deleteFinished) {
        GenericDeleteFromDownloadlistAction.this.deleteFinished = deleteFinished;

    }

    public void setDeleteOffline(final boolean deleteOffline) {
        GenericDeleteFromDownloadlistAction.this.deleteOffline = deleteOffline;

    }

    public void setIgnoreFiltered(final boolean ignoreFiltered) {
        GenericDeleteFromDownloadlistAction.this.ignoreFiltered = ignoreFiltered;

    }

    protected void update() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                switch (includedSelection.getSelectionType()) {
                case SELECTED:
                    selection = getTable().getSelectionInfo();
                    break;
                case UNSELECTED:
                    selection = getTable().getSelectionInfo();

                    setVisible(true);

                    if (lastLink != null && !selection.contains(lastLink)) {
                        if (checkLink(lastLink)) {

                            setEnabled(true);
                            return;
                        }
                    }

                    final List<PackageControllerTableModelFilter<FilePackage, DownloadLink>> filters = DownloadsTableModel.getInstance().getTableFilters();

                    boolean read = DownloadController.getInstance().readLock();
                    try {
                        for (FilePackage pkg : DownloadController.getInstance().getPackages()) {
                            if (selection.getFullPackages().contains(pkg)) {
                                continue;
                            }
                            boolean readL2 = pkg.getModifyLock().readLock();
                            try {
                                childs: for (DownloadLink child : pkg.getChildren()) {
                                    if (selection.contains(child)) {
                                        continue;
                                    }
                                    if (isIgnoreFiltered()) {

                                        for (PackageControllerTableModelFilter<FilePackage, DownloadLink> filter : filters) {
                                            if (filter.isFiltered((DownloadLink) child)) {
                                                continue childs;
                                            }
                                        }
                                    }
                                    if (checkLink(child)) {
                                        setEnabled(true);
                                        lastLink = child;
                                        return;
                                    }
                                }
                            } finally {
                                pkg.getModifyLock().readUnlock(readL2);
                            }
                        }
                    } finally {
                        DownloadController.getInstance().readUnlock(read);
                    }

                    setEnabled(false);

                    return;

                default:
                    if (isIgnoreFiltered()) {

                        selection = getTable().getSelectionInfo(false, false);
                    } else {
                        selection = getTable().getSelectionInfo(false, true);

                    }
                    break;
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

    private void updateName() {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setName(createName());
            }
        };

    }

    @Override
    public void onExtTableModelEvent(ExtTableModelEventWrapper event) {
        delayer.resetAndStart();
    }

}
