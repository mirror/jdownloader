package org.jdownloader.captcha.v2;

import jd.plugins.DownloadLink;
import jd.plugins.PluginProgress;

import org.appwork.timetracker.TrackerJob;

final public class CaptchaTrackerJob extends TrackerJob {

    private DownloadLink downloadLink;
    private String       id;

    public CaptchaTrackerJob(String id, DownloadLink link) {
        super(1);
        this.downloadLink = link;
        this.id = id;

    }

    @Override
    public void waitForNextSlot(long waitFor) throws InterruptedException {

        if (downloadLink == null) {
            while (waitFor > 0) {
                waitFor -= 1000;
                synchronized (this) {
                    if (waitFor <= 0) {
                        return;
                    }
                    if (waitFor > 1000) {
                        wait(1000);
                    } else {
                        this.wait(waitFor);
                    }
                }
            }
            return;
        }
        if (downloadLink.getDownloadLinkController().isAborting()) {
            throw new InterruptedException("DownloadLink is aboring");
        }
        final PluginProgress progress = new WaitForTrackerSlotPluginProcess(waitFor, "Captcha-Slot");
        progress.setProgressSource(this);
        progress.setDisplayInProgressColumnEnabled(false);
        try {
            downloadLink.addPluginProgress(progress);
            while (waitFor > 0 && !downloadLink.getDownloadLinkController().isAborting()) {
                progress.setCurrent(waitFor);
                synchronized (this) {
                    wait(Math.min(1000, Math.max(0, waitFor)));
                }
                waitFor -= 1000;
            }

        } finally {
            downloadLink.removePluginProgress(progress);
        }
        if (downloadLink.getDownloadLinkController().isAborting()) {
            throw new InterruptedException("DownloadLink is aboring");
        }
    }
}