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

import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.controlling.IOPermission;
import jd.http.Browser;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Regex;
import org.appwork.utils.logging.Log;

public class LinkCrawler implements IOPermission {

    private static ArrayList<DecryptPluginWrapper> pDecrypts;
    private static ArrayList<HostPluginWrapper>    pHosts;
    private static PluginForHost                   directHTTP              = null;
    private ArrayList<CrawledLinkInfo>             crawledLinks            = new ArrayList<CrawledLinkInfo>();
    private AtomicInteger                          crawledLinksCounter     = new AtomicInteger(0);
    private AtomicInteger                          crawler                 = new AtomicInteger(0);
    private HashSet<String>                        duplicateFinder         = new HashSet<String>();
    private LinkCrawlerHandler                     handler                 = null;
    private LinkCrawlerHandler                     defaultFinalLinkHandler = null;
    private long                                   createdDate             = -1;
    private static ThreadPoolExecutor              threadPool              = null;

    private HashSet<String>                        captchaBlockedHoster    = new HashSet<String>();
    private boolean                                captchaBlockedAll       = false;

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
        pDecrypts = DecryptPluginWrapper.getDecryptWrapper();
        pHosts = HostPluginWrapper.getHostWrapper();
        for (HostPluginWrapper pHost : pHosts) {
            if ("http links".equals(pHost.getHost())) {
                /* for direct access to the directhttp plugin */
                directHTTP = pHost.getNewPluginInstance();
                break;
            }
        }
    }

    public LinkCrawler() {
        defaultFinalLinkHandler = defaulHandlerFactory();
        setHandler(defaultFinalLinkHandler);
        this.createdDate = System.currentTimeMillis();
    }

    public void crawlNormal(String text) {
        crawlNormal(text, null);
    }

    public void crawlNormal(String text, String url) {
        String[] possibleLinks = HTMLParser.getHttpLinks(text, url);
        crawlNormal(possibleLinks);
    }

    public void crawlNormal(String[] links) {
        if (links == null || links.length == 0) return;
        ArrayList<CrawledLinkInfo> possibleCryptedLinks = new ArrayList<CrawledLinkInfo>(links.length);
        for (String possibleLink : links) {
            possibleCryptedLinks.add(new CrawledLinkInfo(possibleLink));
        }
        distribute(possibleCryptedLinks);
    }

    public void enqueueNormal(String text, String url) {
        String[] possibleLinks = HTMLParser.getHttpLinks(text, url);
        if (possibleLinks == null || possibleLinks.length == 0) return;
        final ArrayList<CrawledLinkInfo> possibleCryptedLinks = new ArrayList<CrawledLinkInfo>(possibleLinks.length);
        for (String possibleLink : possibleLinks) {
            possibleCryptedLinks.add(new CrawledLinkInfo(possibleLink));
        }
        if (insideDecrypterPlugin()) {
            /*
             * direct decrypt this link because we are already inside a
             * LinkCrawlerThread and this avoids deadlocks on plugin waiting for
             * linkcrawler results
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
    }

    public void enqueueDeep(String text, String url) {
        final String[] possibleLinks = HTMLParser.getHttpLinks(text, url);
        if (possibleLinks == null || possibleLinks.length == 0) return;
        if (insideDecrypterPlugin()) {
            /*
             * direct decrypt this link because we are already inside a
             * LinkCrawlerThread and this avoids deadlocks on plugin waiting for
             * linkcrawler results
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
    }

    public void crawlDeep(String text, String url) {
        crawlDeep(HTMLParser.getHttpLinks(text, url));
    }

    public void crawlDeep(String text) {
        crawlDeep(text, null);
    }

    public void crawlDeep(String[] links) {
        for (final String url : links) {
            if (insideDecrypterPlugin()) {
                /*
                 * direct decrypt this link because we are already inside a
                 * LinkCrawlerThread and this avoids deadlocks on plugin waiting
                 * for linkcrawler results
                 */
                crawlDeeper(url);
            } else {
                /*
                 * enqueue this cryptedLink for decrypting
                 */
                checkStartNotify();
                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this) {
                    public void run() {
                        try {
                            crawlDeeper(url);
                        } finally {
                            checkFinishNotify();
                        }
                    }
                });
            }
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

    protected void crawlDeeper(String url) {
        checkStartNotify();
        try {
            Browser br = new Browser();
            try {
                new URL(url);
                br.openGetConnection(url);
                if (br.getHttpConnection().isContentDisposition() || (br.getHttpConnection().getContentType() != null && !br.getHttpConnection().getContentType().contains("text"))) {
                    try {
                        br.getHttpConnection().disconnect();
                    } catch (Throwable e) {
                    }
                    /*
                     * downloadable content, we use directhttp and distribute
                     * the url
                     */
                    ArrayList<CrawledLinkInfo> links = new ArrayList<CrawledLinkInfo>();
                    links.add(new CrawledLinkInfo("directhttp://" + url));
                    distribute(links);
                } else {
                    /* try to load the webpage and find links on it */
                    br.followConnection();
                    String baseUrl = new Regex(url, "(.+)(/|$)").getMatch(0);
                    if (baseUrl != null && !baseUrl.endsWith("/")) {
                        baseUrl = baseUrl + "/";
                    }
                    crawlNormal(br.toString(), baseUrl);
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

    protected void distribute(ArrayList<CrawledLinkInfo> possibleCryptedLinks) {
        checkStartNotify();
        try {
            if (possibleCryptedLinks == null || possibleCryptedLinks.size() == 0) return;
            mainloop: for (final CrawledLinkInfo possibleCryptedLink : possibleCryptedLinks) {
                String url = possibleCryptedLink.getURL();
                synchronized (duplicateFinder) {
                    if (!duplicateFinder.add(url)) {
                        continue mainloop;
                    }
                }
                if (url == null) continue;
                /* first we will walk through all available decrypter plugins */
                for (final DecryptPluginWrapper pDecrypt : pDecrypts) {
                    if (pDecrypt.canHandle(url)) {
                        try {
                            PluginForDecrypt plg = pDecrypt.getPlugin();
                            if (plg != null) {
                                ArrayList<CrawledLinkInfo> allPossibleCryptedLinks = plg.getCrawlableLinks(url);
                                if (allPossibleCryptedLinks != null) {
                                    for (final CrawledLinkInfo decryptThis : allPossibleCryptedLinks) {
                                        if (possibleCryptedLink.getCryptedLink() != null) {
                                            /*
                                             * source contains CryptedLink, so
                                             * lets forward important infos
                                             */
                                            HashMap<String, Object> props = possibleCryptedLink.getCryptedLink().getProperties();
                                            if (props != null) {
                                                decryptThis.getCryptedLink().setProperties(new HashMap<String, Object>(props));
                                            }
                                            decryptThis.getCryptedLink().setDecrypterPassword(possibleCryptedLink.getCryptedLink().getDecrypterPassword());
                                        }

                                        if (insideDecrypterPlugin()) {
                                            /*
                                             * direct decrypt this link because
                                             * we are already inside a
                                             * LinkCrawlerThread and this avoids
                                             * deadlocks on plugin waiting for
                                             * linkcrawler results
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
                /* now we will walk through all available hoster plugins */
                for (final HostPluginWrapper pHost : pHosts) {
                    if (pHost.canHandle(url)) {
                        try {
                            PluginForHost plg = pHost.getPlugin();
                            if (plg != null) {
                                /* TODO: package handling, info forwarding */
                                ArrayList<DownloadLink> hosterLinks = plg.getDownloadLinks(url, null);
                                if (hosterLinks != null) {
                                    forwardDownloadLinkInfos(possibleCryptedLink.getDownloadLink(), hosterLinks);
                                    for (DownloadLink hosterLink : hosterLinks) {
                                        handleFinalLink(new CrawledLinkInfo(hosterLink));
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
                            /* TODO: package handling, info forwarding */
                            ArrayList<DownloadLink> httpLinks = directHTTP.getDownloadLinks(url, null);
                            if (httpLinks != null) {
                                forwardDownloadLinkInfos(possibleCryptedLink.getDownloadLink(), httpLinks);
                                for (DownloadLink hosterLink : httpLinks) {
                                    handleFinalLink(new CrawledLinkInfo(hosterLink));
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

    protected void forwardDownloadLinkInfos(DownloadLink source, List<DownloadLink> dests) {
        if (source == null || dests == null || dests.size() == 0) return;
        source.getFilePackage().remove(source);
        for (DownloadLink dl : dests) {
            dl.addSourcePluginPasswordList(source.getSourcePluginPasswordList());
            dl.setSourcePluginComment(source.getSourcePluginComment());
            dl.setName(source.getName());
            dl.forceFileName(source.getForcedFileName());
            dl.setFinalFileName(source.getFinalFileName());
            dl.setBrowserUrl(source.getBrowserUrl());
            if (source.isAvailabilityStatusChecked()) {
                dl.setAvailable(source.isAvailable());
            }
            if (!source.getProperties().isEmpty()) {
                dl.setProperties(new HashMap<String, Object>(source.getProperties()));
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

    protected void crawl(CrawledLinkInfo cryptedLink) {
        checkStartNotify();
        try {
            if (cryptedLink == null || cryptedLink.getdPlugin() == null || cryptedLink.getCryptedLink() == null) return;
            /* we have to create new plugin instance here */
            PluginForDecrypt plg = cryptedLink.getdPlugin().getWrapper().getNewPluginInstance();
            plg.setIOPermission(this);
            plg.setBrowser(new Browser());
            /* now we run the plugin and let it find some links */
            LinkCrawlerThread lct = null;
            if (Thread.currentThread() instanceof LinkCrawlerThread) {
                lct = (LinkCrawlerThread) Thread.currentThread();
            }
            ArrayList<DownloadLink> decryptedPossibleLinks = null;
            boolean lctb = false;
            try {
                if (lct != null) {
                    /* mark thread to be used by decrypter plugin */
                    lctb = lct.isLinkCrawlerThreadUsedbyDecrypter();
                    lct.setLinkCrawlerThreadUsedbyDecrypter(true);
                }
                decryptedPossibleLinks = plg.decryptLink(cryptedLink.getCryptedLink());
            } finally {
                if (lct != null) {
                    /* reset thread to last known used state */
                    lct.setLinkCrawlerThreadUsedbyDecrypter(lctb);
                }
            }
            if (decryptedPossibleLinks != null && decryptedPossibleLinks.size() > 0) {
                ArrayList<CrawledLinkInfo> possibleCryptedLinks = new ArrayList<CrawledLinkInfo>(decryptedPossibleLinks.size());
                for (DownloadLink decryptedLink : decryptedPossibleLinks) {
                    /*
                     * we set source url here to hide the original link if
                     * needed
                     */
                    decryptedLink.setBrowserUrl(cryptedLink.getURL());
                    possibleCryptedLinks.add(new CrawledLinkInfo(decryptedLink));
                }
                distribute(possibleCryptedLinks);
            }
        } finally {
            checkFinishNotify();
        }
    }

    public ArrayList<CrawledLinkInfo> getCrawledLinks() {
        return crawledLinks;
    }

    protected void handleFinalLink(CrawledLinkInfo link) {
        link.setCreated(createdDate);
        if (link.getDownloadLink() != null && link.getDownloadLink().getBooleanProperty("ALLOW_DUPE", false)) {
            /* forward dupeAllow info from DownloadLink to CrawledLinkInfo */
            link.getDownloadLink().getProperties().remove("ALLOW_DUPE");
            link.setDupeAllow(true);
        }
        crawledLinksCounter.incrementAndGet();
        handler.handleFinalLink(link);
    }

    public int crawledLinksFound() {
        return crawledLinksCounter.get();
    }

    public LinkCrawlerHandler getDefaultHandler() {
        return defaultFinalLinkHandler;
    }

    protected LinkCrawlerHandler defaulHandlerFactory() {
        return new LinkCrawlerHandler() {

            public void handleFinalLink(CrawledLinkInfo link) {
                if (link == null) return;
                synchronized (crawledLinks) {
                    crawledLinks.add(link);
                }
            }

            public void linkCrawlerStarted() {
            }

            public void linkCrawlerStopped() {
            }
        };
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
            if (hoster != null) {
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
