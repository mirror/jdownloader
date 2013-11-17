package org.jdownloader.gui.toolbar.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.KeyStroke;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
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
import org.jdownloader.gui.views.downloads.action.DownloadTabActionUtils;
import org.jdownloader.gui.views.downloads.action.Modifier;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.DeleteFileOptions;

public class GenericDeleteFromTableToolbarAction extends AbstractToolBarAction implements ExtTableListener, DownloadControllerListener, GUIListener, LinkCollectorListener, ActionContext {

    public static final String DELETE_ALL       = "deleteAll";
    public static final String DELETE_DISABLED  = "deleteDisabled";
    public static final String DELETE_FAILED    = "deleteFailed";
    public static final String DELETE_FINISHED  = "deleteFinished";
    public static final String DELETE_OFFLINE   = "deleteOffline";
    /**
     * 
     */
    private static final long  serialVersionUID = 1L;

    private DelayedRunnable    delayer;
    private boolean            deleteAll        = false;

    private boolean            deleteDisabled   = false;

    private boolean            deleteFailed     = false;

    private boolean            deleteFinished   = false;

    private boolean            deleteOffline    = false;

    private boolean            ignoreFiltered   = true;
    private boolean            bypassDialog     = false;

    @Customizer(name = "Bypass the 'Really?' Dialog")
    public boolean isBypassDialog() {
        Modifier byPassDialog = getByPassDialogToggleModifier();

        if (byPassDialog != null && KeyObserver.getInstance().isModifierPressed(byPassDialog.getModifier(), false)) { return !bypassDialog; }

        return bypassDialog;
    }

    public void setBypassDialog(boolean bypassDialog) {
        this.bypassDialog = bypassDialog;
    }

    private CrawledLink                  lastCrawledLink;
    private DownloadLink                 lastDownloadLink;

    private boolean                      onlySelectedItems = false;
    private SelectionInfo<?, ?>          selection;
    private PackageControllerTable<?, ?> table;

    public GenericDeleteFromTableToolbarAction() {
        super();

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

    private Modifier byPassDialogToggleModifier = null;
    private Modifier deleteFilesToggleModifier  = null;

    @Customizer(name = "Key Modifier to toggle 'Bypass Rly? Dialog'")
    public Modifier getByPassDialogToggleModifier() {
        return byPassDialogToggleModifier;
    }

    public void setByPassDialogToggleModifier(Modifier byPassDialogToggleModifier) {
        this.byPassDialogToggleModifier = byPassDialogToggleModifier;
    }

    @Customizer(name = "Key Modifier to toggle 'Delete Files'")
    public Modifier getDeleteFilesToggleModifier() {
        return deleteFilesToggleModifier;
    }

    public void setDeleteFilesToggleModifier(Modifier deleteFilesToggleModifier) {
        this.deleteFilesToggleModifier = deleteFilesToggleModifier;
    }

    public List<KeyStroke> getAdditionalShortcuts(KeyStroke keystroke) {
        if (keystroke == null) return null;

        ArrayList<KeyStroke> ret = new ArrayList<KeyStroke>();
        Modifier mod = getByPassDialogToggleModifier();
        if (mod != null) {
            ret.add(KeyStroke.getKeyStroke(keystroke.getKeyCode(), keystroke.getModifiers() | mod.getModifier()));
        }

        mod = getDeleteFilesToggleModifier();
        if (mod != null) {
            ret.add(KeyStroke.getKeyStroke(keystroke.getKeyCode(), keystroke.getModifiers() | mod.getModifier()));
        }
        return ret;
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

    private DeleteFileOptions deleteMode;

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (table == null) return;
        if (table instanceof DownloadsTable) {
            final List<DownloadLink> nodesToDelete = new ArrayList<DownloadLink>();
            for (final Object dl : selection.getChildren()) {
                if (checkDownloadLink((DownloadLink) dl)) {
                    nodesToDelete.add((DownloadLink) dl);
                }
            }
            if (nodesToDelete.size() > 0) {
                final SelectionInfo<FilePackage, DownloadLink> si = new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete, null, null, e, (DownloadsTable) table);
                if (si.getChildren().size() > 0) {

                    DownloadTabActionUtils.deleteLinksRequest(si, _GUI._.GenericDeleteFromDownloadlistAction_actionPerformed_ask_(createName()), getDeleteMode(), isBypassDialog());
                    return;
                }
            }

            Toolkit.getDefaultToolkit().beep();
            Dialog.getInstance().showErrorDialog(_GUI._.GenericDeleteSelectedToolbarAction_actionPerformed_nothing_to_delete_());

        } else if (table instanceof LinkGrabberTable) {
            final List<CrawledLink> nodesToDelete = new ArrayList<CrawledLink>();
            boolean containsOnline = false;
            for (final Object l : selection.getChildren()) {
                CrawledLink dl = (CrawledLink) l;
                if (checkCrawledLink(dl)) {
                    nodesToDelete.add(dl);

                    if (TYPE.OFFLINE == dl.getParentNode().getType()) continue;
                    if (TYPE.POFFLINE == dl.getParentNode().getType()) continue;
                    if (dl.getDownloadLink().getAvailableStatus() != AvailableStatus.FALSE) {
                        containsOnline = true;

                    }
                }
            }
            LinkCollector.requestDeleteLinks(nodesToDelete, containsOnline, createName());
        }
    }

    public boolean checkCrawledLink(CrawledLink cl) {
        if (isDeleteAll()) return true;
        if (isDeleteDisabled() && !cl.isEnabled()) {

        return true; }

        if (isDeleteOffline() && cl.getDownloadLink().isAvailabilityStatusChecked() && cl.getDownloadLink().getAvailableStatus() == AvailableStatus.FALSE) {

        return true; }
        return false;
    }

    public boolean checkDownloadLink(DownloadLink link) {
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
    protected String createTooltip() {
        return getName();
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

    @Customizer(name = "Only Selected Links")
    public boolean isOnlySelectedItems() {
        return onlySelectedItems;
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
    public void onGuiMainTabSwitch(View oldView, View newView) {
        updateListeners();
    }

    @Override
    public void onKeyModifier(int parameter) {
    }

    @Override
    public void onLinkCollectorAbort(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
        delayer.resetAndStart();
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
        delayer.resetAndStart();
    }

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
        delayer.resetAndStart();
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
        delayer.resetAndStart();
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
        delayer.resetAndStart();
    }

    @Override
    public void onLinkCrawlerAdded(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerStarted(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerStopped(LinkCollectorCrawler parameter) {
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        update();
    }

    public void setDeleteAll(final boolean deleteIdle) {
        GenericDeleteFromTableToolbarAction.this.deleteAll = deleteIdle;
        updateName();
        delayer.resetAndStart();
    }

    public void setDeleteDisabled(final boolean deleteDisabled) {
        GenericDeleteFromTableToolbarAction.this.deleteDisabled = deleteDisabled;
        updateName();
        delayer.resetAndStart();
    }

    public void setDeleteFailed(final boolean deleteFailed) {
        GenericDeleteFromTableToolbarAction.this.deleteFailed = deleteFailed;
        updateName();
        delayer.resetAndStart();
    }

    public void setDeleteFinished(final boolean deleteFinished) {
        GenericDeleteFromTableToolbarAction.this.deleteFinished = deleteFinished;
        updateName();
        delayer.resetAndStart();
    }

    public void setDeleteOffline(final boolean deleteOffline) {
        GenericDeleteFromTableToolbarAction.this.deleteOffline = deleteOffline;
        updateName();
        delayer.resetAndStart();
    }

    @Override
    public void setEnabled(boolean newValue) {

        super.setEnabled(newValue);
    }

    public void setIgnoreFiltered(final boolean ignoreFiltered) {
        GenericDeleteFromTableToolbarAction.this.ignoreFiltered = ignoreFiltered;
        updateName();
        delayer.resetAndStart();
    }

    public void setOnlySelectedItems(final boolean onlySelectedItems) {
        GenericDeleteFromTableToolbarAction.this.onlySelectedItems = onlySelectedItems;
        updateListeners();
        updateName();
        delayer.resetAndStart();
    }

    protected void update() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (table == null) {
                    selection = null;
                    return;
                }
                if (isOnlySelectedItems()) {
                    selection = (table.getSelectionInfo(true, true));

                } else {
                    if (isIgnoreFiltered()) {
                        selection = table.getSelectionInfo(false, true);

                    } else {

                        selection = table.getSelectionInfo(false, false);
                    }

                }
                if (table instanceof DownloadsTable) {
                    // we remember the last link and try it first
                    if (lastDownloadLink != null && selection.contains(lastDownloadLink)) {
                        if (checkDownloadLink(lastDownloadLink)) {
                            setEnabled(true);
                            //
                            return;
                        }
                    }
                    if (isDeleteAll() && !selection.isEmpty()) {
                        setEnabled(true);
                        return;

                    }
                    for (Object link : selection.getChildren()) {
                        if (checkDownloadLink((DownloadLink) link)) {
                            lastDownloadLink = (DownloadLink) link;
                            setEnabled(true);
                            return;
                        }
                    }
                    setEnabled(false);
                } else if (table instanceof LinkGrabberTable) {

                    // we remember the last link and try it first
                    if (lastCrawledLink != null && selection.contains(lastCrawledLink)) {
                        if (checkCrawledLink(lastCrawledLink)) {

                            setEnabled(true);
                            return;
                        }
                    }

                    for (Object link : selection.getChildren()) {
                        if (checkCrawledLink((CrawledLink) link)) {
                            setEnabled(true);
                            lastCrawledLink = (CrawledLink) link;
                            return;
                        }
                    }
                    setEnabled(false);
                }
            }

            /**
             * @param link
             * @return
             */

        };

    }

    private void updateListeners() {

        LinkGrabberTableModel.getInstance().getTable().getEventSender().removeListener(this);
        DownloadsTableModel.getInstance().getTable().getEventSender().removeListener(this);
        View newView = MainTabbedPane.getInstance().getSelectedView();
        if (newView instanceof LinkGrabberView) {
            table = LinkGrabberTableModel.getInstance().getTable();
            if (isOnlySelectedItems()) {
                LinkGrabberTable.getInstance().getEventSender().addListener(GenericDeleteFromTableToolbarAction.this, true);
                DownloadsTable.getInstance().getEventSender().removeListener(this);
                LinkCollector.getInstance().getEventsender().removeListener(GenericDeleteFromTableToolbarAction.this);
            } else {
                LinkGrabberTable.getInstance().getEventSender().removeListener(this);
                DownloadsTable.getInstance().getEventSender().removeListener(this);
                LinkCollector.getInstance().getEventsender().addListener(GenericDeleteFromTableToolbarAction.this, true);
            }
            update();
        } else if (newView instanceof DownloadsView) {
            table = DownloadsTableModel.getInstance().getTable();
            if (isOnlySelectedItems()) {
                DownloadsTable.getInstance().getEventSender().addListener(GenericDeleteFromTableToolbarAction.this, true);
                LinkGrabberTable.getInstance().getEventSender().removeListener(this);
                DownloadController.getInstance().getEventSender().removeListener(GenericDeleteFromTableToolbarAction.this);
            } else {
                DownloadsTable.getInstance().getEventSender().removeListener(this);
                LinkGrabberTable.getInstance().getEventSender().removeListener(this);
                DownloadController.getInstance().getEventSender().addListener(GenericDeleteFromTableToolbarAction.this, true);
            }
        } else {
            table = null;
            setEnabled(false);
        }

        update();

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

}
