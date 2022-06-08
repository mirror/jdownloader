package jd.gui.swing;

import java.awt.Image;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jd.SecondLevelLaunch;
import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTHelper;
import org.jdownloader.controlling.AggregatedNumbers;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.icon.IconBadgePainter;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.MacDockProgressDisplay;

public class MacOSDockAdapter {
    private static final AtomicReference<Thread> thread   = new AtomicReference<Thread>();
    private static final AtomicBoolean           initFlag = new AtomicBoolean();

    public static void init() {
        if (initFlag.compareAndSet(false, true)) {
            SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {
                private DownloadWatchdogListener listener;

                @Override
                public void run() {
                    TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                        @Override
                        protected Void run() throws RuntimeException {
                            MacOSApplicationAdapter.setDockIcon(NewTheme.I().getImage("logo/jd_logo_128_128", 128));
                            return null;
                        }
                    });
                    EnumKeyHandler MacDOCKProgressDisplay = JsonConfig.create(GraphicalUserInterfaceSettings.class)._getStorageHandler().getKeyHandler("MacDockProgressDisplay", EnumKeyHandler.class);
                    MacDOCKProgressDisplay.getEventSender().addListener(new GenericConfigEventListener<Enum>() {
                        @Override
                        public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
                        }

                        @Override
                        public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
                            if (MacDockProgressDisplay.TOTAL_PROGRESS.equals(newValue)) {
                                startDockUpdater();
                            } else {
                                stopDockUpdater();
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
    }

    private static void startDockUpdater() {
        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).getMacDockProgressDisplay() != MacDockProgressDisplay.TOTAL_PROGRESS) {
            return;
        }
        Thread thread = MacOSDockAdapter.thread.getAndSet(null);
        if (thread != null && thread.isAlive()) {
            return;
        }
        thread = new Thread("MacDOCKUpdater") {
            @Override
            public void run() {
                int lastPercent = -1;
                HashMap<Integer, Image> imageCache = new HashMap<Integer, Image>();
                try {
                    while (Thread.currentThread() == MacOSDockAdapter.thread.get()) {
                        try {
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                break;
                            }
                            final AggregatedNumbers aggn = new AggregatedNumbers(DownloadsTable.getInstance().getSelectionInfo(false, false));
                            int percent = 0;
                            if (aggn.getTotalBytes() > 0) {
                                percent = (int) ((aggn.getLoadedBytes() * 100) / aggn.getTotalBytes());
                            }
                            final int finalpercent = percent;
                            if (lastPercent == finalpercent) {
                                continue;
                            }
                            lastPercent = finalpercent;
                            Image image = imageCache.get(finalpercent);
                            if (image == null) {
                                image = new EDTHelper<Image>() {
                                    @Override
                                    public Image edtRun() {
                                        return new IconBadgePainter(NewTheme.I().getImage("logo/jd_logo_128_128", 128)).getImage(finalpercent, finalpercent + "");
                                    }
                                }.getReturnValue();
                                // interrupt will return null
                                imageCache.put(finalpercent, image);
                            }
                            if (image != null) {
                                final Image finalImage = image;
                                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                                    @Override
                                    protected Void run() throws RuntimeException {
                                        MacOSApplicationAdapter.setDockIcon(finalImage);
                                        return null;
                                    }
                                });
                            }
                        } catch (final Throwable e) {
                            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                        }
                    }
                } finally {
                    /* restore default Icon */
                    TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                        @Override
                        protected Void run() throws RuntimeException {
                            MacOSApplicationAdapter.setDockIcon(NewTheme.I().getImage("logo/jd_logo_128_128", 128));
                            return null;
                        }
                    });
                    /* release reference if this thread is current dockUpdater */
                    MacOSDockAdapter.thread.compareAndSet(Thread.currentThread(), null);
                }
            }
        };
        thread.setDaemon(true);
        stopDockUpdater(thread);
        thread.start();
    }

    private static void stopDockUpdater() {
        stopDockUpdater(null);
    }

    private static void stopDockUpdater(Thread newThread) {
        final Thread thread = MacOSDockAdapter.thread.getAndSet(newThread);
        if (thread != null && thread.isDaemon()) {
            thread.interrupt();
        }
    }
}
