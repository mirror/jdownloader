package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
    protected CrawledPackage               crawledPackage;
    protected HashSet<CrawledLink>         enabled;
    protected HashMap<CrawledLink, Long>   sizes;
    private HashSet<CrawledLink>           offline;

    public CrawledPackageView() {
        this.fileSize = 0l;
        hostCountMap = new HashMap<DomainInfo, Integer>();
        domainList = new TreeSet<DomainInfo>();
        enabled = new HashSet<CrawledLink>();
        offline = new HashSet<CrawledLink>();
        sizes = new HashMap<CrawledLink, Long>();
    }

    @Override
    public void replace(ArrayList<CrawledLink> items) {
        CrawledPackageView newl = new CrawledPackageView();
        newl.addAll(items);
        this.fileSize = newl.fileSize;
        this.hostCountMap = newl.hostCountMap;
        this.domainList = newl.domainList;
        this.domainInfos = newl.domainInfos;
        this.enabled = newl.enabled;
        this.sizes = newl.sizes;
        this.offline = newl.offline;
    }

    @Override
    public CrawledLink set(int index, CrawledLink element) {
        CrawledLink old = get(index);
        try {
            return super.set(index, element);
        } finally {
            if (old != null) {
                removeInfo(old);
            }
            addInfo(element);
        }
    }

    @Override
    public boolean add(CrawledLink e) {
        try {

            return super.add(e);
        } finally {

            addInfo(e);
        }
    }

    @Override
    public void add(int index, CrawledLink element) {

        try {

            super.add(index, element);
        } finally {

            addInfo(element);
        }
    }

    private void addInfo(CrawledLink element) {
        // domain
        DomainInfo domainInfo = element.getDomainInfo();
        Integer current = hostCountMap.get(domainInfo);
        if (current == null) current = 0;
        hostCountMap.put(domainInfo, current + 1);
        domainList.add(domainInfo);
        domainInfos = domainList.toArray(new DomainInfo[] {});
        // enabled
        if (element.isEnabled()) enabled.add(element);
        // online
        if (element.getLinkState() == LinkState.OFFLINE) offline.add(element);
        // size
        sizes.put(element, element.getSize());
        fileSize += element.getSize();
        // System.out.println(element + " add: " + crawledPackage.getName()
        // + " : " + hostCountMap + " " + domainList);
    }

    private void removeInfo(CrawledLink element) {
        // domain
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
        // size
        fileSize -= sizes.get(element);
        if (fileSize < 0) throw new WTFException("Filesize cannot be less than 0");
        // System.out.println(element + " rem: " + crawledPackage.getName()
        // + " : " + hostCountMap + " " + domainList);
    }

    @Override
    public CrawledLink remove(int index) {
        CrawledLink ret = super.remove(index);
        removeInfo(ret);
        return ret;
    }

    @Override
    public boolean remove(Object o) {
        try {
            return super.remove(o);
        } finally {
            if (o instanceof CrawledLink) {
                removeInfo((CrawledLink) o);
            }
        }
    }

    @Override
    public void clear() {
        try {

            super.clear();
        } finally {
            this.fileSize = 0l;
            hostCountMap.clear();
            domainList.clear();
            offline.clear();
            enabled.clear();
            sizes.clear();

        }
    }

    @Override
    public boolean addAll(Collection<? extends CrawledLink> c) {
        try {
            return super.addAll(c);
        } finally {
            for (CrawledLink cc : c) {
                addInfo(cc);
            }
        }
    }

    public void update() {

    }

    @Override
    public boolean addAll(int index, Collection<? extends CrawledLink> c) {
        try {
            return super.addAll(index, c);
        } finally {
            for (CrawledLink cc : c) {
                addInfo(cc);
            }
        }
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        ArrayList<CrawledLink> old = new ArrayList<CrawledLink>();
        for (int i = fromIndex; i < toIndex; i++) {
            old.add(get(i));
        }
        super.removeRange(fromIndex, toIndex);

        for (CrawledLink c : old) {
            if (c != null) removeInfo(c);
        }
    }

    @Override
    public boolean removeAll(Collection<?> old) {
        try {
            return super.removeAll(old);

        } finally {
            for (Object c : old) {
                if (c != null && c instanceof CrawledLink) removeInfo((CrawledLink) c);
            }
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new WTFException("Not supported");
        // return super.retainAll(c);
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

    public long getFileSize() {
        return fileSize;
    }

}
