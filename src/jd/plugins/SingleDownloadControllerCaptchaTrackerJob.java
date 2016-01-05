package jd.plugins;

import jd.controlling.downloadcontroller.SingleDownloadController;

import org.appwork.timetracker.TrackerJob;
import org.jdownloader.captcha.v2.WaitForTrackerSlotPluginProcess;

public class SingleDownloadControllerCaptchaTrackerJob extends TrackerJob {

    private final SingleDownloadController controller;

    public SingleDownloadControllerCaptchaTrackerJob(final String id, final SingleDownloadController controller) {
        super(1);
        this.controller = controller;
    }

    @Override
    public void waitForNextSlot(long waitFor) throws InterruptedException {
        if (controller.isAborting()) {
            throw new InterruptedException("DownloadLink is aborting");
        }
        final PluginProgress progress = new WaitForTrackerSlotPluginProcess(waitFor, "Captcha-Slot");
        progress.setProgressSource(this);
        progress.setDisplayInProgressColumnEnabled(false);
        final DownloadLink downloadLink = controller.getDownloadLink();
        try {
            downloadLink.addPluginProgress(progress);
            while (waitFor > 0 && !controller.isAborting()) {
                progress.setCurrent(waitFor);
                synchronized (this) {
                    wait(Math.min(1000, Math.max(0, waitFor)));
                }
                waitFor -= 1000;
            }

        } finally {
            downloadLink.removePluginProgress(progress);
        }
        if (controller.isAborting()) {
            throw new InterruptedException("DownloadLink is aborting");
        }
    }
}
