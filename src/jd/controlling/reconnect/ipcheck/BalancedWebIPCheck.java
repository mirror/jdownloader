package jd.controlling.reconnect.ipcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.reconnect.ReconnectConfig;
import jd.http.Browser;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.logging.LogController;

/**
 * balanced IP check uses the jdownloader ip check servers. This type of ip check is default, and fallback for all reconnect methods
 * 
 * @author thomas
 * 
 */
public class BalancedWebIPCheck implements IPCheckProvider {
    public static BalancedWebIPCheck getInstance() {
        return BalancedWebIPCheck.INSTANCE;
    }

    private static final java.util.List<String>  SERVICES = new ArrayList<String>();
    static {
        SERVICES.add("http://ipcheck3.jdownloader.org");
        SERVICES.add("http://ipcheck2.jdownloader.org");
        SERVICES.add("http://ipcheck1.jdownloader.org");
        SERVICES.add("http://ipcheck0.jdownloader.org");
    }
    /**
     * All registered ip check urls
     */
    private final java.util.List<String>         servicesInUse;

    private final Browser                   br;

    private final Pattern                   pattern;

    private static final BalancedWebIPCheck INSTANCE = new BalancedWebIPCheck(false);
    private final Object                    LOCK     = new Object();

    private boolean                         checkOnlyOnce;

    public BalancedWebIPCheck(boolean useGlobalProxy) {
        this.servicesInUse = new ArrayList<String>();
        setOnlyUseWorkingServices(false);
        this.pattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
        Collections.shuffle(this.servicesInUse);
        this.br = new Browser();
        this.br.setDebug(true);
        this.br.setVerbose(true);
        if (!useGlobalProxy) this.br.setProxy(HTTPProxy.NONE);
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
            System.out.println("CHECK");
            for (int i = 0; i < (checkOnlyOnce ? 1 : this.servicesInUse.size()); i++) {
                LogSource logger = LogController.CL(false);
                logger.setAllowTimeoutFlush(false);
                try {
                    final String service = this.servicesInUse.get(i);
                    /* call website and check for ip */
                    br.setLogger(logger);
                    final Matcher matcher = this.pattern.matcher(this.br.getPage(service));
                    if (matcher.find()) {
                        if (matcher.groupCount() > 0) {
                            logger.clear();
                            return IP.getInstance(matcher.group(1));
                        }
                    }
                } catch (final Throwable e2) {
                    try {
                        br.disconnect();
                    } catch (final Throwable e) {
                    }
                    logger.log(e2);
                } finally {
                    br.setLogger(null);
                    logger.close();
                }
            }
            LogController.CL().severe("All balanced Services failed");
            throw new OfflineException("All balanced Services failed");

        }
    }

    /**
     * Returns how often the controller should use this IP Check.
     */
    public int getIpCheckInterval() {
        return 5;
    }

    public void setOnlyUseWorkingServices(boolean b) {
        synchronized (this.LOCK) {
            checkOnlyOnce = b;
            if (b) {
                servicesInUse.clear();
                for (int i = 0; i < SERVICES.size(); i++) {
                    LogSource logger = LogController.CL(false);
                    logger.setAllowTimeoutFlush(false);
                    try {

                        final String service = SERVICES.get(i);
                        /* call website and check for ip */
                        br.setLogger(logger);
                        final Matcher matcher = this.pattern.matcher(this.br.getPage(service));
                        if (matcher.find()) {
                            if (matcher.groupCount() > 0) {
                                logger.clear();
                                servicesInUse.add(service);
                            }
                        }
                    } catch (final Throwable e2) {
                        logger.log(e2);
                    } finally {
                        logger.close();
                    }
                }
                if (servicesInUse.size() == 0) {
                    servicesInUse.clear();
                    servicesInUse.addAll(SERVICES);
                    throw new IllegalStateException("No Services are Working. reverted to all");
                }
            } else {
                servicesInUse.clear();
                servicesInUse.addAll(SERVICES);
            }

        }
    }

}
