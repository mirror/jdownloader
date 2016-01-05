package jd.plugins;

import jd.controlling.downloadcontroller.SingleDownloadController;

import org.appwork.timetracker.TrackerJob;
import org.jdownloader.captcha.v2.WaitForTrackerSlotPluginProcess;

public class DownloadLinkCaptchaTracker extends TrackerJob {

    private final DownloadLink downloadLink;

    public DownloadLinkCaptchaTracker(String id, DownloadLink link) {
        super(1);
        this.downloadLink = link;
    }

    @Override
    public void waitForNextSlot(long waitFor) throws InterruptedException {
        if (downloadLink == null) {
            while (waitFor > 0) {
                synchronized (this) {
                    if (waitFor <= 0) {
                        return;
                    }
                    if (waitFor > 1000) {
                        wait(1000);
                    } else {
                        this.wait(waitFor);
                    }
                    waitFor -= 1000;
                }
            }
        } else {
            final SingleDownloadController controller = downloadLink.getDownloadLinkController();
            if (controller != null && controller.isAborting()) {
                throw new InterruptedException("DownloadLink is aborting");
            }
            final PluginProgress progress = new WaitForTrackerSlotPluginProcess(waitFor, "Captcha-Slot");
            progress.setProgressSource(this);
            progress.setDisplayInProgressColumnEnabled(false);
            try {
                downloadLink.addPluginProgress(progress);
                while (waitFor > 0 && (controller == null || !controller.isAborting())) {
                    progress.setCurrent(waitFor);
                    synchronized (this) {
                        wait(Math.min(1000, Math.max(0, waitFor)));
                    }
                    waitFor -= 1000;
                }

            } finally {
                downloadLink.removePluginProgress(progress);
            }
            if (controller != null && controller.isAborting()) {
                throw new InterruptedException("DownloadLink is aborting");
            }
        }
    }
}