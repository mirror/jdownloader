package jd.controlling.linkcrawler;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jd.config.Property;
import jd.controlling.IOEQ;
import jd.controlling.IOPermission;
import jd.controlling.JDPluginLogger;
import jd.controlling.linkcollector.LinknameCleaner;
import jd.http.Browser;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.jdownloader.controlling.UniqueSessionID;
import org.jdownloader.plugins.controller.container.ContainerPluginController;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.GeneralSettings;

public class LinkCrawler implements IOPermission {

    private PluginForHost               directHTTP                  = null;
    private PluginForHost               ftp                         = null;
    private ArrayList<CrawledLink>      crawledLinks                = new ArrayList<CrawledLink>();
    private AtomicInteger               crawledLinksCounter         = new AtomicInteger(0);
    private ArrayList<CrawledLink>      filteredLinks               = new ArrayList<CrawledLink>();
    private AtomicInteger               filteredLinksCounter        = new AtomicInteger(0);
    private AtomicInteger               crawler                     = new AtomicInteger(0);
    private static AtomicInteger        CRAWLER                     = new AtomicInteger(0);
    private HashSet<String>             duplicateFinderContainer    = new HashSet<String>();
    private HashSet<String>             duplicateFinderCrawler      = new HashSet<String>();
    private HashSet<String>             duplicateFinderFinal        = new HashSet<String>();
    private HashSet<String>             duplicateFinderDeep         = new HashSet<String>();
    private LinkCrawlerHandler          handler                     = null;
    private static ThreadPoolExecutor   threadPool                  = null;

    private HashSet<String>             captchaBlockedHoster        = new HashSet<String>();
    private LinkCrawlerFilter           filter                      = null;
    private volatile boolean            allowCrawling               = true;
    private AtomicInteger               crawlerGeneration           = new AtomicInteger(0);
    private LinkCrawler                 parentCrawler               = null;
    private final long                  created;

    public static final String          ALLOW_MERGE                 = "ALLOW_MERGE";
    public static final UniqueSessionID PERMANENT_OFFLINE_ID        = new UniqueSessionID();
    private boolean                     doDuplicateFinderFinalCheck = true;

    public boolean isDoDuplicateFinderFinalCheck() {
        if (parentCrawler != null) parentCrawler.isDoDuplicateFinderFinalCheck();
        return doDuplicateFinderFinalCheck;
    }

    /*
     * customized comparator we use to prefer faster decrypter plugins over
     * slower ones
     */
    private static Comparator<Runnable>   comparator  = new Comparator<Runnable>() {

                                                          public int compare(Runnable o1, Runnable o2) {
                                                              if (o1 == o2) return 0;
                                                              long l1 = ((LinkCrawlerRunnable) o1).getAverageRuntime();
                                                              long l2 = ((LinkCrawlerRunnable) o2).getAverageRuntime();
                                                              return (l1 < l2) ? -1 : ((l1 == l2) ? 0 : 1);
                                                          }

                                                      };

    static {
        int maxThreads = Math.max(JsonConfig.create(LinkCrawlerConfig.class).getMaxThreads(), 1);
        int keepAlive = Math.max(JsonConfig.create(LinkCrawlerConfig.class).getThreadKeepAlive(), 100);

        threadPool = new ThreadPoolExecutor(0, maxThreads, keepAlive, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>(100, comparator), new ThreadFactory() {

            public Thread newThread(Runnable r) {
                /*
                 * our thread factory so we have logger,browser settings
                 * available
                 */
                return new LinkCrawlerThread(r);
            }

        }, new ThreadPoolExecutor.AbortPolicy()) {

            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                /*
                 * WORKAROUND for stupid SUN /ORACLE way of
                 * "how a threadpool should work" !
                 */
                int working = threadPool.getActiveCount();
                int active = threadPool.getPoolSize();
                int max = threadPool.getMaximumPoolSize();
                if (active < max) {
                    if (working == active) {
                        /*
                         * we can increase max pool size so new threads get
                         * started
                         */
                        threadPool.setCorePoolSize(Math.min(max, active + 1));
                    }
                }
            }

        };
        threadPool.allowCoreThreadTimeOut(true);
    }

    private static LinkCrawlerEventSender EVENTSENDER = new LinkCrawlerEventSender();

    public static LinkCrawlerEventSender getEventSender() {
        return EVENTSENDER;
    }

    public LinkCrawler() {
        this(true, true);
    }

    public LinkCrawler(boolean connectParentCrawler, boolean avoidDuplicates) {
        setHandler(defaulHandlerFactory());
        setFilter(defaultFilterFactory());
        if (connectParentCrawler && Thread.currentThread() instanceof LinkCrawlerThread) {
            /* forward crawlerGeneration from parent to this child */
            LinkCrawlerThread thread = (LinkCrawlerThread) Thread.currentThread();
            parentCrawler = thread.getCurrentLinkCrawler();
        }
        for (LazyHostPlugin pHost : HostPluginController.getInstance().list()) {
            if (directHTTP == null && "http links".equals(pHost.getDisplayName())) {
                /* for direct access to the directhttp plugin */
                directHTTP = pHost.getPrototype();
            }
            if (ftp == null && "ftp".equals(pHost.getDisplayName())) {
                /* for generic ftp sites */
                ftp = pHost.getPrototype();
            }
        }
        this.created = System.currentTimeMillis();
        this.doDuplicateFinderFinalCheck = avoidDuplicates;
    }

    public LinkCrawler(boolean connectParentCrawler) {
        this(true, true);
    }

    public long getCreated() {
        if (parentCrawler != null) return parentCrawler.getCreated();
        return created;
    }

    /**
     * returns the generation of this LinkCrawler if thisGeneration is true.
     * 
     * if a parent LinkCrawler does exist and thisGeneration is false, we return
     * the older generation of the parent LinkCrawler or this child
     * 
     * @param thisGeneration
     * @return
     */
    protected int getCrawlerGeneration(boolean thisGeneration) {
        if (!thisGeneration && parentCrawler != null) { return Math.max(crawlerGeneration.get(), parentCrawler.getCrawlerGeneration(false)); }
        return crawlerGeneration.get();
    }

    protected CrawledLink crawledLinkFactorybyURL(String url) {
        return new CrawledLink(url);
    }

    public void crawl(String text) {
        crawl(text, null, false);
    }

    public void crawl(final String text, final String url, final boolean allowDeep) {
        final int generation = this.getCrawlerGeneration(true);
        if (!checkStartNotify()) return;
        try {
            if (StringUtils.isEmpty(text)) return;
            if (insideDecrypterPlugin()) {
                ArrayList<CrawledLink> links = _crawl(text, url, allowDeep);
                crawl(links);
            } else {
                if (!checkStartNotify()) return;
                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {

                    @Override
                    void crawling() {
                        ArrayList<CrawledLink> links = _crawl(text, url, allowDeep);
                        crawl(links);
                    }
                });
                return;
            }
        } finally {
            checkFinishNotify();
        }
    }

    private ArrayList<CrawledLink> _crawl(String text, String url, boolean allowDeep) {
        String[] possibleLinks = HTMLParser.getHttpLinks(text, url);
        if (possibleLinks == null || possibleLinks.length == 0) return null;
        final ArrayList<CrawledLink> possibleCryptedLinks = new ArrayList<CrawledLink>(possibleLinks.length);
        for (String possibleLink : possibleLinks) {
            CrawledLink link;
            possibleCryptedLinks.add(link = crawledLinkFactorybyURL(possibleLink));
            link.setCrawlDeep(allowDeep);
        }
        return possibleCryptedLinks;
    }

    public void crawl(final ArrayList<CrawledLink> possibleCryptedLinks) {
        final int generation = this.getCrawlerGeneration(true);
        if (!checkStartNotify()) return;
        try {
            if (possibleCryptedLinks == null || possibleCryptedLinks.size() == 0) return;
            if (insideDecrypterPlugin()) {
                /*
                 * direct decrypt this link because we are already inside a
                 * LinkCrawlerThread and this avoids deadlocks on plugin waiting
                 * for linkcrawler results
                 */
                distribute(possibleCryptedLinks);
                return;
            } else {
                /*
                 * enqueue this cryptedLink for decrypting
                 */
                if (!checkStartNotify()) return;
                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {

                    @Override
                    void crawling() {
                        distribute(possibleCryptedLinks);
                    }
                });
                return;
            }
        } finally {
            checkFinishNotify();
        }
    }

    private boolean insideDecrypterPlugin() {
        if (Thread.currentThread() instanceof LinkCrawlerThread && ((LinkCrawlerThread) Thread.currentThread()).isLinkCrawlerThreadUsedbyDecrypter()) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * check if all known crawlers are done and notify all waiting listener +
     * cleanup DuplicateFinder
     */
    protected void checkFinishNotify() {
        boolean stopped = false;
        synchronized (this) {
            if (crawler.decrementAndGet() == 0) {
                /* this LinkCrawler instance stopped, notify static counter */
                synchronized (CRAWLER) {
                    CRAWLER.decrementAndGet();
                }
                synchronized (crawler) {
                    crawler.notifyAll();
                }
                /*
                 * all tasks are done , we can now cleanup our duplicateFinder
                 */
                synchronized (duplicateFinderContainer) {
                    duplicateFinderContainer.clear();
                }
                synchronized (duplicateFinderCrawler) {
                    duplicateFinderCrawler.clear();
                }
                synchronized (duplicateFinderFinal) {
                    duplicateFinderFinal.clear();
                }
                synchronized (duplicateFinderDeep) {
                    duplicateFinderDeep.clear();
                }
                stopped = true;
            }
        }
        if (stopped) {
            EVENTSENDER.fireEvent(new LinkCrawlerEvent(this, LinkCrawlerEvent.Type.STOPPED));
        }
    }

    private boolean checkStartNotify() {
        boolean started = false;
        synchronized (this) {
            if (!allowCrawling) return false;
            if (crawler.get() == 0) {
                started = true;
                /* this LinkCrawler instance started, notify static counter */
                synchronized (CRAWLER) {
                    CRAWLER.incrementAndGet();
                }
            }
            crawler.incrementAndGet();
        }
        if (started) {
            EVENTSENDER.fireEvent(new LinkCrawlerEvent(this, LinkCrawlerEvent.Type.STARTED));
        }
        return true;
    }

    protected void crawlDeeper(CrawledLink source) {
        if (!checkStartNotify()) return;
        try {
            if (source == null) return;
            synchronized (duplicateFinderDeep) {
                /* did we already crawlDeeper this url */
                if (!duplicateFinderDeep.add(source.getURL())) { return; }
            }
            Browser br = null;
            try {
                ArrayList<CrawledLink> possibleCryptedLinks = null;
                new URL(source.getURL());
                if (this.isCrawledLinkFiltered(source)) {
                    /* link is filtered, stop here */
                    return;
                }
                br = new Browser();
                String url = source.getURL();
                br.openGetConnection(url);
                if (br.getRedirectLocation() != null) {
                    try {
                        br.getHttpConnection().disconnect();
                    } catch (Throwable e) {
                    }
                    url = br.getRedirectLocation();
                    br.openGetConnection(url);
                }
                if (br.getHttpConnection().isContentDisposition() || (br.getHttpConnection().getContentType() != null && !br.getHttpConnection().getContentType().contains("text"))) {
                    try {
                        br.getHttpConnection().disconnect();
                    } catch (Throwable e) {
                    }
                    /*
                     * downloadable content, we use directhttp and distribute
                     * the url
                     */
                    possibleCryptedLinks = _crawl("directhttp://" + url, null, false);
                } else {
                    /* try to load the webpage and find links on it */
                    br.followConnection();
                    String baseUrl = new Regex(source.getURL(), "(.+)/").getMatch(0);
                    if (baseUrl != null && !baseUrl.endsWith("/")) {
                        baseUrl = baseUrl + "/";
                    }
                    possibleCryptedLinks = _crawl(br.toString(), baseUrl, false);
                }
                if (possibleCryptedLinks != null && possibleCryptedLinks.size() > 0) {
                    crawl(possibleCryptedLinks);
                }
            } catch (Throwable e) {
            } finally {
                try {
                    br.getHttpConnection().disconnect();
                } catch (Throwable e) {
                }
            }
        } finally {
            checkFinishNotify();
        }
    }

    protected void distribute(ArrayList<CrawledLink> possibleCryptedLinks) {
        final int generation = this.getCrawlerGeneration(true);
        if (!checkStartNotify()) return;
        try {
            if (possibleCryptedLinks == null || possibleCryptedLinks.size() == 0) return;
            mainloop: for (final CrawledLink possibleCryptedLink : possibleCryptedLinks) {
                mainloopretry: while (true) {
                    UnknownCrawledLinkHandler unnknownHandler = possibleCryptedLink.getUnknownHandler();
                    possibleCryptedLink.setUnknownHandler(null);
                    if (possibleCryptedLink.gethPlugin() != null) {
                        // direct forward, if we already have a final link.
                        this.handleCrawledLink(possibleCryptedLink);
                        continue mainloop;
                    }
                    String url = possibleCryptedLink.getURL();
                    if (url == null) continue mainloop;

                    if (!url.startsWith("directhttp")) {
                        /*
                         * first we will walk through all available container
                         * plugins
                         */
                        for (final PluginsC pCon : ContainerPluginController.getInstance().list()) {
                            if (pCon.canHandle(url)) {
                                try {
                                    ArrayList<CrawledLink> allPossibleCryptedLinks = pCon.getContainerLinks(url);
                                    if (allPossibleCryptedLinks != null) {
                                        for (final CrawledLink decryptThis : allPossibleCryptedLinks) {
                                            /*
                                             * forward important data to new
                                             * ones
                                             */
                                            forwardCrawledLinkInfos(possibleCryptedLink, decryptThis);
                                            if (possibleCryptedLink.getCryptedLink() != null) {
                                                /*
                                                 * source contains CryptedLink,
                                                 * so lets forward important
                                                 * infos
                                                 */
                                                HashMap<String, Object> props = possibleCryptedLink.getCryptedLink().getProperties();
                                                if (props != null && !props.isEmpty()) {
                                                    decryptThis.getCryptedLink().setProperties(new HashMap<String, Object>(props));
                                                }
                                                decryptThis.getCryptedLink().setDecrypterPassword(possibleCryptedLink.getCryptedLink().getDecrypterPassword());
                                            }

                                            if (insideDecrypterPlugin()) {
                                                /*
                                                 * direct decrypt this link
                                                 * because we are already inside
                                                 * a LinkCrawlerThread and this
                                                 * avoids deadlocks on plugin
                                                 * waiting for linkcrawler
                                                 * results
                                                 */
                                                container(decryptThis);
                                            } else {
                                                /*
                                                 * enqueue this cryptedLink for
                                                 * decrypting
                                                 */
                                                if (!checkStartNotify()) return;
                                                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {

                                                    @Override
                                                    void crawling() {
                                                        container(decryptThis);
                                                    }
                                                });

                                            }
                                        }
                                    }
                                } catch (Throwable e) {
                                    Log.exception(e);
                                }
                                continue mainloop;
                            }
                        }
                        /*
                         * first we will walk through all available decrypter
                         * plugins
                         */
                        for (final LazyCrawlerPlugin pDecrypt : CrawlerPluginController.getInstance().list()) {
                            if (pDecrypt.canHandle(url)) {
                                try {
                                    PluginForDecrypt plg = pDecrypt.getPrototype();
                                    if (plg != null) {
                                        ArrayList<CrawledLink> allPossibleCryptedLinks = plg.getCrawlableLinks(url);
                                        if (allPossibleCryptedLinks != null) {
                                            for (final CrawledLink decryptThis : allPossibleCryptedLinks) {
                                                /*
                                                 * forward important data to new
                                                 * ones
                                                 */
                                                forwardCrawledLinkInfos(possibleCryptedLink, decryptThis);
                                                if (possibleCryptedLink.getCryptedLink() != null) {
                                                    /*
                                                     * source contains
                                                     * CryptedLink, so lets
                                                     * forward important infos
                                                     */
                                                    HashMap<String, Object> props = possibleCryptedLink.getCryptedLink().getProperties();
                                                    if (props != null && !props.isEmpty()) {
                                                        decryptThis.getCryptedLink().setProperties(new HashMap<String, Object>(props));
                                                    }
                                                    decryptThis.getCryptedLink().setDecrypterPassword(possibleCryptedLink.getCryptedLink().getDecrypterPassword());
                                                }

                                                if (insideDecrypterPlugin()) {
                                                    /*
                                                     * direct decrypt this link
                                                     * because we are already
                                                     * inside a
                                                     * LinkCrawlerThread and
                                                     * this avoids deadlocks on
                                                     * plugin waiting for
                                                     * linkcrawler results
                                                     */
                                                    crawl(decryptThis);
                                                } else {
                                                    /*
                                                     * enqueue this cryptedLink
                                                     * for decrypting
                                                     */
                                                    if (!checkStartNotify()) return;
                                                    threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {

                                                        public long getAverageRuntime() {
                                                            return pDecrypt.getAverageCrawlRuntime();
                                                        }

                                                        @Override
                                                        void crawling() {
                                                            LinkCrawler.this.crawl(decryptThis);
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    }
                                } catch (Throwable e) {
                                    Log.exception(e);
                                }
                                continue mainloop;
                            }
                        }
                    }
                    /* now we will walk through all available hoster plugins */
                    for (final LazyHostPlugin pHost : HostPluginController.getInstance().list()) {
                        if (pHost.canHandle(url)) {
                            try {
                                PluginForHost plg = pHost.getPrototype();
                                if (plg != null) {
                                    if (ftp != null && ftp == plg) {
                                        /*
                                         * generic ftp handling is done at the
                                         * end
                                         */
                                        continue;
                                    }
                                    FilePackage sourcePackage = null;
                                    if (possibleCryptedLink.getDownloadLink() != null) {
                                        sourcePackage = possibleCryptedLink.getDownloadLink().getFilePackage();
                                    }
                                    ArrayList<DownloadLink> hosterLinks = plg.getDownloadLinks(url, sourcePackage);
                                    if (hosterLinks != null) {
                                        forwardDownloadLinkInfos(possibleCryptedLink.getDownloadLink(), hosterLinks);
                                        for (DownloadLink hosterLink : hosterLinks) {
                                            CrawledLink link = new CrawledLink(hosterLink);
                                            /*
                                             * forward important data to new
                                             * ones
                                             */
                                            forwardCrawledLinkInfos(possibleCryptedLink, link);
                                            handleCrawledLink(link);
                                        }
                                    }
                                }
                            } catch (Throwable e) {
                                Log.exception(e);
                            }
                            continue mainloop;
                        }
                    }
                    if (unnknownHandler != null) {
                        /*
                         * CrawledLink is unhandled till now , but has an
                         * UnknownHandler set, lets call it, maybe it makes the
                         * Link handable by a Plugin
                         */
                        try {
                            unnknownHandler.unhandledCrawledLink(possibleCryptedLink, this);
                        } catch (final Throwable e) {
                            Log.exception(e);
                        }
                        /* lets retry this crawledLink */
                        continue mainloopretry;
                    }
                    /* now we will check for normal http links */
                    if (directHTTP != null) {
                        url = url.replaceFirst("http://", "httpviajd://");
                        url = url.replaceFirst("https://", "httpsviajd://");
                        if (directHTTP.canHandle(url)) {
                            try {
                                FilePackage sourcePackage = null;
                                if (possibleCryptedLink.getDownloadLink() != null) {
                                    sourcePackage = possibleCryptedLink.getDownloadLink().getFilePackage();
                                }
                                ArrayList<DownloadLink> httpLinks = directHTTP.getDownloadLinks(url, sourcePackage);
                                if (httpLinks != null) {
                                    forwardDownloadLinkInfos(possibleCryptedLink.getDownloadLink(), httpLinks);
                                    for (DownloadLink hosterLink : httpLinks) {
                                        CrawledLink link = new CrawledLink(hosterLink);
                                        /* forward important data to new ones */
                                        forwardCrawledLinkInfos(possibleCryptedLink, link);
                                        handleCrawledLink(link);
                                    }
                                }
                            } catch (Throwable e) {
                                Log.exception(e);
                            }
                            continue mainloop;
                        }
                    }
                    /* now we will check for generic ftp links */
                    if (ftp != null) {
                        if (ftp.canHandle(url)) {
                            try {
                                FilePackage sourcePackage = null;
                                if (possibleCryptedLink.getDownloadLink() != null) {
                                    sourcePackage = possibleCryptedLink.getDownloadLink().getFilePackage();
                                }
                                ArrayList<DownloadLink> httpLinks = ftp.getDownloadLinks(url, sourcePackage);
                                if (httpLinks != null) {
                                    forwardDownloadLinkInfos(possibleCryptedLink.getDownloadLink(), httpLinks);
                                    for (DownloadLink hosterLink : httpLinks) {
                                        CrawledLink link = new CrawledLink(hosterLink);
                                        /* forward important data to new ones */
                                        forwardCrawledLinkInfos(possibleCryptedLink, link);
                                        handleCrawledLink(link);
                                    }
                                }
                            } catch (Throwable e) {
                                Log.exception(e);
                            }
                            continue mainloop;
                        }
                    }
                    if (possibleCryptedLink.isCrawlDeep()) {
                        /* the link is allowed to crawlDeep */
                        if (insideDecrypterPlugin()) {
                            crawlDeeper(possibleCryptedLink);
                        } else {
                            if (!checkStartNotify()) return;
                            threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {

                                @Override
                                void crawling() {
                                    crawlDeeper(possibleCryptedLink);
                                }
                            });
                        }
                        continue mainloop;
                    }
                    /* break for mainloopretry */
                    break;
                }
            }
        } finally {
            checkFinishNotify();
        }
    }

    private void forwardCrawledLinkInfos(CrawledLink source, CrawledLink dest) {
        if (source == null || dest == null) return;
        dest.setSourceLink(source);
        dest.setMatchingFilter(source.getMatchingFilter());
        dest.setSourceJob(source.getSourceJob());
        dest.setDesiredPackageInfo(source.getDesiredPackageInfo());
        convertFilePackageInfos(dest);
        permanentOffline(dest);
    }

    private PackageInfo convertFilePackageInfos(CrawledLink link) {
        if (link.getDownloadLink() != null && !FilePackage.isDefaultFilePackage(link.getDownloadLink().getFilePackage())) {
            PackageInfo fpi = link.getDesiredPackageInfo();
            if (fpi == null) fpi = new PackageInfo();
            FilePackage dp = link.getDownloadLink().getFilePackage();

            if (dp.getDownloadDirectory() != null && !dp.getDownloadDirectory().equals(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder())) {
                // do not set downloadfolder if it is the defaultfolder
                fpi.setDestinationFolder(dp.getDownloadDirectory());
            }

            fpi.setAutoExtractionEnabled(dp.isPostProcessing());
            fpi.setName(LinknameCleaner.cleanFileName(dp.getName()));
            if (Boolean.FALSE.equals(dp.getBooleanProperty(ALLOW_MERGE, false))) {
                fpi.setUniqueId(dp.getUniqueID());
            }
            for (String s : dp.getPasswordList()) {
                fpi.getExtractionPasswords().add(s);
            }
            link.setDesiredPackageInfo(fpi);
            return fpi;
        }
        return null;
    }

    private void permanentOffline(CrawledLink link) {
        DownloadLink dl = link.getDownloadLink();
        try {
            if (dl != null && dl.getDefaultPlugin().getLazyP().getClassname().contains("Offline")) {
                PackageInfo dpi = link.getDesiredPackageInfo();
                if (dpi == null) {
                    dpi = new PackageInfo();
                    link.setDesiredPackageInfo(dpi);
                }
                dpi.setUniqueId(PERMANENT_OFFLINE_ID);
            }
        } catch (final Throwable e) {
        }

    }

    @SuppressWarnings("deprecation")
    protected void forwardDownloadLinkInfos(DownloadLink source, List<DownloadLink> dests) {
        if (source == null || dests == null || dests.size() == 0) return;
        // source.getFilePackage().remove(source);
        for (DownloadLink dl : dests) {
            dl.addSourcePluginPasswordList(source.getSourcePluginPasswordList());
            dl.setSourcePluginComment(source.getComment());
            dl.setName(source.getName());
            dl.forceFileName(source.getForcedFileName());
            dl.setFinalFileName(source.getFinalFileName());
            if (source.gotBrowserUrl()) dl.setBrowserUrl(source.getBrowserUrl());
            if (source.isAvailabilityStatusChecked()) {
                dl.setAvailable(source.isAvailable());
            }
            HashMap<String, Object> props = source.getProperties();
            if (props != null && !props.isEmpty()) {
                dl.setProperties(new HashMap<String, Object>(props));
            }
            dl.getLinkStatus().setStatusText(source.getLinkStatus().getStatusString());
            dl.setDownloadSize(source.getDownloadSize());
        }
    }

    public boolean isCrawlingAllowed() {
        return this.allowCrawling;
    }

    public void setCrawlingAllowed(boolean b) {
        this.allowCrawling = b;
    }

    public void stopCrawling() {
        crawlerGeneration.incrementAndGet();
    }

    public boolean waitForCrawling() {
        while (crawler.get() > 0) {
            synchronized (LinkCrawler.this) {
                try {
                    LinkCrawler.this.wait(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        return crawler.get() == 0;
    }

    public boolean isRunning() {
        return crawler.get() > 0;
    }

    public static boolean isCrawling() {
        return CRAWLER.get() > 0;
    }

    protected void container(final CrawledLink cryptedLink) {
        final int generation = this.getCrawlerGeneration(true);
        if (!checkStartNotify()) return;
        try {
            synchronized (duplicateFinderContainer) {
                /* did we already decrypt this crypted link? */
                if (!duplicateFinderContainer.add(cryptedLink.getURL())) { return; }
            }
            if (this.isCrawledLinkFiltered(cryptedLink)) {
                /* link is filtered, stop here */
                return;
            }
            if (cryptedLink.getcPlugin() == null || cryptedLink.getURL() == null) return;
            PluginsC plg = null;
            try {
                plg = cryptedLink.getcPlugin().getClass().newInstance();
            } catch (final Throwable e) {
                return;
            }
            /* now we run the plugin and let it find some links */
            LinkCrawlerThread lct = null;
            if (Thread.currentThread() instanceof LinkCrawlerThread) {
                lct = (LinkCrawlerThread) Thread.currentThread();
            }
            boolean lctb = false;
            LinkCrawler previousCrawler = null;
            try {
                if (lct != null) {
                    /* mark thread to be used by decrypter plugin */
                    lctb = lct.isLinkCrawlerThreadUsedbyDecrypter();
                    lct.setLinkCrawlerThreadUsedbyDecrypter(true);
                    previousCrawler = lct.getCurrentLinkCrawler();
                    lct.setCurrentLinkCrawler(this);
                }
                final ArrayList<CrawledLink> decryptedPossibleLinks = plg.decryptContainer(cryptedLink);
                if (decryptedPossibleLinks != null && decryptedPossibleLinks.size() > 0) {
                    /* we found some links, distribute them */
                    for (CrawledLink decryptedPossibleLink : decryptedPossibleLinks) {
                        forwardCrawledLinkInfos(cryptedLink, decryptedPossibleLink);
                    }
                    if (!checkStartNotify()) return;
                    /* enqueue distributing of the links */
                    threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {

                        @Override
                        void crawling() {
                            LinkCrawler.this.distribute(decryptedPossibleLinks);
                        }
                    });
                }
            } finally {
                if (lct != null) {
                    /* reset thread to last known used state */
                    lct.setLinkCrawlerThreadUsedbyDecrypter(lctb);
                    lct.setCurrentLinkCrawler(previousCrawler);
                }
            }
        } finally {
            checkFinishNotify();
        }
    }

    protected void crawl(final CrawledLink cryptedLink) {
        final int generation = this.getCrawlerGeneration(true);
        if (!checkStartNotify()) return;
        ClassLoader oldClassLoader = null;
        try {
            synchronized (duplicateFinderCrawler) {
                /* did we already decrypt this crypted link? */
                if (!duplicateFinderCrawler.add(cryptedLink.getURL())) { return; }
            }
            if (this.isCrawledLinkFiltered(cryptedLink)) {
                /* link is filtered, stop here */
                return;
            }
            if (cryptedLink.getdPlugin() == null || cryptedLink.getCryptedLink() == null) return;
            PluginForDecrypt oplg = cryptedLink.getdPlugin();
            PluginForDecrypt wplg = oplg.getNewInstance();
            wplg.setIOPermission(this);
            wplg.setBrowser(new Browser());
            wplg.setLogger(new JDPluginLogger(cryptedLink.getURL()));
            /* now we run the plugin and let it find some links */
            LinkCrawlerThread lct = null;
            if (Thread.currentThread() instanceof LinkCrawlerThread) {
                lct = (LinkCrawlerThread) Thread.currentThread();
            }
            boolean lctb = false;
            LinkCrawlerDistributer dist = null;
            LinkCrawler previousCrawler = null;
            ArrayList<DownloadLink> decryptedPossibleLinks = null;

            try {
                final ArrayList<CrawledLink> distributedLinks = new ArrayList<CrawledLink>();
                final boolean useDelay = wplg.getDistributeDelayerMinimum() > 0;
                int minimumDelay = Math.max(10, wplg.getDistributeDelayerMinimum());
                int maximumDelay = wplg.getDistributeDelayerMaximum();
                if (maximumDelay == 0) {
                    maximumDelay = -1;
                }
                final DelayedRunnable distributeLinksDelayer = new DelayedRunnable(IOEQ.TIMINGQUEUE, minimumDelay, maximumDelay) {

                    @Override
                    public void delayedrun() {
                        synchronized (distributedLinks) {
                            if (distributedLinks.size() == 0) return;
                            final ArrayList<CrawledLink> distributeThis = new ArrayList<CrawledLink>(distributedLinks);
                            distributedLinks.clear();
                            if (!checkStartNotify()) { return; }
                            /* enqueue distributing of the links */
                            threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {

                                @Override
                                void crawling() {
                                    LinkCrawler.this.distribute(distributeThis);
                                }
                            });
                        }
                    }
                };
                /*
                 * set LinkCrawlerDistributer in case the plugin wants to add
                 * links in realtime
                 */
                wplg.setDistributer(dist = new LinkCrawlerDistributer() {

                    CrawledLinkModifier lm = new CrawledLinkModifier() {
                                               /*
                                                * this modifier sets the
                                                * BrowserURL if not set yet
                                                */
                                               public void modifyCrawledLink(CrawledLink link) {
                                                   DownloadLink dl = link.getDownloadLink();
                                                   if (dl != null && !dl.gotBrowserUrl()) {
                                                       dl.setBrowserUrl(cryptedLink.getURL());
                                                   }
                                               }
                                           };

                    public void distribute(DownloadLink... links) {
                        if (links == null || links.length == 0) return;
                        final ArrayList<CrawledLink> possibleCryptedLinks = new ArrayList<CrawledLink>(links.length);
                        for (DownloadLink link : links) {
                            CrawledLink ret;
                            possibleCryptedLinks.add(ret = new CrawledLink(link));
                            ret.setCustomCrawledLinkModifier(lm);
                            forwardCrawledLinkInfos(cryptedLink, ret);
                        }
                        if (useDelay) {
                            /* we delay the distribute */
                            synchronized (distributedLinks) {
                                /* synchronized adding */
                                distributedLinks.addAll(possibleCryptedLinks);
                            }
                            /* restart delayer to distribute links */
                            distributeLinksDelayer.run();
                        } else {
                            /* we do not delay the distribute */
                            if (!checkStartNotify()) { return; }
                            /* enqueue distributing of the links */
                            threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {

                                @Override
                                void crawling() {
                                    LinkCrawler.this.distribute(possibleCryptedLinks);
                                }
                            });
                        }
                    }
                });
                if (lct != null) {
                    /* mark thread to be used by decrypter plugin */
                    lctb = lct.isLinkCrawlerThreadUsedbyDecrypter();
                    lct.setLinkCrawlerThreadUsedbyDecrypter(true);
                    previousCrawler = lct.getCurrentLinkCrawler();
                    lct.setCurrentLinkCrawler(this);
                }
                long startTime = System.currentTimeMillis();
                oldClassLoader = Thread.currentThread().getContextClassLoader();
                /*
                 * make sure the current Thread uses the PluginClassLoaderChild
                 * of the Plugin in use
                 */
                Thread.currentThread().setContextClassLoader(oplg.getLazyC().getClassLoader());
                try {
                    decryptedPossibleLinks = wplg.decryptLink(cryptedLink);
                } finally {
                    distributeLinksDelayer.stop();
                    /* make sure we dont have any unprocessed delayed Links */
                    distributeLinksDelayer.delayedrun();
                }
                long endTime = System.currentTimeMillis() - startTime;
                oplg.getLazyC().updateCrawlRuntime(endTime);
            } finally {
                if (lct != null) {
                    /* reset thread to last known used state */
                    lct.setLinkCrawlerThreadUsedbyDecrypter(lctb);
                    lct.setCurrentLinkCrawler(previousCrawler);
                }
                /* remove distributer from plugin */
                wplg.setDistributer(null);
                /* restore old ClassLoader for current Thread */
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
            BrokenCrawlerHandler brokenCrawler = cryptedLink.getBrokenCrawlerHandler();
            cryptedLink.setBrokenCrawlerHandler(null);
            if (decryptedPossibleLinks != null) {
                dist.distribute(decryptedPossibleLinks.toArray(new DownloadLink[decryptedPossibleLinks.size()]));
            } else if (brokenCrawler != null) {
                try {
                    brokenCrawler.brokenCrawler(cryptedLink, this);
                } catch (final Throwable e) {
                    Log.exception(e);
                }
            }
        } finally {
            checkFinishNotify();
        }
    }

    public ArrayList<CrawledLink> getCrawledLinks() {
        return crawledLinks;
    }

    public ArrayList<CrawledLink> getFilteredLinks() {
        return filteredLinks;
    }

    protected void handleCrawledLink(CrawledLink link) {
        if (link == null) return;
        link.setCreated(getCreated());
        if (link.getDownloadLink() != null && link.getDownloadLink().getBooleanProperty("ALLOW_DUPE", false)) {
            /* forward dupeAllow info from DownloadLink to CrawledLinkInfo */
            link.getDownloadLink().setProperty("ALLOW_DUPE", Property.NULL);
        }
        try {
            /* call the general LinkModifier first */
            generalCrawledLinkModifier(link);
        } catch (final Throwable e) {
            Log.exception(e);
        }
        /*
         * build history of this crawledlink so we can call each existing
         * LinkModifier in correct order
         */
        ArrayList<CrawledLink> history = new ArrayList<CrawledLink>();
        CrawledLink source = link.getSourceLink();
        history.add(0, link);
        /* build history */
        while (source != null) {
            history.add(0, source);
            source = source.getSourceLink();
        }
        for (CrawledLink historyLink : history) {
            /* call each LinkModifier from the beginning to this link */
            CrawledLinkModifier customModifier = historyLink.getCustomCrawledLinkModifier();
            if (link == historyLink) {
                /*
                 * remove reference to ModifyHandler, because we never will call
                 * it again
                 */
                link.setCustomCrawledLinkModifier(null);
            }
            if (customModifier != null) {
                try {
                    customModifier.modifyCrawledLink(link);
                } catch (final Throwable e) {
                    Log.exception(e);
                }
            }
        }
        /* check if we already handled this url */
        CrawledLink origin = link.getOriginLink();
        /* specialHandling: Crypted A - > B - > Final C , and A equals C */
        boolean specialHandling = (origin != link) && (origin.getLinkID().equals(link.getLinkID()));
        if (isDoDuplicateFinderFinalCheck()) {
            synchronized (duplicateFinderFinal) {
                if (!duplicateFinderFinal.add(link.getLinkID()) && !specialHandling) { return; }
            }
        }
        if (isCrawledLinkFiltered(link) == false) {
            /* link is not filtered, so we can process it normally */
            crawledLinksCounter.incrementAndGet();
            handler.handleFinalLink(link);
        }
    }

    /* Overwrite this if you want to modify final handled CrawledLinks */
    protected void generalCrawledLinkModifier(CrawledLink link) {

    }

    protected boolean isCrawledLinkFiltered(CrawledLink link) {
        if (filter.dropByUrl(link)) {
            filteredLinksCounter.incrementAndGet();
            handler.handleFilteredLink(link);
            return true;
        }
        return false;
    }

    public int crawledLinksFound() {
        return crawledLinksCounter.get();
    }

    public int filteredLinksFound() {
        return filteredLinksCounter.get();
    }

    protected LinkCrawlerHandler defaulHandlerFactory() {
        return new LinkCrawlerHandler() {

            public void handleFinalLink(CrawledLink link) {
                synchronized (crawledLinks) {
                    crawledLinks.add(link);
                }
            }

            public void handleFilteredLink(CrawledLink link) {
                synchronized (filteredLinks) {
                    filteredLinks.add(link);
                }
            }
        };
    }

    public static LinkCrawlerFilter defaultFilterFactory() {
        return new LinkCrawlerFilter() {

            public boolean dropByUrl(CrawledLink link) {
                return false;
            }

            public boolean dropByFileProperties(CrawledLink link) {
                return false;
            };

        };
    }

    public void setFilter(LinkCrawlerFilter filter) {
        if (filter == null) throw new IllegalArgumentException("filter is null");
        this.filter = filter;
    }

    public LinkCrawlerFilter getFilter() {
        return filter;
    }

    public void setHandler(LinkCrawlerHandler handler) {
        if (handler == null) throw new IllegalArgumentException("handler is null");
        this.handler = handler;
    }

    public LinkCrawlerHandler getHandler() {
        return this.handler;
    }

    /**
     * checks if the given host is allowed to ask for Captcha.
     * 
     * if a parentCrawler does exist, the state of the parentCrawler is returned
     */
    public synchronized boolean isCaptchaAllowed(String hoster) {
        if (this.parentCrawler != null) return this.parentCrawler.isCaptchaAllowed(hoster);
        if (captchaBlockedHoster.contains(null)) return false;
        return !captchaBlockedHoster.contains(hoster);
    }

    /**
     * sets captchaAllowed state for given host.
     * 
     * if a parentCrawler does exist, the state is set in the parentCrawler
     */
    public synchronized void setCaptchaAllowed(String hoster, CAPTCHA mode) {
        if (this.parentCrawler != null) {
            this.parentCrawler.setCaptchaAllowed(hoster, mode);
            return;
        }
        switch (mode) {
        case OK:
            if (hoster != null && hoster.length() > 0) {
                captchaBlockedHoster.remove(hoster);
            } else {
                captchaBlockedHoster.clear();
            }
            break;
        case BLOCKALL:
            captchaBlockedHoster.add(null);
            break;
        case BLOCKHOSTER:
            captchaBlockedHoster.add(hoster);
            break;
        }
    }
}
