package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.linkcrawler.CrawledLink.LinkState;
import jd.controlling.packagecontroller.ChildrenView;

import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.Priority;

public class CrawledPackageView extends ChildrenView<CrawledLink> {

    protected long                      fileSize;
    private DomainInfo[]                domainInfos;
    protected boolean                   enabled         = false;
    private int                         offline         = 0;
    private int                         online          = 0;
    private java.util.List<CrawledLink> items           = new ArrayList<CrawledLink>();
    private AtomicLong                  updatesRequired = new AtomicLong(0);
    private long                        updatesDone     = -1;
    private Priority                    lowestPriority;
    private Priority                    highestPriority;

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
            for (CrawledLink item : litems) {

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
            fileSize = newFileSize;
            enabled = newEnabled;
            this.lowestPriority = priorityLowset;
            this.highestPriority = priorityHighest;
            offline = newOffline;
            online = newOnline;
            updatesDone = lupdatesRequired;
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
            for (CrawledLink item : updatedItems) {
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
            fileSize = newFileSize;
            enabled = newEnabled;
            offline = newOffline;
            online = newOnline;
            this.lowestPriority = priorityLowset;
            this.highestPriority = priorityHighest;
            items = updatedItems;
            domainInfos = domains.toArray(new DomainInfo[] {});
            updatesDone = lupdatesRequired;
        }
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

}
