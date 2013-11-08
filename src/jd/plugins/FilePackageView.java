package jd.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.packagecontroller.ChildrenView;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.download.DownloadInterface;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.AvailabilityColumn;
import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class FilePackageView extends ChildrenView<DownloadLink> {

    private FilePackage                  fp                       = null;

    protected volatile long              lastUpdateTimestamp      = -1;

    protected boolean                    lastRunningState         = false;
    protected long                       finishedDate             = -1;
    protected long                       estimatedETA             = -1;

    private int                          offline                  = 0;
    private int                          online                   = 0;
    private AtomicLong                   updatesRequired          = new AtomicLong(0);
    private long                         updatesDone              = -1;
    private String                       availabilityColumnString = null;
    private ChildrenAvailablility        availability             = ChildrenAvailablility.UNKNOWN;

    private java.util.List<DownloadLink> items                    = new ArrayList<DownloadLink>();

    protected static final long          GUIUPDATETIMEOUT         = JsonConfig.create(GraphicalUserInterfaceSettings.class).getDownloadViewRefresh();

    public boolean isEnabled() {
        return enabledCount > 0;
    }

    /**
     * This constructor is protected. do not use this class outside the FilePackage
     * 
     * @param fp
     */
    protected FilePackageView(FilePackage fp) {
        this.fp = fp;
    }

    private DomainInfo[]                   infos = new DomainInfo[0];
    private long                           size  = 0;
    private long                           done  = 0;

    private int                            enabledCount;

    private Priority                       lowestPriority;

    private Priority                       highestPriority;

    private HashSet<ConditionalSkipReason> skipReasons;

    public HashSet<ConditionalSkipReason> getSkipReasons() {
        return skipReasons;
    }

    public DomainInfo[] getDomainInfos() {
        return infos;
    }

    public long getSize() {
        return Math.max(done, size);
    }

    public long getDone() {
        return done;
    }

    public long getETA() {
        return estimatedETA;
    }

    public boolean isFinished() {
        return finishedDate > 0;
    }

    public int getDisabledCount() {
        return Math.max(0, getItems().size() - enabledCount);
    }

    public long getFinishedDate() {
        return finishedDate;
    }

    @Override
    public void aggregate() {

        long lupdatesRequired = updatesRequired.get();
        lastUpdateTimestamp = System.currentTimeMillis();
        synchronized (this) {
            /* this is called for tablechanged, so update everything for given items */
            Temp tmp = new Temp();

            boolean readL = fp.getModifyLock().readLock();

            try {
                tmp.children = fp.getChildren().size();
                for (DownloadLink link : fp.getChildren()) {

                    addLinkToTemp(tmp, link);
                }
            } finally {
                fp.getModifyLock().readUnlock(readL);
            }
            for (Long size : tmp.downloadSizes.values()) {
                tmp.newSize += size;
            }
            for (Long done : tmp.downloadDone.values()) {
                tmp.newDone += done;
            }
            writeTempToFields(tmp);
            updatesDone = lupdatesRequired;

        }
    }

    public Priority getLowestPriority() {
        return lowestPriority;
    }

    public Priority getHighestPriority() {
        return highestPriority;
    }

    private class Temp {
        private long                           newSize         = 0;
        private long                           newDone         = 0;
        private long                           newFinishedDate = -1;
        private int                            newOffline      = 0;
        private int                            newOnline       = 0;
        private int                            newEnabledCount = 0;
        private int                            children        = 0;
        private long                           fpETA           = -1;
        private long                           fpTODO          = 0;
        private Priority                       priorityLowset  = Priority.HIGHEST;
        private Priority                       priorityHighest = Priority.LOWER;
        private long                           fpSPEED         = 0;
        private boolean                        sizeKnown       = false;
        private boolean                        fpRunning       = false;
        private HashMap<String, Long>          downloadSizes   = new HashMap<String, Long>();
        private HashMap<String, Long>          downloadDone    = new HashMap<String, Long>();
        private HashSet<String>                eta             = new HashSet<String>();
        private HashSet<DomainInfo>            newInfos        = new HashSet<DomainInfo>();
        private boolean                        allFinished     = true;
        private HashSet<ConditionalSkipReason> skipReasons     = new HashSet<ConditionalSkipReason>();
    }

    @Override
    public void setItems(List<DownloadLink> updatedItems) {
        long lupdatesRequired = updatesRequired.get();
        lastUpdateTimestamp = System.currentTimeMillis();
        synchronized (this) {
            /* this is called for tablechanged, so update everything for given items */
            Temp tmp = new Temp();

            boolean readL = fp.getModifyLock().readLock();

            try {
                tmp.children = fp.getChildren().size();
                for (DownloadLink link : fp.getChildren()) {

                    tmp.newInfos.add(link.getDomainInfo());

                    addLinkToTemp(tmp, link);
                }
            } finally {
                fp.getModifyLock().readUnlock(readL);
            }
            for (Long size : tmp.downloadSizes.values()) {
                tmp.newSize += size;
            }
            for (Long done : tmp.downloadDone.values()) {
                tmp.newDone += done;
            }
            writeTempToFields(tmp);
            items = updatedItems;
            updatesDone = lupdatesRequired;
            infos = tmp.newInfos.toArray(new DomainInfo[tmp.newInfos.size()]);
        }
    }

    protected void writeTempToFields(Temp tmp) {
        size = tmp.newSize;
        done = tmp.newDone;
        this.enabledCount = tmp.newEnabledCount;
        if (tmp.allFinished) {
            /* all links have reached finished state */
            finishedDate = tmp.newFinishedDate;
        } else {
            /* not all have finished */
            finishedDate = -1;
        }
        lastRunningState = tmp.fpRunning;
        if (tmp.fpRunning) {
            if (tmp.sizeKnown && tmp.fpSPEED > 0) {
                /* we could calc an ETA because at least one filesize is known */
                estimatedETA = tmp.fpETA;
            } else {
                /* no filesize is known, we use Integer.Min_value to signal this */
                estimatedETA = Integer.MIN_VALUE;
            }
        } else {
            /* no download running */
            estimatedETA = -1;
        }
        offline = tmp.newOffline;
        online = tmp.newOnline;
        updateAvailability(tmp);
        this.lowestPriority = tmp.priorityLowset;
        this.highestPriority = tmp.priorityHighest;

        availabilityColumnString = _GUI._.AvailabilityColumn_getStringValue_object_(tmp.newOnline, tmp.children);
    }

    protected void addLinkToTemp(Temp tmp, DownloadLink link) {
        if (link.getPriorityEnum().ordinal() < tmp.priorityLowset.ordinal()) {
            tmp.priorityLowset = link.getPriorityEnum();
        }
        if (link.getPriorityEnum().ordinal() > tmp.priorityHighest.ordinal()) {
            tmp.priorityHighest = link.getPriorityEnum();
        }

        if (AvailableStatus.FALSE == link.getAvailableStatus()) {
            // offline
            tmp.newOffline++;
        } else if (AvailableStatus.TRUE == link.getAvailableStatus()) {
            // online
            tmp.newOnline++;
        }

        ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
        if (conditionalSkipReason != null && !conditionalSkipReason.isConditionReached()) {
            tmp.skipReasons.add(conditionalSkipReason);
        }
        if (link.isEnabled()) {
            /*
             * we still have enabled links, so package must be enabled too
             */

            tmp.newEnabledCount++;
        }
        Long downloadSize = tmp.downloadSizes.get(link.getName());
        if (downloadSize == null) {
            tmp.downloadSizes.put(link.getName(), link.getDownloadSize());
            tmp.downloadDone.put(link.getName(), link.getDownloadCurrent());
        } else {
            if (!tmp.eta.contains(link.getName())) {
                if (link.isEnabled()) {
                    tmp.downloadSizes.put(link.getName(), link.getDownloadSize());
                    tmp.downloadDone.put(link.getName(), link.getDownloadCurrent());
                } else if (downloadSize < link.getDownloadSize()) {
                    tmp.downloadSizes.put(link.getName(), link.getDownloadSize());
                    tmp.downloadDone.put(link.getName(), link.getDownloadCurrent());
                }
            }
        }

        /* ETA calculation */
        if (link.isEnabled() && link.getFinalLinkState() == null) {
            /* link must be enabled and not finished state */
            boolean linkRunning = link.getDownloadLinkController() != null;
            if (linkRunning || tmp.eta.contains(link.getName()) == false) {
                if (linkRunning) {
                    tmp.fpRunning = true;
                    tmp.eta.add(link.getName());
                }

                if (link.getKnownDownloadSize() >= 0) {
                    /* we know at least one filesize */
                    tmp.sizeKnown = true;
                }
                long linkTodo = Math.max(0, link.getDownloadSize() - link.getDownloadCurrent());
                SingleDownloadController sdc = link.getDownloadLinkController();
                DownloadInterface dli = null;
                if (sdc != null) dli = sdc.getDownloadInstance();
                long linkSpeed = link.getDownloadSpeed();
                if (dli == null || (System.currentTimeMillis() - dli.getStartTimeStamp()) < 5000) {
                    /* wait at least 5 secs when download is running, to avoid speed fluctuations in overall ETA */
                    linkSpeed = 0;
                }
                tmp.fpSPEED += linkSpeed;
                tmp.fpTODO += linkTodo;
                if (tmp.fpSPEED > 0) {
                    /* we have ongoing downloads, lets calculate ETA */
                    long newfpETA = tmp.fpTODO / tmp.fpSPEED;
                    if (newfpETA > tmp.fpETA) {
                        tmp.fpETA = newfpETA;
                    }
                }
                if (linkSpeed > 0) {
                    /* link is running,lets calc ETA for single link */
                    long currentETA = linkTodo / linkSpeed;
                    if (currentETA > tmp.fpETA) {
                        /*
                         * ETA for single link is bigger than ETA for all, so we use the bigger one
                         */
                        tmp.fpETA = currentETA;
                    }
                }
            }
        }

        if (link.isEnabled() && link.getFinalLinkState() == null) {
            /* we still have an enabled link which is not finished */
            tmp.allFinished = false;
        } else if (tmp.allFinished && link.getFinishedDate() > tmp.newFinishedDate) {
            /*
             * we can set latest finished date because all links till now are finished
             */
            tmp.newFinishedDate = link.getFinishedDate();
        }
    }

    @Override
    public void clear() {
        infos = new DomainInfo[0];
        items = new ArrayList<DownloadLink>();
    }

    @Override
    public List<DownloadLink> getItems() {
        return items;
    }

    public int getOfflineCount() {
        return offline;
    }

    public int getOnlineCount() {
        return online;
    }

    @Override
    public void requestUpdate() {
        updatesRequired.incrementAndGet();
    }

    @Override
    public boolean updateRequired() {
        boolean ret = updatesRequired.get() != updatesDone;
        if (ret == false) {
            ret = fp.isEnabled() && (System.currentTimeMillis() - lastUpdateTimestamp > GUIUPDATETIMEOUT) && DownloadWatchDog.getInstance().hasRunningDownloads(fp);
        }
        return ret;
    }

    private final void updateAvailability(Temp tmp) {

        if (online == tmp.children) {
            availability = ChildrenAvailablility.ONLINE;
            return;
        }
        if (offline == size) {
            availability = ChildrenAvailablility.OFFLINE;
            return;
        }
        if ((tmp.newOffline == 0 && tmp.newOnline == 0) || (tmp.newOnline == 0 && tmp.newOffline > 0)) {
            availability = ChildrenAvailablility.UNKNOWN;
            return;
        }
        availability = ChildrenAvailablility.MIXED;
        return;
    }

    @Override
    public ChildrenAvailablility getAvailability() {
        return availability;
    }

    @Override
    public String getMessage(Object requestor) {
        if (requestor instanceof AvailabilityColumn) return availabilityColumnString;
        return null;
    }

}
