package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.http.Browser;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.utils.logging.Log;

public class LinkCrawler {

    private static ArrayList<DecryptPluginWrapper> pDecrypts;
    private static ArrayList<HostPluginWrapper>    pHosts;
    private static PluginForHost                   directHTTP      = null;
    private ArrayList<CrawledLinkInfo>             crawledLinks    = new ArrayList<CrawledLinkInfo>();
    private AtomicInteger                          tasks           = new AtomicInteger(0);
    public AtomicInteger                           distributes     = new AtomicInteger(0);
    private HashSet<String>                        duplicateFinder = new HashSet<String>();
    private static ThreadPoolExecutor              threadPool      = null;
    static {
        threadPool = new ThreadPoolExecutor(0, 8, 30, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(), new ThreadFactory() {

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
                if (r instanceof LinkCrawlerRunnable) {
                    ((LinkCrawlerRunnable) r).getLinkCrawler().tasks.incrementAndGet();
                }
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

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                if (r instanceof LinkCrawlerRunnable) {
                    LinkCrawler crawler = ((LinkCrawlerRunnable) r).getLinkCrawler();
                    if (crawler.tasks.decrementAndGet() == 0) {
                        synchronized (crawler) {
                            crawler.notifyAll();
                        }
                        /*
                         * all tasks are done , we can now cleanup our
                         * duplicateFinder
                         */
                        synchronized (crawler.duplicateFinder) {
                            crawler.duplicateFinder.clear();
                        }
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
    }

    public void crawlNormal(String text) {
        crawlNormal(text, null);
    }

    public void crawlNormal(String text, String url) {
        String[] possibleLinks = HTMLParser.getHttpLinks(text, url);
        if (possibleLinks == null || possibleLinks.length == 0) return;
        ArrayList<CrawledLinkInfo> possibleCryptedLinks = new ArrayList<CrawledLinkInfo>(possibleLinks.length);
        for (String possibleLink : possibleLinks) {
            possibleCryptedLinks.add(new CrawledLinkInfo(possibleLink));
        }
        distribute(possibleCryptedLinks);
    }

    public void crawlDeep(String text) {
        crawlDeep(HTMLParser.getHttpLinks(text, null));
    }

    public void crawlDeep(String[] links) {

    }

    protected void distribute(ArrayList<CrawledLinkInfo> possibleCryptedLinks) {
        distributes.incrementAndGet();
        try {
            if (possibleCryptedLinks == null || possibleCryptedLinks.size() == 0) return;
            for (final CrawledLinkInfo possibleCryptedLink : possibleCryptedLinks) {
                String url = possibleCryptedLink.getURL();
                synchronized (duplicateFinder) {
                    if (!duplicateFinder.add(url)) {
                        continue;
                    }
                }
                if (url == null) continue;
                /* first we will walk through all available decrypter plugins */
                for (final DecryptPluginWrapper pDecrypt : pDecrypts) {
                    if (pDecrypt.canHandle(url)) {
                        try {
                            PluginForDecrypt plg = pDecrypt.getPlugin();
                            if (plg != null) {
                                ArrayList<CrawledLinkInfo> allPossibleCryptedLinks = plg.crawlLinks(url);
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
                                        /*
                                         * enqueue this cryptedLink for
                                         * decrypting
                                         */
                                        if (Thread.currentThread() instanceof LinkCrawlerThread && ((LinkCrawlerThread) Thread.currentThread()).isLinkCrawlerThreadUsedbyDecrypter()) {
                                            System.out.println("direct decrypt");
                                            crawl(decryptThis);
                                        } else {
                                            threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this) {
                                                public void run() {
                                                    crawl(decryptThis);
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            Log.exception(e);
                        }
                        continue;
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
                                    for (DownloadLink hosterLink : hosterLinks) {
                                        handleFinalLink(new CrawledLinkInfo(hosterLink));
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            Log.exception(e);
                        }
                        continue;
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
            distributes.decrementAndGet();
            if (distributes.get() == 0) {
                synchronized (LinkCrawler.this) {
                    LinkCrawler.this.notifyAll();
                }
            }
        }
    }

    public boolean waitForCrawling() {
        while (threadPool.getActiveCount() > 0 && (tasks.get() > 0 || distributes.get() > 0)) {
            synchronized (LinkCrawler.this) {
                try {
                    LinkCrawler.this.wait(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        return threadPool.getActiveCount() == 0 || (tasks.get() == 0 && distributes.get() == 0);
    }

    protected void crawl(CrawledLinkInfo cryptedLink) {
        if (cryptedLink == null || cryptedLink.getdPlugin() == null || cryptedLink.getCryptedLink() == null) return;
        /* we have to create new plugin instance here */
        PluginForDecrypt plg = cryptedLink.getdPlugin().getWrapper().getNewPluginInstance();
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
                lctb = lct.isLinkCrawlerThreadUsedbyDecrypter();
                lct.setLinkCrawlerThreadUsedbyDecrypter(true);
            }
            decryptedPossibleLinks = plg.decryptLink(cryptedLink.getCryptedLink());
        } finally {
            if (lct != null) {
                lct.setLinkCrawlerThreadUsedbyDecrypter(lctb);
            }
        }
        if (decryptedPossibleLinks != null && decryptedPossibleLinks.size() > 0) {
            ArrayList<CrawledLinkInfo> possibleCryptedLinks = new ArrayList<CrawledLinkInfo>(decryptedPossibleLinks.size());
            for (DownloadLink decryptedLink : decryptedPossibleLinks) {
                /* we set source url here to hide the original link if needed */
                decryptedLink.setBrowserUrl(cryptedLink.getURL());
                possibleCryptedLinks.add(new CrawledLinkInfo(decryptedLink));
            }
            distribute(possibleCryptedLinks);
        }
    }

    public ArrayList<CrawledLinkInfo> getCrawledLinks() {
        return crawledLinks;
    }

    protected void handleFinalLink(CrawledLinkInfo link) {
        if (link == null) return;
        synchronized (crawledLinks) {
            crawledLinks.add(link);
        }
    }

}
