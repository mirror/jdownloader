package org.jdownloader.gui.notify.downloads;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadSession;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogJob;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;

import org.appwork.storage.config.WeakHashSet;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.AbstractBubbleSupport;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.notify.gui.CFG_BUBBLE;
import org.jdownloader.gui.translate._GUI;

public class StartDownloadsBubbleSupport extends AbstractBubbleSupport implements DownloadWatchdogListener {

    private ArrayList<Element> elements;

    public StartDownloadsBubbleSupport() {
        super(_GUI._.plugins_optional_JDLightTray_ballon_startstopdownloads2(), CFG_BUBBLE.BUBBLE_NOTIFY_START_STOP_DOWNLOADS_ENABLED);
        elements = new ArrayList<Element>();
        elements.add(new Element(CFG_BUBBLE.DOWNLOAD_STARTED_BUBBLE_CONTENT_FILENAME_VISIBLE, _GUI._.lit_filename(), IconKey.ICON_FILE));
        elements.add(new Element(CFG_BUBBLE.DOWNLOAD_STARTED_BUBBLE_CONTENT_HOSTER_VISIBLE, _GUI._.lit_hoster(), IconKey.ICON_DOWNLOAD));
        elements.add(new Element(CFG_BUBBLE.DOWNLOAD_STARTED_BUBBLE_CONTENT_ACCOUNT_VISIBLE, _GUI._.lit_account(), IconKey.ICON_PREMIUM));
        elements.add(new Element(CFG_BUBBLE.DOWNLOAD_STARTED_BUBBLE_CONTENT_PROXY_VISIBLE, _GUI._.lit_proxy(), IconKey.ICON_PROXY));
        elements.add(new Element(CFG_BUBBLE.DOWNLOAD_STARTED_BUBBLE_CONTENT_SAVE_TO_VISIBLE, _GUI._.lit_save_to(), IconKey.ICON_FOLDER));
        elements.add(new Element(CFG_BUBBLE.DOWNLOAD_STARTED_BUBBLE_CONTENT_STATUS_VISIBLE, _GUI._.lit_status(), IconKey.ICON_MEDIA_PLAYBACK_START));
        DownloadWatchDog.getInstance().getEventSender().addListener(this, true);
    }

    private class QueuedStart {
        public QueuedStart(SingleDownloadController downloadController) {
            controller = downloadController;
            this.time = System.currentTimeMillis();
        }

        private final SingleDownloadController controller;
        private final long                     time;
    }

    private final WeakHashMap<SingleDownloadController, QueuedStart> queue       = new WeakHashMap<SingleDownloadController, QueuedStart>();
    private volatile Thread                                          queueWorker = null;
    private final WeakHashSet<SingleDownloadController>              started     = new WeakHashSet<SingleDownloadController>();

    @Override
    public List<Element> getElements() {
        return elements;
    }

    @Override
    public synchronized void onDownloadControllerStart(final SingleDownloadController downloadController, DownloadLinkCandidate candidate) {
        if (!isEnabled()) {
            started.clear();
            queue.clear();
        } else {
            queue.put(downloadController, new QueuedStart(downloadController));
            Thread thread = queueWorker;
            if (thread == null || !thread.isAlive()) {
                thread = new Thread("BubbleNotifyDelayerQUeue") {
                    public void run() {
                        try {
                            while (true) {
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    synchronized (StartDownloadsBubbleSupport.this) {
                                        started.clear();
                                        queue.clear();
                                        queueWorker = null;
                                    }
                                    return;
                                }
                                synchronized (StartDownloadsBubbleSupport.this) {
                                    for (Iterator<QueuedStart> it = queue.values().iterator(); it.hasNext();) {
                                        final QueuedStart next = it.next();
                                        if (next != null) {
                                            final SingleDownloadController controller = next.controller;
                                            if (!controller.isActive()) {
                                                it.remove();
                                            } else if (System.currentTimeMillis() - next.time > CFG_BUBBLE.CFG.getDownloadStartEndNotifyDelay()) {
                                                it.remove();
                                                DownloadWatchDog.getInstance().enqueueJob(new DownloadWatchDogJob() {

                                                    @Override
                                                    public void interrupt() {
                                                    }

                                                    @Override
                                                    public void execute(DownloadSession currentSession) {
                                                        if (controller.isActive()) {
                                                            show(new AbstractNotifyWindowFactory() {

                                                                @Override
                                                                public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
                                                                    if (controller.isActive()) {
                                                                        return new DownloadStartedNotify(StartDownloadsBubbleSupport.this, controller);
                                                                    } else {
                                                                        return null;
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    }
                                    if (queue.size() == 0) {
                                        queueWorker = null;
                                        return;
                                    }
                                }
                            }
                        } finally {
                            synchronized (StartDownloadsBubbleSupport.this) {
                                queueWorker = null;
                            }
                        }
                    }
                };
                thread.setDaemon(true);
                thread.start();
                queueWorker = thread;
            }
        }
    }

    @Override
    public synchronized void onDownloadControllerStopped(final SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        if (!isEnabled()) {
            started.clear();
            queue.clear();
        } else {
            if (started.remove(downloadController)) {
                show(new AbstractNotifyWindowFactory() {

                    @Override
                    public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
                        return new DownloadStoppedNotify(StartDownloadsBubbleSupport.this, downloadController);
                    }
                });
            }
        }
    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
    }

    @Override
    public void onDownloadWatchdogStateIsPause() {
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
    }

    @Override
    public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {
    }
}
