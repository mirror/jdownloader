package org.jdownloader.gui.views.linkgrabber.overview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.AggregatedCrawlerNumbers;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.laf.jddefault.LAFOptions;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.overviewpanel.DataEntry;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class LinkgrabberOverview extends MigPanel implements ActionListener, HierarchyListener, GenericConfigEventListener<Boolean>, LinkCollectorListener, GUIListener {

    private LinkGrabberTable table;
    private DataEntry        packageCount;
    private DataEntry        linkCount;
    private DataEntry        size;

    protected Timer          updateTimer;
    private DataEntry        hosterCount;
    private DataEntry        onlineCount;
    private DataEntry        offlineCount;
    private DataEntry        unknownCount;

    public LinkgrabberOverview(LinkGrabberTable table) {
        super("ins 0", "[][grow,fill][]", "[grow,fill]");
        this.table = table;

        LAFOptions.getInstance().applyPanelBackground(this);
        MigPanel info = new MigPanel("ins 2 0 0 0", "[grow]10[grow]", "[grow,fill]2[grow,fill]");
        info.setOpaque(false);
        GUIEventSender.getInstance().addListener(this, true);
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

        // DownloadWatchDog.getInstance().getActiveDownloads(), DownloadWatchDog.getInstance().getDownloadSpeedManager().connections()

        CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE.getEventSender().addListener(this);
        CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE.getEventSender().addListener(this);
        CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE.getEventSender().addListener(this);
        CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.getEventSender().addListener(this);

        add(info, "pushy,growy");
        add(Box.createHorizontalGlue());

        LinkCollector.getInstance().getEventsender().addListener(this);
        LinkGrabberTableModel.getInstance().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                update();
            }
        });

        this.addHierarchyListener(this);
        onConfigValueModified(null, null);
        // new Timer(1000, this).start();
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) { return; }

                update();
                onConfigValueModified(null, null);
            }
        });
    }

    private JComponent left(JLabel packageCount22) {
        // packageCount22.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, packageCount22.getForeground()));
        return packageCount22;
    }

    protected void update() {
        if (!this.isDisplayable()) { return; }
        IOEQ.add(new Runnable() {

            public void run() {
                if (!isDisplayable()) { return; }

                final AggregatedCrawlerNumbers total = CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE.isEnabled() ? new AggregatedCrawlerNumbers(new SelectionInfo<CrawledPackage, CrawledLink>(null, LinkCollector.getInstance().getAllChildren(), null, null, null, null)) : null;
                final AggregatedCrawlerNumbers filtered = (CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE.isEnabled() || CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.isEnabled()) ? new AggregatedCrawlerNumbers(new SelectionInfo<CrawledPackage, CrawledLink>(null, table.getModel().getAllChildrenNodes(), null, null, null, table)) : null;
                final AggregatedCrawlerNumbers selected = (CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE.isEnabled() || CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.isEnabled()) ? new AggregatedCrawlerNumbers(new SelectionInfo<CrawledPackage, CrawledLink>(null, table.getModel().getSelectedObjects(), null, null, null, table)) : null;
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
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
                    }

                };
            }
        }, true);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        if (!this.isDisplayable()) {
            updateTimer.stop();
        }

    }

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        /**
         * Disable/Enable the updatetimer if the panel gets enabled/disabled
         */
        if (!this.isDisplayable()) {
            if (updateTimer != null) updateTimer.stop();
        } else {
            if (updateTimer == null || !updateTimer.isRunning()) {
                startUpdateTimer();
            }
            update();
        }
    }

    protected void startUpdateTimer() {
        if (updateTimer != null) updateTimer.stop();
        updateTimer = new Timer(1000, LinkgrabberOverview.this);
        updateTimer.setRepeats(true);
        updateTimer.start();
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                packageCount.updateVisibility();

                size.updateVisibility();
                linkCount.updateVisibility();
                offlineCount.updateVisibility();
                onlineCount.updateVisibility();
                unknownCount.updateVisibility();
                hosterCount.updateVisibility();

            }
        };
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
    public void onLinkCollectorContentModified(LinkCollectorEvent event) {
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
                    startUpdateTimer();
                } else {
                    if (updateTimer != null) updateTimer.stop();
                }
            }
        };

    }

}
