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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

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
import jd.http.Browser;
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
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.download.DownloadLinkDownloadable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premium.to" }, urls = { "https?://torrent[a-z0-9]*?\\.(premium\\.to|premium4\\.me)/(t|z)/[^<>/\"]+(/[^<>/\"]+){0,1}(/\\d+)*|https?://storage[a-z0-9]*?\\.(?:premium\\.to|premium4\\.me)/file/[A-Z0-9]+" })
public class PremiumTo extends UseNet {
    private final String                   normalTraffic             = "normalTraffic";
    private final String                   specialTraffic            = "specialTraffic";
    private final String                   storageTraffic            = "storageTraffic";
    private static final String            type_storage              = "https?://storage.+";
    // private static final String type_torrent = "https?://torrent.+";
    private static final String            API_BASE                  = "http://api.premium.to/api/2";
    private static final String            API_BASE_STORAGE          = "https://storage.premium.to/api";
    /* 2019-10-22: Disabled upon admin request. Storage hosts will not be displayed as supported host either when this is disabled! */
    private static final boolean           supports_storage_download = false;
    private static MultiHosterManagement   mhm                       = new MultiHosterManagement("premium.to");
    private static final ArrayList<String> hosts_regular             = new ArrayList<String>();
    private static final ArrayList<String> hosts_storage             = new ArrayList<String>();

    public PremiumTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://premium.to/");
    }

    public static interface PremiumDotToConfigInterface extends UsenetAccountConfigInterface {
        public class Translation {
            public String getClearDownloadHistory_label() {
                return "Delete storage.premium.to file(s) in your account after each successful download?";
            }
        }

        public static final PremiumDotToConfigInterface.Translation TRANSLATION = new Translation();

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
    public PremiumDotToConfigInterface getAccountJsonConfig(Account acc) {
        return (PremiumDotToConfigInterface) super.getAccountJsonConfig(acc);
    }

    private Browser prepBrowser(Browser prepBr) {
        prepBr.setFollowRedirects(true);
        prepBr.setAcceptLanguage("en, en-gb;q=0.8");
        prepBr.setConnectTimeout(90 * 1000);
        prepBr.setReadTimeout(90 * 1000);
        prepBr.getHeaders().put("User-Agent", "JDownloader");
        return prepBr;
    }

    private boolean login(Account account, boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBrowser(br);
                String apikey = this.getAPIKey(account);
                String userid = this.getUserID(account);
                boolean attemptedAPIKeyLogin = false;
                if (!StringUtils.isEmpty(apikey) && !StringUtils.isEmpty(userid)) {
                    if (!force) {
                        /* Trust existing data without check */
                        return false;
                    }
                    boolean username_and_pw_is_userid_and_apikey = false;
                    try {
                        username_and_pw_is_userid_and_apikey = account.getUser().equals(userid) && account.getPass().equals(apikey);
                    } catch (final Throwable e) {
                    }
                    br.getPage(API_BASE + "/traffic.php?userid=" + userid + "&apikey=" + apikey);
                    try {
                        this.handleErrorsAPI(account);
                        return true;
                    } catch (final Throwable e) {
                        /* E.g. API logindata has changed */
                        if (username_and_pw_is_userid_and_apikey) {
                            /* No need to try anything. User initially entered userid + apikey as username & password --> */
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                        e.printStackTrace();
                        logger.info("Login via apikey failed, trying via username + password");
                        attemptedAPIKeyLogin = true;
                    }
                }
                /* First try old way ia username & password */
                final boolean login_via_username_and_password_possible = true;
                final boolean logindata_looks_like_api_logindata = new Regex(account.getUser(), Pattern.compile("[a-z0-9]+", Pattern.CASE_INSENSITIVE)).matches() && new Regex(account.getPass(), Pattern.compile("[a-z0-9]{32}", Pattern.CASE_INSENSITIVE)).matches();
                if (!login_via_username_and_password_possible && !logindata_looks_like_api_logindata) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Enter API userid as username and API key as password, see premium.to website --> Account tab", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                logger.info("Logging in with username + password");
                br.getPage(API_BASE + "/getapicredentials.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                apikey = PluginJSonUtils.getJson(br, "apikey");
                userid = PluginJSonUtils.getJson(br, "userid");
                /* Failed? Hmm user might have entered 'new' API logindata, see premium.to homepage --> Account */
                if ((StringUtils.isEmpty(apikey) || StringUtils.isEmpty(userid)) && logindata_looks_like_api_logindata && !attemptedAPIKeyLogin) {
                    logger.info("User might have entered new API username (userid) and api password (apikey)");
                    br.getPage(API_BASE + "/traffic.php?userid=" + account.getUser() + "&apikey=" + account.getPass());
                    userid = account.getUser();
                    apikey = account.getPass();
                }
                this.handleErrorsAPI(account);
                if (StringUtils.isEmpty(apikey) || StringUtils.isEmpty(userid)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* Save API logindata */
                account.setProperty("apikey", apikey);
                account.setProperty("userid", userid);
                /* TODO: Dump originally stored username + password to protect them in case e.g. account database gets stolen! */
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        login(account, true);
        final String apikey = this.getAPIKey(account);
        final String userid = this.getUserID(account);
        if (br.getURL() == null || !br.getURL().contains("traffic.php")) {
            br.getPage(API_BASE + "/traffic.php?userid=" + userid + "&apikey=" + apikey);
            this.handleErrorsAPI(account);
        }
        /* NormalTraffic:SpecialTraffic:TorrentTraffic(StorageTraffic) */
        String additionalAccountStatus = "";
        /* Normal traffic */
        final long nT = Long.parseLong(PluginJSonUtils.getJson(br, "traffic"));
        /* Special traffic */
        final long spT = Long.parseLong(PluginJSonUtils.getJson(br, "specialtraffic"));
        /* Storage traffic, 2019-04-17: According to admin, this type of traffic does not exist anymore(API will always return 0) */
        final long stT = 0;
        ac.setTrafficLeft(nT + spT + stT);
        // set both so we can check in canHandle.
        account.setProperty(normalTraffic, nT + stT);
        account.setProperty(specialTraffic, spT);
        account.setProperty(storageTraffic, stT);
        if (nT > 0 && spT > 0) {
            additionalAccountStatus = String.format(" | Normal Traffic: %d MiB Special Traffic: %d MiB", nT, spT);
        }
        final ArrayList<String> supported_hosts_regular = new ArrayList<String>();
        try {
            br.getPage(API_BASE + "/hosts.php?userid=" + userid + "&apikey=" + apikey);
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("hosts");
            for (final Object hostO : ressourcelist) {
                if (hostO instanceof String) {
                    supported_hosts_regular.add((String) hostO);
                }
            }
        } catch (final Throwable e) {
        }
        supported_hosts_regular.add("usenet");
        hosts_regular.addAll(supported_hosts_regular);
        account.setType(AccountType.PREMIUM);
        ac.setStatus("Premium account" + additionalAccountStatus);
        /* Find storage hosts and add them to array of supported hosts as well */
        br.getPage(API_BASE_STORAGE + "/hosts.php?apikey=" + apikey);
        final String hosters_storage[] = br.toString().toLowerCase().split(";|\\s+");
        final ArrayList<String> supported_hosts_storage = new ArrayList<String>(Arrays.asList(hosters_storage));
        for (final String supported_host_storage : supported_hosts_storage) {
            if (!supported_hosts_regular.contains(supported_host_storage)) {
                /*
                 * Make sure to add only "storage-only" hosts to storage Array as some hosts can be used via both ways - we prefer direct
                 * downloads!
                 */
                if (supports_storage_download) {
                    logger.info("Adding storage host: " + supported_host_storage);
                    supported_hosts_regular.add(supported_host_storage);
                    hosts_storage.add(supported_host_storage);
                } else {
                    logger.info("Skipping storage host: " + supported_host_storage);
                }
            }
        }
        ac.setMultiHostSupport(this, supported_hosts_regular);
        return ac;
    }

    /**
     * 2019-04-15: Required for downloading from STORAGE hosts. This apikey will always be the same until the user changes his password
     * (which he then also has to change in JDownloader)! </br>
     * 2019-10-22: @Deprecated since the existance of APIv2: https://premium.to/API.html
     */
    @Deprecated
    private String findAndStoreAPIKey(final Account account) throws Exception {
        String apikey = getAPIKey(account);
        if (apikey == null) {
            br.getPage(API_BASE_STORAGE + "/getauthcode.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            /* 2019-04-15: apikey = username+hash */
            if (br.toString().length() > account.getUser().length()) {
                apikey = br.toString();
                account.setProperty("apikey", apikey);
                /* Cookie & header not required for all requests but for e.g. '/removeFile.php' */
                this.br.setCookie("premium.to", "auth", this.findAndStoreAPIKey(account));
                this.br.getHeaders().put("auth", this.findAndStoreAPIKey(account));
            } else {
                account.setProperty("apikey", Property.NULL);
            }
        }
        return apikey;
    }

    private String getAPIKey(final Account account) {
        return account.getStringProperty("apikey", null);
    }

    private String getUserID(final Account account) {
        return account.getStringProperty("userid", null);
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

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else {
            final String url = link.getPluginPatternMatcher();
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, url, true, -10);
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
            String apikey = this.getAPIKey(account);
            final String userid = this.getUserID(account);
            final String url = Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
            final int maxConnections = -10;
            String finalURL = null;
            /*
             * 2019-04-15: URLs of some hosts can only be downloaded via storage (= have to be fully downloaded top the servers of this
             * Multihost first and can then be downloaded by the user) while others can be used via normal download AND storage (e.g.
             * uploaded.net) - we prefer normal download and only use storage download if necessary.
             */
            final boolean requiresStorageDownload = hosts_storage != null && hosts_storage.contains(link.getHost());
            if (requiresStorageDownload) {
                /* Storage download */
                if (!supports_storage_download) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Storage download is not yet supported via API");
                }
                if (apikey == null) {
                    apikey = findAndStoreAPIKey(account);
                }
                /* Check if that URL has already been downloaded to their cloud. */
                br.getPage(API_BASE_STORAGE + "/check.php?apikey=" + apikey + "&url=" + url);
                handleErrorsAPI(account);
                final String status = getStorageAPIStatus();
                if ("Not in queue".equalsIgnoreCase(status)) {
                    /* Not on their servers? Add to download-queue! */
                    br.getPage(API_BASE_STORAGE + "/add.php?apikey=" + apikey + "&url=" + url);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Added URL to premium.to Storage: Storage download pending", 5 * 60 * 1000);
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
                finalURL = API_BASE + "/getfile.php?link=" + url + "&userid=" + userid + "&apikey=" + apikey;
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
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadable, br.createGetRequest(finalURL), true, maxConnections);
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
            /* Check if the download is successful && user wants JD to delete the file in his premium.to account afterwards. */
            final PremiumDotToConfigInterface config = getAccountJsonConfig(account);
            if (dl.startDownload() && config.isClearDownloadHistory()) {
                String storageID = null;
                if (link.getDownloadURL().matches(type_storage)) {
                    storageID = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
                } else if (serverside_filename != null) {
                    /*
                     * 2019-06-05: This is a workaround!! Their API should better return the storageID via 'download.php' (see upper code).
                     */
                    logger.info("Trying to find storageID");
                    try {
                        /* Make sure we're logged-IN via apikey! */
                        this.findAndStoreAPIKey(account);
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
        }
    }

    private void handleErrorsAPI(final Account account) throws Exception {
        int responsecode = 0;
        final String responsecodeStr = PluginJSonUtils.getJson(br, "code");
        if (!StringUtils.isEmpty(responsecodeStr) && responsecodeStr.matches("\\d+")) {
            responsecode = Integer.parseInt(responsecodeStr);
        }
        switch (responsecode) {
        case 0:
            /* No error */
            break;
        case 200:
            /* Everything ok */
            break;
        case 400:
            /* Invalid parameter - this should never happen! */
            throw new PluginException(LinkStatus.ERROR_FATAL, "API response 400");
        case 401:
            /* Invalid apikey --> Invalid logindata */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        case 402:
            /* TODO: Unsupported filehost */
            mhm.handleErrorGeneric(account, this.getDownloadLink(), "unsupported_filehost", 20, 5 * 60 * 1000);
        case 403:
            /* Not enough traffic left */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Not enough traffic left", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        case 404:
            /* File not found TODO: Check whether we can trust this or not! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 405:
            /* User has reached max. storage files limit (2019-04-15: Current limit: 200 files) */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Storage max files limit reached", 5 * 60 * 1000);
        case 500:
            /* {"code":500,"message":"Currently no available premium acccount for this filehost"} */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Storage max files limit reached", 5 * 60 * 1000);
        default:
            /* TODO: Unknown error */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Unknown API error", 5 * 60 * 1000);
        }
        if (br.getURL() != null && br.getURL().contains("storage.premium.to")) {
            /* Now handle special Storage errors / statuscodes */
            if ("Invalid API Key".equalsIgnoreCase(br.toString())) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Invalid Storage apikey", 5 * 60 * 1000);
            }
            final String status = getStorageAPIStatus();
            if (StringUtils.isEmpty(status)) {
                /* No errors */
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

    @Override
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (isUsenetLink(link)) {
            return super.requestFileInformation(link);
        } else {
            final String dlink = Encoding.urlDecode(link.getPluginPatternMatcher(), true);
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            long fileSize = -1;
            ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(this.getHost());
            if (accs == null || accs.size() == 0) {
                if (link.getPluginPatternMatcher().matches(type_storage)) {
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
            return true;
        }
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
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