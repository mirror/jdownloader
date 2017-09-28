package org.jdownloader.gui.views.downloads.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.swing.KeyStroke;

import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.exttable.ExtTableEvent;
import org.appwork.swing.exttable.ExtTableListener;
import org.appwork.swing.exttable.ExtTableModelEventWrapper;
import org.appwork.swing.exttable.ExtTableModelListener;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTHelper;
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
import org.jdownloader.gui.views.components.packagetable.EmptySelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.bottombar.IncludedSelectionSetup;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.DeleteFileOptions;
import org.jdownloader.translate._JDT;

public class GenericDeleteFromDownloadlistAction extends CustomizableAppAction implements ExtTableListener, ActionContext, DownloadControllerListener, ExtTableModelListener {
    public static final String                                        DELETE_ALL                = "deleteAll";
    public static final String                                        DELETE_DISABLED           = "deleteDisabled";
    public static final String                                        DELETE_FAILED             = "deleteFailed";
    public static final String                                        DELETE_FINISHED           = "deleteFinished";
    public static final String                                        DELETE_OFFLINE            = "deleteOffline";
    public static final String                                        DELETE_MODE               = "deleteMode";
    /**
     *
     */
    private static final long                                         serialVersionUID          = 1L;
    private final DelayedRunnable                                     delayer;
    private boolean                                                   deleteAll                 = false;
    private boolean                                                   deleteDisabled            = false;
    private boolean                                                   deleteFailed              = false;
    private boolean                                                   deleteFinished            = false;
    private boolean                                                   deleteOffline             = false;
    private boolean                                                   ignoreFiltered            = true;
    protected WeakReference<DownloadLink>                             lastLink                  = new WeakReference<DownloadLink>(null);
    private Modifier                                                  deleteFilesToggleModifier = null;
    protected WeakReference<SelectionInfo<FilePackage, DownloadLink>> selection                 = new WeakReference<SelectionInfo<FilePackage, DownloadLink>>(null);

    public static String getTranslationForDeleteFilesToggleModifier() {
        return _JDT.T.GenericDeleteFromDownloadlistAction_getTranslationForDeleteFilesToggleModifier();
    }

    @Customizer(link = "#getTranslationForDeleteFilesToggleModifier")
    public Modifier getDeleteFilesToggleModifier() {
        return deleteFilesToggleModifier;
    }

    public void setDeleteFilesToggleModifier(Modifier deleteFilesToggleModifier) {
        this.deleteFilesToggleModifier = deleteFilesToggleModifier;
    }

    @Override
    public void initContextDefaults() {
        super.initContextDefaults();
        includedSelection.setIncludeSelectedLinks(true);
        includedSelection.setIncludeUnselectedLinks(true);
    }

    public static String getTranslationForDeleteMode() {
        return _JDT.T.GenericDeleteFromDownloadlistAction_getTranslationForDeleteMode();
    }

    @Customizer(link = "#getTranslationForDeleteMode")
    public DeleteFileOptions getDeleteMode() {
        // Modifier byPassDialog = getByPassDialogToggleModifier();
        Modifier deletToggle = getDeleteFilesToggleModifier();
        if (deleteMode == null) {
            deleteMode = DeleteFileOptions.REMOVE_LINKS_ONLY;
        }
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
    private boolean                  deleteFinishedPackage;

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
        delayer = new DelayedRunnable(TaskQueue.TIMINGQUEUE, 500, 1500) {
            @Override
            public void delayedrun() {
                update();
            }
        };
        initIncludedSelectionSupport();
        setIconKey(IconKey.ICON_DELETE);
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
        if (keystroke == null) {
            return null;
        }
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
        final SelectionInfo<FilePackage, DownloadLink> finalSelection = selection.get();
        if (finalSelection != null) {
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                @Override
                protected Void run() throws RuntimeException {
                    final List<DownloadLink> nodesToDelete = new ArrayList<DownloadLink>();
                    boolean createNewSelectionInfo = false;
                    switch (includedSelection.getSelectionType()) {
                    case NONE:
                        return null;
                    case UNSELECTED:
                        createNewSelectionInfo = true;
                        for (final DownloadLink child : finalSelection.getUnselectedChildren()) {
                            if (checkLink(child)) {
                                nodesToDelete.add(child);
                            }
                        }
                        break;
                    default:
                        for (final DownloadLink dl : finalSelection.getChildren()) {
                            if (checkLink(dl)) {
                                nodesToDelete.add(dl);
                            } else {
                                createNewSelectionInfo = true;
                            }
                        }
                    }
                    if (nodesToDelete.size() > 0) {
                        final SelectionInfo<FilePackage, DownloadLink> si;
                        if (createNewSelectionInfo) {
                            si = new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete);
                        } else {
                            si = finalSelection;
                        }
                        if (si.getChildren().size() > 0) {
                            DownloadTabActionUtils.deleteLinksRequest(si, _GUI.T.GenericDeleteFromDownloadlistAction_actionPerformed_ask_(createName()), getDeleteMode(), byPassDialog.isBypassDialog());
                            return null;
                        }
                    }
                    new EDTHelper<Void>() {
                        @Override
                        public Void edtRun() {
                            Toolkit.getDefaultToolkit().beep();
                            Dialog.getInstance().showErrorDialog(_GUI.T.GenericDeleteSelectedToolbarAction_actionPerformed_nothing_to_delete_());
                            return null;
                        }
                    }.start(true);
                    return null;
                }
            });
        }
    }

    public boolean checkLink(DownloadLink link) {
        if (isDeleteAll()) {
            return true;
        }
        if (isDeleteDisabled() && !link.isEnabled()) {
            return true;
        }
        if (isDeleteFailed() && FinalLinkState.CheckFailed(link.getFinalLinkState())) {
            return true;
        }
        if (isDeleteFinished() && FinalLinkState.CheckFinished(link.getFinalLinkState())) {
            return true;
        }
        if (isDeleteFinishedPackage() && link.getFilePackage().getView().isFinished()) {
            return true;
        }
        if (isDeleteOffline() && link.getFinalLinkState() == FinalLinkState.OFFLINE) {
            return true;
        }
        return false;
    }

    private String createName() {
        final StringBuilder sb = new StringBuilder();
        if (isDeleteAll()) {
            switch (includedSelection.getSelectionType()) {
            case SELECTED:
                sb.append(_GUI.T.GenericDeleteSelectedToolbarAction_updateName_object_selected_all().trim());
                break;
            case UNSELECTED:
                sb.append(_GUI.T.GenericDeleteSelectedToolbarAction_updateName_object_keep_selected().trim());
                break;
            default:
                sb.append(_GUI.T.GenericDeleteSelectedToolbarAction_updateName_object_all().trim());
            }
        } else {
            switch (includedSelection.getSelectionType()) {
            case SELECTED:
                sb.append(_GUI.T.GenericDeleteSelectedToolbarAction_updateName_object_selected().trim());
                break;
            case UNSELECTED:
                sb.append(_GUI.T.GenericDeleteSelectedToolbarAction_updateName_object_keep_unselected().trim());
                break;
            default:
                sb.append(_GUI.T.GenericDeleteSelectedToolbarAction_updateName_object().trim());
            }
            boolean first = true;
            if (isDeleteDisabled()) {
                if (!first) {
                    appendMissingSpace(sb);
                    sb.append("&");
                }
                appendMissingSpace(sb);
                sb.append(_GUI.T.lit_disabled().trim());
                first = false;
            }
            if (isDeleteFailed()) {
                if (!first) {
                    appendMissingSpace(sb);
                    sb.append("&");
                }
                first = false;
                appendMissingSpace(sb);
                sb.append(_GUI.T.lit_failed().trim());
            }
            if (isDeleteFinished()) {
                if (!first) {
                    appendMissingSpace(sb);
                    sb.append("&");
                }
                first = false;
                appendMissingSpace(sb);
                sb.append(_GUI.T.lit_finished().trim());
            }
            if (isDeleteFinishedPackage()) {
                if (!first) {
                    appendMissingSpace(sb);
                    sb.append("&");
                }
                first = false;
                appendMissingSpace(sb);
                sb.append(_GUI.T.lit_finished_package().trim());
            }
            if (isDeleteOffline()) {
                if (!first) {
                    appendMissingSpace(sb);
                    sb.append("&");
                }
                first = false;
                appendMissingSpace(sb);
                sb.append(_GUI.T.lit_offline().trim());
            }
        }
        switch (getDeleteMode()) {
        case REMOVE_LINKS_AND_DELETE_FILES:
            appendMissingSpace(sb);
            sb.append(_GUI.T.deleteaction_and_delete_files().trim());
            break;
        case REMOVE_LINKS_AND_RECYCLE_FILES:
            appendMissingSpace(sb);
            sb.append(_GUI.T.deleteaction_and_recycle_files().trim());
            break;
        case REMOVE_LINKS_ONLY:
            break;
        }
        return sb.toString();
    }

    private void appendMissingSpace(StringBuilder sb) {
        if (sb.charAt(sb.length() - 1) != ' ') {
            sb.append(" ");
        }
    }

    protected DownloadsTable getTable() {
        return (DownloadsTable) DownloadsTableModel.getInstance().getTable();
    }

    public static String getTranslationForDeleteAll() {
        return _JDT.T.GenericDeleteFromDownloadlistAction_getTranslationForDeleteAll();
    }

    @Customizer(link = "#getTranslationForDeleteAll")
    public boolean isDeleteAll() {
        return deleteAll;
    }

    public static String getTranslationForDeleteDisabled() {
        return _JDT.T.GenericDeleteFromDownloadlistAction_getTranslationForDeleteDisabled();
    }

    @Customizer(link = "#getTranslationForDeleteDisabled")
    public boolean isDeleteDisabled() {
        return deleteDisabled;
    }

    public static String getTranslationForDeleteFailed() {
        return _JDT.T.GenericDeleteFromDownloadlistAction_getTranslationForDeleteFailed();
    }

    @Customizer(link = "#getTranslationForDeleteFailed")
    public boolean isDeleteFailed() {
        return deleteFailed;
    }

    public static String getTranslationForDeleteFinishedPackage() {
        return _JDT.T.GenericDeleteFromDownloadlistAction_getTranslationForDeleteFinishedPackage();
    }

    public static String getTranslationForDeleteFinished() {
        return _JDT.T.GenericDeleteFromDownloadlistAction_getTranslationForDeleteFinished();
    }

    @Customizer(link = "#getTranslationForDeleteFinishedPackage")
    public boolean isDeleteFinishedPackage() {
        return deleteFinishedPackage;
    }

    public void setDeleteFinishedPackage(final boolean deleteFinished) {
        GenericDeleteFromDownloadlistAction.this.deleteFinishedPackage = deleteFinished;
    }

    @Customizer(link = "#getTranslationForDeleteFinished")
    public boolean isDeleteFinished() {
        return deleteFinished;
    }

    public static String getTranslationForDeleteOffline() {
        return _JDT.T.GenericDeleteFromDownloadlistAction_getTranslationForDeleteOffline();
    }

    @Customizer(link = "#getTranslationForDeleteOffline")
    public boolean isDeleteOffline() {
        return deleteOffline;
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled();
    }

    public static String getTranslationForIgnoreFiltered() {
        return _JDT.T.GenericDeleteFromDownloadlistAction_getTranslationForIgnoreFiltered();
    }

    @Customizer(link = "#getTranslationForIgnoreFiltered")
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
        if (lastLink != null) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    final SelectionInfo<FilePackage, DownloadLink> selectionInfo;
                    switch (includedSelection.getSelectionType()) {
                    case SELECTED:
                        selection = new WeakReference<SelectionInfo<FilePackage, DownloadLink>>(selectionInfo = getTable().getSelectionInfo());
                        break;
                    case UNSELECTED:
                        selection = new WeakReference<SelectionInfo<FilePackage, DownloadLink>>(selectionInfo = getTable().getSelectionInfo());
                        final DownloadLink lastDownloadLink = lastLink.get();
                        if (lastDownloadLink != null && !selectionInfo.contains(lastDownloadLink)) {
                            if (checkLink(lastDownloadLink)) {
                                setEnabled(true);
                                return;
                            }
                        }
                        if (selectionInfo.getUnselectedChildren() != null) {
                            for (final DownloadLink child : selectionInfo.getUnselectedChildren()) {
                                if (checkLink(child)) {
                                    setEnabled(true);
                                    lastLink = new WeakReference<DownloadLink>(child);
                                    return;
                                }
                            }
                        }
                        setEnabled(false);
                        return;
                        // fix this
                    case ALL:
                        if (isIgnoreFiltered()) {
                            selectionInfo = getTable().getSelectionInfo(false, true);
                        } else {
                            selectionInfo = getTable().getSelectionInfo(false, false);
                        }
                        selection = new WeakReference<SelectionInfo<FilePackage, DownloadLink>>(selectionInfo);
                        break;
                    case NONE:
                    default:
                        selectionInfo = new EmptySelectionInfo<FilePackage, DownloadLink>(getTable().getController());
                        selection = new WeakReference<SelectionInfo<FilePackage, DownloadLink>>(selectionInfo);
                        break;
                    }
                    final DownloadLink lastDownloadLink = lastLink.get();
                    if (lastDownloadLink != null && !selectionInfo.contains(lastDownloadLink)) {
                        if (checkLink(lastDownloadLink)) {
                            setEnabled(true);
                            return;
                        }
                    }
                    if (isDeleteAll() && !selectionInfo.isEmpty()) {
                        setEnabled(true);
                        return;
                    }
                    for (final DownloadLink child : selectionInfo.getChildren()) {
                        if (checkLink(child)) {
                            lastLink = new WeakReference<DownloadLink>(child);
                            setEnabled(true);
                            return;
                        }
                    }
                    setEnabled(false);
                }
            };
        }
    }

    @Override
    public void onExtTableModelEvent(ExtTableModelEventWrapper event) {
        delayer.resetAndStart();
    }
}
