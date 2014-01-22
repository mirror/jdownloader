package org.jdownloader.gui.views.downloads.overviewpanel;

import java.awt.event.HierarchyListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JSeparator;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.menu.ChunksEditor;
import jd.gui.swing.jdgui.menu.ParalellDownloadsEditor;
import jd.gui.swing.jdgui.menu.ParallelDownloadsPerHostEditor;
import jd.gui.swing.jdgui.menu.SpeedlimitEditor;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;
import jd.plugins.LinkStatusProperty;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.controlling.AggregatedNumbers;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DownloadOverview extends AbstractOverviewPanel<AggregatedNumbers> implements DownloadControllerListener, HierarchyListener, GenericConfigEventListener<Boolean>, GUIListener {

    /**
     * 
     */
    private static final long                   serialVersionUID = 7849517111823717677L;

    private ListSelectionListener               listSelection;
    private TableModelListener                  tableListener;
    private StateEventListener                  stateListener;
    private GenericConfigEventListener<Boolean> settingsListener;

    @Override
    public void onKeyModifier(int parameter) {
    }

    public DownloadOverview(DownloadsTable table) {
        super();

        // new line

        // DownloadWatchDog.getInstance().getActiveDownloads(), DownloadWatchDog.getInstance().getDownloadSpeedManager().connections()

        CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.getEventSender().addListener(this, true);
        final MigPanel settings = new MigPanel("ins 2 0 0 0 ,wrap 3", "[][fill][fill]", "[]2[]");
        SwingUtils.setOpaque(settings, false);
        settings.add(new JSeparator(JSeparator.VERTICAL), "spany,pushy,growy");
        settings.add(new ChunksEditor(true));
        settings.add(new ParalellDownloadsEditor(true));
        settings.add(new ParallelDownloadsPerHostEditor(true));
        settings.add(new SpeedlimitEditor(true));
        CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_SETTINGS_VISIBLE.getEventSender().addListener(settingsListener = new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                settings.setVisible(newValue);
            }
        });
        settings.setVisible(CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_SETTINGS_VISIBLE.isEnabled());
        CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.getEventSender().addListener(this, true);

        add(Box.createHorizontalGlue());

        add(settings, "hidemode 3");
        DownloadController.getInstance().addListener(this, true);
        DownloadsTableModel.getInstance().addTableModelListener(tableListener = new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                slowDelayer.run();
            }
        });
        DownloadWatchDog.getInstance().getStateMachine().addListener(stateListener = new StateEventListener() {

            @Override
            public void onStateUpdate(StateEvent event) {
            }

            @Override
            public void onStateChange(final StateEvent event) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        if (event.getNewState() == DownloadWatchDog.RUNNING_STATE || event.getNewState() == DownloadWatchDog.PAUSE_STATE) {
                            startUpdateTimer();
                        } else {
                            stopUpdateTimer();
                        }
                    }
                };
            }
        });

        DownloadsTableModel.getInstance().getTable().getSelectionModel().addListSelectionListener(listSelection = new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e == null || e.getValueIsAdjusting() || DownloadsTableModel.getInstance().isTableSelectionClearing()) { return; }
                onConfigValueModified(null, null);
            }
        });

    }

    protected List<DataEntry<AggregatedNumbers>> createDataEntries() {
        DataEntry<AggregatedNumbers> packageCount = new DataEntry<AggregatedNumbers>(_GUI._.DownloadOverview_DownloadOverview_packages()) {

            @Override
            public void setData(AggregatedNumbers total, AggregatedNumbers filtered, AggregatedNumbers selected) {
                if (total != null) setTotal(total.getPackageCount() + "");
                if (filtered != null) setFiltered(filtered.getPackageCount() + "");
                if (selected != null) setSelected(selected.getPackageCount() + "");
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PACKAGE_COUNT_VISIBLE;
            }

        };
        DataEntry<AggregatedNumbers> size = new DataEntry<AggregatedNumbers>(_GUI._.DownloadOverview_DownloadOverview_size()) {

            @Override
            public void setData(AggregatedNumbers total, AggregatedNumbers filtered, AggregatedNumbers selected) {
                if (total != null) setTotal(total.getTotalBytesString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
                if (filtered != null) setFiltered(filtered.getTotalBytesString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
                if (selected != null) setSelected(selected.getTotalBytesString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_TOTAL_BYTES_VISIBLE;
            }

        };
        DataEntry<AggregatedNumbers> bytesLoaded = new DataEntry<AggregatedNumbers>(_GUI._.DownloadOverview_DownloadOverview_loaded()) {

            @Override
            public void setData(AggregatedNumbers total, AggregatedNumbers filtered, AggregatedNumbers selected) {
                if (total != null) setTotal(total.getLoadedBytesString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
                if (filtered != null) setFiltered(filtered.getLoadedBytesString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
                if (selected != null) setSelected(selected.getLoadedBytesString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_BYTES_LOADED_VISIBLE;
            }

        };
        DataEntry<AggregatedNumbers> runningDownloads = new DataEntry<AggregatedNumbers>(_GUI._.DownloadOverview_DownloadOverview_running_downloads()) {

            @Override
            public void setData(AggregatedNumbers total, AggregatedNumbers filtered, AggregatedNumbers selected) {
                if (total != null) setTotal(total.getRunning() + "");
                if (filtered != null) setFiltered(filtered.getRunning() + "");
                if (selected != null) setSelected(selected.getRunning() + "");
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_RUNNING_DOWNLOADS_COUNT_VISIBLE;
            }

        };

        DataEntry<AggregatedNumbers> linkCount = new DataEntry<AggregatedNumbers>(_GUI._.DownloadOverview_DownloadOverview_links()) {

            @Override
            public void setData(AggregatedNumbers total, AggregatedNumbers filtered, AggregatedNumbers selected) {
                if (total != null) setTotal(total.getLinkCount() + "");
                if (filtered != null) setFiltered(filtered.getLinkCount() + "");
                if (selected != null) setSelected(selected.getLinkCount() + "");
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_LINK_COUNT_VISIBLE;
            }

        };

        DataEntry<AggregatedNumbers> speed = new DataEntry<AggregatedNumbers>(_GUI._.DownloadOverview_DownloadOverview_speed()) {

            @Override
            public void setData(AggregatedNumbers total, AggregatedNumbers filtered, AggregatedNumbers selected) {
                if (total != null) setTotal(total.getDownloadSpeedString());
                if (filtered != null) setFiltered(filtered.getDownloadSpeedString());
                if (selected != null) setSelected(selected.getDownloadSpeedString());
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_SPEED_VISIBLE;
            }

        };
        DataEntry<AggregatedNumbers> eta = new DataEntry<AggregatedNumbers>(_GUI._.DownloadOverview_DownloadOverview_eta()) {

            @Override
            public void setData(AggregatedNumbers total, AggregatedNumbers filtered, AggregatedNumbers selected) {
                if (total != null) setTotal(total.getEtaString());
                if (filtered != null) setFiltered(filtered.getEtaString());
                if (selected != null) setSelected(selected.getEtaString());
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_ETAVISIBLE;
            }

        };

        DataEntry<AggregatedNumbers> connections = new DataEntry<AggregatedNumbers>(_GUI._.DownloadOverview_DownloadOverview_connections()) {

            @Override
            public void setData(AggregatedNumbers total, AggregatedNumbers filtered, AggregatedNumbers selected) {
                if (total != null) setTotal(total.getConnections() + "");
                if (filtered != null) setFiltered(filtered.getConnections() + "");
                if (selected != null) setSelected(selected.getConnections() + "");
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_CONNECTIONS_VISIBLE;
            }

        };

        DataEntry<AggregatedNumbers> finishedDownloads = new DataEntry<AggregatedNumbers>(_GUI._.DownloadOverview_DownloadOverview_finished_downloads()) {

            @Override
            public void setData(AggregatedNumbers total, AggregatedNumbers filtered, AggregatedNumbers selected) {
                if (total != null) setTotal(total.getFinishedString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
                if (filtered != null) setFiltered(filtered.getFinishedString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
                if (selected != null) setSelected(selected.getFinishedString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_LINKS_FINISHED_COUNT_VISIBLE;
            }

        };
        DataEntry<AggregatedNumbers> skippedDownloads = new DataEntry<AggregatedNumbers>(_GUI._.DownloadOverview_DownloadOverview_skipped_downloads()) {

            @Override
            public void setData(AggregatedNumbers total, AggregatedNumbers filtered, AggregatedNumbers selected) {
                if (total != null) setTotal(total.getSkippedString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
                if (filtered != null) setFiltered(filtered.getSkippedString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
                if (selected != null) setSelected(selected.getSkippedString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_LINKS_SKIPPED_COUNT_VISIBLE;
            }

        };
        DataEntry<AggregatedNumbers> failedDownloads = new DataEntry<AggregatedNumbers>(_GUI._.DownloadOverview_DownloadOverview_failed_downloads()) {

            @Override
            public void setData(AggregatedNumbers total, AggregatedNumbers filtered, AggregatedNumbers selected) {
                if (total != null) setTotal(total.getFailedString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
                if (filtered != null) setFiltered(filtered.getFailedString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
                if (selected != null) setSelected(selected.getFailedString(CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS.isEnabled()));
            }

            @Override
            public BooleanKeyHandler getVisibleKeyHandler() {
                return CFG_GUI.OVERVIEW_PANEL_DOWNLOAD_LINKS_FAILED_COUNT_VISIBLE;
            }

        };

        ArrayList<DataEntry<AggregatedNumbers>> entries = new ArrayList<DataEntry<AggregatedNumbers>>();
        entries.add(packageCount);
        entries.add(linkCount);
        entries.add(size);
        entries.add(speed);
        entries.add(bytesLoaded);
        entries.add(eta);
        entries.add(runningDownloads);
        entries.add(connections);
        entries.add(finishedDownloads);
        entries.add(skippedDownloads);
        entries.add(failedDownloads);
        return entries;
    }

    public void removeListeners() {
        super.removeListeners();
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_SETTINGS_VISIBLE.getEventSender().removeListener(settingsListener);
                CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.getEventSender().removeListener(DownloadOverview.this);
                DownloadController.getInstance().removeListener(DownloadOverview.this);
                DownloadsTableModel.getInstance().removeTableModelListener(tableListener);
                DownloadsTableModel.getInstance().getTable().getSelectionModel().removeListSelectionListener(listSelection);
                DownloadWatchDog.getInstance().getStateMachine().removeListener(stateListener);
            }
        };
    }

    // protected void update() {
    // if (visible.get() == false) return;
    // super.update();
    //
    //
    // }

    @Override
    public void onDownloadControllerAddedPackage(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerStructureRefresh(FilePackage pkg) {
        slowDelayer.run();
    }

    @Override
    public void onDownloadControllerStructureRefresh() {
        slowDelayer.run();
    }

    @Override
    public void onDownloadControllerStructureRefresh(AbstractNode node, Object param) {
        slowDelayer.run();
    }

    @Override
    public void onDownloadControllerRemovedPackage(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerRemovedLinklist(List<DownloadLink> list) {
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, DownloadLinkProperty property) {
        slowDelayer.run();
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {
        slowDelayer.run();
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, LinkStatusProperty property) {
        slowDelayer.run();
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {
        slowDelayer.run();
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg) {
        slowDelayer.run();
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE == keyHandler && Boolean.FALSE.equals(newValue)) {
            removeListeners();
            visible.set(false);
        } else {
            super.onConfigValueModified(keyHandler, newValue);
        }
    }

    @Override
    protected boolean isActiveView(View newView) {
        return newView instanceof DownloadsView;
    }

    protected AggregatedNumbers createSelected() {
        return new AggregatedNumbers(DownloadsTableModel.getInstance().getTable().getSelectionInfo(true, true));
    }

    protected AggregatedNumbers createFiltered() {
        return new AggregatedNumbers(DownloadsTableModel.getInstance().getTable().getSelectionInfo(false, true));
    }

    protected AggregatedNumbers createTotal() {
        return new AggregatedNumbers(DownloadsTableModel.getInstance().getTable().getSelectionInfo(false, false));
    }
}
