package jd.controlling.linkcollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import jd.controlling.IOEQ;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerFilter;
import jd.controlling.linkcrawler.LinkCrawlerHandler;
import jd.controlling.linkcrawler.PackageInfo;
import jd.controlling.packagecontroller.PackageController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.StringUtils;
import org.appwork.utils.event.Eventsender;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.packagizer.PackagizerController;

public class LinkCollector extends PackageController<CrawledPackage, CrawledLink> implements LinkCheckerHandler<CrawledLink>, LinkCrawlerHandler {

    private transient Eventsender<LinkCollectorListener, LinkCollectorEvent> broadcaster   = new Eventsender<LinkCollectorListener, LinkCollectorEvent>() {

                                                                                               @Override
                                                                                               protected void fireEvent(final LinkCollectorListener listener, final LinkCollectorEvent event) {
                                                                                                   listener.onLinkCollectorEvent(event);
                                                                                               };
                                                                                           };

    private static LinkCollector                                             INSTANCE      = new LinkCollector();
    private LinkChecker<CrawledLink>                                         linkChecker   = null;

    /**
     * NOTE: only access these fields inside the IOEQ
     */
    private HashSet<String>                                                  dupeCheckMap  = new HashSet<String>();
    private HashMap<String, CrawledPackage>                                  packageMap    = new HashMap<String, CrawledPackage>();

    /* sync on filteredStuff itself when accessing it */
    private ArrayList<CrawledLink>                                           filteredStuff = new ArrayList<CrawledLink>();

    private LinkCrawlerFilter                                                crawlerFilter = null;

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
            setPackagizer(PackagizerController.getInstance());
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
            dupeCheckMap.remove(link.getLinkID());
        }
    }

    @Override
    protected void _controllerPackageNodeRemoved(CrawledPackage pkg, QueuePriority priority) {
        /* update packageMap */
        for (Iterator<Entry<String, CrawledPackage>> iterator = packageMap.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, CrawledPackage> type = iterator.next();
            if (type.getValue() == pkg) {
                packageMap.remove(type.getKey());
                break;
            }
        }

        broadcaster.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REMOVE_CONTENT, pkg, priority));
        synchronized (pkg) {
            for (CrawledLink link : pkg.getChildren()) {
                dupeCheckMap.remove(link.getLinkID());
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
                if (job.getOutputFolder() != null && (ret.getDesiredPackageInfo() == null || ret.getDesiredPackageInfo().getDestinationFolder() == null)) {
                    if (ret.getDesiredPackageInfo() == null) ret.setDesiredPackageInfo(new PackageInfo());
                    ret.getDesiredPackageInfo().setDestinationFolder(job.getOutputFolder().getAbsolutePath());
                }
                if (!StringUtils.isEmpty(job.getPackageName()) && (ret.getDesiredPackageInfo() == null || StringUtils.isEmpty(ret.getDesiredPackageInfo().getName()))) {
                    if (ret.getDesiredPackageInfo() == null) ret.setDesiredPackageInfo(new PackageInfo());
                    ret.getDesiredPackageInfo().setName(job.getPackageName());
                }

                if (!StringUtils.isEmpty(job.getExtractPassword())) {
                    if (ret.getDesiredPackageInfo() == null) ret.setDesiredPackageInfo(new PackageInfo());
                    ret.getDesiredPackageInfo().getExtractionPasswords().add(job.getExtractPassword());
                }
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
            PackagizerInterface pc;
            if ((pc = getPackagizer()) != null) {
                /* run packagizer on checked link */
                pc.runByFile(link);
            }
            addCrawledLink(link);
        }
    }

    private PackagizerInterface packagizer = null;

    public PackagizerInterface getPackagizer() {
        return packagizer;
    }

    public void setPackagizer(PackagizerInterface packagizerInterface) {
        this.packagizer = packagizerInterface;
    }

    public FilePackage removeAndConvert(final CrawledPackage pkg) {
        if (pkg == null) return null;
        return IOEQ.getQueue().addWait(new QueueAction<FilePackage, RuntimeException>() {

            @Override
            protected FilePackage run() throws RuntimeException {
                if (LinkCollector.this != pkg.getControlledBy()) {
                    Log.exception(new Throwable("not controlled by this packagecontroller"));
                    return null;
                }
                /* remove CrawledPackage from controller first */
                removePackage(pkg);
                /* get new FilePackage Instance */
                return createFilePackage(pkg, null);
            }
        });
    }

    /*
     * converts a CrawledPackage into a FilePackage
     * 
     * if plinks is not set, then the original children of the CrawledPackage
     * will get added to the FilePackage
     * 
     * if plinks is set, then only plinks will get added to the FilePackage
     */
    private FilePackage createFilePackage(final CrawledPackage pkg, ArrayList<CrawledLink> plinks) {
        FilePackage ret = FilePackage.getInstance();
        /* set values */
        ret.setName(pkg.getName());
        ret.setDownloadDirectory(pkg.getDownloadFolder());
        ret.setCreated(pkg.getCreated());
        ret.setExpanded(pkg.isExpanded());
        ret.setComment(pkg.getComment());
        synchronized (pkg) {
            /* add Children from CrawledPackage to FilePackage */
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>(pkg.getChildren().size());
            List<CrawledLink> pkgLinks = pkg.getChildren();
            if (plinks != null && plinks.size() > 0) pkgLinks = new ArrayList<CrawledLink>(plinks);
            for (CrawledLink link : pkgLinks) {
                /* extract DownloadLink from CrawledLink */
                DownloadLink dl = link.getDownloadLink();
                if (dl != null) {
                    /*
                     * change filename if it is different than original
                     * downloadlink
                     */
                    if (link.isNameSet()) dl.forceFileName(link.getName());
                    links.add(dl);
                    /* set correct Parent node */
                    dl.setParentNode(ret);
                }
            }
            /* add all children to FilePackage */
            ret.getChildren().addAll(links);
        }
        return ret;
    }

    public ArrayList<FilePackage> removeAndConvert(final ArrayList<CrawledLink> links) {
        if (links == null || links.size() == 0) return null;
        return IOEQ.getQueue().addWait(new QueueAction<ArrayList<FilePackage>, RuntimeException>() {

            @Override
            protected ArrayList<FilePackage> run() throws RuntimeException {
                ArrayList<FilePackage> ret = new ArrayList<FilePackage>();
                HashMap<CrawledPackage, ArrayList<CrawledLink>> map = new HashMap<CrawledPackage, ArrayList<CrawledLink>>();
                for (CrawledLink link : links) {
                    CrawledPackage parent = link.getParentNode();
                    if (parent == null || parent.getControlledBy() != LinkCollector.this) {
                        Log.exception(new Throwable("not controlled by this packagecontroller"));
                        continue;
                    }
                    ArrayList<CrawledLink> pkg_links = map.get(parent);
                    if (pkg_links == null) {
                        pkg_links = new ArrayList<CrawledLink>();
                        map.put(parent, pkg_links);
                    }
                    pkg_links.add(link);
                }
                Iterator<Entry<CrawledPackage, ArrayList<CrawledLink>>> it = map.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<CrawledPackage, ArrayList<CrawledLink>> next = it.next();
                    LinkCollector.this.removeChildren(next.getKey(), next.getValue(), true);
                    ret.add(createFilePackage(next.getKey(), next.getValue()));
                }
                return ret;
            }

        });
    }

    private void addCrawledLink(final CrawledLink link) {

        /* try to find good matching package or create new one */
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            public boolean handleException(Throwable e) {
                /* remove dupeCheck map item in case something went wrong */
                dupeCheckMap.remove(link.getLinkID());
                return super.handleException(e);
            }

            @Override
            protected Void run() throws RuntimeException {
                /* update dupeCheck map */
                if (!dupeCheckMap.add(link.getLinkID())) return null;
                PackageInfo dpi = link.getDesiredPackageInfo();
                String packageID = null;
                CrawledPackage pkgMatch = null;
                if (dpi != null) {
                    packageID = dpi.createPackageID();
                }
                if (packageID != null) {
                    /*
                     * packageID available, lets reuse an existing package with
                     * same id or create new one
                     */
                    pkgMatch = packageMap.get(packageID);

                    // if(packageID!=null&&pkgMatch.getCustomName()==null){
                    // pkgMatch.getChildren()
                    //
                    // LinknameCleaner.comparepackages(pkg.getAutoPackageName(),
                    // packageName);

                    if (pkgMatch == null) {
                        pkgMatch = PackageInfo.createCrawledPackage(link);
                        if (pkgMatch != null) {
                            /*
                             * we dont want autopackager(by filename) to work
                             * with this package
                             */
                            pkgMatch.setAllowAutoPackage(false);
                            packageMap.put(packageID, pkgMatch);
                        }
                    }
                }
                if (pkgMatch == null) {
                    /* no packageID available, lets work on filename only */
                    String packageName = LinknameCleaner.cleanFileName(link.getName());
                    int bestMatch = 0;
                    boolean readL = readLock();
                    try {
                        for (CrawledPackage pkg : packages) {
                            if (pkg.isAllowAutoPackage() == false) continue;
                            if (dpi != null && dpi.getDestinationFolder() != null) {
                                boolean eq = pkg.isDownloadFolderSet() && (CrossSystem.isWindows() ? dpi.getDestinationFolder().equalsIgnoreCase(pkg.getRawDownloadFolder()) : dpi.getDestinationFolder().equals(pkg.getRawDownloadFolder()));
                                if (!eq) continue;

                            } else if (pkg.isDownloadFolderSet()) {
                                continue;
                            }

                            int sim = LinknameCleaner.comparepackages(pkg.getAutoPackageName(), packageName);
                            if (sim > bestMatch) {
                                bestMatch = sim;
                                pkgMatch = pkg;
                            }
                        }
                    } finally {
                        readUnlock(readL);
                    }
                    if (bestMatch < 99 || pkgMatch == null) {
                        /* create new Package */
                        pkgMatch = new CrawledPackage();
                        pkgMatch.setCreated(link.getCreated());
                        pkgMatch.setAllowAutoPackage(true);
                        if (dpi != null && dpi.getDestinationFolder() != null) {
                            pkgMatch.setDownloadFolder(dpi.getDestinationFolder());

                        }

                    } else {
                        /* rename existing one */
                        packageName = getSimString(pkgMatch.getAutoPackageName(), packageName);
                    }
                    pkgMatch.setAutoPackageName(packageName);
                }
                List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                add.add(link);

                /* add link to linkcollector */
                LinkCollector.this.addmoveChildren(pkgMatch, add, -1);

                return null;
            }
        });

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
            PackagizerInterface pc;
            if ((pc = getPackagizer()) != null) {
                /* run packagizer on un-checked link */
                pc.runByUrl(link);
            }
            addCrawledLink(link);
        }
    }

    public void handleFilteredLink(CrawledLink link) {
        addFilteredStuff(link);
    }

}
