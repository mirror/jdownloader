package jd.plugins;

import java.util.HashSet;

import org.jdownloader.DomainInfo;

public class FilePackageInfo {

    private FilePackage fp                = null;
    protected long      structureVersion  = 0;
    protected long      lastIconVersion   = -1;

    protected long      statusVersion     = 0;
    protected long      lastStatusVersion = -1;

    protected FilePackageInfo(FilePackage fp) {
        this.fp = fp;
    }

    private DomainInfo[] infos = new DomainInfo[0];
    private long         size  = 0;

    public DomainInfo[] getDomainInfos() {
        if (lastIconVersion == structureVersion) return infos;
        synchronized (this) {
            if (lastIconVersion == structureVersion) return infos;
            HashSet<DomainInfo> infos = new HashSet<DomainInfo>();
            synchronized (fp) {
                for (DownloadLink link : fp.getChildren()) {
                    infos.add(link.getDomainInfo());
                }
            }
            DomainInfo[] newinfos = new DomainInfo[infos.size()];
            int index = 0;
            for (DomainInfo info : infos) {
                newinfos[index++] = info;
            }
            this.infos = newinfos;
            lastIconVersion = structureVersion;
        }
        return infos;
    }

    public void changeStructure() {
        structureVersion++;
        statusVersion++;
    }

    public long getSize() {
        if (lastStatusVersion == statusVersion) return size;
        synchronized (this) {
            if (lastStatusVersion == statusVersion) return size;
            updateStatus();
        }
        return size;
    }

    private synchronized void updateStatus() {
        long newSize = 0;
        synchronized (fp) {
            for (DownloadLink link : fp.getChildren()) {
                if (!link.isEnabled()) continue;
                newSize += link.getDownloadSize();
            }
        }
        size = newSize;
        lastStatusVersion = statusVersion;
    }
}
