package org.jdownloader.controlling;

import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.download.DownloadInterface;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.plugins.FinalLinkState;

public class AggregatedNumbers {

    private long totalBytes;

    public String getTotalBytesString() {
        if (totalBytes < 0) { return _GUI._.lit_unknown(); }
        return SizeFormatter.formatBytes(totalBytes);
    }

    public String getLoadedBytesString() {
        return SizeFormatter.formatBytes(loadedBytes);
    }

    public String getDownloadSpeedString() {
        return SizeFormatter.formatBytes(downloadSpeed) + "/s";
    }

    public String getEtaString() {
        return eta > 0 ? TimeFormatter.formatSeconds(eta, 0) : "~";
    }

    public int getLinkCount() {
        return linkCount;
    }

    public int getPackageCount() {
        return packageCount;
    }

    private long loadedBytes;
    private long downloadSpeed;
    private long eta;
    private int  linkCount;
    private int  packageCount;
    private long running;

    public long getRunning() {
        return running;
    }

    public long getConnections() {
        return connections;
    }

    private long connections;
    private long disabledTotalBytes;
    private long disabledLoadedBytes;
    private long failedTotalBytes;

    public long getDisabledTotalBytes() {
        return disabledTotalBytes;
    }

    public long getDisabledLoadedBytes() {
        return disabledLoadedBytes;
    }

    public AggregatedNumbers(SelectionInfo<FilePackage, DownloadLink> selection) {
        totalBytes = 0l;
        disabledTotalBytes = 0l;
        disabledLoadedBytes = 0l;
        failedTotalBytes = 0l;
        loadedBytes = 0l;
        downloadSpeed = 0l;
        running = 0l;
        connections = 0l;
        packageCount = selection.getAllPackages().size();
        linkCount = selection.getChildren().size();
        for (DownloadLink dl : selection.getChildren()) {
            if (dl == null) continue;
            if (FinalLinkState.CheckFailed(dl.getFinalLinkState())) {
                failedTotalBytes += dl.getDownloadSize();
            } else {
                if (dl.isEnabled()) {

                    totalBytes += dl.getDownloadSize();
                    loadedBytes += dl.getDownloadCurrent();

                } else {

                    disabledTotalBytes += dl.getDownloadSize();
                    disabledLoadedBytes += dl.getDownloadCurrent();
                }
            }
            downloadSpeed += dl.getDownloadSpeed();
            SingleDownloadController sdc = dl.getDownloadLinkController();
            if (sdc != null) {
                running++;
                DownloadInterface conInst = sdc.getDownloadInstance();
                if (conInst != null) {
                    ManagedThrottledConnectionHandler handlerP = conInst.getManagedConnetionHandler();
                    if (handlerP != null) {
                        connections += handlerP.size();

                    }

                }
            }

        }

        eta = downloadSpeed == 0 ? 0 : (totalBytes - loadedBytes) / downloadSpeed;

    }

    public long getFailedTotalBytes() {
        return failedTotalBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getDownloadSpeed() {
        return downloadSpeed;
    }

    public long getEta() {
        return eta;
    }

    public long getLoadedBytes() {
        return loadedBytes;
    }
}
