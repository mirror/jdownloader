package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import jd.controlling.linkcrawler.CrawledLink.LinkState;
import jd.controlling.packagecontroller.ChildrenView;

import org.appwork.exceptions.WTFException;
import org.jdownloader.DomainInfo;

public class CrawledPackageView extends ChildrenView<CrawledLink> {

    /**
     * 
     */
    private static final long              serialVersionUID = 4415726693932960026L;
    protected long                         fileSize;
    protected HashMap<DomainInfo, Integer> hostCountMap;
    protected TreeSet<DomainInfo>          domainList;
    private DomainInfo[]                   domainInfos;
    protected HashSet<CrawledLink>         enabled;
    protected HashMap<CrawledLink, Long>   sizes;
    private HashSet<CrawledLink>           offline;
    private HashSet<CrawledLink>           online;
    private ArrayList<CrawledLink>         items            = new ArrayList<CrawledLink>();

    public CrawledPackageView() {
        this.fileSize = 0l;
        hostCountMap = new HashMap<DomainInfo, Integer>();
        domainList = new TreeSet<DomainInfo>();
        enabled = new HashSet<CrawledLink>();
        offline = new HashSet<CrawledLink>();
        online = new HashSet<CrawledLink>();
        sizes = new HashMap<CrawledLink, Long>();
        domainInfos = new DomainInfo[0];
    }

    public void update(ArrayList<CrawledLink> items) {
        CrawledPackageView newl = new CrawledPackageView();
        for (CrawledLink item : items) {
            newl.addInfo(item);
        }
        this.fileSize = newl.fileSize;
        this.hostCountMap = newl.hostCountMap;
        this.domainList = newl.domainList;
        this.domainInfos = newl.domainInfos;
        this.enabled = newl.enabled;
        this.sizes = newl.sizes;
        this.offline = newl.offline;
        this.online = newl.online;
        this.items = newl.items;
    }

    protected void addInfo(CrawledLink element) {
        // domain
        DomainInfo domainInfo = element.getDomainInfo();
        Integer current = hostCountMap.get(domainInfo);
        if (current == null) current = 0;
        hostCountMap.put(domainInfo, current + 1);
        domainList.add(domainInfo);
        domainInfos = domainList.toArray(new DomainInfo[] {});
        // enabled
        if (element.isEnabled()) enabled.add(element);
        if (element.getLinkState() == LinkState.OFFLINE) {
            // offline
            offline.add(element);
        } else if (element.getLinkState() == LinkState.ONLINE) {
            // online
            online.add(element);
        }
        // size
        sizes.put(element, element.getSize());
        fileSize += sizes.get(element);
        items.add(element);
        // System.out.println(element + " add: " + crawledPackage.getName()
        // + " : " + hostCountMap + " " + domainList);
    }

    protected void removeInfo(CrawledLink element) {
        if (!items.remove(element)) return;
        DomainInfo domainInfo = element.getDomainInfo();
        Integer current = hostCountMap.get(domainInfo);
        if (current == null || current < 1) throw new WTFException("cannot remove element. Is not there");

        if (current == 1) {
            hostCountMap.remove(domainInfo);
            domainList.remove(domainInfo);
        } else {
            hostCountMap.put(domainInfo, current - 1);
        }
        domainInfos = domainList.toArray(new DomainInfo[] {}); // enabled
        enabled.remove(element);
        offline.remove(element);
        online.remove(element);
        // size
        fileSize -= sizes.get(element);
        if (fileSize < 0) throw new WTFException("Filesize cannot be less than 0");
        // System.out.println(element + " rem: " + crawledPackage.getName()
        // + " : " + hostCountMap + " " + domainList);
    }

    public void clear() {
        this.fileSize = 0l;
        hostCountMap.clear();
        domainList.clear();
        offline.clear();
        online.clear();
        enabled.clear();
        sizes.clear();
    }

    public void updateInfo(CrawledLink crawledLink) {
        removeInfo(crawledLink);
        addInfo(crawledLink);
    }

    public DomainInfo[] getDomainInfos() {
        return domainInfos;
    }

    public boolean isEnabled() {
        return enabled.size() > 0;
    }

    public int getOfflineCount() {
        return offline.size();
    }

    public int getOnlineCount() {
        return online.size();
    }

    public long getFileSize() {
        return fileSize;
    }

    @Override
    public List<CrawledLink> getItems() {
        return items;
    }

}
