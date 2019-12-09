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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premium.to" }, urls = { "https?://torrent(?:\\d+)?\\.premium\\.to/(?:t/[a-z0-9]+/\\d+|z/[a-z0-9]+|r/\\d+/[A-F0-9]{32}/[a-z0-9]+/\\d+/[^/]+)|https?://storage\\.premium\\.to/(?:file/[A-Z0-9]+|remote/[A-Z0-9]+/[A-Z0-9]+/[A-Z0-9]+/[^/]+)" })
public class PremiumTo extends UseNet {
    private final String                   normalTraffic                   = "normalTraffic";
    private final String                   specialTraffic                  = "specialTraffic";
    private static final String            type_torrent                    = "https?://torrent.*?\\..+";
    private static final String            type_torrent_file               = "https?://torrent.*?\\.[^/]+/(?:t|z)/(.+)";
    private static final String            type_torrent_remote             = "https?://torrent.*?\\.[^/]+/r/\\d+/[A-F0-9]{32}/([a-z0-9]+/\\d+)/[^/]+";
    private static final String            type_storage                    = "https?://storage\\..+";
    /* storage.premium.to --> Extract download URLs */
    private static final String            type_storage_file               = "https?://storage\\.[^/]+/file/(.+)";
    /* storage.premium.to --> Extract remote URLs */
    private static final String            type_storage_remote             = "https?://storage\\.[^/]+/(?:remote|r)/[A-Z0-9]+/[A-Z0-9]+/([A-Z0-9]+)/.+";
    // private static final String type_torrent = "https?://torrent.+";
    /* 2019-10-23: According to admin, missing https support for API is not an issue */
    private static final String            API_BASE                        = "http://api.premium.to/api/2";
    private static final String            API_BASE_STORAGE                = "https://storage.premium.to/api/2";
    private static final String            API_BASE_TORRENT                = "https://torrent.premium.to/api/2";
    /*
     * 2019-11-10: Internal switch to force disable all Storage hosts - do not touch this unless e.g. admin requests this or API breaks
     * down.
     */
    private static final boolean           debug_supports_storage_download = true;
    private static MultiHosterManagement   mhm                             = new MultiHosterManagement("premium.to");
    private static final ArrayList<String> supported_hosts_storage         = new ArrayList<String>();

    public PremiumTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://premium.to/");
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private boolean requiresAccount(final DownloadLink link) {
        if (link != null && link.getPluginPatternMatcher() != null && (link.getPluginPatternMatcher().matches(type_torrent_remote) || link.getPluginPatternMatcher().matches(type_storage_remote))) {
            return false;
        } else {
            return true;
        }
    }

    private String getFID(final DownloadLink link) {
        if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches(type_storage)) {
            if (link.getPluginPatternMatcher().matches(type_storage_file)) {
                return new Regex(link.getPluginPatternMatcher(), type_storage_file).getMatch(0);
            } else {
                return new Regex(link.getPluginPatternMatcher(), type_storage_remote).getMatch(0);
            }
        } else if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches(type_torrent)) {
            if (link.getPluginPatternMatcher().matches(type_torrent_file)) {
                return new Regex(link.getPluginPatternMatcher(), type_torrent_file).getMatch(0);
            } else {
                return new Regex(link.getPluginPatternMatcher(), type_torrent_remote).getMatch(0);
            }
        } else {
            return null;
        }
    }

    public static interface PremiumDotToConfigInterface extends UsenetAccountConfigInterface {
        public class Translation {
            public String getClearDownloadHistory_label() {
                return "Delete storage.premium.to file(s) in your account after each successful download?";
            }

            public String getEnableStorageWhiteListing_label() {
                return "Enable storage.premium.to whitelisting (comma separated)? This will only allow JD to automatically add links to Storage for the services listed below. Leave the list empty to disable all storage hosts.";
            }

            public String getWhitelistedStorageHosts_label() {
                return "Enter comma seprarated whitelist of supported storage.premium.to hosts.";
            }
        }

        public static final PremiumDotToConfigInterface.Translation TRANSLATION = new Translation();

        @DefaultBooleanValue(true)
        @Order(10)
        boolean isClearDownloadHistory();

        void setClearDownloadHistory(boolean b);

        @DefaultBooleanValue(false)
        @Order(20)
        boolean isEnableStorageWhiteListing();

        void setEnableStorageWhiteListing(boolean b);

        @AboutConfig
        @DefaultStringValue("examplehost1.com,examplehost2.net")
        String getWhitelistedStorageHosts();

        void setWhitelistedStorageHosts(String whitelist);
    };

    @Override
    protected PluginConfigPanelNG createConfigPanel() {
        return new UsenetConfigPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean showKeyHandler(KeyHandler<?> keyHandler) {
                return "cleardownloadhistory".equals(keyHandler.getKey()) || "enablestoragewhitelisting".equals(keyHandler.getKey()) || "whitelistedstoragehosts".equals(keyHandler.getKey());
            }

            @Override
            protected boolean useCustomUI(KeyHandler<?> keyHandler) {
                return !"cleardownloadhistory".equals(keyHandler.getKey()) && !"enablestoragewhitelisting".equals(keyHandler.getKey()) && !"whitelistedstoragehosts".equals(keyHandler.getKey());
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

    private int getReadTimeout() {
        return 90 * 1000;
    }

    private int getConnectTimeout() {
        return 90 * 1000;
    }

    private Browser prepBrowser(Browser prepBr) {
        prepBr.setFollowRedirects(true);
        prepBr.setAcceptLanguage("en, en-gb;q=0.8");
        prepBr.setConnectTimeout(getReadTimeout());
        prepBr.setReadTimeout(getConnectTimeout());
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
                        this.handleErrorsAPI(account, false);
                        return true;
                    } catch (final Exception e) {
                        /* E.g. API logindata has changed */
                        if (username_and_pw_is_userid_and_apikey) {
                            /* No need to try anything. User initially entered userid + apikey as username & password --> */
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, null, PluginException.VALUE_ID_PREMIUM_DISABLE, e);
                        }
                        logger.log(e);
                        logger.info("Login via apikey failed, trying via username + password");
                        attemptedAPIKeyLogin = true;
                    }
                }
                /* First try old way via username & password */
                /*
                 * TODO: Set this to false once account 'conversion' is done (= login is only possible via userid & apikey;
                 * getapicredentials.php will not work anymore then!)
                 */
                final boolean login_via_username_and_password_possible = true;
                final boolean logindata_looks_like_api_logindata = new Regex(account.getUser(), Pattern.compile("[a-z0-9]+", Pattern.CASE_INSENSITIVE)).matches() && new Regex(account.getPass(), Pattern.compile("[a-z0-9]{32}", Pattern.CASE_INSENSITIVE)).matches();
                if (!login_via_username_and_password_possible && !logindata_looks_like_api_logindata) {
                    if (System.getProperty("user.language").equals("de")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Falscher Benutzername/Passwort!\r\nBitte verwende die extra für JDownloader zur Verfügung gestellten API Zugangsdaten.\r\nDiese findest du hier:\r\npremium.to Webseite --> Account Tab\r\n--> 'JDownloader username / API userid' UND 'JDownloader password / API key'", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Wrong username/password!\r\nBe sure to use the special JDownloader logindata provided under:\r\npremium.to website --> Account tab\r\n--> 'JDownloader username / API userid' AND 'JDownloader password / API key'", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                logger.info("Logging in with username + password");
                br.getPage(API_BASE + "/getapicredentials.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                apikey = PluginJSonUtils.getJson(br, PROPERTY_APIKEY);
                userid = PluginJSonUtils.getJson(br, PROPERTY_USERID);
                /* Failed? Hmm user might have entered 'new' API logindata, see premium.to homepage --> Account */
                if ((StringUtils.isEmpty(apikey) || StringUtils.isEmpty(userid)) && logindata_looks_like_api_logindata && !attemptedAPIKeyLogin) {
                    logger.info("User might have entered new API username (userid) and api password (apikey)");
                    br.getPage(API_BASE + "/traffic.php?userid=" + account.getUser() + "&apikey=" + account.getPass());
                    userid = account.getUser();
                    apikey = account.getPass();
                }
                if (StringUtils.isEmpty(apikey) || StringUtils.isEmpty(userid)) {
                    if (System.getProperty("user.language").equals("de")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Falscher Benutzername/Passwort!\r\nBitte verwende die extra für JDownloader zur Verfügung gestellten API Zugangsdaten.\r\nDiese findest du hier:\r\npremium.to Webseite --> Account Tab\r\n--> 'JDownloader username / API userid' UND 'JDownloader password / API key'", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Wrong username/password!\r\nBe sure to use the special JDownloader logindata provided under:\r\npremium.to website --> Account tab\r\n--> 'JDownloader username / API userid' AND 'JDownloader password / API key'", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Save API logindata */
                account.setProperty(PROPERTY_APIKEY, apikey);
                account.setProperty(PROPERTY_USERID, userid);
                /* TODO: Dump originally stored username + password to protect them in case e.g. account database gets stolen! */
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    /* 2019-10-23: There are no cookies given via API anymore */
                    // account.clearCookies("");
                    account.removeProperty(PROPERTY_APIKEY);
                    account.removeProperty(PROPERTY_USERID);
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
            this.handleErrorsAPI(account, false);
        }
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
        if (nT > 0 && spT > 0) {
            additionalAccountStatus = String.format(" | Normal Traffic: %d MiB Special Traffic: %d MiB", nT, spT);
        }
        final ArrayList<String> supported_hosts_regular = new ArrayList<String>();
        ArrayList<String> supported_hosts_storage = new ArrayList<String>();
        try {
            // br.setConnectTimeout(5 * 1000);
            // br.setReadTimeout(5 * 1000);
            br.getPage(API_BASE + "/hosts.php?userid=" + userid + "&apikey=" + apikey);
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("hosts");
            for (final Object hostO : ressourcelist) {
                if (hostO instanceof String) {
                    supported_hosts_regular.add((String) hostO);
                }
            }
        } catch (final Throwable e) {
        } finally {
            // br.setConnectTimeout(getReadTimeout());
            // br.setReadTimeout(getConnectTimeout());
        }
        supported_hosts_regular.add("usenet");
        supported_hosts_regular.addAll(supported_hosts_regular);
        account.setType(AccountType.PREMIUM);
        /* Find storage hosts and add them to array of supported hosts */
        try {
            /*
             * 2019-12-05: They're having server issues with the server that handles this request --> Catch errors and in the worst case,
             * continue without adding any Storage hosts!
             */
            br.getPage(API_BASE_STORAGE + "/hosts.php?userid=" + userid + "&apikey=" + apikey);
            /* We expect a comma separated array */
            final String tmp_supported_hosts_storage[] = br.toString().toLowerCase().split(";|\\s+");
            for (final String tmp_supported_host_storage : tmp_supported_hosts_storage) {
                if (!supported_hosts_regular.contains(tmp_supported_host_storage)) {
                    /*
                     * Make sure to add only "storage-only" hosts to storage Array as some hosts can be used via both ways - we prefer
                     * direct downloads!
                     */
                    if (debug_supports_storage_download) {
                        logger.info("Found Storage host: " + tmp_supported_host_storage);
                        supported_hosts_storage.add(tmp_supported_host_storage);
                    } else {
                        logger.info("Storage functionality disabled: Skipping Storage host: " + tmp_supported_host_storage);
                    }
                }
            }
        } catch (final Throwable e) {
            logger.info("Failed to find Storage hosts");
        }
        /*
         * Now we've found all supported hosts - let's get the REAL list of supported hosts via a workaround (important for user-settings
         * below).
         */
        List<String> real_supported_hosts_regular = null;
        List<String> real_supported_hosts_storage = null;
        List<String> user_whitelisted_hosts_storage = new ArrayList<String>();
        List<String> real_user_whitelisted_hosts_storage = null;
        List<String> final_real_user_whitelisted_hosts_storage = new ArrayList<String>();
        ac.setMultiHostSupport(this, supported_hosts_regular);
        real_supported_hosts_regular = ac.getMultiHostSupport();
        ac.setMultiHostSupport(this, supported_hosts_storage);
        real_supported_hosts_storage = ac.getMultiHostSupport();
        {
            /* Handling for Storage hosts */
            final PremiumDotToConfigInterface config = getAccountJsonConfig(account);
            final boolean onlyAllowWhitelistedStorageHosts = config.isEnableStorageWhiteListing();
            if (onlyAllowWhitelistedStorageHosts) {
                logger.info("User enabled whitelisting of Storage hosts");
                final String whitelistedStorageHostsCommaSeparated = config.getWhitelistedStorageHosts();
                if (!StringUtils.isEmpty(whitelistedStorageHostsCommaSeparated)) {
                    final String[] whitelistedHosts = whitelistedStorageHostsCommaSeparated.split(",");
                    for (final String whitelistedHost : whitelistedHosts) {
                        user_whitelisted_hosts_storage.add(whitelistedHost);
                    }
                    ac.setMultiHostSupport(this, user_whitelisted_hosts_storage);
                    real_user_whitelisted_hosts_storage = ac.getMultiHostSupport();
                }
                /*
                 * Only allow verified entries e.g. user enters "examplehost4.com" but real_supported_hosts_storage does not even contain
                 * this --> Ignore that. Don't let the user add random hosts which the multihost does not even support!
                 */
                if (real_user_whitelisted_hosts_storage != null) {
                    for (final String real_user_whitelisted_storage_host : real_user_whitelisted_hosts_storage) {
                        if (real_supported_hosts_storage != null && real_supported_hosts_storage.contains(real_user_whitelisted_storage_host)) {
                            final_real_user_whitelisted_hosts_storage.add(real_user_whitelisted_storage_host);
                        }
                    }
                }
                /* Clear list of Storage hosts to fill it again with whitelisted entries of user */
                if (real_supported_hosts_storage != null) {
                    real_supported_hosts_storage.clear();
                }
                if (final_real_user_whitelisted_hosts_storage.isEmpty()) {
                    logger.info("User whitelisted nothing or entered invalid values (e.g. non-Storage hosts) --> Adding no Storage hosts at all");
                    additionalAccountStatus += " | Whitelisted Storage hosts: None [All disabled]";
                } else {
                    logger.info("User whitelisted the following Storage hosts:");
                    additionalAccountStatus += " | Whitelisted Storage hosts: ";
                    int counter = 0;
                    for (final String final_real_user_whitelisted_storage_host : final_real_user_whitelisted_hosts_storage) {
                        logger.info("WhitelistedStorageHost: " + final_real_user_whitelisted_storage_host);
                        real_supported_hosts_storage.add(final_real_user_whitelisted_storage_host);
                        additionalAccountStatus += final_real_user_whitelisted_storage_host;
                        if (counter < final_real_user_whitelisted_hosts_storage.size() - 1) {
                            additionalAccountStatus += ", ";
                        }
                        counter++;
                    }
                }
            } else {
                logger.info("User disabled whitelisting of Storage hosts (= add all Storage hosts to list)");
            }
            /* Finally, add Storage hosts to regular host array to be able to use them and display the list of supported hosts. */
            if (real_supported_hosts_storage == null || real_supported_hosts_storage.isEmpty()) {
                logger.info("Storage host array is empty");
            } else {
                for (final String real_supported_host_storage : real_supported_hosts_storage) {
                    logger.info("Adding final active Storage host: " + real_supported_host_storage);
                    real_supported_hosts_regular.add(real_supported_host_storage);
                    /* Add host to special Array of Storage hosts */
                    PremiumTo.supported_hosts_storage.add(real_supported_host_storage);
                }
            }
        }
        ac.setMultiHostSupport(this, real_supported_hosts_regular);
        ac.setStatus("Premium account" + additionalAccountStatus);
        return ac;
    }

    private final String PROPERTY_APIKEY = "apikey";
    private final String PROPERTY_USERID = "userid";

    private String getAPIKey(final Account account) {
        return account.getStringProperty(PROPERTY_APIKEY, null);
    }

    private String getUserID(final Account account) {
        return account.getStringProperty(PROPERTY_USERID, null);
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
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDirectDownload(link, null);
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
            handleDirectDownload(link, account);
        }
    }

    private void handleDirectDownload(final DownloadLink link, final Account account) throws Exception {
        this.requestFileInformation(link);
        final String dllink = getDirectURL(link, account);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, -10);
        if (dl.getConnection().getResponseCode() == 403) {
            /*
             * This e.g. happens if the user deletes a file via the premium.to site and then tries to download the previously added link via
             * JDownloader.
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 10 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("application/json")) {
            br.followConnection();
            this.handleErrorsAPI(account, true);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    /** no override to keep plugin compatible to old stable */
    @SuppressWarnings("deprecation")
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else {
            mhm.runCheck(account, link);
            synchronized (supported_hosts_storage) {
                if (supported_hosts_storage.isEmpty()) {
                    logger.info("Storage-host list is empty: Performing full login to refresh it");
                    this.fetchAccountInfo(account);
                    if (supported_hosts_storage.isEmpty()) {
                        logger.info("Storage-host list is still empty");
                    } else {
                        logger.info("Storage-host list is filled now");
                    }
                }
            }
            String serverside_filename = link.getStringProperty("serverside_filename", null);
            dl = null;
            final String apikey = this.getAPIKey(account);
            final String userid = this.getUserID(account);
            final String url = Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
            final int maxConnections = -10;
            String finalURL = null;
            /*
             * 2019-04-15: URLs of some hosts can only be downloaded via storage (= have to be fully downloaded top the servers of this
             * Multihost first and can then be downloaded by the user) while others can be used via normal download AND storage (e.g.
             * uploaded.net) - we prefer normal download and only use storage download if necessary.
             */
            final boolean requiresStorageDownload = supported_hosts_storage != null && supported_hosts_storage.contains(link.getHost());
            if (requiresStorageDownload) {
                /* Storage download */
                logger.info("Attempting STORAGE download: " + link.getHost());
                if (!debug_supports_storage_download) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Storage download is not yet supported via API");
                }
                /* Check if that URL has already been downloaded to their cloud. */
                br.getPage(API_BASE_STORAGE + "/check.php?userid=" + userid + "&apikey=" + apikey + "&url=" + url);
                handleErrorsAPI(account, false);
                final String status = getStorageAPIStatus();
                /* 2019-11-11: "Canceled" = URL has been added to Storage before but was deleted e.g. by user --> Add it again */
                if ("Not in queue".equalsIgnoreCase(status) || "Canceled".equalsIgnoreCase(status)) {
                    /* Not on their servers? Add to download-queue! */
                    br.getPage(API_BASE_STORAGE + "/add.php?userid=" + userid + "&apikey=" + apikey + "&url=" + url);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Added URL to premium.to Storage: Storage download pending", 1 * 60 * 1000);
                } else if ("completed".equalsIgnoreCase(status)) {
                    /* File has been downloaded to their servers and download should be possible now. */
                    finalURL = API_BASE_STORAGE + "/download.php?userid=" + userid + "&apikey=" + apikey + "&url=" + url;
                } else {
                    /* WTF this should never happen */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown status");
                }
                /* We might need this later. */
                serverside_filename = PluginJSonUtils.getJson(br, "Filename");
            } else {
                /* Normal (direct) download */
                logger.info("Attempting DIRECT download: " + link.getHost());
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
                this.handleErrorsAPI(account, false);
                logger.severe("PremiumTo Error");
                /* 2019-10-24: TODO: Consider removing this old errorhandling code. All errors should be returned via json by now. */
                if (br.containsHTML("File not found")) {
                    // we can not trust multi-hoster file not found returns, they could be wrong!
                    // jiaz new handling to dump to next download candidate.
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                } else if (br.toString().matches("File hosting service not supported")) {
                    mhm.putError(account, link, 10 * 60 * 1000l, "hoster_unsupported");
                } else if (br.containsHTML("Not enough traffic")) {
                    /*
                     * With our special traffic it's a bit complicated. When you still have a little Special Traffic but you have enough
                     * standard traffic, it will show you "Not enough traffic" for the filehost Uploaded.net for example.
                     */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                } else if (br.containsHTML("No premium account available")) {
                    mhm.putError(account, link, 60 * 60 * 1000l, "No premium account available");
                }
                /*
                 * after x retries we disable this host and retry with normal plugin
                 */
                mhm.handleErrorGeneric(account, link, "unknown_dl_error", 2, 5 * 60 * 1000l);
            }
            /* Check if the download is successful && user wants JD to delete the file in his premium.to account afterwards. */
            final PremiumDotToConfigInterface config = getAccountJsonConfig(account);
            /* 2019-10-24: An API call is missing for that. This feature will have to remain disabled until we get that. */
            final boolean canDeleteStorageFile = false;
            if (dl.startDownload() && config.isClearDownloadHistory() && canDeleteStorageFile) {
                String storageID = null;
                if (link.getDownloadURL().matches(type_storage)) {
                    storageID = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
                } else if (serverside_filename != null) {
                    /*
                     * 2019-06-05: This is a workaround!! Their API should better return the storageID via 'download.php' (see upper code).
                     */
                    logger.info("Trying to find storageID");
                    try {
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

    private void handleErrorsAPI(final Account account, final boolean trust_error_404_as_file_not_found) throws Exception {
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
            /*
             * Unsupported filehost - rare case but will happen if admin e.g. forgets to remove currently non-working hosts from array of
             * supported hosts
             */
            mhm.putError(account, this.getDownloadLink(), 10 * 60 * 1000l, "Filehost is not supported");
        case 403:
            /* Not enough traffic left */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Not enough traffic left", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        case 404:
            if (trust_error_404_as_file_not_found) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                /* 2019-10-30: We cannot trust this API errormessage */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Untrusted error 404", 5 * 60 * 1000);
            }
        case 405:
            /* Rare case: User has reached max. storage files limit (2019-04-15: Current limit: 200 files) */
            /* {"code":405,"message":"Too many files"} */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Storage max files limit reached", 5 * 60 * 1000);
        case 500:
            /* {"code":500,"message":"Currently no available premium acccount for this filehost"} */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Storage max files limit reached", 5 * 60 * 1000);
        default:
            /* TODO: Unknown error */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Unknown error", 5 * 60 * 1000);
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
            br.setFollowRedirects(true);
            ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(this.getHost());
            if (accs == null || accs.size() == 0) {
                /* 2019-10-23: All o those URLs should be downloadable without account/logging in */
                // if (link.getPluginPatternMatcher().matches(type_storage)) {
                // /* This linktype can only be downloaded/checked via account */
                // link.getLinkStatus().setStatusText("Only downlodable via account!");
                // return AvailableStatus.UNCHECKABLE;
                // }
                if (requiresAccount(link)) {
                    return AvailableStatus.UNCHECKABLE;
                }
                return getDirecturlStatus(link, null);
            } else {
                for (final Account acc : accs) {
                    login(acc, false);
                    return getDirecturlStatus(link, acc);
                }
                return AvailableStatus.UNCHECKABLE;
            }
        }
    }

    private AvailableStatus getDirecturlStatus(final DownloadLink link, final Account account) throws Exception {
        if (link.getPluginPatternMatcher().matches(type_storage_file) && account == null) {
            return AvailableStatus.UNCHECKABLE;
        }
        final String dllink = getDirectURL(link, account);
        URLConnectionAdapter con = null;
        try {
            /* 2019-10-23: HEADRequest does not work anymore, use GET instead */
            con = br.openGetConnection(dllink);
            if (con.getResponseCode() == 403) {
                /* Either invalid URL or user deleted file from Storage/Cloud --> URL is invalid now. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (con.getContentType().contains("html") || con.getContentType().contains("application/json")) {
                br.followConnection();
                /* We expect an API error */
                this.handleErrorsAPI(account, true);
                return AvailableStatus.UNCHECKABLE;
            }
            long fileSize = con.getLongContentLength();
            if (fileSize <= 0) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            link.setFinalFileName(getFileNameFromHeader(con));
            link.setDownloadSize(fileSize);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    private String getDirectURL(final DownloadLink link, final Account account) {
        final String dllink;
        if (link.getPluginPatternMatcher().matches(type_storage_file)) {
            /* API required */
            if (account == null) {
                /* This linktype can only be generated with Account */
                return null;
            }
            final String apikey = this.getAPIKey(account);
            final String userid = this.getUserID(account);
            final String fileid = new Regex(link.getPluginPatternMatcher(), type_storage_file).getMatch(0);
            dllink = API_BASE_STORAGE + "/getstoragefile.php?userid=" + userid + "&apikey=" + apikey + "&file=" + fileid;
        } else if (link.getPluginPatternMatcher().matches(type_torrent_file)) {
            /* API required */
            if (account == null) {
                /* This linktype can only be generated with Account */
                return null;
            }
            final String apikey = this.getAPIKey(account);
            final String userid = this.getUserID(account);
            final String[] ids = new Regex(link.getPluginPatternMatcher(), type_torrent_file).getMatch(0).split("/");
            final String torrentID = ids[0];
            /* 2019-12-01: Replaces static usage of API_BASE_TORRENT to e.g. allow torrent2.premium.to */
            final String api_host = new Regex(link.getPluginPatternMatcher(), "https?://([^/]+)/").getMatch(0);
            if (link.getPluginPatternMatcher().matches("https?://torrent\\.[^/]+/z/.+")) {
                /* zip file --> One file contains all files of one particular .torrent download */
                dllink = "https://" + api_host + "/api/2/getzip.php?userid=" + userid + "&apikey=" + apikey + "&torrent=" + torrentID;
            } else {
                /* Other filetype */
                final String fileID = ids[1];
                dllink = "https://" + api_host + "/api/2/getfile.php?userid=" + userid + "&apikey=" + apikey + "&torrent=" + torrentID + "&file=" + fileID;
            }
        } else {
            /* Remote-Directurl (Torrent & Storage) - no API and no account required */
            dllink = link.getPluginPatternMatcher();
        }
        return dllink;
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
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account != null) {
            return true;
        } else if (!requiresAccount(downloadLink)) {
            /* Some directurls are downloadable without account */
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