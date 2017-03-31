//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.utils.locale.JDL;

import org.jdownloader.plugins.ConditionalSkipReasonException;
import org.jdownloader.plugins.WaitingSkipReason;
import org.jdownloader.plugins.WaitingSkipReason.CAUSE;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premium.to" }, urls = { "https?://torrent[a-z0-9]*?\\.(premium\\.to|premium4\\.me)/(t|z)/[^<>/\"]+(/[^<>/\"]+){0,1}(/\\d+)*|https?://storage[a-z0-9]*?\\.(?:premium\\.to|premium4\\.me)/file/[A-Z0-9]+" })
public class PremiumTo extends UseNet {
    private static WeakHashMap<Account, HashMap<String, Long>> hostUnavailableMap             = new WeakHashMap<Account, HashMap<String, Long>>();

    private final String                                       noChunks                       = "noChunks";
    private static Object                                      LOCK                           = new Object();
    private final String                                       normalTraffic                  = "normalTraffic";
    private final String                                       specialTraffic                 = "specialTraffic";
    private static final String                                lang                           = System.getProperty("user.language");
    private static final String                                CLEAR_DOWNLOAD_HISTORY_STORAGE = "CLEAR_DOWNLOAD_HISTORY";
    private static final String                                type_storage                   = "https?://storage.+";
    private static final String                                type_torrent                   = "https?://torrent.+";
    private static final String                                API_BASE                       = "http://api.premium.to/";

    public PremiumTo(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2 * 1000L);
        this.enablePremium("http://premium.to/");
        setConfigElements();
    }

    public static interface PremiumToConfigInterface extends UsenetAccountConfigInterface {
    };

    @Override
    public String rewriteHost(String host) {
        if (host == null || "premium4.me".equals(host) || "premium.to".equals(host)) {
            return "premium.to";
        }
        return super.rewriteHost(host);
    }

    private Browser prepBrowser(Browser prepBr) {
        prepBr.setFollowRedirects(true);
        prepBr.setAcceptLanguage("en, en-gb;q=0.8");
        prepBr.setConnectTimeout(90 * 1000);
        prepBr.setReadTimeout(90 * 1000);
        prepBr.getHeaders().put("User-Agent", "JDownloader");
        prepBr.setAllowedResponseCodes(new int[] { 400 });
        return prepBr;
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account, AbstractProxySelectorImpl proxy) {
        if (link != null && "keep2share.cc".equals(link.getHost())) {
            return 1;
        } else if (link != null && "share-online.biz".equals(link.getHost())) {
            // re admin: only 1 possible
            return 1;
        } else {
            return super.getMaxSimultanDownload(link, account, proxy);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setProperty("multiHostSupport", Property.NULL);
            account.setValid(false);
            throw e;
        }
        Browser tbr = br.cloneBrowser();
        tbr.getPage("http://" + this.getHost() + "/sstraffic.php");
        /* NormalTraffic:SpecialTraffic:TorrentTraffic */
        String[] traffic = tbr.toString().split(";");
        if (traffic != null && traffic.length == 3) {
            // because we can not account for separate traffic allocations.
            /* Normal traffic */
            final long nT = Long.parseLong(traffic[0]);
            /* Special traffic */
            final long spT = Long.parseLong(traffic[1]);
            /* Storage traffic */
            final long stT = Long.parseLong(traffic[2]);
            ac.setTrafficLeft(nT + spT + stT + "MiB");
            // set both so we can check in canHandle.
            account.setProperty(normalTraffic, nT + stT);
            account.setProperty(specialTraffic, spT);
        }
        {
            final Browser hbr = br.cloneBrowser();
            hbr.getPage(API_BASE + "hosts.php");
            final String hosters[] = hbr.toString().split(";|\\s+");
            if (hosters != null && hosters.length != 0) {
                final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosters));
                supportedHosts.add("usenet");
                ac.setMultiHostSupport(this, supportedHosts);
            }
        }
        account.setValid(true);
        ac.setStatus("Premium account");
        return ac;
    }

    @Override
    public String getAGBLink() {
        return "http://premium.to/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST, FEATURE.USENET };
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else {
            login(account, false);
            String url = link.getDownloadURL();
            // allow resume and up to 10 chunks
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, -10);
            if (dl.getConnection().getResponseCode() == 403) {
                /*
                 * This e.g. happens if the user deletes a file via the premium.to site and then tries to download the previously added link
                 * via JDownloader.
                 */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403 (file offline?)", 30 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            dl.startDownload();
        }
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        final boolean redirect = br.isFollowingRedirects();
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                prepBrowser(br);
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
                            br.setCookie(this.getHost(), key, value);
                        }
                        return;
                    }
                }
                br.postPageRaw(API_BASE + "login.php", "{\"u\":\"" + Encoding.urlEncode(account.getUser()) + "\", \"p\":\"" + Encoding.urlEncode(account.getPass()) + "\", \"r\":true}");
                if (br.getHttpConnection().getResponseCode() == 400) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.getCookie(this.br.getHost(), "auth") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(this.getHost());
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            } finally {
                br.setFollowRedirects(redirect);
            }
        }
    }

    /** no override to keep plugin compatible to old stable */
    @SuppressWarnings("deprecation")
    public void handleMultiHost(DownloadLink link, Account account) throws Exception {
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else {
            synchronized (hostUnavailableMap) {
                HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
                if (unavailableMap != null) {
                    Long lastUnavailable = unavailableMap.get(link.getHost());
                    if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                        final long wait = lastUnavailable - System.currentTimeMillis();
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost(), wait);
                    } else if (lastUnavailable != null) {
                        unavailableMap.remove(link.getHost());
                        if (unavailableMap.size() == 0) {
                            hostUnavailableMap.remove(account);
                        }
                    }
                }
            }
            dl = null;
            String url = link.getDownloadURL().replaceFirst("https?://", "");
            // this here is bullshit... multihoster side should do all the corrections.
            /* begin code from premium.to support */
            if (url.startsWith("http://")) {
                url = url.substring(7);
            }
            if (url.startsWith("www.")) {
                url = url.substring(4);
            }
            if (url.startsWith("freakshare.com/")) {
                url = url.replaceFirst("freakshare.com/", "fs.com/");
            } else if (url.startsWith("depositfiles.com/")) {
                url = url.replaceFirst("depositfiles.com/", "df.com/");
            } else if (url.startsWith("turbobit.net/")) {
                url = url.replaceFirst("turbobit.net/", "tb.net/");
            } else if (url.startsWith("filefactory.com/")) {
                url = url.replaceFirst("filefactory.com/", "ff.com/");
            } else if (url.startsWith("k2s.cc/")) {
                // doesn't work...
                // url = url.replaceFirst("k2s.cc/", "keep2share.cc/");
            }
            /* end code from premium.to support */
            if (url.startsWith("oboom.com/")) {
                url = url.replaceFirst("oboom.com/#", "oboom.com/");
            }
            url = Encoding.urlEncode(url);
            showMessage(link, "Phase 1/3: Login...");
            login(account, false);
            showMessage(link, "Phase 2/3: Get link");
            int connections = getConnections(link.getHost());
            if (link.getChunks() != -1) {
                if (connections < 1) {
                    connections = link.getChunks();
                }
            }
            if (link.getBooleanProperty(noChunks, false)) {
                connections = 1;
            }
            String finalURL = API_BASE + "getfile.php?link=" + url;
            final DownloadLinkDownloadable downloadable;
            if (link.getName().matches(".+(rar|r\\d+)$")) {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                final URLConnectionAdapter con = brc.openGetConnection(finalURL);
                try {
                    if (con.isOK() && con.isContentDisposition() && con.getLongContentLength() > 0) {
                        finalURL = con.getRequest().getUrl();
                        if (link.getVerifiedFileSize() != -1 && link.getVerifiedFileSize() != con.getLongContentLength()) {
                            logger.info("Workaround for size missmatch(rar padding?!)!");
                            link.setVerifiedFileSize(con.getLongContentLength());
                        }
                    }
                } finally {
                    con.disconnect();
                }
                downloadable = new DownloadLinkDownloadable(link) {

                    @Override
                    public boolean isHashCheckEnabled() {
                        return false;
                    }

                };
            } else {
                downloadable = new DownloadLinkDownloadable(link);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadable, br.createGetRequest(finalURL), true, connections);
            if (dl.getConnection().getResponseCode() == 404) {
                /* file offline */
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!dl.getConnection().isContentDisposition()) {
                if (dl.getConnection().getResponseCode() == 420) {
                    dl.close();
                    int timesFailed = link.getIntegerProperty("timesfailedpremiumto_420dlerror", 0);
                    link.getLinkStatus().setRetryCount(0);
                    logger.info("premium.to: Download attempt failed because of server error 420");
                    if (timesFailed <= 5) {
                        timesFailed++;
                        link.setProperty("timesfailedpremiumto_420dlerror", timesFailed);
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Download could not be started (420)");
                    } else {
                        link.setProperty("timesfailedpremiumto_420dlerror", Property.NULL);
                        logger.info("premium.to: 420 download error - disabling current host!");
                        tempUnavailableHoster(account, link, 60 * 60 * 1000l);
                    }
                }
                br.followConnection();
                logger.severe("PremiumTo Error");
                if (br.toString().matches("File not found")) {
                    // we can not trust multi-hoster file not found returns, they could be wrong!
                    // jiaz new handling to dump to next download candidate.
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
                if (br.toString().matches("File hosting service not supported")) {
                    tempUnavailableHoster(account, link, 60 * 60 * 1000);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                /*
                 * after x retries we disable this host and retry with normal plugin
                 */
                if (link.getLinkStatus().getRetryCount() >= 3) {
                    /* disable hoster for 1h */
                    tempUnavailableHoster(account, link, 60 * 60 * 1000);
                    /* reset retry counter */
                    link.getLinkStatus().setRetryCount(0);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                String msg = "(" + (link.getLinkStatus().getRetryCount() + 1) + "/" + 3 + ")";
                showMessage(link, msg);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 10 * 1000l);
            }
            showMessage(link, "Phase 3/3: Download...");
            try {
                /* Check if the download is successful && user wants JD to delete the file in his premium.to account afterwards. */
                if (dl.startDownload() && this.getPluginConfig().getBooleanProperty(CLEAR_DOWNLOAD_HISTORY_STORAGE, default_clear_download_history_storage) && link.getDownloadURL().matches(type_storage)) {
                    boolean success = false;
                    try {
                        /*
                         * TODO: Check if there is a way to determine if the deletion was successful and add loggers for
                         * successful/unsuccessful cases!
                         */
                        br.getPage("https://storage." + this.getHost() + "/removeFile.php?f=" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
                        success = true;
                    } catch (final Throwable e) {
                        /* Don't fail here */
                    }
                    if (success) {
                        logger.info("Deletion of downloaded file seems to be successful");
                    } else {
                        logger.warning("Deletion of downloaded file seems have failed");
                    }
                }
            } catch (final PluginException ex) {
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(noChunks, false) == false) {
                    link.setProperty(noChunks, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY, null, -1, ex);
                }
            }

        }
    }

    /**
     * JD 2 Code. DO NOT USE OVERRIDE FOR COMPATIBILITY REASONS
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (isUsenetLink(link)) {
            return super.requestFileInformation(link);
        } else {
            final String dlink = Encoding.urlDecode(link.getDownloadURL(), true);
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            long fileSize = -1;
            ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(this.getHost());
            if (accs == null || accs.size() == 0) {
                if (link.getDownloadURL().matches(type_storage)) {
                    /* This linktype can only be downloaded/checked via account */
                    link.getLinkStatus().setStatusText("Only downlodable via account!");
                    return AvailableStatus.UNCHECKABLE;
                }
                /* try without login (only possible for links with token) */
                try {
                    con = br.openGetConnection(dlink);
                    if (!con.getContentType().contains("html")) {
                        fileSize = con.getLongContentLength();
                        if (fileSize == 0) {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        } else if (fileSize == -1) {
                            link.getLinkStatus().setStatusText("Only downlodable via account!");
                            return AvailableStatus.UNCHECKABLE;
                        }
                        String name = con.getHeaderField("Content-Disposition");
                        if (name != null) {
                            /* filter the filename from content disposition and decode it... */
                            name = new Regex(name, "filename.=UTF-8\'\'([^\"]+)").getMatch(0);
                            name = Encoding.UTF8Decode(name).replaceAll("%20", " ");
                            if (name != null) {
                                link.setFinalFileName(name);
                            }
                        }
                        link.setDownloadSize(fileSize);
                        return AvailableStatus.TRUE;
                    } else {
                        return AvailableStatus.UNCHECKABLE;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            } else {
                // if accounts available try all whether the link belongs to it links with token should work anyway
                for (Account acc : accs) {
                    login(acc, false);
                    try {
                        con = br.openGetConnection(dlink);
                        if (!con.getContentType().contains("html")) {
                            fileSize = con.getLongContentLength();
                            if (fileSize <= 0) {
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                            link.setDownloadSize(fileSize);
                            String name = con.getHeaderField("Content-Disposition");
                            if (name != null) {
                                // filter the filename from content disposition and decode it...
                                name = new Regex(name, "filename.=UTF-8\'\'([^\"]+)").getMatch(0);
                                name = Encoding.UTF8Decode(name).replaceAll("%20", " ");
                                name = Encoding.htmlDecode(name);
                                if (name != null) {
                                    link.setFinalFileName(name);
                                }
                            }
                            return AvailableStatus.TRUE;
                        }
                    } finally {
                        try {
                            con.disconnect();
                        } catch (Throwable e) {
                        }
                    }
                }
                return AvailableStatus.UNCHECKABLE;
            }
        }
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        if (isUsenetLink(link)) {
            /* 2016-07-29: psp: Lowered this from 10 to 3 RE: admin */
            return 3;
        } else {
            /* Not sure about this value. */
            return 20;
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account != null) {
            synchronized (hostUnavailableMap) {
                final HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
                if (unavailableMap != null) {
                    final Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                    if (lastUnavailable != null) {
                        final long remainingTime = lastUnavailable - System.currentTimeMillis();
                        if (remainingTime > 0) {
                            throw new ConditionalSkipReasonException(new WaitingSkipReason(CAUSE.HOST_TEMP_UNAVAILABLE, remainingTime, null));
                        } else {
                            unavailableMap.remove(downloadLink.getHost());
                        }
                    }
                }
            }
            // some routine to check traffic allocations: normalTraffic specialTraffic
            // if (downloadLink.getHost().matches("uploaded\\.net|uploaded\\.to|ul\\.to|filemonkey\\.in|oboom\\.com")) {
            // We no longer sell Special traffic! Special traffic works only with our Usenet servers and for these 5 filehosts:
            // uploaded.net,share-online.biz, rapidgator.net, filer.net
            // special traffic
            if (downloadLink.getHost().matches("uploaded\\.net|uploaded\\.to|ul\\.to|share-online\\.biz|rapidgator\\.net|filer\\.net")) {
                if (account.getLongProperty(specialTraffic, 0) > 0) {
                    return true;
                }
            }
            // normal traffic, can include special traffic hosts also... (yes confusing)
            if (account.getLongProperty(normalTraffic, 0) > 0) {
                return true;
            }
        }
        return false;
    }

    private final boolean default_clear_download_history_storage = false;

    /*
     * TODO: There is no easy way to add this setting for their torrent links as well because users can e.g. download specified files inside
     * archives so we do not know if the user is currently downloading a complete single torrent or single files of it. To determine if
     * there is any way to do this we'd have to compare the finallinks of complete torrent downloads and files inside them first.
     */
    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CLEAR_DOWNLOAD_HISTORY_STORAGE, JDL.L("plugins.hoster.premiumto.clear_serverside_download_history_storage", "Delete storage.premium.to file(s) in your account after each successful download?")).setDefaultValue(default_clear_download_history_storage));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private int getConnections(String host) {
        if ("keep2share.cc".equals(host)) {
            return 1;
        } else if ("share-online.biz".equals(host)) {
            // re admin: only 1 possible
            return 1;
        } else {
            // default is up to 10 connections
            return -10;
        }
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("usenet2.premium.to", false, 119, 81));
        ret.addAll(UsenetServer.createServerList("usenet2.premium.to", true, 563, 444));
        return ret;
    }
}