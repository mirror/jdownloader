package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.packagecontroller.ChildrenView;
import jd.plugins.DownloadLink;

import org.appwork.utils.StringUtils;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.AvailabilityColumn;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;

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
        Temp tmp = new Temp();

        synchronized (this) {
            /* this is called for repaint, so only update values that could have changed for existing items */
            List<CrawledLink> updatedItems = getItems();

            for (CrawledLink item : updatedItems) {
                addtoTmp(tmp, item);

            }

            writeTmpToFields(tmp);
            updateAvailability(updatedItems.size(), tmp.newOffline, tmp.newOnline);
            availabilityColumnString = _GUI._.AvailabilityColumn_getStringValue_object_(tmp.newOnline, updatedItems.size());
        }
    }

    public Priority getLowestPriority() {
        return lowestPriority;
    }

    public Priority getHighestPriority() {
        return highestPriority;
    }

    private class Temp {
        HashMap<String, Long> names             = new HashMap<String, Long>();
        TreeSet<DomainInfo>   domains           = new TreeSet<DomainInfo>();
        int                   newOnline         = 0;
        long                  newFileSize       = 0;
        boolean               newEnabled        = false;
        int                   newOffline        = 0;
        Priority              priorityLowset    = Priority.HIGHEST;
        Priority              priorityHighest   = Priority.LOWER;

        String                sameSource        = null;
        boolean               sameSourceFullUrl = true;
        long                  lupdatesRequired  = updatesRequired.get();
    }

    public void setItems(List<CrawledLink> updatedItems) {
        Temp tmp = new Temp();

        synchronized (this) {

            /* this is called for tablechanged, so update everything for given items */

            for (CrawledLink item : updatedItems) {
                // domain
                tmp.domains.add(item.getDomainInfo());
                addtoTmp(tmp, item);
            }
            writeTmpToFields(tmp);

            ArrayList<DomainInfo> lst = new ArrayList<DomainInfo>(tmp.domains);
            Collections.sort(lst, new Comparator<DomainInfo>() {

                @Override
                public int compare(DomainInfo o1, DomainInfo o2) {
                    return o1.getTld().compareTo(o2.getTld());
                }
            });

            domainInfos = lst.toArray(new DomainInfo[] {});
            updateAvailability(updatedItems.size(), tmp.newOffline, tmp.newOnline);
            items = updatedItems;
            availabilityColumnString = _GUI._.AvailabilityColumn_getStringValue_object_(tmp.newOnline, updatedItems.size());
        }
    }

    protected void writeTmpToFields(Temp tmp) {
        this.commonSourceUrl = tmp.sameSource;
        if (!tmp.sameSourceFullUrl) {
            commonSourceUrl += "[...]";
        }
        fileSize = tmp.newFileSize;
        enabled = tmp.newEnabled;
        offline = tmp.newOffline;
        online = tmp.newOnline;

        this.lowestPriority = tmp.priorityLowset;
        this.highestPriority = tmp.priorityHighest;

        updatesDone = tmp.lupdatesRequired;

    }

    protected void addtoTmp(Temp tmp, CrawledLink link) {
        DownloadLink dlLink = ((CrawledLink) link).getDownloadLink();
        String sourceUrl = null;
        if (dlLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
            if (dlLink.gotBrowserUrl()) sourceUrl = dlLink.getBrowserUrl();
        } else {
            sourceUrl = dlLink.getBrowserUrl();
        }
        if (sourceUrl != null) {
            tmp.sameSource = StringUtils.getCommonalities(tmp.sameSource, sourceUrl);
            tmp.sameSourceFullUrl = tmp.sameSourceFullUrl && tmp.sameSource.equals(sourceUrl);
        }

        if (link.getPriority().getId() < tmp.priorityLowset.getId()) {
            tmp.priorityLowset = link.getPriority();
        }
        if (link.getPriority().getId() > tmp.priorityHighest.getId()) {
            tmp.priorityHighest = link.getPriority();
        }

        // enabled
        if (link.isEnabled()) tmp.newEnabled = true;
        if (link.getLinkState() == AvailableLinkState.OFFLINE) {
            // offline
            tmp.newOffline++;
        } else if (link.getLinkState() == AvailableLinkState.ONLINE) {
            // online
            tmp.newOnline++;
        }
        String name = link.getName();
        Long size = tmp.names.get(name);
        /* we use the largest filesize */
        long itemSize = Math.max(0, link.getSize());
        if (size == null) {
            tmp.names.put(name, itemSize);
            tmp.newFileSize += itemSize;
        } else if (size < itemSize) {
            tmp.newFileSize -= size;
            tmp.names.put(name, itemSize);
            tmp.newFileSize += itemSize;
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
