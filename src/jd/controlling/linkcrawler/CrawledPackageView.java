package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import jd.controlling.linkcrawler.CrawledLink.LinkState;
import jd.controlling.packagecontroller.ChildrenView;

import org.jdownloader.DomainInfo;

public class CrawledPackageView extends ChildrenView<CrawledLink> {

    /**
     * 
     */
    private static final long           serialVersionUID = 4415726693932960026L;
    protected long                      fileSize;
    private DomainInfo[]                domainInfos;
    protected boolean                   enabled          = false;
    private int                         offline          = 0;
    private int                         online           = 0;
    private java.util.List<CrawledLink> items            = new ArrayList<CrawledLink>();

    public CrawledPackageView() {
        this.fileSize = 0l;
        domainInfos = new DomainInfo[0];
    }

    public void update(ArrayList<CrawledLink> items) {
        CrawledPackageView newl = new CrawledPackageView();
        newl._update(items);
        this.fileSize = newl.fileSize;
        this.domainInfos = newl.domainInfos;
        this.enabled = newl.enabled;
        this.offline = newl.offline;
        this.online = newl.online;
        this.items = newl.items;
    }

    private void _update(java.util.List<CrawledLink> links) {
        HashMap<String, Long> names = new HashMap<String, Long>();
        TreeSet<DomainInfo> domains = new TreeSet<DomainInfo>();
        for (CrawledLink item : links) {
            // domain
            DomainInfo domainInfo = item.getDomainInfo();
            domains.add(domainInfo);
            // enabled
            if (item.isEnabled()) enabled = true;
            if (item.getLinkState() == LinkState.OFFLINE) {
                // offline
                offline++;
            } else if (item.getLinkState() == LinkState.ONLINE) {
                // online
                online++;
            }
            String name = item.getName();
            Long size = names.get(name);
            /* we use the largest filesize */
            if (size == null) {
                names.put(name, item.getSize());
                fileSize += item.getSize();
            } else if (size < item.getSize()) {
                fileSize -= size;
                names.put(name, item.getSize());
                fileSize += item.getSize();
            }
            items.add(item);
        }
        domainInfos = domains.toArray(new DomainInfo[] {});
    }

    public void clear() {
        fileSize = 0l;
        offline = 0;
        enabled = false;
        online = 0;
        items = new ArrayList<CrawledLink>();
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

}
