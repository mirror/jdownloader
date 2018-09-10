package org.jdownloader.controlling;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.download.DownloadInterface;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.MirrorLoading;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.settings.GeneralSettings;

public class AggregatedNumbers {
    protected static final boolean FORCED_MIRROR_CASE_INSENSITIVE = CrossSystem.isWindows() || JsonConfig.create(GeneralSettings.class).isForceMirrorDetectionCaseInsensitive();
    private final long             totalBytes;

    public final String getFinishedString(final boolean inclDisabled) {
        if (inclDisabled) {
            return String.valueOf(downloadsFinished + disabledDownloadsFinished);
        } else {
            return String.valueOf(downloadsFinished);
        }
    }

    public String getSkippedString(boolean inclDisabled) {
        if (inclDisabled) {
            return String.valueOf(downloadsSkipped + disabledDownloadsSkipped);
        } else {
            return String.valueOf(downloadsSkipped);
        }
    }

    public String getFailedString(boolean inclDisabled) {
        if (inclDisabled) {
            return String.valueOf(downloadsFailed + disabledDownloadsFailed);
        } else {
            return String.valueOf(downloadsFailed);
        }
    }

    public String getTotalBytesString(boolean inclDisabled) {
        if (inclDisabled) {
            return format(totalBytes + disabledTotalBytes);
        }
        return format(totalBytes);
    }

    private String format(long totalBytes2) {
        if (totalBytes2 < 0) {
            return _GUI.T.lit_unknown();
        }
        return SizeFormatter.formatBytes(totalBytes2);
    }

    public String getLoadedBytesString(boolean inclDisabled) {
        if (inclDisabled) {
            format(loadedBytes + disabledLoadedBytes);
        }
        return format(loadedBytes);
    }

    public String getDownloadSpeedString() {
        return SizeFormatter.formatBytes(downloadSpeed) + "/s";
    }

    public String getEtaString() {
        return eta > 0 ? TimeFormatter.formatSeconds(eta, 0) : "~";
    }

    public final int getLinkCount() {
        return linkCount;
    }

    public final int getPackageCount() {
        return packageCount;
    }

    private final long loadedBytes;
    private final long downloadSpeed;
    private final long eta;
    private final int  linkCount;
    private final int  packageCount;
    private final int  running;

    public final int getRunning() {
        return running;
    }

    public final int getConnections() {
        return connections;
    }

    private final long enabledUnfinishedTotalBytes;
    private final long enabledUnfinishedLoadedBytes;
    private final int  connections;
    private final long disabledTotalBytes;
    private final long disabledLoadedBytes;
    private final long downloadsFinished;
    private final long downloadsFailed;
    private final long downloadsSkipped;
    private final long disabledDownloadsFinished;
    private final long disabledDownloadsFailed;
    private final long disabledDownloadsSkipped;

    private final static class AggregatedDownloadLink {
        private long         bytesTotal  = -1;
        private long         bytesDone   = -1;
        private long         speed       = -1;
        private int          connections = 0;
        private boolean      enabled     = true;
        private DownloadLink link        = null;
    }

    private void aggregrate(DownloadLink link, Map<String, AggregatedDownloadLink> linkInfos) {
        final boolean isEnabled = link.isEnabled();
        final DownloadLinkView view = link.getView();
        final ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
        final String displayName;
        if (FORCED_MIRROR_CASE_INSENSITIVE) {
            displayName = view.getDisplayName().toLowerCase(Locale.ENGLISH);
        } else {
            displayName = view.getDisplayName();
        }
        if (isEnabled) {
            if (conditionalSkipReason instanceof MirrorLoading) {
                final MirrorLoading mirrorLoading = (MirrorLoading) conditionalSkipReason;
                final DownloadLink downloadLink = mirrorLoading.getDownloadLink();
                AggregatedDownloadLink linkInfo = linkInfos.get(displayName);
                if (linkInfo == null || linkInfo.link != downloadLink) {
                    linkInfo = new AggregatedDownloadLink();
                    linkInfo.link = downloadLink;
                    final DownloadLinkView downloadView = downloadLink.getView();
                    linkInfo.bytesTotal = downloadView.getBytesTotal();
                    linkInfo.bytesDone = downloadView.getBytesLoaded();
                    final SingleDownloadController controller = downloadLink.getDownloadLinkController();
                    if (controller != null) {
                        final DownloadInterface downloadInterface = controller.getDownloadInstance();
                        if (downloadInterface == null || ((System.currentTimeMillis() - downloadInterface.getStartTimeStamp()) < 5000)) {
                            linkInfo.speed = 0;
                        } else {
                            linkInfo.speed = downloadView.getSpeedBps();
                        }
                        if (downloadInterface != null) {
                            final ManagedThrottledConnectionHandler handlerP = downloadInterface.getManagedConnetionHandler();
                            if (handlerP != null) {
                                linkInfo.connections = Math.max(linkInfo.connections, handlerP.size());
                            }
                        }
                    }
                    linkInfos.put(displayName, linkInfo);
                }
            } else {
                AggregatedDownloadLink linkInfo = linkInfos.get(displayName);
                if (linkInfo == null) {
                    linkInfo = new AggregatedDownloadLink();
                    linkInfo.link = link;
                    linkInfos.put(displayName, linkInfo);
                }
                linkInfo.enabled = true;
                final SingleDownloadController controller = link.getDownloadLinkController();
                if (controller != null) {
                    linkInfo.bytesTotal = view.getBytesTotal();
                    linkInfo.bytesDone = view.getBytesLoaded();
                    final DownloadInterface downloadInterface = controller.getDownloadInstance();
                    if (downloadInterface == null || ((System.currentTimeMillis() - downloadInterface.getStartTimeStamp()) < 5000)) {
                        linkInfo.speed = 0;
                    } else {
                        linkInfo.speed = view.getSpeedBps();
                    }
                    if (downloadInterface != null) {
                        final ManagedThrottledConnectionHandler handlerP = downloadInterface.getManagedConnetionHandler();
                        if (handlerP != null) {
                            linkInfo.connections = Math.max(linkInfo.connections, handlerP.size());
                        }
                    }
                } else {
                    if (linkInfo.speed < 0) {
                        if (linkInfo.bytesTotal < view.getBytesTotal()) {
                            linkInfo.bytesTotal = view.getBytesTotal();
                        }
                        if (linkInfo.bytesDone < view.getBytesLoaded()) {
                            linkInfo.bytesDone = view.getBytesLoaded();
                        }
                    }
                }
            }
        } else {
            AggregatedDownloadLink linkInfo = linkInfos.get(displayName);
            if (linkInfo == null) {
                linkInfo = new AggregatedDownloadLink();
                linkInfo.link = link;
                linkInfo.enabled = false;
                linkInfos.put(displayName, linkInfo);
            }
            if (linkInfo.enabled == false && linkInfo.speed < 0) {
                if (linkInfo.bytesTotal < view.getBytesTotal()) {
                    linkInfo.bytesTotal = view.getBytesTotal();
                }
                if (linkInfo.bytesDone < view.getBytesLoaded()) {
                    linkInfo.bytesDone = view.getBytesLoaded();
                }
            }
        }
    }

    public AggregatedNumbers(final SelectionInfo<FilePackage, DownloadLink> selection) {
        final List<PackageView<FilePackage, DownloadLink>> packageViews = selection.getPackageViews();
        packageCount = packageViews.size();
        int linkCount = 0;
        long downloadSpeed = 0;
        long totalBytes = -1;
        long totalBytesDisabled = -1;
        long loadedBytes = 0;
        long loadedBytesDisabled = 0;
        long downloadsSkipped = 0;
        long downloadsSkippedDisabled = 0l;
        long downloadsFinished = 0l;
        long downloadsFinishedDisabled = 0l;
        long downloadsFailed = 0l;
        long downloadsFailedDisabled = 0l;
        int running = 0;
        int connections = 0;
        long enabledUnfinishedTotalBytes = -1;
        long enabledUnfinishedLoadedBytes = 0;
        for (PackageView<FilePackage, DownloadLink> packageView : packageViews) {
            final HashMap<String, AggregatedDownloadLink> linkInfos = new HashMap<String, AggregatedDownloadLink>();
            for (final DownloadLink link : packageView.getChildren()) {
                aggregrate(link, linkInfos);
            }
            linkCount += linkInfos.size();
            for (final AggregatedDownloadLink linkInfo : linkInfos.values()) {
                final FinalLinkState state = linkInfo.link.getFinalLinkState();
                connections += linkInfo.connections;
                final SkipReason skipReason = linkInfo.link.getSkipReason();
                if (linkInfo.speed >= 0) {
                    if (downloadSpeed == -1) {
                        downloadSpeed = 0;
                    }
                    downloadSpeed += linkInfo.speed;
                    running++;
                }
                if (linkInfo.enabled) {
                    boolean enabledUnfinished = false;
                    if (state == null) {
                        if (skipReason != null) {
                            downloadsSkipped++;
                        } else {
                            enabledUnfinished = true;
                        }
                    } else {
                        if (state.isFailed()) {
                            downloadsFailed++;
                        } else if (state.isFinished()) {
                            downloadsFinished++;
                        }
                    }
                    if (linkInfo.bytesTotal >= 0) {
                        if (totalBytes == -1) {
                            totalBytes = 0;
                        }
                        totalBytes += linkInfo.bytesTotal;
                        if (enabledUnfinished) {
                            if (enabledUnfinishedTotalBytes == -1) {
                                enabledUnfinishedTotalBytes = 0;
                            }
                            enabledUnfinishedTotalBytes += linkInfo.bytesTotal;
                        }
                    }
                    if (linkInfo.bytesDone >= 0) {
                        loadedBytes += linkInfo.bytesDone;
                        if (enabledUnfinished) {
                            enabledUnfinishedLoadedBytes += linkInfo.bytesDone;
                        }
                    }
                } else {
                    if (state == null) {
                        if (skipReason != null) {
                            downloadsSkippedDisabled++;
                        }
                    } else {
                        if (state.isFailed()) {
                            downloadsFailedDisabled++;
                        } else if (state.isFinished()) {
                            downloadsFinishedDisabled++;
                        }
                    }
                    if (linkInfo.bytesTotal >= 0) {
                        if (totalBytesDisabled == -1) {
                            totalBytesDisabled = 0;
                        }
                        totalBytesDisabled += linkInfo.bytesTotal;
                    }
                    if (linkInfo.bytesDone >= 0) {
                        loadedBytesDisabled += linkInfo.bytesDone;
                    }
                }
            }
        }
        if (totalBytes >= 0 && downloadSpeed > 0) {
            /* we could calc an ETA because at least one filesize is known */
            final long bytesLeft = Math.max(-1, totalBytes - loadedBytes);
            eta = bytesLeft / downloadSpeed;
        } else {
            /* no filesize is known, we use -1 to signal this */
            eta = -1;
        }
        this.connections = connections;
        this.running = running;
        this.downloadSpeed = downloadSpeed;
        this.disabledDownloadsFailed = downloadsFailedDisabled;
        this.disabledDownloadsFinished = downloadsFinishedDisabled;
        this.disabledDownloadsSkipped = downloadsSkippedDisabled;
        this.disabledLoadedBytes = loadedBytesDisabled;
        if (totalBytesDisabled == -1 && linkCount == 0) {
            totalBytesDisabled = 0;
        }
        this.disabledTotalBytes = totalBytesDisabled;
        this.downloadsFailed = downloadsFailed;
        this.downloadsFinished = downloadsFinished;
        this.downloadsSkipped = downloadsSkipped;
        if (totalBytes == -1 && linkCount == 0) {
            totalBytes = 0;
        }
        this.totalBytes = totalBytes;
        this.loadedBytes = loadedBytes;
        this.enabledUnfinishedLoadedBytes = enabledUnfinishedLoadedBytes;
        if (enabledUnfinishedTotalBytes == -1 && linkCount == 0) {
            enabledUnfinishedTotalBytes = 0;
        }
        this.enabledUnfinishedTotalBytes = enabledUnfinishedTotalBytes;
        this.linkCount = linkCount;
    }

    public long getEnabledUnfinishedTotalBytes() {
        return enabledUnfinishedTotalBytes;
    }

    public long getEnabledUnfinishedLoadedBytes() {
        return enabledUnfinishedLoadedBytes;
    }

    public final long getTotalBytes() {
        return totalBytes;
    }

    public final long getDownloadSpeed() {
        return downloadSpeed;
    }

    public final long getEta() {
        return eta;
    }

    public final long getLoadedBytes() {
        return loadedBytes;
    }
}
