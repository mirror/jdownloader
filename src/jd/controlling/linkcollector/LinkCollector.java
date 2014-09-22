package jd.controlling.linkcollector;

import java.awt.Toolkit;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;

import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcollector.autostart.AutoStartManager;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkProperty;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerFilter;
import jd.controlling.linkcrawler.LinkCrawlerHandler;
import jd.controlling.linkcrawler.PackageInfo;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.PackageController;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
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
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.zip.ZipIOReader;
import org.appwork.utils.zip.ZipIOWriter;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.linkcrawler.GenericVariants;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkgrabberSearchField;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MenuManagerLinkgrabberTableContext;
import org.jdownloader.images.NewTheme;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.plugins.controller.host.PluginFinder;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;
import org.jdownloader.translate._JDT;

public class LinkCollector extends PackageController<CrawledPackage, CrawledLink> implements LinkCheckerHandler<CrawledLink>, LinkCrawlerHandler, ShutdownVetoListener {

    public static final class JobLinkCrawler extends LinkCollectorCrawler {
        private final LinkCollectingJob         job;
        private final LinkCollectingInformation collectingInfo;
        private final LinkCollector             linkCollector;

        public JobLinkCrawler(final LinkCollector linkCollector, final LinkCollectingJob job) {
            this.job = job;
            collectingInfo = new LinkCollectingInformation(this, linkCollector.getLinkChecker(), linkCollector);
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
                    link.setCollectingInfo(collectingInfo);
                    link.setSourceJob(job);
                    linkCollector.handleFinalLink(link);
                    defaultHandler.handleFinalLink(link);
                }

                @Override
                public void handleFilteredLink(CrawledLink link) {
                    link.setCollectingInfo(collectingInfo);
                    link.setSourceJob(job);
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
            final CrawledLink ret = new CrawledLink(url);
            if (job != null) {
                ret.setOrigin(job.getOrigin());
            }
            return ret;
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

    private static class CrawledPackageMappingID {
        private final String id;

        private String getId() {
            return id;
        }

        private String getPackageName() {
            return packageName;
        }

        private String getDownloadFolder() {
            return downloadFolder;
        }

        private final String packageName;
        private final String downloadFolder;
        private final String combined;

        public String getCombined() {
            return combined;
        }

        private static CrawledPackageMappingID get(String combined) {
            if (combined != null) {
                String[] infos = new Regex(combined, "^(.*?)\\|_\\|(.*?)\\|_\\|(.*?)$").getRow(0);
                if (infos == null) {
                    // compatibility
                    infos = new Regex(combined, "^(.*?)_\\|\\|_(.*?)_\\|\\|_(.*?)$").getRow(0);
                }
                if (infos != null && infos.length == 3) {
                    if ("Null".equalsIgnoreCase(infos[0])) {
                        infos[0] = null;
                    }
                    if ("Null".equalsIgnoreCase(infos[1])) {
                        infos[1] = null;
                    }
                    if ("Null".equalsIgnoreCase(infos[2])) {
                        infos[2] = null;
                    }
                    if (infos[0] != null || infos[1] != null || infos[2] != null) {
                        return new CrawledPackageMappingID(infos[0], infos[1], infos[2]);
                    }
                }
            }
            return null;
        }

        private CrawledPackageMappingID(String id, String packageName, String downloadFolder) {
            this.id = id;
            this.packageName = packageName;
            this.downloadFolder = downloadFolder;
            combined = id + "|_|" + packageName + "|_|" + downloadFolder;
        }

        @Override
        public int hashCode() {
            return combined.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj != null && obj instanceof CrawledPackageMappingID) {
                return StringUtils.equals(combined, ((CrawledPackageMappingID) obj).combined);
            }
            return false;
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

    private transient LinkCollectorEventSender                                  eventsender        = new LinkCollectorEventSender();
    public final ScheduledExecutorService                                       TIMINGQUEUE        = DelayedRunnable.getNewScheduledExecutorService();
    public static SingleReachableState                                          CRAWLERLIST_LOADED = new SingleReachableState("CRAWLERLIST_COMPLETE");
    private static LinkCollector                                                INSTANCE           = new LinkCollector();

    private volatile LinkChecker<CrawledLink>                                   linkChecker        = null;
    /**
     * NOTE: only access these fields inside the IOEQ
     */
    private final HashMap<String, WeakReference<CrawledLink>>                   dupeCheckMap       = new HashMap<String, WeakReference<CrawledLink>>();
    private HashMap<CrawledPackageMappingID, CrawledPackage>                    packageMap         = new HashMap<CrawledPackageMappingID, CrawledPackage>();

    /* sync on filteredStuff itself when accessing it */
    private java.util.List<CrawledLink>                                         filteredStuff      = new ArrayList<CrawledLink>();

    private LinkCrawlerFilter                                                   crawlerFilter      = null;

    private ExtractionExtension                                                 archiver;
    private DelayedRunnable                                                     asyncSaving        = null;

    private PackagizerInterface                                                 packagizer         = null;

    protected CrawledPackage                                                    offlinePackage;

    protected CrawledPackage                                                    variousPackage;

    protected CrawledPackage                                                    permanentofflinePackage;

    private final CopyOnWriteArrayList<File>                                    linkcollectorLists = new CopyOnWriteArrayList<File>();

    private final HashMap<CrawledPackageMappingID, java.util.List<CrawledLink>> offlineMap         = new HashMap<CrawledPackageMappingID, java.util.List<CrawledLink>>();
    private final HashMap<CrawledPackageMappingID, java.util.List<CrawledLink>> variousMap         = new HashMap<CrawledPackageMappingID, java.util.List<CrawledLink>>();
    private final HashMap<CrawledPackageMappingID, java.util.List<CrawledLink>> badMappingMap      = new HashMap<CrawledPackageMappingID, java.util.List<CrawledLink>>();
    private final WeakHashMap<CrawledPackage, HashMap<Object, Object>>          autoRenameCache    = new WeakHashMap<CrawledPackage, HashMap<Object, Object>>();

    private DelayedRunnable                                                     asyncCacheCleanup;
    private final AtomicLong                                                    collectingID       = new AtomicLong(0);

    public long getCollectingID() {
        return collectingID.get();
    }

    private final AtomicLong       collectingRequested  = new AtomicLong(0);
    private final AtomicLong       collectingProcessed  = new AtomicLong(0);

    private boolean                restoreButtonEnabled = false;

    private final AutoStartManager autoStartManager;

    private LinkCollector() {
        autoStartManager = new AutoStartManager();
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

    public AutoStartManager getAutoStartManager() {
        return autoStartManager;
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
        autoRenameCache.remove(pkg);
        final CrawledPackageMappingID id = getPackageMapID(pkg);
        if (id != null) {
            packageMap.remove(id);
        }
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REMOVE_CONTENT, pkg, priority));
        cleanupMaps(getChildrenCopy(pkg));
    }

    /**
     * NOTE: only access the IOEQ
     */
    private CrawledPackageMappingID getPackageMapID(CrawledPackage pkg) {
        for (Iterator<Entry<CrawledPackageMappingID, CrawledPackage>> iterator = packageMap.entrySet().iterator(); iterator.hasNext();) {
            final Entry<CrawledPackageMappingID, CrawledPackage> type = iterator.next();
            if (type.getValue() == pkg) {
                return type.getKey();
            }
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
        collectingID.incrementAndGet();
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.ABORT));
        final LinkChecker<CrawledLink> llinkChecker = linkChecker;
        if (llinkChecker != null) {
            llinkChecker.stopChecking();
        }
    }

    public boolean isCollecting() {
        final LinkChecker<CrawledLink> llinkChecker = linkChecker;
        return llinkChecker != null && llinkChecker.isRunning() || (collectingRequested.get() != collectingProcessed.get());
    }

    protected void autoFileNameCorrection(List<CrawledLink> pkgchildren, CrawledPackage pkg) {
        // long t = System.currentTimeMillis();
        // if we have only one single hoster, we can hardly learn anything
        if (CFG_LINKGRABBER.AUTO_FILENAME_CORRECTION_ENABLED.isEnabled()) {
            final HashSet<LazyHostPlugin> hosts = new HashSet<LazyHostPlugin>();
            final ArrayList<DownloadLink> dlinks = new ArrayList<DownloadLink>();
            final ArrayList<DownloadLink> maybebadfilenames = new ArrayList<DownloadLink>();
            final ArrayList<CrawledLink> processLinks = new ArrayList<CrawledLink>();
            HashMap<Object, Object> cache = autoRenameCache.get(pkg);
            HashSet<String> nameCache = null;
            if (cache != null) {
                nameCache = (HashSet<String>) cache.get("nameCache");
            }
            if (nameCache == null) {
                nameCache = new HashSet<String>();
                if (cache != null) {
                    cache.put("nameCache", nameCache);
                }
            }
            boolean newNames = false;
            for (final CrawledLink link : pkgchildren) {
                final String name = link.getDownloadLink().getNameSetbyPlugin();
                if (AvailableLinkState.ONLINE.equals(link.getLinkState()) && name != null) {
                    hosts.add(link.gethPlugin().getLazyP());
                    if (!link.gethPlugin().isHosterManipulatesFilenames()) {
                        nameCache.add(name);
                        dlinks.add(link.getDownloadLink());
                    } else {
                        processLinks.add(link);
                        maybebadfilenames.add(link.getDownloadLink());
                    }
                }
            }
            if (hosts.size() > 1 && processLinks.size() > 0) {
                for (CrawledLink link : processLinks) {
                    final String name = link.getDownloadLink().getNameSetbyPlugin();
                    if (newNames == false && nameCache != null && !nameCache.contains(name)) {
                        newNames = true;
                        break;
                    }
                }
                if (newNames) {
                    if (cache == null) {
                        cache = new HashMap<Object, Object>();
                        autoRenameCache.put(pkg, cache);
                    }
                    cache.put("nameCache", nameCache);
                    for (CrawledLink link : processLinks) {
                        String name = link.getDownloadLink().getNameSetbyPlugin();
                        if (name != null) {
                            nameCache.add(name);
                            String newName = link.gethPlugin().autoFilenameCorrection(cache, name, link.getDownloadLink(), dlinks);
                            if (newName == null) {
                                newName = link.gethPlugin().autoFilenameCorrection(cache, name, link.getDownloadLink(), maybebadfilenames);
                            }
                            if (newName != null && !name.equals(newName)) {
                                logger.info("Renamed file " + name + " to " + newName);
                                /*
                                 * we do not force a filename if newName equals to name set by plugin!
                                 */
                                link.getDownloadLink().setForcedFileName(newName);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void moveOrAddAt(CrawledPackage pkg, final List<CrawledLink> movechildren, int moveChildrenindex, int pkgIndex) {
        QUEUE.add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                for (CrawledLink l : movechildren) {
                    putCrawledLinkByLinkID(l.getLinkID(), l);
                }
                return null;
            }

        });
        super.moveOrAddAt(pkg, movechildren, moveChildrenindex, pkgIndex);
    }

    private List<CrawledLink> getIdentifiedMap(final CrawledPackageMappingID crawledPackageMappingID, HashMap<CrawledPackageMappingID, List<CrawledLink>> map) {
        List<CrawledLink> list = map.get(crawledPackageMappingID);
        if (list == null) {
            list = new ArrayList<CrawledLink>();
            if (crawledPackageMappingID != null) {
                map.put(crawledPackageMappingID, list);
            }
        }
        return list;
    }

    public void addCrawledLink(final CrawledLink link) {
        collectingRequested.incrementAndGet();
        /* try to find good matching package or create new one */
        QUEUE.add(new QueueAction<Void, RuntimeException>() {

            private void newPackage(final java.util.List<CrawledLink> links, String newPackageName, final String downloadFolder, final CrawledPackageMappingID crawledPackageMappingID) {
                final CrawledPackage pkg = new CrawledPackage();
                pkg.setExpanded(JsonConfig.create(LinkCollectorConfig.class).isPackageAutoExpanded());
                pkg.setName(newPackageName);
                if (downloadFolder != null) {
                    pkg.setDownloadFolder(downloadFolder);
                }
                packageMap.put(crawledPackageMappingID, pkg);
                if (links != null && links.size() > 0) {
                    LinkCollector.this.moveOrAddAt(pkg, links, -1);
                    putBadMappings(newPackageName, crawledPackageMappingID, links);
                }
                // check of we have matching links in offline maper
                List<CrawledLink> list = offlineMap.remove(crawledPackageMappingID);
                if (list != null && list.size() > 0) {
                    LinkCollector.this.moveOrAddAt(pkg, list, -1);
                }
                list = variousMap.remove(crawledPackageMappingID);
                if (list != null && list.size() > 0) {
                    LinkCollector.this.moveOrAddAt(pkg, list, -1);
                }
                if (StringUtils.equals(newPackageName, crawledPackageMappingID.getPackageName())) {
                    list = getBadMappings(crawledPackageMappingID, pkg);
                    if (list != null && list.size() > 0) {
                        LinkCollector.this.moveOrAddAt(pkg, list, -1);
                    }
                }
            }

            private void putBadMappings(String newPackageName, CrawledPackageMappingID crawledPackageMappingID, List<CrawledLink> links) {
                if (!StringUtils.equals(newPackageName, crawledPackageMappingID.getPackageName())) {
                    final CrawledPackageMappingID badID = new CrawledPackageMappingID(crawledPackageMappingID.getId(), null, crawledPackageMappingID.getDownloadFolder());
                    List<CrawledLink> badMappings = badMappingMap.get(badID);
                    if (links != null) {
                        for (CrawledLink link : links) {
                            if (link.getDownloadLink().hasBrowserUrl()) {
                                if (badMappings == null) {
                                    badMappings = new ArrayList<CrawledLink>();
                                    badMappingMap.put(badID, badMappings);
                                }
                                badMappings.add(link);
                            }
                        }
                    }
                }
            }

            private List<CrawledLink> getBadMappings(CrawledPackageMappingID crawledPackageMappingID, CrawledPackage pkg) {
                final List<CrawledLink> ret = new ArrayList<CrawledLink>();
                final CrawledPackageMappingID badID = new CrawledPackageMappingID(crawledPackageMappingID.getId(), null, crawledPackageMappingID.getDownloadFolder());
                List<CrawledLink> badMappings = badMappingMap.get(badID);
                if (badMappings != null) {
                    final HashMap<String, Boolean> checked = new HashMap<String, Boolean>();
                    final boolean readL = pkg.getModifyLock().readLock();
                    try {
                        for (CrawledLink link : badMappings) {
                            final String browserURL = link.getDownloadLink().getBrowserUrl();
                            Boolean check = checked.get(browserURL);
                            if (check == null) {
                                for (final CrawledLink cLink : pkg.getChildren()) {
                                    final DownloadLink dlLink = cLink.getDownloadLink();
                                    if (StringUtils.equals(browserURL, dlLink.getBrowserUrl())) {
                                        check = Boolean.TRUE;
                                        checked.put(browserURL, check);
                                        break;
                                    }
                                }
                                if (check == null) {
                                    check = Boolean.FALSE;
                                    checked.put(browserURL, check);
                                }
                            }
                            if (Boolean.TRUE.equals(check)) {
                                ret.add(link);
                            }
                        }
                    } finally {
                        pkg.getModifyLock().readUnlock(readL);
                    }
                    badMappings.removeAll(ret);
                    if (badMappings.size() == 0) {
                        badMappingMap.remove(badID);
                    }
                }
                return ret;
            }

            private CrawledPackage getCrawledPackage(CrawledPackageMappingID crawledPackageMappingID, CrawledLink mappingLink) {
                CrawledPackage ret = packageMap.get(crawledPackageMappingID);
                if (ret == null && crawledPackageMappingID.getPackageName() == null && mappingLink.getDownloadLink().hasBrowserUrl()) {
                    final HashMap<Integer, HashMap<CrawledPackageMappingID, CrawledPackage>> bestMappings = new HashMap<Integer, HashMap<CrawledPackageMappingID, CrawledPackage>>();
                    for (final Entry<CrawledPackageMappingID, CrawledPackage> chance : packageMap.entrySet()) {
                        int equals = 0;
                        if (StringUtils.equals(crawledPackageMappingID.getId(), chance.getKey().getId())) {
                            equals++;
                        }
                        if (StringUtils.equals(crawledPackageMappingID.getDownloadFolder(), chance.getKey().getDownloadFolder())) {
                            equals++;
                        }
                        if (equals > 0) {
                            HashMap<CrawledPackageMappingID, CrawledPackage> mappings = bestMappings.get(new Integer(equals));
                            if (mappings == null) {
                                mappings = new HashMap<CrawledPackageMappingID, CrawledPackage>();
                                bestMappings.put(new Integer(equals), mappings);
                            }
                            mappings.put(chance.getKey(), chance.getValue());
                        }
                    }
                    final String browserURL = mappingLink.getDownloadLink().getBrowserUrl();
                    for (int x = 2; x > 0; x--) {
                        HashMap<CrawledPackageMappingID, CrawledPackage> mappings = bestMappings.get(new Integer(x));
                        if (mappings != null) {
                            for (final Entry<CrawledPackageMappingID, CrawledPackage> mapping : mappings.entrySet()) {
                                final CrawledPackage pkg = mapping.getValue();
                                final boolean readL = pkg.getModifyLock().readLock();
                                try {
                                    for (final CrawledLink cLink : pkg.getChildren()) {
                                        final DownloadLink dlLink = cLink.getDownloadLink();
                                        if (dlLink != null && dlLink.hasBrowserUrl() && StringUtils.equals(dlLink.getBrowserUrl(), browserURL)) {
                                            final CrawledPackageMappingID id = mapping.getKey();
                                            if (id.getPackageName() != null) {
                                                return pkg;
                                            } else if (ret != null) {
                                                ret = pkg;
                                            }
                                        }
                                    }
                                } finally {
                                    pkg.getModifyLock().readUnlock(readL);
                                }
                            }
                        }
                    }
                }
                return ret;
            }

            @Override
            protected Void run() throws RuntimeException {
                try {
                    final String linkID = link.getLinkID();
                    final CrawledLink existingLink = getCrawledLinkByLinkID(linkID);
                    if (existingLink != null && existingLink != link) {
                        /* clear references */
                        link.setCollectingInfo(null);
                        link.setSourceJob(null);
                        link.setMatchingFilter(null);
                        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.DUPE_LINK, link, QueuePriority.NORM));
                        return null;
                    }
                    final LinkCollectingInformation info = link.getCollectingInfo();
                    if (info == null || info.getCollectingID() == getCollectingID()) {
                        putCrawledLinkByLinkID(linkID, link);
                        LinkCollectingJob job = link.getSourceJob();
                        if (job != null) {
                            try {
                                if (job.getCrawledLinkModifier() != null) {
                                    job.getCrawledLinkModifier().modifyCrawledLink(link);
                                }
                            } catch (final Throwable e) {
                                logger.log(e);
                            }
                            if (link.getDownloadLink() != null && StringUtils.isNotEmpty(job.getCustomSourceUrl())) {
                                link.getDownloadLink().setReferrerUrl(job.getCustomSourceUrl());
                            }
                        }
                        if (link.getDownloadLink() != null) {
                            /* set CrawledLink as changeListener to its DownloadLink */
                            link.getDownloadLink().setNodeChangeListener(link);
                        }
                        PackageInfo dpi = link.getDesiredPackageInfo();
                        UniqueAlltimeID uID = null;

                        String crawledPackageName = null;
                        String crawledPackageID = null;
                        String downloadFolder = null;
                        boolean ignoreSpecialPackages = dpi != null && (dpi.isPackagizerRuleMatched() || dpi.isIgnoreVarious());
                        if (dpi != null) {
                            crawledPackageName = dpi.getName();
                            downloadFolder = dpi.getDestinationFolder();
                            if (downloadFolder != null) {
                                downloadFolder = downloadFolder.replaceFirst("/$", "");
                                String defaultFolder = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
                                if (defaultFolder != null) {
                                    defaultFolder = defaultFolder.replaceFirst("/$", "");
                                }
                                if (!downloadFolder.equals(defaultFolder)) {
                                    /* we have a custom downloadFolder, so let's not use various package */
                                    ignoreSpecialPackages = true;
                                }
                            }
                            if ((uID = dpi.getUniqueId()) != null) {
                                crawledPackageID = dpi.getUniqueId().toString();
                                if (ignoreSpecialPackages && LinkCrawler.PERMANENT_OFFLINE_ID == uID) {
                                    crawledPackageID = null;
                                }
                            }
                        }
                        if (crawledPackageName == null) {
                            final DownloadLink dlLink = link.getDownloadLink();
                            if (link.isNameSet() || dlLink.isNameSet()) {
                                crawledPackageName = link.getName();
                            } else {
                                final String name = link.getName();
                                final String extension = Files.getExtension(name);
                                if (extension != null) {
                                    crawledPackageName = name;
                                }
                            }
                            if (crawledPackageName != null) {
                                crawledPackageName = LinknameCleaner.cleanFileName(crawledPackageName, false, false, LinknameCleaner.EXTENSION_SETTINGS.REMOVE_ALL, true);
                            }
                        }
                        if (crawledPackageName == null) {
                            final CrawledLinkFactory clf = new CrawledLinkFactory(link);
                            final ExtractionExtension lArchiver = archiver;
                            if (lArchiver != null && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.ARCHIVE_PACKAGIZER_ENABLED.getValue()) {
                                if (lArchiver.isMultiPartArchive(clf)) {
                                    if (crawledPackageID == null) {
                                        crawledPackageID = lArchiver.createArchiveID(clf);
                                    }
                                    if (crawledPackageName == null) {
                                        crawledPackageName = _JDT._.LinkCollector_archiv(LinknameCleaner.cleanFileName(lArchiver.getArchiveName(clf), false, true, LinknameCleaner.EXTENSION_SETTINGS.REMOVE_KNOWN, true));
                                    }
                                }
                            }
                        }
                        if (CrossSystem.isWindows()) {
                            if (downloadFolder != null) {
                                downloadFolder = downloadFolder.toLowerCase(Locale.ENGLISH);
                            }
                        }
                        final CrawledPackageMappingID crawledPackageMapID = new CrawledPackageMappingID(crawledPackageID, crawledPackageName, downloadFolder);

                        String newPackageName = crawledPackageName;
                        if (newPackageName == null) {
                            newPackageName = link.getName();
                            if (newPackageName != null) {
                                newPackageName = LinknameCleaner.cleanFileName(newPackageName, false, true, LinknameCleaner.EXTENSION_SETTINGS.REMOVE_ALL, true);
                            }
                            if (StringUtils.isEmpty(newPackageName)) {
                                newPackageName = _JDT._.LinkCollector_addCrawledLink_offlinepackage();
                            }
                        }
                        final CrawledPackage pkg = getCrawledPackage(crawledPackageMapID, link);
                        if (pkg == null) {
                            if (!ignoreSpecialPackages && LinkCrawler.PERMANENT_OFFLINE_ID == uID) {
                                /* these links will never come back online */
                                List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                                add.add(link);
                                LinkCollector.this.moveOrAddAt(getPermanentOfflineCrawledPackage(), add, -1);
                            } else if (!ignoreSpecialPackages && link.getLinkState() == AvailableLinkState.OFFLINE && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.OFFLINE_PACKAGE_ENABLED.getValue()) {
                                java.util.List<CrawledLink> list = getIdentifiedMap(crawledPackageMapID, offlineMap);
                                list.add(link);
                                List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                                add.add(link);
                                LinkCollector.this.moveOrAddAt(getOfflineCrawledPackage(), add, -1);
                            } else if (!ignoreSpecialPackages && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.VARIOUS_PACKAGE_LIMIT.getValue() > 0 && CFG_LINKGRABBER.VARIOUS_PACKAGE_ENABLED.isEnabled()) {
                                java.util.List<CrawledLink> list = getIdentifiedMap(crawledPackageMapID, variousMap);
                                list.add(link);
                                if (list.size() > org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.VARIOUS_PACKAGE_LIMIT.getValue()) {
                                    newPackage(null, newPackageName, downloadFolder, crawledPackageMapID);
                                } else {
                                    List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                                    add.add(link);
                                    LinkCollector.this.moveOrAddAt(getVariousCrawledPackage(), add, -1);
                                }
                            } else {
                                java.util.List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                                add.add(link);
                                newPackage(add, newPackageName, downloadFolder, crawledPackageMapID);
                            }
                        } else {
                            List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                            add.add(link);
                            LinkCollector.this.moveOrAddAt(pkg, add, -1);
                        }
                        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.ADDED_LINK, link, QueuePriority.NORM));
                        autoStartManager.onLinkAdded(link);
                    }
                    return null;
                } catch (Throwable e) {
                    removeCrawledLinkByLinkID(link);
                    logger.log(e);
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    throw new RuntimeException(e);
                } finally {
                    /* clear references */
                    link.setCollectingInfo(null);
                    link.setSourceJob(null);
                    link.setMatchingFilter(null);
                    collectingProcessed.incrementAndGet();
                }
            }
        });
    }

    private CrawledPackage getPermanentOfflineCrawledPackage() {
        CrawledPackage lpermanentofflinePackage = permanentofflinePackage;
        if (lpermanentofflinePackage != null && TYPE.POFFLINE == lpermanentofflinePackage.getType()) {
            return lpermanentofflinePackage;
        }
        lpermanentofflinePackage = new CrawledPackage();
        lpermanentofflinePackage.setExpanded(true);
        lpermanentofflinePackage.setName(_GUI._.Permanently_Offline_Package());
        lpermanentofflinePackage.setType(TYPE.POFFLINE);
        permanentofflinePackage = lpermanentofflinePackage;
        return lpermanentofflinePackage;
    }

    private CrawledPackage getVariousCrawledPackage() {
        CrawledPackage lvariousPackage = variousPackage;
        if (lvariousPackage != null && TYPE.VARIOUS == lvariousPackage.getType()) {
            return lvariousPackage;
        }
        lvariousPackage = new CrawledPackage();
        lvariousPackage.setExpanded(true);
        lvariousPackage.setName(_JDT._.LinkCollector_addCrawledLink_variouspackage());
        lvariousPackage.setType(TYPE.VARIOUS);
        variousPackage = lvariousPackage;
        return lvariousPackage;
    }

    private CrawledPackage getOfflineCrawledPackage() {
        CrawledPackage lofflinePackage = offlinePackage;
        if (lofflinePackage != null && TYPE.OFFLINE == lofflinePackage.getType()) {
            return lofflinePackage;
        }
        lofflinePackage = new CrawledPackage();
        lofflinePackage.setExpanded(true);
        lofflinePackage.setName(_JDT._.LinkCollector_addCrawledLink_offlinepackage());
        lofflinePackage.setType(TYPE.OFFLINE);
        offlinePackage = lofflinePackage;
        return lofflinePackage;
    }

    public LinkCrawler addCrawlerJob(final java.util.List<CrawledLink> links, final LinkCollectingJob job) {
        if (links == null || links.size() == 0) {
            throw new IllegalArgumentException("no links");
        }
        lazyInit();
        synchronized (shutdownLock) {
            if (ShutdownController.getInstance().isShutDownRequested()) {
                return null;
            }
            final JobLinkCrawler lc = new JobLinkCrawler(this, job);
            java.util.List<CrawledLink> jobs = new ArrayList<CrawledLink>(links);
            lc.crawl(jobs);
            return lc;
        }
    }

    public LinkCrawler addCrawlerJob(final LinkCollectingJob job) {
        try {
            if (job == null) {
                throw new IllegalArgumentException("job is null");
            }
            if (ShutdownController.getInstance().isShutDownRequested()) {
                return null;
            }
            lazyInit();
            final JobLinkCrawler lc = new JobLinkCrawler(this, job);
            /*
             * we don't want to keep reference on text during the whole link grabbing/checking/collecting way
             */
            String jobText = job.getText();
            onCrawlerAdded(lc);
            // keep text if it is tiny.
            // if we have the text in the job, we can display it for example in the balloons
            if (StringUtils.isNotEmpty(jobText) && jobText.length() > 500) {
                job.setText(null);
            }
            lc.crawl(jobText, job.getCustomSourceUrl(), job.isDeepAnalyse());
            return lc;
        } catch (VerifyError e) {
            logger.log(e);
            if (!Application.isJared(LinkCollector.class)) {
                Dialog.getInstance().showExceptionDialog("Eclipse Java 1.7 Bug", "This is an eclipse Java 7 bug. See here: http://goo.gl/REs9c\r\nAdd JVM Parameter -XX:-UseSplitVerifier", e);
            }
            throw e;
        }
    }

    private void addFilteredStuff(final CrawledLink filtered, final boolean checkDupe) {
        filtered.setCollectingInfo(null);
        if (restoreButtonEnabled == false) {
            /** RestoreButton is disabled, no need to save the filtered links */
            /* clear references */
            filtered.setSourceJob(null);
            filtered.setMatchingFilter(null);
            return;
        } else {
            collectingRequested.incrementAndGet();
            QUEUE.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    try {
                        final String linkID = filtered.getLinkID();
                        if (checkDupe) {
                            final CrawledLink existing = getCrawledLinkByLinkID(linkID);
                            if (existing != null) {
                                /* clear references */
                                filtered.setSourceJob(null);
                                filtered.setMatchingFilter(null);
                                eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.DUPE_LINK, filtered, QueuePriority.NORM));
                                return null;
                            }
                        }
                        putCrawledLinkByLinkID(linkID, filtered);
                        filteredStuff.add(filtered);
                        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.FILTERED_AVAILABLE));
                    } finally {
                        collectingProcessed.incrementAndGet();
                    }
                    return null;
                }
            });
        }

    }

    // clean up offline/various/dupeCheck maps
    protected void cleanupMaps(List<CrawledLink> links) {
        if (links == null) {
            return;
        }
        for (CrawledLink l : links) {
            removeCrawledLinkByLinkID(l);
            if (!removeFromMap(variousMap, l)) {
                if (!removeFromMap(offlineMap, l)) {
                    removeFromMap(badMappingMap, l);
                }
            }
        }
    }

    public void clearFilteredLinks() {
        QUEUE.add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                cleanupMaps(filteredStuff);
                filteredStuff.clear();
                eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.FILTERED_EMPTY));
                return null;
            }
        });
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
                badMappingMap.clear();
                offlineMap.clear();
                autoRenameCache.isEmpty();
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
        ret.setPriorityEnum(pkg.getPriorityEnum());
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
                if (link.isNameSet()) {
                    dl.setForcedFileName(link.getName());
                }
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
        final LinkCollectingInformation info = link.getCollectingInfo();
        if (info != null && info.getCollectingID() != getCollectingID()) {
            link.setCollectingInfo(null);
            link.setSourceJob(null);
            link.setMatchingFilter(null);
        } else {
            if (org.jdownloader.settings.staticreferences.CFG_LINKCOLLECTOR.DO_LINK_CHECK.isEnabled()) {
                QUEUE.add(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.HIGH) {

                    @Override
                    protected Void run() throws RuntimeException {

                        /* avoid additional linkCheck when linkID already exists */
                        /* update dupeCheck map */
                        final String id = link.getLinkID();
                        final CrawledLink existing = getCrawledLinkByLinkID(id);
                        if (existing != null) {
                            /* clear references */
                            logger.info("Filtered Dupe: " + id);
                            link.setCollectingInfo(null);
                            link.setSourceJob(null);
                            link.setMatchingFilter(null);
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
                QUEUE.add(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.HIGH) {

                    @Override
                    protected Void run() throws RuntimeException {
                        addCrawledLink(link);
                        return null;
                    }
                });
            }
        }
    }

    private void lazyInit() {
        if (linkChecker != null) {
            return;
        }
        synchronized (this) {
            if (linkChecker != null) {
                return;
            }
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
            if (!addGenericVariant(link.getDownloadLink())) {
                return;
            }

            addCrawledLink(link);
        }
    }

    private boolean addGenericVariant(final DownloadLink downloadLink) {
        if (!downloadLink.hasVariantSupport()) {
            final List<GenericVariants> converts = downloadLink.getDefaultPlugin().getGenericVariants(downloadLink);
            if (converts != null && converts.size() > 0) {
                final List<LinkVariant> variants = new ArrayList<LinkVariant>();
                variants.add(GenericVariants.ORIGINAL);
                variants.addAll(converts);
                if (variants.size() > 1) {
                    return Boolean.TRUE.equals(getQueue().addWait(new QueueAction<Boolean, RuntimeException>() {

                        @Override
                        protected Boolean run() throws RuntimeException {
                            downloadLink.setProperty("GENERIC_VARIANTS", true);
                            downloadLink.setVariants(variants);
                            downloadLink.setVariant(GenericVariants.ORIGINAL);
                            final CrawledLink existing = getCrawledLinkByLinkID(downloadLink.getLinkID());
                            if (existing != null && existing.getDownloadLink() != downloadLink) {
                                logger.info("Dupecheck Filtered Variant");
                                return Boolean.FALSE;
                            }
                            downloadLink.setVariantSupport(true);
                            return Boolean.TRUE;
                        }

                    }));
                }
            }
        }
        return true;
    }

    public java.util.List<FilePackage> convert(final List<CrawledLink> links, final boolean removeLinks) {
        if (links == null || links.size() == 0) {
            return null;
        }
        return QUEUE.addWait(new QueueAction<java.util.List<FilePackage>, RuntimeException>() {

            @Override
            protected java.util.List<FilePackage> run() throws RuntimeException {
                java.util.List<FilePackage> ret = new ArrayList<FilePackage>();
                HashMap<CrawledPackage, java.util.List<CrawledLink>> map = new HashMap<CrawledPackage, java.util.List<CrawledLink>>();
                if (removeLinks) {
                    cleanupMaps(links);
                }
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
                    if (removeLinks) {
                        LinkCollector.this.removeChildren(next.getKey(), next.getValue(), true);
                    }
                    ret.add(createFilePackage(next.getKey(), next.getValue()));
                }
                return ret;
            }

        });
    }

    private boolean removeFromMap(HashMap<CrawledPackageMappingID, java.util.List<CrawledLink>> idListMap, CrawledLink l) {
        final Iterator<Entry<CrawledPackageMappingID, java.util.List<CrawledLink>>> mapIt = idListMap.entrySet().iterator();
        while (mapIt.hasNext()) {
            final Entry<CrawledPackageMappingID, java.util.List<CrawledLink>> map = mapIt.next();
            final java.util.List<CrawledLink> list = map.getValue();
            if (list != null && list.remove(l)) {
                if (list.size() == 0) {
                    mapIt.remove();
                }
                return true;
            }
        }
        return false;
    }

    private CrawledPackageMappingID getIDFromMap(HashMap<CrawledPackageMappingID, java.util.List<CrawledLink>> idListMap, CrawledLink l) {
        final Iterator<Entry<CrawledPackageMappingID, java.util.List<CrawledLink>>> it = idListMap.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<CrawledPackageMappingID, java.util.List<CrawledLink>> elem = it.next();
            final CrawledPackageMappingID identifier = elem.getKey();
            final java.util.List<CrawledLink> mapElems = elem.getValue();
            if (mapElems != null && mapElems.contains(l)) {
                return identifier;
            }
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
                    if (counter != null) {
                        sortedAvailable.add(Long.parseLong(counter));
                    }
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
                    File loadedList = null;
                    for (File collectorList : availableCollectorLists) {
                        try {
                            if (lpackages == null) {
                                restoreMap.clear();
                                lpackages = load(collectorList, restoreMap);
                            } else {
                                loadedList = collectorList;
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
                    /* add loaded Packages to this controller */
                    try {
                        preProcessCrawledPackages(lpackages);
                        try {
                            writeLock();
                            for (final CrawledPackage filePackage : lpackages) {
                                for (final CrawledLink link : filePackage.getChildren()) {
                                    if (link.getDownloadLink() != null) {
                                        /* set CrawledLink as changeListener to its DownloadLink */
                                        link.getDownloadLink().setNodeChangeListener(link);
                                    }
                                }
                                filePackage.setControlledBy(LinkCollector.this);
                                CrawledPackageStorable storable = restoreMap.get(filePackage);
                                switch (filePackage.getType()) {
                                case NORMAL:
                                    if (storable.getPackageID() != null) {
                                        final CrawledPackageMappingID packageID = CrawledPackageMappingID.get(storable.getPackageID());
                                        if (packageID != null) {
                                            packageMap.put(packageID, filePackage);
                                        }
                                    }
                                    break;
                                case VARIOUS:
                                    if (variousPackage == null) {
                                        variousPackage = filePackage;
                                    }
                                    for (final CrawledLinkStorable link : storable.getLinks()) {
                                        final String id = link.getID();
                                        if (id != null) {
                                            final CrawledPackageMappingID packageID = CrawledPackageMappingID.get(id);
                                            if (packageID != null) {
                                                final List<CrawledLink> list = getIdentifiedMap(packageID, variousMap);
                                                list.add(link._getCrawledLink());
                                            }
                                        }
                                    }
                                    break;
                                case OFFLINE:
                                    if (offlinePackage == null) {
                                        offlinePackage = filePackage;
                                    }
                                    for (final CrawledLinkStorable link : storable.getLinks()) {
                                        final String id = link.getID();
                                        if (id != null) {
                                            final CrawledPackageMappingID packageID = CrawledPackageMappingID.get(id);
                                            if (packageID != null) {
                                                final List<CrawledLink> list = getIdentifiedMap(packageID, offlineMap);
                                                list.add(link._getCrawledLink());
                                            }
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
                            updateUniqueAlltimeIDMaps(lpackages);
                            for (final CrawledPackage filePackage : lpackages) {
                                for (final CrawledLink link : filePackage.getChildren()) {
                                    putCrawledLinkByLinkID(link.getLinkID(), link);
                                }
                            }
                            packages.addAll(0, lpackages);
                        } finally {
                            writeUnlock();
                        }
                        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_STRUCTURE));
                    } catch (final Throwable e) {
                        if (loadedList != null) {
                            final File renameTo = new File(loadedList.getAbsolutePath() + ".backup");
                            boolean backup = false;
                            try {
                                if (loadedList.exists()) {
                                    if (loadedList.renameTo(renameTo) == false) {
                                        IO.copyFile(loadedList, renameTo);
                                    }
                                    backup = true;
                                }
                            } catch (final Throwable e2) {
                                logger.log(e2);
                            }
                            logger.severe("Could backup " + loadedList + " to " + renameTo + " ->" + backup);
                        }
                        logger.log(e);
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
        if (fps == null || fps.size() == 0) {
            return;
        }
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
                                if (restoreMap != null) {
                                    restoreMap.put(storable._getCrawledPackage(), storable);
                                }
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
                if (lcs != null && JsonConfig.create(GeneralSettings.class).isConvertRelativePathsJDRoot()) {
                    try {
                        String oldRootPath = lcs.getRootPath();
                        if (!StringUtils.isEmpty(oldRootPath)) {
                            String newRoot = JDUtilities.getJDHomeDirectoryFromEnvironment().toString();
                            /*
                             * convert paths relative to JDownloader root,only in jared version
                             */
                            for (CrawledPackage pkg : ret2) {
                                if (!CrossSystem.isAbsolutePath(pkg.getDownloadFolder())) {
                                    /* no need to convert relative paths */
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
                final File renameTo = new File(file.getAbsolutePath() + ".backup");
                boolean backup = false;
                try {
                    if (file.exists()) {
                        if (file.renameTo(renameTo) == false) {
                            IO.copyFile(file, renameTo);
                        }
                        backup = true;
                    }
                } catch (final Throwable e2) {
                    logger.log(e2);
                }
                logger.severe("Could backup " + file + " to " + renameTo + " ->" + backup);
                logger.log(e);
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
                    if (file == null) {
                        file = Application.getResource("cfg/linkcollector.zip");
                    }
                }
                boolean deleteFile = true;
                ZipIOWriter zip = null;
                FileOutputStream fos = null;
                final int bufferSize;
                if (linkcollectorLists.size() > 0) {
                    final long fileLength = linkcollectorLists.get(0).length();
                    if (fileLength > 0) {
                        final int paddedFileLength = (((int) fileLength / 32768) + 1) * 32768;
                        bufferSize = Math.max(32768, paddedFileLength);
                    } else {
                        bufferSize = 32768;
                    }
                } else {
                    bufferSize = 32768;
                }
                if (packages != null && file != null) {
                    try {
                        if (file.exists()) {
                            if (file.isDirectory()) {
                                throw new IOException("File " + file + " is a directory");
                            }
                            if (FileCreationManager.getInstance().delete(file, null) == false) {
                                throw new IOException("Could not delete file " + file);
                            }
                        } else {
                            if (file.getParentFile().exists() == false && FileCreationManager.getInstance().mkdir(file.getParentFile()) == false) {
                                throw new IOException("Could not create parentFolder for file " + file);
                            }
                        }
                        int index = 0;
                        /* prepare formatter for package filenames in zipfiles */
                        String format = "%02d";
                        if (packages.size() >= 10) {
                            format = String.format("%%0%dd", (int) Math.log10(packages.size()) + 1);
                        }
                        fos = new FileOutputStream(file) {
                            @Override
                            public void close() throws IOException {
                                if (getChannel().isOpen()) {
                                    getChannel().force(true);
                                }
                                super.close();
                            }
                        };
                        zip = new ZipIOWriter(new BufferedOutputStream(fos, bufferSize));
                        for (final CrawledPackage pkg : packages) {
                            /* convert FilePackage to JSon */
                            final CrawledPackageStorable storable = new CrawledPackageStorable(pkg);
                            /* save packageID */
                            final CrawledPackageMappingID crawledPackageMappingID = LinkCollector.this.getPackageMapID(pkg);
                            if (crawledPackageMappingID != null) {
                                storable.setPackageID(crawledPackageMappingID.getCombined());
                            }
                            if (!CrawledPackageStorable.TYPE.NORMAL.equals(storable.getType())) {
                                if (CrawledPackageStorable.TYPE.VARIOUS.equals(storable.getType())) {
                                    /* save ID for variousMap */
                                    for (CrawledLinkStorable link : storable.getLinks()) {
                                        final CrawledLink cLink = link._getCrawledLink();
                                        final CrawledPackageMappingID id = getIDFromMap(variousMap, cLink);
                                        if (id != null) {
                                            link.setID(id.getCombined());
                                        }
                                    }
                                } else if (CrawledPackageStorable.TYPE.OFFLINE.equals(storable.getType())) {
                                    /* save ID for offlineMap */
                                    for (CrawledLinkStorable link : storable.getLinks()) {
                                        final CrawledLink cLink = link._getCrawledLink();
                                        final CrawledPackageMappingID id = getIDFromMap(offlineMap, cLink);
                                        if (id != null) {
                                            link.setID(id.getCombined());
                                        }
                                    }
                                }
                            }
                            final String string = JSonStorage.serializeToJson(storable);
                            byte[] bytes = string.getBytes("UTF-8");
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
                        fos = null;
                        deleteFile = false;
                        linkcollectorLists.add(0, file);
                        try {
                            int keepXOld = Math.max(JsonConfig.create(GeneralSettings.class).getKeepXOldLists(), 0);
                            if (linkcollectorLists.size() > keepXOld) {
                                for (int removeIndex = linkcollectorLists.size() - 1; removeIndex > keepXOld; removeIndex--) {
                                    File remove = linkcollectorLists.remove(removeIndex);
                                    if (remove != null) {
                                        logger.info("Delete outdated CollectorList: " + remove + " " + FileCreationManager.getInstance().delete(remove, null));
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
                            if (fos != null) {
                                fos.close();
                            }
                        } catch (final Throwable e) {
                        }
                        if (deleteFile && file.exists()) {
                            FileCreationManager.getInstance().delete(file, null);
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
        if (!allowSaving()) {
            return;
        }
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
        if (crawlerFilter == null) {
            throw new IllegalArgumentException("crawlerFilter is null");
        }
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
        if (request.hasVetos()) {
            return;
        }

        if (request.isSilent()) {
            synchronized (shutdownLock) {
                if (LinkChecker.isChecking() || LinkCrawler.isCrawling()) {
                    throw new ShutdownVetoException("LinkCollector is still running", this);
                }

            }
        } else {
            synchronized (shutdownLock) {

                if (LinkChecker.isChecking() || LinkCrawler.isCrawling()) {
                    if (JDGui.bugme(WarnLevel.NORMAL)) {
                        if (UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _JDT._.LinkCollector_onShutdownRequest_(), _JDT._.LinkCollector_onShutdownRequest_msg(), NewTheme.I().getIcon("linkgrabber", 32), _JDT._.literally_yes(), null)) {
                        } else {
                            return;
                        }
                        return;
                    }
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

        for (PackageView<CrawledPackage, CrawledLink> cp : selection.getPackageViews()) {
            List<CrawledLink> links;

            links = cp.getChildren();

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
        if (force.size() > 0) {
            DownloadWatchDog.getInstance().forceDownload(force);
        } else if (autostart) {
            DownloadWatchDog.getInstance().startDownloads();
        }

    }

    public void removeChildren(final List<CrawledLink> removechildren) {

        if (removechildren != null && removechildren.size() > 0) {
            QUEUE.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    internalRemoveChildren(removechildren);
                    cleanupMaps(removechildren);
                    _controllerStructureChanged(this.getQueuePrio());

                    return null;
                }
            });
        }
    }

    public static void requestDeleteLinks(final List<CrawledLink> nodesToDelete, final boolean containsOnline, final String string, final boolean byPassDialog, final boolean isCancelLinkcrawlerJobs, final boolean isResetTableSorter, final boolean isClearSearchFilter, final boolean isClearFilteredLinks) {

        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {

                boolean taskToDo = false;
                taskToDo = taskToDo || (nodesToDelete.size() > 0);
                taskToDo = taskToDo || ((isClearSearchFilter) && !LinkgrabberSearchField.getInstance().isEmpty());
                taskToDo = taskToDo || ((isResetTableSorter) && LinkGrabberTableModel.getInstance().getSortColumn() != null);
                taskToDo = taskToDo || ((isClearFilteredLinks) && LinkCollector.getInstance().isCollecting());
                taskToDo = taskToDo || ((isCancelLinkcrawlerJobs) && LinkCollector.getInstance().getfilteredStuffSize() > 0);

                if (!taskToDo) {

                    Toolkit.getDefaultToolkit().beep();
                    return null;

                }
                WarnLevel level = WarnLevel.LOW;

                boolean byPassDialog2 = byPassDialog;
                if (containsOnline) {
                    level = WarnLevel.NORMAL;
                }
                if (!JDGui.bugme(level)) {

                    byPassDialog2 = true;
                }
                if (!byPassDialog2 && !CFG_GUI.CFG.isBypassAllRlyDeleteDialogsEnabled()) {
                    GenericResetLinkgrabberRlyDialog dialog = new GenericResetLinkgrabberRlyDialog(nodesToDelete, containsOnline, string, isCancelLinkcrawlerJobs, isClearFilteredLinks, isClearSearchFilter, isResetTableSorter);
                    try {
                        Dialog.getInstance().showDialog(dialog);

                        if (dialog.isCancelCrawler()) {
                            LinkCollector.getInstance().abort();
                        }

                        if (dialog.isResetSort()) {
                            new EDTRunner() {

                                @Override
                                protected void runInEDT() {
                                    final LinkGrabberTable table = MenuManagerLinkgrabberTableContext.getInstance().getTable();
                                    table.getModel().setSortColumn(null);
                                    table.getModel().refreshSort();
                                    table.getTableHeader().repaint();
                                }
                            };
                        }
                        if (dialog.isResetSearch()) {

                            LinkgrabberSearchField.getInstance().setText("");
                            LinkgrabberSearchField.getInstance().onChanged();

                        }

                        if (dialog.isDeleteLinks()) {
                            LinkCollector.getInstance().removeChildren(nodesToDelete);

                        }
                        if (dialog.isClearFiltered()) {
                            LinkCollector.getInstance().clearFilteredLinks();
                        }

                    } catch (DialogClosedException e) {
                        e.printStackTrace();
                    } catch (DialogCanceledException e) {
                        e.printStackTrace();
                    }

                } else {

                    if (isCancelLinkcrawlerJobs) {
                        LinkCollector.getInstance().abort();
                    }

                    if (isResetTableSorter) {
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                final LinkGrabberTable table = MenuManagerLinkgrabberTableContext.getInstance().getTable();
                                table.getModel().setSortColumn(null);
                                table.getModel().refreshSort();
                                table.getTableHeader().repaint();
                            }
                        };
                    }
                    if (isClearSearchFilter) {

                        LinkgrabberSearchField.getInstance().setText("");
                        LinkgrabberSearchField.getInstance().onChanged();

                    }

                    if (isClearFilteredLinks) {
                        LinkCollector.getInstance().clearFilteredLinks();
                    }

                    if (nodesToDelete.size() > 0) {

                        LinkCollector.getInstance().removeChildren(nodesToDelete);

                    }

                }

                return null;
            }
        });

    }

    public void setActiveVariantForLink(final CrawledLink crawledLink, final LinkVariant linkVariant) {

        getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                CrawledPackage parent = crawledLink.getParentNode();
                if (parent == null || parent.getControlledBy() != LinkCollector.this) {
                    /* link is no longer controlled by this controller */
                    return null;
                }
                if (!crawledLink.hasVariantSupport()) {
                    /* link does not support variants */
                    return null;
                }
                // reset any custom names.
                crawledLink.setName(null);
                setActiveVariantForLink(crawledLink.getDownloadLink(), linkVariant);
                java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);
                checkableLinks.add(crawledLink);
                LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                linkChecker.check(checkableLinks);

                return null;
            }

        });

    }

    public void setActiveVariantForLink(final DownloadLink link, final LinkVariant variant) {

        getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final LinkVariant oldVariant = link.getDefaultPlugin().getActiveVariantByLink(link);
                link.getDefaultPlugin().setActiveVariantByLink(link, variant);
                final String newLinkID = link.getLinkID();
                final CrawledLink existing = getCrawledLinkByLinkID(newLinkID);
                if (existing != null && link != existing.getDownloadLink()) {
                    logger.info("Dupecheck Filtered Variant");
                    //
                    // variant available
                    link.getDefaultPlugin().setActiveVariantByLink(link, oldVariant);
                    return null;
                }
                return null;
            }

        });
    }

    private CrawledLink getCrawledLinkByLinkID(final String linkID) {
        final WeakReference<CrawledLink> item = dupeCheckMap.get(linkID);
        if (item != null) {
            final CrawledLink itemLink = item.get();
            if (itemLink == null || itemLink.getParentNode() == null || itemLink.getParentNode().getControlledBy() == null) {
                dupeCheckMap.remove(linkID);
                return itemLink;
            } else if (StringUtils.equals(itemLink.getLinkID(), linkID)) {
                return itemLink;
            } else {
                logger.warning("DupeCheckMap pollution detected: " + linkID);
                dupeCheckMap.remove(linkID);
                if (putCrawledLinkByLinkID(itemLink.getLinkID(), itemLink) == null) {
                    return getCrawledLinkByLinkID(linkID);
                } else {
                    logger.warning("Failed to clean DupeCheckMap pollution: " + itemLink.getLinkID());
                }
            }
        }
        return null;
    }

    private boolean removeCrawledLinkByLinkID(final CrawledLink link) {
        final String linkID = link.getLinkID();
        final CrawledLink itemLink = getCrawledLinkByLinkID(linkID);
        if (itemLink == link) {
            dupeCheckMap.remove(linkID);
        } else if (itemLink != null) {
            logger.warning("Failed to remove item from DupeCheckMap: " + linkID);
            return false;
        }
        return true;
    }

    private CrawledLink putCrawledLinkByLinkID(final String linkID, final CrawledLink link) {
        final WeakReference<CrawledLink> item = dupeCheckMap.put(linkID, new WeakReference<CrawledLink>(link));
        if (item != null) {
            final CrawledLink itemLink = item.get();
            if (itemLink != null) {
                final String itemLinkID = itemLink.getLinkID();
                if (itemLink == link) {
                    return null;
                } else if (StringUtils.equals(itemLinkID, linkID)) {
                    return itemLink;
                } else {
                    logger.warning("DupeCheckMap pollution detected: " + linkID);
                    if (putCrawledLinkByLinkID(itemLinkID, itemLink) != null) {
                        logger.warning("Failed to clean DupeCheckMap pollution: " + itemLinkID);
                    }
                }
            }
        }
        return null;
    }

    public boolean containsLinkId(final String linkID) {
        return linkID != null && Boolean.TRUE.equals(getQueue().addWait(new QueueAction<Boolean, RuntimeException>() {

            @Override
            protected Boolean run() throws RuntimeException {
                return getCrawledLinkByLinkID(linkID) != null;
            }

        }));
    }

    public CrawledLink addAdditional(final CrawledLink link, final LinkVariant o) {

        final DownloadLink dllink = new DownloadLink(link.getDownloadLink().getDefaultPlugin(), link.getDownloadLink().getView().getDisplayName(), link.getDownloadLink().getHost(), link.getDownloadLink().getPluginPattern(), true);
        dllink.setProperties(link.getDownloadLink().getProperties());
        // so plugins like youtube set inherent browserurl (not the youtubev2:// link)
        dllink.setOriginUrl(link.getDownloadLink().getOriginUrl());
        dllink.setContentUrl(link.getDownloadLink().getContainerUrl());
        dllink.setReferrerUrl(link.getDownloadLink().getReferrerUrl());
        final CrawledLink cl = new CrawledLink(dllink);

        cl.getDownloadLink().getDefaultPlugin().setActiveVariantByLink(cl.getDownloadLink(), o);

        return LinkCollector.getInstance().getQueue().addWait(new QueueAction<CrawledLink, RuntimeException>() {

            @Override
            protected CrawledLink run() throws RuntimeException {
                if (LinkCollector.getInstance().containsLinkId(cl.getLinkID())) {
                    return null;
                }
                final ArrayList<CrawledLink> list = new ArrayList<CrawledLink>();
                list.add(cl);

                LinkCollector.getInstance().moveOrAddAt(link.getParentNode(), list, link.getParentNode().indexOf(link) + 1);

                return cl;
            }
        });

    }

}
