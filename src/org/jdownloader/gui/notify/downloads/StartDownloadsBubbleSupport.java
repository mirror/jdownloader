package org.jdownloader.gui.notify.downloads;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.AbstractBubbleSupport;
import org.jdownloader.gui.notify.Element;
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

        private SingleDownloadController controller;
        private long                     time;
    }

    private LinkedList<QueuedStart>           queue   = new LinkedList<QueuedStart>();
    private Thread                            queueWorker;
    private HashSet<SingleDownloadController> started = new HashSet<SingleDownloadController>();

    @Override
    public List<Element> getElements() {
        return elements;
    }

    @Override
    public void onDownloadControllerStart(final SingleDownloadController downloadController, DownloadLinkCandidate candidate) {
        if (!CFG_BUBBLE.BUBBLE_NOTIFY_START_STOP_DOWNLOADS_ENABLED.isEnabled()) {
            started.clear();
            queue.clear();
            return;
        }
        synchronized (queue) {
            queue.add(new QueuedStart(downloadController));
            if (queueWorker == null) {
                queueWorker = new Thread("BubbleNotofyDelayerQUeue") {
                    public void run() {
                        while (true) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                return;
                            }
                            synchronized (queue) {

                                for (Iterator<QueuedStart> it = queue.iterator(); it.hasNext();) {
                                    final QueuedStart next = it.next();
                                    if (!next.controller.isActive()) {
                                        it.remove();
                                    } else if (System.currentTimeMillis() - next.time > CFG_BUBBLE.CFG.getDownloadStartEndNotifyDelay()) {
                                        it.remove();
                                        started.add(next.controller);
                                        new EDTRunner() {
                                            @Override
                                            protected void runInEDT() {

                                                DownloadStartedNotify no = new DownloadStartedNotify(StartDownloadsBubbleSupport.this, next.controller);
                                                show(no);
                                            }
                                        };
                                    }
                                }
                                if (queue.size() == 0) {
                                    queueWorker = null;
                                    return;
                                }
                            }
                        }
                    }
                };
                queueWorker.start();
            }
        }

    }

    @Override
    public void onDownloadControllerStopped(final SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        if (!CFG_BUBBLE.BUBBLE_NOTIFY_START_STOP_DOWNLOADS_ENABLED.isEnabled()) {
            started.clear();
            queue.clear();
            return;
        }
        synchronized (queue) {
            if (!started.contains(downloadController)) return;
            started.remove(downloadController);
        }
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                DownloadStoppedNotify no = new DownloadStoppedNotify(StartDownloadsBubbleSupport.this, downloadController);
                show(no);
            }
        };
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
}
