package org.jdownloader.gui.views.downloads.overviewpanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Timer;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public abstract class AbstractOverviewPanel<T> extends MigPanel implements GUIListener, GenericConfigEventListener<Boolean>, HierarchyListener {

    private List<DataEntry<T>>                  dataEntries;
    protected AtomicBoolean                     visible     = new AtomicBoolean(false);
    protected NullsafeAtomicReference<Timer>    updateTimer = new NullsafeAtomicReference<Timer>(null);

    protected final DelayedRunnable             slowDelayer;
    protected final DelayedRunnable             fastDelayer;
    private GenericConfigEventListener<Boolean> relayoutListener;

    public AbstractOverviewPanel() {
        super("ins 0", "[][grow,fill][]", "[grow,fill]");
        LAFOptions.getInstance().applyPanelBackground(this);
        GUIEventSender.getInstance().addListener(this, true);
        final MigPanel info = new MigPanel("ins 2 0 0 0", "[grow]10[grow]", "[grow,fill]2[grow,fill]");
        info.setOpaque(false);
        relayoutListener = new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        layoutInfoPanel(info);
                        update();

                        revalidate();
                    }

                };
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        };
        layoutInfoPanel(info);
        add(info, "pushy,growy");
        final ScheduledExecutorService queue = DelayedRunnable.getNewScheduledExecutorService();
        slowDelayer = new DelayedRunnable(queue, 500, 5000) {

            @Override
            public void delayedrun() {
                update();
            }
        };
        fastDelayer = new DelayedRunnable(queue, 50, 200) {

            @Override
            public void delayedrun() {
                update();
            }
        };
        CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE.getEventSender().addListener(this, true);

        CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.getEventSender().addListener(this, true);

        this.addHierarchyListener(this);
        onConfigValueModified(null, null);

        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                visible.set(isViewActive());
                fastDelayer.run();
            }
        });
        onConfigValueModified(null, null);

    }

    protected boolean isViewActive() {
        return new EDTHelper<Boolean>() {

            @Override
            public Boolean edtRun() {

                return isActiveView(JDGui.getInstance().getMainTabbedPane().getSelectedView());
            }
        }.getReturnValue();
    }

    protected void layoutInfoPanel(MigPanel info) {
        info.removeAll();

        this.dataEntries = new ArrayList<DataEntry<T>>();
        for (DataEntry<T> s : createDataEntries()) {
            if (s.getVisibleKeyHandler() == null || s.getVisibleKeyHandler().isEnabled()) dataEntries.add(s);
            if (s.getVisibleKeyHandler() != null) {
                s.getVisibleKeyHandler().getEventSender().addListener(relayoutListener);
            }
        }
        // selected
        // filtered
        // speed
        // eta

        int splitafter = dataEntries.size() / 2;
        ArrayList<DataEntry<T>> row1 = new ArrayList<DataEntry<T>>();
        ArrayList<DataEntry<T>> row2 = new ArrayList<DataEntry<T>>();

        for (int i = 0; i < dataEntries.size(); i++) {

            if (i % 2 == 0) {
                row1.add(dataEntries.get(i));
            } else {
                row2.add(dataEntries.get(i));
            }

        }

        for (DataEntry<T> de : row1) {
            de.addTo(info);
        }

        boolean first = true;
        for (DataEntry<T> de : row2) {
            if (first) {
                de.addTo(info, ",newline");
            } else {
                de.addTo(info);
            }

            first = false;
        }
    }

    public List<DataEntry<T>> getDataEntries() {
        return dataEntries;
    }

    protected abstract List<DataEntry<T>> createDataEntries();

    public void removeListeners() {

        new EDTRunner() {

            @Override
            protected void runInEDT() {

                stopUpdateTimer();
                GUIEventSender.getInstance().removeListener(AbstractOverviewPanel.this);
                CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE.getEventSender().removeListener(AbstractOverviewPanel.this);
                CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE.getEventSender().removeListener(AbstractOverviewPanel.this);
                CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE.getEventSender().removeListener(AbstractOverviewPanel.this);
                CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.getEventSender().removeListener(AbstractOverviewPanel.this);

                removeHierarchyListener(AbstractOverviewPanel.this);

            }
        };
    }

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        /**
         * Disable/Enable the updatetimer if the panel gets enabled/disabled
         */
        if (!this.isDisplayable()) {
            stopUpdateTimer();
        } else {
            startUpdateTimer();
            fastDelayer.run();
        }
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                boolean hasSelectedObjects = DownloadsTableModel.getInstance().hasSelectedObjects();
                for (DataEntry di : dataEntries) {
                    di.updateVisibility(hasSelectedObjects);
                }

                fastDelayer.run();
            }
        };

    }

    @Override
    public void onGuiMainTabSwitch(View oldView, final View newView) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (isActiveView(newView)) {
                    visible.set(true);
                    startUpdateTimer();
                    fastDelayer.run();
                } else {
                    visible.set(false);
                    stopUpdateTimer();
                }
            }
        };

    }

    abstract protected boolean isActiveView(View newView);

    protected void startUpdateTimer() {
        Timer currentTimer = updateTimer.get();
        if (currentTimer != null && currentTimer.isRunning()) return;
        if (DownloadWatchDog.getInstance().isRunning() == false) return;
        currentTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!(e.getSource() instanceof Timer)) return;
                if (e.getSource() != updateTimer.get() || !isDisplayable()) {
                    Timer timer = ((Timer) e.getSource());
                    updateTimer.compareAndSet(timer, null);
                    timer.stop();
                    return;
                }
                fastDelayer.run();
            }
        });
        currentTimer.setRepeats(true);
        updateTimer.set(currentTimer);
        currentTimer.start();
    }

    protected void stopUpdateTimer() {
        Timer old = updateTimer.getAndSet(null);
        if (old != null) old.stop();
    }

    public void update() {
        if (visible.get() == false) return;
        final T total;
        final T filtered;
        final T selected;
        final boolean smartInfo = CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.isEnabled();
        if (CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE.isEnabled()) {
            total = createTotal();
        } else {
            total = null;
        }
        if ((CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE.isEnabled() || smartInfo)) {
            filtered = createFiltered();
        } else {
            filtered = null;
        }
        if ((CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE.isEnabled() || smartInfo)) {
            selected = createSelected();
        } else {
            selected = null;
        }
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (!isDisplayable() || visible.get() == false) { return; }
                for (DataEntry<T> entry : dataEntries) {
                    set(entry);
                }

            }

            private void set(DataEntry<T> dataEntry) {
                if (dataEntry != null) {
                    dataEntry.setData(total, filtered, selected);

                }
            }
        }.waitForEDT();
    }

    protected abstract T createSelected();

    protected abstract T createFiltered();

    protected abstract T createTotal();
}
