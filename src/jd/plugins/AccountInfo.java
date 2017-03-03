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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import jd.config.Property;
import jd.http.Browser;
import jd.nutils.NaturalOrderComparator;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.plugins.controller.host.PluginFinder;

public class AccountInfo extends Property {

    private static final long serialVersionUID       = 1825140346023286206L;

    private long              account_validUntil     = -1;

    private long              account_trafficLeft    = -1;
    private long              account_trafficMax     = -1;

    private long              account_filesNum       = -1;
    private long              account_premiumPoints  = -1;
    private long              account_accountBalance = -1;
    private long              account_usedSpace      = -1;

    private String            account_status;
    private long              account_createTime     = 0;
    /**
     * indicator that host, account has special traffic handling, do not temp disable if traffic =0
     */
    private boolean           specialTraffic         = false;

    private boolean           account_trafficRefill  = true;

    public boolean isTrafficRefill() {
        return account_trafficRefill;
    }

    public void setTrafficRefill(boolean account_trafficRefill) {
        this.account_trafficRefill = account_trafficRefill;
    }

    public long getCreateTime() {
        return account_createTime;
    }

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
        long serverTime = -1;
        if (br != null && br.getHttpConnection() != null) {
            // lets use server time to determine time out value; we then need to adjust timeformatter reference +- time against server time
            final String dateString = br.getHttpConnection().getHeaderField("Date");
            if (dateString != null) {
                if (StringUtils.isNotEmpty(formatter)) {
                    serverTime = TimeFormatter.getMilliSeconds(dateString, formatter, Locale.ENGLISH);
                } else {
                    final Date date = TimeFormatter.parseDateString(dateString);
                    if (date != null) {
                        serverTime = date.getTime();
                    }
                }
            }
        }
        if (serverTime > 0) {
            final long a1 = validuntil + (System.currentTimeMillis() - serverTime);
            if (false) {
                final Date b1 = new Date(a1);
                final SimpleDateFormat s = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
                System.out.println("Valid until: " + s.format(b1));
            }
            setValidUntil(a1);
            return true;
        } else {
            // failover
            setValidUntil(validuntil);
            return false;
        }
    }

    /**
     * -1 für Niemals ablaufen
     *
     * @param validUntil
     */
    public void setValidUntil(final long validUntil) {
        this.account_validUntil = validUntil;
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
            return setMultiHostSupport(multiHostPlugin, multiHostSupport, new PluginFinder(multiHostPlugin.getLogger()));
        } else {
            final LogSource logSource = LogController.getFastPluginLogger(Thread.currentThread().getName());
            try {
                return setMultiHostSupport(multiHostPlugin, multiHostSupport, new PluginFinder(logSource));
            } finally {
                logSource.close();
            }
        }

    }

    public List<String> setMultiHostSupport(final PluginForHost multiHostPlugin, final List<String> multiHostSupportList, final PluginFinder pluginFinder) {
        if (multiHostSupportList != null && multiHostSupportList.size() > 0) {
            final HostPluginController hpc = HostPluginController.getInstance();
            final HashSet<String> assignedMultiHostSupport = new HashSet<String>();
            final HashMap<String, String> mapping = new HashMap<String, String>();
            {
                final HashSet<String> nonTldHosts = new HashSet<String>();
                // lets do some preConfiguring, and match hosts which do not contain tld
                for (final String host : multiHostSupportList) {
                    final String cleanup = host.trim().toLowerCase(Locale.ENGLISH);
                    mapping.put(host, cleanup);
                    if (cleanup.matches("http|https|file|up|upload|video|torrent|ftp")) {
                        // we need to ignore/blacklist common phrases, else too many false positives
                        continue;
                    } else if ("usenet".equals(cleanup)) {
                        // special cases
                        assignedMultiHostSupport.add(cleanup);
                    } else if (cleanup.indexOf('.') == -1) {
                        /*
                         * if the multihoster doesn't include full host name with tld, we can search and add all partial matches!
                         */
                        nonTldHosts.add(cleanup);
                    } else {
                        assignedMultiHostSupport.add(cleanup);
                    }
                }
                if (!nonTldHosts.isEmpty()) {
                    final HashMap<String, List<LazyHostPlugin>> map = new HashMap<String, List<LazyHostPlugin>>();
                    loop: for (final LazyHostPlugin lazyHostPlugin : hpc.list()) {
                        if (lazyHostPlugin.isFallbackPlugin()) {
                            continue;
                        }
                        if (lazyHostPlugin.isSitesSupported()) {
                            try {
                                final PluginForHost plugin = lazyHostPlugin.getPrototype(null);
                                if (plugin == null || plugin.getLazyP().isFallbackPlugin()) {
                                    continue;
                                }
                                final String[] siteSupportedNames = plugin.siteSupportedNames();
                                if (siteSupportedNames != null) {
                                    final Iterator<String> it = nonTldHosts.iterator();
                                    while (it.hasNext()) {
                                        final String nonTldHost = it.next();
                                        for (final String siteSupportedName : siteSupportedNames) {
                                            if (StringUtils.containsIgnoreCase(siteSupportedName, nonTldHost)) {
                                                List<LazyHostPlugin> list = map.get(nonTldHost);
                                                if (list == null) {
                                                    list = new ArrayList<LazyHostPlugin>();
                                                    map.put(nonTldHost, list);
                                                }
                                                list.add(lazyHostPlugin);
                                                break;
                                            }
                                        }
                                    }
                                    continue loop;
                                }
                            } catch (UpdateRequiredClassNotFoundException e) {
                                LogController.CL().log(e);
                            }
                        }
                        final String pattern = lazyHostPlugin.getPatternSource();
                        for (final String nonTldHost : nonTldHosts) {
                            if (StringUtils.containsIgnoreCase(pattern, nonTldHost)) {
                                List<LazyHostPlugin> list = map.get(nonTldHost);
                                if (list == null) {
                                    list = new ArrayList<LazyHostPlugin>();
                                    map.put(nonTldHost, list);
                                }
                                list.add(lazyHostPlugin);
                            }
                        }
                    }
                    for (Entry<String, List<LazyHostPlugin>> entry : map.entrySet()) {
                        final List<LazyHostPlugin> list = entry.getValue();
                        for (final LazyHostPlugin lazyHostPlugin : list) {
                            if (!lazyHostPlugin.isOfflinePlugin()) {
                                assignedMultiHostSupport.add(lazyHostPlugin.getHost());
                            }
                        }
                    }
                }
            }
            final List<String> unassignedMultiHostSupport = new ArrayList<String>(assignedMultiHostSupport);
            assignedMultiHostSupport.clear();
            Iterator<String> it = unassignedMultiHostSupport.iterator();
            while (it.hasNext()) {
                final String host = it.next();
                if (assignedMultiHostSupport.contains(host)) {
                    it.remove();
                    mapping.put(host, host);
                } else if (host != null) {
                    final String assignedHost;
                    if (pluginFinder == null) {
                        assignedHost = host;
                    } else {
                        assignedHost = pluginFinder.assignHost(host);
                    }
                    if (assignedHost != null) {
                        it.remove();
                        if (assignedMultiHostSupport.contains(assignedHost)) {
                            mapping.put(host, assignedHost);
                        } else {
                            final LazyHostPlugin lazyPlugin = hpc.get(assignedHost);
                            if (lazyPlugin != null && !lazyPlugin.isOfflinePlugin() && !lazyPlugin.isFallbackPlugin() && !assignedMultiHostSupport.contains(lazyPlugin.getHost())) {
                                try {
                                    if (!lazyPlugin.isHasAllowHandle()) {
                                        assignedMultiHostSupport.add(lazyPlugin.getHost());
                                        mapping.put(host, lazyPlugin.getHost());
                                    } else {
                                        final DownloadLink link = new DownloadLink(null, "", lazyPlugin.getHost(), "", false);
                                        if (lazyPlugin.getPrototype(null).allowHandle(link, multiHostPlugin)) {
                                            assignedMultiHostSupport.add(lazyPlugin.getHost());
                                            mapping.put(host, lazyPlugin.getHost());
                                        }
                                    }
                                } catch (final Throwable e) {
                                    LogController.CL().log(e);
                                }
                            }
                        }
                    }
                }
            }
            it = unassignedMultiHostSupport.iterator();
            while (it.hasNext()) {
                final String host = it.next();
                final String hostParts[] = host.split("\\.");
                if (hostParts.length == 2) {
                    for (final LazyHostPlugin lazyHostPlugin : hpc.list()) {
                        if (lazyHostPlugin.isFallbackPlugin() || lazyHostPlugin.isOfflinePlugin()) {
                            continue;
                        }
                        final String pattern = lazyHostPlugin.getPatternSource();
                        if (pattern.matches(".*(\\\\.|/|\\?:?|\\(|\\|)" + hostParts[0] + "(\\|.*?)?\\\\.(" + hostParts[1] + "|[^\\)]*" + hostParts[1] + ").*")) {
                            assignedMultiHostSupport.add(lazyHostPlugin.getHost());
                            mapping.put(host, lazyHostPlugin.getHost());
                            it.remove();
                            break;
                        }
                    }
                }
            }
            if (assignedMultiHostSupport.size() > 0) {
                // sorting will now work properly since they are all pre-corrected to lowercase.
                final List<String> list = new ArrayList<String>(assignedMultiHostSupport);
                Collections.sort(list, new NaturalOrderComparator());
                this.setProperty("multiHostSupport", new CopyOnWriteArrayList<String>(list));
                final List<String> ret = new ArrayList<String>();
                for (final String host : multiHostSupportList) {
                    final String map = mapping.get(host);
                    if (assignedMultiHostSupport.contains(map)) {
                        ret.add(map);
                    } else {
                        ret.add(null);
                    }
                }
                return ret;
            }
        }
        this.setProperty("multiHostSupport", Property.NULL);
        return null;
    }

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
}
