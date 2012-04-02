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
import java.util.Locale;
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
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
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
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.zip.ZipIOReader;
import org.appwork.utils.zip.ZipIOWriter;
import org.jdownloader.controlling.UniqueSessionID;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.uiserio.NewUIO;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;
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
    private HashMap<String, ArrayList<CrawledLink>> hosterMap     = new HashMap<String, ArrayList<CrawledLink>>();
    private HashMap<Object, Object>                 autoRenameCache;
    private long                                    lastRenameTime;

    private LinkCollector() {
        autoRenameCache = new HashMap<Object, Object>();
        ShutdownController.getInstance().addShutdownVetoListener(new ShutdownVetoListener() {

            @Override
            public void onShutdownVeto(ArrayList<ShutdownVetoException> vetos) {
            }

            @Override
            public void onShutdownRequest(int vetos) throws ShutdownVetoException {
                if (vetos > 0) {
                    /* we already abort shutdown, no need to ask again */
                    return;
                }
                if (LinkChecker.isChecking() || LinkCrawler.isCrawling()) {
                    try {
                        NewUIO.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _JDT._.LinkCollector_onShutdownRequest_(), _JDT._.LinkCollector_onShutdownRequest_msg(), NewTheme.I().getIcon("linkgrabber", 32), _JDT._.literally_yes(), null);
                        return;
                    } catch (DialogNoAnswerException e) {
                        e.printStackTrace();
                    }
                    throw new ShutdownVetoException("LinkCollector is still running");
                }
            }

            @Override
            public void onShutdown() {
            }
        });
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                LinkCollector.this.abort();
                int retry = 10;
                while (retry > 0) {
                    if (!LinkChecker.isChecking() && !LinkCrawler.isCrawling()) {
                        /*
                         * we wait till the LinkCollector is finished or max 10
                         * secs
                         */
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                        break;
                    }
                    retry--;
                }
                IOEQ.getQueue().addWait(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.HIGH) {

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

            public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
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

    protected void autoFileNameCorrection(List<CrawledLink> pkgchildren) {
        long t = System.currentTimeMillis();
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
                    Log.L.info("Renamed file " + name + " to " + newName);
                } else {
                    newName = link.gethPlugin().autoFilenameCorrection(autoRenameCache, name, link.getDownloadLink(), maybebadfilenames);
                    if (newName != null && !newName.equals(name)) {
                        Log.L.info("Renamed file2 " + name + " to " + newName);
                    }
                }
                if (newName != null && !name.equals(newName)) {
                    /*
                     * we do not force a filename if newName equals to name set
                     * by plugin!
                     */
                    link.getDownloadLink().forceFileName(newName);
                }
            }
        }
        lastRenameTime = (System.currentTimeMillis() - t);
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
                    if (archiver != null && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.ARCHIVE_PACKAGIZER_ENABLED.getValue()) {
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
                            if (lazyPlg != null) plg2 = lazyPlg.getPrototype();
                            if (plg2 != null) {
                                identifier = plg2.filterPackageID(identifier);
                            }
                        }
                        ArrayList<CrawledLink> list = hosterMap.get(link.getHost());

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
                                        addmoveChildren(next.getValue(), existing.getChildren(), -1);
                                    }
                                    newMap.put(newID, next.getValue());
                                }
                            }

                            for (Iterator<Entry<String, CrawledPackage>> it = newMap.entrySet().iterator(); it.hasNext();) {
                                next = it.next();
                                CrawledPackage current = packageMap.remove(next.getKey());

                                if (current != null) {
                                    addmoveChildren(current, next.getValue().getChildren(), -1);
                                } else {
                                    packageMap.put(next.getKey(), next.getValue());
                                }

                            }
                            remapIdentifier(variousMap, plg);
                            remapIdentifier(offlineMap, plg);
                        }

                    }
                    // add to hostermap
                    ArrayList<CrawledLink> hosterlist = getIdentifiedMap(link.getHost(), hosterMap);
                    hosterlist.add(link);

                    CrawledPackage pkg = packageMap.get(identifier);
                    if (pkg == null) {
                        if (LinkCrawler.PERMANENT_OFFLINE_ID == uID) {
                            /* these links will never come back online */
                            getPermanentOfflineCrawledPackage();
                            List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                            add.add(link);
                            LinkCollector.this.addmoveChildren(getPermanentOfflineCrawledPackage(), add, -1);
                        } else if (link.getLinkState() == LinkState.OFFLINE && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.OFFLINE_PACKAGE_ENABLED.getValue()) {
                            getOfflineCrawledPackage();
                            ArrayList<CrawledLink> list = getIdentifiedMap(identifier, offlineMap);
                            list.add(link);
                            List<CrawledLink> add = new ArrayList<CrawledLink>(1);
                            add.add(link);
                            LinkCollector.this.addmoveChildren(getOfflineCrawledPackage(), add, -1);
                        } else if (org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.VARIOUS_PACKAGE_LIMIT.getValue() > 0) {
                            getVariousCrawledPackage();
                            ArrayList<CrawledLink> list = getIdentifiedMap(identifier, variousMap);
                            list.add(link);
                            if (list.size() > org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.VARIOUS_PACKAGE_LIMIT.getValue()) {
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
                    eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.ADDED_LINK, link, QueuePriority.NORM));
                    return null;
                } catch (RuntimeException e) {
                    dupeCheckMap.remove(link.getLinkID());
                    throw e;
                }
            }
        });
    }

    private static void remapIdentifier(HashMap<String, ArrayList<CrawledLink>> map, PluginForHost plg) {

        Entry<String, ArrayList<CrawledLink>> entry;
        HashMap<String, ArrayList<CrawledLink>> mapmap = new HashMap<String, ArrayList<CrawledLink>>();
        for (Iterator<Entry<String, ArrayList<CrawledLink>>> it = map.entrySet().iterator(); it.hasNext();) {
            entry = it.next();
            String newID = plg.filterPackageID(entry.getKey());
            if (!entry.getKey().equals(newID)) {
                it.remove();

                ArrayList<CrawledLink> existing = mapmap.get(newID);

                if (existing != null) {
                    existing.addAll(entry.getValue());

                }
                mapmap.put(newID, entry.getValue());
            }
        }
        for (Iterator<Entry<String, ArrayList<CrawledLink>>> it = mapmap.entrySet().iterator(); it.hasNext();) {
            entry = it.next();
            ArrayList<CrawledLink> current = map.remove(entry.getKey());
            if (current != null) {
                current.addAll(entry.getValue());
            } else {
                map.put(entry.getKey(), entry.getValue());
            }

        }

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
        ArrayList<CrawledLink> jobs = new ArrayList<CrawledLink>(links);
        LinkCollectingJob newJob = new LinkCollectingJob();
        newJob.setLinkCrawler(lc);
        newJob.setLinkChecker(linkChecker);
        for (CrawledLink job : jobs) {
            LinkCollectingJob sourceJob = job.getSourceJob();
            if (sourceJob != null) {
                /* we set weak reference to linkcrawler for this job */
                sourceJob.setLinkCrawler(lc);
                sourceJob.setLinkChecker(linkChecker);
            } else {
                /* no job set yet, we set new one */
                job.setSourceJob(newJob);
            }
        }
        lc.crawl(jobs);
        return lc;
    }

    public LinkCrawler addCrawlerJob(final LinkCollectingJob job) {
        if (job == null) throw new IllegalArgumentException("job is null");
        lazyInit();
        final LinkCollectorCrawler lc = new LinkCollectorCrawler() {
            @Override
            protected CrawledLink crawledLinkFactorybyURL(String url) {
                CrawledLink ret = new CrawledLink(url);
                ret.setSourceJob(job);
                return ret;
            }

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
        /* we set weak reference to linkcrawler for this job */
        job.setLinkCrawler(lc);
        job.setLinkChecker(linkChecker);
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
    protected void cleanupMaps(List<CrawledLink> links) {
        if (links == null) return;
        for (CrawledLink l : links) {
            dupeCheckMap.remove(l.getLinkID());
            removeFromMap(variousMap, l);
            removeFromMap(offlineMap, l);
            removeFromMap(hosterMap, l);
        }
        autoRenameCache.clear();

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
        hosterMap.clear();
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
                    dl.setNodeChangeListener(null);
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
        if (org.jdownloader.settings.staticreferences.CFG_LINKCOLLECTOR.DO_LINK_CHECK.isEnabled()) {
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

    public void refreshData() {
        eventsender.fireEvent(new LinkCollectorEvent(LinkCollector.this, LinkCollectorEvent.TYPE.REFRESH_DATA));
    }

    public ArrayList<FilePackage> removeAndConvert(final List<CrawledLink> links) {
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

    private String getIDFromMap(HashMap<String, ArrayList<CrawledLink>> idListMap, CrawledLink l) {
        Iterator<Entry<String, ArrayList<CrawledLink>>> it = idListMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, ArrayList<CrawledLink>> elem = it.next();
            String identifier = elem.getKey();
            ArrayList<CrawledLink> mapElems = elem.getValue();
            if (mapElems != null && mapElems.contains(l)) return identifier;
        }
        return null;
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
                                ArrayList<CrawledLink> list = getIdentifiedMap(link.getHost(), hosterMap);
                                list.add(link);

                            }
                            filePackage.setControlledBy(LinkCollector.this);
                            CrawledPackageStorable storable = restoreMap.get(filePackage);
                            if (storable != null) {
                                if (CrawledPackageStorable.TYPE.NORMAL.equals(storable._getType()) && storable.getPackageID() != null) {
                                    /* keep packageMap up2date */
                                    packageMap.put(storable.getPackageID(), filePackage);
                                } else if (CrawledPackageStorable.TYPE.VARIOUS.equals(storable._getType())) {
                                    /* restore variousMap */
                                    for (CrawledLinkStorable link : storable.getLinks()) {
                                        String id = link.getID();
                                        if (id != null) {
                                            ArrayList<CrawledLink> list = getIdentifiedMap(id, variousMap);
                                            list.add(link._getCrawledLink());
                                        }
                                    }
                                } else if (CrawledPackageStorable.TYPE.OFFLINE.equals(storable._getType())) {
                                    /* restore variousMap */
                                    for (CrawledLinkStorable link : storable.getLinks()) {
                                        String id = link.getID();
                                        if (id != null) {
                                            ArrayList<CrawledLink> list = getIdentifiedMap(id, offlineMap);
                                            list.add(link._getCrawledLink());
                                        }
                                    }
                                }
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
