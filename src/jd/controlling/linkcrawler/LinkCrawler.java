package jd.controlling.linkcrawler;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jd.config.Property;
import jd.controlling.IOPermission;
import jd.controlling.JDPluginLogger;
import jd.http.Browser;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Regex;
import org.appwork.utils.logging.Log;
import org.jdownloader.plugins.controller.container.ContainerPluginController;
import org.jdownloader.plugins.controller.container.LazyContainerPlugin;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public class LinkCrawler implements IOPermission {

    private static PluginForHost      directHTTP           = null;
    private ArrayList<CrawledLink>    crawledLinks         = new ArrayList<CrawledLink>();
    private AtomicInteger             crawledLinksCounter  = new AtomicInteger(0);
    private ArrayList<CrawledLink>    filteredLinks        = new ArrayList<CrawledLink>();
    private AtomicInteger             filteredLinksCounter = new AtomicInteger(0);
    private AtomicInteger             crawler              = new AtomicInteger(0);
    private HashSet<String>           duplicateFinder      = new HashSet<String>();
    private LinkCrawlerHandler        handler              = null;
    private long                      createdDate          = -1;
    private static ThreadPoolExecutor threadPool           = null;

    private HashSet<String>           captchaBlockedHoster = new HashSet<String>();
    private boolean                   captchaBlockedAll    = false;
    private LinkCrawlerFilter         filter               = null;

    static {
        int maxThreads = Math.max(JsonConfig.create(LinkCrawlerConfig.class).getMaxThreads(), 1);
        int keepAlive = Math.max(JsonConfig.create(LinkCrawlerConfig.class).getThreadKeepAlive(), 100);
        threadPool = new ThreadPoolExecutor(0, maxThreads, keepAlive, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), new ThreadFactory() {

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

        for (LazyHostPlugin pHost : HostPluginController.getInstance().list()) {
            if ("http links".equals(pHost.getDisplayName())) {
                /* for direct access to the directhttp plugin */
                directHTTP = pHost.newInstance();
                break;
            }
        }
    }

    public LinkCrawler() {
        setHandler(defaulHandlerFactory());
        setFilter(defaultFilterFactory());
        this.createdDate = System.currentTimeMillis();
    }

    public void crawlNormal(String text) {
        crawlNormal(text, null);
    }

    public void crawlNormal(String text, String url) {
        String[] possibleLinks = HTMLParser.getHttpLinks(text, url);
        crawlNormal(possibleLinks);
    }

    protected CrawledLink crawledLinkFactorybyURL(String url) {
        return new CrawledLink(url);
    }

    public void crawlNormal(String[] links) {
        if (links == null || links.length == 0) return;
        ArrayList<CrawledLink> possibleCryptedLinks = new ArrayList<CrawledLink>(links.length);
        for (String possibleLink : links) {
            possibleCryptedLinks.add(crawledLinkFactorybyURL(possibleLink));
        }
        distribute(possibleCryptedLinks);
    }

    public void enqueueNormal(String text, String url) {
        String[] possibleLinks = HTMLParser.getHttpLinks(text, url);
        if (possibleLinks == null || possibleLinks.length == 0) return;
        final ArrayList<CrawledLink> possibleCryptedLinks = new ArrayList<CrawledLink>(possibleLinks.length);
        for (String possibleLink : possibleLinks) {
            possibleCryptedLinks.add(crawledLinkFactorybyURL(possibleLink));
        }
        enqueueNormal(possibleCryptedLinks);
    }

    public void enqueueNormal(final ArrayList<CrawledLink> possibleCryptedLinks) {
        if (possibleCryptedLinks == null || possibleCryptedLinks.size() == 0) return;
        checkStartNotify();
        try {
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
                checkStartNotify();
                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this) {
                    public void run() {
                        try {
                            distribute(possibleCryptedLinks);
                        } finally {
                            checkFinishNotify();
                        }
                    }
                });
                return;
            }
        } finally {
            checkFinishNotify();
        }
    }

    public void enqueueDeep(String text, String url) {
        final String[] possibleLinks = HTMLParser.getHttpLinks(text, url);
        if (possibleLinks == null || possibleLinks.length == 0) return;
        checkStartNotify();
        try {
            if (insideDecrypterPlugin()) {
                /*
                 * direct decrypt this link because we are already inside a
                 * LinkCrawlerThread and this avoids deadlocks on plugin waiting
                 * for linkcrawler results
                 */
                crawlDeep(possibleLinks);
                return;
            } else {
                /*
                 * enqueue this cryptedLink for decrypting
                 */
                checkStartNotify();
                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this) {
                    public void run() {
                        try {
                            crawlDeep(possibleLinks);
                        } finally {
                            checkFinishNotify();
                        }
                    }
                });
                return;
            }
        } finally {
            checkFinishNotify();
        }
    }

    public void crawlDeep(String text, String url) {
        crawlDeep(HTMLParser.getHttpLinks(text, url));
    }

    public void crawlDeep(String text) {
        crawlDeep(text, null);
    }

    public void crawlDeep(String[] links) {
        if (links == null || links.length == 0) return;
        final ArrayList<CrawledLink> possibleCryptedLinks = new ArrayList<CrawledLink>(links.length);
        for (String possibleLink : links) {
            possibleCryptedLinks.add(crawledLinkFactorybyURL(possibleLink));
        }
        enqueueDeep(possibleCryptedLinks);
    }

    public void enqueueDeep(final ArrayList<CrawledLink> possibleCryptedLinks) {
        if (possibleCryptedLinks == null || possibleCryptedLinks.size() == 0) return;
        checkStartNotify();
        try {
            for (final CrawledLink link : possibleCryptedLinks) {
                if (insideDecrypterPlugin()) {
                    /*
                     * direct decrypt this link because we are already inside a
                     * LinkCrawlerThread and this avoids deadlocks on plugin
                     * waiting for linkcrawler results
                     */
                    crawlDeeper(link);
                } else {
                    /*
                     * enqueue this cryptedLink for decrypting
                     */
                    checkStartNotify();
                    threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this) {
                        public void run() {
                            try {
                                crawlDeeper(link);
                            } finally {
                                checkFinishNotify();
                            }
                        }
                    });
                }
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
    private void checkFinishNotify() {
        boolean stopped = false;
        synchronized (this) {
            if (crawler.decrementAndGet() == 0) {
                synchronized (crawler) {
                    crawler.notifyAll();
                }
                /*
                 * all tasks are done , we can now cleanup our duplicateFinder
                 */
                synchronized (duplicateFinder) {
                    duplicateFinder.clear();
                }
                stopped = true;
            }
        }
        if (stopped) handler.linkCrawlerStopped();
    }

    private void checkStartNotify() {
        boolean started = false;
        synchronized (this) {
            if (crawler.get() == 0) {
                started = true;
            }
            crawler.incrementAndGet();
        }
        if (started) handler.linkCrawlerStarted();
    }

    protected void crawlDeeper(CrawledLink url) {
        checkStartNotify();
        try {
            if (url == null) return;
            synchronized (duplicateFinder) {
                /* did we already crawlDeeper this url */
                if (!duplicateFinder.add(url.getURL())) { return; }
            }
            Browser br = new Browser();
            try {
                new URL(url.getURL());
                if (this.isCrawledLinkFiltered(url)) {
                    /* link is filtered, stop here */
                    return;
                }
                br.openGetConnection(url.getURL());
                if (br.getHttpConnection().isContentDisposition() || (br.getHttpConnection().getContentType() != null && !br.getHttpConnection().getContentType().contains("text"))) {
                    try {
                        br.getHttpConnection().disconnect();
                    } catch (Throwable e) {
                    }
                    /*
                     * downloadable content, we use directhttp and distribute
                     * the url
                     */
                    ArrayList<CrawledLink> links = new ArrayList<CrawledLink>();
                    CrawledLink link;
                    links.add(link = crawledLinkFactorybyURL("directhttp://" + url));
                    forwardCrawledLinkInfos(url, link);
                    distribute(links);
                } else {
                    /* try to load the webpage and find links on it */
                    br.followConnection();
                    String baseUrl = new Regex(url, "(.+)(/|$)").getMatch(0);
                    if (baseUrl != null && !baseUrl.endsWith("/")) {
                        baseUrl = baseUrl + "/";
                    }
                    String[] possibleLinks = HTMLParser.getHttpLinks(br.toString(), baseUrl);
                    if (possibleLinks == null || possibleLinks.length == 0) return;
                    ArrayList<CrawledLink> possibleCryptedLinks = new ArrayList<CrawledLink>(possibleLinks.length);
                    for (String possibleLink : possibleLinks) {
                        CrawledLink link;
                        possibleCryptedLinks.add(link = crawledLinkFactorybyURL(possibleLink));
                        forwardCrawledLinkInfos(url, link);
                    }
                    distribute(possibleCryptedLinks);
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
        checkStartNotify();
        try {
            if (possibleCryptedLinks == null || possibleCryptedLinks.size() == 0) return;
            mainloop: for (final CrawledLink possibleCryptedLink : possibleCryptedLinks) {
                String url = possibleCryptedLink.getURL();
                if (url == null) continue;
                if (!url.startsWith("directhttp")) {
                    /*
                     * first we will walk through all available container
                     * plugins
                     */
                    for (final LazyContainerPlugin pCon : ContainerPluginController.getInstance().list()) {
                        if (pCon.canHandle(url)) {
                            try {
                                PluginsC plg = pCon.getPrototype();
                                if (plg != null) {
                                    ArrayList<CrawledLink> allPossibleCryptedLinks = plg.getContainerLinks(url);
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
                                                checkStartNotify();
                                                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this) {
                                                    public void run() {
                                                        try {
                                                            container(decryptThis);
                                                        } finally {
                                                            checkFinishNotify();
                                                        }
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
                                                crawl(decryptThis);
                                            } else {
                                                /*
                                                 * enqueue this cryptedLink for
                                                 * decrypting
                                                 */
                                                checkStartNotify();
                                                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this) {
                                                    public void run() {
                                                        try {
                                                            crawl(decryptThis);
                                                        } finally {
                                                            checkFinishNotify();
                                                        }
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
                                FilePackage sourcePackage = null;
                                if (possibleCryptedLink.getDownloadLink() != null) {
                                    sourcePackage = possibleCryptedLink.getDownloadLink().getFilePackage();
                                }
                                ArrayList<DownloadLink> hosterLinks = plg.getDownloadLinks(url, sourcePackage);
                                if (hosterLinks != null) {
                                    forwardDownloadLinkInfos(possibleCryptedLink.getDownloadLink(), hosterLinks);
                                    for (DownloadLink hosterLink : hosterLinks) {
                                        CrawledLink link = new CrawledLink(hosterLink);
                                        /* forward important data to new ones */
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
                    }
                }
            }
        } finally {
            checkFinishNotify();
        }
    }

    private void forwardCrawledLinkInfos(CrawledLink source, CrawledLink dest) {
        if (source == null || dest == null) return;
        dest.setParentLink(source);
        dest.setMatchingFilter(source.getMatchingFilter());
        dest.setSourceJob(source.getSourceJob());
    }

    protected void forwardDownloadLinkInfos(DownloadLink source, List<DownloadLink> dests) {
        if (source == null || dests == null || dests.size() == 0) return;
        // source.getFilePackage().remove(source);
        for (DownloadLink dl : dests) {
            dl.addSourcePluginPasswordList(source.getSourcePluginPasswordList());
            dl.setSourcePluginComment(source.getComment());
            dl.setName(source.getName());
            dl.forceFileName(source.getForcedFileName());
            dl.setFinalFileName(source.getFinalFileName());
            dl.setBrowserUrl(source.getBrowserUrl());
            if (source.isAvailabilityStatusChecked()) {
                dl.setAvailable(source.isAvailable());
            }
            HashMap<String, Object> props = source.getProperties();
            if (props != null && !props.isEmpty()) {
                dl.setProperties(new HashMap<String, Object>(props));
            }
            dl.getLinkStatus().setStatusText(source.getLinkStatus().getStatusString());
            dl.setDownloadSize(source.getDownloadSize());
            dl.setSubdirectory(source);
        }
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

    protected void container(final CrawledLink cryptedLink) {
        checkStartNotify();
        try {
            synchronized (duplicateFinder) {
                /* did we already decrypt this crypted link? */
                if (!duplicateFinder.add(cryptedLink.getURL())) { return; }
            }
            if (this.isCrawledLinkFiltered(cryptedLink)) {
                /* link is filtered, stop here */
                return;
            }
            if (cryptedLink == null || cryptedLink.getcPlugin() == null || cryptedLink.getURL() == null) return;
            PluginsC plg = cryptedLink.getcPlugin().getNewInstance();
            plg.setBrowser(new Browser());
            plg.setLogger(new JDPluginLogger(cryptedLink.getURL()));
            /* now we run the plugin and let it find some links */
            LinkCrawlerThread lct = null;
            if (Thread.currentThread() instanceof LinkCrawlerThread) {
                lct = (LinkCrawlerThread) Thread.currentThread();
            }
            boolean lctb = false;
            LinkCrawlerDistributer dist = null;
            ArrayList<DownloadLink> decryptedPossibleLinks = null;
            try {
                /*
                 * set LinkCrawlerDistributer in case the plugin wants to add
                 * links in realtime
                 */
                dist = new LinkCrawlerDistributer() {

                    public void distribute(DownloadLink... links) {
                        if (links == null || links.length == 0) return;
                        final ArrayList<CrawledLink> possibleCryptedLinks = new ArrayList<CrawledLink>(links.length);
                        for (DownloadLink link : links) {
                            /*
                             * we set source url here to hide the original link
                             * if needed
                             */
                            link.setBrowserUrl(cryptedLink.getURL());
                            CrawledLink ret;
                            possibleCryptedLinks.add(ret = new CrawledLink(link));
                            forwardCrawledLinkInfos(cryptedLink, ret);
                        }
                        checkStartNotify();
                        /* enqueue distributing of the links */
                        threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this) {
                            public void run() {
                                try {
                                    LinkCrawler.this.distribute(possibleCryptedLinks);
                                } finally {
                                    checkFinishNotify();
                                }
                            }
                        });
                    }
                };
                if (lct != null) {
                    /* mark thread to be used by decrypter plugin */
                    lctb = lct.isLinkCrawlerThreadUsedbyDecrypter();
                    lct.setLinkCrawlerThreadUsedbyDecrypter(true);
                }
                decryptedPossibleLinks = plg.decryptContainer(cryptedLink);
            } finally {
                if (lct != null) {
                    /* reset thread to last known used state */
                    lct.setLinkCrawlerThreadUsedbyDecrypter(lctb);
                }
            }
            if (decryptedPossibleLinks != null) {
                dist.distribute(decryptedPossibleLinks.toArray(new DownloadLink[decryptedPossibleLinks.size()]));
            }
        } finally {
            checkFinishNotify();
        }
    }

    protected void crawl(final CrawledLink cryptedLink) {
        checkStartNotify();
        try {
            synchronized (duplicateFinder) {
                /* did we already decrypt this crypted link? */
                if (!duplicateFinder.add(cryptedLink.getURL())) { return; }
            }
            if (this.isCrawledLinkFiltered(cryptedLink)) {
                /* link is filtered, stop here */
                return;
            }
            if (cryptedLink == null || cryptedLink.getdPlugin() == null || cryptedLink.getCryptedLink() == null) return;
            PluginForDecrypt plg = cryptedLink.getdPlugin().getNewInstance();
            plg.setIOPermission(this);
            plg.setBrowser(new Browser());
            plg.setLogger(new JDPluginLogger(cryptedLink.getURL()));
            /* now we run the plugin and let it find some links */
            LinkCrawlerThread lct = null;
            if (Thread.currentThread() instanceof LinkCrawlerThread) {
                lct = (LinkCrawlerThread) Thread.currentThread();
            }
            boolean lctb = false;
            LinkCrawlerDistributer dist = null;
            ArrayList<DownloadLink> decryptedPossibleLinks = null;
            try {
                /*
                 * set LinkCrawlerDistributer in case the plugin wants to add
                 * links in realtime
                 */
                plg.setDistributer(dist = new LinkCrawlerDistributer() {

                    public void distribute(DownloadLink... links) {
                        if (links == null || links.length == 0) return;
                        final ArrayList<CrawledLink> possibleCryptedLinks = new ArrayList<CrawledLink>(links.length);
                        for (DownloadLink link : links) {
                            /*
                             * we set source url here to hide the original link
                             * if needed
                             */
                            link.setBrowserUrl(cryptedLink.getURL());
                            CrawledLink ret;
                            possibleCryptedLinks.add(ret = new CrawledLink(link));
                            forwardCrawledLinkInfos(cryptedLink, ret);
                        }
                        checkStartNotify();
                        /* enqueue distributing of the links */
                        threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this) {
                            public void run() {
                                try {
                                    LinkCrawler.this.distribute(possibleCryptedLinks);
                                } finally {
                                    checkFinishNotify();
                                }
                            }
                        });
                    }
                });
                if (lct != null) {
                    /* mark thread to be used by decrypter plugin */
                    lctb = lct.isLinkCrawlerThreadUsedbyDecrypter();
                    lct.setLinkCrawlerThreadUsedbyDecrypter(true);
                }
                decryptedPossibleLinks = plg.decryptLink(cryptedLink);
            } finally {
                if (lct != null) {
                    /* reset thread to last known used state */
                    lct.setLinkCrawlerThreadUsedbyDecrypter(lctb);
                }
                /* remove distributer from plugin */
                plg.setDistributer(null);
            }
            if (decryptedPossibleLinks != null) {
                dist.distribute(decryptedPossibleLinks.toArray(new DownloadLink[decryptedPossibleLinks.size()]));
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
        link.setCreated(createdDate);
        if (link.getDownloadLink() != null && link.getDownloadLink().getBooleanProperty("ALLOW_DUPE", false)) {
            /* forward dupeAllow info from DownloadLink to CrawledLinkInfo */
            link.getDownloadLink().setProperty("ALLOW_DUPE", Property.NULL);
            link.setDupeAllow(true);
        }
        if (link.isDupeAllow() == false) {
            /* check if we already handled this url */
            CrawledLink origin = link.getOriginLink();
            /* specialHandling: Crypted A - > B - > Final C , and A equals C */
            boolean specialHandling = (origin != link) && (origin.getURL().equals(link.getURL()));
            synchronized (duplicateFinder) {
                if (!duplicateFinder.add(link.getURL()) && !specialHandling) { return; }
            }
        }
        if (isCrawledLinkFiltered(link) == false) {
            /* link is not filtered, so we can process it normally */
            crawledLinksCounter.incrementAndGet();
            handler.handleFinalLink(link);
        }
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

            public void linkCrawlerStarted() {
            }

            public void linkCrawlerStopped() {
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

    public synchronized boolean isCaptchaAllowed(String hoster) {
        if (captchaBlockedAll) return false;
        return !captchaBlockedHoster.contains(hoster);
    }

    public synchronized void setCaptchaAllowed(String hoster, CAPTCHA mode) {
        switch (mode) {
        case OK:
            if (hoster != null && hoster.length() > 0) {
                captchaBlockedHoster.remove(hoster);
            } else {
                captchaBlockedHoster.clear();
                captchaBlockedAll = false;
            }
            break;
        case BLOCKALL:
            captchaBlockedAll = true;
            break;
        case BLOCKHOSTER:
            captchaBlockedHoster.add(hoster);
            break;
        }
    }
}
