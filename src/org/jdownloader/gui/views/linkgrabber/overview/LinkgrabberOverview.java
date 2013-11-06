package org.jdownloader.gui.views.linkgrabber.overview;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Box;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.SecondLevelLaunch;
import jd.controlling.TaskQueue;
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
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.AggregatedCrawlerNumbers;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.overviewpanel.DataEntry;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public class LinkgrabberOverview extends MigPanel implements GenericConfigEventListener<Boolean>, LinkCollectorListener, GUIListener {

    /**
     * 
     */
    private static final long     serialVersionUID = -195024600818162517L;
    private LinkGrabberTable      table;
    private DataEntry             packageCount;
    private DataEntry             linkCount;
    private DataEntry             size;

    protected Timer               updateTimer;
    private DataEntry             hosterCount;
    private DataEntry             onlineCount;
    private DataEntry             offlineCount;
    private DataEntry             unknownCount;
    private AtomicBoolean         updating         = new AtomicBoolean(false);
    private AtomicBoolean         visible          = new AtomicBoolean(false);
    private ListSelectionListener selectionListener;
    private TableModelListener    tableListener;

    public LinkgrabberOverview(LinkGrabberTable table) {
        super("ins 0", "[][grow,fill][]", "[grow,fill]");
        this.table = table;

        LAFOptions.getInstance().applyPanelBackground(this);
        MigPanel info = new MigPanel("ins 2 0 0 0", "[grow]10[grow]", "[grow,fill]2[grow,fill]");
        info.setOpaque(false);
        packageCount = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_packages());
        size = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_size());

        linkCount = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_links());
        hosterCount = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_hoster());
        onlineCount = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_online());
        offlineCount = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_offline());
        unknownCount = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_unknown());
        // selected
        // filtered
        // speed
        // eta
        packageCount.addTo(info);
        size.addTo(info);
        hosterCount.addTo(info);
        linkCount.addTo(info, ",newline");
        onlineCount.addTo(info);

        offlineCount.addTo(info);
        unknownCount.addTo(info);
        // new line

        CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.getEventSender().addListener(this, true);

        add(info, "pushy,growy");
        add(Box.createHorizontalGlue());

        LinkCollector.getInstance().getEventsender().addListener(this, true);
        LinkGrabberTableModel.getInstance().addTableModelListener(tableListener = new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                update();
            }
        });

        onConfigValueModified(null, null);
        // new Timer(1000, this).start();
        table.getSelectionModel().addListSelectionListener(selectionListener = new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e == null || e.getValueIsAdjusting()) return;
                update();
                onConfigValueModified(null, null);
            }
        });
        GUIEventSender.getInstance().addListener(this, true);
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                visible.set(JDGui.getInstance().isCurrentPanel(Panels.LINKGRABBER));
                update();
            }
        });
    }

    public void removeListeners() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE.getEventSender().removeListener(LinkgrabberOverview.this);
                CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE.getEventSender().removeListener(LinkgrabberOverview.this);
                CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE.getEventSender().removeListener(LinkgrabberOverview.this);
                CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.getEventSender().removeListener(LinkgrabberOverview.this);
                CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.getEventSender().removeListener(LinkgrabberOverview.this);
                LinkCollector.getInstance().getEventsender().removeListener(LinkgrabberOverview.this);
                LinkGrabberTableModel.getInstance().removeTableModelListener(tableListener);
                table.getSelectionModel().removeListSelectionListener(selectionListener);
                GUIEventSender.getInstance().removeListener(LinkgrabberOverview.this);
            }
        };
    }

    protected void update() {
        if (visible.get() == false) return;
        if (updating.getAndSet(true) == false) {
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    if (visible.get() == false) {
                        updating.set(false);
                        return null;
                    }
                    final AggregatedCrawlerNumbers total = CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE.isEnabled() ? new AggregatedCrawlerNumbers(table.getSelectionInfo(false, false)) : null;
                    final AggregatedCrawlerNumbers filtered = (CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE.isEnabled() || CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.isEnabled()) ? new AggregatedCrawlerNumbers(table.getSelectionInfo(false, true)) : null;
                    final AggregatedCrawlerNumbers selected;
                    if ((CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE.isEnabled() || CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.isEnabled())) {
                        selected = new AggregatedCrawlerNumbers(table.getSelectionInfo(true, true));
                    } else {
                        selected = null;
                    }
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            try {
                                if (!isDisplayable()) { return; }
                                if (total != null) packageCount.setTotal(total.getPackageCount() + "");
                                if (filtered != null) packageCount.setFiltered(filtered.getPackageCount() + "");
                                if (selected != null) packageCount.setSelected(selected.getPackageCount() + "");

                                if (total != null) linkCount.setTotal(total.getLinkCount() + "");
                                if (filtered != null) linkCount.setFiltered(filtered.getLinkCount() + "");
                                if (selected != null) linkCount.setSelected(selected.getLinkCount() + "");

                                if (total != null) size.setTotal(total.getTotalBytesString());
                                if (filtered != null) size.setFiltered(filtered.getTotalBytesString());
                                if (selected != null) size.setSelected(selected.getTotalBytesString());

                                if (total != null) onlineCount.setTotal(total.getStatusOnline());
                                if (filtered != null) onlineCount.setFiltered(filtered.getStatusOnline());
                                if (selected != null) onlineCount.setSelected(selected.getStatusOnline());

                                if (total != null) offlineCount.setTotal(total.getStatusOffline());
                                if (filtered != null) offlineCount.setFiltered(filtered.getStatusOffline());
                                if (selected != null) offlineCount.setSelected(selected.getStatusOffline());

                                if (total != null) unknownCount.setTotal(total.getStatusUnknown());
                                if (filtered != null) unknownCount.setFiltered(filtered.getStatusUnknown());
                                if (selected != null) unknownCount.setSelected(selected.getStatusUnknown());

                                if (total != null) hosterCount.setTotal(total.getHoster().size());
                                if (filtered != null) hosterCount.setFiltered(filtered.getHoster().size());
                                if (selected != null) hosterCount.setSelected(selected.getHoster().size());
                            } finally {
                                updating.set(false);
                            }
                        }
                    };
                    return null;

                }
            });
        }
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE == keyHandler && Boolean.FALSE.equals(newValue)) {
            removeListeners();
            visible.set(false);
        } else {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    boolean hasSelectedObjects = table.getModel().hasSelectedObjects();
                    packageCount.updateVisibility(hasSelectedObjects);
                    size.updateVisibility(hasSelectedObjects);
                    linkCount.updateVisibility(hasSelectedObjects);
                    offlineCount.updateVisibility(hasSelectedObjects);
                    onlineCount.updateVisibility(hasSelectedObjects);
                    unknownCount.updateVisibility(hasSelectedObjects);
                    hosterCount.updateVisibility(hasSelectedObjects);
                }
            };
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
        update();
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
        update();
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
        update();
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
        update();
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
        update();
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onGuiMainTabSwitch(View oldView, final View newView) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (newView instanceof LinkGrabberView) {
                    visible.set(true);
                    update();
                } else {
                    visible.set(false);
                }
            }
        };

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

}
