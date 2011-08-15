package jd.controlling.linkfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.controlling.BrowserSettingsThread;
import jd.http.Browser;
import jd.http.BrowserSettings;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.utils.logging.Log;
import org.appwork.utils.net.httpconnection.HTTPProxy;

public class LinkFinder implements ThreadFactory, BrowserSettings {

    private ThreadPoolExecutor              threadPool;
    private HTTPProxy                       proxy          = null;
    private Logger                          logger;
    private boolean                         debug;
    private boolean                         verbose;
    private ArrayList<DecryptPluginWrapper> pDecrypts;
    private ArrayList<HostPluginWrapper>    pHosts;
    private PluginForHost                   directHTTP     = null;
    private ArrayList<DownloadLink>         decryptedLinks = new ArrayList<DownloadLink>();

    public LinkFinder() {
        this.threadPool = new ThreadPoolExecutor(0, 8, 10, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(), this, new ThreadPoolExecutor.AbortPolicy()) {

            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                /*
                 * WORKAROUND for stupid SUN/ORACLE way of
                 * "how a threadpool should work"!
                 */
                int active = threadPool.getActiveCount();
                if (active < threadPool.getMaximumPoolSize()) {
                    threadPool.setCorePoolSize(Math.max(threadPool.getMaximumPoolSize(), active + 1));
                }
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
            }

        };

        pDecrypts = DecryptPluginWrapper.getDecryptWrapper();
        pHosts = HostPluginWrapper.getHostWrapper();
        for (HostPluginWrapper pHost : pHosts) {
            if ("http links".equals(pHost.getHost())) {
                /* for direct access to the directhttp plugin */
                directHTTP = pHost.getNewPluginInstance();
                break;
            }
        }
        copySettings();
    }

    public Thread newThread(Runnable r) {
        /* our thread factory so we have logger,browser settings available */
        BrowserSettingsThread ret = new BrowserSettingsThread(r);
        ret.setDaemon(true);
        ret.setCurrentProxy(proxy);
        ret.setDebug(debug);
        ret.setVerbose(verbose);
        ret.setLogger(logger);
        return ret;
    }

    public void parse(String text, String url) {
        String[] possibleLinks = HTMLParser.getHttpLinks(text, url);
        if (possibleLinks == null || possibleLinks.length == 0) return;
        ArrayList<CryptedLink> possibleCryptedLinks = new ArrayList<CryptedLink>(possibleLinks.length);
        for (String possibleLink : possibleLinks) {
            possibleCryptedLinks.add(new CryptedLink(possibleLink));
        }
        distribute(possibleCryptedLinks);
    }

    protected void distribute(ArrayList<CryptedLink> possibleCryptedLinks) {
        if (possibleCryptedLinks == null || possibleCryptedLinks.size() == 0) return;
        for (final CryptedLink possibleCryptedLink : possibleCryptedLinks) {
            /* first we will walk through all available decrypter plugins */
            for (final DecryptPluginWrapper pDecrypt : pDecrypts) {
                /* get url from CryptedLink */
                String url = possibleCryptedLink.getCryptedUrl();
                if (url == null && possibleCryptedLink.getDecryptedLink() != null) {
                    /* get url from decrypted DownloadLink if available */
                    url = possibleCryptedLink.getDecryptedLink().getDownloadURL();
                }
                if (pDecrypt.canHandle(url)) {
                    try {
                        PluginForDecrypt plg = pDecrypt.getPlugin();
                        if (plg != null) {
                            ArrayList<CryptedLink> allPossibleCryptedLinks = plg.getDecryptableLinks(url);
                            if (allPossibleCryptedLinks != null) {
                                for (final CryptedLink decryptThis : allPossibleCryptedLinks) {
                                    /* set plugin to use for decrypting */
                                    decryptThis.setPlugin(plg);
                                    /* clone infos from source link */
                                    HashMap<String, Object> props = possibleCryptedLink.getProperties();
                                    if (props != null) {
                                        decryptThis.setProperties(new HashMap<String, Object>(props));
                                    }
                                    decryptThis.setDecrypterPassword(possibleCryptedLink.getDecrypterPassword());
                                    /*
                                     * enqueue this cryptedLink for decrypting
                                     */
                                    threadPool.execute(new Runnable() {
                                        public void run() {
                                            decrypt(decryptThis);
                                        }
                                    });
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
                /* get url from CryptedLink */
                String url = possibleCryptedLink.getCryptedUrl();
                if (url == null && possibleCryptedLink.getDecryptedLink() != null) {
                    /* get url from decrypted DownloadLink if available */
                    url = possibleCryptedLink.getDecryptedLink().getDownloadURL();
                }
                if (url != null && pHost.canHandle(url)) {
                    try {
                        PluginForHost plg = pHost.getPlugin();
                        if (plg != null) {
                            /* TODO: package handling, info forwarding */
                            ArrayList<DownloadLink> hosterLinks = plg.getDownloadLinks(url, null);
                            if (hosterLinks != null) {
                                for (DownloadLink hosterLink : hosterLinks) {
                                    handleFinalLink(hosterLink);
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
                String url = possibleCryptedLink.getCryptedUrl();
                if (url == null && possibleCryptedLink.getDecryptedLink() != null) {
                    /* get url from decrypted DownloadLink if available */
                    url = possibleCryptedLink.getDecryptedLink().getDownloadURL();
                }
                if (url != null) {
                    url = url.replaceFirst("http://", "httpviajd://");
                    url = url.replaceFirst("https://", "httpsviajd://");
                    if (directHTTP.canHandle(url)) {
                        try {
                            /* TODO: package handling, info forwarding */
                            ArrayList<DownloadLink> httpLinks = directHTTP.getDownloadLinks(url, null);
                            if (httpLinks != null) {
                                for (DownloadLink hosterLink : httpLinks) {
                                    handleFinalLink(hosterLink);
                                }
                            }
                        } catch (Throwable e) {
                            Log.exception(e);
                        }
                    }
                }
            }
        }
    }

    protected void decrypt(CryptedLink cryptedLink) {
        if (cryptedLink == null || cryptedLink.getPlugin() == null) return;
        PluginForDecrypt plg = cryptedLink.getPlugin().getWrapper().getNewPluginInstance();
        plg.setBrowser(new Browser());
        ArrayList<DownloadLink> decryptedPossibleLinks = plg.decryptLink(cryptedLink);
        if (decryptedPossibleLinks != null && decryptedPossibleLinks.size() > 0) {
            ArrayList<CryptedLink> possibleCryptedLinks = new ArrayList<CryptedLink>(decryptedPossibleLinks.size());
            for (DownloadLink decryptedLink : decryptedPossibleLinks) {
                decryptedLink.setBrowserUrl(cryptedLink.getCryptedUrl());
                possibleCryptedLinks.add(new CryptedLink(decryptedLink));
            }
            distribute(possibleCryptedLinks);
        }
    }

    private void copySettings() {
        final Thread currentThread = Thread.currentThread();
        /**
         * use BrowserSettings from current thread if available
         */
        if (currentThread != null && currentThread instanceof BrowserSettings) {
            final BrowserSettings settings = (BrowserSettings) currentThread;
            this.proxy = settings.getCurrentProxy();
            this.debug = settings.isDebug();
            this.verbose = settings.isVerbose();
            this.logger = settings.getLogger();
        }
    }

    public boolean waitForDecrypting() {
        while (this.threadPool.getQueue().size() > 0) {
            synchronized (LinkFinder.this) {
                try {
                    LinkFinder.this.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        return this.threadPool.getQueue().size() == 0;
    }

    public ArrayList<DownloadLink> getDecryptedLinks() {
        return decryptedLinks;
    }

    protected void handleFinalLink(DownloadLink link) {
        if (link == null) return;
        synchronized (decryptedLinks) {
            decryptedLinks.add(link);
        }
    }

    public HTTPProxy getCurrentProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setCurrentProxy(HTTPProxy proxy) {
        this.proxy = proxy;
    }

    public void setDebug(boolean b) {
        this.debug = b;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setVerbose(boolean b) {
        this.verbose = b;
    }

}
