package org.jdownloader.captcha.v2.solver.browser;

import java.awt.Rectangle;
import java.io.IOException;

import jd.controlling.accountchecker.AccountChecker.AccountCheckJob;
import jd.controlling.accountchecker.AccountCheckerThread;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkchecker.LinkCheckerThread;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.remoteapi.exceptions.RemoteAPIException;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.solverjob.ResponseList;

public abstract class AbstractBrowserChallenge extends Challenge<String> {
    protected final Plugin  plugin;
    protected final Browser pluginBrowser;

    public Plugin getPlugin() {
        return plugin;
    }

    public Browser getPluginBrowser() {
        return pluginBrowser;
    }

    public boolean isSolved() {
        final ResponseList<String> results = getResult();
        return results != null && results.getValue() != null;
    }

    protected AbstractBrowserChallenge(final String method, final Plugin plugin, Browser pluginBrowser) {
        super(method, null);
        this.plugin = plugin;
        this.pluginBrowser = pluginBrowser;
    }

    public AbstractBrowserChallenge(final String method, final Plugin plugin) {
        super(method, null);
        if (plugin == null) {
            this.plugin = getPluginFromThread();
        } else {
            this.plugin = plugin;
        }
        if (this.plugin instanceof PluginForHost) {
            this.pluginBrowser = ((PluginForHost) this.plugin).getBrowser();
        } else if (this.plugin instanceof PluginForDecrypt) {
            this.pluginBrowser = ((PluginForDecrypt) this.plugin).getBrowser();
        } else {
            this.pluginBrowser = null;
        }
    }

    abstract public String getHTML(String id);

    abstract public BrowserViewport getBrowserViewport(BrowserWindow screenResource, Rectangle elementBounds);

    public boolean onGetRequest(BrowserReference browserReference, GetRequest request, HttpResponse response) throws IOException, RemoteAPIException {
        return false;
    }

    public boolean onPostRequest(BrowserReference browserReference, PostRequest request, HttpResponse response) throws IOException, RemoteAPIException {
        return false;
    }

    public boolean onRawPostRequest(final BrowserReference browserRefefence, final PostRequest request, final HttpResponse response) throws IOException, RemoteAPIException {
        return false;
    }

    public boolean onRawGetRequest(final BrowserReference browserReference, final GetRequest request, final HttpResponse response) throws IOException, RemoteAPIException {
        return false;
    }

    private Plugin getPluginFromThread() {
        final Thread thread = Thread.currentThread();
        if (thread instanceof AccountCheckerThread) {
            final AccountCheckJob job = ((AccountCheckerThread) thread).getJob();
            if (job != null) {
                final Account account = job.getAccount();
                return account.getPlugin();
            }
        } else if (thread instanceof LinkCheckerThread) {
            final PluginForHost plg = ((LinkCheckerThread) thread).getPlugin();
            if (plg != null) {
                return plg;
            }
        } else if (thread instanceof SingleDownloadController) {
            return ((SingleDownloadController) thread).getDownloadLinkCandidate().getCachedAccount().getPlugin();
        } else if (thread instanceof LinkCrawlerThread) {
            final Object owner = ((LinkCrawlerThread) thread).getCurrentOwner();
            if (owner instanceof Plugin) {
                return (Plugin) owner;
            }
        }
        return null;
    }

    public String getHttpPath() {
        if (plugin != null) {
            return plugin.getHost();
        } else {
            return "jd";
        }
    }
}
