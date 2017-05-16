package jd.controlling.reconnect.ipcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.proxy.ProxyController;
import jd.controlling.reconnect.ReconnectConfig;
import jd.http.Browser;
import jd.http.ProxySelectorInterface;
import jd.http.StaticProxySelector;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_RECONNECT;

/**
 * balanced IP check uses the jdownloader ip check servers. This type of ip check is default, and fallback for all reconnect methods
 *
 * @author thomas
 *
 */
public class BalancedWebIPCheck implements IPCheckProvider {
    private static final java.util.List<String> SERVICES = new ArrayList<String>();
    static {
        SERVICES.add("http://ipcheck4.jdownloader.org");
        SERVICES.add("http://ipcheck3.jdownloader.org");
        SERVICES.add("http://ipcheck2.jdownloader.org");
        SERVICES.add("http://ipcheck1.jdownloader.org");
        SERVICES.add("http://ipcheck0.jdownloader.org");
        Collections.shuffle(SERVICES);
    }
    /**
     * All registered ip check urls
     */
    protected final Browser                     br;
    private final Pattern                       pattern;
    private final Object                        LOCK     = new Object();

    public BalancedWebIPCheck() {
        this(CFG_RECONNECT.CFG.isIPCheckUsesProxyEnabled() ? ProxyController.getInstance() : new StaticProxySelector(HTTPProxy.NONE));
    }

    public BalancedWebIPCheck(final ProxySelectorInterface proxySelector) {
        this.pattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
        this.br = new Browser();
        this.br.setDebug(true);
        this.br.setVerbose(true);
        if (proxySelector != null) {
            br.setProxySelector(proxySelector);
        }
        this.br.setConnectTimeout(JsonConfig.create(ReconnectConfig.class).getIPCheckConnectTimeout());
        this.br.setReadTimeout(JsonConfig.create(ReconnectConfig.class).getIPCheckReadTimeout());
    }

    /**
     * gets the external IP.
     *
     * @throws IPCheckException
     *             if there is no valid external IP
     */
    public IP getExternalIP() throws IPCheckException {
        synchronized (this.LOCK) {
            final LogSource logger = LogController.getFastPluginLogger("BalancedWebIPCheck");
            logger.setAllowTimeoutFlush(false);
            br.setLogger(logger);
            for (String service : SERVICES) {
                try {
                    /* call website and check for ip */
                    final Matcher matcher = this.pattern.matcher(this.br.getPage(service));
                    if (matcher.find()) {
                        if (matcher.groupCount() > 0) {
                            logger.clear();
                            logger.close();
                            return IP.getInstance(matcher.group(1));
                        }
                    }
                } catch (final Throwable e2) {
                    logger.log(e2);
                } finally {
                    try {
                        br.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
            logger.severe("All balanced Services failed");
            logger.close();
            throw new OfflineException("All balanced Services failed");
        }
    }

    /**
     * Returns how often the controller should use this IP Check.
     */
    public int getIpCheckInterval() {
        return 5;
    }
}
