package org.jdownloader.gui.toolbar.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.swing.KeyStroke;

import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
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
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.KeyObserver;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.action.ByPassDialogSetup;
import org.jdownloader.gui.views.downloads.action.DownloadTabActionUtils;
import org.jdownloader.gui.views.downloads.action.Modifier;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.DeleteFileOptions;
import org.jdownloader.translate._JDT;

public class GenericDeleteFromTableToolbarAction extends AbstractToolBarAction implements ExtTableListener, GUIListener, ExtTableModelListener, ActionContext, DownloadControllerListener, LinkCollectorListener {
    public static final String                 ONLY_SELECTED_ITEMS = "OnlySelectedItems";
    public static final String                 DELETE_ALL          = "deleteAll";
    public static final String                 DELETE_DISABLED     = "deleteDisabled";
    public static final String                 DELETE_FAILED       = "deleteFailed";
    public static final String                 DELETE_FINISHED     = "deleteFinished";
    public static final String                 DELETE_OFFLINE      = "deleteOffline";
    /**
     *
     */
    private static final long                  serialVersionUID    = 1L;
    private DelayedRunnable                    delayer;
    private boolean                            deleteAll           = false;
    private boolean                            deleteDisabled      = false;
    private boolean                            deleteFailed        = false;
    private boolean                            deleteFinished      = false;
    private boolean                            deleteOffline       = false;
    private boolean                            ignoreFiltered      = true;
    private WeakReference<CrawledLink>         lastCrawledLink     = new WeakReference<CrawledLink>(null);
    private WeakReference<DownloadLink>        lastDownloadLink    = new WeakReference<DownloadLink>(null);
    private boolean                            onlySelectedItems   = false;
    private WeakReference<SelectionInfo<?, ?>> selection           = new WeakReference<SelectionInfo<?, ?>>(null);
    private ByPassDialogSetup                  byPass;

    public GenericDeleteFromTableToolbarAction() {
        super();
        addContextSetup(byPass = new ByPassDialogSetup());
        setIconKey(IconKey.ICON_DELETE);
        delayer = new DelayedRunnable(500, 1500) {
            @Override
            public void delayedrun() {
                update();
            }
        };
        GUIEventSender.getInstance().addListener(this, true);
        onGuiMainTabSwitch(null, MainTabbedPane.getInstance().getSelectedView());
        update();
    }

    private Modifier deleteFilesToggleModifier = null;

    public static String getTranslationForDeleteFilesToggleModifier() {
        return _JDT.T.GenericDeleteFromTableToolbarAction_getTranslationForDeleteFilesToggleModifier();
    }

    public static String getTranslationForDeleteMode() {
        return _JDT.T.GenericDeleteFromTableToolbarAction_getTranslationForDeleteMode();
    }

    public static String getTranslationForDeleteAll() {
        return _JDT.T.GenericDeleteFromTableToolbarAction_getTranslationForDeleteAll();
    }

    public static String getTranslationForDeleteDisabled() {
        return _JDT.T.GenericDeleteFromTableToolbarAction_getTranslationForDeleteDisabled();
    }

    public static String getTranslationForDeleteFailed() {
        return _JDT.T.GenericDeleteFromTableToolbarAction_getTranslationForDeleteFailed();
    }

    public static String getTranslationForDeleteFinished() {
        return _JDT.T.GenericDeleteFromTableToolbarAction_getTranslationForDeleteFinished();
    }

    public static String getTranslationForDeleteOffline() {
        return _JDT.T.GenericDeleteFromTableToolbarAction_getTranslationForDeleteOffline();
    }

    public static String getTranslationForIgnoreFiltered() {
        return _JDT.T.GenericDeleteFromTableToolbarAction_getTranslationForIgnoreFiltered();
    }

    public static String getTranslationForOnlySelectedItems() {
        return _JDT.T.GenericDeleteFromTableToolbarAction_getTranslationForOnlySelectedItems();
    }

    @Customizer(link = "#getTranslationForDeleteFilesToggleModifier")
    public Modifier getDeleteFilesToggleModifier() {
        return deleteFilesToggleModifier;
    }

    public void setDeleteFilesToggleModifier(Modifier deleteFilesToggleModifier) {
        this.deleteFilesToggleModifier = deleteFilesToggleModifier;
    }

    public List<KeyStroke> getAdditionalShortcuts(KeyStroke keystroke) {
        if (keystroke == null) {
            return null;
        }
        ArrayList<KeyStroke> ret = new ArrayList<KeyStroke>();
        Modifier mod = byPass.getByPassDialogToggleModifier();
        if (mod != null) {
            ret.add(KeyStroke.getKeyStroke(keystroke.getKeyCode(), keystroke.getModifiers() | mod.getModifier()));
        }
        mod = getDeleteFilesToggleModifier();
        if (mod != null) {
            ret.add(KeyStroke.getKeyStroke(keystroke.getKeyCode(), keystroke.getModifiers() | mod.getModifier()));
        }
        return ret;
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

    private DeleteFileOptions deleteMode;

    @Override
    public void actionPerformed(final ActionEvent e) {
        final SelectionInfo<?, ?> lselection = selection.get();
        if (lselection == null || lselection.isEmpty()) {
            return;
        } else {
            if (lselection.getController() instanceof DownloadController) {
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                    @Override
                    protected Void run() throws RuntimeException {
                        final List<DownloadLink> nodesToDelete = new ArrayList<DownloadLink>();
                        boolean createNewSelectionInfo = false;
                        for (final Object dl : lselection.getChildren()) {
                            if (checkDownloadLink((DownloadLink) dl)) {
                                nodesToDelete.add((DownloadLink) dl);
                            } else {
                                createNewSelectionInfo = true;
                            }
                        }
                        if (nodesToDelete.size() > 0) {
                            final SelectionInfo<FilePackage, DownloadLink> si;
                            if (createNewSelectionInfo) {
                                si = new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete);
                            } else {
                                si = (SelectionInfo<FilePackage, DownloadLink>) lselection;
                            }
                            if (si.getChildren().size() > 0) {
                                DownloadTabActionUtils.deleteLinksRequest(si, _GUI.T.GenericDeleteFromDownloadlistAction_actionPerformed_ask_(createName()), getDeleteMode(), byPass.isBypassDialog());
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
            } else if (lselection.getController() instanceof LinkCollector) {
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                    @Override
                    protected Void run() throws RuntimeException {
                        final List<CrawledLink> nodesToDelete = new ArrayList<CrawledLink>();
                        boolean containsOnline = false;
                        for (final Object l : lselection.getChildren()) {
                            final CrawledLink dl = (CrawledLink) l;
                            final CrawledPackage parentNode = dl.getParentNode();
                            if (parentNode != null && checkCrawledLink(dl)) {
                                nodesToDelete.add(dl);
                                if ((TYPE.OFFLINE == parentNode.getType() || TYPE.POFFLINE == parentNode.getType())) {
                                    continue;
                                }
                                if (dl.getDownloadLink().getAvailableStatus() != AvailableStatus.FALSE) {
                                    containsOnline = true;
                                }
                            }
                        }
                        if (nodesToDelete.size() > 0) {
                            LinkCollector.requestDeleteLinks(nodesToDelete, containsOnline, createName(), byPass.isBypassDialog(), false, false, false, false);
                        }
                        return null;
                    }
                });
            }
        }
    }

    public boolean checkCrawledLink(CrawledLink cl) {
        if (isDeleteAll()) {
            return true;
        } else if (isDeleteDisabled() && !cl.isEnabled()) {
            return true;
        } else if (isDeleteOffline() && cl.getDownloadLink().isAvailabilityStatusChecked() && cl.getDownloadLink().getAvailableStatus() == AvailableStatus.FALSE) {
            return true;
        } else {
            return false;
        }
    }

    public boolean checkDownloadLink(DownloadLink link) {
        if (isDeleteAll()) {
            return true;
        } else if (isDeleteDisabled() && !link.isEnabled()) {
            return true;
        } else if (isDeleteFailed() && FinalLinkState.CheckFailed(link.getFinalLinkState())) {
            return true;
        } else if (isDeleteFinished() && FinalLinkState.CheckFinished(link.getFinalLinkState())) {
            return true;
        } else if (isDeleteOffline() && link.getFinalLinkState() == FinalLinkState.OFFLINE) {
            return true;
        } else {
            return false;
        }
    }

    private String createName() {
        final StringBuilder sb = new StringBuilder();
        if (isDeleteAll()) {
            if (isOnlySelectedItems()) {
                sb.append(_GUI.T.GenericDeleteSelectedToolbarAction_updateName_object_selected_all());
            } else {
                sb.append(_GUI.T.GenericDeleteSelectedToolbarAction_updateName_object_all());
            }
        } else {
            if (isOnlySelectedItems()) {
                sb.append(_GUI.T.GenericDeleteSelectedToolbarAction_updateName_object_selected());
            } else {
                sb.append(_GUI.T.GenericDeleteSelectedToolbarAction_updateName_object());
            }
            boolean first = true;
            if (isDeleteDisabled()) {
                if (!first) {
                    sb.append(" & ");
                }
                sb.append(_GUI.T.lit_disabled());
                first = false;
            }
            if (isDeleteFailed()) {
                if (!first) {
                    sb.append(" & ");
                }
                first = false;
                sb.append(_GUI.T.lit_failed());
            }
            if (isDeleteFinished()) {
                if (!first) {
                    sb.append(" & ");
                }
                first = false;
                sb.append(_GUI.T.lit_finished());
            }
            if (isDeleteOffline()) {
                if (!first) {
                    sb.append(" & ");
                }
                first = false;
                sb.append(_GUI.T.lit_offline());
            }
        }
        return sb.toString();
    }

    @Override
    protected String createTooltip() {
        return getName();
    }

    @Customizer(link = "#getTranslationForDeleteAll")
    public boolean isDeleteAll() {
        return deleteAll;
    }

    @Customizer(link = "#getTranslationForDeleteDisabled")
    public boolean isDeleteDisabled() {
        return deleteDisabled;
    }

    @Customizer(link = "#getTranslationForDeleteFailed")
    public boolean isDeleteFailed() {
        return deleteFailed;
    }

    @Customizer(link = "#getTranslationForDeleteFinished")
    public boolean isDeleteFinished() {
        return deleteFinished;
    }

    @Customizer(link = "#getTranslationForDeleteOffline")
    public boolean isDeleteOffline() {
        return deleteOffline;
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled();
    }

    @Customizer(link = "#getTranslationForIgnoreFiltered")
    public boolean isIgnoreFiltered() {
        return ignoreFiltered;
    }

    @Customizer(link = "#getTranslationForOnlySelectedItems")
    public boolean isOnlySelectedItems() {
        return onlySelectedItems;
    }

    @Override
    public void onExtTableEvent(ExtTableEvent<?> event) {
        if (event.getType() == ExtTableEvent.Types.SELECTION_CHANGED) {
            update();
        }
    }

    @Override
    public void onExtTableModelEvent(ExtTableModelEventWrapper listener) {
        delayer.resetAndStart();
    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {
        update();
    }

    @Override
    public void onKeyModifier(int parameter) {
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        update();
    }

    public void setDeleteAll(final boolean deleteIdle) {
        GenericDeleteFromTableToolbarAction.this.deleteAll = deleteIdle;
        update();
        updateName();
        delayer.resetAndStart();
    }

    public void setDeleteDisabled(final boolean deleteDisabled) {
        GenericDeleteFromTableToolbarAction.this.deleteDisabled = deleteDisabled;
        update();
        updateName();
        delayer.resetAndStart();
    }

    public void setDeleteFailed(final boolean deleteFailed) {
        GenericDeleteFromTableToolbarAction.this.deleteFailed = deleteFailed;
        update();
        updateName();
        delayer.resetAndStart();
    }

    public void setDeleteFinished(final boolean deleteFinished) {
        GenericDeleteFromTableToolbarAction.this.deleteFinished = deleteFinished;
        update();
        updateName();
        delayer.resetAndStart();
    }

    public void setDeleteOffline(final boolean deleteOffline) {
        GenericDeleteFromTableToolbarAction.this.deleteOffline = deleteOffline;
        update();
        updateName();
        delayer.resetAndStart();
    }

    public void setIgnoreFiltered(final boolean ignoreFiltered) {
        GenericDeleteFromTableToolbarAction.this.ignoreFiltered = ignoreFiltered;
        updateName();
        delayer.resetAndStart();
    }

    public void setOnlySelectedItems(final boolean onlySelectedItems) {
        GenericDeleteFromTableToolbarAction.this.onlySelectedItems = onlySelectedItems;
        update();
        updateName();
        delayer.resetAndStart();
    }

    protected SelectionInfo<?, ?> fetchSelectionInfo(PackageControllerTable table) {
        if (table == null) {
            return null;
        } else if (isOnlySelectedItems()) {
            return table.getSelectionInfo(true, true);
        } else {
            if (isIgnoreFiltered()) {
                return table.getSelectionInfo(false, true);
            } else {
                return table.getSelectionInfo(false, false);
            }
        }
    }

    protected void update() {
        if (lastDownloadLink != null && lastCrawledLink != null) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    final View newView = MainTabbedPane.getInstance().getSelectedView();
                    if (newView instanceof LinkGrabberView) {
                        addListener(LinkGrabberTable.getInstance());
                        removeListener(DownloadsTable.getInstance());
                        final CrawledLink lastCl = lastCrawledLink.get();
                        final SelectionInfo<?, ?> selectionInfo = fetchSelectionInfo(LinkGrabberTable.getInstance());
                        selection = new WeakReference<SelectionInfo<?, ?>>(selectionInfo);
                        if (lastCl != null && selectionInfo.contains(lastCl)) {
                            if (checkCrawledLink(lastCl)) {
                                setEnabled(true);
                                return;
                            }
                        }
                        for (Object link : selectionInfo.getChildren()) {
                            if (checkCrawledLink((CrawledLink) link)) {
                                setEnabled(true);
                                lastCrawledLink = new WeakReference<CrawledLink>((CrawledLink) link);
                                return;
                            }
                        }
                        setEnabled(false);
                    } else if (newView instanceof DownloadsView) {
                        addListener(DownloadsTable.getInstance());
                        removeListener(LinkGrabberTable.getInstance());
                        final SelectionInfo<?, ?> selectionInfo = fetchSelectionInfo(DownloadsTable.getInstance());
                        selection = new WeakReference<SelectionInfo<?, ?>>(selectionInfo);
                        if (isDeleteAll() && !selectionInfo.isEmpty()) {
                            setEnabled(true);
                            return;
                        }
                        final DownloadLink lastDl = lastDownloadLink.get();
                        if (lastDl != null && selectionInfo.contains(lastDl)) {
                            if (checkDownloadLink(lastDl)) {
                                setEnabled(true);
                                return;
                            }
                        }
                        for (Object link : selectionInfo.getChildren()) {
                            if (checkDownloadLink((DownloadLink) link)) {
                                lastDownloadLink = new WeakReference<DownloadLink>((DownloadLink) link);
                                setEnabled(true);
                                return;
                            }
                        }
                        setEnabled(false);
                    } else {
                        selection = new WeakReference<SelectionInfo<?, ?>>(null);
                        removeListener(LinkGrabberTable.getInstance());
                        removeListener(DownloadsTable.getInstance());
                        setEnabled(false);
                    }
                }
            };
        }
    }

    private void addListener(PackageControllerTable<?, ?> table) {
        table.getEventSender().addListener(GenericDeleteFromTableToolbarAction.this, true);
        table.getModel().getEventSender().addListener(GenericDeleteFromTableToolbarAction.this, true);
        if (table.getController() instanceof DownloadController) {
            DownloadController.getInstance().addListener(this, true);
        } else if (table.getController() instanceof LinkCollector) {
            LinkCollector.getInstance().getEventsender().addListener(this, true);
        }
    }

    private void removeListener(PackageControllerTable<?, ?> table) {
        table.getEventSender().removeListener(GenericDeleteFromTableToolbarAction.this);
        table.getModel().getEventSender().removeListener(GenericDeleteFromTableToolbarAction.this);
        if (table.getController() instanceof DownloadController) {
            DownloadController.getInstance().removeListener(this);
        } else if (table.getController() instanceof LinkCollector) {
            LinkCollector.getInstance().getEventsender().removeListener(this);
        }
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
    public void onLinkCollectorAbort(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
        delayer.resetAndStart();
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
        delayer.resetAndStart();
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
        delayer.resetAndStart();
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
        delayer.resetAndStart();
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink link) {
        delayer.resetAndStart();
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink link) {
    }

    @Override
    public void onLinkCrawlerAdded(LinkCollectorCrawler crawler) {
    }

    @Override
    public void onLinkCrawlerStarted(LinkCollectorCrawler crawler) {
    }

    @Override
    public void onLinkCrawlerStopped(LinkCollectorCrawler crawler) {
    }

    @Override
    public void onLinkCrawlerFinished() {
    }

    @Override
    public void onLinkCrawlerNewJob(LinkCollectingJob job) {
    }

    @Override
    public void onDownloadControllerAddedPackage(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg) {
    }
}
