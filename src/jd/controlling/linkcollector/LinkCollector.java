package jd.controlling.linkcollector;

import java.awt.Toolkit;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadSession;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogJob;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcollector.autostart.AutoStartManager;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkModifier;
import jd.controlling.linkcrawler.CrawledLinkProperty;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerDeepInspector;
import jd.controlling.linkcrawler.LinkCrawlerFilter;
import jd.controlling.linkcrawler.LinkCrawlerHandler;
import jd.controlling.linkcrawler.LinkCrawlerRule;
import jd.controlling.linkcrawler.LinkCrawlerRule.RULE;
import jd.controlling.linkcrawler.PackageInfo;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.controlling.packagecontroller.PackageController;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.controlling.SingleReachableState;
import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
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
import org.appwork.utils.io.J7FileList;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.captcha.blacklist.BlockAllCrawlerCaptchasEntry;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ArchiveExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.DocumentExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.linkcrawler.GenericVariants;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkgrabberSearchField;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MenuManagerLinkgrabberTableContext;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.plugins.controller.host.PluginFinder;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.settings.staticreferences.CFG_LINKCOLLECTOR;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;
import org.jdownloader.translate._JDT;

public class LinkCollector extends PackageController<CrawledPackage, CrawledLink> implements LinkCheckerHandler<CrawledLink>, LinkCrawlerHandler, ShutdownVetoListener {
    public static final String                        SOURCE_VARIANT_ID = "SOURCE_VARIANT_ID";
    private final WeakHashMap<JobLinkCrawler, Object> jobLinkCrawlers   = new WeakHashMap<JobLinkCrawler, Object>();

    public static final class JobLinkChecker extends LinkChecker<CrawledLink> {
        private final JobLinkCrawler jobLinkCrawler;

        public final JobLinkCrawler getJobLinkCrawler() {
            return jobLinkCrawler;
        }

        protected JobLinkChecker(JobLinkCrawler jobLinkCrawler) {
            this.jobLinkCrawler = jobLinkCrawler;
        }
    }

    public static final class JobLinkCrawler extends LinkCollectorCrawler {
        private final LinkCollectingJob         job;
        private final LinkCollectingInformation collectingInfo;
        private final LinkCollector             linkCollector;
        private final AtomicBoolean             crawlerAdded   = new AtomicBoolean(false);
        private final JobLinkChecker            linkChecker;
        private final AtomicInteger             waitingInQueue = new AtomicInteger(0);

        protected LinkCollector getLinkCollector() {
            return linkCollector;
        }

        public LinkChecker<CrawledLink> getLinkChecker() {
            return linkChecker;
        }

        public boolean isCollecting() {
            return isRunning() || getLinkChecker().isRunning() || hasWaitingInQueue();
        }

        public final boolean hasWaitingInQueue() {
            return getQueueSize() > 0;
        }

        public final int getQueueSize() {
            return waitingInQueue.get();
        }

        protected void enqueuOrDequeue(CrawledLink link, boolean enqueueOrDequeue) {
            if (enqueueOrDequeue) {
                waitingInQueue.incrementAndGet();
            } else {
                waitingInQueue.decrementAndGet();
            }
        }

        private final AtomicBoolean abortedFlag = new AtomicBoolean(false);

        public boolean isAborted() {
            return abortedFlag.get();
        }

        public boolean abort() {
            CaptchaBlackList.getInstance().add(new BlockAllCrawlerCaptchasEntry(this));
            final boolean aborted = abortedFlag.compareAndSet(false, true);
            final boolean wasCollecting = isCollecting();
            getLinkChecker().stopChecking();
            stopCrawling();
            return aborted || wasCollecting;
        }

        protected JobLinkCrawler(final LinkCollector linkCollector, final LinkCollectingJob job) {
            this.job = job;
            this.linkCollector = linkCollector;
            this.collectingInfo = new LinkCollectingInformation(this);
            this.linkChecker = new JobLinkChecker(this);
            this.linkChecker.setLinkCheckHandler(linkCollector);
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
                    final LinkCollectingJob job = getJob();
                    link.setSourceJob(job);
                    if (job.isAssignJobID()) {
                        final DownloadLink downloadLink = link.getDownloadLink();
                        if (downloadLink != null) {
                            downloadLink.setProperty(DownloadLink.PROPERTY_JOB_ID, job.getUniqueAlltimeID().getID());
                        }
                    }
                    linkCollector.handleFinalLink(link, getLinkChecker());
                    defaultHandler.handleFinalLink(link);
                }

                @Override
                public void handleFilteredLink(CrawledLink link) {
                    link.setSourceJob(getJob());
                    linkCollector.handleFilteredLink(link);
                    defaultHandler.handleFilteredLink(link);
                }

                @Override
                public void handleBrokenLink(CrawledLink link) {
                    linkCollector.handleBrokenLink(link);
                    defaultHandler.handleBrokenLink(link);
                }
            });
            synchronized (linkCollector.jobLinkCrawlers) {
                linkCollector.jobLinkCrawlers.put(this, Boolean.TRUE);
            }
        }

        public LinkCollectingJob getJob() {
            return job;
        }

        @Override
        public LinkCrawlerFilter getFilter() {
            return linkCollector.getCrawlerFilter();
        }

        @Override
        protected CrawledLink crawledLinkFactorybyURL(CharSequence url) {
            final CrawledLink ret = new CrawledLink(url);
            final LinkCollectingJob job = getJob();
            if (job != null) {
                ret.setOrigin(job.getOrigin());
                ret.setSourceJob(job);
            }
            return ret;
        }

        @Override
        protected void crawlerStopped() {
            if (crawlerAdded.compareAndSet(true, false)) {
                linkCollector.onCrawlerStopped(this);
            }
            super.crawlerStopped();
        }

        @Override
        protected void crawlerStarted() {
            if (crawlerAdded.compareAndSet(false, true)) {
                linkCollector.onCrawlerAdded(this);
            }
            linkCollector.onCrawlerStarted(this);
            super.crawlerStarted();
        }

        @Override
        protected void crawlerFinished() {
            linkCollector.onCrawlerFinished(this);
            super.crawlerFinished();
        }
    }

    private static class CrawledPackageMappingID {
        private final String id;

        private String getId() {
            return id;
        }

        private boolean isNull() {
            return getPackageName() == null && getDownloadFolder() == null && getDownloadFolderRaw() == null;
        }

        private String getPackageName() {
            return packageName;
        }

        private String getDownloadFolder() {
            return downloadFolder;
        }

        private String getDownloadFolderRaw() {
            return downloadFolderRaw;
        }

        private String getMappingID() {
            return getId() + "|_|" + getPackageName() + "|_|" + getDownloadFolderRaw();
        }

        private final String packageName;
        private final String downloadFolder;
        private final String downloadFolderRaw;
        private final String internalID;

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
            if (packageName != null) {
                /**
                 * we remove all non words/digits because some hoster replace/remove other chars
                 */
                if (Application.getJavaVersion() >= Application.JAVA17) {
                    packageName = packageName.replaceAll("(?U)[^\\w]", "").replaceAll("(?U)[_]", "").toLowerCase(Locale.ENGLISH);
                } else {
                    packageName = packageName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ENGLISH);
                }
            }
            this.packageName = packageName;
            this.downloadFolderRaw = downloadFolder;
            if (CrossSystem.isWindows() && downloadFolder != null) {
                /**
                 * windows has case insensitive filesystem
                 */
                downloadFolder = downloadFolder.toLowerCase(Locale.ENGLISH);
            }
            this.downloadFolder = downloadFolder;
            internalID = id + "|_|" + packageName + "|_|" + downloadFolder;
        }

        @Override
        public int hashCode() {
            return internalID.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj != null && obj instanceof CrawledPackageMappingID) {
                return StringUtils.equals(internalID, ((CrawledPackageMappingID) obj).internalID);
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

    public void onCrawlerFinished(LinkCollectorCrawler crawledLinkCrawler) {
        getEventsender().removeListener(crawledLinkCrawler);
        eventsender.fireEvent(new LinkCollectorEvent(this, LinkCollectorEvent.TYPE.CRAWLER_FINISHED, crawledLinkCrawler, QueuePriority.NORM));
    }

    public void onCrawlerStarted(LinkCollectorCrawler crawledLinkCrawler) {
        getEventsender().addListener(crawledLinkCrawler, true);
        eventsender.fireEvent(new LinkCollectorEvent(this, LinkCollectorEvent.TYPE.CRAWLER_STARTED, crawledLinkCrawler, QueuePriority.NORM));
    }

    private transient LinkCollectorEventSender                                  eventsender        = new LinkCollectorEventSender();
    public final ScheduledExecutorService                                       TIMINGQUEUE        = DelayedRunnable.getNewScheduledExecutorService();
    public static SingleReachableState                                          CRAWLERLIST_LOADED = new SingleReachableState("CRAWLERLIST_COMPLETE");
    private static LinkCollector                                                INSTANCE           = new LinkCollector();
    private volatile LinkChecker<CrawledLink>                                   defaultLinkChecker = null;
    /**
     * NOTE: only access these fields inside the IOEQ
     */
    private final HashMap<String, WeakReference<CrawledLink>>                   dupeCheckMap       = new HashMap<String, WeakReference<CrawledLink>>();
    private final HashMap<CrawledPackageMappingID, CrawledPackage>              packageMap         = new HashMap<CrawledPackageMappingID, CrawledPackage>();
    private final List<CrawledLink>                                             filteredStuff      = new CopyOnWriteArrayList<CrawledLink>();
    private volatile ExtractionExtension                                        archiver;
    private final DelayedRunnable                                               asyncSaving;
    protected volatile CrawledPackage                                           offlinePackage;
    protected volatile CrawledPackage                                           variousPackage;
    protected volatile CrawledPackage                                           permanentofflinePackage;
    private final CopyOnWriteArrayList<File>                                    linkcollectorLists = new CopyOnWriteArrayList<File>();
    private final HashMap<CrawledPackageMappingID, java.util.List<CrawledLink>> offlineMap         = new HashMap<CrawledPackageMappingID, java.util.List<CrawledLink>>();
    private final HashMap<CrawledPackageMappingID, java.util.List<CrawledLink>> variousMap         = new HashMap<CrawledPackageMappingID, java.util.List<CrawledLink>>();
    private final HashMap<CrawledPackageMappingID, java.util.List<CrawledLink>> badMappingMap      = new HashMap<CrawledPackageMappingID, java.util.List<CrawledLink>>();
    private final WeakHashMap<CrawledPackage, HashMap<Object, Object>>          autoRenameCache    = new WeakHashMap<CrawledPackage, HashMap<Object, Object>>();
    private final DelayedRunnable                                               asyncCacheCleanup;
    private final AutoStartManager                                              autoStartManager;
    private final boolean                                                       isDupeManagerEnabled;

    private LinkCollector() {
        autoStartManager = new AutoStartManager();
        ShutdownController.getInstance().addShutdownVetoListener(this);
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public long getMaxDuration() {
                return 0;
            }

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                LinkCollector.this.abort();
                QUEUE.addWait(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.HIGH) {
                    @Override
                    protected Void run() throws RuntimeException {
                        saveLinkCollectorLinks(true);
                        return null;
                    }
                });
            }

            @Override
            public String toString() {
                return "save linkcollector...";
            }
        });
        final LinkCollectorConfig cfg = JsonConfig.create(LinkCollectorConfig.class);
        this.isDupeManagerEnabled = cfg.isDupeManagerEnabled();
        final long minimumDelay = Math.max(5000, cfg.getMinimumSaveDelay());
        long maximumDelay = cfg.getMaximumSaveDelay();
        if (maximumDelay <= 0) {
            maximumDelay = -1;
        } else {
            maximumDelay = Math.max(maximumDelay, minimumDelay);
        }
        asyncSaving = new DelayedRunnable(TIMINGQUEUE, minimumDelay, maximumDelay) {
            private final boolean ignoreShutDown = false;

            @Override
            public void run() {
                if (isSavingAllowed(ignoreShutDown)) {
                    super.run();
                }
            }

            @Override
            public String getID() {
                return "LinkCollectorSave";
            }

            @Override
            public void delayedrun() {
                if (isSavingAllowed(ignoreShutDown)) {
                    QUEUE.addWait(new QueueAction<Void, RuntimeException>() {
                        @Override
                        protected Void run() throws RuntimeException {
                            saveLinkCollectorLinks(ignoreShutDown);
                            return null;
                        }
                    });
                }
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

            @Override
            public void onLinkCrawlerNewJob(LinkCollectingJob job) {
            }

            @Override
            public void onLinkCrawlerFinished() {
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
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.ABORT));
        synchronized (jobLinkCrawlers) {
            for (final JobLinkCrawler jobLinkCrawler : jobLinkCrawlers.keySet()) {
                jobLinkCrawler.abort();
            }
        }
    }

    public boolean isCollecting() {
        synchronized (jobLinkCrawlers) {
            for (final JobLinkCrawler jobLinkCrawler : jobLinkCrawlers.keySet()) {
                if (jobLinkCrawler.isCollecting()) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<JobLinkCrawler> getJobLinkCrawlerByJobId(long... ids) {
        List<JobLinkCrawler> result = new ArrayList<LinkCollector.JobLinkCrawler>();
        final Set<UniqueAlltimeID> jobs = UniqueAlltimeID.createSet(ids);
        synchronized (jobLinkCrawlers) {
            for (final JobLinkCrawler jobLinkCrawler : jobLinkCrawlers.keySet()) {
                if (jobs.contains(jobLinkCrawler.getJob().getUniqueAlltimeID())) {
                    result.add(jobLinkCrawler);
                }
            }
        }
        return result;
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
        final LinkCollectingInformation info = link.getCollectingInfo();
        if (info != null && info.isAborted()) {
            clearCrawledLinkReferences(link);
        } else {
            /* try to find good matching package or create new one */
            QUEUE.add(new QueueAction<Void, RuntimeException>() {
                private LinkCollectingInformation lci = info;

                @Override
                protected void onEnqueu(Queue queue) {
                    if (info != null) {
                        info.enqueu(this, link);
                    }
                }

                @Override
                protected void postRun() {
                    dequeu();
                }

                private void dequeu() {
                    final LinkCollectingInformation info = this.lci;
                    this.lci = null;
                    if (info != null) {
                        info.dequeu(this, link);
                    }
                }

                private void addToNewPackage(final List<CrawledLink> links, String newPackageName, final CrawledPackageMappingID crawledPackageMappingID) {
                    final CrawledPackage pkg = new CrawledPackage();
                    pkg.setExpanded(CFG_LINKCOLLECTOR.CFG.isPackageAutoExpanded());
                    pkg.setName(newPackageName);
                    pkg.setDownloadFolder(crawledPackageMappingID.getDownloadFolderRaw());
                    packageMap.put(crawledPackageMappingID, pkg);
                    if (links != null && links.size() > 0) {
                        LinkCollector.this.moveOrAddAt(pkg, links, -1);
                    }
                    if (crawledPackageMappingID.getPackageName() != null) {
                        // check if we have matching links in offline maper
                        final List<CrawledLink> offline = offlineMap.remove(crawledPackageMappingID);
                        if (offline != null && offline.size() > 0) {
                            LinkCollector.this.moveOrAddAt(pkg, offline, -1);
                        }
                        final List<CrawledLink> various = variousMap.remove(crawledPackageMappingID);
                        if (various != null && various.size() > 0) {
                            LinkCollector.this.moveOrAddAt(pkg, various, -1);
                        }
                        final List<CrawledLink> bad = getBadMappings(crawledPackageMappingID, pkg);
                        if (bad != null && bad.size() > 0) {
                            LinkCollector.this.moveOrAddAt(pkg, bad, -1);
                        }
                    } else {
                        putBadMappings(newPackageName, crawledPackageMappingID, links);
                    }
                }

                private void addToExistingPackage(final List<CrawledLink> links, CrawledPackage pkg, final CrawledPackageMappingID crawledPackageMappingID) {
                    final String packageName = pkg.getName();
                    if (links != null && links.size() > 0) {
                        LinkCollector.this.moveOrAddAt(pkg, links, -1);
                    }
                    if (crawledPackageMappingID.getPackageName() != null) {
                        if (!TYPE.VARIOUS.equals(pkg.getType())) {
                            // check if we have matching links in offline maper
                            final List<CrawledLink> offline = offlineMap.remove(crawledPackageMappingID);
                            if (offline != null && offline.size() > 0) {
                                LinkCollector.this.moveOrAddAt(pkg, offline, -1);
                            }
                            final List<CrawledLink> various = variousMap.remove(crawledPackageMappingID);
                            if (various != null && various.size() > 0) {
                                LinkCollector.this.moveOrAddAt(pkg, various, -1);
                            }
                            final List<CrawledLink> bad = getBadMappings(crawledPackageMappingID, pkg);
                            if (bad != null && bad.size() > 0) {
                                LinkCollector.this.moveOrAddAt(pkg, bad, -1);
                            }
                        }
                    } else {
                        putBadMappings(packageName, crawledPackageMappingID, links);
                    }
                }

                private void putBadMappings(String newPackageName, CrawledPackageMappingID crawledPackageMappingID, List<CrawledLink> links) {
                    final CrawledPackageMappingID badID = new CrawledPackageMappingID(crawledPackageMappingID.getId(), null, crawledPackageMappingID.getDownloadFolderRaw());
                    List<CrawledLink> badMappings = badMappingMap.get(badID);
                    if (links != null) {
                        for (CrawledLink link : links) {
                            final DownloadLink dlLink = link.getDownloadLink();
                            if (dlLink.getContainerUrl() != null || dlLink.getOriginUrl() != null) {
                                if (badMappings == null) {
                                    badMappings = new ArrayList<CrawledLink>();
                                    badMappingMap.put(badID, badMappings);
                                }
                                badMappings.add(link);
                            }
                        }
                    }
                }

                private List<CrawledLink> getBadMappings(CrawledPackageMappingID crawledPackageMappingID, CrawledPackage pkg) {
                    final List<CrawledLink> ret = new ArrayList<CrawledLink>();
                    final CrawledPackageMappingID badID = new CrawledPackageMappingID(crawledPackageMappingID.getId(), null, crawledPackageMappingID.getDownloadFolderRaw());
                    List<CrawledLink> badMappings = badMappingMap.get(badID);
                    if (badMappings != null) {
                        final HashSet<String> searchFor = new HashSet<String>();
                        final boolean readL = pkg.getModifyLock().readLock();
                        try {
                            for (final CrawledLink cLink : pkg.getChildren()) {
                                final DownloadLink dlLink = cLink.getDownloadLink();
                                searchFor.add(dlLink.getContainerUrl());
                                searchFor.add(dlLink.getOriginUrl());
                            }
                        } finally {
                            pkg.getModifyLock().readUnlock(readL);
                        }
                        searchFor.remove(null);
                        for (final CrawledLink cLink : badMappings) {
                            final DownloadLink dlLink = cLink.getDownloadLink();
                            if (searchFor.contains(dlLink.getContainerUrl()) || searchFor.contains(dlLink.getOriginUrl())) {
                                ret.add(cLink);
                            }
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
                    if (ret == null && crawledPackageMappingID.getPackageName() == null) {
                        final String containerURL = mappingLink.getDownloadLink().getContainerUrl();
                        final String originURL = mappingLink.getDownloadLink().getOriginUrl();
                        if (containerURL != null || originURL != null) {
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
                            for (int x = 2; x > 0; x--) {
                                HashMap<CrawledPackageMappingID, CrawledPackage> mappings = bestMappings.get(new Integer(x));
                                if (mappings != null) {
                                    for (final Entry<CrawledPackageMappingID, CrawledPackage> mapping : mappings.entrySet()) {
                                        final CrawledPackage pkg = mapping.getValue();
                                        final boolean readL = pkg.getModifyLock().readLock();
                                        try {
                                            for (final CrawledLink cLink : pkg.getChildren()) {
                                                final DownloadLink dlLink = cLink.getDownloadLink();
                                                if (dlLink != null && ((containerURL != null && StringUtils.equals(dlLink.getContainerUrl(), containerURL)) || (originURL != null && StringUtils.equals(dlLink.getOriginUrl(), originURL)))) {
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
                    }
                    return ret;
                }

                @Override
                protected Void run() throws RuntimeException {
                    try {
                        if (info != null && info.isAborted()) {
                            return null;
                        }
                        String linkID = link.getLinkID();
                        CrawledLink existingLink = getCrawledLinkByLinkID(linkID);
                        // give the hPLugin a chance to fix this;
                        while (existingLink != null) {
                            PluginForHost hPlugin = link.gethPlugin();
                            if (hPlugin == null || !hPlugin.onLinkCollectorDupe(existingLink, link)) {
                                break;
                            }
                            linkID = link.getLinkID();
                            existingLink = getCrawledLinkByLinkID(linkID);
                        }
                        if (existingLink != null && existingLink != link) {
                            /* clear references */
                            clearCrawledLinkReferences(link);
                            eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.DUPE_LINK, link, QueuePriority.NORM));
                            return null;
                        }
                        if (info == null || !info.isAborted()) {
                            putCrawledLinkByLinkID(linkID, link);
                            if (link.getDownloadLink() != null) {
                                /* set CrawledLink as changeListener to its DownloadLink */
                                link.getDownloadLink().setNodeChangeListener(link);
                            }
                            PackageInfo dpi = link.getDesiredPackageInfo();
                            UniqueAlltimeID uID = null;
                            String crawledPackageName = null;
                            String crawledPackageID = null;
                            boolean ignoreSpecialPackages = dpi != null && (dpi.isPackagizerRuleMatched() || Boolean.TRUE.equals(dpi.isIgnoreVarious()));
                            final String downloadFolder;
                            if (dpi != null) {
                                crawledPackageName = dpi.getName();
                                downloadFolder = dpi.getDestinationFolder();
                                if (downloadFolder != null && ignoreSpecialPackages == false) {
                                    /** this regex cuts of trailing / and \ for equals check **/
                                    final String compareCustom = downloadFolder.replaceAll("(.+?)(/|\\\\)+$", "$1");
                                    String compareDefault = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
                                    if (compareDefault != null) {
                                        compareDefault = compareDefault.replaceAll("(.+?)(/|\\\\)+$", "$1");
                                    }
                                    /* check for custom downloadFolder. let's not use various package then */
                                    // if (CrossSystem.isWindows() ||
                                    // JsonConfig.create(GeneralSettings.class).isForceMirrorDetectionCaseInsensitive()) {
                                    if (CrossSystem.isWindows()) {
                                        if (!compareCustom.equalsIgnoreCase(compareDefault)) {
                                            ignoreSpecialPackages = true;
                                        }
                                    } else if (!compareCustom.equals(compareDefault)) {
                                        ignoreSpecialPackages = true;
                                    }
                                }
                                if ((uID = dpi.getUniqueId()) != null) {
                                    crawledPackageID = dpi.getUniqueId().toString();
                                    if (ignoreSpecialPackages && LinkCrawler.PERMANENT_OFFLINE_ID == uID) {
                                        crawledPackageID = null;
                                    }
                                }
                            } else {
                                downloadFolder = null;
                            }
                            if (crawledPackageName == null) {
                                final DownloadLink dlLink = link.getDownloadLink();
                                final String fileName;
                                if (link.isNameSet() || dlLink.isNameSet()) {
                                    fileName = link.getName();
                                } else {
                                    fileName = LinkCrawler.getUnsafeName(link.getName(), null);
                                }
                                if (fileName != null) {
                                    if (AvailableLinkState.ONLINE.equals(link.getLinkState())) {
                                        crawledPackageName = fileName;
                                    } else {
                                        final ExtensionsFilterInterface extension = link.getLinkInfo().getExtension();
                                        if (!"".equalsIgnoreCase(extension.name())) {
                                            if (!DocumentExtensions.HTML.equals(extension.getSource()) && !DocumentExtensions.PHP.equals(extension.getSource())) {
                                                crawledPackageName = fileName;
                                            } else {
                                                final String tmpFileName = fileName.replaceFirst("\\.(html?|php)$", "");
                                                final ExtensionsFilterInterface tmpExtension = CompiledFiletypeFilter.getExtensionsFilterInterface(Files.getExtension(tmpFileName));
                                                if (tmpExtension != null) {
                                                    crawledPackageName = tmpFileName;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (crawledPackageName != null) {
                                    crawledPackageName = LinknameCleaner.cleanFileName(crawledPackageName, false, false, LinknameCleaner.EXTENSION_SETTINGS.REMOVE_ALL, true);
                                }
                            }
                            if (crawledPackageName == null && link.getLinkInfo().getExtension() instanceof ArchiveExtensions) {
                                final ExtractionExtension lArchiver = archiver;
                                if (lArchiver != null && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.ARCHIVE_PACKAGIZER_ENABLED.isEnabled()) {
                                    final CrawledLinkFactory clf = new CrawledLinkFactory(link);
                                    final Archive archive = lArchiver.buildArchive(clf);
                                    if (archive != null && archive.getArchiveFiles().size() > 1) {
                                        if (crawledPackageID == null) {
                                            crawledPackageID = archive.getArchiveID();
                                        }
                                        if (crawledPackageName == null) {
                                            crawledPackageName = _JDT.T.LinkCollector_archiv(LinknameCleaner.cleanFileName(archive.getName(), false, true, LinknameCleaner.EXTENSION_SETTINGS.REMOVE_KNOWN, true));
                                        }
                                    }
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
                                    newPackageName = _JDT.T.LinkCollector_addCrawledLink_offlinepackage();
                                }
                            }
                            final CrawledPackage pkg = getCrawledPackage(crawledPackageMapID, link);
                            if (pkg == null) {
                                if (!ignoreSpecialPackages && LinkCrawler.PERMANENT_OFFLINE_ID == uID) {
                                    /* these links will never come back online */
                                    final List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                                    add.add(link);
                                    LinkCollector.this.moveOrAddAt(getPermanentOfflineCrawledPackage(), add, -1);
                                } else if (!ignoreSpecialPackages && link.getLinkState() == AvailableLinkState.OFFLINE && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.OFFLINE_PACKAGE_ENABLED.isEnabled()) {
                                    final List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                                    add.add(link);
                                    LinkCollector.this.moveOrAddAt(getOfflineCrawledPackage(), add, -1);
                                } else if (!ignoreSpecialPackages && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.VARIOUS_PACKAGE_LIMIT.getValue() > 0 && CFG_LINKGRABBER.VARIOUS_PACKAGE_ENABLED.isEnabled()) {
                                    final List<CrawledLink> list;
                                    if (!crawledPackageMapID.isNull() || !AvailableLinkState.OFFLINE.equals(link.getLinkState())) {
                                        list = getIdentifiedMap(crawledPackageMapID, variousMap);
                                        list.add(link);
                                    } else {
                                        list = null;
                                    }
                                    if (list != null && list.size() > org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.VARIOUS_PACKAGE_LIMIT.getValue()) {
                                        addToNewPackage(list, newPackageName, crawledPackageMapID);
                                    } else {
                                        final List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                                        add.add(link);
                                        addToExistingPackage(add, getVariousCrawledPackage(), crawledPackageMapID);
                                    }
                                } else {
                                    final List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                                    add.add(link);
                                    addToNewPackage(add, newPackageName, crawledPackageMapID);
                                }
                            } else {
                                final List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                                add.add(link);
                                addToExistingPackage(add, pkg, crawledPackageMapID);
                            }
                            dequeu();
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
                        clearCrawledLinkReferences(link);
                    }
                }
            });
        }
    }

    private CrawledPackage getPermanentOfflineCrawledPackage() {
        CrawledPackage lpermanentofflinePackage = permanentofflinePackage;
        if (lpermanentofflinePackage != null && TYPE.POFFLINE == lpermanentofflinePackage.getType()) {
            return lpermanentofflinePackage;
        }
        lpermanentofflinePackage = new CrawledPackage();
        lpermanentofflinePackage.setExpanded(CFG_LINKCOLLECTOR.CFG.isPackageAutoExpanded());
        lpermanentofflinePackage.setName(_GUI.T.Permanently_Offline_Package());
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
        lvariousPackage.setExpanded(CFG_LINKCOLLECTOR.CFG.isPackageAutoExpanded());
        lvariousPackage.setName(_JDT.T.LinkCollector_addCrawledLink_variouspackage());
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
        lofflinePackage.setExpanded(CFG_LINKCOLLECTOR.CFG.isPackageAutoExpanded());
        lofflinePackage.setName(_JDT.T.LinkCollector_addCrawledLink_offlinepackage());
        lofflinePackage.setType(TYPE.OFFLINE);
        offlinePackage = lofflinePackage;
        return lofflinePackage;
    }

    public LinkCrawler addCrawlerJob(final java.util.List<CrawledLink> links, final LinkCollectingJob job) {
        if (ShutdownController.getInstance().isShutDownRequested()) {
            return null;
        }
        if (links == null || links.size() == 0) {
            throw new IllegalArgumentException("no links");
        }
        final JobLinkCrawler lc = newJobLinkCrawler(job);
        lc.crawl(new ArrayList<CrawledLink>(links));
        return lc;
    }

    public JobLinkCrawler newJobLinkCrawler(final LinkCollectingJob job) {
        return new JobLinkCrawler(this, job);
    }

    public LinkCrawler addCrawlerJob(final LinkCollectingJob job) {
        try {
            if (ShutdownController.getInstance().isShutDownRequested()) {
                return null;
            }
            if (job == null) {
                throw new IllegalArgumentException("job is null");
            }
            getEventsender().fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.NEW_CRAWLER_JOB, job, QueuePriority.NORM));
            logger.info("Added CrawlerJob:" + job);
            final JobLinkCrawler lc = newJobLinkCrawler(job);
            /*
             * we don't want to keep reference on text during the whole link grabbing/checking/collecting way
             */
            final String jobText = job.getText();
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
        final LinkCollectingInformation info = filtered.getCollectingInfo();
        filtered.setCollectingInfo(null);
        if (CFG_LINKGRABBER.RESTORE_BUTTON_ENABLED.isEnabled() == false) {
            /** RestoreButton is disabled, no need to save the filtered links */
            /* clear references */
            clearCrawledLinkReferences(filtered);
            return;
        } else {
            QUEUE.add(new QueueAction<Void, RuntimeException>() {
                @Override
                protected void onEnqueu(Queue queue) {
                    if (info != null) {
                        info.enqueu(this, filtered);
                    }
                }

                @Override
                protected void postRun() {
                    if (info != null) {
                        info.dequeu(this, filtered);
                    }
                }

                @Override
                protected Void run() throws RuntimeException {
                    String linkID = filtered.getLinkID();
                    if (checkDupe) {
                        CrawledLink existingLink = getCrawledLinkByLinkID(linkID);
                        // give the hPLugin a chance to fix this;
                        while (existingLink != null) {
                            PluginForHost hPlugin = filtered.gethPlugin();
                            if (hPlugin == null || !hPlugin.onLinkCollectorDupe(existingLink, filtered)) {
                                break;
                            }
                            linkID = filtered.getLinkID();
                            existingLink = getCrawledLinkByLinkID(linkID);
                        }
                        if (existingLink != null) {
                            /* clear references */
                            clearCrawledLinkReferences(filtered);
                            eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.DUPE_LINK, filtered, QueuePriority.NORM));
                            return null;
                        }
                    }
                    putCrawledLinkByLinkID(linkID, filtered);
                    filteredStuff.add(filtered);
                    eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.FILTERED_AVAILABLE));
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
        ret.setDownloadDirectory(LinkTreeUtils.getDownloadDirectory(pkg).getAbsolutePath());
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
        return LinkFilterController.getInstance();
    }

    public java.util.List<CrawledLink> getFilteredStuff(final boolean clearAfterGet) {
        return QUEUE.addWait(new QueueAction<java.util.List<CrawledLink>, RuntimeException>() {
            @Override
            protected java.util.List<CrawledLink> run() throws RuntimeException {
                final List<CrawledLink> ret2 = new ArrayList<CrawledLink>(filteredStuff);
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

    public synchronized LinkChecker<CrawledLink> getDefaultLinkChecker() {
        if (defaultLinkChecker == null) {
            final LinkChecker<CrawledLink> linkChecker = new LinkChecker<CrawledLink>();
            linkChecker.setLinkCheckHandler(this);
            defaultLinkChecker = linkChecker;
        }
        return defaultLinkChecker;
    }

    public void handleFilteredLink(CrawledLink link) {
        /* this method is called from LinkCrawler directly, we have to update dupeCheckMap */
        addFilteredStuff(link, true);
    }

    private final void clearCrawledLinkReferences(final CrawledLink link) {
        if (link != null) {
            link.setBrokenCrawlerHandler(null);
            link.setCustomCrawledLinkModifier(null);
            link.setUnknownHandler(null);
            link.setDesiredPackageInfo(null);
            link.setSourceLink(null);
            link.setCollectingInfo(null);
            link.setSourceJob(null);
            link.setMatchingFilter(null);
            link.setMatchingRule(null);
        }
    }

    public void handleFinalLink(final CrawledLink link) {
        handleFinalLink(link, getDefaultLinkChecker());
    }

    public void handleFinalLink(final CrawledLink link, final LinkChecker<CrawledLink> linkChecker) {
        final LinkCollectingInformation info = link.getCollectingInfo();
        if (info != null && info.isAborted()) {
            clearCrawledLinkReferences(link);
        } else {
            if (org.jdownloader.settings.staticreferences.CFG_LINKCOLLECTOR.DO_LINK_CHECK.isEnabled()) {
                QUEUE.add(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.LOW) {
                    @Override
                    protected void onEnqueu(Queue queue) {
                        if (info != null) {
                            info.enqueu(this, link);
                        }
                    }

                    @Override
                    protected void postRun() {
                        if (info != null) {
                            info.dequeu(this, link);
                        }
                    }

                    @Override
                    protected Void run() throws RuntimeException {
                        if (info != null && info.isAborted()) {
                            clearCrawledLinkReferences(link);
                        } else {
                            /* avoid additional linkCheck when linkID already exists */
                            /* update dupeCheck map */
                            String linkID = link.getLinkID();
                            CrawledLink existingLink = getCrawledLinkByLinkID(linkID);
                            // give the hPLugin a chance to fix this;
                            while (existingLink != null) {
                                PluginForHost hPlugin = link.gethPlugin();
                                if (hPlugin == null || !hPlugin.onLinkCollectorDupe(existingLink, link)) {
                                    break;
                                }
                                linkID = link.getLinkID();
                                existingLink = getCrawledLinkByLinkID(linkID);
                            }
                            if (existingLink != null) {
                                /* clear references */
                                logger.info("Filtered Dupe: " + linkID);
                                clearCrawledLinkReferences(link);
                                eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.DUPE_LINK, link, QueuePriority.NORM));
                                return null;
                            }
                            linkChecker.check(link);
                        }
                        return null;
                    }
                });
            } else {
                QUEUE.add(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.LOW) {
                    private LinkCollectingInformation lci = info;

                    @Override
                    protected void onEnqueu(Queue queue) {
                        if (info != null) {
                            info.enqueu(this, link);
                        }
                    }

                    @Override
                    protected void postRun() {
                        dequeu();
                    }

                    private void dequeu() {
                        final LinkCollectingInformation info = this.lci;
                        this.lci = null;
                        if (info != null) {
                            info.dequeu(this, link);
                        }
                    }

                    @Override
                    protected Void run() throws RuntimeException {
                        if (info != null && info.isAborted()) {
                            clearCrawledLinkReferences(link);
                        } else {
                            applyJobCrawledLinkModifier(link, true);
                            final PackagizerInterface pc = PackagizerController.getInstance();
                            if (pc != null) {
                                /* run packagizer on un-checked link */
                                pc.runByUrl(link);
                            }
                            applyJobCrawledLinkModifier(link, false);
                            dequeu();
                            addCrawledLink(link);
                        }
                        return null;
                    }
                });
            }
        }
    }

    public void linkCheckDone(final CrawledLink link) {
        /* this method is called by LinkChecker, we already updated the dupeCheckMap */
        if (getCrawlerFilter().dropByFileProperties(link)) {
            addFilteredStuff(link, false);
        } else {
            if (!addGenericVariant(link.getDownloadLink())) {
                return;
            }
            QUEUE.add(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.LOW) {
                @Override
                protected Void run() throws RuntimeException {
                    applyJobCrawledLinkModifier(link, true);
                    final PackagizerInterface pc = PackagizerController.getInstance();
                    if (pc != null) {
                        /* run packagizer on checked link */
                        pc.runByFile(link);
                    }
                    applyJobCrawledLinkModifier(link, false);
                    addCrawledLink(link);
                    return null;
                }
            });
        }
    }

    private void applyJobCrawledLinkModifier(final CrawledLink link, final boolean prePackagizer) {
        if (link != null) {
            final LinkCollectingJob job = link.getSourceJob();
            if (job != null) {
                final List<CrawledLinkModifier> modifiers = prePackagizer ? job.getPrePackagizerModifier() : job.getPostPackagizerModifier();
                for (final CrawledLinkModifier modifier : modifiers) {
                    try {
                        modifier.modifyCrawledLink(link);
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
            }
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

    public List<FilePackage> convert(final List<CrawledLink> links, final boolean removeLinks) {
        if (links == null || links.size() == 0) {
            return null;
        }
        return QUEUE.addWait(new QueueAction<List<FilePackage>, RuntimeException>() {
            @Override
            protected List<FilePackage> run() throws RuntimeException {
                final List<FilePackage> ret = new ArrayList<FilePackage>();
                final HashMap<CrawledPackage, List<CrawledLink>> map = new HashMap<CrawledPackage, List<CrawledLink>>();
                if (removeLinks) {
                    cleanupMaps(links);
                }
                for (final CrawledLink link : links) {
                    final CrawledPackage parent = link.getParentNode();
                    if (parent == null || parent.getControlledBy() != LinkCollector.this) {
                        logger.log(new Throwable("not controlled by this packagecontroller"));
                        continue;
                    }
                    List<CrawledLink> pkg_links = map.get(parent);
                    if (pkg_links == null) {
                        pkg_links = new ArrayList<CrawledLink>();
                        map.put(parent, pkg_links);
                    }
                    pkg_links.add(link);
                }
                final Iterator<Entry<CrawledPackage, List<CrawledLink>>> it = map.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<CrawledPackage, List<CrawledLink>> next = it.next();
                    ret.add(createFilePackage(next.getKey(), next.getValue()));
                    if (removeLinks) {
                        LinkCollector.this.removeChildren(next.getKey(), next.getValue(), true);
                    }
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
        logger.info("Collect Lists");
        File[] filesInCfg = null;
        final File cfg = Application.getResource("cfg/");
        if (Application.getJavaVersion() >= Application.JAVA17) {
            try {
                filesInCfg = J7FileList.findFiles(Pattern.compile("^linkcollector.*?\\.zip$", Pattern.CASE_INSENSITIVE), cfg, true).toArray(new File[0]);
            } catch (IOException e) {
                logger.log(e);
            }
        }
        if (filesInCfg == null) {
            filesInCfg = Application.getResource("cfg/").listFiles();
        }
        ArrayList<Long> sortedAvailable = new ArrayList<Long>();
        ArrayList<File> ret = new ArrayList<File>();
        if (filesInCfg != null) {
            for (File collectorList : filesInCfg) {
                final String name = collectorList.getName();
                if (name.startsWith("linkcollector") && collectorList.isFile()) {
                    String counter = new Regex(name, "linkcollector(\\d+)\\.zip$").getMatch(0);
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
                final ArrayList<File> availableCollectorLists = findAvailableCollectorLists();
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
                        importList(lpackages, restoreMap);
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

    public void importList(final LinkedList<CrawledPackage> lpackages, final Map<CrawledPackage, CrawledPackageStorable> restoreMap) {
        QUEUE.add(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.HIGH) {
            @Override
            protected Void run() throws RuntimeException {
                if (lpackages != null && restoreMap != null) {
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
                            final CrawledPackageStorable storable = restoreMap.get(filePackage);
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
                    final long version = backendChanged.incrementAndGet();
                    childrenChanged.set(version);
                    structureChanged.set(version);
                    eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_STRUCTURE));
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
        final PluginFinder pluginFinder = new PluginFinder(logger);
        while (iterator.hasNext()) {
            final CrawledPackage fp = iterator.next();
            if (fp.getChildren() != null) {
                final Iterator<CrawledLink> it = fp.getChildren().iterator();
                while (it.hasNext()) {
                    final CrawledLink localLink = it.next();
                    final DownloadLink dlLink = localLink.getDownloadLink();
                    if (dlLink == null) {
                        /* remove crawledLinks without DownloadLink */
                        it.remove();
                        continue;
                    }
                    /* assign defaultPlugin matching the hostname */
                    pluginFinder.assignPlugin(dlLink, true);
                }
            }
            if (fp.getChildren() == null || fp.getChildren().size() == 0) {
                /* remove empty packages */
                iterator.remove();
                continue;
            }
        }
    }

    public void checkPluginUpdates() {
        if (CRAWLERLIST_LOADED.isReached()) {
            QUEUE.add(new QueueAction<Void, RuntimeException>() {
                private final PluginFinder finder = new PluginFinder(logger);

                @Override
                protected Void run() throws RuntimeException {
                    getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {
                        @Override
                        public int returnMaxResults() {
                            return 0;
                        }

                        private final void updatePluginInstance(DownloadLink link) {
                            final long currentDefaultVersion;
                            final String currentDefaultHost;
                            final PluginForHost defaultPlugin = link.getDefaultPlugin();
                            if (defaultPlugin != null) {
                                currentDefaultHost = defaultPlugin.getLazyP().getHost();
                                currentDefaultVersion = defaultPlugin.getLazyP().getVersion();
                            } else {
                                currentDefaultHost = null;
                                currentDefaultVersion = -1;
                            }
                            final PluginForHost newDefaultPlugin = finder.assignPlugin(link, true);
                            final long newDefaultVersion;
                            final String newDefaultHost;
                            if (newDefaultPlugin != null) {
                                newDefaultVersion = newDefaultPlugin.getLazyP().getVersion();
                                newDefaultHost = newDefaultPlugin.getLazyP().getHost();
                            } else {
                                newDefaultVersion = -1;
                                newDefaultHost = null;
                            }
                            if (newDefaultPlugin != null && (currentDefaultVersion != newDefaultVersion || !StringUtils.equals(currentDefaultHost, newDefaultHost))) {
                                logger.info("Update Plugin for: " + link.getName() + ":" + link.getHost() + ":" + currentDefaultVersion + " to " + newDefaultPlugin.getLazyP().getDisplayName() + ":" + newDefaultPlugin.getLazyP().getVersion());
                                if (link.getFinalLinkState() == FinalLinkState.PLUGIN_DEFECT) {
                                    link.setFinalLinkState(null);
                                }
                            }
                        }

                        @Override
                        public boolean acceptNode(final CrawledLink node) {
                            if (node.getDownloadLink() != null) {
                                updatePluginInstance(node.getDownloadLink());
                            }
                            return false;
                        }
                    });
                    return null;
                }
            });
        }
    }

    private class LoadedPackage {
        private CrawledPackage                      crawledPackage = null;
        private final HashMap<Integer, CrawledLink> crawledLinks   = new HashMap<Integer, CrawledLink>();

        private CrawledPackage getLoadedPackage() {
            if (crawledPackage != null) {
                if (crawledPackage.getChildren().size() == 0) {
                    final List<Integer> childIndices = new ArrayList<Integer>(crawledLinks.keySet());
                    Collections.sort(childIndices);
                    for (final Integer childIndex : childIndices) {
                        final CrawledLink child = crawledLinks.get(childIndex);
                        crawledPackage.getChildren().add(child);
                        child.setParentNode(crawledPackage);
                    }
                }
                return crawledPackage;
            }
            return null;
        }
    }

    private LinkedList<CrawledPackage> load(File file, HashMap<CrawledPackage, CrawledPackageStorable> restoreMap) {
        try {
            return loadFile(file, restoreMap);
        } catch (Throwable e) {
            if (file != null) {
                final File backupTo = new File(file.getAbsolutePath() + ".backup");
                boolean backupSucceeded = false;
                Long size = null;
                try {
                    if (file.exists()) {
                        size = file.length();
                        if (size > 0) {
                            if (file.renameTo(backupTo) == false) {
                                IO.copyFile(file, backupTo);
                                backupSucceeded = backupTo.exists();
                                if (backupSucceeded && file.exists()) {
                                    if (file.delete() == false) {
                                        file.deleteOnExit();
                                    }
                                }
                            } else {
                                backupSucceeded = backupTo.exists();
                            }
                        } else {
                            file.delete();
                        }
                    }
                } catch (final Throwable e2) {
                    logger.log(e2);
                }
                if (backupSucceeded) {
                    logger.severe("Could backup " + file + "<to>" + backupTo);
                } else {
                    logger.severe("Could not backup " + file + "<to>" + backupTo + " because size=" + size);
                }
            }
            logger.log(e);
        }
        return null;
    }

    public LinkedList<CrawledPackage> loadFile(File file, Map<CrawledPackage, CrawledPackageStorable> restoreMap) throws IOException {
        LinkedList<CrawledPackage> ret = null;
        if (file != null && file.exists()) {
            FileInputStream fis = null;
            ZipInputStream zis = null;
            try {
                fis = new FileInputStream(file);
                zis = new ZipInputStream(fis);
                /* lets restore the CrawledPackages from Json */
                final HashMap<Integer, LoadedPackage> packageMap = new HashMap<Integer, LoadedPackage>();
                LinkCollectorStorable lcs = null;
                final TypeRef<CrawledLinkStorable> crawledLinkStorable = new TypeRef<CrawledLinkStorable>() {
                };
                final TypeRef<CrawledPackageStorable> crawledPackageStorable = new TypeRef<CrawledPackageStorable>() {
                };
                final TypeRef<LinkCollectorStorable> linkCollectorStorable = new TypeRef<LinkCollectorStorable>() {
                };
                ZipEntry entry = null;
                final ZipInputStream finalZis = zis;
                final InputStream entryInputStream = new InputStream() {
                    @Override
                    public int read() throws IOException {
                        return finalZis.read();
                    }

                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        return finalZis.read(b, off, len);
                    }

                    @Override
                    public long skip(long n) throws IOException {
                        return finalZis.skip(n);
                    }

                    @Override
                    public int available() throws IOException {
                        return finalZis.available();
                    }

                    @Override
                    public boolean markSupported() {
                        return false;
                    }

                    @Override
                    public void close() throws IOException {
                    }

                    @Override
                    public synchronized void mark(int readlimit) {
                    }
                };
                int entries = 0;
                final Pattern entryType = Pattern.compile("(\\d+)(?:_(\\d+))?|extraInfo", Pattern.CASE_INSENSITIVE);
                while ((entry = zis.getNextEntry()) != null) {
                    try {
                        entries++;
                        final Matcher entryName = entryType.matcher(entry.getName());
                        if (entryName.matches()) {
                            if (entryName.group(2) != null) {
                                // \\d+_\\d+ CrawledLinkStorable
                                final Integer packageIndex = Integer.valueOf(entryName.group(1));
                                final Integer childIndex = Integer.valueOf(entryName.group(2));
                                LoadedPackage loadedPackage = packageMap.get(packageIndex);
                                if (loadedPackage == null) {
                                    loadedPackage = new LoadedPackage();
                                    packageMap.put(packageIndex, loadedPackage);
                                }
                                final CrawledLinkStorable storable = JSonStorage.getMapper().inputStreamToObject(entryInputStream, crawledLinkStorable);
                                if (storable != null) {
                                    loadedPackage.crawledLinks.put(childIndex, storable._getCrawledLink());
                                } else {
                                    throw new WTFException("restored a null CrawledLinkStorable");
                                }
                            } else if (entryName.group(1) != null) {
                                // \\d+ CrawledPackageStorable
                                final Integer packageIndex = Integer.valueOf(entry.getName());
                                final CrawledPackageStorable storable = JSonStorage.getMapper().inputStreamToObject(entryInputStream, crawledPackageStorable);
                                if (storable != null) {
                                    LoadedPackage loadedPackage = packageMap.get(packageIndex);
                                    if (loadedPackage == null) {
                                        loadedPackage = new LoadedPackage();
                                        packageMap.put(packageIndex, loadedPackage);
                                    }
                                    loadedPackage.crawledPackage = storable._getCrawledPackage();
                                    if (restoreMap != null) {
                                        restoreMap.put(storable._getCrawledPackage(), storable);
                                    }
                                } else {
                                    throw new WTFException("restored a null CrawledPackageStorable");
                                }
                            } else {
                                // extraInfo
                                lcs = JSonStorage.getMapper().inputStreamToObject(entryInputStream, linkCollectorStorable);
                            }
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                        if (entry != null) {
                            logger.info("Entry:" + entry + "|Size:" + entry.getSize() + "|Compressed Size:" + entry.getCompressedSize());
                        }
                        throw e;
                    }
                }
                if (entries == 0) {
                    throw new WTFException("Empty/Invalid Zip:" + file + "|Size:" + file.length());
                }
                /* sort positions */
                final List<Integer> packageIndices = new ArrayList<Integer>(packageMap.keySet());
                Collections.sort(packageIndices);
                /* build final ArrayList of CrawledPackage */
                final List<CrawledPackage> ret2 = new ArrayList<CrawledPackage>(packageIndices.size());
                for (final Integer packageIndex : packageIndices) {
                    final LoadedPackage loadedPackage = packageMap.get(packageIndex);
                    final CrawledPackage crawledPackage = loadedPackage.getLoadedPackage();
                    if (crawledPackage != null) {
                        ret2.add(crawledPackage);
                    } else {
                        throw new WTFException("CrawledPackage at Index " + packageIndex + " is missing!");
                    }
                }
                if (lcs != null && JsonConfig.create(GeneralSettings.class).isConvertRelativePathsJDRoot()) {
                    try {
                        final String oldRootPath = lcs.getRootPath();
                        if (!StringUtils.isEmpty(oldRootPath)) {
                            final String newRoot = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath();
                            if (!oldRootPath.equals(newRoot)) {
                                /*
                                 * convert paths relative to JDownloader root,only in jared version
                                 */
                                for (final CrawledPackage pkg : ret2) {
                                    if (!CrossSystem.isAbsolutePath(pkg.getDownloadFolder())) {
                                        /* no need to convert relative paths */
                                        continue;
                                    }
                                    final String pkgPath = LinkTreeUtils.getDownloadDirectory(pkg).toString();
                                    if (pkgPath.startsWith(oldRootPath + "/") || pkgPath.startsWith(oldRootPath + "\\")) {
                                        /*
                                         * folder is inside JDRoot, lets update it
                                         */
                                        String restPath = pkgPath.substring(oldRootPath.length());
                                        // cut of leading path seperator
                                        restPath = restPath.replaceFirst("^(/+|\\\\+)", "");
                                        // fix path seperators
                                        restPath = CrossSystem.fixPathSeparators(restPath);
                                        final String newPath = new File(newRoot, restPath).toString();
                                        if (!StringUtils.equals(pkgPath, newPath)) {
                                            pkg.setDownloadFolder(newPath);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (final Throwable e) {
                        /* this method can throw exceptions, eg in SVN */
                        logger.log(e);
                    }
                }
                ret = new LinkedList<CrawledPackage>(ret2);
            } catch (final Throwable e) {
                try {
                    if (zis != null) {
                        zis.close();
                        zis = null;
                        fis = null;
                    } else if (fis != null) {
                        fis.close();
                        fis = null;
                    }
                } catch (final Throwable ignore) {
                }
                if (e instanceof IOException) {
                    throw (IOException) e;
                }
                throw new IOException(e);
            } finally {
                try {
                    if (zis != null) {
                        zis.close();
                    } else if (fis != null) {
                        fis.close();
                    }
                } catch (final Throwable ignore) {
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
                ZipOutputStream zos = null;
                FileOutputStream fos = null;
                final int bufferSize;
                if (linkcollectorLists.size() > 0) {
                    final long fileLength = linkcollectorLists.get(0).length();
                    if (fileLength > 0) {
                        final int paddedFileLength = (((int) fileLength / 32768) + 1) * 32768;
                        bufferSize = Math.max(32768, Math.min(1024 * 1024, paddedFileLength));
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
                        /* prepare formatter for package filenames in zipfiles */
                        final String packageFormat;
                        if (packages.size() >= 10) {
                            packageFormat = String.format("%%0%dd", (int) Math.log10(packages.size()) + 1);
                        } else {
                            packageFormat = "%02d";
                        }
                        fos = new FileOutputStream(file) {
                            @Override
                            public void close() throws IOException {
                                try {
                                    if (getChannel().isOpen()) {
                                        getChannel().force(true);
                                    }
                                } finally {
                                    super.close();
                                }
                            }
                        };
                        zos = new ZipOutputStream(new BufferedOutputStream(fos, bufferSize));
                        final ZipOutputStream finalZos = zos;
                        final OutputStream entryOutputStream = new OutputStream() {
                            @Override
                            public void write(int b) throws IOException {
                                finalZos.write(b);
                            }

                            @Override
                            public void write(byte[] b, int off, int len) throws IOException {
                                finalZos.write(b, off, len);
                            }

                            @Override
                            public void close() throws IOException {
                                finalZos.flush();
                            }

                            @Override
                            public void flush() throws IOException {
                                finalZos.flush();
                            }
                        };
                        int packageIndex = 0;
                        for (final CrawledPackage pkg : packages) {
                            final boolean readL = pkg.getModifyLock().readLock();
                            try {
                                final int childrenSize = pkg.getChildren().size();
                                if (childrenSize > 0) {
                                    final String packageEntryID = String.format(packageFormat, packageIndex++);
                                    {
                                        /* convert FilePackage to JSon */
                                        final CrawledPackageStorable packageStorable = new CrawledPackageStorable(pkg, false);
                                        /* save packageID */
                                        final CrawledPackageMappingID crawledPackageMappingID = LinkCollector.this.getPackageMapID(pkg);
                                        if (crawledPackageMappingID != null) {
                                            packageStorable.setPackageID(crawledPackageMappingID.getMappingID());
                                        }
                                        final ZipEntry packageEntry = new ZipEntry(packageEntryID);
                                        packageEntry.setMethod(ZipEntry.DEFLATED);
                                        zos.putNextEntry(packageEntry);
                                        JSonStorage.getMapper().writeObject(entryOutputStream, packageStorable);
                                        zos.closeEntry();
                                    }
                                    final String childFormat;
                                    if (childrenSize >= 10) {
                                        childFormat = String.format("%%0%dd", (int) Math.log10(childrenSize) + 1);
                                    } else {
                                        childFormat = "%02d";
                                    }
                                    int childIndex = 0;
                                    for (final CrawledLink link : pkg.getChildren()) {
                                        final CrawledLinkStorable linkStorable = new CrawledLinkStorable(link);
                                        CrawledPackageMappingID id = null;
                                        switch (pkg.getType()) {
                                        case VARIOUS:
                                            id = getIDFromMap(variousMap, link);
                                            break;
                                        case OFFLINE:
                                            id = getIDFromMap(offlineMap, link);
                                            break;
                                        default:
                                            break;
                                        }
                                        if (id != null) {
                                            linkStorable.setID(id.getMappingID());
                                        }
                                        final String childEntryID = String.format(childFormat, childIndex++);
                                        final ZipEntry linkEntry = new ZipEntry(packageEntryID + "_" + childEntryID);
                                        linkEntry.setMethod(ZipEntry.DEFLATED);
                                        zos.putNextEntry(linkEntry);
                                        JSonStorage.getMapper().writeObject(entryOutputStream, linkStorable);
                                        zos.closeEntry();
                                    }
                                }
                            } finally {
                                pkg.getModifyLock().readUnlock(readL);
                            }
                        }
                        final LinkCollectorStorable lcs = new LinkCollectorStorable();
                        try {
                            /*
                             * set current RootPath of JDownloader, so we can update it when user moves JDownloader folder
                             */
                            lcs.setRootPath(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath());
                        } catch (final Throwable e) {
                            /* the method above can throw exceptions, eg in SVN */
                            logger.log(e);
                        }
                        final ZipEntry linkCollectorEntry = new ZipEntry("extraInfo");
                        linkCollectorEntry.setMethod(ZipEntry.DEFLATED);
                        zos.putNextEntry(linkCollectorEntry);
                        JSonStorage.getMapper().writeObject(entryOutputStream, lcs);
                        zos.closeEntry();
                        zos.close();
                        zos = null;
                        fos = null;
                        deleteFile = false;
                        try {
                            final int keepXOld = Math.max(JsonConfig.create(GeneralSettings.class).getKeepXOldLists(), 0);
                            if (linkcollectorLists.size() > keepXOld) {
                                for (int removeIndex = linkcollectorLists.size() - 1; removeIndex >= keepXOld; removeIndex--) {
                                    final File remove = linkcollectorLists.remove(removeIndex);
                                    if (remove != null) {
                                        final boolean delete = FileCreationManager.getInstance().delete(remove, null);
                                        if (LogController.getInstance().isDebugMode()) {
                                            logger.info("Delete outdated CollectorList: " + remove + " " + delete);
                                        }
                                    }
                                }
                            }
                        } catch (final Throwable e) {
                            logger.log(e);
                        } finally {
                            linkcollectorLists.add(0, file);
                        }
                        return null;
                    } catch (final Throwable e) {
                        logger.log(e);
                    } finally {
                        try {
                            if (zos != null) {
                                zos.close();
                            } else if (fos != null) {
                                fos.close();
                            }
                        } catch (final Throwable e) {
                            logger.log(e);
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

    private boolean isSavingAllowed(final boolean ignoreShutDown) {
        return CRAWLERLIST_LOADED.isReached() && (ignoreShutDown || !ShutdownController.getInstance().isShuttingDown()) && JsonConfig.create(GeneralSettings.class).isSaveLinkgrabberListEnabled();
    }

    /**
     * save the current CrawledPackages/CrawledLinks controlled by this LinkCollector
     */
    private void saveLinkCollectorLinks(final boolean ignoreShutdown) {
        if (isSavingAllowed(ignoreShutdown)) {
            /* save as new Json ZipFile */
            try {
                save(getPackagesCopy(), null);
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
    }

    public void setArchiver(ExtractionExtension archiver) {
        this.archiver = archiver;
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
                        if (UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _JDT.T.LinkCollector_onShutdownRequest_(), _JDT.T.LinkCollector_onShutdownRequest_msg(), new AbstractIcon(IconKey.ICON_LINKGRABBER, 32), _JDT.T.literally_yes(), null)) {
                        } else {
                            throw new ShutdownVetoException("LinkCollector is still running", this);
                        }
                        return;
                    }
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

    public static enum MoveLinksMode {
        MANUAL,
        AUTO
    }

    public final static class MoveLinksSettings {
        final MoveLinksMode mode;

        public final MoveLinksMode getMode() {
            return mode;
        }

        public final Boolean getAutoStart() {
            return autoStart;
        }

        public final Boolean getAutoForce() {
            return autoForce;
        }

        final Boolean  autoStart;
        final Boolean  autoForce;
        final Priority autoPriority;

        public final Priority getAutoPriority() {
            return autoPriority;
        }

        public MoveLinksSettings(MoveLinksMode mode, Boolean autoStart, Boolean autoForce, Priority autoPriority) {
            this.mode = mode;
            this.autoForce = autoForce;
            this.autoStart = autoStart;
            this.autoPriority = autoPriority;
        }
    }

    public void moveLinksToDownloadList(final MoveLinksSettings moveLinksSettings, SelectionInfo<CrawledPackage, CrawledLink> selection) {
        final List<FilePackage> filePackagesToAdd = new ArrayList<FilePackage>();
        final List<DownloadLink> force = new ArrayList<DownloadLink>();
        final boolean forcedAutoStart = Boolean.TRUE.equals(moveLinksSettings.getAutoForce());
        final boolean autoMode = MoveLinksMode.AUTO.equals(moveLinksSettings.getMode());
        boolean autoStartLinks = false;
        for (final PackageView<CrawledPackage, CrawledLink> packageView : selection.getPackageViews()) {
            final List<CrawledLink> links = packageView.getChildren();
            final List<FilePackage> convertedLinks = LinkCollector.getInstance().convert(links, true);
            for (final CrawledLink cl : links) {
                autoStartLinks |= cl.isAutoStartEnabled();
                if ((autoMode && cl.isForcedAutoStartEnabled()) || forcedAutoStart) {
                    force.add(cl.getDownloadLink());
                }
                if (Priority.DEFAULT.equals(cl.getPriority())) {
                    cl.setPriority(moveLinksSettings.getAutoPriority());
                }
            }
            if (convertedLinks != null) {
                filePackagesToAdd.addAll(convertedLinks);
            }
        }
        /* convert all selected CrawledLinks to FilePackages */
        final boolean addTop = org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_ADD_AT_TOP.isEnabled();
        /* add the converted FilePackages to DownloadController */
        /**
         * addTop = 0, to insert the packages at the top
         *
         * addBottom = negative number -> add at the end
         */
        final boolean finalAutoStart;
        if (autoMode) {
            final Boolean autoStart = moveLinksSettings.getAutoStart();
            finalAutoStart = autoStartLinks || Boolean.TRUE.equals(autoStart);
        } else {
            final Boolean autoStart = moveLinksSettings.getAutoStart();
            if (autoStart != null) {
                finalAutoStart = autoStart.booleanValue();
            } else {
                finalAutoStart = autoStartLinks;
            }
        }
        DownloadController.getInstance().addAllAt(filePackagesToAdd, addTop ? 0 : -(filePackagesToAdd.size() + 10));
        DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                if (force.size() > 0) {
                    DownloadWatchDog.getInstance().forceDownload(force);
                    if (finalAutoStart) {
                        DownloadWatchDog.getInstance().enqueueJob(new DownloadWatchDogJob() {
                            @Override
                            public void interrupt() {
                            }

                            @Override
                            public void execute(DownloadSession currentSession) {
                                currentSession.setForcedOnlyModeEnabled(false);
                            }

                            @Override
                            public boolean isHighPriority() {
                                return false;
                            }
                        });
                    }
                } else if (finalAutoStart) {
                    DownloadWatchDog.getInstance().startDownloads();
                }
                return null;
            }
        });
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

    public Thread getAddLinksThread(final LinkCollectingJob job, final AtomicReference<LinkCrawler> lcReference) {
        return new Thread("AddLinksThread:" + job.getOrigin().getOrigin()) {
            private final HashSet<String> autoExtensionLearnBlackList = new HashSet<String>();
            {
                autoExtensionLearnBlackList.add("shtml");
                autoExtensionLearnBlackList.add("phtml");
                autoExtensionLearnBlackList.add("html");
                autoExtensionLearnBlackList.add("htm");
                autoExtensionLearnBlackList.add("php");
                autoExtensionLearnBlackList.add("js");
                autoExtensionLearnBlackList.add("css");
            }

            public void run() {
                LinkCrawler lc = LinkCollector.getInstance().addCrawlerJob(job);
                if (lcReference != null) {
                    lcReference.set(lc);
                }
                if (lc != null) {
                    lc.waitForCrawling();
                    if (!job.isDeepAnalyse() && lc.getProcessedLinksCounter() == 0 && lc.getUnhandledLinksFoundCounter() > 0) {
                        final List<CrawledLink> unhandledLinks = new ArrayList<CrawledLink>(lc.getUnhandledLinks());
                        final LinkOrigin origin = job.getOrigin().getOrigin();
                        for (CrawledLink unhandledLink : unhandledLinks) {
                            unhandledLink.setCrawlDeep(true);
                        }
                        final String[] origins = LinkCrawler.getConfig().getAutoLearnExtensionOrigins();
                        final boolean autoExtensionLearning;
                        if (origins != null && unhandledLinks.size() == 1) {
                            autoExtensionLearning = Arrays.asList(origins).contains(origin.name());
                        } else {
                            autoExtensionLearning = false;
                        }
                        if (!autoExtensionLearning) {
                            try {
                                final ConfirmDialog dialog = new ConfirmDialog(0, _GUI.T.AddLinksAction_actionPerformed_deep_title(), _GUI.T.AddLinksAction_actionPerformed_deep_msg(), null, _GUI.T.literally_yes(), _GUI.T.literall_no());
                                dialog.show().throwCloseExceptions();
                            } catch (DialogNoAnswerException e) {
                                e.printStackTrace();
                                if (!e.isCausedByDontShowAgain()) {
                                    return;
                                }
                            }
                        }
                        lc = LinkCollector.getInstance().addCrawlerJob(unhandledLinks, job);
                        if (lcReference != null) {
                            lcReference.set(lc);
                        }
                        if (lc != null) {
                            if (autoExtensionLearning) {
                                final LinkCrawlerDeepInspector defaultDeepInspector = lc.defaultDeepInspector();
                                lc.setDeepInspector(new LinkCrawlerDeepInspector() {
                                    public boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
                                        final boolean hasContentType = urlConnection.getHeaderField(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE) != null;
                                        if (urlConnection.getResponseCode() == 200) {
                                            if (urlConnection.isContentDisposition()) {
                                                return true;
                                            } else if (hasContentType && StringUtils.contains(urlConnection.getContentType(), "octet-stream")) {
                                                return true;
                                            } else if (hasContentType && StringUtils.contains(urlConnection.getContentType(), "audio")) {
                                                return true;
                                            } else if (hasContentType && StringUtils.contains(urlConnection.getContentType(), "video")) {
                                                return true;
                                            } else if (hasContentType && StringUtils.contains(urlConnection.getContentType(), "image")) {
                                                return true;
                                            } else if (urlConnection.getLongContentLength() > 2 * 1024 * 1024l && (!hasContentType || !StringUtils.contains(urlConnection.getContentType(), "text"))) {
                                                return true;
                                            }
                                        }
                                        return false;
                                    }

                                    private final boolean hasDirectHTTPRule(LinkCrawler lc, final URLConnectionAdapter urlConnection) {
                                        final List<LinkCrawlerRule> rules = lc.getLinkCrawlerRules();
                                        if (rules != null) {
                                            final String url = urlConnection.getURL().toString();
                                            for (final LinkCrawlerRule rule : rules) {
                                                if (RULE.DIRECTHTTP.equals(rule.getRule()) && rule.matches(url)) {
                                                    return true;
                                                }
                                            }
                                        }
                                        return false;
                                    }

                                    @Override
                                    public List<CrawledLink> deepInspect(LinkCrawler lc, final LinkCrawler.LinkCrawlerGeneration generation, Browser br, URLConnectionAdapter urlConnection, CrawledLink link) throws Exception {
                                        if (urlConnection.getResponseCode() == 200 && urlConnection.getRequest().getLocation() == null) {
                                            final LinkCrawlerRule matchingRule = link.getMatchingRule();
                                            if (matchingRule == null && looksLikeDownloadableContent(urlConnection)) {
                                                final URL url = urlConnection.getURL();
                                                if (StringUtils.endsWithCaseInsensitive(url.getPath(), ".php") && url.getQuery() != null) {
                                                    // hoster.domain/script.php?somevalue=somekey.....->Download
                                                    if (!hasDirectHTTPRule(lc, urlConnection)) {
                                                        final String domain = Browser.getHost(url, false);
                                                        final LinkCrawlerRule rule = new LinkCrawlerRule();
                                                        rule.setName("Learned php script download: " + domain + url.getPath());
                                                        rule.setPattern("(?i)https?://.*?" + Pattern.quote(domain) + Pattern.quote(url.getPath()) + "\\?.+");
                                                        rule.setRule(RULE.DIRECTHTTP);
                                                        LinkCrawler.addLinkCrawlerRule(rule);
                                                    }
                                                    try {
                                                        urlConnection.disconnect();
                                                    } catch (Throwable e) {
                                                    }
                                                    final ArrayList<CrawledLink> ret = new ArrayList<CrawledLink>();
                                                    final CrawledLink direct = lc.createDirectHTTPCrawledLink(urlConnection);
                                                    if (direct != null) {
                                                        ret.add(direct);
                                                    }
                                                    return ret;
                                                } else {
                                                    final String fileName = Plugin.getFileNameFromURL(url);
                                                    final String fileExtension = Files.getExtension(fileName);
                                                    if (StringUtils.isNotEmpty(fileExtension) && !autoExtensionLearnBlackList.contains(fileExtension)) {
                                                        if (!hasDirectHTTPRule(lc, urlConnection)) {
                                                            final LinkCrawlerRule rule = new LinkCrawlerRule();
                                                            rule.setName("Learned file extension: " + fileExtension);
                                                            rule.setPattern("(?i)https?://.*\\." + fileExtension + "($|\\?.*$)");
                                                            rule.setRule(RULE.DIRECTHTTP);
                                                            LinkCrawler.addLinkCrawlerRule(rule);
                                                        }
                                                        try {
                                                            urlConnection.disconnect();
                                                        } catch (Throwable e) {
                                                        }
                                                        final ArrayList<CrawledLink> ret = new ArrayList<CrawledLink>();
                                                        final CrawledLink direct = lc.createDirectHTTPCrawledLink(urlConnection);
                                                        if (direct != null) {
                                                            ret.add(direct);
                                                        }
                                                        return ret;
                                                    }
                                                }
                                            }
                                        }
                                        return defaultDeepInspector.deepInspect(lc, generation, br, urlConnection, link);
                                    }
                                });
                            }
                            lc.waitForCrawling();
                        }
                    }
                }
            }
        };
    }

    public static void requestDeleteLinks(final List<CrawledLink> nodesToDelete, final boolean containsOnline, final String string, final boolean byPassDialog, final boolean isCancelLinkcrawlerJobs, final boolean isResetTableSorter, final boolean isClearSearchFilter, final boolean isClearFilteredLinks) {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                boolean taskToDo = false;
                taskToDo = taskToDo || (nodesToDelete.size() > 0);
                if (!Application.isHeadless()) {
                    taskToDo = taskToDo || ((isClearSearchFilter) && !LinkgrabberSearchField.getInstance().isEmpty());
                }
                taskToDo = taskToDo || ((isResetTableSorter) && LinkGrabberTableModel.getInstance().getSortColumn() != null);
                taskToDo = taskToDo || ((isClearFilteredLinks) && LinkCollector.getInstance().isCollecting());
                taskToDo = taskToDo || ((isCancelLinkcrawlerJobs) && LinkCollector.getInstance().getfilteredStuffSize() > 0);
                if (!taskToDo) {
                    if (!Application.isHeadless()) {
                        Toolkit.getDefaultToolkit().beep();
                    }
                    return null;
                }
                final WarnLevel level;
                if (containsOnline) {
                    level = WarnLevel.NORMAL;
                } else {
                    level = WarnLevel.LOW;
                }
                final boolean finalByPassDialog;
                if (!JDGui.bugme(level)) {
                    finalByPassDialog = true;
                } else {
                    finalByPassDialog = byPassDialog;
                }
                if (!finalByPassDialog && !CFG_GUI.CFG.isBypassAllRlyDeleteDialogsEnabled()) {
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
                if (itemLink == null || (itemLink.getParentNode() == null && !filteredStuff.contains(itemLink)) || (itemLink.getParentNode() != null && itemLink.getParentNode().getControlledBy() == null)) {
                    dupeCheckMap.remove(linkID);
                }
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
        if (isDupeManagerEnabled) {
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
        final DownloadLink dllink = new DownloadLink(link.getDownloadLink().getDefaultPlugin(), link.getDownloadLink().getView().getDisplayName(), link.getDownloadLink().getHost(), link.getDownloadLink().getPluginPatternMatcher(), true);
        dllink.setProperties(link.getDownloadLink().getProperties());
        dllink.setProperty(SOURCE_VARIANT_ID, link.getUniqueID().getID());
        // so plugins like youtube set inherent browserurl (not the youtubev2:// link)
        dllink.setOriginUrl(link.getDownloadLink().getOriginUrl());
        dllink.setContentUrl(link.getDownloadLink().getContainerUrl());
        dllink.setReferrerUrl(link.getDownloadLink().getReferrerUrl());
        final CrawledLink cl = new CrawledLink(dllink);
        dllink.setNodeChangeListener(cl);
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
