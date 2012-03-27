package jd.plugins;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.packagecontroller.ChildrenView;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.DomainInfo;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class FilePackageView extends ChildrenView<DownloadLink> {

    /**
	 * 
	 */
    private static final long   serialVersionUID  = -7310026158976695034L;

    private FilePackage         fp                = null;
    protected long              structureVersion  = 0;
    protected long              lastIconVersion   = -1;

    protected long              statusVersion     = 0;
    protected long              lastStatusVersion = -1;
    protected boolean           isEnabled         = false;
    protected long              finishedDate      = -1;

    protected long              lastStatsUpdate   = -1;
    protected static final long GUIUPDATETIMEOUT  = JsonConfig.create(GraphicalUserInterfaceSettings.class).getDownloadViewRefresh();

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
    private long         done  = 0;

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
        if (lastStatusVersion == statusVersion) return Math.max(done, size);
        synchronized (this) {
            if (lastStatusVersion == statusVersion) return Math.max(done, size);
            updateStatus();
        }
        return Math.max(done, size);
    }

    public long getDone() {
        boolean guiUpdate = false;
        if (fp.isEnabled() && System.currentTimeMillis() - lastStatsUpdate > GUIUPDATETIMEOUT) {
            guiUpdate = DownloadWatchDog.getInstance().hasRunningDownloads(fp);
        }
        if (lastStatusVersion == statusVersion && !guiUpdate) return done;
        synchronized (this) {
            if (guiUpdate && System.currentTimeMillis() - lastStatsUpdate < GUIUPDATETIMEOUT) {
                guiUpdate = false;
            }
            if (lastStatusVersion == statusVersion && !guiUpdate) return done;
            updateStatus();
        }
        return done;
    }

    private synchronized void updateStatus() {
        long newSize = 0;
        long newDone = 0;
        long newFinishedDate = -1;
        boolean newEnabled = false;
        lastStatusVersion = statusVersion;
        lastStatsUpdate = System.currentTimeMillis();
        HashSet<String> names = new HashSet<String>();
        synchronized (fp) {
            for (DownloadLink link : fp.getChildren()) {
                if (!link.isEnabled()) continue;
                newEnabled = true;
                if (names.add(link.getName())) {
                    newSize += link.getDownloadSize();
                    newDone += link.getDownloadCurrent();
                }
            }
        }
        size = newSize;
        done = newDone;
        isEnabled = newEnabled;
        finishedDate = newFinishedDate;
    }

    public boolean isFinished() {
        if (lastStatusVersion == statusVersion) return finishedDate > 0;
        synchronized (this) {
            if (lastStatusVersion == statusVersion) return finishedDate > 0;
            updateStatus();
        }
        return finishedDate > 0;
    }

    public long getFinishedDate() {
        if (lastStatusVersion == statusVersion) return finishedDate;
        synchronized (this) {
            if (lastStatusVersion == statusVersion) return finishedDate;
            updateStatus();
        }
        return finishedDate;
    }

    @Override
    public void update(ArrayList<DownloadLink> items) {
    }

    @Override
    public void clear() {
    }

    @Override
    public List<DownloadLink> getItems() {
        return fp.getChildren();
    }
}
