package org.jdownloader.controlling;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.StringUtils;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.plugins.FinalLinkState;

public class DownloadLinkAggregator implements MirrorPackageSetup {

    private int totalCount;

    private int onlineStatusOfflineCount;
    private int onlineStatusOnlineCount;

    public int getTotalCount() {
        return totalCount;
    }

    public int getOnlineStatusOfflineCount() {
        return onlineStatusOfflineCount;
    }

    public int getOnlineStatusOnlineCount() {
        return onlineStatusOnlineCount;
    }

    public int getOnlineStatusUnkownCount() {
        return getTotalCount() - getOnlineStatusOfflineCount() - getOnlineStatusOfflineCount();
    }

    public int getEnabledCount() {
        return enabledCount;
    }

    public int getDisabledCount() {
        return getTotalCount() - getEnabledCount();
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getBytesLoaded() {
        return bytesLoaded;
    }

    public int getFinishedCount() {
        return finishedCount;
    }

    private int     enabledCount;
    private long    totalBytes;
    private long    bytesLoaded;
    private int     finishedCount;

    private long    eta;

    private boolean localFileUsageEnabled = false;

    public void setLocalFileUsageEnabled(boolean fileSizeCheckEnabled) {
        this.localFileUsageEnabled = fileSizeCheckEnabled;
    }

    private boolean mirrorHandlingEnabled = true;

    private int     localFileCount;

    public boolean isMirrorHandlingEnabled() {
        return mirrorHandlingEnabled;
    }

    public void setMirrorHandlingEnabled(boolean mirrorHandlingEnabled) {
        this.mirrorHandlingEnabled = mirrorHandlingEnabled;
    }

    public DownloadLinkAggregator(FilePackage fp) {
        final boolean readL = fp.getModifyLock().readLock();
        try {
            update(fp.getChildren());
        } finally {
            fp.getModifyLock().readUnlock(readL);
        }
    }

    public DownloadLinkAggregator() {
    }

    public DownloadLinkAggregator(SelectionInfo<FilePackage, DownloadLink> si) {
        update(si.getChildren());
    }

    public void update(final List<DownloadLink> children) {
        int enabled = 0;
        int offline = 0;
        int online = 0;
        long totalBytes = -1;
        long bytesLoaded = 0;
        long bytesToDo = 0;
        int finished = 0;
        long speed = 0;
        int localFileCount = 0;
        final int total;
        final boolean isLocalFileMode = isLocalFileUsageEnabled();
        if (isMirrorHandlingEnabled()) {
            final HashMap<String, MirrorPackage> mirrorPackages = new HashMap<String, MirrorPackage>();
            for (final DownloadLink link : children) {
                String mirrorID = createDupeID(link);
                final HashSet<String> idDupes = new HashSet<String>();
                idDupes.add(mirrorID);
                while (mirrorID != null) {
                    MirrorPackage mirrorPackage = mirrorPackages.get(mirrorID);
                    if (mirrorPackage == null) {
                        mirrorPackages.put(mirrorID, mirrorPackage = new MirrorPackage(mirrorID, this));
                    }
                    final String newID = mirrorPackage.add(link);
                    if (newID == null || idDupes.add(newID) == false) {
                        break;
                    } else {
                        mirrorID = newID;
                    }
                }
            }
            total = mirrorPackages.values().size();
            boolean fileSizeKnown = false;
            for (final MirrorPackage mirrorPackage : mirrorPackages.values()) {
                final long fileSize = mirrorPackage.getTotalBytes();
                final long loaded = mirrorPackage.getBytesLoaded();
                if (fileSize >= 0) {
                    fileSizeKnown = true;
                    totalBytes += fileSize;
                }

                if (isLocalFileMode && loaded > 0) {
                    localFileCount++;
                }

                bytesLoaded += loaded;
                bytesToDo += Math.max(0, fileSize - loaded);

                if (mirrorPackage.isFinished()) {
                    finished++;
                }
                if (mirrorPackage.isEnabled()) {
                    enabled++;
                    speed += mirrorPackage.getSpeed();
                }
                if (mirrorPackage.isOnline()) {
                    online++;
                } else if (mirrorPackage.isOffline()) {
                    offline++;
                }
            }
            if (fileSizeKnown) {
                totalBytes += 1;
            }
        } else {
            final HashSet<String> localFileCheck = new HashSet<String>();
            boolean fileSizeKnown = false;
            total = children.size();
            for (final DownloadLink link : children) {
                final DownloadLinkView view = link.getView();
                final long fileSize = view.getBytesTotal();
                if (fileSize >= 0) {
                    fileSizeKnown = true;
                    totalBytes += fileSize;
                }
                final boolean isFinished = FinalLinkState.CheckFinished(link.getFinalLinkState());
                if (isFinished) {
                    finished++;
                }
                long loaded = 0;
                if (isLocalFileMode) {
                    final String fileOutput = link.getFileOutput();
                    if (StringUtils.isNotEmpty(fileOutput) && localFileCheck.add(fileOutput)) {
                        final File completeFile = new File(fileOutput);
                        if (completeFile.exists()) {
                            loaded = completeFile.length();
                            localFileCount++;
                        } else {
                            final File partFile = new File(fileOutput + ".part");
                            if (partFile.exists()) {
                                loaded = partFile.length();
                                localFileCount++;
                            }
                        }
                    }
                } else {
                    loaded = view.getBytesLoaded();
                }
                bytesLoaded += loaded;
                bytesToDo += Math.max(0, fileSize - loaded);
                if (link.isEnabled()) {
                    enabled++;
                    if (!isFinished && link.getDownloadLinkController() != null) {
                        speed += view.getSpeedBps();
                    }
                }
                switch (link.getAvailableStatus()) {
                case TRUE:
                    online++;
                    break;
                case FALSE:
                    offline++;
                    break;
                }
            }
            if (fileSizeKnown) {
                totalBytes += 1;
            }
        }
        this.localFileCount = localFileCount;
        this.totalCount = total;
        this.onlineStatusOfflineCount = offline;
        this.onlineStatusOnlineCount = online;
        this.enabledCount = enabled;
        if (totalBytes == -1 && totalCount == 0) {
            totalBytes = 0;
        }
        this.totalBytes = totalBytes;
        this.bytesLoaded = bytesLoaded;
        this.finishedCount = finished;
        this.eta = speed > 0 ? (bytesToDo) / speed : -1;
        if (bytesToDo == 0 && !isFinished()) {
            // filesizes are unknown
            eta = -1;
        }

    }

    public int getLocalFileCount() {
        if (!isLocalFileUsageEnabled()) {
            throw new IllegalStateException("isLocalFileUsageEnabled() is disabled");
        }
        return localFileCount;
    }

    @Override
    public boolean isLocalFileUsageEnabled() {
        return localFileUsageEnabled;
    }

    public long getEta() {
        return eta;
    }

    private String createDupeID(DownloadLink link) {
        return link.getFileOutput();
    }

    public boolean isFinished() {
        return finishedCount == totalCount || (totalCount == finishedCount + onlineStatusOfflineCount);
    }
}
