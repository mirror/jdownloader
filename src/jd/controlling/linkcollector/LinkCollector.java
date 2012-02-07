package jd.controlling.linkcollector;

import java.io.File;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.zip.ZipEntry;

import jd.controlling.IOEQ;
import jd.controlling.JDLogger;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLink.LinkState;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerFilter;
import jd.controlling.linkcrawler.LinkCrawlerHandler;
import jd.controlling.linkcrawler.PackageInfo;
import jd.controlling.packagecontroller.PackageController;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging.Log;
import org.appwork.utils.zip.ZipIOReader;
import org.appwork.utils.zip.ZipIOWriter;
import org.jdownloader.controlling.UniqueSessionID;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.translate._JDT;

public class LinkCollector extends PackageController<CrawledPackage, CrawledLink> implements LinkCheckerHandler<CrawledLink>, LinkCrawlerHandler {

    public static LinkCollector getInstance() {
        return INSTANCE;
    }

    private transient LinkCollectorEventSender      eventsender   = new LinkCollectorEventSender();
    private static LinkCollector                    INSTANCE      = new LinkCollector();

    private LinkChecker<CrawledLink>                linkChecker   = null;
    /**
     * NOTE: only access these fields inside the IOEQ
     */
    private HashSet<String>                         dupeCheckMap  = new HashSet<String>();
    private HashMap<String, CrawledPackage>         packageMap    = new HashMap<String, CrawledPackage>();

    /* sync on filteredStuff itself when accessing it */
    private ArrayList<CrawledLink>                  filteredStuff = new ArrayList<CrawledLink>();

    private LinkCrawlerFilter                       crawlerFilter = null;

    private ExtractionExtension                     archiver;
    private DelayedRunnable                         asyncSaving   = null;

    private boolean                                 allowSave     = false;

    private boolean                                 allowLoad     = true;

    private PackagizerInterface                     packagizer    = null;

    protected OfflineCrawledPackage                 offlinePackage;

    protected VariousCrawledPackage                 variousPackage;

    protected PermanentOfflinePackage               permanentofflinePackage;

    private HashMap<String, ArrayList<CrawledLink>> offlineMap    = new HashMap<String, ArrayList<CrawledLink>>();

    private HashMap<String, ArrayList<CrawledLink>> variousMap    = new HashMap<String, ArrayList<CrawledLink>>();

    private LinkCollector() {
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                saveLinkCollectorLinks();
            }

            @Override
            public String toString() {
                return "save linkcollector...";
            }
        });
        asyncSaving = new DelayedRunnable(IOEQ.TIMINGQUEUE, 5000l, 60000l) {

            @Override
            public void delayedrun() {
                saveLinkCollectorLinks();
            }

        };
        this.eventsender.addListener(new LinkCollectorListener() {

            public void onLinkCollectorAbort(LinkCollectorEvent event) {
                asyncSaving.run();
            }

            public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
                asyncSaving.run();
            }

            public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
                asyncSaving.run();
            }

            public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
                asyncSaving.run();
            }

            public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
                asyncSaving.run();
            }

            public void onLinkCollectorLinksRemoved(LinkCollectorEvent event) {
                asyncSaving.run();
            }
        });
    }

    public LinkCollectorEventSender getEventsender() {
        return eventsender;
    }

    @Override
    protected void _controllerPackageNodeAdded(CrawledPackage pkg, QueuePriority priority) {
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_STRUCTURE, priority));
    }

    @Override
    protected void _controllerPackageNodeRemoved(CrawledPackage pkg, QueuePriority priority) {
        /* update packageMap */
        String id = getPackageMapID(pkg);
        if (id != null) packageMap.remove(id);
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REMOVE_CONTENT, pkg, priority));
        ArrayList<CrawledLink> children = null;
        synchronized (pkg) {
            children = new ArrayList<CrawledLink>(pkg.getChildren());
        }
        cleanupMaps(children);
    }

    /**
     * NOTE: only access the IOEQ
     */
    private String getPackageMapID(CrawledPackage pkg) {
        for (Iterator<Entry<String, CrawledPackage>> iterator = packageMap.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, CrawledPackage> type = iterator.next();
            if (type.getValue() == pkg) { return type.getKey(); }
        }
        return null;
    }

    @Override
    protected void _controllerParentlessLinks(List<CrawledLink> links, QueuePriority priority) {
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REMOVE_CONTENT, links, priority));
        cleanupMaps(links);
    }

    @Override
    protected void _controllerStructureChanged(QueuePriority priority) {
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_STRUCTURE, priority));
    }

    public void abort() {
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.ABORT));
        if (linkChecker != null) linkChecker.stopChecking();
    }

    private void addCrawledLink(final CrawledLink link) {
        /* try to find good matching package or create new one */
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

            public void newPackage(final ArrayList<CrawledLink> links, String packageName, String downloadFolder, String identifier) {
                CrawledPackage pkg = new CrawledPackage();
                pkg.setCreated(System.currentTimeMillis());
                pkg.setName(packageName);
                if (downloadFolder != null) {
                    pkg.setDownloadFolder(downloadFolder);
                }
                packageMap.put(identifier, pkg);
                if (links != null && links.size() > 0) {
                    LinkCollector.this.addmoveChildren(pkg, links, -1);
                }
                // check of we have matching links in offline maper
                ArrayList<CrawledLink> list = offlineMap.remove(identifier);
                if (list != null && list.size() > 0) {
                    LinkCollector.this.addmoveChildren(pkg, list, -1);
                }
                list = variousMap.remove(identifier);
                if (list != null && list.size() > 0) {
                    LinkCollector.this.addmoveChildren(pkg, list, -1);
                }
            }

            @Override
            protected Void run() throws RuntimeException {
                try {
                    /* update dupeCheck map */
                    if (!dupeCheckMap.add(link.getLinkID())) {
                        //
                        return null;
                    }
                    PackageInfo dpi = link.getDesiredPackageInfo();
                    UniqueSessionID uID = null;

                    String packageName = null;
                    String packageID = null;
                    String downloadFolder = null;
                    boolean isMultiArchive = false;
                    if (dpi != null) {
                        packageName = dpi.getName();
                        if ((uID = dpi.getUniqueId()) != null) {
                            packageID = dpi.getUniqueId().toString();
                        }
                        downloadFolder = dpi.getDestinationFolder();
                    }
                    CrawledLinkFactory clf = new CrawledLinkFactory(link);
                    if (archiver != null && LinkgrabberSettings.ARCHIVE_PACKAGIZER_ENABLED.getValue()) {
                        if (archiver.isMultiPartArchive(clf)) {
                            isMultiArchive = true;
                        }
                    }
                    if (packageName == null) {
                        packageName = LinknameCleaner.cleanFileName(link.getName());
                        if (isMultiArchive) {
                            packageID = archiver.createArchiveID(clf);
                            if (packageID != null) {
                                packageName = _JDT._.LinkCollector_archiv(LinknameCleaner.cleanFileName(archiver.getArchiveName(clf)));
                            }
                        }
                    }

                    String identifier = packageID + "_" + packageName + "_" + downloadFolder;
                    CrawledPackage pkg = packageMap.get(identifier);
                    if (pkg == null) {
                        if (LinkCrawler.PERMANENT_OFFLINE_ID == uID) {
                            /* these links will never come back online */
                            getPermanentOfflineCrawledPackage();
                            List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                            add.add(link);
                            LinkCollector.this.addmoveChildren(getPermanentOfflineCrawledPackage(), add, -1);
                        } else if (link.getLinkState() == LinkState.OFFLINE && LinkgrabberSettings.OFFLINE_PACKAGE_ENABLED.getValue()) {
                            getOfflineCrawledPackage();
                            ArrayList<CrawledLink> list = getIdentifiedMap(identifier, offlineMap);
                            list.add(link);
                            List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                            add.add(link);
                            LinkCollector.this.addmoveChildren(getOfflineCrawledPackage(), add, -1);
                        } else if (LinkgrabberSettings.VARIOUS_PACKAGE_LIMIT.getValue() > 0) {
                            getVariousCrawledPackage();
                            ArrayList<CrawledLink> list = getIdentifiedMap(identifier, variousMap);
                            list.add(link);
                            if (list.size() > LinkgrabberSettings.VARIOUS_PACKAGE_LIMIT.getValue()) {
                                newPackage(null, packageName, downloadFolder, identifier);
                            } else {
                                List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                                add.add(link);
                                LinkCollector.this.addmoveChildren(getVariousCrawledPackage(), add, -1);
                            }
                        } else {
                            ArrayList<CrawledLink> add = new ArrayList<CrawledLink>(1);
                            add.add(link);
                            newPackage(add, packageName, downloadFolder, identifier);
                        }
                    } else {
                        List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                        add.add(link);
                        LinkCollector.this.addmoveChildren(pkg, add, -1);
                    }

                    return null;
                } catch (RuntimeException e) {
                    dupeCheckMap.remove(link.getLinkID());
                    throw e;
                }
            }
        });
    }

    private ArrayList<CrawledLink> getIdentifiedMap(String identifier, HashMap<String, ArrayList<CrawledLink>> map) {
        ArrayList<CrawledLink> list = map.get(identifier);
        if (list == null) {
            list = new ArrayList<CrawledLink>();
            map.put(identifier, list);
        }
        return list;
    }

    private PermanentOfflinePackage getPermanentOfflineCrawledPackage() {
        if (permanentofflinePackage != null) return permanentofflinePackage;
        permanentofflinePackage = new PermanentOfflinePackage();
        permanentofflinePackage.setExpanded(true);
        permanentofflinePackage.setName(_GUI._.Permanently_Offline_Package());
        return permanentofflinePackage;
    }

    private VariousCrawledPackage getVariousCrawledPackage() {
        if (variousPackage != null) return variousPackage;
        variousPackage = new VariousCrawledPackage();
        variousPackage.setExpanded(true);
        variousPackage.setName(_JDT._.LinkCollector_addCrawledLink_variouspackage());
        return variousPackage;
    }

    private OfflineCrawledPackage getOfflineCrawledPackage() {
        if (offlinePackage != null) return offlinePackage;
        offlinePackage = new OfflineCrawledPackage();
        offlinePackage.setExpanded(true);
        offlinePackage.setName(_JDT._.LinkCollector_addCrawledLink_offlinepackage());
        return offlinePackage;
    }

    public LinkCrawler addCrawlerJob(final ArrayList<CrawledLink> links) {
        if (links == null || links.size() == 0) throw new IllegalArgumentException("no links");
        lazyInit();
        final LinkCollectorCrawler lc = new LinkCollectorCrawler() {
            @Override
            protected void generalCrawledLinkModifier(CrawledLink link) {
                LinkCollectingJob job = link.getSourceJob();
                if (job != null) {
                    if (link.getDownloadLink() != null) {
                        if (job.getCustomSourceUrl() != null) link.getDownloadLink().setBrowserUrl(job.getCustomSourceUrl());
                        if (job.getCustomComment() != null) link.getDownloadLink().setComment(job.getCustomComment());
                    }
                    if (job.getOutputFolder() != null && (link.getDesiredPackageInfo() == null || link.getDesiredPackageInfo().getDestinationFolder() == null)) {
                        if (link.getDesiredPackageInfo() == null) link.setDesiredPackageInfo(new PackageInfo());
                        link.getDesiredPackageInfo().setDestinationFolder(job.getOutputFolder().getAbsolutePath());
                    }
                    if (!StringUtils.isEmpty(job.getPackageName()) && (link.getDesiredPackageInfo() == null || StringUtils.isEmpty(link.getDesiredPackageInfo().getName()))) {
                        if (link.getDesiredPackageInfo() == null) link.setDesiredPackageInfo(new PackageInfo());
                        link.getDesiredPackageInfo().setName(job.getPackageName());
                    }

                    if (!StringUtils.isEmpty(job.getExtractPassword())) {
                        if (link.getDesiredPackageInfo() == null) link.setDesiredPackageInfo(new PackageInfo());
                        link.getDesiredPackageInfo().getExtractionPasswords().add(job.getExtractPassword());
                    }
                }
            }
        };
        eventsender.addListener(lc, true);
        lc.setFilter(crawlerFilter);
        lc.setHandler(this);
        lc.crawl(new ArrayList<CrawledLink>(links));
        return lc;
    }

    public LinkCrawler addCrawlerJob(final LinkCollectingJob job) {
        if (job == null) throw new IllegalArgumentException("job is null");
        lazyInit();
        final LinkCollectorCrawler lc = new LinkCollectorCrawler() {

            @Override
            protected void generalCrawledLinkModifier(CrawledLink link) {
                if (link.getDownloadLink() != null) {
                    if (job.getCustomSourceUrl() != null) link.getDownloadLink().setBrowserUrl(job.getCustomSourceUrl());
                    if (job.getCustomComment() != null) link.getDownloadLink().setComment(job.getCustomComment());
                }
                if (job.getOutputFolder() != null && (link.getDesiredPackageInfo() == null || link.getDesiredPackageInfo().getDestinationFolder() == null)) {
                    if (link.getDesiredPackageInfo() == null) link.setDesiredPackageInfo(new PackageInfo());
                    link.getDesiredPackageInfo().setDestinationFolder(job.getOutputFolder().getAbsolutePath());
                }
                if (!StringUtils.isEmpty(job.getPackageName()) && (link.getDesiredPackageInfo() == null || StringUtils.isEmpty(link.getDesiredPackageInfo().getName()))) {
                    if (link.getDesiredPackageInfo() == null) link.setDesiredPackageInfo(new PackageInfo());
                    link.getDesiredPackageInfo().setName(job.getPackageName());
                }

                if (!StringUtils.isEmpty(job.getExtractPassword())) {
                    if (link.getDesiredPackageInfo() == null) link.setDesiredPackageInfo(new PackageInfo());
                    link.getDesiredPackageInfo().getExtractionPasswords().add(job.getExtractPassword());
                }
            }

        };
        eventsender.addListener(lc, true);
        lc.setFilter(crawlerFilter);
        lc.setHandler(this);
        String jobText = job.getText();
        /*
         * we don't want to keep reference on text during the whole link
         * grabbing/checking/collecting way
         */
        job.setText(null);
        lc.crawl(jobText, null, job.isDeepAnalyse());
        return lc;
    }

    private void addFilteredStuff(CrawledLink filtered) {
        synchronized (filteredStuff) {
            filteredStuff.add(filtered);
        }
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.FILTERED_AVAILABLE));
    }

    // clean up offline/various/dupeCheck maps
    private void cleanupMaps(List<CrawledLink> links) {
        if (links == null) return;
        for (CrawledLink l : links) {
            dupeCheckMap.remove(l.getLinkID());
            removeFromMap(variousMap, l);
            removeFromMap(offlineMap, l);
        }
    }

    @Override
    public void clear() {
        super.clear();
        synchronized (filteredStuff) {
            filteredStuff.clear();
        }
        dupeCheckMap.clear();
        offlinePackage = null;
        variousPackage = null;
        permanentofflinePackage = null;
        variousMap.clear();
        offlineMap.clear();
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.FILTERED_EMPTY));
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
                    /* set correct enabled/disabled state */
                    dl.setEnabled(link.isEnabled());
                    /* remove reference to crawledLink */
                    dl.setPropertyListener(null);
                    dl.setCreated(link.getCreated());
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

    private String getCheckFileName() {
        return "check.info";
    }

    /**
     * @return the crawlerFilter
     */
    public LinkCrawlerFilter getCrawlerFilter() {
        return crawlerFilter;
    }

    public ArrayList<CrawledLink> getFilteredStuff(boolean clearAfterGet) {
        ArrayList<CrawledLink> ret = null;
        synchronized (filteredStuff) {
            ret = new ArrayList<CrawledLink>(filteredStuff);
            if (clearAfterGet) filteredStuff.clear();
        }
        if (clearAfterGet) eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.FILTERED_EMPTY));
        return ret;
    }

    public int getfilteredStuffSize() {
        return filteredStuff.size();
    }

    public LinkChecker<CrawledLink> getLinkChecker() {
        lazyInit();
        return linkChecker;
    }

    public PackagizerInterface getPackagizer() {
        return packagizer;
    }

    public void handleFilteredLink(CrawledLink link) {
        addFilteredStuff(link);
    }

    public void handleFinalLink(CrawledLink link) {
        if (LinkCollectorConfig.DOLINKCHECK.isEnabled()) {
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

    public boolean isLoadAllowed() {
        return allowLoad;
    }

    public boolean isSaveAllowed() {
        return allowSave;
    }

    private void lazyInit() {
        if (linkChecker != null) return;
        synchronized (this) {
            if (linkChecker != null) return;
            linkChecker = new LinkChecker<CrawledLink>();
            linkChecker.setLinkCheckHandler(this);
            setCrawlerFilter(LinkFilterController.getInstance());
            setPackagizer(PackagizerController.getInstance());
            try {
                setArchiver((ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension());
            } catch (Throwable e) {
                Log.exception(Level.SEVERE, e);
            }
        }
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

    public void refreshData() {
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_DATA));
    }

    public ArrayList<FilePackage> removeAndConvert(final ArrayList<CrawledLink> links) {
        if (links == null || links.size() == 0) return null;
        return IOEQ.getQueue().addWait(new QueueAction<ArrayList<FilePackage>, RuntimeException>() {

            @Override
            protected ArrayList<FilePackage> run() throws RuntimeException {
                ArrayList<FilePackage> ret = new ArrayList<FilePackage>();
                HashMap<CrawledPackage, ArrayList<CrawledLink>> map = new HashMap<CrawledPackage, ArrayList<CrawledLink>>();
                cleanupMaps(links);
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

    // clean up offline and various map
    private void removeFromMap(HashMap<String, ArrayList<CrawledLink>> idListMap, CrawledLink l) {
        Iterator<Entry<String, ArrayList<CrawledLink>>> it = idListMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, ArrayList<CrawledLink>> elem = it.next();
            String identifier = elem.getKey();
            ArrayList<CrawledLink> mapElems = elem.getValue();
            if (mapElems != null && mapElems.remove(l)) {
                if (mapElems.size() == 0) idListMap.remove(identifier);
                break;
            }
        }
    }

    /**
     * load all CrawledPackages/CrawledLinks from Database
     */
    public synchronized void initLinkCollector() {
        if (isLoadAllowed() == false) {
            /* loading is not allowed */
            return;
        }
        LinkedList<CrawledPackage> lpackages = null;
        final HashMap<CrawledPackage, CrawledPackageStorable> restoreMap = new HashMap<CrawledPackage, CrawledPackageStorable>();
        try {
            /* load from new json zip */
            lpackages = load(getLinkCollectorListFile(), restoreMap);
        } catch (final Throwable e) {
            Log.exception(e);
        }
        try {
            /* try fallback to load tmp file */
            if (lpackages == null) {
                restoreMap.clear();
                lpackages = load(new File(getLinkCollectorListFile().getAbsolutePath() + ".tmp"), restoreMap);
            }
        } catch (final Throwable e) {
            Log.exception(e);
        }
        if (lpackages == null) {
            restoreMap.clear();
            lpackages = new LinkedList<CrawledPackage>();
        }
        postInit(lpackages);
        final LinkedList<CrawledPackage> lpackages2 = lpackages;
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.HIGH) {

            @Override
            protected Void run() throws RuntimeException {
                if (isLoadAllowed() == true) {
                    writeLock();
                    /* add loaded Packages to this controller */
                    try {
                        for (final CrawledPackage filePackage : lpackages2) {
                            for (CrawledLink link : filePackage.getChildren()) {
                                /* keep maps up2date */
                                dupeCheckMap.add(link.getLinkID());
                            }
                            filePackage.setControlledBy(LinkCollector.this);
                            CrawledPackageStorable storable = restoreMap.get(filePackage);
                            if (storable != null && CrawledPackageStorable.TYPE.NORMAL.equals(storable.getType()) && storable.getPackageID() != null) {
                                /* keep packageMap up2date */
                                packageMap.put(storable.getPackageID(), filePackage);
                            }
                        }
                        packages.addAll(0, lpackages2);
                    } finally {
                        /* loaded, we no longer allow loading */
                        setLoadAllowed(false);
                        /* now we allow saving */
                        setSaveAllowed(true);
                        writeUnlock();
                    }
                    eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_STRUCTURE));
                }
                return null;
            }
        });
        return;
    }

    private void postInit(LinkedList<CrawledPackage> fps) {
        if (fps == null || fps.size() == 0) return;
        final Iterator<CrawledPackage> iterator = fps.iterator();
        DownloadLink dlLink = null;
        CrawledLink localLink = null;
        PluginForHost pluginForHost = null;
        Iterator<CrawledLink> it;
        CrawledPackage fp;
        while (iterator.hasNext()) {
            fp = iterator.next();
            if (fp.getChildren().size() == 0) {
                /* remove empty packages */
                iterator.remove();
                continue;
            }
            it = fp.getChildren().iterator();
            while (it.hasNext()) {
                localLink = it.next();
                dlLink = localLink.getDownloadLink();
                if (dlLink == null) {
                    /* remove crawledLinks without DownloadLink */
                    it.remove();
                    continue;
                }
                /* assign defaultPlugin matching the hostname */
                try {
                    pluginForHost = null;
                    LazyHostPlugin hPlugin = HostPluginController.getInstance().get(dlLink.getHost());
                    if (hPlugin != null) {
                        pluginForHost = hPlugin.getPrototype();
                    }
                } catch (final Throwable e) {
                    JDLogger.exception(e);
                }
                if (pluginForHost == null) {
                    try {
                        for (LazyHostPlugin p : HostPluginController.getInstance().list()) {
                            if (p.getPrototype().rewriteHost(dlLink)) {
                                pluginForHost = p.getPrototype();
                                break;
                            }
                        }
                        if (pluginForHost != null) {
                            Log.L.info("Plugin " + pluginForHost.getHost() + " now handles " + localLink.getName());
                        }
                    } catch (final Throwable e) {
                        Log.exception(e);
                    }
                }
                if (pluginForHost != null) {
                    dlLink.setDefaultPlugin(pluginForHost);
                } else {
                    Log.L.severe("Could not find plugin " + localLink.getHost() + " for " + localLink.getName());
                }
            }
        }
    }

    private LinkedList<CrawledPackage> load(File file, HashMap<CrawledPackage, CrawledPackageStorable> restoreMap) {
        LinkedList<CrawledPackage> ret = null;
        if (file != null) {
            ZipIOReader zip = null;
            try {
                zip = new ZipIOReader(file);
                ZipEntry check = zip.getZipFile(getCheckFileName());
                String checkString = null;
                if (check != null) {
                    /* parse checkFile if it exists */
                    InputStream checkIS = null;
                    try {
                        checkIS = zip.getInputStream(check);
                        byte[] checkbyte = IO.readStream(1024, checkIS);
                        checkString = new String(checkbyte, "UTF-8");
                        checkbyte = null;
                    } finally {
                        try {
                            checkIS.close();
                        } catch (final Throwable e) {
                        }
                    }
                    check = null;
                }
                if (checkString != null) {
                    /* checkFile exists, lets verify */
                    MessageDigest md = MessageDigest.getInstance("SHA1");
                    byte[] buffer = new byte[1024];
                    int found = 0;
                    for (ZipEntry entry : zip.getZipFiles()) {
                        if (entry.getName().matches("^\\d+$")) {
                            found++;
                            DigestInputStream checkIS = null;
                            try {
                                checkIS = new DigestInputStream(zip.getInputStream(entry), md);
                                while (checkIS.read(buffer) >= 0) {
                                }
                            } finally {
                                try {
                                    checkIS.close();
                                } catch (final Throwable e) {
                                }
                            }
                        }
                    }
                    String hash = HexFormatter.byteArrayToHex(md.digest());
                    String time = new Regex(checkString, "(\\d+)").getMatch(0);
                    String numberCheck = new Regex(checkString, ".*?:(\\d+)").getMatch(0);
                    String hashCheck = new Regex(checkString, ".*?:.*?:(.+)").getMatch(0);
                    boolean numberOk = (numberCheck != null && Integer.parseInt(numberCheck) == found);
                    boolean hashOk = (hashCheck != null && hashCheck.equalsIgnoreCase(hash));
                    Log.L.info("LinkCollectorListVerify: TimeStamp(" + time + ")|numberOfPackages(" + found + "):" + numberOk + "|hash:" + hashOk);
                }
                /* lets restore the CrawledPackages from Json */
                HashMap<Integer, CrawledPackage> map = new HashMap<Integer, CrawledPackage>();
                for (ZipEntry entry : zip.getZipFiles()) {
                    if (entry.getName().matches("^\\d+$")) {
                        int packageIndex = Integer.parseInt(entry.getName());
                        InputStream is = null;
                        try {
                            is = zip.getInputStream(entry);
                            byte[] bytes = IO.readStream((int) entry.getSize(), is);
                            String json = new String(bytes, "UTF-8");
                            bytes = null;
                            CrawledPackageStorable storable = JSonStorage.restoreFromString(json, new TypeRef<CrawledPackageStorable>() {
                            }, null);
                            json = null;
                            if (storable != null) {
                                map.put(packageIndex, storable._getCrawledPackage());
                                if (restoreMap != null) restoreMap.put(storable._getCrawledPackage(), storable);
                            }
                        } finally {
                            try {
                                is.close();
                            } catch (final Throwable e) {
                            }
                        }
                    }
                }
                /* sort positions */
                ArrayList<Integer> positions = new ArrayList<Integer>(map.keySet());
                Collections.sort(positions);
                /* build final ArrayList of CrawledPackage */
                ArrayList<CrawledPackage> ret2 = new ArrayList<CrawledPackage>(positions.size());
                for (Integer position : positions) {
                    ret2.add(map.get(position));
                }
                map = null;
                positions = null;
                ret = new LinkedList<CrawledPackage>(ret2);
            } catch (final Throwable e) {
                Log.exception(e);
            } finally {
                try {
                    zip.close();
                } catch (final Throwable e) {
                }
            }
        }
        return ret;
    }

    /**
     * saves List of CrawledPackages to given File as ZippedJSon
     * 
     * @param packages
     * @param file
     */
    private void save(final ArrayList<CrawledPackage> packages, final File file) {
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.HIGH) {

            @Override
            protected Void run() throws RuntimeException {
                if (packages != null && file != null) {
                    /* prepare tmp file */
                    final File tmpfile = new File(file.getAbsolutePath() + ".tmp");
                    tmpfile.getParentFile().mkdirs();
                    tmpfile.delete();
                    ZipIOWriter zip = null;
                    int index = 0;
                    /* prepare formatter for package filenames in zipfiles */
                    String format = "%02d";
                    if (packages.size() >= 10) {
                        format = String.format("%%0%dd", (int) Math.log10(packages.size()) + 1);
                    }
                    try {
                        MessageDigest md = MessageDigest.getInstance("SHA1");
                        zip = new ZipIOWriter(tmpfile, true);
                        for (CrawledPackage pkg : packages) {
                            /* convert FilePackage to JSon */
                            CrawledPackageStorable storable = new CrawledPackageStorable(pkg);
                            /* save packageID */
                            storable.setPackageID(LinkCollector.this.getPackageMapID(pkg));
                            String string = JSonStorage.toString(storable);
                            storable = null;
                            byte[] bytes = string.getBytes("UTF-8");
                            string = null;
                            md.update(bytes);
                            zip.addByteArry(bytes, true, "", String.format(format, (index++)));
                        }
                        String check = System.currentTimeMillis() + ":" + packages.size() + ":" + HexFormatter.byteArrayToHex(md.digest());
                        zip.addByteArry(check.getBytes("UTF-8"), true, "", getCheckFileName());
                        /* close ZipIOWriter, so we can rename tmp file now */
                        try {
                            zip.close();
                        } catch (final Throwable e) {
                            return null;
                        }
                        /* try to delete destination file if it already exists */
                        if (file.exists()) {
                            if (!file.delete()) {
                                Log.exception(new WTFException("Could not delete: " + file.getAbsolutePath()));
                                return null;
                            }
                        }
                        /* rename tmpfile to destination file */
                        if (!tmpfile.renameTo(file)) {
                            Log.exception(new WTFException("Could not rename file: " + tmpfile + " to " + file));
                            return null;
                        }
                        return null;
                    } catch (final Throwable e) {
                        Log.exception(e);
                    } finally {
                        try {
                            zip.close();
                        } catch (final Throwable e) {
                        }
                    }
                }
                return null;
            }
        });
    }

    /**
     * save the current CrawledPackages/CrawledLinks controlled by this
     * LinkCollector
     */
    public void saveLinkCollectorLinks() {
        if (isSaveAllowed() == false) return;
        ArrayList<CrawledPackage> packages = null;
        final boolean readL = this.readLock();
        try {
            packages = new ArrayList<CrawledPackage>(this.packages);
        } finally {
            readUnlock(readL);
        }
        /* save as new Json ZipFile */
        save(packages, getLinkCollectorListFile());
    }

    private File getLinkCollectorListFile() {
        return Application.getResource("cfg/linkcollector.zip");
    }

    private void setArchiver(ExtractionExtension archiver) {
        this.archiver = archiver;
    }

    /**
     * @param crawlerFilter
     *            the crawlerFilter to set
     */
    public void setCrawlerFilter(LinkCrawlerFilter crawlerFilter) {
        if (crawlerFilter == null) throw new IllegalArgumentException("crawlerFilter is null");
        this.crawlerFilter = crawlerFilter;
    }

    public void setLoadAllowed(boolean allowLoad) {
        this.allowLoad = allowLoad;
    }

    public void setPackagizer(PackagizerInterface packagizerInterface) {
        this.packagizer = packagizerInterface;
    }

    public void setSaveAllowed(boolean allowSave) {
        this.allowSave = allowSave;
    }

}
