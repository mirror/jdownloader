package org.jdownloader.gui.views.downloads.overviewpanel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadControllerEvent;
import jd.controlling.downloadcontroller.DownloadControllerListener;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.menu.ChunksEditor;
import jd.gui.swing.jdgui.menu.ParalellDownloadsEditor;
import jd.gui.swing.jdgui.menu.ParallelDownloadsPerHostEditor;
import jd.gui.swing.jdgui.menu.SpeedlimitEditor;
import jd.gui.swing.laf.LookAndFeelController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DownloadOverview extends MigPanel implements ActionListener, DownloadControllerListener, HierarchyListener, GenericConfigEventListener<Boolean> {

    private DownloadsTable downloadTable;
    private DataEntry      packageCount;
    private DataEntry      linkCount;
    private DataEntry      size;
    private DataEntry      bytesLoaded;
    private DataEntry      speed;
    private DataEntry      eta;
    protected Timer        updateTimer;

    private DataEntry      runningDownloads;
    private DataEntry      connections;

    public DownloadOverview(DownloadsTable table) {
        super("ins 0", "[][grow,fill][]", "[grow,fill]");
        this.downloadTable = table;
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();

        if (c >= 0) {
            setBackground(new Color(c));
            setOpaque(true);
        }
        MigPanel info = new MigPanel("ins 2 0 0 0", "[grow]10[grow]", "[grow,fill]2[grow,fill]");
        info.setOpaque(false);

        packageCount = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_packages());
        size = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_size());
        bytesLoaded = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_loaded());
        runningDownloads = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_running_downloads());

        linkCount = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_links());

        speed = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_speed());
        eta = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_eta());

        connections = new DataEntry(_GUI._.DownloadOverview_DownloadOverview_connections());

        // selected
        // filtered
        // speed
        // eta
        packageCount.addTo(info);
        size.addTo(info);
        bytesLoaded.addTo(info);
        runningDownloads.addTo(info);
        linkCount.addTo(info, ",newline");
        speed.addTo(info);
        eta.addTo(info);
        connections.addTo(info);

        // new line

        // DownloadWatchDog.getInstance().getActiveDownloads(), DownloadWatchDog.getInstance().getDownloadSpeedManager().connections()

        CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_TOTAL_INFO_VISIBLE.getEventSender().addListener(this);
        CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_SELECTED_INFO_VISIBLE.getEventSender().addListener(this);
        CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_VISIBLE_ONLY_INFO_VISIBLE.getEventSender().addListener(this);
        CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_SMART_INFO_VISIBLE.getEventSender().addListener(this);
        final MigPanel settings = new MigPanel("ins 2 0 0 0 ,wrap 3", "[][fill][fill]", "[]2[]");
        SwingUtils.setOpaque(settings, false);
        settings.add(new JSeparator(JSeparator.VERTICAL), "spany,pushy,growy");
        settings.add(new ChunksEditor(true), "height 20!");
        settings.add(new ParalellDownloadsEditor(true));
        settings.add(new ParallelDownloadsPerHostEditor(true));
        settings.add(new SpeedlimitEditor(true));
        CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_SETTINGS_VISIBLE.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                settings.setVisible(newValue);
            }
        });
        settings.setVisible(CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_SETTINGS_VISIBLE.isEnabled());
        add(info, "pushy,growy");
        add(Box.createHorizontalGlue());

        add(settings, "hidemode 3");
        DownloadController.getInstance().addListener(this);
        DownloadsTableModel.getInstance().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                update();
            }
        });
        DownloadWatchDog.getInstance().getStateMachine().addListener(new StateEventListener() {

            @Override
            public void onStateUpdate(StateEvent event) {
            }

            @Override
            public void onStateChange(StateEvent event) {
                if (event.getNewState() == DownloadWatchDog.RUNNING_STATE) {
                    startUpdateTimer();
                } else {
                    if (updateTimer != null) updateTimer.stop();
                }
            }
        });
        this.addHierarchyListener(this);
        onConfigValueModified(null, null);
        // new Timer(1000, this).start();
        DownloadsTableModel.getInstance().getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
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
        if (!this.isDisplayable() || true) { return; }
        IOEQ.add(new Runnable() {

            public void run() {
                final AggregatedNumbers total = CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_TOTAL_INFO_VISIBLE.isEnabled() ? new AggregatedNumbers(new SelectionInfo<FilePackage, DownloadLink>(null, DownloadController.getInstance().getAllDownloadLinks())) : null;
                final AggregatedNumbers filtered = (CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_VISIBLE_ONLY_INFO_VISIBLE.isEnabled() || CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_SMART_INFO_VISIBLE.isEnabled()) ? new AggregatedNumbers(new SelectionInfo<FilePackage, DownloadLink>(null, DownloadsTableModel.getInstance().getAllChildrenNodes())) : null;
                final AggregatedNumbers selected = (CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_SELECTED_INFO_VISIBLE.isEnabled() || CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_SMART_INFO_VISIBLE.isEnabled()) ? new AggregatedNumbers(new SelectionInfo<FilePackage, DownloadLink>(null, DownloadsTableModel.getInstance().getSelectedObjects())) : null;
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {

                        if (total != null) packageCount.setTotal(total.getPackageCount() + "");
                        if (filtered != null) packageCount.setFiltered(filtered.getPackageCount() + "");
                        if (selected != null) packageCount.setSelected(selected.getPackageCount() + "");

                        if (total != null) linkCount.setTotal(total.getLinkCount() + "");
                        if (filtered != null) linkCount.setFiltered(filtered.getLinkCount() + "");
                        if (selected != null) linkCount.setSelected(selected.getLinkCount() + "");

                        if (total != null) size.setTotal(total.getTotalBytesString());
                        if (filtered != null) size.setFiltered(filtered.getTotalBytesString());
                        if (selected != null) size.setSelected(selected.getTotalBytesString());

                        if (total != null) bytesLoaded.setTotal(total.getLoadedBytesString());
                        if (filtered != null) bytesLoaded.setFiltered(filtered.getLoadedBytesString());
                        if (selected != null) bytesLoaded.setSelected(selected.getLoadedBytesString());

                        if (total != null) speed.setTotal(total.getDownloadSpeedString());
                        if (filtered != null) speed.setFiltered(filtered.getDownloadSpeedString());
                        if (selected != null) speed.setSelected(selected.getDownloadSpeedString());

                        if (total != null) eta.setTotal(total.getEtaString());
                        if (filtered != null) eta.setFiltered(filtered.getEtaString());
                        if (selected != null) eta.setSelected(selected.getEtaString());

                        if (total != null) connections.setTotal(total.getConnections() + "");
                        if (filtered != null) connections.setFiltered(filtered.getConnections() + "");
                        if (selected != null) connections.setSelected(selected.getConnections() + "");

                        if (total != null) runningDownloads.setTotal(total.getRunning() + "");
                        if (filtered != null) runningDownloads.setFiltered(filtered.getRunning() + "");
                        if (selected != null) runningDownloads.setSelected(selected.getRunning() + "");

                    }

                };
            }
        });

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        update();

        if (!this.isDisplayable()) {
            updateTimer.stop();
        }

    }

    @Override
    public void onDownloadControllerEvent(final DownloadControllerEvent event) {

        switch (event.getType()) {
        case REFRESH_STRUCTURE:
            update();

        }
        ;

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
        updateTimer = new Timer(1000, DownloadOverview.this);
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
                bytesLoaded.updateVisibility();
                size.updateVisibility();
                linkCount.updateVisibility();
                speed.updateVisibility();
                eta.updateVisibility();
                connections.updateVisibility();
                runningDownloads.updateVisibility();

            }
        };
    }

}
