package org.jdownloader.gui.views.linkgrabber.overview;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.SecondLevelLaunch;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.JDGui.Panels;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.AggregatedCrawlerNumbers;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.overviewpanel.AbstractOverviewPanel;
import org.jdownloader.gui.views.downloads.overviewpanel.DataEntry;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class LinkgrabberOverview extends AbstractOverviewPanel<AggregatedCrawlerNumbers> implements GenericConfigEventListener<Boolean>, LinkCollectorListener, GUIListener {

    /**
     * 
     */
    private static final long     serialVersionUID = -195024600818162517L;
    private LinkGrabberTable      table;

    private ListSelectionListener selectionListener;
    private TableModelListener    tableListener;

    public LinkgrabberOverview(final LinkGrabberTable table) {
        super();
        this.table = table;

        CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.getEventSender().addListener(this, true);

        LinkCollector.getInstance().getEventsender().addListener(this, true);
        LinkGrabberTableModel.getInstance().addTableModelListener(tableListener = new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                slowDelayer.run();
            }
        });

        // new Timer(1000, this).start();
        table.getSelectionModel().addListSelectionListener(selectionListener = new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e == null || e.getValueIsAdjusting() || table.getModel().isTableSelectionClearing()) { return; }
                onConfigValueModified(null, null);
            }
        });

        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                visible.set(JDGui.getInstance().isCurrentPanel(Panels.LINKGRABBER));
                slowDelayer.run();
            }
        });
    }

    public void removeListeners() {
        super.removeListeners();
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_INCLUDE_DISABLED_LINKS.getEventSender().removeListener(LinkgrabberOverview.this);

                CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.getEventSender().removeListener(LinkgrabberOverview.this);
                LinkCollector.getInstance().getEventsender().removeListener(LinkgrabberOverview.this);
                LinkGrabberTableModel.getInstance().removeTableModelListener(tableListener);
                table.getSelectionModel().removeListSelectionListener(selectionListener);

            }
        };
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE == keyHandler && Boolean.FALSE.equals(newValue)) {
            removeListeners();
            visible.set(false);
        } else {
            super.onConfigValueModified(keyHandler, newValue);
        }
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
        slowDelayer.run();
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
        slowDelayer.run();
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
        slowDelayer.run();
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
        slowDelayer.run();
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
        slowDelayer.run();
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    protected boolean isActiveView(View newView) {
        return newView instanceof LinkGrabberView;
    }

    @Override
    public void onKeyModifier(int parameter) {
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
    protected List<DataEntry<AggregatedCrawlerNumbers>> createDataEntries() {

        DataEntry<AggregatedCrawlerNumbers> packageCount = new DataEntry<AggregatedCrawlerNumbers>(_GUI._.DownloadOverview_DownloadOverview_packages()) {

            @Override
            public void setData(AggregatedCrawlerNumbers total, AggregatedCrawlerNumbers filtered, AggregatedCrawlerNumbers selected) {
                if (total != null) setTotal(total.getPackageCount() + "");
                if (filtered != null) setFiltered(filtered.getPackageCount() + "");
                if (selected != null) setSelected(selected.getPackageCount() + "");
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_PACKAGE_COUNT_VISIBLE;
            }

        };
        DataEntry<AggregatedCrawlerNumbers> size = new DataEntry<AggregatedCrawlerNumbers>(_GUI._.DownloadOverview_DownloadOverview_size()) {

            @Override
            public void setData(AggregatedCrawlerNumbers total, AggregatedCrawlerNumbers filtered, AggregatedCrawlerNumbers selected) {
                if (total != null) setTotal(total.getTotalBytesString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
                if (filtered != null) setFiltered(filtered.getTotalBytesString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
                if (selected != null) setSelected(selected.getTotalBytesString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_TOTAL_BYTES_VISIBLE;
            }

        };

        DataEntry<AggregatedCrawlerNumbers> linkCount = new DataEntry<AggregatedCrawlerNumbers>(_GUI._.DownloadOverview_DownloadOverview_links()) {

            @Override
            public void setData(AggregatedCrawlerNumbers total, AggregatedCrawlerNumbers filtered, AggregatedCrawlerNumbers selected) {
                if (total != null) setTotal(total.getLinkCount() + "");
                if (filtered != null) setFiltered(filtered.getLinkCount() + "");
                if (selected != null) setSelected(selected.getLinkCount() + "");
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_LINKS_COUNT_VISIBLE;
            }

        };
        DataEntry<AggregatedCrawlerNumbers> hosterCount = new DataEntry<AggregatedCrawlerNumbers>(_GUI._.DownloadOverview_DownloadOverview_hoster()) {

            @Override
            public void setData(AggregatedCrawlerNumbers total, AggregatedCrawlerNumbers filtered, AggregatedCrawlerNumbers selected) {
                if (total != null) setTotal(total.getHoster().size() + "");
                if (filtered != null) setFiltered(total.getHoster().size() + "");
                if (selected != null) setSelected(total.getHoster().size() + "");
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_HOSTER_COUNT_VISIBLE;
            }

        };
        DataEntry<AggregatedCrawlerNumbers> onlineCount = new DataEntry<AggregatedCrawlerNumbers>(_GUI._.DownloadOverview_DownloadOverview_online()) {

            @Override
            public void setData(AggregatedCrawlerNumbers total, AggregatedCrawlerNumbers filtered, AggregatedCrawlerNumbers selected) {
                if (total != null) setTotal(total.getStatusOnline() + "");
                if (filtered != null) setFiltered(filtered.getStatusOnline() + "");
                if (selected != null) setSelected(selected.getStatusOnline() + "");
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_STATUS_ONLINE_VISIBLE;
            }

        };
        DataEntry<AggregatedCrawlerNumbers> offlineCount = new DataEntry<AggregatedCrawlerNumbers>(_GUI._.DownloadOverview_DownloadOverview_offline()) {

            @Override
            public void setData(AggregatedCrawlerNumbers total, AggregatedCrawlerNumbers filtered, AggregatedCrawlerNumbers selected) {
                if (total != null) setTotal(total.getStatusOffline() + "");
                if (filtered != null) setFiltered(filtered.getStatusOffline() + "");
                if (selected != null) setSelected(selected.getStatusOffline() + "");
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_STATUS_OFFLINE_VISIBLE;
            }

        };
        DataEntry<AggregatedCrawlerNumbers> unknownCount = new DataEntry<AggregatedCrawlerNumbers>(_GUI._.DownloadOverview_DownloadOverview_unknown()) {

            @Override
            public void setData(AggregatedCrawlerNumbers total, AggregatedCrawlerNumbers filtered, AggregatedCrawlerNumbers selected) {
                if (total != null) setTotal(total.getStatusUnknown() + "");
                if (filtered != null) setFiltered(filtered.getStatusUnknown() + "");
                if (selected != null) setSelected(selected.getStatusUnknown() + "");
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_STATUS_UNKNOWN_VISIBLE;
            }

        };

        ArrayList<DataEntry<AggregatedCrawlerNumbers>> entries = new ArrayList<DataEntry<AggregatedCrawlerNumbers>>();
        entries.add(packageCount);
        entries.add(linkCount);
        entries.add(size);
        entries.add(onlineCount);
        entries.add(hosterCount);
        entries.add(offlineCount);
        entries.add(unknownCount);

        return entries;
    }

    @Override
    protected AggregatedCrawlerNumbers createSelected() {
        return new AggregatedCrawlerNumbers(table.getSelectionInfo(true, true));
    }

    @Override
    protected AggregatedCrawlerNumbers createFiltered() {
        return new AggregatedCrawlerNumbers(table.getSelectionInfo(false, true));
    }

    @Override
    protected AggregatedCrawlerNumbers createTotal() {
        return new AggregatedCrawlerNumbers(table.getSelectionInfo(false, false));
    }

}
