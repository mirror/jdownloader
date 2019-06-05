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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetConfigPanel;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.config.AccountConfigInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.download.DownloadLinkDownloadable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premium.to" }, urls = { "https?://torrent[a-z0-9]*?\\.(premium\\.to|premium4\\.me)/(t|z)/[^<>/\"]+(/[^<>/\"]+){0,1}(/\\d+)*|https?://storage[a-z0-9]*?\\.(?:premium\\.to|premium4\\.me)/file/[A-Z0-9]+" })
public class PremiumTo extends UseNet {
    private final String                   noChunks                       = "noChunks";
    private static Object                  LOCK                           = new Object();
    private final String                   normalTraffic                  = "normalTraffic";
    private final String                   specialTraffic                 = "specialTraffic";
    private final String                   storageTraffic                 = "storageTraffic";
    private static final String            CLEAR_DOWNLOAD_HISTORY_STORAGE = "CLEAR_DOWNLOAD_HISTORY";
    private static final String            type_storage                   = "https?://storage.+";
    private static final String            type_torrent                   = "https?://torrent.+";
    private static final String            API_BASE                       = "http://api.premium.to/";
    private static final String            API_BASE_STORAGE               = "http://storage.premium.to/api";
    private static MultiHosterManagement   mhm                            = new MultiHosterManagement("premium.to");
    private static final ArrayList<String> hosts_regular                  = new ArrayList<String>();
    private static final ArrayList<String> hosts_storage                  = new ArrayList<String>();

    public PremiumTo(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2 * 1000L);
        this.enablePremium("http://premium.to/");
    }

    public static interface PremiumtoConfigInterface extends UsenetAccountConfigInterface {
        public class Translation {
            public String getClearDownloadHistory_label() {
                return "Delete storage.premium.to file(s) in your account after each successful download?";
            }
        }

        public static final PremiumtoConfigInterface.Translation TRANSLATION = new Translation();

        @DefaultBooleanValue(true)
        @Order(10)
        boolean isClearDownloadHistory();

        void setClearDownloadHistory(boolean b);
    };

    @Override
    protected PluginConfigPanelNG createConfigPanel() {
        return new UsenetConfigPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean showKeyHandler(KeyHandler<?> keyHandler) {
                return "cleardownloadhistory".equals(keyHandler.getKey());
            }

            @Override
            protected boolean useCustomUI(KeyHandler<?> keyHandler) {
                return !"cleardownloadhistory".equals(keyHandler.getKey());
            }

            @Override
            protected void initAccountConfig(PluginForHost plgh, Account acc, Class<? extends AccountConfigInterface> cf) {
                super.initAccountConfig(plgh, acc, cf);
                extend(this, getHost(), getAvailableUsenetServer(), getAccountJsonConfig(acc));
            }
        };
    }

    @Override
    public PremiumtoConfigInterface getAccountJsonConfig(Account acc) {
        return (PremiumtoConfigInterface) super.getAccountJsonConfig(acc);
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
            /* re admin: only 1 possible */
            return 1;
        } else {
            return super.getMaxSimultanDownload(link, account, proxy);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        login(account, true);
        final Browser tbr = br.cloneBrowser();
        tbr.setFollowRedirects(true);
        tbr.getPage("https://" + this.getHost() + "/sstraffic.php");
        /* NormalTraffic:SpecialTraffic:TorrentTraffic(StorageTraffic) */
        String[] traffic = tbr.toString().split(";");
        String additionalAccountStatus = "";
        if (traffic != null && traffic.length == 3) {
            /* Normal traffic */
            final long nT = Long.parseLong(traffic[0]);
            /* Special traffic */
            final long spT = Long.parseLong(traffic[1]);
            /* Storage traffic, 2019-04-17: According to admin, this type of traffic does not exist anymore(API will always return 0) */
            final long stT = Long.parseLong(traffic[2]);
            ac.setTrafficLeft(nT + spT + stT + "MiB");
            // set both so we can check in canHandle.
            account.setProperty(normalTraffic, nT + stT);
            account.setProperty(specialTraffic, spT);
            account.setProperty(storageTraffic, stT);
            if (nT > 0 && spT > 0) {
                additionalAccountStatus = String.format(" | Normal Traffic: %d MiB Special Traffic: %d MiB", nT, spT);
            }
        }
        final Browser hbr = br.cloneBrowser();
        hbr.setFollowRedirects(true);
        hbr.getPage(API_BASE + "hosts.php");
        final String hosters_regular[] = hbr.toString().toLowerCase().split(";|\\s+");
        final ArrayList<String> supported_hosts_regular = new ArrayList<String>(Arrays.asList(hosters_regular));
        supported_hosts_regular.add("usenet");
        hosts_regular.addAll(supported_hosts_regular);
        account.setType(AccountType.PREMIUM);
        ac.setStatus("Premium account" + additionalAccountStatus);
        String apikey = null;
        try {
            /* Do not fail here */
            apikey = getAndStoreAPIKey(account);
        } catch (final Throwable e) {
        }
        /* Find storage hosts and add them to array of supported hosts as well */
        hbr.getPage(API_BASE_STORAGE + "/hosts.php?apikey=" + apikey);
        final String hosters_storage[] = hbr.toString().toLowerCase().split(";|\\s+");
        final ArrayList<String> supported_hosts_storage = new ArrayList<String>(Arrays.asList(hosters_storage));
        for (final String supported_host_storage : supported_hosts_storage) {
            if (!supported_hosts_regular.contains(supported_host_storage)) {
                /*
                 * Make sure to add only "storage-only" hosts to storage Array as some hosts can be used via both ways - we prefer direct
                 * downloads!
                 */
                logger.info("Adding storage host: " + supported_host_storage);
                supported_hosts_regular.add(supported_host_storage);
                hosts_storage.add(supported_host_storage);
            }
        }
        if (supported_hosts_regular.size() > 0) {
            ac.setMultiHostSupport(this, supported_hosts_regular);
        }
        return ac;
    }

    /**
     * 2019-04-15: Required for downloading from STORAGE hosts. This apikey will always be the same until the user changes his password
     * (which he then also has to change in JDownloader)!
     */
    private String getAndStoreAPIKey(final Account account) throws Exception {
        String apikey = account.getStringProperty("apikey");
        if (apikey == null) {
            br.getPage(API_BASE + "api/getauthcode.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            /* 2019-04-15: apikey = username+hash */
            if (br.toString().length() > account.getUser().length()) {
                apikey = br.toString();
                account.setProperty("apikey", apikey);
                /* Cookie & header not required for all requests but for e.g. '/removeFile.php' */
                this.br.setCookie("premium.to", "auth", this.getAndStoreAPIKey(account));
                this.br.getHeaders().put("auth", this.getAndStoreAPIKey(account));
            } else {
                account.setProperty("apikey", Property.NULL);
            }
        }
        return apikey;
    }

    @Override
    public String getAGBLink() {
        return "https://premium.to/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
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
            final boolean freshLogin = login(account, false);
            String url = link.getDownloadURL();
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, url, true, -10);
            if (dl.getConnection().getResponseCode() == 403 && !freshLogin) {
                dl.getConnection().disconnect();
                login(account, true);
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, url, true, -10);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                /*
                 * This e.g. happens if the user deletes a file via the premium.to site and then tries to download the previously added link
                 * via JDownloader.
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            dl.startDownload();
        }
    }

    private boolean login(Account account, boolean force) throws Exception {
        final boolean redirect = br.isFollowingRedirects();
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                prepBrowser(br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    return false;
                }
                final Map<String, Object> login = new HashMap<String, Object>();
                login.put("u", account.getUser());
                login.put("p", account.getPass());
                login.put("r", Boolean.TRUE);
                final PostRequest post = new PostRequest(API_BASE + "login.php");
                post.setContentType("application/json");
                post.setPostDataString(JSonStorage.toString(login));
                br.getPage(post);
                if (br.getHttpConnection().getResponseCode() == 400) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.getCookie(this.br.getHost(), "auth", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            } finally {
                br.setFollowRedirects(redirect);
            }
        }
    }

    /** no override to keep plugin compatible to old stable */
    @SuppressWarnings("deprecation")
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else {
            mhm.runCheck(account, link);
            synchronized (hosts_storage) {
                if (hosts_storage.isEmpty()) {
                    logger.info("Storage-host list is empty: Performing full login to refresh it");
                    this.fetchAccountInfo(account);
                    if (hosts_storage.isEmpty()) {
                        logger.info("Storage-host list is still empty");
                    } else {
                        logger.info("Storage-host list is filled now");
                    }
                }
            }
            String serverside_filename = link.getStringProperty("serverside_filename", null);
            dl = null;
            String url = link.getDownloadURL().replaceFirst("https?://", "");
            /* this here is bullshit... multihoster side should do all the corrections. */
            /* TODO: Remove these workarounds */
            if (url.startsWith("http://")) {
                url = url.substring(7);
            }
            if (url.startsWith("www.")) {
                url = url.substring(4);
            }
            if (url.startsWith("depositfiles.com/")) {
                url = url.replaceFirst("depositfiles.com/", "df.com/");
            } else if (url.startsWith("filefactory.com/")) {
                url = url.replaceFirst("filefactory.com/", "ff.com/");
            }
            if (url.startsWith("oboom.com/")) {
                url = url.replaceFirst("oboom.com/#", "oboom.com/");
            }
            url = Encoding.urlEncode(url);
            int connections = getConnections(link.getHost());
            if (link.getChunks() != -1) {
                if (connections < 1) {
                    connections = link.getChunks();
                }
            }
            if (link.getBooleanProperty(noChunks, false)) {
                connections = 1;
            }
            String finalURL = null;
            /*
             * 2019-04-15: URLs of some hosts can only be downloaded via storage (= have to be fully downloaded top the servers of this
             * Multihost first and can then be downloaded by the user) while others can be used via normal download AND storage (e.g.
             * uploaded.net) - we prefer normal download and only use storage download if necessary.
             */
            final boolean requiresStorageDownload = hosts_storage != null && hosts_storage.contains(link.getHost());
            if (requiresStorageDownload) {
                /* Storage download */
                final String apikey = getAndStoreAPIKey(account);
                /* Check if that URL has already been downloaded to their cloud. */
                br.getPage(API_BASE_STORAGE + "/check.php?apikey=" + apikey + "&url=" + url);
                handleErrorsStorageAPI();
                final String status = getStorageAPIStatus();
                if ("Not in queue".equalsIgnoreCase(status)) {
                    /* Not on their servers? Add to download-queue! */
                    br.getPage(API_BASE_STORAGE + "/add.php?apikey=" + apikey + "&url=" + url);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Added URL to Storage: Storage download pending", 5 * 60 * 1000);
                } else if ("completed".equalsIgnoreCase(status)) {
                    /* File has been downloaded to their servers and download should be possible now. */
                    finalURL = API_BASE_STORAGE + "/download.php?apikey=" + apikey + "&url=" + url;
                } else {
                    /* WTF */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown status");
                }
                /* We might need this later. */
                serverside_filename = PluginJSonUtils.getJson(br, "Filename");
            } else {
                /* Normal (direct) download */
                login(account, false);
                finalURL = API_BASE + "getfile.php?link=" + url;
            }
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
            final DownloadLinkDownloadable downloadable = new DownloadLinkDownloadable(link) {
                @Override
                public boolean isHashCheckEnabled() {
                    return false;
                }
            };
            if (!StringUtils.isEmpty(serverside_filename)) {
                /* We might need this information later */
                link.setProperty("serverside_filename", serverside_filename);
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadable, br.createGetRequest(finalURL), true, connections);
            if (dl.getConnection().getResponseCode() == 404) {
                /* file offline */
                dl.getConnection().disconnect();
                mhm.handleErrorGeneric(account, link, "server_error_404", 2, 5 * 60 * 1000l);
            }
            if (!dl.getConnection().isContentDisposition()) {
                if (dl.getConnection().getResponseCode() == 420) {
                    dl.close();
                    mhm.handleErrorGeneric(account, link, "server_error_420", 2, 5 * 60 * 1000l);
                }
                br.followConnection();
                logger.severe("PremiumTo Error");
                if (br.toString().matches("File not found")) {
                    // we can not trust multi-hoster file not found returns, they could be wrong!
                    // jiaz new handling to dump to next download candidate.
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                } else if (br.toString().matches("File hosting service not supported")) {
                    mhm.putError(account, link, 10 * 60 * 1000l, "hoster_unsupported");
                } else if ("Not enough traffic".equals(br.toString())) {
                    /*
                     * With our special traffic it's a bit complicated. When you still have a little Special Traffic but you have enough
                     * standard traffic, it will show you "Not enough traffic" for the filehost Uploaded.net for example.
                     */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
                /*
                 * after x retries we disable this host and retry with normal plugin
                 */
                mhm.handleErrorGeneric(account, link, "unknown_dl_error", 2, 5 * 60 * 1000l);
            }
            try {
                /* Check if the download is successful && user wants JD to delete the file in his premium.to account afterwards. */
                final PremiumtoConfigInterface config = getAccountJsonConfig(account);
                if (dl.startDownload() && config.isClearDownloadHistory()) {
                    String storageID = null;
                    if (link.getDownloadURL().matches(type_storage)) {
                        storageID = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
                    } else if (serverside_filename != null) {
                        /*
                         * 2019-06-05: This is a workaround!! Their API should better return the storageID via 'download.php' (see upper
                         * code).
                         */
                        logger.info("Trying to find storageID");
                        try {
                            /* Make sure we're logged-IN via apikey! */
                            this.getAndStoreAPIKey(account);
                            br.getPage("https://storage.premium.to/status.php");
                            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                            final ArrayList<Object> storage_objects = (ArrayList<Object>) entries.get("f");
                            for (final Object fileO : storage_objects) {
                                entries = (LinkedHashMap<String, Object>) fileO;
                                final String serverside_filename_tmp = (String) entries.get("n");
                                if (StringUtils.equals(serverside_filename_tmp, serverside_filename)) {
                                    storageID = (String) entries.get("i");
                                    break;
                                }
                            }
                        } catch (final Throwable e) {
                        }
                        if (StringUtils.isEmpty(storageID)) {
                            logger.warning("Failed to find storageID");
                        } else {
                            logger.info("Successfully found storageID");
                        }
                    }
                    boolean success = false;
                    try {
                        if (!StringUtils.isEmpty(storageID)) {
                            logger.info("Trying to delete file from storage");
                            br.getPage("https://storage." + this.getHost() + "/removeFile.php?f=" + storageID);
                            /*
                             * TODO: Check if there is a way to determine for sure whether the deletion was successful or not.
                             */
                            if (br.getHttpConnection().getResponseCode() == 200) {
                                success = true;
                            }
                        }
                    } catch (final Throwable e) {
                        /* Don't fail here */
                        logger.warning("Failed to delete file from storage");
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

    private void handleErrorsStorageAPI() throws Exception {
        if (br.toString().equalsIgnoreCase("Invalid API key")) {
            /* Temp disable account --> Next accountcheck will refresh apikey */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else {
            final int responsecode = br.getHttpConnection().getResponseCode();
            switch (responsecode) {
            case 200:
                /* Everything ok */
                break;
            case 401:
                /* Invalid apikey (same as above) */
                /* Temp disable account --> Next accountcheck will refresh apikey */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 403:
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Not enough traffic left", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 405:
                /* User has reached max. storage files limit (2019-04-15: 200 files) */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Storage max files limit reached", 5 * 60 * 1000);
            }
            final String status = getStorageAPIStatus();
            if (StringUtils.isEmpty(status)) {
                return;
            }
            if (status.equalsIgnoreCase("In queue")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Storage download pending", 5 * 60 * 1000);
            }
        }
    }

    private String getStorageAPIStatus() {
        return PluginJSonUtils.getJson(br, "Status");
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
                /* try without login (only possible for URLs with token) */
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
                        con = br.openHeadConnection(dlink);
                        if (con.getResponseCode() == 403) {
                            /* Either invalid URL or user deleted file from Storage/Cloud --> URL is invalid now. */
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        if (!con.getContentType().contains("html")) {
                            fileSize = con.getLongContentLength();
                            if (fileSize <= 0) {
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                            link.setFinalFileName(getFileNameFromHeader(con));
                            link.setDownloadSize(fileSize);
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
            // some routine to check traffic allocations: normalTraffic specialTraffic
            // if (downloadLink.getHost().matches("uploaded\\.net|uploaded\\.to|ul\\.to|filemonkey\\.in|oboom\\.com")) {
            // We no longer sell Special traffic! Special traffic works only with our Usenet servers and for these 5 filehosts:
            // uploaded.net,share-online.biz, rapidgator.net, filer.net
            // special traffic
            if (downloadLink.getHost().matches("uploaded\\.net|uploaded\\.to|ul\\.to|share\\-online\\.biz|rapidgator\\.net|filer\\.net")) {
                if (account.getLongProperty(specialTraffic, 0) > 0) {
                    return true;
                }
            }
            /* normal traffic, can include special traffic hosts also... (yes confusing) */
            if (account.getLongProperty(normalTraffic, 0) > 0) {
                return true;
            }
        }
        return false;
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
        ret.addAll(UsenetServer.createServerList("usenet.premium.to", false, 119, 81));
        ret.addAll(UsenetServer.createServerList("usenet.premium.to", true, 444, 563));
        ret.addAll(UsenetServer.createServerList("usenet2.premium.to", false, 119, 81));
        ret.addAll(UsenetServer.createServerList("usenet2.premium.to", true, 444, 563));
        return ret;
    }
}