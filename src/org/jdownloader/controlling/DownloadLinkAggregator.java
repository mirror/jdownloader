package org.jdownloader.controlling;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class DownloadLinkAggregator {

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

    private int  onlineStatusUnkownCount;
    private int  enabledCount;
    private int  disabledCount;
    private long totalBytes;
    private long bytesLoaded;
    private int  finishedCount;

    private long eta;

    public DownloadLinkAggregator(FilePackage fp, boolean mirrorHandlingEnabled) {
        boolean readL = fp.getModifyLock().readLock();
        try {
            update(fp.getChildren(), mirrorHandlingEnabled);
        } finally {
            fp.getModifyLock().readUnlock(readL);
        }
    }

    private void update(List<DownloadLink> children, boolean mirrorHandlingEnabled) {
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
        HashMap<String, MirrorPackage> dupeSet = new HashMap<String, MirrorPackage>();
        MirrorPackage list;
        for (DownloadLink link : children) {
            if (mirrorHandlingEnabled) {
                String mirrorID = createDupeID(link);
                // TODO:Check if this can result in an endless loop
                while (true) {
                    list = dupeSet.get(mirrorID);
                    if (list == null) {
                        dupeSet.put(mirrorID, list = new MirrorPackage(mirrorID));
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
                bytesLoaded += link.getDownloadCurrent();
                bytesToDo += Math.max(0, link.getDownloadMax() - link.getDownloadSize());
                total++;
                if (link.getLinkStatus().isFinished()) {
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
