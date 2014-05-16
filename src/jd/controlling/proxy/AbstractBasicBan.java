package jd.controlling.proxy;

import java.net.URL;

import jd.controlling.accountchecker.AccountChecker.AccountCheckJob;
import jd.controlling.accountchecker.AccountCheckerThread;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkchecker.LinkCheckerThread;
import jd.plugins.Account;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;

public abstract class AbstractBasicBan extends AbstractBan {

    protected Plugin    plugin;

    protected HTTPProxy proxy;

    public AbstractBasicBan(Plugin plugin, AbstractProxySelectorImpl proxySelector, HTTPProxy proxy) {
        super(proxySelector);
        this.plugin = plugin;

        this.proxy = proxy;
    }

    @Override
    public String toString() {
        return getSelector() + ":" + getClass() + " Plugin :" + (plugin == null ? null : plugin.getHost()) + " proxy: " + proxy;
    }

    // _JDT._.plugins_errors_proxy_connection()

    @Override
    public boolean validate(AbstractProxySelectorImpl selector, HTTPProxy orgReference, URL url) {

        if (proxy != null) {
            if (!proxy.equals(orgReference)) {
                return false;
            }
        }
        Thread thread = Thread.currentThread();
        if (thread instanceof AccountCheckerThread) {

            AccountCheckJob job = ((AccountCheckerThread) thread).getJob();
            if (job != null) {
                Account account = job.getAccount();
                PluginForHost plg = account.getPlugin();
                if (plg != null) {
                    if (StringUtils.equalsIgnoreCase(plg.getHost(), plugin.getHost())) {
                        return true;
                    }

                }
                return false;
            }

        } else if (thread instanceof LinkCheckerThread) {
            PluginForHost plg = ((LinkCheckerThread) thread).getPlugin();
            if (plg != null) {
                if (StringUtils.equalsIgnoreCase(plg.getHost(), plugin.getHost())) {

                }
                return false;
            }
        } else if (thread instanceof SingleDownloadController) {
            DownloadLinkCandidate candidate = ((SingleDownloadController) thread).getDownloadLinkCandidate();
            return isSelectorBannedByPlugin(candidate.getCachedAccount().getPlugin());

        }

        return false;
    }

    @Override
    public boolean isSelectorBannedByPlugin(Plugin candidate) {
        // auth is always a ban reason
        return true;
    }

    @Override
    public boolean isProxyBannedByUrlOrPlugin(HTTPProxy orgReference, URL url, Plugin pluginFromThread) {
        // auth is always a ban reason
        return true;
    }

    @Override
    public boolean isSelectorBannedByUrl(URL url) {
        return false;
    }

    private boolean pluginsEquals(Plugin a, Plugin b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }

        return StringUtils.equalsIgnoreCase(a.getHost(), b.getHost());

    }

    @Override
    public boolean validate(ConnectionBan ban) {
        if (ban instanceof AbstractBasicBan) {
            AbstractBasicBan abb = (AbstractBasicBan) ban;
            if (StringUtils.equalsIgnoreCase(abb.plugin.getHost(), plugin.getHost())) {
                if (proxyEquals(abb.proxy, proxy)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean proxyEquals(HTTPProxy a, HTTPProxy b) {
        if (a == b) {
            return true;
        }
        if (a != null && b != null) {
            return a.getType() == b.getType() && stringEquals(a.getHost(), b.getHost()) && a.getPort() == b.getPort();
        }
        return false;
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    private boolean stringEquals(String a, String b) {
        if (a == null) {
            a = "";
        }
        if (b == null) {
            b = "";
        }
        return StringUtils.equals(a, b);
    }
}
