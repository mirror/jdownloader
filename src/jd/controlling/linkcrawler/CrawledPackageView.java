package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.linkcrawler.CrawledLink.LinkState;
import jd.controlling.packagecontroller.ChildrenView;
import jd.plugins.DownloadLink;

import org.appwork.utils.StringUtils;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.AvailabilityColumn;

public class CrawledPackageView extends ChildrenView<CrawledLink> {

    protected long                      fileSize;
    private DomainInfo[]                domainInfos;
    protected boolean                   enabled                  = false;
    private int                         offline                  = 0;
    private int                         online                   = 0;
    private java.util.List<CrawledLink> items                    = new ArrayList<CrawledLink>();
    private AtomicLong                  updatesRequired          = new AtomicLong(0);
    private long                        updatesDone              = -1;
    private Priority                    lowestPriority;
    private Priority                    highestPriority;
    private String                      commonSourceUrl;
    private String                      availabilityColumnString = null;
    private ChildrenAvailablility       availability             = ChildrenAvailablility.UNKNOWN;

    public CrawledPackageView() {
        this.fileSize = 0l;
        domainInfos = new DomainInfo[0];

    }

    @Override
    public void aggregate() {
        long lupdatesRequired = updatesRequired.get();
        synchronized (this) {
            /* this is called for repaint, so only update values that could have changed for existing items */
            List<CrawledLink> litems = getItems();
            boolean newEnabled = false;
            int newOffline = 0;
            Priority priorityLowset = Priority.HIGHEST;
            Priority priorityHighest = Priority.LOWER;
            int newOnline = 0;
            long newFileSize = 0;
            HashMap<String, Long> names = new HashMap<String, Long>();
            String sameSource = null;
            boolean sameSourceFullUrl = true;
            for (CrawledLink item : litems) {

                DownloadLink dlLink = ((CrawledLink) item).getDownloadLink();
                String sourceUrl = dlLink.getBrowserUrl();

                if (sourceUrl != null) {
                    sameSource = StringUtils.getCommonalities(sameSource, sourceUrl);
                    sameSourceFullUrl = sameSourceFullUrl && sameSource.equals(sourceUrl);
                }
                if (item.getPriority().ordinal() < priorityLowset.ordinal()) {
                    priorityLowset = item.getPriority();
                }
                if (item.getPriority().ordinal() > priorityHighest.ordinal()) {
                    priorityHighest = item.getPriority();
                }
                // enabled
                if (item.isEnabled()) newEnabled = true;
                if (item.getLinkState() == LinkState.OFFLINE) {
                    // offline
                    newOffline++;
                } else if (item.getLinkState() == LinkState.ONLINE) {
                    // online
                    newOnline++;
                }
                String name = item.getName();
                Long size = names.get(name);
                /* we use the largest filesize */
                if (size == null) {
                    names.put(name, item.getSize());
                    newFileSize += item.getSize();
                } else if (size < item.getSize()) {
                    newFileSize -= size;
                    names.put(name, item.getSize());
                    newFileSize += item.getSize();
                }
            }
            this.commonSourceUrl = sameSource;
            if (!sameSourceFullUrl) {
                commonSourceUrl += "[...]";
            }
            fileSize = newFileSize;
            enabled = newEnabled;
            this.lowestPriority = priorityLowset;
            this.highestPriority = priorityHighest;
            offline = newOffline;
            online = newOnline;
            updateAvailability(litems.size(), newOffline, newOnline);
            updatesDone = lupdatesRequired;
            availabilityColumnString = _GUI._.AvailabilityColumn_getStringValue_object_(newOnline, litems.size());
        }
    }

    public Priority getLowestPriority() {
        return lowestPriority;
    }

    public Priority getHighestPriority() {
        return highestPriority;
    }

    public void setItems(List<CrawledLink> updatedItems) {
        long lupdatesRequired = updatesRequired.get();
        synchronized (this) {
            /* this is called for tablechanged, so update everything for given items */
            boolean newEnabled = false;
            int newOffline = 0;
            Priority priorityLowset = Priority.HIGHEST;
            Priority priorityHighest = Priority.LOWER;
            int newOnline = 0;
            long newFileSize = 0;
            HashMap<String, Long> names = new HashMap<String, Long>();
            TreeSet<DomainInfo> domains = new TreeSet<DomainInfo>();
            String sameSource = null;
            boolean sameSourceFullUrl = true;
            for (CrawledLink item : updatedItems) {

                DownloadLink dlLink = ((CrawledLink) item).getDownloadLink();
                String sourceUrl = dlLink.getBrowserUrl();
                if (sourceUrl != null) {
                    sameSource = StringUtils.getCommonalities(sameSource, sourceUrl);
                    sameSourceFullUrl = sameSourceFullUrl && sameSource.equals(sourceUrl);
                }
                if (item.getPriority().ordinal() < priorityLowset.ordinal()) {
                    priorityLowset = item.getPriority();
                }
                if (item.getPriority().ordinal() > priorityHighest.ordinal()) {
                    priorityHighest = item.getPriority();
                }
                // domain
                domains.add(item.getDomainInfo());
                // enabled
                if (item.isEnabled()) newEnabled = true;
                if (item.getLinkState() == LinkState.OFFLINE) {
                    // offline
                    newOffline++;
                } else if (item.getLinkState() == LinkState.ONLINE) {
                    // online
                    newOnline++;
                }
                String name = item.getName();
                Long size = names.get(name);
                /* we use the largest filesize */
                if (size == null) {
                    names.put(name, item.getSize());
                    newFileSize += item.getSize();
                } else if (size < item.getSize()) {
                    newFileSize -= size;
                    names.put(name, item.getSize());
                    newFileSize += item.getSize();
                }
            }
            this.commonSourceUrl = sameSource;
            if (!sameSourceFullUrl) {
                commonSourceUrl += "[...]";
            }
            fileSize = newFileSize;
            enabled = newEnabled;
            offline = newOffline;
            online = newOnline;
            updateAvailability(updatedItems.size(), newOffline, newOnline);
            this.lowestPriority = priorityLowset;
            this.highestPriority = priorityHighest;
            items = updatedItems;
            domainInfos = domains.toArray(new DomainInfo[] {});
            updatesDone = lupdatesRequired;
            availabilityColumnString = _GUI._.AvailabilityColumn_getStringValue_object_(newOnline, updatedItems.size());
        }
    }

    private final void updateAvailability(int size, int offline, int online) {
        if (online == size) {
            availability = ChildrenAvailablility.ONLINE;
            return;
        }
        if (offline == size) {
            availability = ChildrenAvailablility.OFFLINE;
            return;
        }
        if ((offline == 0 && online == 0) || (online == 0 && offline > 0)) {
            availability = ChildrenAvailablility.UNKNOWN;
            return;
        }
        availability = ChildrenAvailablility.MIXED;
        return;
    }

    public String getCommonSourceUrl() {
        return commonSourceUrl;
    }

    public void clear() {
        items = new ArrayList<CrawledLink>();
        domainInfos = new DomainInfo[0];
    }

    public DomainInfo[] getDomainInfos() {
        return domainInfos;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getOfflineCount() {
        return offline;
    }

    public int getOnlineCount() {
        return online;
    }

    public long getFileSize() {
        return fileSize;
    }

    @Override
    public List<CrawledLink> getItems() {
        return items;
    }

    @Override
    public void requestUpdate() {
        updatesRequired.incrementAndGet();
    }

    @Override
    public boolean updateRequired() {
        return updatesRequired.get() != updatesDone;
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
