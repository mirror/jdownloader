package jd.controlling.linkcollector;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.zip.ZipEntry;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLink.LinkState;
import jd.controlling.linkcrawler.CrawledLinkProperty;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerFilter;
import jd.controlling.linkcrawler.LinkCrawlerHandler;
import jd.controlling.linkcrawler.PackageInfo;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.PackageController;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.controlling.SingleReachableState;
import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.zip.ZipIOReader;
import org.appwork.utils.zip.ZipIOWriter;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.plugins.controller.host.PluginFinder;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;
import org.jdownloader.translate._JDT;

public class LinkCollector extends PackageController<CrawledPackage, CrawledLink> implements LinkCheckerHandler<CrawledLink>, LinkCrawlerHandler, ShutdownVetoListener {

    public static final class CrawledLinkCrawler extends LinkCollectorCrawler {
        private LinkCollector linkCollector;

        public CrawledLinkCrawler(LinkCollector linkCollector) {
            this.linkCollector = linkCollector;
            setFilter(linkCollector.getCrawlerFilter());
            setHandler(linkCollector);
        }

        @Override
        protected void generalCrawledLinkModifier(CrawledLink link) {
            LinkCollector.crawledLinkModifier(link, link.getSourceJob());
        }

        @Override
        protected void crawlerStopped() {
            linkCollector.onCrawlerStopped(this);
            super.crawlerStopped();

        }

        @Override
        protected void crawlerStarted() {
            linkCollector.onCrawlerStarted(this);
            super.crawlerStarted();

        }
    }

    public static final class JobLinkCrawler extends LinkCollectorCrawler {
        private final LinkCollectingJob   job;
        private LinkCollectingInformation collectingInfo;
        private LinkCollector             linkCollector;

        public JobLinkCrawler(final LinkCollector linkCollector, LinkCollectingJob job) {
            this.job = job;
            collectingInfo = new LinkCollectingInformation(this, linkCollector.getLinkChecker());
            this.linkCollector = linkCollector;
            setFilter(linkCollector.getCrawlerFilter());
            final LinkCrawlerHandler defaultHandler = defaulHandlerFactory();
            setHandler(new LinkCrawlerHandler() {

                @Override
                public void handleUnHandledLink(CrawledLink link) {
                    linkCollector.handleUnHandledLink(link);
                    defaultHandler.handleUnHandledLink(link);
                }

                @Override
                public void handleFinalLink(CrawledLink link) {
                    linkCollector.handleFinalLink(link);
                    defaultHandler.handleFinalLink(link);
                }

                @Override
                public void handleFilteredLink(CrawledLink link) {
                    linkCollector.handleFilteredLink(link);
                    defaultHandler.handleFilteredLink(link);
                }

                @Override
                public void handleBrokenLink(CrawledLink link) {
                    linkCollector.handleBrokenLink(link);
                    defaultHandler.handleBrokenLink(link);
                }
            });
        }

        public LinkCollectingJob getJob() {
            return job;
        }

        @Override
        protected CrawledLink crawledLinkFactorybyURL(String url) {
            CrawledLink ret = new CrawledLink(url);
            ret.setCollectingInfo(collectingInfo);
            ret.setSourceJob(job);
            return ret;
        }

        @Override
        protected void generalCrawledLinkModifier(CrawledLink link) {
            LinkCollector.crawledLinkModifier(link, job);
        }

        @Override
        protected void crawlerStopped() {
            linkCollector.onCrawlerStopped(this);
            super.crawlerStopped();

        }

        @Override
        protected void crawlerStarted() {
            linkCollector.onCrawlerStarted(this);
            super.crawlerStarted();

        }
    }

    public static LinkCollector getInstance() {
        return INSTANCE;
    }

    private void onCrawlerAdded(LinkCollectorCrawler jobLinkCrawler) {

        eventsender.fireEvent(new LinkCollectorEvent(this, LinkCollectorEvent.TYPE.CRAWLER_ADDED, jobLinkCrawler, QueuePriority.NORM));
    }

    public void onCrawlerStopped(LinkCollectorCrawler crawledLinkCrawler) {
        getEventsender().removeListener(crawledLinkCrawler);
        eventsender.fireEvent(new LinkCollectorEvent(this, LinkCollectorEvent.TYPE.CRAWLER_STOPPED, crawledLinkCrawler, QueuePriority.NORM));
    }

    public void onCrawlerStarted(LinkCollectorCrawler crawledLinkCrawler) {
        getEventsender().addListener(crawledLinkCrawler, true);
        eventsender.fireEvent(new LinkCollectorEvent(this, LinkCollectorEvent.TYPE.CRAWLER_STARTED, crawledLinkCrawler, QueuePriority.NORM));
    }

    private transient LinkCollectorEventSender           eventsender          = new LinkCollectorEventSender();
    public final ScheduledExecutorService                TIMINGQUEUE          = DelayedRunnable.getNewScheduledExecutorService();
    public static SingleReachableState                   CRAWLERLIST_LOADED   = new SingleReachableState("CRAWLERLIST_COMPLETE");
    private static LinkCollector                         INSTANCE             = new LinkCollector();

    private volatile LinkChecker<CrawledLink>            linkChecker          = null;
    /**
     * NOTE: only access these fields inside the IOEQ
     */
    private HashSet<String>                              dupeCheckMap         = new HashSet<String>();
    private HashMap<String, CrawledPackage>              packageMap           = new HashMap<String, CrawledPackage>();

    /* sync on filteredStuff itself when accessing it */
    private java.util.List<CrawledLink>                  filteredStuff        = new ArrayList<CrawledLink>();

    private LinkCrawlerFilter                            crawlerFilter        = null;

    private ExtractionExtension                          archiver;
    private DelayedRunnable                              asyncSaving          = null;

    private PackagizerInterface                          packagizer           = null;

    protected CrawledPackage                             offlinePackage;

    protected CrawledPackage                             variousPackage;

    protected CrawledPackage                             permanentofflinePackage;

    private final CopyOnWriteArrayList<File>             linkcollectorLists   = new CopyOnWriteArrayList<File>();

    private HashMap<String, java.util.List<CrawledLink>> offlineMap           = new HashMap<String, java.util.List<CrawledLink>>();
    private HashMap<String, java.util.List<CrawledLink>> variousMap           = new HashMap<String, java.util.List<CrawledLink>>();
    private HashMap<String, java.util.List<CrawledLink>> hosterMap            = new HashMap<String, java.util.List<CrawledLink>>();
    private HashMap<Object, Object>                      autoRenameCache;
    private DelayedRunnable                              asyncCacheCleanup;

    private boolean                                      restoreButtonEnabled = false;

    private LinkCollector() {
        autoRenameCache = new HashMap<Object, Object>();
        ShutdownController.getInstance().addShutdownVetoListener(this);

        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                LinkCollector.this.abort();
                QUEUE.addWait(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.HIGH) {

                    @Override
                    protected Void run() throws RuntimeException {
                        saveLinkCollectorLinks();
                        return null;
                    }
                });
            }

            @Override
            public String toString() {
                return "save linkcollector...";
            }
        });
        asyncSaving = new DelayedRunnable(TIMINGQUEUE, 5000l, 60000l) {

            @Override
            public void run() {
                if (allowSaving()) {
                    super.run();
                }
            }

            @Override
            public String getID() {
                return "LinkCollectorSave";
            }

            @Override
            public void delayedrun() {
                saveLinkCollectorLinks();
            }

        };
        asyncCacheCleanup = new DelayedRunnable(TIMINGQUEUE, 30000l, 120000l) {
            @Override
            public String getID() {
                return "LinkCollectorCleanup";
            }

            @Override
            public void delayedrun() {
                QUEUE.add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        autoRenameCache.clear();
                        return null;
                    }
                });

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

            public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
                asyncCacheCleanup.run();
            }

            @Override
            public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
            }

            @Override
            public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
                asyncSaving.run();
            }

            @Override
            public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
            }

            @Override
            public void onLinkCrawlerAdded(LinkCollectorCrawler parameter) {
            }

            @Override
            public void onLinkCrawlerStarted(LinkCollectorCrawler parameter) {
            }

            @Override
            public void onLinkCrawlerStopped(LinkCollectorCrawler parameter) {
            }

        });
    }

    public LinkCollectorEventSender getEventsender() {
        return eventsender;
    }

    @Override
    protected void _controllerPackageNodeAdded(CrawledPackage pkg, QueuePriority priority) {
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.ADD_CONTENT, pkg, priority));
    }

    @Override
    protected void _controllerPackageNodeRemoved(CrawledPackage pkg, QueuePriority priority) {
        /* update packageMap */
        if (TYPE.NORMAL != pkg.getType()) {
            switch (pkg.getType()) {
            case OFFLINE:
                offlinePackage = null;
                break;
            case POFFLINE:
                permanentofflinePackage = null;
                break;
            case VARIOUS:
                variousPackage = null;
                break;
            }
        }
        String id = getPackageMapID(pkg);
        if (id != null) packageMap.remove(id);
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REMOVE_CONTENT, pkg, priority));
        java.util.List<CrawledLink> children = null;
        boolean readL = pkg.getModifyLock().readLock();
        try {
            children = new ArrayList<CrawledLink>(pkg.getChildren());
        } finally {
            pkg.getModifyLock().readUnlock(readL);
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
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REMOVE_CONTENT, new ArrayList<CrawledLink>(links), priority));
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

    protected void autoFileNameCorrection(List<CrawledLink> pkgchildren) {
        // long t = System.currentTimeMillis();
        // if we have only one single hoster, we can hardly learn anything
        if (CFG_LINKGRABBER.AUTO_FILENAME_CORRECTION_ENABLED.isEnabled() && hosterMap.size() > 1) {
            ArrayList<DownloadLink> dlinks = new ArrayList<DownloadLink>();
            ArrayList<DownloadLink> maybebadfilenames = new ArrayList<DownloadLink>();
            for (CrawledLink link : pkgchildren) {
                if (!link.gethPlugin().isHosterManipulatesFilenames()) {
                    dlinks.add(link.getDownloadLink());
                } else {
                    maybebadfilenames.add(link.getDownloadLink());
                }
            }

            for (CrawledLink link : pkgchildren) {
                String name = link.getDownloadLink().getNameSetbyPlugin();
                if (name == null) {

                    continue;
                }
                String newName = link.gethPlugin().autoFilenameCorrection(autoRenameCache, name, link.getDownloadLink(), dlinks);
                if (newName != null && !newName.equals(name)) {
                    logger.info("Renamed file " + name + " to " + newName);
                } else {
                    newName = link.gethPlugin().autoFilenameCorrection(autoRenameCache, name, link.getDownloadLink(), maybebadfilenames);
                    if (newName != null && !newName.equals(name)) {
                        logger.info("Renamed file2 " + name + " to " + newName);
                    }
                }
                if (newName != null && !name.equals(newName)) {
                    /*
                     * we do not force a filename if newName equals to name set by plugin!
                     */
                    link.getDownloadLink().forceFileName(newName);
                }
            }
        }
    }

    public void moveOrAddAt(final CrawledPackage pkg, final List<CrawledLink> movechildren, final int index) {

        super.moveOrAddAt(pkg, movechildren, index);

    }

    private void addCrawledLink(final CrawledLink link) {

        /* try to find good matching package or create new one */
        QUEUE.add(new QueueAction<Void, RuntimeException>() {

            public void newPackage(final java.util.List<CrawledLink> links, String packageName, String downloadFolder, String identifier) {
                CrawledPackage pkg = new CrawledPackage();
                pkg.setExpanded(JsonConfig.create(LinkCollectorConfig.class).isPackageAutoExpanded());
                pkg.setName(packageName);
                if (downloadFolder != null) {
                    pkg.setDownloadFolder(downloadFolder);
                }
                packageMap.put(identifier, pkg);
                if (links != null && links.size() > 0) {
                    LinkCollector.this.moveOrAddAt(pkg, links, -1);
                }
                // check of we have matching links in offline maper
                java.util.List<CrawledLink> list = offlineMap.remove(identifier);
                if (list != null && list.size() > 0) {
                    LinkCollector.this.moveOrAddAt(pkg, list, -1);
                }
                list = variousMap.remove(identifier);
                if (list != null && list.size() > 0) {
                    LinkCollector.this.moveOrAddAt(pkg, list, -1);
                }
            }

            @Override
            protected Void run() throws RuntimeException {
                try {
                    if (link.getDownloadLink() != null) {
                        /* set CrawledLink as changeListener to its DownloadLink */
                        link.getDownloadLink().setNodeChangeListener(link);
                    }
                    PackageInfo dpi = link.getDesiredPackageInfo();
                    UniqueAlltimeID uID = null;

                    String packageName = null;
                    String packageID = null;
                    String downloadFolder = null;
                    boolean ignoreSpecialPackages = dpi != null && (dpi.isPackagizerRuleMatched() || dpi.isIgnoreVarious());
                    boolean isMultiArchive = false;
                    if (dpi != null) {
                        packageName = dpi.getName();
                        if ((uID = dpi.getUniqueId()) != null) {
                            packageID = dpi.getUniqueId().toString();
                        }
                        downloadFolder = dpi.getDestinationFolder();
                        if (downloadFolder != null) {
                            downloadFolder = downloadFolder.replaceFirst("/$", "");
                            String defaultFolder = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
                            if (defaultFolder != null) defaultFolder = defaultFolder.replaceFirst("/$", "");
                            if (!downloadFolder.equals(defaultFolder)) {
                                /* we have a custom downloadFolder, so let's not use various package */
                                ignoreSpecialPackages = true;
                            }
                        }
                    }
                    CrawledLinkFactory clf = new CrawledLinkFactory(link);
                    ExtractionExtension lArchiver = archiver;
                    if (lArchiver != null && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.ARCHIVE_PACKAGIZER_ENABLED.getValue()) {
                        if (lArchiver.isMultiPartArchive(clf)) {
                            isMultiArchive = true;
                        }
                    }
                    if (packageName == null) {
                        packageName = LinknameCleaner.cleanFileName(link.getName());
                        if (isMultiArchive) {
                            packageID = lArchiver.createArchiveID(clf);
                            if (packageID != null) {
                                packageName = _JDT._.LinkCollector_archiv(LinknameCleaner.cleanFileName(lArchiver.getArchiveName(clf), false, true));
                            }
                        }
                    }

                    String identifier = (packageID + "_||_" + packageName + "_||_" + downloadFolder);
                    if (CrossSystem.isWindows() || CFG_LINKGRABBER.AUTO_FILENAME_CORRECTION_ENABLED.isEnabled()) {
                        // only on windows, because mac and linux have case
                        // sensitive file systems.
                        identifier = identifier.toLowerCase(Locale.ENGLISH);
                    }
                    LazyHostPlugin lazyPlg = null;
                    PluginForHost plg = link.gethPlugin();

                    if (plg != null && CFG_LINKGRABBER.AUTO_PACKAGE_MATCHING_CORRECTION_ENABLED.isEnabled()) {
                        identifier = plg.filterPackageID(identifier);
                        // run through all active hosts, and filter the
                        // identifier
                        for (String host : hosterMap.keySet()) {
                            lazyPlg = HostPluginController.getInstance().get(host);
                            PluginForHost plg2 = null;
                            if (lazyPlg != null) {
                                try {
                                    plg2 = lazyPlg.getPrototype(null);
                                } catch (UpdateRequiredClassNotFoundException e) {
                                    logger.log(e);
                                }
                            }
                            if (plg2 != null) {
                                identifier = plg2.filterPackageID(identifier);
                            }
                        }
                        java.util.List<CrawledLink> list = hosterMap.get(link.getHost());

                        if (list == null || list.size() == 0) {
                            // new hoster - we have to update all identifiers
                            // and maps

                            HashMap<String, CrawledPackage> newMap = new HashMap<String, CrawledPackage>();
                            Entry<String, CrawledPackage> next;
                            for (Iterator<Entry<String, CrawledPackage>> it = packageMap.entrySet().iterator(); it.hasNext();) {
                                next = it.next();
                                String newID = plg.filterPackageID(next.getKey());
                                if (!next.getKey().equals(newID)) {
                                    it.remove();

                                    CrawledPackage existing = newMap.get(newID);

                                    if (existing != null) {
                                        List<CrawledLink> links = null;
                                        boolean readL = existing.getModifyLock().readLock();
                                        try {
                                            links = existing.getChildren();
                                        } finally {
                                            existing.getModifyLock().readUnlock(readL);
                                        }
                                        moveOrAddAt(next.getValue(), links, -1);
                                    }
                                    newMap.put(newID, next.getValue());
                                }
                            }

                            for (Iterator<Entry<String, CrawledPackage>> it = newMap.entrySet().iterator(); it.hasNext();) {
                                next = it.next();
                                CrawledPackage current = packageMap.remove(next.getKey());

                                if (current != null) {
                                    moveOrAddAt(current, next.getValue().getChildren(), -1);
                                } else {
                                    packageMap.put(next.getKey(), next.getValue());
                                }

                            }
                            remapIdentifier(variousMap, plg);
                            remapIdentifier(offlineMap, plg);
                        }

                    }
                    // add to hostermap
                    java.util.List<CrawledLink> hosterlist = getIdentifiedMap(link.getHost(), hosterMap);
                    hosterlist.add(link);

                    CrawledPackage pkg = packageMap.get(identifier);
                    if (pkg == null) {
                        if (!ignoreSpecialPackages && LinkCrawler.PERMANENT_OFFLINE_ID == uID) {
                            /* these links will never come back online */
                            List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                            add.add(link);
                            LinkCollector.this.moveOrAddAt(getPermanentOfflineCrawledPackage(), add, -1);
                        } else if (!ignoreSpecialPackages && link.getLinkState() == LinkState.OFFLINE && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.OFFLINE_PACKAGE_ENABLED.getValue()) {
                            java.util.List<CrawledLink> list = getIdentifiedMap(identifier, offlineMap);
                            list.add(link);
                            List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                            add.add(link);
                            LinkCollector.this.moveOrAddAt(getOfflineCrawledPackage(), add, -1);
                        } else if (!ignoreSpecialPackages && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.VARIOUS_PACKAGE_LIMIT.getValue() > 0 && CFG_LINKGRABBER.VARIOUS_PACKAGE_ENABLED.isEnabled()) {
                            java.util.List<CrawledLink> list = getIdentifiedMap(identifier, variousMap);
                            list.add(link);
                            if (list.size() > org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.VARIOUS_PACKAGE_LIMIT.getValue()) {
                                newPackage(null, packageName, downloadFolder, identifier);
                            } else {
                                List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                                add.add(link);
                                LinkCollector.this.moveOrAddAt(getVariousCrawledPackage(), add, -1);
                            }
                        } else {
                            java.util.List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                            add.add(link);
                            newPackage(add, packageName, downloadFolder, identifier);
                        }
                    } else {
                        List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                        add.add(link);
                        LinkCollector.this.moveOrAddAt(pkg, add, -1);
                    }
                    eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.ADDED_LINK, link, QueuePriority.NORM));
                    return null;
                } catch (Throwable e) {
                    dupeCheckMap.remove(link.getLinkID());
                    logger.log(e);
                    if (e instanceof RuntimeException) throw (RuntimeException) e;
                    throw new RuntimeException(e);
                } finally {
                    link.setCollectingInfo(null);
                    link.setSourceJob(null);
                }
            }
        });
    }

    private static void remapIdentifier(HashMap<String, java.util.List<CrawledLink>> map, PluginForHost plg) {

        Entry<String, java.util.List<CrawledLink>> entry;
        HashMap<String, java.util.List<CrawledLink>> mapmap = new HashMap<String, java.util.List<CrawledLink>>();
        for (Iterator<Entry<String, java.util.List<CrawledLink>>> it = map.entrySet().iterator(); it.hasNext();) {
            entry = it.next();
            String newID = plg.filterPackageID(entry.getKey());
            if (!entry.getKey().equals(newID)) {
                it.remove();

                java.util.List<CrawledLink> existing = mapmap.get(newID);

                if (existing != null) {
                    existing.addAll(entry.getValue());

                }
                mapmap.put(newID, entry.getValue());
            }
        }
        for (Iterator<Entry<String, java.util.List<CrawledLink>>> it = mapmap.entrySet().iterator(); it.hasNext();) {
            entry = it.next();
            java.util.List<CrawledLink> current = map.remove(entry.getKey());
            if (current != null) {
                current.addAll(entry.getValue());
            } else {
                map.put(entry.getKey(), entry.getValue());
            }

        }

    }

    private java.util.List<CrawledLink> getIdentifiedMap(String identifier, HashMap<String, java.util.List<CrawledLink>> map) {
        java.util.List<CrawledLink> list = map.get(identifier);
        if (list == null) {
            list = new ArrayList<CrawledLink>();
            map.put(identifier, list);
        }
        return list;
    }

    private CrawledPackage getPermanentOfflineCrawledPackage() {
        CrawledPackage lpermanentofflinePackage = permanentofflinePackage;
        if (lpermanentofflinePackage != null && TYPE.POFFLINE == lpermanentofflinePackage.getType()) return lpermanentofflinePackage;
        lpermanentofflinePackage = new CrawledPackage();
        lpermanentofflinePackage.setExpanded(true);
        lpermanentofflinePackage.setName(_GUI._.Permanently_Offline_Package());
        lpermanentofflinePackage.setType(TYPE.POFFLINE);
        permanentofflinePackage = lpermanentofflinePackage;
        return lpermanentofflinePackage;
    }

    private CrawledPackage getVariousCrawledPackage() {
        CrawledPackage lvariousPackage = variousPackage;
        if (lvariousPackage != null && TYPE.VARIOUS == lvariousPackage.getType()) return lvariousPackage;
        lvariousPackage = new CrawledPackage();
        lvariousPackage.setExpanded(true);
        lvariousPackage.setName(_JDT._.LinkCollector_addCrawledLink_variouspackage());
        lvariousPackage.setType(TYPE.VARIOUS);
        variousPackage = lvariousPackage;
        return lvariousPackage;
    }

    private CrawledPackage getOfflineCrawledPackage() {
        CrawledPackage lofflinePackage = offlinePackage;
        if (lofflinePackage != null && TYPE.OFFLINE == lofflinePackage.getType()) return lofflinePackage;
        lofflinePackage = new CrawledPackage();
        lofflinePackage.setExpanded(true);
        lofflinePackage.setName(_JDT._.LinkCollector_addCrawledLink_offlinepackage());
        lofflinePackage.setType(TYPE.OFFLINE);
        offlinePackage = lofflinePackage;
        return lofflinePackage;
    }

    public LinkCrawler addCrawlerJob(final java.util.List<CrawledLink> links) {
        if (links == null || links.size() == 0) throw new IllegalArgumentException("no links");
        lazyInit();
        synchronized (shutdownLock) {
            if (ShutdownController.getInstance().isShutDownRequested()) return null;
            final LinkCollectorCrawler lc = new CrawledLinkCrawler(this);

            LinkCollectingInformation collectingInfo = new LinkCollectingInformation(lc, linkChecker);
            java.util.List<CrawledLink> jobs = new ArrayList<CrawledLink>(links);
            collectingInfo.setLinkCrawler(lc);
            collectingInfo.setLinkChecker(linkChecker);
            for (CrawledLink job : jobs) {
                job.setCollectingInfo(collectingInfo);
            }
            onCrawlerAdded(lc);
            lc.crawl(jobs);

            return lc;
        }
    }

    private static void crawledLinkModifier(CrawledLink link, LinkCollectingJob job) {
        if (job != null && link != null) {
            if (link.getDownloadLink() != null) {
                if (!StringUtils.isEmpty(job.getCustomSourceUrl())) link.getDownloadLink().setBrowserUrl(job.getCustomSourceUrl());
                if (!StringUtils.isEmpty(job.getCustomComment())) link.getDownloadLink().setComment(job.getCustomComment());
                if (!StringUtils.isEmpty(job.getDownloadPassword())) link.getDownloadLink().setDownloadPassword(job.getDownloadPassword());
            }
            if (job.getOutputFolder() != null && (link.getDesiredPackageInfo() == null || link.getDesiredPackageInfo().getDestinationFolder() == null)) {
                if (link.getDesiredPackageInfo() == null) link.setDesiredPackageInfo(new PackageInfo());
                link.getDesiredPackageInfo().setDestinationFolder(job.getOutputFolder().getAbsolutePath());
            }
            if (!StringUtils.isEmpty(job.getPackageName()) && (link.getDesiredPackageInfo() == null || StringUtils.isEmpty(link.getDesiredPackageInfo().getName()))) {
                if (link.getDesiredPackageInfo() == null) link.setDesiredPackageInfo(new PackageInfo());
                link.getDesiredPackageInfo().setName(job.getPackageName());
            }
            if (job.getExtractPasswords() != null && job.getExtractPasswords().size() > 0) {
                if (link.getDesiredPackageInfo() == null) link.setDesiredPackageInfo(new PackageInfo());
                link.getArchiveInfo().getExtractionPasswords().addAll(job.getExtractPasswords());
            }
            if (job.getAutoExtract() != null) {
                if (link.getArchiveInfo().getAutoExtract() == BooleanStatus.UNSET) {
                    link.getArchiveInfo().setAutoExtract(job.getAutoExtract());
                }
            }
            if (job.isAutoStart()) {
                link.setAutoConfirmEnabled(true);
                link.setAutoStartEnabled(true);
            }

            if (job.getPriority() != null) {
                link.setPriority(job.getPriority());
            }
        }
    }

    public LinkCrawler addCrawlerJob(final LinkCollectingJob job) {
        try {
            if (job == null) throw new IllegalArgumentException("job is null");
            lazyInit();
            synchronized (shutdownLock) {
                if (ShutdownController.getInstance().isShutDownRequested()) return null;
                final JobLinkCrawler lc = new JobLinkCrawler(this, job);

                String jobText = job.getText();
                /*
                 * we don't want to keep reference on text during the whole link grabbing/checking/collecting way
                 */

                onCrawlerAdded(lc);
                // keep text if it is tiny.
                // if we have the text in the job, we can display it for example in the balloons
                if (StringUtils.isNotEmpty(jobText) && jobText.length() > 500) {
                    job.setText(null);
                }
                lc.crawl(jobText, job.getCustomSourceUrl(), job.isDeepAnalyse());
                return lc;
            }
        } catch (VerifyError e) {
            Dialog.getInstance().showExceptionDialog("Eclipse Java 1.7 Bug", "This is an eclipse Java 7 bug. See here: http://goo.gl/REs9c\r\nAdd JVM Parameter -XX:-UseSplitVerifier", e);

            throw e;
        }
    }

    private void addFilteredStuff(final CrawledLink filtered, final boolean checkDupe) {
        filtered.setCollectingInfo(null);
        if (restoreButtonEnabled == false) {
            /** RestoreButton is disabled, no need to save the filtered links */
            return;
        }
        QUEUE.add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (checkDupe && !dupeCheckMap.add(filtered.getLinkID())) {
                    eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.DUPE_LINK, filtered, QueuePriority.NORM));
                    return null;
                }
                filteredStuff.add(filtered);
                eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.FILTERED_AVAILABLE));
                return null;
            }
        });

    }

    // clean up offline/various/dupeCheck maps
    protected void cleanupMaps(List<CrawledLink> links) {
        if (links == null) return;
        for (CrawledLink l : links) {
            dupeCheckMap.remove(l.getLinkID());
            removeFromMap(variousMap, l);
            removeFromMap(offlineMap, l);
            removeFromMap(hosterMap, l);
        }
    }

    @Override
    public void clear() {
        QUEUE.add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                LinkCollector.super.clear();
                filteredStuff.clear();
                dupeCheckMap.clear();
                offlinePackage = null;
                variousPackage = null;
                permanentofflinePackage = null;
                variousMap.clear();
                offlineMap.clear();
                hosterMap.clear();
                asyncCacheCleanup.stop();
                asyncCacheCleanup.delayedrun();
                eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.FILTERED_EMPTY));
                return null;
            }
        });
    }

    /*
     * converts a CrawledPackage into a FilePackage
     * 
     * if plinks is not set, then the original children of the CrawledPackage will get added to the FilePackage
     * 
     * if plinks is set, then only plinks will get added to the FilePackage
     */
    private FilePackage createFilePackage(final CrawledPackage pkg, java.util.List<CrawledLink> plinks) {
        FilePackage ret = FilePackage.getInstance();
        /* set values */
        ret.setName(pkg.getName());
        /* FilePackage contains full absolute path! */

        ret.setDownloadDirectory(LinkTreeUtils.getDownloadDirectory(pkg).toString());

        ret.setCreated(pkg.getCreated());
        ret.setExpanded(pkg.isExpanded());
        ret.setComment(pkg.getComment());

        List<CrawledLink> pkgLinks = null;
        if (plinks != null && plinks.size() > 0) {
            pkgLinks = new ArrayList<CrawledLink>(plinks);
        } else {
            boolean readL = pkg.getModifyLock().readLock();
            try {
                /* add Children from CrawledPackage to FilePackage */
                pkgLinks = new ArrayList<CrawledLink>(pkg.getChildren());
            } finally {
                pkg.getModifyLock().readUnlock(readL);
            }
        }
        java.util.List<DownloadLink> links = new ArrayList<DownloadLink>(pkgLinks.size());
        for (CrawledLink link : pkgLinks) {
            /* extract DownloadLink from CrawledLink */
            DownloadLink dl = link.getDownloadLink();

            if (dl != null) {
                /* remove reference to crawledLink */
                dl.setNodeChangeListener(null);
                /*
                 * change filename if it is different than original downloadlink
                 */
                if (link.isNameSet()) dl.forceFileName(link.getName());
                /* set correct enabled/disabled state */
                dl.setEnabled(link.isEnabled());
                dl.setCreated(link.getCreated());
                links.add(dl);
                /* set correct Parent node */
                dl.setParentNode(ret);
            }
        }

        /* add all children to FilePackage */
        ret.getChildren().addAll(links);

        return ret;
    }

    /**
     * @return the crawlerFilter
     */
    public LinkCrawlerFilter getCrawlerFilter() {
        return crawlerFilter;
    }

    public java.util.List<CrawledLink> getFilteredStuff(final boolean clearAfterGet) {
        return QUEUE.addWait(new QueueAction<java.util.List<CrawledLink>, RuntimeException>() {

            @Override
            protected java.util.List<CrawledLink> run() throws RuntimeException {
                java.util.List<CrawledLink> ret2 = new ArrayList<CrawledLink>(filteredStuff);
                if (clearAfterGet) {
                    filteredStuff.clear();
                    cleanupMaps(ret2);
                    eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.FILTERED_EMPTY));
                }
                return ret2;
            }
        });
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
        /* this method is called from LinkCrawler directly, we have to update dupeCheckMap */
        addFilteredStuff(link, true);
    }

    public void handleFinalLink(final CrawledLink link) {
        if (org.jdownloader.settings.staticreferences.CFG_LINKCOLLECTOR.DO_LINK_CHECK.isEnabled()) {
            QUEUE.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    /* avoid additional linkCheck when linkID already exists */
                    /* update dupeCheck map */
                    if (!dupeCheckMap.add(link.getLinkID())) {
                        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.DUPE_LINK, link, QueuePriority.NORM));
                        return null;
                    }
                    linkChecker.check(link);
                    return null;
                }
            });
        } else {
            PackagizerInterface pc;
            if ((pc = getPackagizer()) != null) {
                /* run packagizer on un-checked link */
                pc.runByUrl(link);
            }
            QUEUE.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    /* update dupeCheck map */
                    if (!dupeCheckMap.add(link.getLinkID())) {
                        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.DUPE_LINK, link, QueuePriority.NORM));
                        return null;
                    }
                    addCrawledLink(link);
                    return null;
                }
            });
        }
    }

    private void lazyInit() {
        if (linkChecker != null) return;
        synchronized (this) {
            if (linkChecker != null) return;
            setCrawlerFilter(LinkFilterController.getInstance());
            restoreButtonEnabled = CFG_LINKGRABBER.RESTORE_BUTTON_ENABLED.isEnabled();
            setPackagizer(PackagizerController.getInstance());
            LinkChecker<CrawledLink> llinkChecker = new LinkChecker<CrawledLink>();
            llinkChecker.setLinkCheckHandler(this);
            linkChecker = llinkChecker;
        }
    }

    public void linkCheckDone(final CrawledLink link) {
        /* this method is called by LinkChecker, we already updated the dupeCheckMap */
        if (crawlerFilter.dropByFileProperties(link)) {
            addFilteredStuff(link, false);
        } else {
            PackagizerInterface pc;
            if ((pc = getPackagizer()) != null) {
                /* run packagizer on checked link */
                pc.runByFile(link);
            }
            addCrawledLink(link);
        }
    }

    public java.util.List<FilePackage> convert(final List<CrawledLink> links, final boolean removeLinks) {
        if (links == null || links.size() == 0) return null;
        return QUEUE.addWait(new QueueAction<java.util.List<FilePackage>, RuntimeException>() {

            @Override
            protected java.util.List<FilePackage> run() throws RuntimeException {
                java.util.List<FilePackage> ret = new ArrayList<FilePackage>();
                HashMap<CrawledPackage, java.util.List<CrawledLink>> map = new HashMap<CrawledPackage, java.util.List<CrawledLink>>();
                if (removeLinks) cleanupMaps(links);
                for (CrawledLink link : links) {
                    CrawledPackage parent = link.getParentNode();
                    if (parent == null || parent.getControlledBy() != LinkCollector.this) {
                        logger.log(new Throwable("not controlled by this packagecontroller"));
                        continue;
                    }
                    java.util.List<CrawledLink> pkg_links = map.get(parent);
                    if (pkg_links == null) {
                        pkg_links = new ArrayList<CrawledLink>();
                        map.put(parent, pkg_links);
                    }
                    pkg_links.add(link);
                }
                Iterator<Entry<CrawledPackage, java.util.List<CrawledLink>>> it = map.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<CrawledPackage, java.util.List<CrawledLink>> next = it.next();
                    if (removeLinks) LinkCollector.this.removeChildren(next.getKey(), next.getValue(), true);
                    ret.add(createFilePackage(next.getKey(), next.getValue()));
                }
                return ret;
            }

        });
    }

    // clean up offline and various map
    private void removeFromMap(HashMap<String, java.util.List<CrawledLink>> idListMap, CrawledLink l) {
        Iterator<Entry<String, java.util.List<CrawledLink>>> it = idListMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, java.util.List<CrawledLink>> elem = it.next();
            String identifier = elem.getKey();
            java.util.List<CrawledLink> mapElems = elem.getValue();
            if (mapElems != null && mapElems.remove(l)) {
                if (mapElems.size() == 0) idListMap.remove(identifier);
                break;
            }
        }
    }

    private String getIDFromMap(HashMap<String, java.util.List<CrawledLink>> idListMap, CrawledLink l) {
        Iterator<Entry<String, java.util.List<CrawledLink>>> it = idListMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, java.util.List<CrawledLink>> elem = it.next();
            String identifier = elem.getKey();
            java.util.List<CrawledLink> mapElems = elem.getValue();
            if (mapElems != null && mapElems.contains(l)) return identifier;
        }
        return null;
    }

    private ArrayList<File> findAvailableCollectorLists() {
        File[] filesInCfg = Application.getResource("cfg/").listFiles();
        ArrayList<Long> sortedAvailable = new ArrayList<Long>();
        ArrayList<File> ret = new ArrayList<File>();
        if (filesInCfg != null) {
            for (File collectorList : filesInCfg) {
                if (collectorList.isFile() && collectorList.getName().startsWith("linkcollector")) {
                    String counter = new Regex(collectorList.getName(), "linkcollector(\\d+)\\.zip").getMatch(0);
                    if (counter != null) sortedAvailable.add(Long.parseLong(counter));
                }
            }
            Collections.sort(sortedAvailable, Collections.reverseOrder());
        }
        for (Long loadOrder : sortedAvailable) {
            ret.add(Application.getResource("cfg/linkcollector" + loadOrder + ".zip"));
        }
        if (Application.getResource("cfg/linkcollector.zip").exists()) {
            ret.add(Application.getResource("cfg/linkcollector.zip"));
        }
        logger.info("Lists: " + ret);
        linkcollectorLists.addAll(ret);
        return ret;
    }

    /**
     * load all CrawledPackages/CrawledLinks from Database
     */
    public void initLinkCollector() {
        QUEUE.add(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.HIGH) {

            @Override
            protected Void run() throws RuntimeException {
                ArrayList<File> availableCollectorLists = findAvailableCollectorLists();
                if (JsonConfig.create(GeneralSettings.class).isSaveLinkgrabberListEnabled()) {
                    LinkedList<CrawledPackage> lpackages = null;
                    final HashMap<CrawledPackage, CrawledPackageStorable> restoreMap = new HashMap<CrawledPackage, CrawledPackageStorable>();
                    for (File collectorList : availableCollectorLists) {
                        try {
                            if (lpackages == null) {
                                restoreMap.clear();
                                lpackages = load(collectorList, restoreMap);
                            } else {
                                break;
                            }
                        } catch (final Throwable e) {
                            logger.log(e);
                        }
                    }
                    if (lpackages != null) {
                        int links = 0;
                        for (CrawledPackage cp : lpackages) {
                            links += cp.getChildren().size();
                        }
                        logger.info("CollectorList found: " + lpackages.size() + "/" + links);
                    } else {
                        logger.info("CollectorList empty!");
                        restoreMap.clear();
                        lpackages = new LinkedList<CrawledPackage>();
                    }
                    preProcessCrawledPackages(lpackages);
                    /* add loaded Packages to this controller */
                    try {
                        try {
                            writeLock();
                            for (final CrawledPackage filePackage : lpackages) {
                                for (CrawledLink link : filePackage.getChildren()) {
                                    if (link.getDownloadLink() != null) {
                                        /* set CrawledLink as changeListener to its DownloadLink */
                                        link.getDownloadLink().setNodeChangeListener(link);
                                    }
                                    /* keep maps up2date */
                                    dupeCheckMap.add(link.getLinkID());
                                    java.util.List<CrawledLink> list = getIdentifiedMap(link.getHost(), hosterMap);
                                    list.add(link);
                                }
                                filePackage.setControlledBy(LinkCollector.this);
                                CrawledPackageStorable storable = restoreMap.get(filePackage);
                                switch (filePackage.getType()) {
                                case NORMAL:
                                    if (storable.getPackageID() != null) packageMap.put(storable.getPackageID(), filePackage);
                                    break;
                                case VARIOUS:
                                    if (variousPackage == null) {
                                        variousPackage = filePackage;
                                    }
                                    for (CrawledLinkStorable link : storable.getLinks()) {
                                        String id = link.getID();
                                        if (id != null) {
                                            java.util.List<CrawledLink> list = getIdentifiedMap(id, variousMap);
                                            list.add(link._getCrawledLink());
                                        }
                                    }
                                    break;
                                case OFFLINE:
                                    if (offlinePackage == null) {
                                        offlinePackage = filePackage;
                                    }
                                    for (CrawledLinkStorable link : storable.getLinks()) {
                                        String id = link.getID();
                                        if (id != null) {
                                            java.util.List<CrawledLink> list = getIdentifiedMap(id, offlineMap);
                                            list.add(link._getCrawledLink());
                                        }
                                    }
                                    break;
                                case POFFLINE:
                                    if (permanentofflinePackage == null) {
                                        permanentofflinePackage = filePackage;
                                    }
                                    break;
                                }
                            }
                            packages.addAll(0, lpackages);
                        } finally {
                            writeUnlock();
                        }
                        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_STRUCTURE));
                    } finally {
                        CRAWLERLIST_LOADED.setReached();
                    }
                } else {
                    CRAWLERLIST_LOADED.setReached();
                }
                return null;
            }
        });
    }

    private void preProcessCrawledPackages(LinkedList<CrawledPackage> fps) {
        if (fps == null || fps.size() == 0) return;
        final Iterator<CrawledPackage> iterator = fps.iterator();
        DownloadLink dlLink = null;
        CrawledLink localLink = null;
        Iterator<CrawledLink> it;
        CrawledPackage fp;
        PluginFinder pluginFinder = new PluginFinder();
        while (iterator.hasNext()) {
            fp = iterator.next();
            if (fp.getChildren() != null) {
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
                    pluginFinder.assignPlugin(dlLink, true, logger);
                }
            }
            if (fp.getChildren() == null || fp.getChildren().size() == 0) {
                /* remove empty packages */
                iterator.remove();
                continue;
            }
        }
    }

    private LinkedList<CrawledPackage> load(File file, HashMap<CrawledPackage, CrawledPackageStorable> restoreMap) {
        LinkedList<CrawledPackage> ret = null;
        if (file != null && file.exists()) {
            ZipIOReader zip = null;
            try {
                zip = new ZipIOReader(file);
                /* lets restore the CrawledPackages from Json */
                HashMap<Integer, CrawledPackage> map = new HashMap<Integer, CrawledPackage>();
                InputStream is = null;
                LinkCollectorStorable lcs = null;
                for (ZipEntry entry : zip.getZipFiles()) {
                    String json = null;
                    try {
                        if (entry.getName().matches("^\\d+$")) {
                            int packageIndex = Integer.parseInt(entry.getName());
                            is = zip.getInputStream(entry);
                            byte[] bytes = IO.readStream((int) entry.getSize(), is);
                            json = new String(bytes, "UTF-8");
                            bytes = null;
                            CrawledPackageStorable storable = JSonStorage.restoreFromString(json, new TypeRef<CrawledPackageStorable>() {
                            }, null);
                            if (storable != null) {
                                map.put(packageIndex, storable._getCrawledPackage());
                                if (restoreMap != null) restoreMap.put(storable._getCrawledPackage(), storable);
                            } else {
                                throw new WTFException("restored a null CrawledPackageStorable");
                            }
                        } else if ("extraInfo".equalsIgnoreCase(entry.getName())) {
                            is = zip.getInputStream(entry);
                            byte[] bytes = IO.readStream((int) entry.getSize(), is);
                            json = new String(bytes, "UTF-8");
                            bytes = null;
                            lcs = JSonStorage.stringToObject(json, new TypeRef<LinkCollectorStorable>() {
                            }, null);
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                        logger.info("String was: " + json);
                        logger.info("Entry was: " + entry);
                        throw e;
                    } finally {
                        try {
                            is.close();
                        } catch (final Throwable e) {
                        }
                    }
                }
                /* sort positions */
                java.util.List<Integer> positions = new ArrayList<Integer>(map.keySet());
                Collections.sort(positions);
                /* build final ArrayList of CrawledPackage */
                java.util.List<CrawledPackage> ret2 = new ArrayList<CrawledPackage>(positions.size());
                for (Integer position : positions) {
                    ret2.add(map.get(position));
                }
                if (lcs != null && JsonConfig.create(GeneralSettings.class).isConvertRelativePathesJDRoot()) {
                    try {
                        String oldRootPath = lcs.getRootPath();
                        if (!StringUtils.isEmpty(oldRootPath)) {
                            String newRoot = JDUtilities.getJDHomeDirectoryFromEnvironment().toString();
                            /*
                             * convert pathes relative to JDownloader root,only in jared version
                             */
                            for (CrawledPackage pkg : ret2) {
                                if (!CrossSystem.isAbsolutePath(pkg.getDownloadFolder())) {
                                    /* no need to convert relative pathes */
                                    continue;
                                }
                                String pkgPath = LinkTreeUtils.getDownloadDirectory(pkg).toString();
                                if (pkgPath.startsWith(oldRootPath)) {
                                    /*
                                     * folder is inside JDRoot, lets update it
                                     */
                                    String restPath = pkgPath.substring(oldRootPath.length());
                                    String newPath = new File(newRoot, restPath).toString();
                                    pkg.setDownloadFolder(newPath);
                                }
                            }
                        }
                    } catch (final Throwable e) {
                        /* this method can throw exceptions, eg in SVN */
                        logger.log(e);
                    }
                }
                map = null;
                positions = null;
                ret = new LinkedList<CrawledPackage>(ret2);
            } catch (final Throwable e) {
                try {
                    zip.close();
                } catch (final Throwable e2) {
                }
                File renameTo = new File(file.getAbsolutePath() + ".broken");
                boolean backup = false;
                try {
                    if (file.renameTo(renameTo) == false) {
                        IO.copyFile(file, renameTo);
                    }
                    backup = true;
                } catch (final Throwable e2) {
                }
                logger.log(e);
                logger.severe("Could backup " + file + " to " + renameTo + " ->" + backup);
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
    private void save(final java.util.List<CrawledPackage> packages, final File destFile) {
        QUEUE.add(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.HIGH) {

            @Override
            protected Void run() throws RuntimeException {
                File file = destFile;
                if (file == null) {
                    if (linkcollectorLists.size() > 0) {
                        String counter = new Regex(linkcollectorLists.get(0).getName(), "linkcollector(\\d+)\\.zip").getMatch(0);
                        long count = 1;
                        if (counter != null) {
                            count = Long.parseLong(counter) + 1;
                        }
                        file = Application.getResource("cfg/linkcollector" + count + ".zip");
                    }
                    if (file == null) file = Application.getResource("cfg/linkcollector.zip");
                }
                boolean deleteFile = true;
                ZipIOWriter zip = null;
                FileOutputStream fos = null;
                if (packages != null && file != null) {
                    try {
                        if (file.exists()) {
                            if (file.isDirectory()) throw new IOException("File " + file + " is a directory");
                            if (FileCreationManager.getInstance().delete(file) == false) throw new IOException("Could not delete file " + file);
                        } else {
                            if (file.getParentFile().exists() == false && FileCreationManager.getInstance().mkdir(file.getParentFile()) == false) throw new IOException("Could not create parentFolder for file " + file);
                        }
                        int index = 0;
                        /* prepare formatter for package filenames in zipfiles */
                        String format = "%02d";
                        if (packages.size() >= 10) {
                            format = String.format("%%0%dd", (int) Math.log10(packages.size()) + 1);
                        }
                        fos = new FileOutputStream(file);
                        zip = new ZipIOWriter(new BufferedOutputStream(fos, 32767));
                        for (CrawledPackage pkg : packages) {
                            /* convert FilePackage to JSon */
                            CrawledPackageStorable storable = new CrawledPackageStorable(pkg);
                            /* save packageID */
                            storable.setPackageID(LinkCollector.this.getPackageMapID(pkg));
                            if (!CrawledPackageStorable.TYPE.NORMAL.equals(storable.getType())) {
                                if (CrawledPackageStorable.TYPE.VARIOUS.equals(storable.getType())) {
                                    /* save ID for variousMap */
                                    for (CrawledLinkStorable link : storable.getLinks()) {
                                        CrawledLink cLink = link._getCrawledLink();
                                        link.setID(getIDFromMap(variousMap, cLink));
                                    }
                                } else if (CrawledPackageStorable.TYPE.OFFLINE.equals(storable.getType())) {
                                    /* save ID for variousMap */
                                    for (CrawledLinkStorable link : storable.getLinks()) {
                                        CrawledLink cLink = link._getCrawledLink();
                                        link.setID(getIDFromMap(offlineMap, cLink));
                                    }
                                }
                            }
                            String string = JSonStorage.serializeToJson(storable);
                            storable = null;
                            byte[] bytes = string.getBytes("UTF-8");
                            string = null;
                            zip.addByteArry(bytes, true, "", String.format(format, (index++)));
                        }
                        LinkCollectorStorable lcs = new LinkCollectorStorable();
                        try {
                            /*
                             * set current RootPath of JDownloader, so we can update it when user moves JDownloader folder
                             */
                            lcs.setRootPath(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath());
                        } catch (final Throwable e) {
                            /* the method above can throw exceptions, eg in SVN */
                            logger.log(e);
                        }
                        zip.addByteArry(JSonStorage.serializeToJson(lcs).getBytes("UTF-8"), true, "", "extraInfo");
                        /* close ZipIOWriter */
                        zip.close();
                        deleteFile = false;
                        linkcollectorLists.add(0, file);
                        try {
                            int keepXOld = Math.max(JsonConfig.create(GeneralSettings.class).getKeepXOldLists(), 0);
                            if (linkcollectorLists.size() > keepXOld) {
                                for (int removeIndex = linkcollectorLists.size() - 1; removeIndex > keepXOld; removeIndex--) {
                                    File remove = linkcollectorLists.remove(removeIndex);
                                    if (remove != null) {
                                        logger.info("Delete outdated CollectorList: " + remove + " " + FileCreationManager.getInstance().delete(remove));
                                    }
                                }
                            }
                        } catch (final Throwable e) {
                            logger.log(e);
                        }
                        return null;
                    } catch (final Throwable e) {
                        logger.log(e);
                    } finally {
                        try {
                            fos.close();
                        } catch (final Throwable e) {
                        }
                        if (deleteFile && file.exists()) {
                            FileCreationManager.getInstance().delete(file);
                        }
                    }
                }
                return null;
            }
        });
    }

    private boolean allowSaving() {
        return CRAWLERLIST_LOADED.isReached() && JsonConfig.create(GeneralSettings.class).isSaveLinkgrabberListEnabled();
    }

    /**
     * save the current CrawledPackages/CrawledLinks controlled by this LinkCollector
     */
    public void saveLinkCollectorLinks() {
        if (!allowSaving()) return;
        /* save as new Json ZipFile */
        try {
            save(getPackagesCopy(), null);
        } catch (final Throwable e) {
            logger.log(e);
        }
    }

    public void setArchiver(ExtractionExtension archiver) {
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

    public void setPackagizer(PackagizerInterface packagizerInterface) {
        this.packagizer = packagizerInterface;
    }

    @Override
    public void handleBrokenLink(CrawledLink link) {
    }

    @Override
    public void handleUnHandledLink(CrawledLink link) {
    }

    @Override
    public void nodeUpdated(AbstractNode source, jd.controlling.packagecontroller.AbstractNodeNotifier.NOTIFY notify, Object param) {
        super.nodeUpdated(source, notify, param);
        switch (notify) {
        case PROPERTY_CHANCE:
            if (param instanceof CrawledLinkProperty) {
                CrawledLinkProperty eventPropery = (CrawledLinkProperty) param;
                switch (eventPropery.getProperty()) {
                case NAME:
                case ENABLED:
                case AVAILABILITY:
                case PRIORITY:
                    eventPropery.getCrawledLink().getParentNode().getView().requestUpdate();
                    break;
                }
            }
            eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_DATA, new Object[] { source, param }, QueuePriority.LOW));
            break;
        case STRUCTURE_CHANCE:
            eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_STRUCTURE, new Object[] { source, param }, QueuePriority.LOW));
            break;
        }
    }

    @Override
    protected void _controllerPackageNodeStructureChanged(CrawledPackage pkg, QueuePriority priority) {
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_STRUCTURE, pkg, priority));
    }

    private Object shutdownLock = new Object();

    @Override
    public void onShutdownVetoRequest(ShutdownRequest request) throws ShutdownVetoException {
        if (request.hasVetos()) { return; }

        if (request.isSilent()) {
            synchronized (shutdownLock) {
                if (LinkChecker.isChecking() || LinkCrawler.isCrawling()) { throw new ShutdownVetoException("LinkCollector is still running", this); }

            }
        } else {
            synchronized (shutdownLock) {

                if (LinkChecker.isChecking() || LinkCrawler.isCrawling()) {
                    if (UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _JDT._.LinkCollector_onShutdownRequest_(), _JDT._.LinkCollector_onShutdownRequest_msg(), NewTheme.I().getIcon("linkgrabber", 32), _JDT._.literally_yes(), null)) {

                    return; }
                    throw new ShutdownVetoException("LinkCollector is still running", this);
                }
            }
        }
    }

    @Override
    public long getShutdownVetoPriority() {
        return 0;
    }

    @Override
    public void onShutdown(ShutdownRequest request) {
    }

    @Override
    public void onShutdownVeto(ShutdownRequest request) {
    }

    @Override
    public boolean hasNotificationListener() {
        return true;
    }

    public void moveLinksToDownloadList(SelectionInfo<CrawledPackage, CrawledLink> selection) {

        java.util.List<FilePackage> filePackagesToAdd = new ArrayList<FilePackage>();

        boolean autostart = false;
        List<DownloadLink> force = new ArrayList<DownloadLink>();

        for (CrawledPackage cp : selection.getAllPackages()) {
            List<CrawledLink> links;
            //
            links = selection.getSelectedLinksByPackage(cp);
            java.util.List<FilePackage> convertedLinks = LinkCollector.getInstance().convert(links, true);
            for (CrawledLink cl : links) {
                autostart |= cl.isAutoStartEnabled();
                if (cl.isForcedAutoStartEnabled()) {
                    force.add(cl.getDownloadLink());
                }
            }

            if (convertedLinks != null) {
                filePackagesToAdd.addAll(convertedLinks);
            }

        }

        /* convert all selected CrawledLinks to FilePackages */
        boolean addTop = org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_ADD_AT_TOP.getValue();

        /* add the converted FilePackages to DownloadController */
        /**
         * addTop = 0, to insert the packages at the top
         * 
         * addBottom = negative number -> add at the end
         */
        DownloadController.getInstance().addAllAt(filePackagesToAdd, addTop ? 0 : -(filePackagesToAdd.size() + 10));
        if (autostart) {
            DownloadWatchDog.getInstance().startDownloads();
        }
        if (force.size() > 0) {
            DownloadWatchDog.getInstance().forceDownload(force);
        }

    }

}
