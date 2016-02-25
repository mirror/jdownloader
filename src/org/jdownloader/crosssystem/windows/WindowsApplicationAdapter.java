package org.jdownloader.crosssystem.windows;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.logging2.extmanager.Log;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.jdownloader.controlling.AggregatedNumbers;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.WindowsTaskBarProgressDisplay;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.SecondLevelLaunch;
import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.gui.swing.jdgui.JDGui;

public class WindowsApplicationAdapter {
    private static final WindowsApplicationAdapter INSTANCE = new WindowsApplicationAdapter();

    /**
     * get the only existing instance of WindowsApplicationAdapter. This is a singleton
     *
     * @return
     */
    public static WindowsApplicationAdapter getInstance() {
        return WindowsApplicationAdapter.INSTANCE;
    }

    /**
     * Create a new instance of WindowsApplicationAdapter. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private WindowsApplicationAdapter() {

        initWin7PlusTaskbarExtension();

    }

    private void initWin7PlusTaskbarExtension() {
        if (!CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_7)) {
            return;
        }
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {

            private DownloadWatchdogListener listener;

            @Override
            public void run() {

                CFG_GUI.WINDOWS_TASKBAR_PROGRESS_DISPLAY.getEventSender().addListener(new GenericConfigEventListener<Enum>() {

                    @Override
                    public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
                    }

                    @Override
                    public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
                        if (WindowsTaskBarProgressDisplay.NOTHING.equals(newValue)) {
                            stopDockUpdater();

                        } else {
                            startDockUpdater();
                        }
                    }

                });
                DownloadWatchDog.getInstance().getEventSender().addListener(listener = new DownloadWatchdogListener() {

                    @Override
                    public void onDownloadWatchdogStateIsStopping() {
                        stopDockUpdater();
                    }

                    @Override
                    public void onDownloadWatchdogStateIsStopped() {
                        stopDockUpdater();
                    }

                    @Override
                    public void onDownloadWatchdogStateIsRunning() {
                        startDockUpdater();
                    }

                    @Override
                    public void onDownloadWatchdogStateIsPause() {
                        startDockUpdater();
                    }

                    @Override
                    public void onDownloadWatchdogStateIsIdle() {
                    }

                    @Override
                    public void onDownloadWatchdogDataUpdate() {
                    }

                    @Override
                    public void onDownloadControllerStart(SingleDownloadController downloadController, DownloadLinkCandidate candidate) {
                    }

                    @Override
                    public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
                    }

                    @Override
                    public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {
                    }
                });
                DownloadWatchDog.getInstance().notifyCurrentState(listener);
            }

        });
    }

    private final Object       LOCK = new Object();

    private double             lastPercent;

    private ScheduledFuture<?> queueHandler;

    private void startDockUpdater() {
        synchronized (LOCK) {
            if (CFG_GUI.CFG.getWindowsTaskbarProgressDisplay() == WindowsTaskBarProgressDisplay.NOTHING) {

                return;
            }
            if (queueHandler != null) {
                return;
            }
            queueHandler = TaskQueue.TIMINGQUEUE.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {

                    try {
                        final AggregatedNumbers aggn = new AggregatedNumbers(DownloadsTable.getInstance().getSelectionInfo(false, false));
                        double percent = 0;
                        if (aggn.getTotalBytes() > 0) {
                            percent = ((double) aggn.getLoadedBytes()) / aggn.getTotalBytes();
                        }
                        System.out.println(percent);
                        if (lastPercent == percent) {
                            // no changes
                            return;
                        }
                        lastPercent = percent;
                        Win7TaskBar.setProgress(JDGui.getInstance().getMainFrame(), lastPercent, true);
                    } catch (Throwable er) {
                        Log.log(er);
                    } finally {
                        synchronized (LOCK) {
                            if (queueHandler == null) {
                                // ensure reset
                                reset();
                                queueHandler.cancel(true);
                            }
                        }
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);

        }
    }

    private void stopDockUpdater() {
        synchronized (LOCK) {

            if (queueHandler != null) {

                queueHandler = null;
            }

        }
    }

    private void reset() {
        try {
            Win7TaskBar.hideProgress(JDGui.getInstance().getMainFrame());
        } catch (Throwable er) {
            Log.log(er);
        }
    }

}
