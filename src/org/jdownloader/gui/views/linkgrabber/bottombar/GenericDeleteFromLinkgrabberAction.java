package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.TaskQueue;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.exttable.ExtTableEvent;
import org.appwork.swing.exttable.ExtTableListener;
import org.appwork.swing.exttable.ExtTableModelEventWrapper;
import org.appwork.swing.exttable.ExtTableModelListener;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkgrabberSearchField;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MenuManagerLinkgrabberTableContext;

public class GenericDeleteFromLinkgrabberAction extends CustomizableAppAction implements ExtTableListener, ActionContext, ExtTableModelListener {

    public static final String FULL_LINK_COLLECTOR_RESET = "fullLinkCollectorReset";
    public static final String CLEAR_FILTERED_LINKS      = "clearFilteredLinks";
    public static final String CLEAR_SEARCH_FILTER       = "clearSearchFilter";
    public static final String RESET_TABLE_SORTER        = "resetTableSorter";
    public static final String CANCEL_LINKCRAWLER_JOBS   = "cancelLinkcrawlerJobs";
    public static final String INCLUDE_UNSELECTED_LINKS  = "includeUnselectedLinks";
    public static final String INCLUDE_SELECTED_LINKS    = "includeSelectedLinks";
    public static final String ONLY_SELECTED_ITEMS       = "onlySelectedItems";
    public static final String DELETE_ALL                = "deleteAll";
    public static final String DELETE_DISABLED           = "deleteDisabled";
    public static final String DELETE_OFFLINE            = "deleteOffline";
    /**
     * 
     */
    private static final long  serialVersionUID          = 1L;

    private DelayedRunnable    delayer;
    private boolean            deleteAll                 = false;

    private boolean            deleteDisabled            = false;

    private boolean            deleteOffline             = false;
    private boolean            cancelLinkcrawlerJobs     = false;
    private boolean            resetTableSorter          = false;
    private boolean            fullLinkCollectorReset    = false;

    @Customizer(name = "Full Reset")
    public boolean isFullLinkCollectorReset() {

        return fullLinkCollectorReset;
    }

    public void setFullLinkCollectorReset(boolean fullLinkCollectorReset) {
        this.fullLinkCollectorReset = fullLinkCollectorReset;
    }

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

    @Customizer(name = "Cancel all running Crawler Jobs")
    public boolean isCancelLinkcrawlerJobs() {

        return cancelLinkcrawlerJobs;
    }

    public void setCancelLinkcrawlerJobs(boolean cancelLinkcrawlerJobs) {
        this.cancelLinkcrawlerJobs = cancelLinkcrawlerJobs;
    }

    @Customizer(name = "Reset Table Sorting")
    public boolean isResetTableSorter() {

        return resetTableSorter;
    }

    public void setResetTableSorter(boolean resetTableSorter) {
        this.resetTableSorter = resetTableSorter;
    }

    @Customizer(name = "Clear Searchfield")
    public boolean isClearSearchFilter() {

        return clearSearchFilter;
    }

    public void setClearSearchFilter(boolean clearSearchFilter) {
        this.clearSearchFilter = clearSearchFilter;
    }

    @Customizer(name = "Clear Filtered Links")
    public boolean isClearFilteredLinks() {

        return clearFilteredLinks;
    }

    public void setClearFilteredLinks(boolean clearFilteredLinks) {
        this.clearFilteredLinks = clearFilteredLinks;
    }

    private boolean                                      clearSearchFilter  = false;
    private boolean                                      clearFilteredLinks = false;
    //

    private boolean                                      ignoreFiltered     = true;

    private CrawledLink                                  lastLink;

    protected SelectionInfo<CrawledPackage, CrawledLink> selection;

    public GenericDeleteFromLinkgrabberAction() {
        super();

        setIconKey(IconKey.ICON_DELETE);
        delayer = new DelayedRunnable(500, 1500) {

            @Override
            public void delayedrun() {
                update();
            }
        };

    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final List<CrawledLink> nodesToDelete = new ArrayList<CrawledLink>();
        boolean containsOnline = false;

        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {

                if (isCancelLinkcrawlerJobs()) {
                    LinkCollector.getInstance().abort();
                }

                if (isResetTableSorter()) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            final LinkGrabberTable table = MenuManagerLinkgrabberTableContext.getInstance().getTable();
                            table.getModel().setSortColumn(null);
                            table.getModel().refreshSort();
                            table.getTableHeader().repaint();
                        }
                    };
                }
                if (isClearSearchFilter()) {

                    LinkgrabberSearchField.getInstance().setText("");
                    LinkgrabberSearchField.getInstance().onChanged();

                }

                if (isFullLinkCollectorReset()) {
                    LinkCollector.getInstance().clear();
                } else {
                    if (isClearFilteredLinks()) {
                        LinkCollector.getInstance().clearFilteredLinks();
                    }
                }

                return null;
            }
        });

        switch (getSelectionType()) {
        case NONE:
            break;
        case UNSELECTED:
            final List<PackageControllerTableModelFilter<CrawledPackage, CrawledLink>> filters = LinkGrabberTableModel.getInstance().getTableFilters();

            LinkCollector.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {

                @Override
                public int returnMaxResults() {
                    return 0;
                }

                @Override
                public boolean acceptNode(CrawledLink node) {
                    if (!selection.contains(node)) {

                        if (isIgnoreFiltered()) {
                            for (PackageControllerTableModelFilter<CrawledPackage, CrawledLink> filter : filters) {
                                if (filter.isFiltered(node)) { return false; }
                            }
                        }
                        nodesToDelete.add(node);
                    }
                    return false;
                }
            });
            break;
        default:
            for (final CrawledLink dl : selection.getChildren()) {
                if (checkLink(dl)) {
                    nodesToDelete.add(dl);

                    if (TYPE.OFFLINE == dl.getParentNode().getType()) continue;
                    if (TYPE.POFFLINE == dl.getParentNode().getType()) continue;
                    if (dl.getDownloadLink().getAvailableStatus() != AvailableStatus.FALSE) {
                        containsOnline = true;

                    }
                }
            }
        }

        LinkCollector.requestDeleteLinks(nodesToDelete, containsOnline, createName());

    }

    public boolean checkLink(CrawledLink cl) {
        if (isDeleteAll()) return true;
        if (isDeleteDisabled() && !cl.isEnabled()) {

        return true; }

        if (isDeleteOffline() && cl.getDownloadLink().isAvailabilityStatusChecked() && cl.getDownloadLink().getAvailableStatus() == AvailableStatus.FALSE) {

        return true; }
        return false;
    }

    private String createName() {
        final StringBuilder sb = new StringBuilder();
        if (isFullLinkCollectorReset()) {
            sb.append(_GUI._.GenericDeleteFromLinkgrabberAction_fullreset());
        } else {
            if (isClearFilteredLinks()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(_GUI._.GenericDeleteFromLinkgrabberAction_clearFiltered());
            }

        }
        if (isCancelLinkcrawlerJobs()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(_GUI._.GenericDeleteFromLinkgrabberAction_cancelCrawler());
        }
        if (isClearSearchFilter()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(_GUI._.GenericDeleteFromLinkgrabberAction_clearSearch());
        }

        if (isResetTableSorter()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(_GUI._.GenericDeleteFromLinkgrabberAction_resetSorter());
        }
        if (sb.length() > 0) sb.append(" & ");
        if (this.isDeleteAll()) {
            switch (getSelectionType()) {
            case ALL:
                sb.append(_GUI._.GenericDeleteFromLinkgrabberAction_createName_updateName_object_all());
                break;
            case SELECTED:
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_selected_all());
                break;
            case UNSELECTED:
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_keep_selected_all());
                break;

            case NONE:
                sb.append("Bad Action Setup");
            }

        } else {

            switch (getSelectionType()) {
            case ALL:
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object());
                break;
            case SELECTED:
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_selected());
                break;
            case UNSELECTED:
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_keep_selected_selected());
                break;

            case NONE:
                sb.append("Bad Action Setup");
            }

            boolean first = true;

            if (this.isDeleteDisabled()) {
                if (!first) {
                    sb.append(" & ");
                }

                sb.append(_GUI._.lit_disabled());
                first = false;
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

    public enum SelectionType {
        SELECTED,
        UNSELECTED,
        ALL,
        NONE;
    }

    protected SelectionType getSelectionType() {
        if (isIncludeSelectedLinks() && isIncludeUnselectedLinks()) return SelectionType.ALL;
        if (isIncludeSelectedLinks()) return SelectionType.SELECTED;
        if (isIncludeUnselectedLinks()) return SelectionType.UNSELECTED;
        return SelectionType.NONE;
    }

    @Customizer(name = "Delete all")
    public boolean isDeleteAll() {
        return deleteAll;
    }

    @Customizer(name = "Delete disabled")
    public boolean isDeleteDisabled() {
        return deleteDisabled;
    }

    @Customizer(name = "Delete Offline")
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
    public void onExtTableEvent(ExtTableEvent<?> event) {
        if (event.getType() == ExtTableEvent.Types.SELECTION_CHANGED) {
            update();
        }
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        updateListeners();
        update();

    }

    public void setDeleteAll(final boolean deleteIdle) {
        GenericDeleteFromLinkgrabberAction.this.deleteAll = deleteIdle;

    }

    public void setDeleteDisabled(final boolean deleteDisabled) {
        GenericDeleteFromLinkgrabberAction.this.deleteDisabled = deleteDisabled;

    }

    public void setDeleteOffline(final boolean deleteOffline) {
        GenericDeleteFromLinkgrabberAction.this.deleteOffline = deleteOffline;

    }

    public void setIgnoreFiltered(final boolean ignoreFiltered) {
        GenericDeleteFromLinkgrabberAction.this.ignoreFiltered = ignoreFiltered;

    }

    private boolean includeSelectedLinks = true; ;

    @Customizer(name = "Include Selected Links")
    public boolean isIncludeSelectedLinks() {
        return includeSelectedLinks;
    }

    public void setIncludeSelectedLinks(boolean includeSelectedLinks) {

        this.includeSelectedLinks = includeSelectedLinks;
        updateListeners();

    }

    @Customizer(name = "Include Unselected Links")
    public boolean isIncludeUnselectedLinks() {
        return includeUnselectedLinks;
    }

    public void setIncludeUnselectedLinks(boolean includeUnselectedLinks) {

        this.includeUnselectedLinks = includeUnselectedLinks;
        updateListeners();

    }

    private boolean includeUnselectedLinks = false;

    protected void updateListeners() {

        switch (getSelectionType()) {
        case ALL:
            LinkGrabberTable.getInstance().getEventSender().removeListener(GenericDeleteFromLinkgrabberAction.this);
            LinkGrabberTableModel.getInstance().getEventSender().addListener(GenericDeleteFromLinkgrabberAction.this, true);
            break;
        case SELECTED:
        case UNSELECTED:
            LinkGrabberTable.getInstance().getEventSender().addListener(GenericDeleteFromLinkgrabberAction.this, true);

            LinkGrabberTableModel.getInstance().getEventSender().removeListener(GenericDeleteFromLinkgrabberAction.this);
            break;

        case NONE:
            LinkGrabberTable.getInstance().getEventSender().removeListener(GenericDeleteFromLinkgrabberAction.this);
            LinkGrabberTableModel.getInstance().getEventSender().removeListener(GenericDeleteFromLinkgrabberAction.this);
        }

    }

    protected void update() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                switch (getSelectionType()) {

                case SELECTED:
                    selection = LinkGrabberTable.getInstance().getSelectionInfo();
                    break;
                case UNSELECTED:
                    selection = LinkGrabberTable.getInstance().getSelectionInfo();

                    setVisible(true);

                    if (lastLink != null && !selection.contains(lastLink)) {
                        if (checkLink(lastLink)) {

                            setEnabled(true);
                            return;
                        }
                    }

                    final List<PackageControllerTableModelFilter<CrawledPackage, CrawledLink>> filters = LinkGrabberTableModel.getInstance().getTableFilters();

                    boolean read = LinkCollector.getInstance().readLock();
                    try {
                        for (CrawledPackage pkg : LinkCollector.getInstance().getPackages()) {
                            if (selection.getFullPackages().contains(pkg)) {
                                continue;
                            }
                            boolean readL2 = pkg.getModifyLock().readLock();
                            try {
                                childs: for (CrawledLink child : pkg.getChildren()) {
                                    if (selection.contains(child)) {
                                        continue;
                                    }
                                    if (isIgnoreFiltered()) {

                                        for (PackageControllerTableModelFilter<CrawledPackage, CrawledLink> filter : filters) {
                                            if (filter.isFiltered((CrawledLink) child)) {
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
                        LinkCollector.getInstance().readUnlock(read);
                    }

                    setEnabled(false);

                    return;

                default:
                    if (isIgnoreFiltered()) {

                        selection = LinkGrabberTable.getInstance().getSelectionInfo(false, false);
                    } else {
                        selection = LinkGrabberTable.getInstance().getSelectionInfo(false, true);

                    }
                    break;
                }
                // if (!tableContext.isItemVisibleForEmptySelection() && !hasSelection()) {
                // setVisible(false);
                // setEnabled(false);
                // return;
                // }
                // if (!tableContext.isItemVisibleForSelections() && hasSelection()) {
                // setVisible(false);
                // setEnabled(false);
                // return;
                // }
                // we remember the last link and try it first
                setVisible(true);

                if (isFullLinkCollectorReset()) {
                    setEnabled(true);
                    return;
                }
                if (isCancelLinkcrawlerJobs()) {
                    setEnabled(true);
                    return;
                }
                if (isClearFilteredLinks()) {
                    setEnabled(true);
                    return;
                }

                if (isClearSearchFilter()) {
                    setEnabled(true);
                    return;
                }
                if (isResetTableSorter()) {
                    setEnabled(true);
                    return;
                }

                if (isClearSearchFilter()) {
                    setEnabled(true);
                    return;
                }
                LinkCollector.getInstance().abort();

                if (lastLink != null && selection.contains(lastLink)) {
                    if (checkLink(lastLink)) {

                        setEnabled(true);
                        return;
                    }
                }

                for (CrawledLink link : selection.getChildren()) {
                    if (checkLink(link)) {
                        setEnabled(true);
                        lastLink = link;
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
        updateName();
    }

    private void updateName() {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setName(createName());
            }
        }.getReturnValue();

    }

    @Override
    public void onExtTableModelEvent(ExtTableModelEventWrapper listener) {
        delayer.resetAndStart();
    }

}
