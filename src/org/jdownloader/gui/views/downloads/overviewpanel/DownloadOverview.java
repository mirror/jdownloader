package org.jdownloader.gui.views.downloads.overviewpanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Box;
import javax.swing.JSeparator;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.SecondLevelLaunch;
import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadControllerEvent;
import jd.controlling.downloadcontroller.DownloadControllerListener;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.JDGui.Panels;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.menu.ChunksEditor;
import jd.gui.swing.jdgui.menu.ParalellDownloadsEditor;
import jd.gui.swing.jdgui.menu.ParallelDownloadsPerHostEditor;
import jd.gui.swing.jdgui.menu.SpeedlimitEditor;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.controlling.AggregatedNumbers;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public class DownloadOverview extends MigPanel implements DownloadControllerListener, HierarchyListener, GenericConfigEventListener<Boolean>, GUIListener {

    /**
     * 
     */
    private static final long                   serialVersionUID = 7849517111823717677L;
    private DataEntry                           packageCount;
    private DataEntry                           linkCount;
    private DataEntry                           size;
    private DataEntry                           bytesLoaded;
    private DataEntry                           speed;
    private DataEntry                           eta;
    protected Timer                             updateTimer;

    private DataEntry                           runningDownloads;
    private DataEntry                           connections;
    private AtomicBoolean                       updating         = new AtomicBoolean(false);
    private AtomicBoolean                       visible          = new AtomicBoolean(false);
    private ListSelectionListener               listSelection;
    private TableModelListener                  tableListener;
    private StateEventListener                  stateListener;
    private GenericConfigEventListener<Boolean> settingsListener;

    public DownloadOverview(DownloadsTable table) {
        super("ins 0", "[][grow,fill][]", "[grow,fill]");

        LAFOptions.getInstance().applyPanelBackground(this);
        GUIEventSender.getInstance().addListener(this, true);
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

        CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.getEventSender().addListener(this, true);
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
        add(info, "pushy,growy");
        add(Box.createHorizontalGlue());

        add(settings, "hidemode 3");
        DownloadController.getInstance().addListener(this, true);
        DownloadsTableModel.getInstance().addTableModelListener(tableListener = new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                update();
            }
        });
        DownloadWatchDog.getInstance().getStateMachine().addListener(stateListener = new StateEventListener() {

            @Override
            public void onStateUpdate(StateEvent event) {
            }

            @Override
            public void onStateChange(StateEvent event) {
                if (event.getNewState() == DownloadWatchDog.RUNNING_STATE || event.getNewState() == DownloadWatchDog.PAUSE_STATE) {
                    startUpdateTimer();
                } else {
                    stopUpdateTimer();
                }
            }
        });
        this.addHierarchyListener(this);
        onConfigValueModified(null, null);
        // new Timer(1000, this).start();
        DownloadsTableModel.getInstance().getTable().getSelectionModel().addListSelectionListener(listSelection = new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) { return; }
                update();
                onConfigValueModified(null, null);
            }
        });
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                visible.set(JDGui.getInstance().isCurrentPanel(Panels.DOWNLOADLIST));
                update();
            }
        });
    }

    public void removeListeners() {
        stopUpdateTimer();
        GUIEventSender.getInstance().removeListener(this);
        CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE.getEventSender().removeListener(this);
        CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE.getEventSender().removeListener(this);
        CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE.getEventSender().removeListener(this);
        CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.getEventSender().removeListener(this);
        CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_SETTINGS_VISIBLE.getEventSender().removeListener(settingsListener);
        CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.getEventSender().removeListener(this);
        removeHierarchyListener(this);
        DownloadController.getInstance().removeListener(this);
        DownloadsTableModel.getInstance().removeTableModelListener(tableListener);
        DownloadsTableModel.getInstance().getTable().getSelectionModel().removeListSelectionListener(listSelection);
        DownloadWatchDog.getInstance().getStateMachine().removeListener(stateListener);
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
                    final AggregatedNumbers total = CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE.isEnabled() ? new AggregatedNumbers(new SelectionInfo<FilePackage, DownloadLink>(null, DownloadController.getInstance().getAllChildren(), null, null, null, null)) : null;
                    final AggregatedNumbers filtered = (CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE.isEnabled() || CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.isEnabled()) ? new AggregatedNumbers(new SelectionInfo<FilePackage, DownloadLink>(null, DownloadsTableModel.getInstance().getAllChildrenNodes(), null, null, null, DownloadsTableModel.getInstance().getTable())) : null;
                    final AggregatedNumbers selected;

                    if ((CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE.isEnabled() || CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.isEnabled())) {
                        List<AbstractNode> selectedObjects = new EDTHelper<List<AbstractNode>>() {

                            @Override
                            public List<AbstractNode> edtRun() {
                                return DownloadsTableModel.getInstance().getSelectedObjects();
                            }

                        }.getReturnValue();
                        selected = new AggregatedNumbers(new SelectionInfo<FilePackage, DownloadLink>(null, selectedObjects, null, null, null, DownloadsTableModel.getInstance().getTable()));
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
    public void onDownloadControllerEvent(final DownloadControllerEvent event) {
        switch (event.getType()) {
        case REFRESH_STRUCTURE:
        case REFRESH_CONTENT:
            update();
        }
    }

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        /**
         * Disable/Enable the updatetimer if the panel gets enabled/disabled
         */
        if (!this.isDisplayable()) {
            stopUpdateTimer();
        } else {
            Timer lupdateTimer = updateTimer;
            if (lupdateTimer == null || !lupdateTimer.isRunning()) {
                startUpdateTimer();
            }
            update();
        }
    }

    protected void startUpdateTimer() {
        stopUpdateTimer();
        if (DownloadWatchDog.getInstance().isRunning() == false) return;
        Timer lupdateTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!(e.getSource() instanceof Timer)) return;
                if (e.getSource() != updateTimer || !isDisplayable()) {
                    ((Timer) e.getSource()).stop();
                    return;
                }
                update();
            }
        });
        lupdateTimer.setRepeats(true);
        updateTimer = lupdateTimer;
        lupdateTimer.start();
    }

    protected void stopUpdateTimer() {
        Timer lupdateTimer = updateTimer;
        updateTimer = null;
        if (lupdateTimer != null) lupdateTimer.stop();
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE == keyHandler && Boolean.FALSE.equals(newValue)) {
            removeListeners();
            visible.set(false);
        } else {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    boolean hasSelectedObjects = DownloadsTableModel.getInstance().hasSelectedObjects();
                    packageCount.updateVisibility(hasSelectedObjects);
                    bytesLoaded.updateVisibility(hasSelectedObjects);
                    size.updateVisibility(hasSelectedObjects);
                    linkCount.updateVisibility(hasSelectedObjects);
                    speed.updateVisibility(hasSelectedObjects);
                    eta.updateVisibility(hasSelectedObjects);
                    connections.updateVisibility(hasSelectedObjects);
                    runningDownloads.updateVisibility(hasSelectedObjects);

                }
            };
        }
    }

    @Override
    public void onGuiMainTabSwitch(View oldView, final View newView) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (newView instanceof DownloadsView) {
                    visible.set(true);
                    startUpdateTimer();
                    update();
                } else {
                    visible.set(false);
                    stopUpdateTimer();
                }
            }
        };

    }
}
