package org.jdownloader.gui.views.linkgrabber.overview;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.SecondLevelLaunch;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.JDGui.Panels;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.storage.config.ValidationException;
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
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class LinkgrabberOverview extends AbstractOverviewPanel<AggregatedCrawlerNumbers> implements GenericConfigEventListener<Boolean>, LinkCollectorListener, GUIListener {

    private static final AtomicBoolean INCLUDE_DISABLED = new AtomicBoolean(false) {
                                                            {
                                                                final AtomicBoolean variable = this;
                                                                CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

                                                                    @Override
                                                                    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                                                                        variable.set(Boolean.TRUE.equals(newValue));
                                                                    }

                                                                    @Override
                                                                    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
                                                                    }
                                                                });
                                                                variable.set(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled());
                                                            }
                                                        };

    private final class UnknownCountEntry extends DataEntry<AggregatedCrawlerNumbers> {
        private UnknownCountEntry(String label) {
            super(label);
        }

        @Override
        public void setData(AggregatedCrawlerNumbers total, AggregatedCrawlerNumbers filtered, AggregatedCrawlerNumbers selected) {
            if (total != null) {
                setTotal(Long.toString(total.getStatusUnknown()));
            }
            if (filtered != null) {
                setFiltered(Long.toString(filtered.getStatusUnknown()));
            }
            if (selected != null) {
                setSelected(Long.toString(selected.getStatusUnknown()));
            }
        }

        @Override
        public BooleanKeyHandler getVisibleKeyHandler() {
            return CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_STATUS_UNKNOWN_VISIBLE;
        }
    }

    private final class OfflineCountEntry extends DataEntry<AggregatedCrawlerNumbers> {
        private OfflineCountEntry(String label) {
            super(label);
        }

        @Override
        public void setData(AggregatedCrawlerNumbers total, AggregatedCrawlerNumbers filtered, AggregatedCrawlerNumbers selected) {
            if (total != null) {
                setTotal(Long.toString(total.getStatusOffline()));
            }
            if (filtered != null) {
                setFiltered(Long.toString(filtered.getStatusOffline()));
            }
            if (selected != null) {
                setSelected(Long.toString(selected.getStatusOffline()));
            }
        }

        @Override
        public BooleanKeyHandler getVisibleKeyHandler() {
            return CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_STATUS_OFFLINE_VISIBLE;
        }
    }

    private final class OnlineCountEntry extends DataEntry<AggregatedCrawlerNumbers> {
        private OnlineCountEntry(String label) {
            super(label);
        }

        @Override
        public void setData(AggregatedCrawlerNumbers total, AggregatedCrawlerNumbers filtered, AggregatedCrawlerNumbers selected) {
            if (total != null) {
                setTotal(Long.toString(total.getStatusOnline()));
            }
            if (filtered != null) {
                setFiltered(Long.toString(filtered.getStatusOnline()));
            }
            if (selected != null) {
                setSelected(Long.toString(selected.getStatusOnline()));
            }
        }

        @Override
        public BooleanKeyHandler getVisibleKeyHandler() {
            return CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_STATUS_ONLINE_VISIBLE;
        }
    }

    private final class HostCountEntry extends DataEntry<AggregatedCrawlerNumbers> {
        private HostCountEntry(String label) {
            super(label);
        }

        @Override
        public void setData(AggregatedCrawlerNumbers total, AggregatedCrawlerNumbers filtered, AggregatedCrawlerNumbers selected) {
            if (total != null) {
                setTotal(Integer.toString(total.getHoster().size()));
            }
            if (filtered != null) {
                setFiltered(Integer.toString(filtered.getHoster().size()));
            }
            if (selected != null) {
                setSelected(Integer.toString(selected.getHoster().size()));
            }
        }

        @Override
        public BooleanKeyHandler getVisibleKeyHandler() {
            return CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_HOSTER_COUNT_VISIBLE;
        }
    }

    private final class LinksCountEntry extends DataEntry<AggregatedCrawlerNumbers> {
        private LinksCountEntry(String label) {
            super(label);
        }

        @Override
        public void setData(AggregatedCrawlerNumbers total, AggregatedCrawlerNumbers filtered, AggregatedCrawlerNumbers selected) {
            if (total != null) {
                setTotal(Integer.toString(total.getLinkCount()));
            }
            if (filtered != null) {
                setFiltered(Integer.toString(filtered.getLinkCount()));
            }
            if (selected != null) {
                setSelected(Integer.toString(selected.getLinkCount()));
            }
        }

        @Override
        public BooleanKeyHandler getVisibleKeyHandler() {
            return CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_LINKS_COUNT_VISIBLE;
        }
    }

    private final class BytesTotalEntry extends DataEntry<AggregatedCrawlerNumbers> {
        private BytesTotalEntry(String label) {
            super(label);
        }

        @Override
        public void setData(AggregatedCrawlerNumbers total, AggregatedCrawlerNumbers filtered, AggregatedCrawlerNumbers selected) {
            final boolean includeDisabled = INCLUDE_DISABLED.get();
            if (total != null) {
                setTotal(total.getTotalBytesString(includeDisabled));
            }
            if (filtered != null) {
                setFiltered(filtered.getTotalBytesString(includeDisabled));
            }
            if (selected != null) {
                setSelected(selected.getTotalBytesString(includeDisabled));
            }
        }

        @Override
        public BooleanKeyHandler getVisibleKeyHandler() {
            return CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_TOTAL_BYTES_VISIBLE;
        }
    }

    private final class PackagesEntry extends DataEntry<AggregatedCrawlerNumbers> {
        private PackagesEntry() {
            super(_GUI.T.DownloadOverview_DownloadOverview_packages());
        }

        @Override
        public void setData(AggregatedCrawlerNumbers total, AggregatedCrawlerNumbers filtered, AggregatedCrawlerNumbers selected) {
            if (total != null) {
                setTotal(Integer.toString(total.getPackageCount()));
            }
            if (filtered != null) {
                setFiltered(Integer.toString(filtered.getPackageCount()));
            }
            if (selected != null) {
                setSelected(Integer.toString(selected.getPackageCount()));
            }
        }

        @Override
        public BooleanKeyHandler getVisibleKeyHandler() {
            return CFG_GUI.OVERVIEW_PANEL_LINKGRABBER_PACKAGE_COUNT_VISIBLE;
        }
    }

    /**
     *
     */
    private static final long           serialVersionUID = -195024600818162517L;

    private final ListSelectionListener selectionListener;
    private final TableModelListener    tableListener;

    public LinkgrabberOverview(final LinkGrabberTable table) {
        super(table.getModel());

        CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.getEventSender().addListener(this, true);

        LinkCollector.getInstance().getEventsender().addListener(this, true);
        tableModel.addTableModelListener(tableListener = new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                slowDelayer.run();
            }
        });

        // new Timer(1000, this).start();
        table.getSelectionModel().addListSelectionListener(selectionListener = new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e == null || e.getValueIsAdjusting() || tableModel.isTableSelectionClearing()) {
                    return;
                }
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
                tableModel.removeTableModelListener(tableListener);
                tableModel.getTable().getSelectionModel().removeListSelectionListener(selectionListener);

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

        DataEntry<AggregatedCrawlerNumbers> packageCount = new PackagesEntry();
        DataEntry<AggregatedCrawlerNumbers> size = new BytesTotalEntry(_GUI.T.DownloadOverview_DownloadOverview_size());

        DataEntry<AggregatedCrawlerNumbers> linkCount = new LinksCountEntry(_GUI.T.DownloadOverview_DownloadOverview_links());
        DataEntry<AggregatedCrawlerNumbers> hosterCount = new HostCountEntry(_GUI.T.DownloadOverview_DownloadOverview_hoster());
        DataEntry<AggregatedCrawlerNumbers> onlineCount = new OnlineCountEntry(_GUI.T.DownloadOverview_DownloadOverview_online());
        DataEntry<AggregatedCrawlerNumbers> offlineCount = new OfflineCountEntry(_GUI.T.DownloadOverview_DownloadOverview_offline());
        DataEntry<AggregatedCrawlerNumbers> unknownCount = new UnknownCountEntry(_GUI.T.DownloadOverview_DownloadOverview_unknown());

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
        return new AggregatedCrawlerNumbers(tableModel.getTable().getSelectionInfo(true, true));
    }

    @Override
    protected AggregatedCrawlerNumbers createFiltered() {
        return new AggregatedCrawlerNumbers(tableModel.getTable().getSelectionInfo(false, true));
    }

    @Override
    protected AggregatedCrawlerNumbers createTotal() {
        return new AggregatedCrawlerNumbers(tableModel.getTable().getSelectionInfo(false, false));
    }

    @Override
    public void onLinkCrawlerNewJob(LinkCollectingJob job) {
    }

    @Override
    public void onLinkCrawlerFinished() {
    }

}
