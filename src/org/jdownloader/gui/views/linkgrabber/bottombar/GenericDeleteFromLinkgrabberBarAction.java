package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.exttable.ExtTableEvent;
import org.appwork.swing.exttable.ExtTableListener;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableSelectionAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;

public class GenericDeleteFromLinkgrabberBarAction extends CustomizableSelectionAppAction implements ExtTableListener, LinkCollectorListener, ActionContext {
    public static final String                         ONLY_SELECTED_ITEMS = "onlySelectedItems";
    /**
     * 
     */
    private static final long                          serialVersionUID    = 1L;
    public static final String                         DELETE_ALL          = "deleteAll";
    public static final String                         DELETE_DISABLED     = "deleteDisabled";
    public static final String                         DELETE_OFFLINE      = "deleteOffline";

    private boolean                                    deleteDisabled      = false;
    private boolean                                    onlySelectedItems   = false;

    private boolean                                    deleteAll           = false;

    private boolean                                    ignoreFiltered      = true;

    private boolean                                    deleteOffline       = false;

    private DelayedRunnable                            delayer;
    private SelectionInfo<CrawledPackage, CrawledLink> selection;
    private CrawledLink                                lastLink;

    public GenericDeleteFromLinkgrabberBarAction() {
        super();
        this.setIconKey(IconKey.ICON_DELETE);
        delayer = new DelayedRunnable(200, 500) {

            @Override
            public void delayedrun() {
                update();
            }
        };
        LinkCollector.getInstance().getEventsender().addListener(this, true);
        update();

    }

    @Override
    public void actionPerformed(final ActionEvent e) {

        final List<CrawledLink> nodesToDelete = new ArrayList<CrawledLink>();
        boolean containsOnline = false;
        for (final CrawledLink dl : selection.getChildren()) {
            if (checkLink(dl)) {
                nodesToDelete.add(dl);

                if (TYPE.OFFLINE == dl.getParentNode().getType()) continue;
                if (TYPE.POFFLINE == dl.getParentNode().getType()) continue;
                if (dl.getDownloadLink().getAvailableStatus() != AvailableStatus.FALSE) {
                    containsOnline = true;
                    break;
                }
            }
        }
        LinkCollector.requestDeleteLinks(nodesToDelete, containsOnline, createName());
    }

    private LinkGrabberTable getTable() {
        return (LinkGrabberTable) LinkGrabberTableModel.getInstance().getTable();
    }

    @Customizer(name = "Include All Links")
    public boolean isDeleteAll() {
        return this.deleteAll;
    }

    @Customizer(name = "Include disabled Links")
    public boolean isDeleteDisabled() {
        return this.deleteDisabled;
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

    public void setDeleteAll(final boolean deleteIdle) {
        this.deleteAll = deleteIdle;
        this.updateName();
        update();
    }

    public void setDeleteDisabled(final boolean deleteDisabled) {
        this.deleteDisabled = deleteDisabled;
        this.updateName();
        update();
    }

    public void setDeleteOffline(final boolean deleteOffline) {
        this.deleteOffline = deleteOffline;
        this.updateName();
        update();
    }

    public void setIgnoreFiltered(final boolean ignoreFiltered) {
        this.ignoreFiltered = ignoreFiltered;
        this.updateName();
        update();
    }

    public void setOnlySelectedItems(final boolean onlySelectedItems) {
        this.onlySelectedItems = onlySelectedItems;

        if (onlySelectedItems) {
            getTable().getEventSender().addListener(this, true);
            LinkCollector.getInstance().getEventsender().removeListener(this);
        } else {
            getTable().getEventSender().removeListener(this);
            LinkCollector.getInstance().getEventsender().addListener(this, true);
        }
        this.updateName();
        update();
    }

    private void update() {

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

    private void updateName() {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setName(createName());
            }
        };

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

        if (this.isDeleteAll()) {
            if (this.isOnlySelectedItems()) {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_selected_all());
            } else {
                sb.append(_GUI._.GenericDeleteFromLinkgrabberAction_createName_updateName_object_all());
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
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
        delayer.resetAndStart();
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
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
    public void onExtTableEvent(ExtTableEvent<?> event) {
        if (event.getType() == ExtTableEvent.Types.SELECTION_CHANGED) {
            update();
        }
    }

}
