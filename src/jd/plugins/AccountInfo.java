//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.plugins.controller.host.PluginFinder;

import jd.config.Property;
import jd.http.Browser;
import jd.nutils.NaturalOrderComparator;

public class AccountInfo extends Property implements AccountTrafficView {
    private static final long serialVersionUID       = 1825140346023286206L;
    private volatile long     account_validUntil     = -1;
    private volatile long     account_LastValidUntil = -1;
    private volatile long     account_trafficLeft    = -1;
    private volatile long     account_trafficMax     = -1;
    private long              account_filesNum       = -1;
    private long              account_premiumPoints  = -1;
    private long              account_accountBalance = -1;
    private long              account_usedSpace      = -1;
    private volatile String   account_status;
    private long              account_createTime     = 0;
    /**
     * indicator that host, account has special traffic handling, do not temp disable if traffic =0
     */
    private volatile boolean  specialTraffic         = false;
    private volatile boolean  account_trafficRefill  = true;

    public boolean isTrafficRefill() {
        return account_trafficRefill;
    }

    public void setTrafficRefill(boolean account_trafficRefill) {
        this.account_trafficRefill = account_trafficRefill;
    }

    public long getCreateTime() {
        return account_createTime;
    }

    /**
     * True = Allow downloads without traffic --> You can set a trafficleft value and it will get displayed to the user but ignored for
     * downloading.
     */
    public void setSpecialTraffic(final boolean b) {
        specialTraffic = b;
    }

    public boolean isSpecialTraffic() {
        return specialTraffic;
    }

    public void setCreateTime(final long createTime) {
        this.account_createTime = createTime;
    }

    /**
     * Gibt zurück wieviel (in Cent) Geld gerade auf diesem Account ist
     *
     * @return
     */
    public long getAccountBalance() {
        return account_accountBalance;
    }

    /**
     * Gibt zurück wieviele Files auf dem Account hochgeladen sind
     *
     * @return
     */
    public long getFilesNum() {
        return account_filesNum;
    }

    /**
     * Gibt an wieviele PremiumPunkte der Account hat
     *
     * @return
     */
    public long getPremiumPoints() {
        return account_premiumPoints;
    }

    public String getStatus() {
        return account_status;
    }

    /**
     * Gibt an wieviel Traffic noch frei ist (in bytes)
     *
     * @return
     */
    public long getTrafficLeft() {
        return Math.max(0, account_trafficLeft);
    }

    public long getTrafficMax() {
        return Math.max(getTrafficLeft(), account_trafficMax);
    }

    /**
     * Gibt zurück wieviel Platz (bytes) die Oploads auf diesem Account belegen
     *
     * @return
     */
    public long getUsedSpace() {
        return account_usedSpace;
    }

    /**
     * Gibt einen Timestamp zurück zu dem der Account auslaufen wird bzw. ausgelaufen ist.(-1 für Nie)
     *
     * @return
     */
    public long getValidUntil() {
        return account_validUntil;
    }

    public long getLastValidUntil() {
        return account_LastValidUntil;
    }

    /**
     * Gibt zurück ob der Account abgelaufen ist
     *
     * @return
     */
    public boolean isExpired() {
        final long validUntil = getValidUntil();
        if (validUntil < 0) {
            return false;
        }
        if (validUntil == 0) {
            return true;
        }
        final boolean expired = validUntil < System.currentTimeMillis();
        return expired;
    }

    public void setAccountBalance(final long parseInt) {
        this.account_accountBalance = Math.max(0, parseInt);
    }

    public void setAccountBalance(final String string) {
        this.setAccountBalance((long) (Double.parseDouble(string) * 100));
    }

    public void setExpired(final boolean b) {
        if (b) {
            setValidUntil(0);
        } else {
            setValidUntil(-1);
        }
    }

    public void setFilesNum(final long parseInt) {
        this.account_filesNum = Math.max(0, parseInt);
    }

    public void setPremiumPoints(final long parseInt) {
        this.account_premiumPoints = Math.max(0, parseInt);
    }

    public void setPremiumPoints(final String string) {
        this.setPremiumPoints(Integer.parseInt(string.trim()));
    }

    public void setStatus(final String string) {
        this.account_status = string;
    }

    public void setTrafficLeft(long size) {
        this.account_trafficLeft = Math.max(0, size);
    }

    public void setUnlimitedTraffic() {
        account_trafficLeft = -1;
    }

    public boolean isUnlimitedTraffic() {
        return account_trafficLeft == -1;
    }

    public void setTrafficLeft(final String freeTraffic) {
        this.setTrafficLeft(SizeFormatter.getSize(freeTraffic, true, true));
    }

    /**
     * @since JD2
     * @param trafficMax
     */
    public void setTrafficMax(final String trafficMax) {
        this.setTrafficMax(SizeFormatter.getSize(trafficMax, true, true));
    }

    public void setTrafficMax(final long trafficMax) {
        this.account_trafficMax = Math.max(0, trafficMax);
    }

    public void setUsedSpace(final long size) {
        this.account_usedSpace = Math.max(0, size);
    }

    public void setUsedSpace(final String string) {
        this.setUsedSpace(SizeFormatter.getSize(string, true, true));
    }

    /**
     * Wrapper, will use standard httpd Date pattern.
     *
     * @author raztoki
     * @param validuntil
     * @param br
     * @return
     */
    public final boolean setValidUntil(final long validuntil, final Browser br) {
        return setValidUntil(validuntil, br, "EEE, dd MMM yyyy HH:mm:ss z");
    }

    /**
     * This method assumes that httpd server time represents hoster timer, and will offset validuntil against users system time and httpd
     * server time. <br />
     * This should also allow when computer clocks are wrong. <br />
     * *** WARNING *** This method wont work when httpd DATE response isn't of hoster time!
     *
     * @author raztoki
     * @since JD2
     * @param validuntil
     * @param br
     */
    public final boolean setValidUntil(final long validuntil, final Browser br, final String formatter) {
        if (validuntil == -1) {
            setValidUntil(-1);
            return true;
        }
        final long serverTime = br.getCurrentServerTime(-1);
        if (serverTime > 0) {
            final long a1 = validuntil + (System.currentTimeMillis() - serverTime);
            setValidUntil(a1);
            return true;
        } else {
            // failover
            setValidUntil(validuntil);
            return false;
        }
    }

    /**
     * -1 = Expires never
     *
     * @param validUntil
     */
    public void setValidUntil(final long validUntil) {
        this.account_validUntil = validUntil;
    }

    public void setLastValidUntil(final long validUntil) {
        this.account_LastValidUntil = validUntil;
    }

    /**
     * Removes forbidden hosts, adds host corrections, de-dupes, and then sets AccountInfo property 'multiHostSupport'
     *
     * @author raztoki
     * @param multiHostPlugin
     * @since JD2
     */
    public List<String> setMultiHostSupport(final PluginForHost multiHostPlugin, final List<String> multiHostSupport) {
        if (multiHostPlugin != null && multiHostPlugin.getLogger() != null) {
            return setMultiHostSupport(multiHostPlugin, multiHostSupport, new PluginFinder(LogController.TRASH));
        } else {
            final LogSource logSource = LogController.getFastPluginLogger(Thread.currentThread().getName());
            try {
                return setMultiHostSupport(multiHostPlugin, multiHostSupport, new PluginFinder(logSource));
            } finally {
                logSource.close();
            }
        }
    }

    public static void testsetMultiHostSupport(final PluginForHost plg) {
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /* Do nothing */
            return;
        }
        final PluginFinder finder = new PluginFinder(plg.getLogger());
        final String onlyThisPlugin = null;
        final AccountInfo ai = new AccountInfo();
        for (final LazyHostPlugin plugin : HostPluginController.getInstance().list()) {
            if (plugin.isOfflinePlugin()) {
                continue;
            } else if (onlyThisPlugin != null && !StringUtils.equalsIgnoreCase(onlyThisPlugin, plugin.getHost())) {
                continue;
            }
            final List<String> hosts = new ArrayList<String>();
            hosts.add(plugin.getHost());
            PluginForHost pl = null;
            try {
                pl = plugin.getPrototype(null);
                final String[] names = pl.siteSupportedNames();
                if (names != null && hosts.size() == 1) {
                    hosts.addAll(Arrays.asList(names));
                }
            } catch (final Exception e) {
                plg.getLogger().log(e);
            }
            for (String host : hosts) {
                final List<String> ret = ai.setMultiHostSupport(plg, Arrays.asList(new String[] { host }), finder);
                if (ret == null || ret.size() != 1) {
                    final LazyHostPlugin lazy = finder._assignHost(host);
                    if (lazy != null && lazy.isOfflinePlugin()) {
                        continue;
                    }
                    final String debugPattern = lazy != null ? lazy.getPatternSource() : null;
                    System.out.println("WTF:" + host + "|" + debugPattern);
                } else if (!host.equals(ret.get(0))) {
                    if (hosts.contains(ret.get(0)) && hosts.get(0).equals(ret.get(0))) {
                        continue;
                    } else if (pl != null && ret.get(0).equals(pl.rewriteHost(host))) {
                        continue;
                    }
                    System.out.println("WTF:" + host + "!=" + ret.get(0));
                }
            }
            System.out.println("nice:" + plugin);
        }
        System.out.println("nice");
    }

    public List<String> setMultiHostSupport(final PluginForHost multiHostPlugin, final List<String> multiHostSupportList, final PluginFinder pluginFinder) {
        final String propertyKey = "multiHostSupport";
        if (multiHostSupportList == null || multiHostSupportList.size() == 0) {
            this.removeProperty(propertyKey);
            return null;
        }
        final LogInterface logger = (multiHostPlugin != null && multiHostPlugin.getLogger() != null) ? multiHostPlugin.getLogger() : LogController.CL();
        final HostPluginController hpc = HostPluginController.getInstance();
        final HashSet<String> assignedMultiHostPlugins = new HashSet<String>();
        final HashMap<String, String> cleanList = new HashMap<String, String>();
        final HashMap<String, Set<LazyHostPlugin>> mapping = new HashMap<String, Set<LazyHostPlugin>>();
        final HashSet<String> nonTldHosts = new HashSet<String>();
        // lets do some preConfiguring, and match hosts which do not contain tld
        for (final String host : multiHostSupportList) {
            if (host == null) {
                continue;
            }
            final String hostCleaned = StringUtils.toLowerCaseOrNull(host).replaceAll("\\s+", "");
            cleanList.put(host, hostCleaned);
            if (StringUtils.isEmpty(hostCleaned)) {
                // blank entry will match every plugin! -raztoki20170315
                continue;
            } else if (hostCleaned.matches("http|https|file|up|upload|video|torrent|ftp")) {
                // we need to ignore/blacklist common phrases, else we get too many false positives
                continue;
            } else if ("usenet".equals(hostCleaned)) {
                // special cases
                assignedMultiHostPlugins.add(hostCleaned);
            } else if (hostCleaned.indexOf('.') == -1) {
                /*
                 * if the multihoster doesn't include full host name with tld, we can search and add all partial matches!
                 */
                nonTldHosts.add(hostCleaned);
            } else {
                assignedMultiHostPlugins.add(hostCleaned);
            }
        }
        if (!nonTldHosts.isEmpty()) {
            final HashMap<String, List<LazyHostPlugin>> map = new HashMap<String, List<LazyHostPlugin>>();
            pluginloop: for (final LazyHostPlugin lazyHostPlugin : hpc.list()) {
                if (lazyHostPlugin.isFallbackPlugin()) {
                    continue;
                }
                final String[] siteSupportedNames = lazyHostPlugin.getSitesSupported();
                if (siteSupportedNames != null) {
                    final Iterator<String> it = nonTldHosts.iterator();
                    while (it.hasNext()) {
                        final String nonTldHost = it.next();
                        for (final String siteSupportedName : siteSupportedNames) {
                            if (StringUtils.equalsIgnoreCase(siteSupportedName, nonTldHost)) {
                                /* Exact match */
                                final List<LazyHostPlugin> list = new ArrayList<LazyHostPlugin>();
                                map.put(nonTldHost, list);
                                list.add(lazyHostPlugin);
                                it.remove();
                                continue pluginloop;
                            } else if (StringUtils.containsIgnoreCase(siteSupportedName, nonTldHost)) {
                                List<LazyHostPlugin> list = map.get(nonTldHost);
                                if (list == null) {
                                    list = new ArrayList<LazyHostPlugin>();
                                    map.put(nonTldHost, list);
                                    list.add(lazyHostPlugin);
                                } else if (!list.contains(lazyHostPlugin)) {
                                    list.add(lazyHostPlugin);
                                }
                                // it.remove();
                                // continue pluginloop;
                            }
                        }
                    }
                    continue pluginloop;
                }
                final String pattern = lazyHostPlugin.getPatternSource();
                for (final String nonTldHost : nonTldHosts) {
                    if (StringUtils.containsIgnoreCase(pattern, nonTldHost) || (nonTldHost.contains("-") && StringUtils.containsIgnoreCase(pattern, nonTldHost.replace("-", "\\-")))) {
                        List<LazyHostPlugin> list = map.get(nonTldHost);
                        if (list == null) {
                            list = new ArrayList<LazyHostPlugin>();
                            map.put(nonTldHost, list);
                        }
                        if (!list.contains(lazyHostPlugin)) {
                            list.add(lazyHostPlugin);
                        }
                    }
                }
            }
            for (final Entry<String, List<LazyHostPlugin>> entry : map.entrySet()) {
                final String host = StringUtils.toLowerCaseOrNull(entry.getKey());
                final List<LazyHostPlugin> list = entry.getValue();
                LazyHostPlugin lazyPlugin = null;
                if (list.size() == 1) {
                    final LazyHostPlugin lazyHostPlugin = list.get(0);
                    if (!lazyHostPlugin.isOfflinePlugin()) {
                        lazyPlugin = lazyHostPlugin;
                    }
                } else if (list.size() > 1) {
                    for (final LazyHostPlugin lazyHostPlugin : list) {
                        if (lazyHostPlugin.isOfflinePlugin()) {
                            continue;
                        }
                        if (lazyPlugin == null) {
                            lazyPlugin = lazyHostPlugin;
                        } else {
                            final boolean a = StringUtils.containsIgnoreCase(lazyPlugin.getHost(), host + ".");
                            final boolean b = StringUtils.containsIgnoreCase(lazyHostPlugin.getHost(), host + ".");
                            if (a && !b) {
                                continue;
                            } else if (!a && b) {
                                lazyPlugin = lazyHostPlugin;
                                continue;
                            } else {
                                lazyPlugin = null;
                                break;
                            }
                        }
                    }
                }
                if (lazyPlugin == null) {
                    continue;
                }
                // update mapping
                assignedMultiHostPlugins.add(lazyPlugin.getHost());
                Set<LazyHostPlugin> plugins = mapping.get(host);
                if (plugins == null) {
                    plugins = new HashSet<LazyHostPlugin>();
                    mapping.put(host, plugins);
                }
                plugins.add(lazyPlugin);
            }
        }
        final List<String> unassignedMultiHostSupport = new ArrayList<String>(assignedMultiHostPlugins);
        assignedMultiHostPlugins.clear();
        Iterator<String> it = unassignedMultiHostSupport.iterator();
        while (it.hasNext()) {
            final String host = it.next();
            if (host == null) {
                continue;
            }
            final LazyHostPlugin lazyPlugin = pluginFinder._assignHost(host);
            if (lazyPlugin == null) {
                continue;
            }
            it.remove();
            if (assignedMultiHostPlugins.contains(lazyPlugin.getHost())) {
                Set<LazyHostPlugin> plugins = mapping.get(host);
                if (plugins == null) {
                    plugins = new HashSet<LazyHostPlugin>();
                    mapping.put(host, plugins);
                }
                plugins.add(lazyPlugin);
            } else {
                if (!lazyPlugin.isOfflinePlugin() && !lazyPlugin.isFallbackPlugin() && !assignedMultiHostPlugins.contains(lazyPlugin.getHost())) {
                    try {
                        if (!lazyPlugin.isHasAllowHandle()) {
                            assignedMultiHostPlugins.add(lazyPlugin.getHost());
                            Set<LazyHostPlugin> plugins = mapping.get(host);
                            if (plugins == null) {
                                plugins = new HashSet<LazyHostPlugin>();
                                mapping.put(host, plugins);
                            }
                            plugins.add(lazyPlugin);
                        } else {
                            final DownloadLink link = new DownloadLink(null, "", lazyPlugin.getHost(), "", false);
                            final PluginForHost plg = pluginFinder.getPlugin(lazyPlugin);
                            if (plg.allowHandle(link, multiHostPlugin)) {
                                assignedMultiHostPlugins.add(lazyPlugin.getHost());
                                Set<LazyHostPlugin> plugins = mapping.get(host);
                                if (plugins == null) {
                                    plugins = new HashSet<LazyHostPlugin>();
                                    mapping.put(host, plugins);
                                }
                                plugins.add(lazyPlugin);
                            }
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
            }
        }
        it = unassignedMultiHostSupport.iterator();
        while (it.hasNext()) {
            final String host = it.next();
            final String hostParts[] = host.split("\\.");
            if (hostParts.length < 2) {
                continue;
            }
            final String tld = hostParts[hostParts.length - 1];
            final String domain = hostParts[hostParts.length - 2];
            final String matcher = ".*(\\\\.|/|\\?:?|\\(|\\||\\\\Q|)" + domain.replaceAll("(-(.+))", "(-$2|\\\\(\\\\?:-$2\\\\)\\\\?|\\\\(-$2\\\\)\\\\?)") + "(\\|[^/]*?)?(\\\\)?.(" + tld + "|[^\\/)]*" + tld + "|[^\\)/]*[a-zA-Z\\.]+\\)[\\?\\.]*" + tld + ").*";
            final String matcher2 = ".*" + Pattern.quote("\\Q" + host + "\\E") + ".*" + Pattern.quote("\\E") + "\\)/.*";
            boolean foundFlag = false;
            for (final LazyHostPlugin lazyHostPlugin : hpc.list()) {
                if (lazyHostPlugin.isFallbackPlugin() || lazyHostPlugin.isOfflinePlugin()) {
                    continue;
                }
                final String pattern = lazyHostPlugin.getPatternSource();
                if (StringUtils.containsIgnoreCase(pattern, host) || pattern.matches(matcher) || pattern.matches(matcher2)) {
                    assignedMultiHostPlugins.add(lazyHostPlugin.getHost());
                    Set<LazyHostPlugin> plugins = mapping.get(host);
                    if (plugins == null) {
                        plugins = new HashSet<LazyHostPlugin>();
                        mapping.put(host, plugins);
                    }
                    plugins.add(lazyHostPlugin);
                    foundFlag = true;
                }
            }
            if (foundFlag) {
                it.remove();
            }
        }
        /* Log items without result */
        if (unassignedMultiHostSupport.size() > 0 && multiHostPlugin != null) {
            for (final String host : unassignedMultiHostSupport) {
                logger.info("Could not assign any host for: " + host);
            }
        }
        if (assignedMultiHostPlugins.size() == 0) {
            logger.info("Failed to find ANY usable results");
            this.removeProperty(propertyKey);
            return null;
        }
        // sorting will now work properly since they are all pre-corrected to lowercase.
        assignedMultiHostPlugins.clear();
        final List<String> list = new ArrayList<String>();
        final List<String> ret = new ArrayList<String>();
        for (final String host : multiHostSupportList) {
            final String cleanHost = cleanList.get(host);
            final Set<LazyHostPlugin> plugins = mapping.get(cleanHost);
            if (plugins == null) {
                ret.add(null);
                continue;
            } else if (plugins.size() == 1) {
                final LazyHostPlugin plugin = plugins.iterator().next();
                final String pluginHost = plugin.getHost();
                ret.add(pluginHost);
                if (!list.contains(pluginHost)) {
                    list.add(pluginHost);
                }
                continue;
            } else {
                List<LazyHostPlugin> best = new ArrayList<LazyHostPlugin>();
                for (LazyHostPlugin plugin : plugins) {
                    try {
                        final PluginForHost plg = pluginFinder.getPlugin(plugin);
                        final String[] siteSupportedNames = plg.siteSupportedNames();
                        if (siteSupportedNames != null && Arrays.asList(siteSupportedNames).contains(cleanHost)) {
                            best.add(plugin);
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
                if (best.size() == 1) {
                    final LazyHostPlugin plugin = best.get(0);
                    final String pluginHost = plugin.getHost();
                    ret.add(pluginHost);
                    if (!list.contains(pluginHost)) {
                        list.add(pluginHost);
                    }
                    continue;
                }
                logger.log(new Exception("DEBUG: " + host));
            }
        }
        Collections.sort(list, new NaturalOrderComparator());
        this.setProperty(propertyKey, new CopyOnWriteArrayList<String>(list));
        return ret;
    }

    /** Removes host from list of supported hosts. */
    public boolean removeMultiHostSupport(final String host) {
        final Object ret = getProperty("multiHostSupport", null);
        if (ret != null && ret instanceof List) {
            final List<String> list = (List<String>) ret;
            if (list.contains(host)) {
                if (list.size() > 1) {
                    final List<String> newList = new CopyOnWriteArrayList<String>(list);
                    if (newList.remove(host)) {
                        this.setProperty("multiHostSupport", newList);
                        return true;
                    }
                } else {
                    this.setProperty("multiHostSupport", Property.NULL);
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> getMultiHostSupport() {
        final Object ret = getProperty("multiHostSupport", null);
        if (ret != null && ret instanceof List) {
            final List<String> list = (List<String>) ret;
            if (list.size() > 0) {
                return list;
            }
        }
        return null;
    }

    public static long getTimestampInServerContext(final Browser br, final long timestamp) {
        final long serverTime = br.getCurrentServerTime(-1);
        if (serverTime > 0) {
            return timestamp + (System.currentTimeMillis() - serverTime);
        } else {
            return timestamp;
        }
    }
}
