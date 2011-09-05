package jd.controlling.reconnect.ipcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.JDLogger;
import jd.controlling.reconnect.ReconnectConfig;
import jd.http.Browser;

import org.appwork.storage.config.JsonConfig;

/**
 * balanced IP check uses the jdownloader ip check servers. This type of ip
 * check is default, and fallback for all reconnect methods
 * 
 * @author thomas
 * 
 */
public class BalancedWebIPCheck implements IPCheckProvider {
    public static BalancedWebIPCheck getInstance() {
        return BalancedWebIPCheck.INSTANCE;
    }

    private static final ArrayList<String>  SERVICES = new ArrayList<String>();
    static {
        SERVICES.add("http://ipcheck3.jdownloader.org");
        SERVICES.add("http://ipcheck2.jdownloader.org");
        SERVICES.add("http://ipcheck1.jdownloader.org");
        SERVICES.add("http://ipcheck0.jdownloader.org");
    }
    /**
     * All registered ip check urls
     */
    private final ArrayList<String>         servicesInUse;

    private final Browser                   br;

    private final Pattern                   pattern;

    private static final BalancedWebIPCheck INSTANCE = new BalancedWebIPCheck();
    private final Object                    LOCK     = new Object();

    private boolean                         checkOnlyOnce;

    private BalancedWebIPCheck() {

        this.servicesInUse = new ArrayList<String>();
        setOnlyUseWorkingServices(false);
        this.pattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
        Collections.shuffle(this.servicesInUse);
        this.br = new Browser();
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
                try {

                    final String service = this.servicesInUse.get(i);
                    /* call website and check for ip */
                    final Matcher matcher = this.pattern.matcher(this.br.getPage(service));
                    if (matcher.find()) {
                        if (matcher.groupCount() > 0) { return IP.getInstance(matcher.group(1)); }
                    }
                } catch (final Throwable e2) {
                    JDLogger.getLogger().info(e2.getMessage());

                }
            }

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
                    try {

                        final String service = SERVICES.get(i);
                        /* call website and check for ip */
                        final Matcher matcher = this.pattern.matcher(this.br.getPage(service));
                        if (matcher.find()) {
                            if (matcher.groupCount() > 0) {
                                servicesInUse.add(service);
                            }
                        }
                    } catch (final Throwable e2) {
                        JDLogger.getLogger().info(e2.getMessage());

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
