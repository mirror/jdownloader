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
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

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
import org.appwork.swing.MigPanel;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class DownloadOverview extends MigPanel implements ActionListener, DownloadControllerListener, HierarchyListener {

    private DownloadsTable downloadTable;
    private JLabel         packageCount;
    private JLabel         linkCount;
    private JLabel         size;
    private JLabel         bytesLoaded;
    private JLabel         speed;
    private JLabel         eta;
    protected Timer        updateTimer;

    public DownloadOverview(DownloadsTable table) {
        super("ins 0", "[][grow,fill][][]", "");
        this.downloadTable = table;
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();

        if (c >= 0) {
            setBackground(new Color(c));
            setOpaque(true);
        }
        MigPanel info = new MigPanel("ins 2 0 0 0 ,wrap 6", "[grow]10[grow]", "[]2[]");
        info.setOpaque(false);
        packageCount = new JLabel();
        linkCount = new JLabel();
        // SwingUtils.setOpaque(packageCount, false);
        // SwingUtils.setOpaque(packageCount, false);
        // total size
        size = new JLabel();
        bytesLoaded = new JLabel();
        speed = new JLabel();
        eta = new JLabel();
        // selected
        // filtered
        // speed
        // eta
        info.add(createHeaderLabel(_GUI._.DownloadOverview_DownloadOverview_packages()), "alignx right");
        info.add(packageCount);
        info.add(createHeaderLabel(_GUI._.DownloadOverview_DownloadOverview_size()), "alignx right");
        info.add(size);
        info.add(createHeaderLabel(_GUI._.DownloadOverview_DownloadOverview_loaded()), "alignx right");
        info.add(bytesLoaded);
        info.add(createHeaderLabel(_GUI._.DownloadOverview_DownloadOverview_links()), "newline,alignx right");
        info.add(linkCount);

        info.add(createHeaderLabel(_GUI._.DownloadOverview_DownloadOverview_speed()), "alignx right");
        info.add(speed);
        info.add(createHeaderLabel(_GUI._.DownloadOverview_DownloadOverview_eta()), "alignx right");
        info.add(eta);
        MigPanel settings = new MigPanel("ins 2 0 0 0 ,wrap 2", "[fill][fill]", "[]2[]");
        SwingUtils.setOpaque(settings, false);
        settings.add(new ChunksEditor(true), "height 20!");
        settings.add(new ParalellDownloadsEditor(true));
        settings.add(new ParallelDownloadsPerHostEditor(true));
        settings.add(new SpeedlimitEditor(true));

        add(info);
        add(Box.createHorizontalGlue());
        add(new JSeparator(JSeparator.VERTICAL), "pushy,growy");
        add(settings);
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
        // new Timer(1000, this).start();
    }

    protected void update() {
        if (!this.isDisplayable()) { return; }
        final SelectionInfo<FilePackage, DownloadLink> selection = new SelectionInfo<FilePackage, DownloadLink>(null, DownloadsTableModel.getInstance().getAllPackageNodes());
        final boolean filtered = DownloadsTableModel.getInstance().isFilteredView();

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                long totalBytes = 0l;
                long loadedBytes = 0l;
                long downloadSpeed = 0l;

                for (FilePackage dl : selection.getAllPackages()) {
                    totalBytes += dl.getView().getSize();
                    loadedBytes += dl.getView().getDone();

                }
                downloadSpeed = DownloadWatchDog.getInstance().getDownloadSpeedManager().getSpeed();
                final long totalETA = downloadSpeed == 0 ? 0 : (totalBytes - loadedBytes) / downloadSpeed;
                if (filtered) {

                    packageCount.setText(DownloadController.getInstance().size() + "/" + selection.getAllPackages().size() + "");
                    linkCount.setText(DownloadController.getInstance().getAllDownloadLinks().size() + "/" + selection.getSelectedChildren().size() + "");
                } else {
                    packageCount.setText(DownloadController.getInstance().size() + "");
                    linkCount.setText(DownloadController.getInstance().getAllDownloadLinks().size() + "");
                }

                size.setText(SizeFormatter.formatBytes(totalBytes));
                bytesLoaded.setText(SizeFormatter.formatBytes(loadedBytes));
                if (downloadSpeed > 0) {
                    speed.setText(SizeFormatter.formatBytes(downloadSpeed) + "/s");
                    eta.setText(TimeFormatter.formatSeconds(totalETA, 0));
                } else {
                    speed.setText(SizeFormatter.formatBytes(downloadSpeed) + "/s");
                    eta.setText("~");

                }
            }
        };

    }

    private JComponent createHeaderLabel(String label) {
        JLabel lbl = new JLabel(label);
        SwingUtils.toBold(lbl);

        lbl.setEnabled(false);
        return lbl;
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
        }
    }

    protected void startUpdateTimer() {
        if (updateTimer != null) updateTimer.stop();
        updateTimer = new Timer(1000, DownloadOverview.this);
        updateTimer.setRepeats(true);
        updateTimer.start();
    }

}
