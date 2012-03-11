package jd.plugins;

import java.util.HashSet;

import jd.controlling.packagecontroller.ChildrenView;

import org.jdownloader.DomainInfo;

public class FilePackageView extends ChildrenView<DownloadLink> {

    /**
	 * 
	 */
    private static final long serialVersionUID  = -7310026158976695034L;

    private FilePackage       fp                = null;
    protected long            structureVersion  = 0;
    protected long            lastIconVersion   = -1;

    protected long            statusVersion     = 0;
    protected long            lastStatusVersion = -1;
    protected boolean         isEnabled         = false;

    public boolean isEnabled() {
        if (lastStatusVersion == statusVersion) return isEnabled;
        synchronized (this) {
            if (lastStatusVersion == statusVersion) return isEnabled;
            updateStatus();
        }
        return isEnabled;
    }

    protected FilePackageView(FilePackage fp) {
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
        synchronized (this) {
            structureVersion++;
            statusVersion++;
        }
    }

    public void changeVersion() {
        synchronized (this) {
            statusVersion++;
        }
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
        boolean newEnabled = false;
        lastStatusVersion = statusVersion;
        synchronized (fp) {
            for (DownloadLink link : fp.getChildren()) {
                if (!link.isEnabled()) continue;
                newSize += link.getDownloadSize();
                newEnabled = true;
            }
        }
        size = newSize;
        isEnabled = newEnabled;
    }

}
