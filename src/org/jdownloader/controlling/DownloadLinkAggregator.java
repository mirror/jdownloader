package org.jdownloader.controlling;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.extensions.extraction.ExtractionStatus;
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
        return onlineStatusUnkownCount;
    }

    public int getEnabledCount() {
        return enabledCount;
    }

    public int getDisabledCount() {
        return disabledCount;
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

    private int     onlineStatusUnkownCount;
    private int     enabledCount;
    private int     disabledCount;
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
        boolean readL = fp.getModifyLock().readLock();
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

    public void update(List<DownloadLink> children) {
        int total = 0;
        int enabled = 0;
        int disabled = 0;
        int offline = 0;
        int online = 0;
        int unknownOnlineStatus = 0;
        long totalBytes = 0;
        long bytesLoaded = 0;
        long bytesToDo = 0;
        int finished = 0;
        long speed = 0;
        int localFileCount = 0;
        HashMap<String, MirrorPackage> dupeSet = new HashMap<String, MirrorPackage>();
        MirrorPackage list;
        for (DownloadLink link : children) {
            if (isMirrorHandlingEnabled()) {
                String mirrorID = createDupeID(link);
                // TODO:Check if this can result in an endless loop
                while (true) {
                    list = dupeSet.get(mirrorID);
                    if (list == null) {
                        dupeSet.put(mirrorID, list = new MirrorPackage(mirrorID, this));
                    }
                    String newID = list.add(link);
                    if (newID != null) {
                        mirrorID = newID;
                    } else {
                        break;
                    }
                }
            } else {
                speed += link.getDownloadSpeed();
                totalBytes += link.getDownloadMax();
                if (isLocalFileUsageEnabled()) {
                    File a = new File(link.getFileOutput() + ".part");
                    if (a.exists()) {
                        bytesLoaded += a.length();
                        localFileCount++;
                    } else {
                        a = new File(link.getFileOutput());
                        if (a.exists()) {
                            bytesLoaded += a.length();
                            localFileCount++;
                        }
                    }

                } else {
                    bytesLoaded += link.getDownloadCurrent();
                }
                bytesToDo += Math.max(0, link.getDownloadMax() - link.getDownloadSize());
                total++;
                if (FinalLinkState.CheckFinished(link.getFinalLinkState()) && (link.getExtractionStatus() == ExtractionStatus.SUCCESSFUL || new File(link.getFileOutput()).exists())) {
                    finished++;
                }
                if (link.isEnabled()) {
                    enabled++;
                } else {
                    disabled++;
                }
                switch (link.getAvailableStatus()) {
                case FALSE:
                    offline++;
                    break;
                case TRUE:
                    online++;
                    break;
                default:
                    unknownOnlineStatus++;
                }
            }

        }
        if (mirrorHandlingEnabled) {
            for (Entry<String, MirrorPackage> e : dupeSet.entrySet()) {
                list = e.getValue();
                totalBytes += list.getTotalBytes();
                bytesLoaded += list.getBytesLoaded();
                if (list.getBytesLoaded() > 0 && isLocalFileUsageEnabled()) {
                    localFileCount++;
                }
                bytesToDo += Math.max(0, list.getTotalBytes() - list.getBytesLoaded());
                speed += list.getSpeed();
                total++;
                if (list.isFinished()) {
                    finished++;
                }
                if (list.isEnabled()) {
                    enabled++;
                } else {
                    disabled++;
                }
                if (list.isOffline()) {
                    offline++;
                } else if (list.isOnline()) {
                    online++;
                } else if (list.isUnknownOnlineStatus()) {
                    unknownOnlineStatus++;
                }

            }

        }
        this.localFileCount = localFileCount;
        this.totalCount = total;
        this.onlineStatusOfflineCount = offline;
        this.onlineStatusOnlineCount = online;
        this.onlineStatusUnkownCount = unknownOnlineStatus;
        this.enabledCount = enabled;
        this.disabledCount = disabled;
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
        if (!isLocalFileUsageEnabled()) throw new IllegalStateException("isLocalFileUsageEnabled() is disabled");
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
        return finishedCount == totalCount;
    }
}
