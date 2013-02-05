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
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.config.Property;
import jd.controlling.IOEQ;
import jd.controlling.IOPermission;
import jd.controlling.linkcollector.LinknameCleaner;
import jd.http.Browser;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.container.ContainerPluginController;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.GeneralSettings;

public class LinkCrawler implements IOPermission {

    private LazyHostPlugin              directHTTP                  = null;
    private LazyHostPlugin              ftp                         = null;
    private java.util.List<CrawledLink> crawledLinks                = new ArrayList<CrawledLink>();
    private AtomicInteger               crawledLinksCounter         = new AtomicInteger(0);
    private java.util.List<CrawledLink> filteredLinks               = new ArrayList<CrawledLink>();
    private AtomicInteger               filteredLinksCounter        = new AtomicInteger(0);
    private java.util.List<CrawledLink> brokenLinks                 = new ArrayList<CrawledLink>();
    private AtomicInteger               brokenLinksCounter          = new AtomicInteger(0);
    private java.util.List<CrawledLink> unhandledLinks              = new ArrayList<CrawledLink>();
    private AtomicInteger               unhandledLinksCounter       = new AtomicInteger(0);
    private AtomicInteger               crawler                     = new AtomicInteger(0);
    private static AtomicInteger        CRAWLER                     = new AtomicInteger(0);
    private HashSet<String>             duplicateFinderContainer    = new HashSet<String>();
    private HashSet<String>             duplicateFinderCrawler      = new HashSet<String>();
    private HashSet<String>             duplicateFinderFinal        = new HashSet<String>();
    private HashSet<String>             duplicateFinderDeep         = new HashSet<String>();
    private LinkCrawlerHandler          handler                     = null;
    protected static ThreadPoolExecutor threadPool                  = null;

    private HashSet<String>             captchaBlockedHoster        = new HashSet<String>();
    private LinkCrawlerFilter           filter                      = null;
    private volatile boolean            allowCrawling               = true;
    private AtomicInteger               crawlerGeneration           = new AtomicInteger(0);
    private LinkCrawler                 parentCrawler               = null;
    private final long                  created;

    public static final String          PACKAGE_ALLOW_MERGE         = "ALLOW_MERGE";
    public static final String          PACKAGE_CLEANUP_NAME        = "CLEANUP_NAME";
    public static final String          PACKAGE_IGNORE_VARIOUS      = "PACKAGE_IGNORE_VARIOUS";
    public static final UniqueAlltimeID PERMANENT_OFFLINE_ID        = new UniqueAlltimeID();
    private boolean                     doDuplicateFinderFinalCheck = true;
    private List<LazyHostPlugin>        pHosts;

    public boolean isDoDuplicateFinderFinalCheck() {
        if (parentCrawler != null) parentCrawler.isDoDuplicateFinderFinalCheck();
        return doDuplicateFinderFinalCheck;
    }

    /*
     * customized comparator we use to prefer faster decrypter plugins over slower ones
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
                 * our thread factory so we have logger,browser settings available
                 */
                return new LinkCrawlerThread(r);
            }

        }, new ThreadPoolExecutor.AbortPolicy()) {

            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                /*
                 * WORKAROUND for stupid SUN /ORACLE way of "how a threadpool should work" !
                 */
                int working = threadPool.getActiveCount();
                int active = threadPool.getPoolSize();
                int max = threadPool.getMaximumPoolSize();
                if (active < max) {
                    if (working == active) {
                        /*
                         * we can increase max pool size so new threads get started
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
        pHosts = new ArrayList<LazyHostPlugin>(HostPluginController.getInstance().list());
        for (LazyHostPlugin pHost : pHosts) {
            if (directHTTP == null && "http links".equals(pHost.getDisplayName())) {
                /* for direct access to the directhttp plugin */
                directHTTP = pHost;
            }
            if (ftp == null && "ftp".equals(pHost.getDisplayName())) {
                /* for generic ftp sites */
                ftp = pHost;
            }
        }
        if (ftp != null) {
            /* generic ftp handling is done at the end */
            /* remove from list, then we don't have to compare each single plugin each round */
            pHosts.remove(ftp);
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
     * if a parent LinkCrawler does exist and thisGeneration is false, we return the older generation of the parent LinkCrawler or this child
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
                java.util.List<CrawledLink> links = _crawl(text, url, allowDeep);
                crawl(links);
            } else {
                if (!checkStartNotify()) return;
                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {

                    @Override
                    void crawling() {
                        java.util.List<CrawledLink> links = _crawl(text, url, allowDeep);
                        crawl(links);
                    }
                });
                return;
            }
        } finally {
            checkFinishNotify();
        }
    }

    private java.util.List<CrawledLink> _crawl(String text, String url, boolean allowDeep) {
        String[] possibleLinks = HTMLParser.getHttpLinks(text, url);
        if (possibleLinks == null || possibleLinks.length == 0) return null;
        final java.util.List<CrawledLink> possibleCryptedLinks = new ArrayList<CrawledLink>(possibleLinks.length);
        for (String possibleLink : possibleLinks) {
            CrawledLink link;
            possibleCryptedLinks.add(link = crawledLinkFactorybyURL(possibleLink));
            link.setCrawlDeep(allowDeep);
        }
        return possibleCryptedLinks;
    }

    public void crawl(final java.util.List<CrawledLink> possibleCryptedLinks) {
        final int generation = this.getCrawlerGeneration(true);
        if (!checkStartNotify()) return;
        try {
            if (possibleCryptedLinks == null || possibleCryptedLinks.size() == 0) return;
            if (insideDecrypterPlugin()) {
                /*
                 * direct decrypt this link because we are already inside a LinkCrawlerThread and this avoids deadlocks on plugin waiting for linkcrawler
                 * results
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
     * check if all known crawlers are done and notify all waiting listener + cleanup DuplicateFinder
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
            crawlerStopped();
        }
    }

    protected void crawlerStopped() {
    }

    protected void crawlerStarted() {
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
            crawlerStarted();
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
                java.util.List<CrawledLink> possibleCryptedLinks = null;
                new URL(source.getURL());
                if (this.isCrawledLinkFiltered(source)) {
                    /* link is filtered, stop here */
                    return;
                }
                br = new Browser();
                String url = source.getURL();
                br.openGetConnection(url);
                HashSet<String> loopAvoid = new HashSet<String>();
                loopAvoid.add(url);
                for (int i = 0; i < 10; i++) {
                    if (br.getRedirectLocation() != null) {
                        try {
                            br.getHttpConnection().disconnect();
                        } catch (Throwable e) {
                        }
                        url = br.getRedirectLocation();
                        if (loopAvoid.add(url) == false) break;
                        br.openGetConnection(url);
                    } else {
                        break;
                    }
                }
                if (br.getRedirectLocation() == null && (br.getHttpConnection().isContentDisposition() || (br.getHttpConnection().getContentType() != null && !br.getHttpConnection().getContentType().contains("text")))) {
                    try {
                        br.getHttpConnection().disconnect();
                    } catch (Throwable e) {
                    }
                    /*
                     * downloadable content, we use directhttp and distribute the url
                     */
                    possibleCryptedLinks = _crawl("directhttp://" + url, null, false);
                    if (possibleCryptedLinks != null) crawl(possibleCryptedLinks);
                } else {
                    /* try to load the webpage and find links on it */
                    br.followConnection();
                    String baseUrl = new Regex(source.getURL(), "(.+)/").getMatch(0);
                    if (baseUrl != null && !baseUrl.endsWith("/")) {
                        baseUrl = baseUrl + "/";
                    }
                    final String finalBaseUrl = baseUrl;
                    final String browserContent = br.toString();
                    possibleCryptedLinks = _crawl(url, null, false);
                    if (possibleCryptedLinks != null) {
                        if (possibleCryptedLinks.size() == 1) {
                            /* first check if the url itself can be handled */
                            CrawledLink link = possibleCryptedLinks.get(0);
                            link.setUnknownHandler(new UnknownCrawledLinkHandler() {

                                @Override
                                public void unhandledCrawledLink(CrawledLink link, LinkCrawler lc) {
                                    /* unhandled url, lets parse the content on it */
                                    List<CrawledLink> possibleCryptedLinks2 = lc._crawl(browserContent, finalBaseUrl, false);
                                    if (possibleCryptedLinks2 != null && possibleCryptedLinks2.size() > 0) lc.crawl(possibleCryptedLinks2);
                                }
                            });
                        }
                        crawl(possibleCryptedLinks);
                    }

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

    protected void distribute(java.util.List<CrawledLink> possibleCryptedLinks) {
        final int generation = this.getCrawlerGeneration(true);
        if (!checkStartNotify()) return;
        try {
            if (possibleCryptedLinks == null || possibleCryptedLinks.size() == 0) return;
            mainloop: for (final CrawledLink possibleCryptedLink : possibleCryptedLinks) {
                if (generation != this.getCrawlerGeneration(false)) {
                    /* LinkCrawler got aborted! */
                    return;
                }
                mainloopretry: while (true) {
                    UnknownCrawledLinkHandler unnknownHandler = possibleCryptedLink.getUnknownHandler();
                    possibleCryptedLink.setUnknownHandler(null);
                    if (possibleCryptedLink.gethPlugin() != null) {
                        // direct forward, if we already have a final link.
                        this.handleCrawledLink(possibleCryptedLink);
                        continue mainloop;
                    }
                    String url = possibleCryptedLink.getURL();
                    if (url == null) {
                        /* WTF, no URL?! let's continue */
                        continue mainloop;
                    }
                    if (!url.startsWith("directhttp")) {
                        /*
                         * first we will walk through all available container plugins
                         */
                        for (final PluginsC pCon : ContainerPluginController.getInstance().list()) {
                            if (pCon.canHandle(url)) {
                                try {
                                    final java.util.List<CrawledLink> allPossibleCryptedLinks = getCrawlableLinks(pCon.getSupportedLinks(), possibleCryptedLink, new CrawledLinkModifier() {
                                        /*
                                         * set new LinkModifier, hides the url if needed
                                         */
                                        public void modifyCrawledLink(CrawledLink link) {
                                            if (pCon.hideLinks()) {
                                                /* we hide the links */
                                                DownloadLink dl = link.getDownloadLink();
                                                if (dl != null) dl.setLinkType(DownloadLink.LINKTYPE_CONTAINER);
                                            }
                                        }
                                    });
                                    if (allPossibleCryptedLinks != null) {
                                        if (insideDecrypterPlugin()) {
                                            /*
                                             * direct decrypt this link because we are already inside a LinkCrawlerThread and this avoids deadlocks on plugin
                                             * waiting for linkcrawler results
                                             */
                                            for (final CrawledLink decryptThis : allPossibleCryptedLinks) {
                                                if (generation != this.getCrawlerGeneration(false)) {
                                                    /* LinkCrawler got aborted! */
                                                    return;
                                                }
                                                container(pCon, decryptThis);
                                            }
                                        } else {
                                            /*
                                             * enqueue these cryptedLinks for decrypting
                                             */
                                            for (final CrawledLink decryptThis : allPossibleCryptedLinks) {
                                                if (!checkStartNotify()) return;
                                                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {

                                                    @Override
                                                    void crawling() {
                                                        container(pCon, decryptThis);
                                                    }
                                                });
                                            }
                                        }
                                    }
                                } catch (Throwable e) {
                                    LogController.CL().log(e);
                                }
                                continue mainloop;
                            }
                        }
                        /*
                         * first we will walk through all available decrypter plugins
                         */
                        for (final LazyCrawlerPlugin pDecrypt : CrawlerPluginController.getInstance().list()) {
                            if (pDecrypt.canHandle(url)) {
                                try {
                                    final java.util.List<CrawledLink> allPossibleCryptedLinks = getCrawlableLinks(pDecrypt.getPattern(), possibleCryptedLink, null);
                                    if (allPossibleCryptedLinks != null) {
                                        if (insideDecrypterPlugin()) {
                                            /*
                                             * direct decrypt this link because we are already inside a LinkCrawlerThread and this avoids deadlocks on plugin
                                             * waiting for linkcrawler results
                                             */
                                            for (final CrawledLink decryptThis : allPossibleCryptedLinks) {
                                                if (generation != this.getCrawlerGeneration(false)) {
                                                    /* LinkCrawler got aborted! */
                                                    return;
                                                }
                                                crawl(pDecrypt, decryptThis);
                                            }
                                        } else {
                                            /*
                                             * enqueue these cryptedLinks for decrypting
                                             */
                                            for (final CrawledLink decryptThis : allPossibleCryptedLinks) {
                                                if (!checkStartNotify()) return;
                                                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {

                                                    public long getAverageRuntime() {
                                                        return pDecrypt.getAverageCrawlRuntime();
                                                    }

                                                    @Override
                                                    protected Object sequentialLockingObject() {
                                                        return pDecrypt.getDisplayName();
                                                    }

                                                    @Override
                                                    protected int maxConcurrency() {
                                                        return pDecrypt.getMaxConcurrentInstances();
                                                    }

                                                    @Override
                                                    void crawling() {
                                                        crawl(pDecrypt, decryptThis);
                                                    }
                                                });
                                            }
                                        }
                                    }
                                } catch (Throwable e) {
                                    LogController.CL().log(e);
                                }
                                continue mainloop;
                            }
                        }
                    }
                    /* now we will walk through all available hoster plugins */
                    for (final LazyHostPlugin pHost : pHosts) {
                        if (pHost.canHandle(url)) {
                            if (insideDecrypterPlugin()) {
                                if (generation != this.getCrawlerGeneration(false)) {
                                    /* LinkCrawler got aborted! */
                                    return;
                                }
                                processHostPlugin(pHost, possibleCryptedLink);
                            } else {
                                if (!checkStartNotify()) return;
                                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {
                                    @Override
                                    public long getAverageRuntime() {
                                        return pHost.getAverageParseRuntime();
                                    }

                                    @Override
                                    void crawling() {
                                        processHostPlugin(pHost, possibleCryptedLink);
                                    }
                                });
                            }
                            continue mainloop;
                        }
                    }
                    if (unnknownHandler != null) {
                        /*
                         * CrawledLink is unhandled till now , but has an UnknownHandler set, lets call it, maybe it makes the Link handable by a Plugin
                         */
                        try {
                            unnknownHandler.unhandledCrawledLink(possibleCryptedLink, this);
                        } catch (final Throwable e) {
                            LogController.CL().log(e);
                        }
                        /* lets retry this crawledLink */
                        continue mainloopretry;
                    }
                    /* now we will check for normal http links */
                    if (directHTTP != null) {
                        url = url.replaceFirst("http://", "httpviajd://");
                        url = url.replaceFirst("https://", "httpsviajd://");
                        /* create new CrawledLink that holds the modified CrawledLink */
                        DownloadLink dl = possibleCryptedLink.getDownloadLink();
                        final CrawledLink modifiedPossibleCryptedLink;
                        if (dl != null) {
                            modifiedPossibleCryptedLink = new CrawledLink(new DownloadLink(dl.getDefaultPlugin(), dl.getName(), dl.getHost(), url, dl.isEnabled()));
                            /* forward downloadLink infos from source to dest */
                            java.util.List<DownloadLink> dlLinks = new ArrayList<DownloadLink>();
                            dlLinks.add(modifiedPossibleCryptedLink.getDownloadLink());
                            forwardDownloadLinkInfos(dl, dlLinks);
                        } else {
                            modifiedPossibleCryptedLink = new CrawledLink(url);
                        }
                        forwardCrawledLinkInfos(possibleCryptedLink, modifiedPossibleCryptedLink);
                        if (directHTTP.canHandle(url)) {
                            if (insideDecrypterPlugin()) {
                                if (generation != this.getCrawlerGeneration(false)) {
                                    /* LinkCrawler got aborted! */
                                    return;
                                }
                                processHostPlugin(directHTTP, modifiedPossibleCryptedLink);
                            } else {
                                if (!checkStartNotify()) return;
                                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {
                                    @Override
                                    public long getAverageRuntime() {
                                        return directHTTP.getAverageParseRuntime();
                                    }

                                    @Override
                                    void crawling() {
                                        processHostPlugin(directHTTP, modifiedPossibleCryptedLink);
                                    }
                                });
                            }
                            continue mainloop;
                        }
                    }
                    /* now we will check for generic ftp links */
                    if (ftp != null) {
                        if (ftp.canHandle(url)) {
                            if (insideDecrypterPlugin()) {
                                if (generation != this.getCrawlerGeneration(false)) {
                                    /* LinkCrawler got aborted! */
                                    return;
                                }
                                processHostPlugin(ftp, possibleCryptedLink);
                            } else {
                                if (!checkStartNotify()) return;
                                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {

                                    @Override
                                    public long getAverageRuntime() {
                                        return ftp.getAverageParseRuntime();
                                    }

                                    @Override
                                    void crawling() {
                                        processHostPlugin(ftp, possibleCryptedLink);
                                    }
                                });
                            }
                            continue mainloop;
                        }
                    }
                    if (possibleCryptedLink.isCrawlDeep()) {
                        /* the link is allowed to crawlDeep */
                        if (insideDecrypterPlugin()) {
                            if (generation != this.getCrawlerGeneration(false)) {
                                /* LinkCrawler got aborted! */
                                return;
                            }
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
                handleUnhandledCryptedLink(possibleCryptedLink);
            }
        } finally {
            checkFinishNotify();
        }
    }

    public java.util.List<CrawledLink> getCrawlableLinks(Pattern pattern, CrawledLink possibleCryptedLink, CrawledLinkModifier modifier) {
        /*
         * we dont need memory optimization here as downloadlink, crypted link itself take care of this
         */
        String[] hits = new Regex(possibleCryptedLink.getURL(), pattern).setMemoryOptimized(false).getColumn(-1);
        java.util.List<CrawledLink> chits = null;
        if (hits != null && hits.length > 0) {
            chits = new ArrayList<CrawledLink>(hits.length);
        } else {
            chits = new ArrayList<CrawledLink>();
        }
        if (hits != null && hits.length > 0) {
            for (String hit : hits) {
                String file = hit;
                file = file.trim();
                /* cut of any unwanted chars */
                while (file.length() > 0 && file.charAt(0) == '"') {
                    file = file.substring(1);
                }
                while (file.length() > 0 && file.charAt(file.length() - 1) == '"') {
                    file = file.substring(0, file.length() - 1);
                }
                file = file.trim();
                CrawledLink cli;
                chits.add(cli = new CrawledLink(new CryptedLink(file)));
                if (modifier != null) cli.setCustomCrawledLinkModifier(modifier);
            }
        }
        for (CrawledLink decryptThis : chits) {
            /*
             * forward important data to new ones
             */
            forwardCrawledLinkInfos(possibleCryptedLink, decryptThis);
            if (possibleCryptedLink.getCryptedLink() != null) {
                /*
                 * source contains CryptedLink, so lets forward important infos
                 */
                HashMap<String, Object> props = possibleCryptedLink.getCryptedLink().getProperties();
                if (props != null && !props.isEmpty()) {
                    decryptThis.getCryptedLink().setProperties(new HashMap<String, Object>(props));
                }
                decryptThis.getCryptedLink().setDecrypterPassword(possibleCryptedLink.getCryptedLink().getDecrypterPassword());
            }
        }
        return chits;
    }

    private void processHostPlugin(LazyHostPlugin pHost, CrawledLink possibleCryptedLink) {
        if (!checkStartNotify()) return;
        ClassLoader oldClassLoader = null;
        try {
            oldClassLoader = Thread.currentThread().getContextClassLoader();
            PluginForHost wplg = null;
            /*
             * use a new PluginClassLoader here
             */
            PluginClassLoaderChild cl;
            Thread.currentThread().setContextClassLoader(cl = PluginClassLoader.getInstance().getChild());
            wplg = pHost.newInstance(cl);
            if (wplg != null) {
                /* now we run the plugin and let it find some links */
                LinkCrawlerThread lct = null;
                if (Thread.currentThread() instanceof LinkCrawlerThread) {
                    lct = (LinkCrawlerThread) Thread.currentThread();
                }
                boolean lctb = false;
                LinkCrawler previousCrawler = null;
                boolean oldDebug = false;
                boolean oldVerbose = false;
                Logger oldLogger = null;
                try {
                    LogSource logger = LogController.getInstance().getLogger(wplg);
                    logger.setAllowTimeoutFlush(false);
                    logger.info("Processing: " + possibleCryptedLink.getURL());
                    if (lct != null) {
                        /* mark thread to be used by crawler plugin */
                        lctb = lct.isLinkCrawlerThreadUsedbyDecrypter();
                        lct.setLinkCrawlerThreadUsedbyDecrypter(true);
                        previousCrawler = lct.getCurrentLinkCrawler();
                        lct.setCurrentLinkCrawler(this);
                        /* save old logger/states */
                        oldLogger = lct.getLogger();
                        oldDebug = lct.isDebug();
                        oldVerbose = lct.isVerbose();
                        /* set new logger and set verbose/debug true */
                        lct.setLogger(logger);
                        lct.setVerbose(true);
                        lct.setDebug(true);
                    }
                    wplg.setIOPermission(this);
                    wplg.setBrowser(new Browser());
                    wplg.setLogger(logger);
                    String url = possibleCryptedLink.getURL();
                    FilePackage sourcePackage = null;
                    if (possibleCryptedLink.getDownloadLink() != null) {
                        sourcePackage = possibleCryptedLink.getDownloadLink().getFilePackage();
                        if (FilePackage.isDefaultFilePackage(sourcePackage)) {
                            /* we don't want the various filePackage getting used */
                            sourcePackage = null;
                        }
                    }
                    long startTime = System.currentTimeMillis();
                    java.util.List<DownloadLink> hosterLinks = null;
                    try {
                        hosterLinks = wplg.getDownloadLinks(url, sourcePackage);
                        /* in case the function returned without exceptions, we can clear log */
                        logger.clear();
                    } finally {
                        long endTime = System.currentTimeMillis() - startTime;
                        wplg.getLazyP().updateParseRuntime(endTime);
                        /* close the logger */
                        logger.close();
                    }
                    if (hosterLinks != null) {
                        forwardDownloadLinkInfos(possibleCryptedLink.getDownloadLink(), hosterLinks);
                        for (DownloadLink hosterLink : hosterLinks) {
                            CrawledLink link = new CrawledLink(hosterLink);
                            /*
                             * forward important data to new ones
                             */
                            forwardCrawledLinkInfos(possibleCryptedLink, link);
                            handleCrawledLink(link);
                        }
                    }
                } finally {
                    if (lct != null) {
                        /* reset thread to last known used state */
                        lct.setLinkCrawlerThreadUsedbyDecrypter(lctb);
                        lct.setCurrentLinkCrawler(previousCrawler);
                        lct.setLogger(oldLogger);
                        lct.setVerbose(oldVerbose);
                        lct.setDebug(oldDebug);
                    }
                }
            } else {
                LogController.CL().info("Hoster Plugin not available:" + pHost.getDisplayName());
            }
        } catch (Throwable e) {
            LogController.CL().log(e);
        } finally {
            /* restore old ClassLoader for current Thread */
            Thread.currentThread().setContextClassLoader(oldClassLoader);
            checkFinishNotify();
        }
    }

    private void forwardCrawledLinkInfos(CrawledLink source, CrawledLink dest) {
        if (source == null || dest == null) return;
        dest.setSourceLink(source);
        dest.setMatchingFilter(source.getMatchingFilter());
        dest.setSourceJob(source.getSourceJob());
        dest.setDesiredPackageInfo(source.getDesiredPackageInfo());

        ArchiveInfo d = dest.getArchiveInfo();
        if (d == null) {
            if (source.getArchiveInfo() != null) dest.setArchiveInfo(new ArchiveInfo().migrate(source.getArchiveInfo()));
        } else {
            d.migrate(source.getArchiveInfo());
        }
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

            if (Boolean.FALSE.equals(dp.getBooleanProperty(PACKAGE_CLEANUP_NAME, true))) {
                fpi.setName(dp.getName());
            } else {
                fpi.setName(LinknameCleaner.cleanFileName(dp.getName(), false, true));
            }

            if (dp.hasProperty(PACKAGE_ALLOW_MERGE)) {
                if (Boolean.FALSE.equals(dp.getBooleanProperty(PACKAGE_ALLOW_MERGE, false))) {
                    fpi.setUniqueId(dp.getUniqueID());
                } else {
                    fpi.setUniqueId(null);
                }
            }
            if (dp.hasProperty(PACKAGE_IGNORE_VARIOUS)) {
                if (Boolean.TRUE.equals(dp.getBooleanProperty(PACKAGE_IGNORE_VARIOUS, false))) {
                    fpi.setIgnoreVarious(true);
                } else {
                    fpi.setIgnoreVarious(false);
                }
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

    protected void forwardDownloadLinkInfos(DownloadLink source, List<DownloadLink> dests) {
        if (source == null || dests == null || dests.size() == 0) return;
        // source.getFilePackage().remove(source);
        for (DownloadLink dl : dests) {
            /* create copy of ArrayList */
            List<String> srcPWs = source.getSourcePluginPasswordList();
            if (srcPWs != null && srcPWs.size() > 0) dl.setSourcePluginPasswordList(new ArrayList<String>(srcPWs));
            dl.setComment(source.getComment());
            dl.setName(source.getName());
            dl.forceFileName(source.getForcedFileName());
            dl.setFinalFileName(source.getFinalFileName());
            if (source.gotBrowserUrl()) dl.setBrowserUrl(source.getBrowserUrl());
            dl.setAvailableStatus(source.getAvailableStatus());
            HashMap<String, Object> props = source.getProperties();
            if (props != null && !props.isEmpty()) {
                dl.setProperties(new HashMap<String, Object>(props));
            }
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

    protected void container(PluginsC oplg, final CrawledLink cryptedLink) {
        final int generation = this.getCrawlerGeneration(true);
        if (!checkStartNotify()) return;
        ClassLoader oldClassLoader = null;
        try {
            oldClassLoader = Thread.currentThread().getContextClassLoader();
            if (oplg == null || cryptedLink.getURL() == null) return;
            synchronized (duplicateFinderContainer) {
                /* did we already decrypt this crypted link? */
                if (!duplicateFinderContainer.add(cryptedLink.getURL())) { return; }
            }
            if (this.isCrawledLinkFiltered(cryptedLink)) {
                /* link is filtered, stop here */
                return;
            }
            /* set new PluginClassLoaderChild because ContainerPlugin maybe uses Hoster/Crawler */
            Thread.currentThread().setContextClassLoader(PluginClassLoader.getInstance().getChild());
            PluginsC plg = null;
            try {
                plg = oplg.getClass().newInstance();
            } catch (final Throwable e) {
                LogController.CL().log(e);
                return;
            }
            /* now we run the plugin and let it find some links */
            LinkCrawlerThread lct = null;
            if (Thread.currentThread() instanceof LinkCrawlerThread) {
                lct = (LinkCrawlerThread) Thread.currentThread();
            }
            boolean lctb = false;
            LinkCrawler previousCrawler = null;
            boolean oldDebug = false;
            boolean oldVerbose = false;
            Logger oldLogger = null;
            try {
                LogSource logger = LogController.getInstance().getLogger(plg.getName());
                logger.setAllowTimeoutFlush(false);
                if (lct != null) {
                    /* mark thread to be used by crawler plugin */
                    lctb = lct.isLinkCrawlerThreadUsedbyDecrypter();
                    lct.setLinkCrawlerThreadUsedbyDecrypter(true);
                    previousCrawler = lct.getCurrentLinkCrawler();
                    lct.setCurrentLinkCrawler(this);
                    /* save old logger/states */
                    oldLogger = lct.getLogger();
                    oldDebug = lct.isDebug();
                    oldVerbose = lct.isVerbose();
                    /* set new logger and set verbose/debug true */
                    lct.setLogger(logger);
                    lct.setVerbose(true);
                    lct.setDebug(true);
                }
                plg.setLogger(logger);
                try {
                    final java.util.List<CrawledLink> decryptedPossibleLinks = plg.decryptContainer(cryptedLink);
                    /* in case the function returned without exceptions, we can clear log */
                    logger.clear();
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
                    /* close the logger */
                    logger.close();
                }
            } finally {
                if (lct != null) {
                    /* reset thread to last known used state */
                    lct.setLinkCrawlerThreadUsedbyDecrypter(lctb);
                    lct.setCurrentLinkCrawler(previousCrawler);
                    lct.setLogger(oldLogger);
                    lct.setVerbose(oldVerbose);
                    lct.setDebug(oldDebug);
                }
            }
        } finally {
            /* restore old ClassLoader for current Thread */
            Thread.currentThread().setContextClassLoader(oldClassLoader);
            checkFinishNotify();
        }
    }

    protected void crawl(LazyCrawlerPlugin lazyC, final CrawledLink cryptedLink) {
        final int generation = this.getCrawlerGeneration(true);
        if (!checkStartNotify()) return;
        ClassLoader oldClassLoader = null;
        try {
            oldClassLoader = Thread.currentThread().getContextClassLoader();
            synchronized (duplicateFinderCrawler) {
                /* did we already decrypt this crypted link? */
                if (!duplicateFinderCrawler.add(cryptedLink.getURL())) { return; }
            }
            if (this.isCrawledLinkFiltered(cryptedLink)) {
                /* link is filtered, stop here */
                return;
            }
            if (lazyC == null || cryptedLink.getCryptedLink() == null) return;
            PluginForDecrypt wplg = null;
            /*
             * we want a fresh pluginClassLoader here
             */
            PluginClassLoaderChild cl;
            Thread.currentThread().setContextClassLoader(cl = PluginClassLoader.getInstance().getChild());
            try {
                wplg = lazyC.newInstance(cl);
            } catch (UpdateRequiredClassNotFoundException e1) {
                LogController.CL().log(e1);
                return;
            }
            wplg.setIOPermission(this);
            wplg.setBrowser(new Browser());
            LogSource logger = null;
            Logger oldLogger = null;
            boolean oldVerbose = false;
            boolean oldDebug = false;
            logger = LogController.getInstance().getLogger(wplg);
            logger.info("Crawling: " + cryptedLink.getURL());
            logger.setAllowTimeoutFlush(false);
            wplg.setLogger(logger);
            /* now we run the plugin and let it find some links */
            LinkCrawlerThread lct = null;
            if (Thread.currentThread() instanceof LinkCrawlerThread) {
                lct = (LinkCrawlerThread) Thread.currentThread();
            }
            boolean lctb = false;
            LinkCrawlerDistributer dist = null;
            DelayedRunnable distributeLinksDelayer = null;
            LinkCrawler previousCrawler = null;
            PluginForDecrypt previousPlugin = null;
            java.util.List<DownloadLink> decryptedPossibleLinks = null;
            try {
                final java.util.List<CrawledLink> distributedLinks = new ArrayList<CrawledLink>();
                final boolean useDelay = wplg.getDistributeDelayerMinimum() > 0;
                int minimumDelay = Math.max(10, wplg.getDistributeDelayerMinimum());
                int maximumDelay = wplg.getDistributeDelayerMaximum();
                if (maximumDelay == 0) {
                    maximumDelay = -1;
                }
                distributeLinksDelayer = new DelayedRunnable(IOEQ.TIMINGQUEUE, minimumDelay, maximumDelay) {

                    @Override
                    public void delayedrun() {
                        /* we are now in IOEQ, thats why we create copy and then push work back into LinkCrawler */
                        java.util.List<CrawledLink> linksToDistribute = null;
                        synchronized (distributedLinks) {
                            if (distributedLinks.size() == 0) return;
                            linksToDistribute = new ArrayList<CrawledLink>(distributedLinks);
                            distributedLinks.clear();
                        }
                        final java.util.List<CrawledLink> linksToDistributeFinal = linksToDistribute;
                        if (!checkStartNotify()) { return; }
                        /* enqueue distributing of the links */
                        threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {

                            @Override
                            void crawling() {
                                final java.util.List<CrawledLink> distributeThis = new ArrayList<CrawledLink>(linksToDistributeFinal);
                                LinkCrawler.this.distribute(distributeThis);
                            }
                        });
                    }
                };
                final DelayedRunnable distributeLinksDelayerFinal = distributeLinksDelayer;
                /*
                 * set LinkCrawlerDistributer in case the plugin wants to add links in realtime
                 */
                wplg.setDistributer(dist = new LinkCrawlerDistributer() {

                    CrawledLinkModifier lm = new CrawledLinkModifier() {
                                               /*
                                                * this modifier sets the BrowserURL if not set yet
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
                        final java.util.List<CrawledLink> possibleCryptedLinks = new ArrayList<CrawledLink>(links.length);
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
                            distributeLinksDelayerFinal.run();
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
                    previousPlugin = lct.getCurrentPlugin();
                    lct.setCurrentPlugin(wplg);
                    lct.setLinkCrawlerThreadUsedbyDecrypter(true);
                    previousCrawler = lct.getCurrentLinkCrawler();
                    lct.setCurrentLinkCrawler(this);
                    /* save old logger/states */
                    oldLogger = lct.getLogger();
                    oldDebug = lct.isDebug();
                    oldVerbose = lct.isVerbose();
                    /* set new logger and set verbose/debug true */
                    lct.setLogger(logger);
                    lct.setVerbose(true);
                    lct.setDebug(true);
                }
                long startTime = System.currentTimeMillis();
                try {
                    wplg.setLinkCrawlerAbort(new LinkCrawlerAbort(generation, this));
                    decryptedPossibleLinks = wplg.decryptLink(cryptedLink);
                    /* in case we return normally, clear the logger */
                    logger.clear();
                } finally {
                    /* close the logger */
                    logger.close();
                    wplg.setLinkCrawlerAbort(null);
                    distributeLinksDelayer.setDelayerEnabled(false);
                    /* make sure we dont have any unprocessed delayed Links */
                    distributeLinksDelayer.delayedrun();
                    long endTime = System.currentTimeMillis() - startTime;
                    lazyC.updateCrawlRuntime(endTime);
                }
            } finally {
                if (lct != null) {
                    /* reset thread to last known used state */
                    lct.setLinkCrawlerThreadUsedbyDecrypter(lctb);
                    lct.setCurrentLinkCrawler(previousCrawler);
                    lct.setLogger(oldLogger);
                    lct.setVerbose(oldVerbose);
                    lct.setDebug(oldDebug);
                    lct.setCurrentPlugin(previousPlugin);
                }
                /* remove distributer from plugin */
                wplg.setDistributer(null);
            }
            BrokenCrawlerHandler brokenCrawler = cryptedLink.getBrokenCrawlerHandler();
            cryptedLink.setBrokenCrawlerHandler(null);
            if (decryptedPossibleLinks != null) {
                dist.distribute(decryptedPossibleLinks.toArray(new DownloadLink[decryptedPossibleLinks.size()]));
            } else {
                this.handleBrokenCrawledLink(cryptedLink);
            }
            if (brokenCrawler != null) {
                try {
                    brokenCrawler.brokenCrawler(cryptedLink, this);
                } catch (final Throwable e) {
                    LogController.CL().log(e);
                }
            }
        } finally {
            /* restore old ClassLoader for current Thread */
            Thread.currentThread().setContextClassLoader(oldClassLoader);
            checkFinishNotify();
        }
    }

    public java.util.List<CrawledLink> getCrawledLinks() {
        return crawledLinks;
    }

    public java.util.List<CrawledLink> getFilteredLinks() {
        return filteredLinks;
    }

    public java.util.List<CrawledLink> getBrokenLinks() {
        return brokenLinks;
    }

    public java.util.List<CrawledLink> getUnhandledLinks() {
        return unhandledLinks;
    }

    protected void handleBrokenCrawledLink(CrawledLink link) {
        this.brokenLinksCounter.incrementAndGet();
        handler.handleBrokenLink(link);
    }

    protected void handleUnhandledCryptedLink(CrawledLink link) {
        this.unhandledLinksCounter.incrementAndGet();
        handler.handleUnHandledLink(link);
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
            LogController.CL().log(e);
        }
        /*
         * build history of this crawledlink so we can call each existing LinkModifier in correct order
         */
        java.util.List<CrawledLink> history = new ArrayList<CrawledLink>();
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
                 * remove reference to ModifyHandler, because we never will call it again
                 */
                link.setCustomCrawledLinkModifier(null);
            }
            if (customModifier != null) {
                try {
                    customModifier.modifyCrawledLink(link);
                } catch (final Throwable e) {
                    LogController.CL().log(e);
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

    public int brokenLinksFound() {
        return brokenLinksCounter.get();
    }

    public int unhandledLinksFound() {
        return unhandledLinksCounter.get();
    }

    public int processedLinks() {
        return crawledLinksCounter.get() + filteredLinksCounter.get() + brokenLinksCounter.get();
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

            @Override
            public void handleBrokenLink(CrawledLink link) {
                synchronized (brokenLinks) {
                    brokenLinks.add(link);
                }
            }

            @Override
            public void handleUnHandledLink(CrawledLink link) {
                synchronized (unhandledLinks) {
                    unhandledLinks.add(link);
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
