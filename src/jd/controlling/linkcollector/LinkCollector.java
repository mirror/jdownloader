package jd.controlling.linkcollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.controlling.IOEQ;
import jd.controlling.UniqueID;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerFilter;
import jd.controlling.linkcrawler.LinkCrawlerHandler;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.utils.event.Eventsender;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.filter.LinkFilterController;

public class LinkCollector extends PackageController<CrawledPackage, CrawledLink> implements LinkCheckerHandler<CrawledLink>, LinkCrawlerHandler {

    private transient Eventsender<LinkCollectorListener, LinkCollectorEvent> broadcaster = new Eventsender<LinkCollectorListener, LinkCollectorEvent>() {

                                                                                             @Override
                                                                                             protected void fireEvent(final LinkCollectorListener listener, final LinkCollectorEvent event) {
                                                                                                 listener.onLinkCollectorEvent(event);
                                                                                             };
                                                                                         };

    private static LinkCollector                                             INSTANCE    = new LinkCollector();
    private LinkChecker<CrawledLink>                                         linkChecker = null;

    private class dupeCheck {
        int counter = 1;
    }

    /**
     * NOTE: only access these fields inside the IOEQ
     */
    private HashMap<String, dupeCheck>        dupeMap           = new HashMap<String, dupeCheck>();
    private HashMap<UniqueID, CrawledPackage> packageMap        = new HashMap<UniqueID, CrawledPackage>();
    private HashMap<CrawledPackage, UniqueID> packageMapReverse = new HashMap<CrawledPackage, UniqueID>();

    /* sync on filteredStuff itself when accessing it */
    private ArrayList<CrawledLink>            filteredStuff     = new ArrayList<CrawledLink>();

    private LinkCrawlerFilter                 crawlerFilter     = null;

    public static LinkCollector getInstance() {
        return INSTANCE;
    }

    private LinkCollector() {
    }

    private void lazyInit() {
        if (linkChecker != null) return;
        synchronized (this) {
            if (linkChecker != null) return;
            linkChecker = new LinkChecker<CrawledLink>();
            linkChecker.setLinkCheckHandler(this);
            setCrawlerFilter(LinkFilterController.getInstance());
        }
    }

    public void abort() {
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.ABORT));
        if (linkChecker != null) linkChecker.stopChecking();
    }

    public void addListener(final LinkCollectorListener l) {
        broadcaster.addListener(l);
    }

    public void removeListener(final LinkCollectorListener l) {
        broadcaster.removeListener(l);
    }

    @Override
    protected void _controllerParentlessLinks(List<CrawledLink> links, QueuePriority priority) {
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REMOVE_CONTENT, links, priority));
        for (CrawledLink link : links) {
            /* update dupeMap */
            dupeCheck dupes = dupeMap.get(link.getURL());
            if (dupes != null) {
                dupes.counter--;
                if (dupes.counter <= 0) {
                    dupeMap.remove(link.getURL());
                }
            }
        }
    }

    @Override
    protected void _controllerPackageNodeRemoved(CrawledPackage pkg, QueuePriority priority) {
        /* update packageMap */
        UniqueID id = packageMapReverse.remove(pkg);
        if (id != null) packageMap.remove(id);
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REMOVE_CONTENT, pkg, priority));
        synchronized (pkg) {
            for (CrawledLink link : pkg.getChildren()) {
                /* update dupeMap */
                dupeCheck dupes = dupeMap.get(link.getURL());
                if (dupes != null) {
                    dupes.counter--;
                    if (dupes.counter <= 0) {
                        dupeMap.remove(link.getURL());
                    }
                }
            }
        }
    }

    @Override
    protected void _controllerStructureChanged(QueuePriority priority) {
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_STRUCTURE, priority));
    }

    @Override
    protected void _controllerPackageNodeAdded(CrawledPackage pkg, QueuePriority priority) {
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_STRUCTURE, priority));
    }

    public void refreshData() {
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_DATA));
    }

    public LinkCrawler addCrawlerJob(final ArrayList<CrawledLink> links) {
        if (links == null || links.size() == 0) throw new IllegalArgumentException("no links");
        lazyInit();
        final LinkCollectorCrawler lc = new LinkCollectorCrawler();
        broadcaster.addListener(lc, true);
        lc.setFilter(crawlerFilter);
        lc.setHandler(this);
        ArrayList<CrawledLink> deep = new ArrayList<CrawledLink>();
        ArrayList<CrawledLink> normal = new ArrayList<CrawledLink>();
        for (CrawledLink link : links) {
            if (link.getSourceJob() == null || !link.getSourceJob().isDeepAnalyse()) {
                normal.add(link);
            } else {
                deep.add(link);
            }
        }
        lc.enqueueNormal(normal);
        normal = null;
        lc.enqueueDeep(deep);
        deep = null;
        return lc;
    }

    public LinkCrawler addCrawlerJob(final LinkCollectingJob job) {
        if (job == null) throw new IllegalArgumentException("job is null");
        lazyInit();
        final LinkCollectorCrawler lc = new LinkCollectorCrawler() {

            @Override
            protected CrawledLink crawledLinkFactorybyURL(String url) {
                CrawledLink ret = super.crawledLinkFactorybyURL(url);
                ret.setSourceJob(job);
                return ret;
            }

        };
        broadcaster.addListener(lc, true);
        lc.setFilter(crawlerFilter);
        lc.setHandler(this);
        if (job.isDeepAnalyse()) {
            lc.enqueueDeep(job.getText(), null);
        } else {
            lc.enqueueNormal(job.getText(), null);
        }
        /*
         * we don't want to keep reference on text during the whole link
         * grabbing/checking/collecting way
         */
        job.setText(null);
        return lc;
    }

    public void linkCheckDone(CrawledLink link) {
        if (crawlerFilter.dropByFileProperties(link)) {
            addFilteredStuff(link);
        } else {
            addCrawledLink(link);
        }
    }

    /**
     * NOTE: use only inside the IOEQ
     */
    private boolean dupeCheck(CrawledLink link) {
        if (!link.isDupeAllow() && dupeMap.containsKey(link.getURL())) {
            return false;
        } else {
            dupeCheck dupes = dupeMap.get(link.getURL());
            if (dupes == null) {
                dupes = new dupeCheck();
                /* counter is already 1 here */
                dupeMap.put(link.getURL(), dupes);
            } else {
                /* increase counter */
                dupes.counter++;
            }
        }
        return true;
    }

    private void addCrawledLink(final CrawledLink link) {
        final UniqueID wanted = link.getDownloadLink().getFilePackage().getUniqueID();
        if (wanted != null) {
            /* custom package was set, try to find it or create new one */
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {
                @Override
                protected Void run() throws RuntimeException {
                    /* update dupeCheck map */
                    if (dupeCheck(link) == false) return null;
                    CrawledPackage match = packageMap.get(wanted);
                    if (match == null) {
                        match = new CrawledPackage();
                        match.setAllowAutoPackage(false);
                        packageMap.put(wanted, match);
                        packageMapReverse.put(match, wanted);
                        /*
                         * forward name from FilePackage Instance to
                         * CrawledPackageInfo
                         */
                        match.setName(link.getDownloadLink().getFilePackage().getName());
                        match.setCreated(link.getCreated());
                    }
                    List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                    add.add(link);
                    LinkCollector.this.addmoveChildren(match, add, -1);
                    return null;
                }
            });
        } else {
            /* try to find good matching package or create new one */
            final String packageName = LinknameCleaner.cleanFileName(link.getName());
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    /* update dupeCheck map */
                    if (dupeCheck(link) == false) return null;
                    String name = packageName;
                    int bestMatch = 0;
                    CrawledPackage bestPackage = null;
                    boolean readL = readLock();
                    try {
                        for (CrawledPackage pkg : packages) {
                            if (pkg.isAllowAutoPackage() == false) continue;
                            int sim = LinknameCleaner.comparepackages(pkg.getAutoPackageName(), name);
                            if (sim > bestMatch) {
                                bestMatch = sim;
                                bestPackage = pkg;
                            }
                        }
                    } finally {
                        readUnlock(readL);
                    }
                    if (bestMatch < 99 || bestPackage == null) {
                        /* create new Package */
                        bestPackage = new CrawledPackage();
                        bestPackage.setCreated(link.getCreated());
                    } else {
                        /* rename existing one */
                        name = getSimString(bestPackage.getAutoPackageName(), name);
                    }
                    bestPackage.setAutoPackageName(name);
                    /* add link to LinkCollector */
                    List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                    add.add(link);
                    LinkCollector.this.addmoveChildren(bestPackage, add, -1);
                    return null;
                }
            });
        }
    }

    private String getSimString(String a, String b) {
        String aa = a.toLowerCase();
        String bb = b.toLowerCase();
        int maxL = Math.min(aa.length(), bb.length());
        StringBuilder ret = new StringBuilder(maxL);
        for (int i = 0; i < maxL; i++) {
            if (aa.charAt(i) == bb.charAt(i)) {
                ret.append(a.charAt(i));
            }
        }
        return ret.toString();
    }

    public void merge(final CrawledPackage dest, final ArrayList<CrawledLink> srcLinks, final ArrayList<CrawledPackage> srcPkgs) {
        if (dest == null) return;
        if (srcLinks == null && srcPkgs == null) return;
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                int positionMerge = LinkCollectorConfig.DOMERGETOP.getValue() ? 0 : -1;
                if (srcLinks != null) {
                    /* move srcLinks to dest */
                    addmoveChildren(dest, srcLinks, positionMerge);
                    if (positionMerge != -1) {
                        /* update positionMerge in case we want merge@top */
                        positionMerge += srcLinks.size();
                    }
                }
                if (srcPkgs != null) {
                    for (CrawledPackage pkg : srcPkgs) {
                        /* move links from srcPkgs to dest */
                        int size = pkg.getChildren().size();
                        addmoveChildren(dest, pkg.getChildren(), positionMerge);
                        if (positionMerge != -1) {
                            /* update positionMerge in case we want merge@top */
                            positionMerge += size;
                        }
                    }
                }
                return null;
            }
        });
    }

    public void move(final ArrayList<CrawledPackage> srcPkgs, final CrawledPackage afterDest) {
        if (srcPkgs == null || srcPkgs.size() == 0) return;
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                for (CrawledPackage srcPkg : srcPkgs) {
                    int destination = 0;
                    if (afterDest != null) {
                        int destI = 0;
                        boolean readL = readLock();
                        try {
                            destI = packages.indexOf(afterDest);
                        } finally {
                            readUnlock(readL);
                        }
                        destination = Math.max(destI, 0) + 1;
                    }
                    addmovePackageAt(srcPkg, destination);
                }
                return null;
            }
        });
    }

    public void move(final ArrayList<CrawledLink> srcLinks, final CrawledPackage dstPkg, final CrawledLink afterLink) {
        if (dstPkg == null || srcLinks == null || srcLinks.size() == 0) return;
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                int destination = 0;
                if (afterLink != null) {
                    int destI = 0;
                    synchronized (dstPkg) {
                        destI = dstPkg.getChildren().indexOf(afterLink);
                    }
                    destination = Math.max(destI, 0) + 1;
                }
                addmoveChildren(dstPkg, srcLinks, destination);
                return null;
            }
        });
    }

    /**
     * @param crawlerFilter
     *            the crawlerFilter to set
     */
    public void setCrawlerFilter(LinkCrawlerFilter crawlerFilter) {
        if (crawlerFilter == null) throw new IllegalArgumentException("crawlerFilter is null");
        this.crawlerFilter = crawlerFilter;
    }

    /**
     * @return the crawlerFilter
     */
    public LinkCrawlerFilter getCrawlerFilter() {
        return crawlerFilter;
    }

    private void addFilteredStuff(CrawledLink filtered) {
        synchronized (filteredStuff) {
            filteredStuff.add(filtered);
        }
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.FILTERED_AVAILABLE));
    }

    @Override
    public void clear() {
        super.clear();
        synchronized (filteredStuff) {
            filteredStuff.clear();
        }
        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.FILTERED_EMPTY));
    }

    public ArrayList<CrawledLink> getFilteredStuff(boolean clearAfterGet) {
        ArrayList<CrawledLink> ret = null;
        synchronized (filteredStuff) {
            ret = new ArrayList<CrawledLink>(filteredStuff);
            if (clearAfterGet) filteredStuff.clear();
        }
        if (clearAfterGet) broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.FILTERED_EMPTY));
        return ret;
    }

    public int getfilteredStuffSize() {
        return filteredStuff.size();
    }

    public void handleFinalLink(CrawledLink link) {
        if (LinkCollectorConfig.DOLINKCHECK.getValue()) {
            linkChecker.check(link);
        } else {
            addCrawledLink(link);
        }
    }

    public void handleFilteredLink(CrawledLink link) {
        addFilteredStuff(link);
    }

    public void confirmCrawledPackage(CrawledPackage pkg) {
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                return null;
            }

        });
    }
}
