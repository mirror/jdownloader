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
import java.util.List;
import java.util.Locale;
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
import jd.parser.Regex;
import jd.plugins.MultiHostHost.MultihosterHostStatus;

public class AccountInfo extends Property implements AccountTrafficView {
    private static final long   serialVersionUID           = 1825140346023286206L;
    private volatile long       account_validUntil         = -1;
    private volatile long       account_LastValidUntil     = -1;
    private volatile long       account_trafficLeft        = -1;
    private volatile long       account_trafficMax         = -1;
    private long                account_filesNum           = -1;
    private long                account_premiumPoints      = -1;
    private long                account_accountBalance     = -1;
    private long                account_usedSpace          = -1;
    private volatile String     account_status;
    private long                account_createTime         = 0;
    private static final String PROPERTY_MULTIHOST_SUPPORT = "multiHostSupport";
    /**
     * indicator that host, account has special traffic handling, do not temp disable if traffic =0
     */
    private volatile boolean    specialTraffic             = false;
    private volatile boolean    account_trafficRefill      = true;

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

    /**
     * Removes forbidden hosts, adds host corrections, de-dupes, and then sets AccountInfo property 'multiHostSupport'
     *
     * @author raztoki
     * @param multiHostPlugin
     * @since JD2
     */
    public List<String> setMultiHostSupport(final PluginForHost multiHostPlugin, final List<String> multiHostSupportListStr) {
        if (multiHostPlugin != null && multiHostPlugin.getLogger() != null) {
            return setMultiHostSupport(multiHostPlugin, multiHostSupportListStr, new PluginFinder(LogController.TRASH));
        } else {
            final LogSource logSource = LogController.getFastPluginLogger(Thread.currentThread().getName());
            try {
                return setMultiHostSupport(multiHostPlugin, multiHostSupportListStr, new PluginFinder(logSource));
            } finally {
                logSource.close();
            }
        }
    }

    /* Last revision with newer slash "in the middle" version of this function: 49753 */
    public List<String> setMultiHostSupport(final PluginForHost multiHostPlugin, final List<String> multiHostSupportListStr, final PluginFinder pluginFinder) {
        /* Last revision with old handling: 49800 */
        final List<MultiHostHost> mhosts = new ArrayList<MultiHostHost>();
        if (multiHostSupportListStr != null) {
            for (final String domain : multiHostSupportListStr) {
                if (domain == null) {
                    continue;
                }
                final MultiHostHost mhost = new MultiHostHost(domain);
                mhosts.add(mhost);
            }
        }
        return setMultiHostSupportV2(multiHostPlugin, mhosts, pluginFinder);
    }

    public List<String> setMultiHostSupportV2(final PluginForHost multiHostPlugin, final List<MultiHostHost> multiHostSupportList) {
        if (multiHostPlugin != null && multiHostPlugin.getLogger() != null) {
            return setMultiHostSupportV2(multiHostPlugin, multiHostSupportList, new PluginFinder(LogController.TRASH));
        } else {
            final LogSource logSource = LogController.getFastPluginLogger(Thread.currentThread().getName());
            try {
                return setMultiHostSupportV2(multiHostPlugin, multiHostSupportList, new PluginFinder(logSource));
            } finally {
                logSource.close();
            }
        }
    }

    public List<String> setMultiHostSupportV2(final PluginForHost multiHostPlugin, final List<MultiHostHost> multiHostSupportList, final PluginFinder pluginFinder) {
        if (multiHostSupportList == null || multiHostSupportList.size() == 0) {
            this.removeProperty(PROPERTY_MULTIHOST_SUPPORT);
            return null;
        }
        final LogInterface logger = (multiHostPlugin != null && multiHostPlugin.getLogger() != null) ? multiHostPlugin.getLogger() : LogController.CL();
        final HostPluginController hpc = HostPluginController.getInstance();
        final HashMap<String, MultiHostHost> cleanList = new HashMap<String, MultiHostHost>();
        final HashMap<String, Set<LazyHostPlugin>> mapping = new HashMap<String, Set<LazyHostPlugin>>();
        final HashSet<String> skippedOfflineEntries = new HashSet<String>();
        final HashSet<String> skippedInvalidEntries = new HashSet<String>();
        final HashSet<String> skippedbyPluginAllowHandleEntries = new HashSet<String>();
        final HashSet<String> otherIgnoreEntries = new HashSet<String>();
        // lets do some preConfiguring, and match hosts which do not contain tld
        final Pattern patternInvalid = Pattern.compile("http|directhttp|https|file|up|upload|video|torrent|ftp", Pattern.CASE_INSENSITIVE);
        mhostLoop: for (final MultiHostHost mhost : multiHostSupportList) {
            final List<String> domains = mhost.getDomains();
            final List<String> cleanedDomains = new ArrayList<String>();
            String maindomainCleaned = null;
            /* Clean domain entries and collect non tld items */
            cleanDomains: for (final String domain : domains) {
                if (domain == null) {
                    continue cleanDomains;
                }
                final String domainCleaned = domain.toLowerCase(Locale.ENGLISH).replaceAll("\\s+", "");
                if (StringUtils.isEmpty(domainCleaned)) {
                    /* Skip null/empty values */
                    continue cleanDomains;
                } else if (new Regex(domainCleaned, patternInvalid).patternMatches()) {
                    /* ignore/blacklist/skip common phrases, else we get too many false positives */
                    skippedInvalidEntries.add(domainCleaned);
                    continue cleanDomains;
                }
                if (maindomainCleaned == null) {
                    maindomainCleaned = domainCleaned;
                }
                cleanedDomains.add(domainCleaned);
            }
            if (maindomainCleaned == null) {
                /* List of domain contained only useless stuff -> Skip */
                continue mhostLoop;
            }
            cleanList.put(maindomainCleaned, mhost);
            LazyHostPlugin safeHit = null;
            final List<LazyHostPlugin> hits = new ArrayList<LazyHostPlugin>();
            pluginloop: for (final LazyHostPlugin lazyHostPlugin : hpc.list()) {
                for (final String domain : cleanedDomains) {
                    if (domain.equals(lazyHostPlugin.getHost())) {
                        /* Exact match -> Safe hit -> Quit loop. */
                        safeHit = lazyHostPlugin;
                        break pluginloop;
                    }
                }
                final String[] siteSupportedNames = lazyHostPlugin.getSitesSupported();
                if (siteSupportedNames != null) {
                    for (final String siteSupportedName : siteSupportedNames) {
                        if (cleanedDomains.contains(siteSupportedName)) {
                            /* Clear previous possible */
                            safeHit = lazyHostPlugin;
                            /* Safe hit -> Quit loop. */
                            break pluginloop;
                        }
                    }
                    /* Look for unsafe hits */
                    for (final String domain : cleanedDomains) {
                        for (final String siteSupportedName : siteSupportedNames) {
                            /* E.g. domain "uptobox" and full domain is "uptobox.com" */
                            if (siteSupportedName.startsWith(domain)) {
                                // hit = lazyHostPlugin;
                                hits.add(lazyHostPlugin);
                                continue pluginloop;
                            }
                        }
                    }
                }
            }
            if (safeHit != null) {
                /* Safe result -> Ignore other results */
                hits.clear();
                hits.add(safeHit);
            }
            if (hits.isEmpty()) {
                /* Items without hits will be logged later */
                continue mhostLoop;
            }
            for (final LazyHostPlugin hit : hits) {
                final String[] siteSupportedNames = hit.getSitesSupported();
                if (siteSupportedNames != null) {
                    for (final String siteSupportedName : siteSupportedNames) {
                        otherIgnoreEntries.add(siteSupportedName);
                    }
                }
                Set<LazyHostPlugin> plugins = mapping.get(maindomainCleaned);
                if (plugins == null) {
                    plugins = new HashSet<LazyHostPlugin>();
                    mapping.put(maindomainCleaned, plugins);
                }
                plugins.add(hit);
            }
        }
        final List<String> finalresults = new ArrayList<String>();
        /* TODO: Make use of this */
        final List<MultiHostHost> finalresults2 = new ArrayList<MultiHostHost>();
        final HashSet<String> unassignedMultiHostSupport = new HashSet<String>();
        cleanListLoop: for (final Entry<String, MultiHostHost> entry : cleanList.entrySet()) {
            final String maindomainCleaned = entry.getKey();
            final MultiHostHost mhost = entry.getValue();
            finalresults2.add(mhost);
            final Set<LazyHostPlugin> plugins = mapping.get(maindomainCleaned);
            if (plugins == null) {
                mhost.setStatus(MultihosterHostStatus.DEACTIVATED_JDOWNLOADER_UNSUPPORTED);
                unassignedMultiHostSupport.add(maindomainCleaned);
                continue cleanListLoop;
            }
            /* Multiple possible results -> Evaluate what's the best */
            final List<LazyHostPlugin> best = new ArrayList<LazyHostPlugin>();
            for (final LazyHostPlugin plugin : plugins) {
                if (plugin.isOfflinePlugin()) {
                    skippedOfflineEntries.add(maindomainCleaned);
                    continue;
                } else if (plugin.isFallbackPlugin()) {
                    otherIgnoreEntries.add(maindomainCleaned);
                    continue;
                }
                if (multiHostPlugin != null && plugin.isHasAllowHandle()) {
                    try {
                        final DownloadLink link = new DownloadLink(null, "", plugin.getHost(), "", false);
                        final PluginForHost plg = pluginFinder.getPlugin(plugin);
                        if (!plg.allowHandle(link, multiHostPlugin)) {
                            skippedbyPluginAllowHandleEntries.add(plugin.getHost());
                            continue;
                        }
                    } catch (final Throwable e) {
                        if (logger != null) {
                            logger.log(e);
                        }
                        otherIgnoreEntries.add(maindomainCleaned);
                        continue;
                    }
                }
                best.add(plugin);
            }
            if (best.size() == 0) {
                unassignedMultiHostSupport.add(maindomainCleaned);
                continue cleanListLoop;
            } else if (best.size() > 1) {
                unassignedMultiHostSupport.add(maindomainCleaned);
                if (logger != null) {
                    logger.warning("Found more than one possible plugins for one domain: " + maindomainCleaned);
                    logger.log(new Exception("DEBUG: " + maindomainCleaned));
                }
                continue cleanListLoop;
            }
            final LazyHostPlugin finalplugin = best.get(0);
            final String pluginHost = finalplugin.getHost();
            if (!finalresults.contains(pluginHost)) {
                finalresults.add(pluginHost);
            }
            // TODO: Improve this
            mhost.getDomains().clear();
            mhost.addDomain(pluginHost);
        }
        /**
         * Remove all "double" entries from remaining list of unmatched entries to avoid wrong log output. </br>
         * If a multihost provides multiple domains of one host e.g. "rg.to" and "rapidgator.net", the main one may have been matched but
         * "rg.to" may remain on the list of unassigned hosts.
         */
        for (final String item : otherIgnoreEntries) {
            unassignedMultiHostSupport.remove(item);
        }
        if (skippedOfflineEntries.size() > 0 && logger != null) {
            logger.info("Found " + skippedOfflineEntries.size() + " offline entries");
            for (final String host : skippedOfflineEntries) {
                logger.info("Skipped offline entry: " + host);
            }
        }
        if (skippedbyPluginAllowHandleEntries.size() > 0 && logger != null) {
            logger.info("Found " + skippedbyPluginAllowHandleEntries.size() + " skippedbyPluginAllowHandle entries");
            for (final String host : skippedbyPluginAllowHandleEntries) {
                logger.info("Skipped by allowHandle entry: " + host);
            }
        }
        if (skippedInvalidEntries.size() > 0 && logger != null) {
            logger.info("Found " + skippedInvalidEntries.size() + " skippedInvalid entries");
            for (final String host : skippedInvalidEntries) {
                logger.info("Skipped invalid entry: " + host);
            }
        }
        /* Most importantly: Log items without result */
        if (unassignedMultiHostSupport.size() > 0 && logger != null) {
            logger.info("Found " + unassignedMultiHostSupport.size() + " unassigned entries");
            for (final String host : unassignedMultiHostSupport) {
                logger.info("Could not assign any host for: " + host);
            }
        }
        if (finalresults.isEmpty()) {
            if (logger != null) {
                logger.info("Failed to find ANY usable results");
            }
            this.removeProperty(PROPERTY_MULTIHOST_SUPPORT);
            return null;
        }
        /* Log final results if wanted. */
        final boolean logValidResults = false;
        if (logger != null && logValidResults) {
            logger.info("Found real hosts: " + finalresults.size());
            for (final String host : finalresults) {
                logger.finest("Found host: " + host);
            }
        }
        /* sorting will now work properly since they are all pre-corrected to lowercase. */
        Collections.sort(finalresults, new NaturalOrderComparator());
        this.setProperty(PROPERTY_MULTIHOST_SUPPORT, new CopyOnWriteArrayList<String>(finalresults));
        return finalresults;
    }

    public List<String> getMultiHostSupport() {
        final Object ret = getProperty(PROPERTY_MULTIHOST_SUPPORT, null);
        if (ret == null) {
            return null;
        } else if (!(ret instanceof List)) {
            return null;
        }
        final List<String> list = (List<String>) ret;
        if (list.size() > 0) {
            return list;
        }
        return null;
    }

    /** 2024-09-06: wrapper function */
    public List<MultiHostHost> getMultiHostSupport2() {
        final List<String> domains = getMultiHostSupport();
        if (domains == null) {
            return null;
        } else if (domains.isEmpty()) {
            return null;
        }
        final List<MultiHostHost> mhosts = new ArrayList<MultiHostHost>();
        for (final String domain : domains) {
            final MultiHostHost mhost = new MultiHostHost(domain);
            mhosts.add(mhost);
        }
        return mhosts;
    }

    /** Returns information about specific host if it is supported. */
    public MultiHostHost getMultihostSupportedHost(final String domain) {
        final List<MultiHostHost> mhosts = getMultiHostSupport2();
        if (mhosts == null || mhosts.size() == 0) {
            return null;
        }
        for (final MultiHostHost mhost : mhosts) {
            if (mhost.supportsDomain(domain)) {
                return mhost;
            }
        }
        return null;
    }

    public void updateMultihostSupportedHost(final MultiHostHost mhost) {
        final List<MultiHostHost> mhosts = getMultiHostSupport2();
        if (mhosts == null || mhosts.size() == 0) {
            return;
        }
        mhosts.remove(mhost);
        mhosts.add(mhost);
        // this.setProperty(PROPERTY_MULTIHOST_SUPPORT, Property.NULL);
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
