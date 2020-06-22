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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
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
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.download.DownloadLinkDownloadable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premium.to" }, urls = { "https?://torrent(?:\\d+)?\\.premium\\.to/(?:t/[a-z0-9]+/\\d+|z/[a-z0-9]+|r/\\d+/[A-F0-9]{32}/[a-z0-9]+/\\d+/[^/]+)|https?://storage\\.premium\\.to/(?:file/[A-Z0-9]+|remote/[A-Z0-9]+/[A-Z0-9]+/[A-Z0-9]+/[^/]+)" })
public class PremiumTo extends UseNet {
    private final String             normalTraffic                   = "normalTraffic";
    private final String             specialTraffic                  = "specialTraffic";
    private static final String      type_torrent                    = "https?://torrent.*?\\..+";
    private static final String      type_torrent_file               = "https?://torrent.*?\\.[^/]+/(?:t|z)/(.+)";
    private static final String      type_torrent_remote             = "https?://torrent.*?\\.[^/]+/r/\\d+/[A-F0-9]{32}/([a-z0-9]+/\\d+)/[^/]+";
    private static final String      type_storage                    = "https?://storage\\..+";
    /* storage.premium.to --> Extract download URLs */
    private static final String      type_storage_file               = "https?://storage\\.[^/]+/file/(.+)";
    /* storage.premium.to --> Extract remote URLs */
    private static final String      type_storage_remote             = "https?://storage\\.[^/]+/(?:remote|r)/[A-Z0-9]+/[A-Z0-9]+/([A-Z0-9]+)/.+";
    // private static final String type_torrent = "https?://torrent.+";
    /* 2019-10-23: According to admin, missing https support for API is not an issue */
    private static final String      API_BASE                        = "http://api.premium.to/api/2";
    private static final String      API_BASE_STORAGE                = "https://storage.premium.to/api/2";
    private static final String      API_BASE_TORRENT                = "https://torrent.premium.to/api/2";
    /*
     * 2019-11-10: Internal switch to force disable all Storage hosts - do not touch this unless e.g. admin requests this or API breaks
     * down.
     */
    private static final boolean     debug_supports_storage_download = true;
    private static final Set<String> supported_hosts_storage         = new HashSet<String>();

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
        return 300 * 1000;
    }

    private int getConnectTimeout() {
        return 300 * 1000;
    }

    private Browser prepBrowser(Browser prepBr) {
        prepBr.setFollowRedirects(true);
        prepBr.setAcceptLanguage("en, en-gb;q=0.8");
        prepBr.setConnectTimeout(getConnectTimeout());
        prepBr.setReadTimeout(getReadTimeout());
        prepBr.getHeaders().put("User-Agent", "JDownloader");
        return prepBr;
    }

    private boolean login(Account account, boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBrowser(br);
                final String userid = this.getUserID(account);
                final String apikey = this.getAPIKey(account);
                if (!force) {
                    /* Trust existing data without check */
                    return false;
                }
                br.getPage(API_BASE + "/traffic.php?userid=" + userid + "&apikey=" + apikey);
                this.handleErrorsAPI(account, false);
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    /* 2019-10-23: There are no cookies given via API anymore */
                    // account.clearCookies("");
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
        ac.setTrafficLeft(nT + spT);
        // set both so we can check in canHandle.
        account.setProperty(normalTraffic, nT);
        account.setProperty(specialTraffic, spT);
        if (nT > 0 && spT > 0) {
            additionalAccountStatus = String.format(" | Normal Traffic: %d MiB Special Traffic: %d MiB", nT, spT);
        }
        final ArrayList<String> supported_hosts_regular = new ArrayList<String>();
        ArrayList<String> supported_hosts_storage = new ArrayList<String>();
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
            logger.info("Failure to find regular supported hosts");
        } finally {
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
            boolean onlyAllowWhitelistedStorageHosts = false;
            String whitelistedStorageHostsCommaSeparated = null;
            try {
                /* 2020-01-29: Temp. workaround for ClassCastException see also: jdlog://6337230900751/ */
                final PremiumDotToConfigInterface config = getAccountJsonConfig(account);
                onlyAllowWhitelistedStorageHosts = config.isEnableStorageWhiteListing();
                whitelistedStorageHostsCommaSeparated = config.getWhitelistedStorageHosts();
            } catch (final Throwable e) {
                logger.warning("Error while trying to load user-settings --> Using default settings");
            }
            if (onlyAllowWhitelistedStorageHosts) {
                logger.info("User enabled whitelisting of Storage hosts");
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
                }
            }
            synchronized (PremiumTo.supported_hosts_storage) {
                PremiumTo.supported_hosts_storage.clear();
                if (real_supported_hosts_storage != null) {
                    /* Add host to special Array of Storage hosts */
                    PremiumTo.supported_hosts_storage.addAll(real_supported_hosts_storage);
                }
            }
        }
        ac.setMultiHostSupport(this, real_supported_hosts_regular);
        ac.setStatus("Premium account" + additionalAccountStatus);
        return ac;
    }

    private String getUserID(final Account account) {
        return account.getUser();
    }

    private String getAPIKey(final Account account) {
        return account.getPass();
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
            final boolean requiresStorageDownload;
            synchronized (supported_hosts_storage) {
                requiresStorageDownload = supported_hosts_storage != null && supported_hosts_storage.contains(link.getHost());
            }
            final UrlQuery query = new UrlQuery();
            query.appendEncoded("userid", userid);
            query.appendEncoded("apikey", apikey);
            /* TODO: Append these additional parameters */
            // if (link.getSha1Hash() != null) {
            // query.appendEncoded("hash_sha1", link.getSha1Hash());
            // }
            // if (link.getMD5Hash() != null) {
            // query.appendEncoded("hash_md5", link.getMD5Hash());
            // }
            // if (link.getSha256Hash() != null) {
            // query.appendEncoded("hash_sha256", link.getSha256Hash());
            // }
            // if (link.getFinalFileName() != null) {
            // query.appendEncoded("final_filename", link.getFinalFileName());
            // } else if (link.getName() != null) {
            // query.appendEncoded("filename", link.getName());
            // }
            if (requiresStorageDownload) {
                /* Storage download */
                logger.info("Attempting STORAGE download: " + link.getHost());
                if (!debug_supports_storage_download) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Storage download is not yet supported via API");
                }
                /* Check if that URL has already been downloaded to their cloud. */
                br.getPage(API_BASE_STORAGE + "/check.php?" + query.toString() + "&url=" + url);
                handleErrorsAPI(account, false);
                final String status = getStorageAPIStatus();
                /* 2019-11-11: "Canceled" = URL has been added to Storage before but was deleted e.g. by user --> Add it again */
                if ("Not in queue".equalsIgnoreCase(status) || "Canceled".equalsIgnoreCase(status)) {
                    /* Not on their servers? Add to download-queue! */
                    br.getPage(API_BASE_STORAGE + "/add.php?" + query.toString() + "&url=" + url);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Added URL to premium.to Storage: Storage download pending", 1 * 60 * 1000);
                } else if ("completed".equalsIgnoreCase(status)) {
                    /* File has been downloaded to their servers and download should be possible now. */
                    finalURL = API_BASE_STORAGE + "/download.php?" + query.toString() + "&url=" + url;
                } else {
                    /* WTF this should never happen */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown status");
                }
                /* We might need this later */
                serverside_filename = PluginJSonUtils.getJson(br, "Filename");
            } else {
                /* Normal (direct) download */
                logger.info("Attempting DIRECT download: " + link.getHost());
                login(account, false);
                finalURL = API_BASE + "/getfile.php?" + query.toString() + "&link=" + url;
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
                /* File offline */
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 1000);
            }
            if (!dl.getConnection().isContentDisposition()) {
                if (dl.getConnection().getResponseCode() == 420) {
                    dl.close();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 420", 3 * 60 * 1000);
                }
                br.followConnection();
                this.handleErrorsAPI(account, false);
                logger.severe("PremiumTo Error");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error", 3 * 60 * 1000);
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
        String errormessage = PluginJSonUtils.getJson(br, "message");
        if (StringUtils.isEmpty(errormessage)) {
            errormessage = "Unknown error";
        }
        switch (responsecode) {
        case 0:
            /* No error */
            break;
        case 200:
            /* Everything ok */
            break;
        case 400:
            /*
             * Invalid parameter - this should never happen! 2020-06-22: Thiy can happen when the user e.g. uses wrong characters in login
             * credentials --> Display account invalid message if this didn't happen during download!
             */
            if (this.getDownloadLink() == null) {
                invalidLogin();
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "API response 400: " + errormessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
        case 401:
            /* Invalid apikey --> Invalid logindata */
            invalidLogin();
        case 402:
            /*
             * Unsupported filehost - rare case but will happen if admin e.g. forgets to remove currently non-working hosts from array of
             * supported hosts. Do not throw a permanent error here as cached content could always be available!
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage, 3 * 60 * 1000);
        case 403:
            /* Not enough traffic left --> Temp. disable account */
            throw new AccountUnavailableException("Not enough traffic left", 5 * 60 * 1000l);
        case 404:
            if (trust_error_404_as_file_not_found) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                /* 2019-10-30: We cannot trust this API errormessage */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Untrusted error 404", 3 * 60 * 1000);
            }
        default:
            /* {"code":405,"message":"Too many files"} */
            /* {"code":500,"message":"Currently no available premium acccount for this filehost"} */
            errormessage = "Err " + responsecode + ": " + errormessage;
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage, 3 * 60 * 1000);
        }
        /* TODO: Check if these ones still exist */
        if (br.getURL() != null && br.getURL().contains("storage.premium.to")) {
            /* Now handle special Storage errors / statuscodes */
            if ("Invalid API Key".equalsIgnoreCase(br.toString())) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Invalid Storage apikey", 3 * 60 * 1000);
            }
            final String status = getStorageAPIStatus();
            if (StringUtils.isEmpty(status)) {
                /* No errors */
                return;
            }
            if (status.equalsIgnoreCase("In queue")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Storage download pending", 3 * 60 * 1000);
            }
        }
    }

    private void invalidLogin() throws PluginException {
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, String.format("Zugangsdaten ungültig!\r\nBitte bedenke, dass dieser Anbieter extra Zugangsdaten für JD zur Verfügung stellt, die von denen der Webseite abweichen!\r\nSiehe premium.to Hauptseite --> Account", this.getHost()), PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, String.format("Invalid login credentials!\r\nPlease keep in mind that this service is providing extra login credentials for JD which are different than the ones used to lgin via website!\r\nSee premium.to mainpage --> Account", this.getHost()), PluginException.VALUE_ID_PREMIUM_DISABLE);
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
                    /* Pick first account */
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
            } catch (final Throwable e) {
            }
        }
    }

    /** Generates final downloadurl of files hosted on premium.to in user's accounts. */
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