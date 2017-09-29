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
package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share-online.biz" }, urls = { "https?://(www\\.)?(share\\-online\\.biz|egoshare\\.com)/(download\\.php\\?id\\=|dl/)[\\w]+" })
public class ShareOnlineBiz extends antiDDoSForHost {
    private static final String                                     COOKIE_HOST                             = "http://share-online.biz";
    private static WeakHashMap<Account, HashMap<String, String>>    ACCOUNTINFOS                            = new WeakHashMap<Account, HashMap<String, String>>();
    private static WeakHashMap<Account, CopyOnWriteArrayList<Long>> THREADFAILURES                          = new WeakHashMap<Account, CopyOnWriteArrayList<Long>>();
    private static Object                                           LOCK                                    = new Object();
    private static HashMap<Long, Long>                              noFreeSlot                              = new HashMap<Long, Long>();
    private static HashMap<Long, Long>                              overloadedServer                        = new HashMap<Long, Long>();
    private long                                                    server                                  = -1;
    private long                                                    waitNoFreeSlot                          = 5 * 60 * 1000l;
    private long                                                    waitOverloadedServer                    = 5 * 60 * 1000l;
    /* Connection stuff */
    private static final boolean                                    free_resume                             = false;
    private static final int                                        free_maxchunks                          = 1;
    private static final int                                        free_maxdownloads                       = 1;
    private static final boolean                                    account_premium_resume                  = true;
    private static final int                                        account_premium_maxchunks               = 0;
    private static final int                                        account_premium_maxdownloads            = 10;
    private static final int                                        account_premium_vipspecial_maxdownloads = 2;
    private static final int                                        account_premium_penalty_maxdownloads    = 2;
    private boolean                                                 hideID                                  = true;
    private static AtomicInteger                                    maxChunksnew                            = new AtomicInteger(-2);
    private char[]                                                  FILENAMEREPLACES                        = new char[] { '_', '&', 'ü' };
    private final String                                            SHARED_IP_WORKAROUND                    = "SHARED_IP_WORKAROUND";
    private final String                                            TRAFFIC_WORKAROUND                      = "TRAFFIC_WORKAROUND";
    private final String                                            PREFER_HTTPS                            = "PREFER_HTTPS";
    private String                                                  trafficmaxlimit                         = "100";
    private final String                                            retrys                                  = "retrys";
    private final String[]                                          allretrys                               = new String[] { "100", "99", "98", "97", "96", "95", "90" };

    public ShareOnlineBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.share-online.biz/service.php?p=31353834353B4A44616363");
        setConfigElements();
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            Browser br = new Browser();
            // loadAPIWorkAround(br);
            br.setCookiesExclusive(true);
            br.setFollowRedirects(true);
            /* api does not support keep-alive */
            br.getHeaders().put(new HTTPHeader("Connection", "close"));
            StringBuilder sb = new StringBuilder();
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 80 links at once */
                    if (index == urls.length || links.size() > 200) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("links=");
                int c = 0;
                for (DownloadLink dl : links) {
                    if (c > 0) {
                        sb.append("\n");
                    }
                    sb.append(getID(dl));
                    c++;
                }
                br.setKeepResponseContentBytes(true);
                // because Request.setHTML(String) it nullifies byte array it will cause NPE here. .. do not call antiddos methods and hope
                // it will work.
                postPage(br, userProtocol() + "://api.share-online.biz/cgi-bin?q=checklinks&md5=1", sb.toString());
                final byte[] responseBytes = br.getRequest().getResponseBytes();
                final String infosUTF8[][] = new Regex(new String(responseBytes, "UTF-8"), Pattern.compile("(.*?);\\s*?(OK)\\s*?;(.*?)\\s*?;(\\d+);([0-9a-fA-F]{32})")).getMatches();
                final String infosISO88591[][] = new Regex(new String(responseBytes, "ISO-8859-1"), Pattern.compile("(.*?);\\s*?(OK)\\s*?;(.*?)\\s*?;(\\d+);([0-9a-fA-F]{32})")).getMatches();
                for (DownloadLink dl : links) {
                    String id = getID(dl);
                    int hit = -1;
                    for (int i = 0; i < infosUTF8.length; i++) {
                        if (infosUTF8[i][0].equalsIgnoreCase(id)) {
                            hit = i;
                            break;
                        }
                    }
                    if (hit == -1) {
                        /* id not in response, so its offline */
                        dl.setAvailable(false);
                    } else {
                        /* workaround for broken fileNames, some are ISO88591, others are UTF-8 */
                        final String fileNameUTF8 = infosUTF8[hit][2].trim();
                        final String fileNameISO88591 = infosISO88591[hit][2].trim();
                        final String fileName;
                        if (fileNameUTF8.length() < fileNameISO88591.length()) {
                            fileName = fileNameUTF8;
                        } else if (fileNameUTF8.length() > fileNameISO88591.length()) {
                            fileName = fileNameISO88591;
                        } else {
                            String bestFileName = null;
                            for (char chr : fileNameISO88591.toCharArray()) {
                                if (chr > 255) {
                                    /* iso88591 only uses first 256 points in char */
                                    bestFileName = fileNameUTF8;
                                    break;
                                }
                            }
                            if (bestFileName == null) {
                                fileName = fileNameISO88591;
                            } else {
                                fileName = bestFileName;
                            }
                        }
                        dl.setFinalFileName(fileName);
                        long size = -1;
                        dl.setDownloadSize(size = SizeFormatter.getSize(infosUTF8[hit][3]));
                        if (size > 0) {
                            dl.setProperty("VERIFIEDFILESIZE", size);
                        }
                        if (infosUTF8[hit][1].trim().equalsIgnoreCase("OK")) {
                            dl.setAvailable(true);
                            dl.setMD5Hash(infosUTF8[hit][4].trim());
                        } else {
                            dl.setAvailable(false);
                        }
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    private void loadAPIWorkAround(final Browser ibr) throws Exception {
        // need to manually do this as we are not calling cloudflare getPage/postPage/etc, we need cloudflare cookie loaded.
        super.prepBrowser(ibr, "share-online.biz");
        // we want to get page to get possible antiddos stops/checks and save cookie.
        if (ibr.getCookies("share-online.biz") == null || ibr.getCookies("share-online.biz").isEmpty()) {
            getPage(ibr, userProtocol() + "://share-online.biz/");
            // this should prevent any bleed over
            ibr.getHeaders().put("Referer", null);
        }
    }

    private String userProtocol() {
        if (userPrefersHttps()) {
            return "https";
        } else {
            return "http";
        }
    }

    private boolean userPrefersHttps() {
        return getPluginConfig().getBooleanProperty(PREFER_HTTPS, false);
    }

    private boolean userTrafficWorkaround() {
        return getPluginConfig().getBooleanProperty(TRAFFIC_WORKAROUND, false);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        // We do not have to change anything here, the regexp also works for egoshare links!
        String protocol = new Regex(link.getDownloadURL(), "(https?)://").getMatch(0);
        if (protocol.equalsIgnoreCase("http") && userPrefersHttps()) {
            protocol = userProtocol();
        }
        link.setUrlDownload(protocol + "://www.share-online.biz/dl/" + getID(link));
        if (hideID) {
            link.setName("download.php");
        }
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SHARED_IP_WORKAROUND, _GUI.T.gui_plugin_settings_share_online_shared_ip_workaround()).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), TRAFFIC_WORKAROUND, _GUI.T.gui_plugin_settings_share_online_traffic_workaround()).setDefaultValue(false));
        /**
         * https downloads are speed-limited serverside
         */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_HTTPS, _GUI.T.gui_plugin_settings_share_online_traffic_premium_prefer_https()).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), retrys, allretrys, JDL.L("", "Traffic max. Limit in GiB")).setDefaultValue(0));
    }

    private void errorHandling(Browser br, DownloadLink downloadLink, Account acc, HashMap<String, String> usedPremiumInfos) throws PluginException {
        Request request = br.getRequest();
        URLConnectionAdapter connection = request == null ? null : request.getHttpConnection();
        if (connection != null && connection.getResponseCode() == 500) {
            // HTTP/1.1 500 Internal Server Error
            // Date: Wed, 17 Jun 2015 01:20:04 GMT
            // Content-Type: text/html; charset=utf-8
            // Transfer-Encoding: chunked
            // Connection: keep-alive....
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server Error. Try again later...", 10 * 60 * 1000l);
        }
        /* file is offline */
        if (br.containsHTML("The requested file is not available")) {
            if (br.containsHTML("The server was not able to find the desired file")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The server was not able to find the desired file", 60 * 60 * 1000l);
            }
            logger.info("The following link was marked as online by the API but is offline: " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* no free slot */
        if (br.containsHTML("No free slots for free users") || br.getURL().contains("failure/full")) {
            downloadLink.getLinkStatus().setRetryCount(0);
            if (server != -1) {
                synchronized (noFreeSlot) {
                    noFreeSlot.put(server, System.currentTimeMillis());
                }
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.servernotavailable3", "No free Free-User Slots! Get PremiumAccount or wait!"), waitNoFreeSlot);
        }
        /* Max chunks/overall connections to the server reached (second condition if if-statement is just failover) */
        if (br.getURL().contains("failure/threads") || br.containsHTML(">Kein weiterer Download-Thread möglich")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.toomanyConnections", "Wait before starting new downloads"), 1 * 60 * 1000l);
        }
        if (br.containsHTML(">Share-Online - Server Maintenance<|>MAINTENANCE</h1>") || br.containsHTML("<title>Share-Online - Not available</title>")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.maintenance", "Server maintenance"), 30 * 60 * 1000l);
        }
        // shared IP error
        if (br.containsHTML("<strong>The usage of different IPs is not possible!</strong>")) {
            /* disabled as it causes problem, added debug to find cause */
            logger.info("IPDEBUG: " + br.toString());
            // // for no account!?
            // if (acc == null) {
            // throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "The usage of different IPs is not possible!", 60
            // * 60 * 1000L);
            // } else {
            // // for Premium
            // acc.setValid(false);
            // UserIO.getInstance().requestMessageDialog(0,
            // "ShareOnlineBiz Premium Error (account has been deactivated, free mode enabled)",
            // "Server reports: "
            // +
            // "You're trying to use your account from more than one IP-Adress.\n"
            // +
            // "The usage of different IP addresses is not allowed with every type of access,\nthe same affects any kind of account
            // sharing.\n"
            // +
            // "You are free to buy a further access for pay accounts, in order to use it from every place you want to.\n"
            // +
            // "A contempt of this rules can result in a complete account deactivation.");
            // throw new PluginException(LinkStatus.ERROR_PREMIUM, "Premium disabled, continued as free user");
            // }
        }
        String url = br.getURL();
        if (url.endsWith("/free/") || url.endsWith("/free")) {
            /* workaround when the redirect was missing */
            String windowLocation = br.getRegex("'(https?://[^']*?/failure/[^']*?)'").getMatch(0);
            if (windowLocation != null) {
                url = windowLocation;
            }
        }
        if (url.contains("failure/server")) {
            /* server offline */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server currently Offline", 2 * 60 * 60 * 1000l);
        }
        if (url.contains("failure/threads")) {
            /* already loading,too many threads */
            if (acc != null) {
                synchronized (LOCK) {
                    HashMap<String, String> infos = ACCOUNTINFOS.get(acc);
                    if (infos != null && usedPremiumInfos == infos) {
                        logger.info("Force refresh of cookies");
                        ACCOUNTINFOS.remove(acc);
                    }
                    CopyOnWriteArrayList<Long> failureThreads = THREADFAILURES.get(acc);
                    if (failureThreads == null) {
                        failureThreads = new CopyOnWriteArrayList<Long>();
                        THREADFAILURES.put(acc, failureThreads);
                    }
                    failureThreads.add(System.currentTimeMillis());
                    final AccountInfo ai = acc.getAccountInfo();
                    if (ai != null) {
                        final int maxDownloads = getMaxSimultanDownload(null, acc);
                        ai.setStatus(infos.get("group") + ":maxDownloads(current)=" + maxDownloads);
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ThreadsError", 3 * 60 * 1000l);
        }
        if (url.contains("failure/chunks")) {
            /* max chunks reached */
            String maxCN = new Regex(url, "failure/chunks/(\\d+)").getMatch(0);
            if (maxCN != null) {
                maxChunksnew.set(-Integer.parseInt(maxCN));
            }
            downloadLink.setChunksProgress(null);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "MaxChunks Error", 10 * 60 * 1000l);
        }
        if (url.contains("failure/freelimit")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "File too big, limited by the file owner.");
        }
        if (url.contains("failure/bandwidth")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        }
        if (url.contains("failure/filenotfound")) {
            try {
                final Browser br2 = new Browser();
                final String id = this.getID(downloadLink);
                getPage(br2, userProtocol() + "://api.share-online.biz/api/account.php?act=fileError&fid=" + id);
            } catch (Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (url.contains("failure/overload")) {
            if (server != -1) {
                synchronized (overloadedServer) {
                    overloadedServer.put(server, System.currentTimeMillis());
                }
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server overloaded", waitOverloadedServer);
        }
        if (url.contains("failure/precheck")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
        }
        if (url.contains("failure/invalid")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 15 * 60 * 1000l);
        }
        if (url.contains("failure/ip")) {
            if (acc != null && getPluginConfig().getBooleanProperty(SHARED_IP_WORKAROUND, false)) {
                logger.info("Using SharedIP workaround to avoid disabling the premium account!");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "SharedIPWorkaround", 2 * 60 * 1000l);
            }
            if (acc != null) {
                synchronized (LOCK) {
                    HashMap<String, String> infos = ACCOUNTINFOS.get(acc);
                    if (infos != null && usedPremiumInfos == infos) {
                        logger.info("Force refresh of cookies");
                        ACCOUNTINFOS.remove(acc);
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP Already loading", 15 * 60 * 1000l);
        }
        if (url.contains("failure/size")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "File too big. Premium needed!");
        }
        if (url.contains("failure/expired") || url.contains("failure/session") || br.containsHTML("<strong>This download ticket is expired")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wait for new ticket", 60 * 1000l);
        }
        if (url.contains("failure/cookie")) {
            if (acc != null) {
                synchronized (LOCK) {
                    HashMap<String, String> infos = ACCOUNTINFOS.get(acc);
                    if (infos != null && usedPremiumInfos == infos) {
                        logger.info("Force refresh of cookies");
                        ACCOUNTINFOS.remove(acc);
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "CookieError", 3 * 60 * 1000l);
        }
        if (br.containsHTML("nput invalid, halting. please avoid more of these requests")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
        }
        if (br.containsHTML("IP is temporary banned")) {
            if (acc != null) {
                synchronized (LOCK) {
                    HashMap<String, String> infos = ACCOUNTINFOS.get(acc);
                    if (infos != null && usedPremiumInfos == infos) {
                        logger.info("Force refresh of cookies");
                        ACCOUNTINFOS.remove(acc);
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP is temporary banned", 15 * 60 * 1000l);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        setBrowserExclusive();
        final HashMap<String, String> infos = loginAPI(account, true);
        if (isFree(account)) {
            try {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(free_maxdownloads);
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
        } else {
            try {
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(account_premium_maxdownloads);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            /* evaluate expire date */
            final Long validUntil = Long.parseLong(infos.get("expire_date"));
            account.setValid(true);
            if (validUntil > 0) {
                ai.setValidUntil(validUntil * 1000);
            } else {
                ai.setValidUntil(-1);
            }
            if (!StringUtils.equalsIgnoreCase(infos.get("group"), "VIP")) {
                /* VIP do not have traffic usage available via api */
                final int chosenRetrys = getPluginConfig().getIntegerProperty(retrys, 0);
                trafficmaxlimit = this.allretrys[chosenRetrys];
                final int maxTraffic = Integer.parseInt(trafficmaxlimit);
                final long maxDay = maxTraffic * 1024 * 1024 * 1024l;// 100 GiB per day
                final String trafficDay = infos.get("traffic_1d");
                final String trafficDayData[] = trafficDay.split(";");
                final long usedDay = Long.parseLong(trafficDayData[0].trim());
                final long freeDay = maxDay - usedDay;
                logger.info("Real daily traffic left: " + freeDay + ", account: " + account.getStringProperty("group", null));
                ai.setTrafficMax(maxDay);
                if (freeDay > 1024 * 1024) {
                    ai.setTrafficLeft(freeDay);
                } else {
                    // Penalty-Premium needs TrafficLeft > 0 (TrafficWorkaround, throttled accounts)
                    if (userTrafficWorkaround()) {
                        ai.setSpecialTraffic(true);
                        ai.setTrafficLeft(1024 * 1024);
                    } else {
                        ai.setSpecialTraffic(false);
                        ai.setTrafficLeft(Math.max(0, freeDay));
                    }
                }
            }
        }
        if (infos.containsKey("points")) {
            ai.setPremiumPoints(Long.parseLong(infos.get("points")));
        }
        if (infos.containsKey("money")) {
            ai.setAccountBalance(infos.get("money"));
        }
        /* set account type */
        final String group = infos.get("group");
        account.setProperty("group", group);
        final int maxDownloads = getMaxSimultanDownload(null, account);
        ai.setStatus(infos.get("group") + ":maxDownloads(current)=" + maxDownloads);
        return ai;
    }

    /** Logs in via share-online.biz site - only needed for free account download. */
    @SuppressWarnings("unchecked")
    private void loginSite(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                getPage("http://www.share-online.biz/");
                postPage("https://www.share-online.biz/user/login", "l_rememberme=1&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML("This account is disabled, please contact support")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "This account is disabled, please contact support", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* English language is needed for free download! */
                getPage("http://www.share-online.biz/lang/set/english");
                final URLConnectionAdapter con = br.getRequest().getHttpConnection();
                if (con.getResponseCode() == 502 || con.getResponseCode() == 504) {
                    throw new IOException(con.getResponseCode() + " " + con.getResponseMessage());
                }
                if (br.getCookie(COOKIE_HOST, "storage") == null) {
                    if (br.containsHTML("MAINTENANCE") || br.containsHTML(">Share-Online - Server Maintenance<|>MAINTENANCE</h1>") || br.containsHTML("<title>Share-Online - Not available</title>")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.shareonlinebiz.errors.maintenance", "Server maintenance"), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public String getAGBLink() {
        return userProtocol() + "://share-online.biz/rules.php";
    }

    private final String getID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "(id\\=|/dl/)([a-zA-Z0-9]+)").getMatch(1);
    }

    /* parse the response from api into an hashmap */
    private HashMap<String, String> getInfos(String response, String separator) throws PluginException {
        if (response == null || response.length() == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String infos[] = Regex.getLines(response);
        final HashMap<String, String> ret = new HashMap<String, String>();
        for (final String info : infos) {
            final String data[] = new Regex(info, "(.*?)" + Pattern.quote(separator) + "(.*)").getRow(0);
            if (data != null) {
                if (data.length == 1) {
                    ret.put(data[0].trim(), null);
                } else if (data.length == 2) {
                    if (StringUtils.isEmpty(data[1])) {
                        ret.put(data[0].trim(), null);
                    } else {
                        ret.put(data[0].trim(), data[1].trim());
                    }
                } else {
                    logger.warning("GetInfos failed, browser content:\n");
                    logger.warning(br.toString());
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        return ret;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    private final long THREADFAILURESTIMEOUT = 5 * 60 * 1000l;

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        if (account == null || isFree(account)) {
            return 1;
        } else {
            final int max;
            final String group = account.getStringProperty("group", null);
            if ("Penalty-Premium".equalsIgnoreCase(group)) {
                max = account_premium_penalty_maxdownloads;
            } else if ("VIP-Special".equalsIgnoreCase(group) || "VIP".equalsIgnoreCase(group)) {
                max = account_premium_vipspecial_maxdownloads;
            } else {
                final AccountInfo ai = account.getAccountInfo();
                if (userTrafficWorkaround() && ai != null) {
                    /**
                     * special handling for throttled accounts
                     */
                    if (ai.getTrafficLeft() > 1024 * 1024) {
                        max = account_premium_maxdownloads;
                    } else {
                        max = account_premium_penalty_maxdownloads;
                    }
                } else {
                    max = account_premium_maxdownloads;
                }
            }
            synchronized (LOCK) {
                CopyOnWriteArrayList<Long> failureThreads = THREADFAILURES.get(account);
                if (failureThreads != null) {
                    Iterator<Long> it = failureThreads.iterator();
                    while (it.hasNext()) {
                        final long next = it.next();
                        if (System.currentTimeMillis() - next > THREADFAILURESTIMEOUT) {
                            it.remove();
                        }
                    }
                    if (failureThreads.isEmpty()) {
                        THREADFAILURES.remove(account);
                    }
                    return Math.max(1, max - failureThreads.size());
                }
            }
            return max;
        }
    }

    protected void showFreeDialog(final String domain) {
        if (System.getProperty("org.jdownloader.revision") != null) { /* JD2 ONLY! */
            super.showFreeDialog(domain);
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String lng = System.getProperty("user.language");
                            String message = null;
                            String title = null;
                            String tab = "                        ";
                            if ("de".equalsIgnoreCase(lng)) {
                                title = domain + " Free Download";
                                message = "Du lädst im kostenlosen Modus von " + domain + ".\r\n";
                                message += "Wie bei allen anderen Hostern holt JDownloader auch hier das Beste für dich heraus!\r\n\r\n";
                                message += tab + "  Falls du allerdings mehrere Dateien\r\n" + "          - und das möglichst mit Fullspeed und ohne Unterbrechungen - \r\n" + "             laden willst, solltest du dir den Premium Modus anschauen.\r\n\r\nUnserer Erfahrung nach lohnt sich das - Aber entscheide am besten selbst. Jetzt ausprobieren?  ";
                            } else {
                                title = domain + " Free Download";
                                message = "You are using the " + domain + " Free Mode.\r\n";
                                message += "JDownloader always tries to get the best out of each hoster's free mode!\r\n\r\n";
                                message += tab + "   However, if you want to download multiple files\r\n" + tab + "- possibly at fullspeed and without any wait times - \r\n" + tab + "you really should have a look at the Premium Mode.\r\n\r\nIn our experience, Premium is well worth the money. Decide for yourself, though. Let's give it a try?   ";
                            }
                            if (CrossSystem.isOpenBrowserSupported()) {
                                int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                                if (JOptionPane.OK_OPTION == result) {
                                    CrossSystem.openURL(new URL("http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?" + domain + "&freedialog"));
                                }
                            }
                        } catch (Throwable e) {
                        }
                    }
                });
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        if (server != -1) {
            synchronized (noFreeSlot) {
                Long ret = noFreeSlot.get(server);
                if (ret != null) {
                    if (System.currentTimeMillis() - ret < waitNoFreeSlot) {
                        if (downloadLink.getLinkStatus().getRetryCount() >= 5) {
                            /*
                             * reset counter this error does not cause plugin to stop
                             */
                            downloadLink.getLinkStatus().setRetryCount(0);
                        }
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.servernotavailable3", "No free Free-User Slots! Get PremiumAccount or wait!"), waitNoFreeSlot);
                    } else {
                        noFreeSlot.remove(server);
                    }
                }
            }
            synchronized (overloadedServer) {
                Long ret = overloadedServer.get(server);
                if (ret != null) {
                    if (System.currentTimeMillis() - ret < waitOverloadedServer) {
                        if (downloadLink.getLinkStatus().getRetryCount() >= 5) {
                            /*
                             * reset counter this error does not cause plugin to stop
                             */
                            downloadLink.getLinkStatus().setRetryCount(0);
                        }
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server overloaded", waitOverloadedServer);
                    } else {
                        overloadedServer.remove(server);
                    }
                }
            }
        }
        // new browser will allow for new prepBrowser to load cookies
        br = new Browser();
        // redirects!
        br.setFollowRedirects(true);
        try {
            getPage(downloadLink.getDownloadURL().replace("https://", "http://"));
        } catch (final BrowserException e) {
            final Request request = e.getRequest();
            if (request != null && request.getHttpConnection() != null && request.getHttpConnection().getResponseCode() == 502) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.maintenance", "Server maintenance"), 30 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.unknownservererror", "Unknown server error"), 1 * 60 * 60 * 1000l);
        }
        if (br.getURL().contains("/failure/proxy/1")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Proxy error");
        }
        errorHandling(br, downloadLink, null, null);
        if (!br.containsHTML(">>> continue for free <<<") && !br.containsHTML(">>> kostenlos weiter <<<")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String ID = getID(downloadLink);
        postPage("/dl/" + ID + "/free/", "dl_free=1&choice=free");
        errorHandling(br, downloadLink, null, null);
        String wait = br.getRegex("var wait=(\\d+)").getMatch(0);
        boolean captcha = br.containsHTML("RECAPTCHA active");
        long startWait = 0;
        if (captcha == true) {
            startWait = System.currentTimeMillis();
        } else {
            if (wait != null) {
                this.sleep(Integer.parseInt(wait) * 1000l, downloadLink);
            }
        }
        String dlINFO = br.getRegex("var dl=\"(.*?)\"").getMatch(0);
        String url = Encoding.Base64Decode(dlINFO);
        if (captcha) {
            /* recaptcha handling */
            final Recaptcha rc = new Recaptcha(br, this);
            rc.setId("6LdatrsSAAAAAHZrB70txiV5p-8Iv8BtVxlTtjKX");
            rc.load();
            long last = -1;
            int imax = 15;
            final long sessionTimeout = startWait + 300 * 1000l;
            while (true) {
                if (imax-- == 0 || System.currentTimeMillis() > sessionTimeout) {
                    /*
                     * must be handled correctly! reload in loop and then abort loop (timeout/imax) is not handled at all, falls through
                     * till uknown error
                     */
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                getLogger().info("Captcha Try " + (20 - imax));
                if (System.currentTimeMillis() - last < 2000) {
                    // antiddos
                    sleep(2000 - (System.currentTimeMillis() - last), downloadLink);
                }
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode("recaptcha", cf, downloadLink);
                if (StringUtils.isEmpty(c)) {
                    rc.reload();
                    continue;
                }
                if (wait != null) {
                    long gotWait = Integer.parseInt(wait) * 500l;
                    long waited = System.currentTimeMillis() - startWait;
                    gotWait -= waited;
                    if (gotWait > 0) {
                        this.sleep(gotWait, downloadLink);
                    }
                }
                postPage("/dl/" + ID + "/free/captcha/" + System.currentTimeMillis(), "dl_free=1&recaptcha_response_field=" + Encoding.urlEncode(c) + "&recaptcha_challenge_field=" + rc.getChallenge());
                url = br.getRegex("([a-zA-Z0-9/=]+)").getMatch(0);
                if ("0".equals(url)) {
                    rc.reload();
                    continue;
                } else {
                    break;
                }
            }
            if ("0".equals(url)) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            url = Encoding.Base64Decode(url);
            if (url == null || !url.startsWith("http")) {
                url = null;
            }
            if (wait != null) {
                this.sleep(Integer.parseInt(wait) * 1000l, downloadLink);
            }
        }
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        if (url != null && url.trim().length() == 0) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 5 * 60 * 1000l);
        }
        if (br.containsHTML(">Proxy-Download not supported for free access")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Proxy download not supported for free access", 5 * 60 * 1000l);
        }
        if (url == null || !url.startsWith("http")) {
            logger.info("share-online.biz: Unknown error");
            int timesFailed = downloadLink.getIntegerProperty("timesfailedshareonlinebiz_unknown", 0);
            if (timesFailed <= 2) {
                timesFailed++;
                downloadLink.setProperty("timesfailedshareonlinebiz_unknown", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
            } else {
                downloadLink.setProperty("timesfailedshareonlinebiz_unknown", Property.NULL);
                logger.info("share-online.biz: Unknown error - Plugin broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        br.setCookie(url, "version", String.valueOf(getVersion()));
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, free_resume, free_maxchunks);
        if (dl.getConnection().isContentDisposition() || (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("octet-stream"))) {
            try {
                validateLastChallengeResponse();
            } catch (final Throwable e) {
            }
            dl.startDownload();
        } else {
            try {
                invalidateLastChallengeResponse();
            } catch (final Throwable e) {
            }
            br.followConnection();
            errorHandling(br, downloadLink, null, null);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            prepBr.getHeaders().put("Accept-Language", "en-us,de;q=0.7,en;q=0.3");
            prepBr.getHeaders().put("Pragma", null);
            prepBr.getHeaders().put("Cache-Control", null);
            prepBr.setCookie("share-online.biz", "page_language", "english");
        }
        return prepBr;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        final HashMap<String, String> infos = loginAPI(account, false);
        if (isFree(account)) {
            requestFileInformation(link);
            doFree(link);
        } else {
            // linkcheck otherwise users get banned ip when trying to download offline content. jdlog://3063296763241/
            requestFileInformation(link);
            final boolean preferHttps = userPrefersHttps() && !StringUtils.equalsIgnoreCase(account.getStringProperty("group", null), "VIP");
            final String linkID = getID(link);
            String dlC = infos.get("dl");
            if (dlC != null && !"not_available".equalsIgnoreCase(dlC)) {
                if (preferHttps) {
                    br.setCookie("https://www.share-online.biz", "dl", dlC);
                } else {
                    br.setCookie("http://www.share-online.biz", "dl", dlC);
                }
            }
            String a = infos.get("a");
            if (a != null && !"not_available".equalsIgnoreCase(a)) {
                if (preferHttps) {
                    br.setCookie("https://www.share-online.biz", "a", a);
                } else {
                    br.setCookie("http://www.share-online.biz", "a", a);
                }
            }
            // loadAPIWorkAround(br);
            br.setFollowRedirects(true);
            br.setKeepResponseContentBytes(true);
            getPage(userProtocol() + "://api.share-online.biz/cgi-bin?q=linkdata&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&lid=" + linkID);
            final byte[] responseBytes = br.getRequest().getResponseBytes();
            final String responseUTF8 = new String(responseBytes, "UTF-8");
            final String responseISO88591 = new String(responseBytes, "ISO-8859-1");
            br.setKeepResponseContentBytes(false);
            if (responseUTF8.contains("** USER DATA INVALID")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (br.containsHTML("your IP is temporary banned")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (responseUTF8.contains("** REQUESTED DOWNLOAD LINK NOT FOUND **")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (responseUTF8.contains("EXCEPTION request download link not found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            // These are NO API errors
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Share-Online - Page not found - #404<|The desired content is not available")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverfehler 404, bitte warten...", 30 * 1000l);
            }
            if (br.getHttpConnection().getResponseCode() == 502 || br.containsHTML("<title>Share-Online - The page is temporarily unavailable</title>")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverfehler 502, bitte warten...", 30 * 1000l);
            }
            final HashMap<String, String> dlInfos = getInfos(responseUTF8, ": ");
            final String fileNameUTF8 = dlInfos.get("NAME");
            final String fileNameISO88591 = getInfos(responseISO88591, ": ").get("NAME");
            final String size = dlInfos.get("SIZE");
            final String status = dlInfos.get("STATUS");
            if (fileNameUTF8 == null || size == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setMD5Hash(dlInfos.get("MD5"));
            if (!"online".equalsIgnoreCase(status)) {
                if ("server_under_maintenance".equalsIgnoreCase(dlInfos.get("URL"))) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server currently Offline", 2 * 60 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (size != null) {
                link.setDownloadSize(Long.parseLong(size));
            }
            /* workaround for broken fileNames, some are ISO88591, others are UTF-8 */
            final String fileName;
            if (fileNameUTF8.length() < fileNameISO88591.length()) {
                fileName = fileNameUTF8;
            } else if (fileNameUTF8.length() > fileNameISO88591.length()) {
                fileName = fileNameISO88591;
            } else {
                String bestFileName = null;
                for (char chr : fileNameISO88591.toCharArray()) {
                    if (chr > 255) {
                        /* iso88591 only uses first 256 points in char */
                        bestFileName = fileNameUTF8;
                        break;
                    }
                }
                if (bestFileName == null) {
                    fileName = fileNameISO88591;
                } else {
                    fileName = bestFileName;
                }
            }
            if (link.getFinalFileName() != null) {
                link.setFinalFileName(fileName);
            }
            String dlURL = dlInfos.get("URL");
            // http://api.share-online.biz/api/account.php?act=fileError&fid=FILE_ID
            if (dlURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if ("server_under_maintenance".equals(dlURL)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server currently Offline", 2 * 60 * 60 * 1000l);
            }
            br.setFollowRedirects(true);
            /* Datei herunterladen */
            /* api does allow resume, but only 1 chunk */
            if (preferHttps) {
                dlURL = dlURL.replace("http://", "https://");
            }
            logger.info("used url: " + dlURL);
            br.setDebug(true);
            br.setCookie(dlURL, "version", String.valueOf(getVersion()));
            int maxchunks = account_premium_maxchunks;
            maxchunks = maxChunksnew.get();
            if ("Penalty-Premium".equalsIgnoreCase(account.getStringProperty("group", null))) {
                logger.info("Account is in penalty, limiting max chunks to 1");
                maxchunks = 1;
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlURL, account_premium_resume, maxchunks);
            if (dl.getConnection().isContentDisposition() || (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("octet-stream"))) {
                dl.startDownload();
            } else {
                br.followConnection();
                errorHandling(br, link, account, infos);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    public HashMap<String, String> loginAPI(final Account account, final boolean forceLogin) throws Exception {
        final String lang = System.getProperty("user.language");
        synchronized (LOCK) {
            try {
                HashMap<String, String> infos = ACCOUNTINFOS.get(account);
                if (infos == null || forceLogin) {
                    final boolean follow = br.isFollowingRedirects();
                    br.setFollowRedirects(true);
                    final String page;
                    try {
                        page = apiGetPage(userProtocol() + "://api.share-online.biz/cgi-bin?q=userdetails&aux=traffic&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    } finally {
                        br.setFollowRedirects(follow);
                    }
                    if (StringUtils.contains(page, "** INVALID USER DATA **")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    infos = getInfos(page, "=");
                    ACCOUNTINFOS.put(account, infos);
                }
                /* check dl cookie, must be available for premium accounts */
                final String dl = infos.get("dl");
                final String a = infos.get("a");
                final String register_date = infos.get("register_date");
                if (register_date != null && register_date.matches("^\\d+$")) {
                    account.setRegisterTimeStamp(Long.parseLong(register_date) * 1000l);
                }
                if (dl == null && a == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /*
                 * Directly after a premium account expires it can happen that API still returns "Premium" as "Group" but download is
                 * impossible --> It is actually a free account then
                 */
                boolean premium_valid = dl != null && !"not_available".equalsIgnoreCase(dl);
                if (premium_valid == false) {
                    premium_valid = a != null && !"not_available".equalsIgnoreCase(a);
                }
                if ("Sammler".equals(infos.get("group")) || !premium_valid) {
                    account.setProperty("free", true);
                    account.setType(AccountType.FREE);
                    try {
                        /* Login via site is needed for free account download. */
                        this.loginSite(account, forceLogin);
                    } catch (final PluginException e) {
                        if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM && e.getValue() == PluginException.VALUE_ID_PREMIUM_DISABLE) {
                            if ("de".equalsIgnoreCase(lang)) {
                                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLoginversuch per Sammler Account schlug fehl - bitte dem JDownloader Support melden!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                            } else {
                                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nFailed to login via free account - please contact the JDownloader support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                            }
                        } else {
                            throw e;
                        }
                    }
                    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
                    // "\r\nEs werden nur share-online Premiumaccounts akzeptiert, dies ist ein Sammleraccount!\r\nJDownloader only accepts
                    // premium accounts, this is a collectors account!",
                    // PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    account.setProperty("free", false);
                    account.setType(AccountType.PREMIUM);
                    /*
                     * check expire date, expire >0 (normal handling) expire<0 (never expire)
                     */
                    final Long validUntil = Long.parseLong(infos.get("expire_date"));
                    if (validUntil > 0 && (System.currentTimeMillis() / 1000) > validUntil) {
                        if (account.getAccountInfo() != null) {
                            // may be null if the account has been added expired
                            account.getAccountInfo().setExpired(true);
                        }
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account expired! || Account abgelaufen!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                return infos;
            } catch (PluginException e) {
                account.setProperty("group", Property.NULL);
                ACCOUNTINFOS.remove(account);
                throw e;
            }
        }
    }

    /* Used for API request - also handles (server) errors */
    private String apiGetPage(final String link) throws Exception {
        // loadAPIWorkAround(br);
        getPage(link);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404");
        }
        return br.toString();
    }

    private String execJS(final String fun) throws Exception {
        Object result = new Object();
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            // // document.getElementById('id').href
            // engine.eval("var document = { getElementById: function (a) { if (!this[a]) { this[a] = new Object(); function href() { return
            // a.href; } this[a].href = href(); } return this[a]; }};");
            // engine.eval(fun);
            // tools.js
            engine.eval("function info(a){a=a.split(\"\").reverse().join(\"\").split(\"a|b\");var b=a[1].split(\"\");a[1]=new Array();var i=0;for(j=0;j<b.length;j++){if(j%3==0&&j!=0){i++}if(typeof(a[1][i])==\"undefined\"){a[1][i]=\"\"}a[1][i]+=b[j]}b=new Array();a[0]=a[0].split(\"\");for(i=0;i<a[1].length;i++){a[1][i]=parseInt(a[1][i].toUpperCase(),16);b[a[1][i]]=parseInt(i)}a[1]=\"\";for(i=0;i<b.length;i++){if(typeof(a[0][b[i]])!=\"undefined\"){a[1]+=a[0][b[i]]}else{a[1]+=\" \"}}return a[1]}");
            engine.eval("var result=info(nfo);");
            result = engine.get("result");
        } catch (final Throwable e) {
            throw new Exception("JS Problem in Rev" + getVersion(), e);
        }
        return result == null ? null : result.toString();
    }

    /**
     * this is API!
     */
    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        // revert
        hideID = false;
        correctDownloadLink(downloadLink);
        server = -1;
        // loadAPIWorkAround(br);
        br.setCookie("http://www.share-online.biz", "king_mylang", "en");
        String id = getID(downloadLink);
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.setKeepResponseContentBytes(true);
        try {
            postPage(userProtocol() + "://api.share-online.biz/cgi-bin?q=checklinks&md5=1&snr=1", "links=" + id);
            final byte[] responseBytes = br.getRequest().getResponseBytes();
            if (br.getRequest().getHtmlCode().matches("\\s*")) {
                // web method failover.
                br = new Browser();
                String startURL = downloadLink.getDownloadURL();
                // workaround to bypass new layout and use old site
                getPage(startURL += startURL.contains("?") ? "&v2=1" : "?v2=1");
                // we only use this direct mode if the API failed twice! in this case this is the only way to get the information
                String js = br.getRegex("var dl=[^\r\n]*").getMatch(-1);
                js = execJS(js);
                String[] strings = js.split(",");
                if (strings == null || strings.length != 5) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final long size = Long.parseLong(strings[0].trim());
                downloadLink.setDownloadSize(size);
                if (size > 0) {
                    downloadLink.setProperty("VERIFIEDFILESIZE", size);
                }
                if (downloadLink.getFinalFileName() == null) {
                    /* website never final! */
                    downloadLink.setName(strings[3].trim());
                }
                downloadLink.setMD5Hash(strings[1]);
                return AvailableStatus.TRUE;
            }
            final String infosUTF8[] = new Regex(new String(responseBytes, "UTF-8"), Pattern.compile("(.*?);([^;]+);(.*?)\\s*?;(\\d+);([0-9a-fA-F]{32});(\\d+)")).getRow(0);
            final String infosISO88591[] = new Regex(new String(responseBytes, "ISO-8859-1"), Pattern.compile("(.*?);([^;]+);(.*?)\\s*?;(\\d+);([0-9a-fA-F]{32});(\\d+)")).getRow(0);
            if (infosUTF8 == null || !infosUTF8[1].equalsIgnoreCase("OK")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* workaround for broken fileNames, some are ISO88591, others are UTF-8 */
            final String fileNameUTF8 = infosUTF8[2].trim();
            final String fileNameISO88591 = infosISO88591[2].trim();
            final String fileName;
            if (fileNameUTF8.length() < fileNameISO88591.length()) {
                fileName = fileNameUTF8;
            } else if (fileNameUTF8.length() > fileNameISO88591.length()) {
                fileName = fileNameISO88591;
            } else {
                String bestFileName = null;
                for (char chr : fileNameISO88591.toCharArray()) {
                    if (chr > 255) {
                        /* iso88591 only uses first 256 points in char */
                        bestFileName = fileNameUTF8;
                        break;
                    }
                }
                if (bestFileName == null) {
                    fileName = fileNameISO88591;
                } else {
                    fileName = bestFileName;
                }
            }
            final long size = Long.parseLong(infosUTF8[3].trim());
            downloadLink.setDownloadSize(size);
            if (size > 0) {
                downloadLink.setProperty("VERIFIEDFILESIZE", size);
            }
            if (downloadLink.getFinalFileName() == null) {
                downloadLink.setFinalFileName(fileName);
            }
            downloadLink.setMD5Hash(infosUTF8[4].trim());
            server = Long.parseLong(infosUTF8[5].trim());
            return AvailableStatus.TRUE;
        } finally {
            br.setKeepResponseContentBytes(false);
        }
    }

    private boolean isFree(final Account acc) {
        return acc.getBooleanProperty("free", false) || AccountType.FREE.equals(acc.getType());
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        synchronized (noFreeSlot) {
            noFreeSlot.clear();
        }
    }

    public String filterPackageID(String packageIdentifier) {
        return packageIdentifier.replaceAll("([^a-zA-Z0-9]+)", "");
    }

    public char[] getFilenameReplaceMap() {
        return FILENAMEREPLACES;
    }

    public boolean isHosterManipulatesFilenames() {
        return true;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}