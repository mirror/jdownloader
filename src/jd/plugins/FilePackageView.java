package jd.plugins;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.packagecontroller.ChildrenView;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.download.DownloadInterface;

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
    protected boolean           lastRunningState  = false;
    protected long              finishedDate      = -1;
    protected long              estimatedETA      = -1;

    private int                 offline           = 0;
    private int                 online            = 0;

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

    public FilePackageView(FilePackage fp) {
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
                    infos.add(link.getDomainInfo(true));
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

    public long getETA() {
        boolean guiUpdate = false;
        if (fp.isEnabled() && System.currentTimeMillis() - lastStatsUpdate > GUIUPDATETIMEOUT) {
            guiUpdate = DownloadWatchDog.getInstance().hasRunningDownloads(fp);
        }
        if (lastStatusVersion == statusVersion && guiUpdate == lastRunningState && !guiUpdate) return estimatedETA;
        synchronized (this) {
            if (guiUpdate && System.currentTimeMillis() - lastStatsUpdate < GUIUPDATETIMEOUT) {
                guiUpdate = false;
            }
            if (lastStatusVersion == statusVersion && guiUpdate == lastRunningState && !guiUpdate) return estimatedETA;
            updateStatus();
        }
        return estimatedETA;
    }

    private synchronized void updateStatus() {
        long newSize = 0;
        long newDone = 0;
        long newFinishedDate = -1;
        int newOffline = 0;
        int newOnline = 0;
        boolean newEnabled = false;

        long fpETA = -1;
        long fpTODO = 0;
        long fpSPEED = 0;
        boolean sizeKnown = false;
        boolean fpRunning = false;
        lastStatusVersion = statusVersion;
        lastStatsUpdate = System.currentTimeMillis();
        HashSet<String> names = new HashSet<String>();
        boolean allFinished = true;
        synchronized (fp) {
            for (DownloadLink link : fp.getChildren()) {
                if (AvailableStatus.FALSE == link.getAvailableStatus()) {
                    // offline
                    newOffline++;
                } else if (AvailableStatus.TRUE == link.getAvailableStatus()) {
                    // online
                    newOnline++;
                }
                if (link.isEnabled()) {
                    /*
                     * we still have enabled links, so package must be enabled too
                     */
                    newEnabled = true;
                }
                if (names.add(link.getName())) {
                    /* only counts unique filenames */
                    newSize += link.getDownloadSize();
                    newDone += link.getDownloadCurrent();
                    /* ETA calculation */
                    if (link.isEnabled() && !link.getLinkStatus().isFinished()) {
                        /* link must be enabled and not finished state */
                        boolean linkRunning = link.getLinkStatus().isPluginActive();
                        if (linkRunning) {
                            fpRunning = true;
                        }
                        if (link.getDownloadMax() >= 0) {
                            /* we know at least one filesize */
                            sizeKnown = true;
                        }
                        long linkTodo = Math.max(0, link.getDownloadSize() - link.getDownloadCurrent());
                        DownloadInterface dli = link.getDownloadInstance();
                        long linkSpeed = link.getDownloadSpeed();
                        if (dli == null || (System.currentTimeMillis() - dli.getStartTimeStamp()) < 5000) {
                            /* wait at least 5 secs when download is running, to avoid speed fluctuations in overall ETA */
                            linkSpeed = 0;
                        }
                        fpSPEED += linkSpeed;
                        fpTODO += linkTodo;
                        if (fpSPEED > 0) {
                            /* we have ongoing downloads, lets calculate ETA */
                            long newfpETA = fpTODO / fpSPEED;
                            if (newfpETA > fpETA) {
                                fpETA = newfpETA;
                            }
                        }
                        if (linkSpeed > 0) {
                            /* link is running,lets calc ETA for single link */
                            long currentETA = linkTodo / linkSpeed;
                            if (currentETA > fpETA) {
                                /*
                                 * ETA for single link is bigger than ETA for all, so we use the bigger one
                                 */
                                fpETA = currentETA;
                            }
                        }
                    }
                }
                if (!link.getLinkStatus().isFinished() && link.isEnabled()) {
                    /* we still have an enabled link which is not finished */
                    allFinished = false;
                } else if (allFinished && link.getFinishedDate() > newFinishedDate) {
                    /*
                     * we can set latest finished date because all links till now are finished
                     */
                    newFinishedDate = link.getFinishedDate();
                }
            }
        }
        size = newSize;
        done = newDone;
        isEnabled = newEnabled;
        if (allFinished) {
            /* all links have reached finished state */
            finishedDate = newFinishedDate;
        } else {
            /* not all have finished */
            finishedDate = -1;
        }
        lastRunningState = fpRunning;
        if (fpRunning) {
            if (sizeKnown && fpSPEED > 0) {
                /* we could calc an ETA because at least one filesize is known */
                estimatedETA = fpETA;
            } else {
                /* no filesize is known, we use Integer.Min_value to signal this */
                estimatedETA = Integer.MIN_VALUE;
            }
        } else {
            /* no download running */
            estimatedETA = -1;
        }
        offline = newOffline;
        online = newOnline;
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

    public int getOfflineCount() {
        return offline;
    }

    public int getOnlineCount() {
        return online;
    }
}
