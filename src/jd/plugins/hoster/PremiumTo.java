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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadLinkDownloadable;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetConfigPanel;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.config.AccountConfigInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.SIZEUNIT;
import org.jdownloader.settings.staticreferences.CFG_GUI;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premium.to" }, urls = { "https?://torrent(?:\\d+)?\\.premium\\.to/(?:t/[a-z0-9]+/\\d+|z/[a-z0-9]+|r/\\d+/[A-F0-9]{32}/[a-z0-9]+/\\d+/[^/]+)|https?://storage\\.premium\\.to/(?:file/[A-Z0-9]+|remote/[A-Z0-9]+/[A-Z0-9]+/[A-Z0-9]+/[^/]+)" })
public class PremiumTo extends UseNet {
    private final String PROPERTY_normalTraffic                                            = "normalTraffic";
    private final String PROPERTY_specialTraffic                                           = "specialTraffic";
    private final String type_torrent                                                      = "(?i)https?://torrent.*?\\..+";
    private final String type_torrent_file                                                 = "(?i)https?://torrent.*?\\.[^/]+/(?:t|z)/(.+)";
    private final String type_torrent_remote                                               = "(?i)https?://torrent.*?\\.[^/]+/r/\\d+/[A-F0-9]{32}/([a-z0-9]+/\\d+)/[^/]+";
    private final String type_storage                                                      = "(?i)https?://storage\\..+";
    /* storage.premium.to --> Extract download URLs */
    private final String type_storage_file                                                 = "(?i)https?://storage\\.[^/]+/file/(.+)";
    /* storage.premium.to --> Extract remote URLs */
    private final String type_storage_remote                                               = "(?i)https?://storage\\.[^/]+/(?:remote|r)/[A-Z0-9]+/[A-Z0-9]+/([A-Z0-9]+)/.+";
    // private static final String type_torrent = "https?://torrent.+";
    private final String API_BASE                                                          = "https://api.premium.to/api/2";
    private final String API_BASE_STORAGE                                                  = "https://storage.premium.to/api/2";
    private final String API_BASE_TORRENT                                                  = "https://torrent.premium.to/api/2";
    private final String PROPERTY_ACCOUNT_DEACTIVATED_FILEHOSTS_DIALOG_SHOWN_AND_CONFIRMED = "deactivated_filehosts_dialog_shown_and_confirmed";

    private boolean requiresAccount(final DownloadLink link) {
        if (link != null && link.getPluginPatternMatcher() != null && (link.getPluginPatternMatcher().matches(type_torrent_remote) || link.getPluginPatternMatcher().matches(type_storage_remote))) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * 2019-11-10: Internal switch to force disable all Storage hosts - do not touch this unless e.g. admin requests this or API breaks
     * down.
     */
    private static final boolean     plugin_supports_storage_download = true;
    private static final Set<String> supported_hosts_storage          = new HashSet<String>();

    public PremiumTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://premium.to/");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = new Browser();
        br.setFollowRedirects(true);
        br.setAcceptLanguage("en, en-gb;q=0.8");
        br.setConnectTimeout(getConnectTimeout());
        br.setReadTimeout(getReadTimeout());
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
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

    private String getFID(final DownloadLink link) {
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(type_storage)) {
            if (link.getPluginPatternMatcher().matches(type_storage_file)) {
                return new Regex(link.getPluginPatternMatcher(), type_storage_file).getMatch(0);
            } else {
                return new Regex(link.getPluginPatternMatcher(), type_storage_remote).getMatch(0);
            }
        } else if (link.getPluginPatternMatcher().matches(type_torrent)) {
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

        @AboutConfig
        @DefaultBooleanValue(true)
        @Order(10)
        boolean isClearDownloadHistory();

        void setClearDownloadHistory(boolean b);

        @AboutConfig
        @DefaultBooleanValue(false)
        @Order(20)
        boolean isEnableStorageWhiteListing();

        void setEnableStorageWhiteListing(boolean b);

        @AboutConfig
        @DefaultStringValue("examplehost1.com,examplehost2.net")
        @Order(30)
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

    private static int getReadTimeout() {
        return 300 * 1000;
    }

    private static int getConnectTimeout() {
        return 300 * 1000;
    }

    private Map<String, Object> login(Account account, boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final String userid = this.getUserID(account);
            final String apikey = this.getAPIKey(account);
            if (!force) {
                /* Trust existing login-data without check */
                return null;
            }
            br.getPage(API_BASE + "/traffic.php?userid=" + userid + "&apikey=" + apikey);
            return this.handleErrorsAPI(null, account, false);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> userinfo = login(account, true);
        /* Normal traffic */
        final long nT = ((Number) userinfo.get("traffic")).longValue();
        /* Special traffic */
        final long spT = ((Number) userinfo.get("specialtraffic")).longValue();
        ai.setTrafficLeft(nT + spT);
        // set both so we can check in canHandle.
        account.setProperty(PROPERTY_normalTraffic, nT);
        account.setProperty(PROPERTY_specialTraffic, spT);
        String additionalAccountStatus = "";
        if (nT > 0 && spT > 0) {
            final SIZEUNIT maxSizeUnit = (SIZEUNIT) CFG_GUI.MAX_SIZE_UNIT.getValue();
            additionalAccountStatus = String.format(" | Normal Traffic: %s Special Traffic: %s", SIZEUNIT.formatValue(maxSizeUnit, nT), SIZEUNIT.formatValue(maxSizeUnit, spT));
        }
        final String apikey = this.getAPIKey(account);
        final String userid = this.getUserID(account);
        final ArrayList<String> supported_hosts_regular = new ArrayList<String>();
        ArrayList<String> supported_hosts_storage = new ArrayList<String>();
        try {
            br.getPage(API_BASE + "/hosts.php?userid=" + userid + "&apikey=" + apikey);
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
            final List<Object> ressourcelist = (List<Object>) entries.get("hosts");
            for (final Object hostO : ressourcelist) {
                if (hostO instanceof String) {
                    supported_hosts_regular.add((String) hostO);
                }
            }
        } catch (final Throwable e) {
            logger.info("Failure to find regular supported hosts");
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
            final String supported_hosts_storage_serverside[] = br.getRequest().getHtmlCode().toLowerCase().split(";|\\s+");
            for (final String tmp_supported_host_storage : supported_hosts_storage_serverside) {
                if (!supported_hosts_regular.contains(tmp_supported_host_storage)) {
                    /*
                     * Make sure to add only "storage-only" hosts to storage Array as some hosts can be used via both ways - we prefer
                     * direct downloads!
                     */
                    if (plugin_supports_storage_download) {
                        logger.info("Found Storage-only host: " + tmp_supported_host_storage);
                        supported_hosts_storage.add(tmp_supported_host_storage);
                    } else {
                        logger.info("Storage functionality disabled: Skipping Storage-only host: " + tmp_supported_host_storage);
                    }
                }
            }
        } catch (final Throwable e) {
            logger.log(e);
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
        ai.setMultiHostSupport(this, supported_hosts_regular);
        real_supported_hosts_regular = ai.getMultiHostSupport();
        ai.setMultiHostSupport(this, supported_hosts_storage);
        real_supported_hosts_storage = ai.getMultiHostSupport();
        {
            /* Handling for Storage hosts based on users' plugin settings. */
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
                    ai.setMultiHostSupport(this, user_whitelisted_hosts_storage);
                    real_user_whitelisted_hosts_storage = ai.getMultiHostSupport();
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
                logger.info("Adding final active storage hosts to list of supported hosts: " + real_supported_hosts_storage);
                real_supported_hosts_regular.addAll(real_supported_hosts_storage);
            }
            synchronized (PremiumTo.supported_hosts_storage) {
                PremiumTo.supported_hosts_storage.clear();
                if (real_supported_hosts_storage != null) {
                    /* Add host to special Array of Storage hosts */
                    PremiumTo.supported_hosts_storage.addAll(real_supported_hosts_storage);
                }
            }
        }
        final List<String[]> defaultServersideDeactivatedWebsites = new ArrayList<String[]>();
        defaultServersideDeactivatedWebsites.add(new String[] { "mega.nz", "mega.co.nz" });
        final Iterator<String[]> it = defaultServersideDeactivatedWebsites.iterator();
        while (it.hasNext()) {
            final String[] domains = it.next();
            for (String domain : domains) {
                if (real_supported_hosts_regular.contains(domain)) {
                    it.remove();
                    break;
                }
            }
        }
        if (defaultServersideDeactivatedWebsites.size() > 0) {
            showServersideDeactivatedHostInformation(account, defaultServersideDeactivatedWebsites.get(0)[0]);
        }
        ai.setMultiHostSupport(this, real_supported_hosts_regular);
        ai.setStatus("Premium account" + additionalAccountStatus);
        return ai;
    }

    /**
     * This dialog is there to make users of this multihoster aware that they can control the list of supported filehosts for this
     * multihoster serverside in their multihoster account. </br> Some filehosts are disabled by default which is the core information this
     * dialog is supposed to tell the user.
     */
    private Thread showServersideDeactivatedHostInformation(final Account account, final String exampleHost) {
        final boolean userConfirmedDialogAlready = account.getBooleanProperty(PROPERTY_ACCOUNT_DEACTIVATED_FILEHOSTS_DIALOG_SHOWN_AND_CONFIRMED, false);
        if (!userConfirmedDialogAlready) {
            final String host = getHost();
            final Thread thread = new Thread() {
                public void run() {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = host + " - Informationen zu serverseitig standardmäßig für Downloadmanager deaktivierten Hostern";
                        message += "Hallo liebe(r) " + host + " NutzerIn\r\n";
                        message += host + " hat einige Filehoster wie z.B. '" + exampleHost + "' standardmäßig für Downloadmanager deaktiviert, um deinen Traffic nicht zu verschwenden.\r\n";
                        message += "Falls du " + exampleHost + " oder andere standardmäßig für Downloadmanager deaktivierte Hoster über " + host + " in JD nutzen möchtest, musst du folgendes tun:\t\r\n";
                        message += "1. Öffne " + host + " im Browser und logge dich ein.\r\n";
                        message += "2. Klicke auf das tab 'DLM' -> Setze das Häckchen bei allen Hostern, die in der Hosterliste in JD erscheinen sollen und klicke auf den Button 'update'.\r\n";
                        message += "3. In JD: Rechtsklick auf deinen " + host + " Account in JDownloader -> Aktualisieren\r\n";
                        message += "Nun sollten Hoster, die vorher ggf. fehlten z.B. '" + exampleHost + "' in der Liste der unterstützten Hoster in JD aufgeführt werden.\r\n";
                    } else {
                        title = host + " - Information about filehosters, deactivated for downloadmanagers by default serverside by " + host;
                        message += "Hello dear " + host + " user\r\n";
                        message += host + " deactivated some filehosts like '" + exampleHost + "' by default for downloadmanagers in order to not waste any of your traffic.\r\n";
                        message += "If you want to use " + exampleHost + " or other filehosts in JD which are disabled by " + host + " for downloadmanagers by default, follow these instructions:\r\n";
                        message += "1. Open " + host + " in your browser and login.\r\n";
                        message += "2. Click on the tab 'DLM' and enable the checkboxes for all filehosts you wish to use in JDownloader and click on the button 'update'.\r\n";
                        message += "3. In JDownloader, rightclick on your " + host + " account -> Refresh\r\n";
                        message += "Now all hosts, which might have been missing before e.g. '" + exampleHost + "' should be visible in the list of supported hosts in JDownloader.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(5 * 60 * 1000);
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    try {
                        ret.throwCloseExceptions();
                        account.setProperty(PROPERTY_ACCOUNT_DEACTIVATED_FILEHOSTS_DIALOG_SHOWN_AND_CONFIRMED, true);
                    } catch (DialogNoAnswerException e) {
                        getLogger().log(e);
                        if (!e.isCausedByTimeout()) {
                            account.setProperty(PROPERTY_ACCOUNT_DEACTIVATED_FILEHOSTS_DIALOG_SHOWN_AND_CONFIRMED, true);
                        }
                    }
                };
            };
            thread.setDaemon(true);
            thread.start();
            return thread;
        } else {
            return null;
        }
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
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST, LazyPlugin.FEATURE.USENET };
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
        } else {
            handleDirectDownload(link, account);
        }
    }

    /** Handles download of selfhosted files. */
    private void handleDirectDownload(final DownloadLink link, final Account account) throws Exception {
        if (this.requiresAccount(link) && account == null) {
            throw new AccountRequiredException();
        }
        this.requestFileInformation(link, account);
        final String dllink = getDirectURL(link, account);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, -10);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                /*
                 * This e.g. happens if the user deletes a file via the premium.to site and then tries to download the previously added link
                 * via JDownloader.
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 10 * 60 * 1000l);
            } else {
                this.handleErrorsAPI(link, account, true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
        }
        dl.startDownload();
    }

    @SuppressWarnings("deprecation")
    @Override
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
            String serverside_filename = link.getStringProperty("serverside_filename");
            this.dl = null;
            final String apikey = this.getAPIKey(account);
            final String userid = this.getUserID(account);
            final String urlUrlEncoded = Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
            /*
             * 2019-04-15: URLs of some hosts can only be downloaded via storage (= have to be fully downloaded top the servers of this
             * Multihost first and can then be downloaded by the user) while others can be used via normal download AND storage (e.g.
             * uploaded.net) - we prefer normal download and only use storage download if necessary.
             */
            final boolean requiresStorageDownload;
            synchronized (supported_hosts_storage) {
                if (supported_hosts_storage != null && supported_hosts_storage.contains(link.getHost())) {
                    requiresStorageDownload = true;
                } else {
                    requiresStorageDownload = false;
                }
            }
            final UrlQuery query = new UrlQuery();
            query.appendEncoded("userid", userid);
            query.appendEncoded("apikey", apikey);
            if (link.getSha1Hash() != null) {
                query.appendEncoded("hash_sha1", link.getSha1Hash());
            }
            if (link.getMD5Hash() != null) {
                query.appendEncoded("hash_md5", link.getMD5Hash());
            }
            if (link.getSha256Hash() != null) {
                query.appendEncoded("hash_sha256", link.getSha256Hash());
            }
            String finalURL = null;
            if (requiresStorageDownload) {
                /* Storage download */
                logger.info("Attempting STORAGE download: " + link.getHost());
                if (!plugin_supports_storage_download) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Storage download is not yet supported via API");
                }
                /* Check if that URL has already been downloaded to their cloud. */
                br.getPage(API_BASE_STORAGE + "/check.php?" + query.toString() + "&url=" + urlUrlEncoded);
                final Map<String, Object> resp = handleErrorsAPI(link, account, false);
                final String status = (String) resp.get("Status");
                /* 2019-11-11: "Canceled" = URL has been added to Storage before but was deleted e.g. by user --> Add it again */
                if ("Not in queue".equalsIgnoreCase(status) || "Canceled".equalsIgnoreCase(status)) {
                    /* Not on their servers? Add to download-queue! */
                    br.getPage(API_BASE_STORAGE + "/add.php?" + query.toString() + "&url=" + urlUrlEncoded);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Added URL to premium.to Storage: Storage download pending", 1 * 60 * 1000);
                } else if ("completed".equalsIgnoreCase(status)) {
                    /* File has been downloaded to their servers and download should be possible now. */
                    finalURL = API_BASE_STORAGE + "/download.php?" + query.toString() + "&url=" + urlUrlEncoded;
                } else {
                    /* WTF this should never happen */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown storage status");
                }
                /* We might need this later */
                serverside_filename = (String) resp.get("Filename");
            } else {
                /* Normal (direct) download */
                logger.info("Attempting DIRECT download: " + link.getHost());
                login(account, false);
                finalURL = API_BASE + "/getfile.php?" + query.toString() + "&link=" + urlUrlEncoded;
            }
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            final URLConnectionAdapter con = brc.openGetConnection(finalURL);
            try {
                if (con.isOK() && this.looksLikeDownloadableContent(con) && con.getCompleteContentLength() > 0) {
                    finalURL = con.getRequest().getUrl();
                    if (link.getVerifiedFileSize() != -1 && link.getVerifiedFileSize() != con.getCompleteContentLength()) {
                        logger.info("Workaround for size missmatch(rar padding?!)!");
                        link.setVerifiedFileSize(con.getCompleteContentLength());
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
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadable, br.createGetRequest(finalURL), true, -10);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 404) {
                    /* File offline */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 1000);
                } else if (dl.getConnection().getResponseCode() == 420) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 420", 3 * 60 * 1000);
                } else {
                    this.handleErrorsAPI(link, account, false);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error", 3 * 60 * 1000);
                }
            }
            /* Check if the download is successful && user wants JD to delete the file in his premium.to account afterwards. */
            final PremiumDotToConfigInterface config = getAccountJsonConfig(account);
            /* 2019-10-24: An API call is missing for that. This feature will have to remain disabled until we get that. */
            final boolean canDeleteStorageFile = false;
            if (dl.startDownload() && config.isClearDownloadHistory() && canDeleteStorageFile && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
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
                        Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
                        final List<Object> storage_objects = (List<Object>) entries.get("f");
                        for (final Object fileO : storage_objects) {
                            entries = (Map<String, Object>) fileO;
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

    private Map<String, Object> handleErrorsAPI(final DownloadLink link, final Account account, final boolean trust_error_404_as_file_not_found) throws Exception {
        if (StringUtils.containsIgnoreCase(br.getHttpConnection().getContentType(), "json")) {
            Map<String, Object> entries = null;
            try {
                entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            } catch (final JSonMapperException ignore) {
                /* This should never happen. */
                if (link != null) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Invalid API response", 60 * 1000l);
                } else {
                    throw new AccountUnavailableException("Invalid API response", 60 * 1000);
                }
            }
            final Number responsecodeO = (Number) entries.get("code");
            if (responsecodeO == null) {
                /* No error e.g. storage download status: {"Status":"Not in queue"} */
                return entries;
            }
            final String errormessage = (String) entries.get("message");
            switch (responsecodeO.intValue()) {
            case 0:
                /* No error */
                break;
            case 200:
                /* Everything ok */
                break;
            case 400:
                /*
                 * Invalid parameter - this should never happen! 2020-06-22: Thiy can happen when the user e.g. uses wrong characters in
                 * login credentials --> Display account invalid message if this didn't happen during download!
                 */
                if (this.getDownloadLink() == null) {
                    exceptionInvalidLogin();
                } else {
                    throw new AccountUnavailableException("API response 400: " + errormessage, 5 * 60 * 1000);
                }
            case 401:
                /* Invalid apikey --> Invalid logindata */
                exceptionInvalidLogin();
            case 402:
                /*
                 * Unsupported filehost - rare case but will happen if admin e.g. forgets to remove currently non-working hosts from array
                 * of supported hosts. Do not throw a permanent error here as cached content could always be available!
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
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Untrusted error 404 file not found", 3 * 60 * 1000);
                }
            default:
                /* {"code":405,"message":"Too many files"} */
                /* {"code":500,"message":"Currently no available premium acccount for this filehost"} */
                if (link == null) {
                    /* Error must have happened during login */
                    throw new AccountUnavailableException(errormessage, 5 * 60 * 1000);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage, 3 * 60 * 1000);
                }
            }
            return entries;
        } else {
            /* TODO: Check if these ones still exist */
            if (br.getURL() != null && br.getURL().contains("storage.premium.to")) {
                /* Now handle special Storage errors / statuscodes */
                if ("Invalid API Key".equalsIgnoreCase(br.getRequest().getHtmlCode())) {
                    /* This should never happen? */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Invalid Storage apikey", 3 * 60 * 1000);
                }
            }
            return null;
        }
    }

    private void exceptionInvalidLogin() throws PluginException {
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new AccountInvalidException("Zugangsdaten ungültig!\r\nDu findest deine Zugangsdaten für JD hier:\r\npremium.to Webseite --> Tab 'Account' --> API Zugangsdaten");
        } else {
            throw new AccountInvalidException("Invalid login credentials!\r\nYou can find your special JD login credentials here:\r\npremium.to website --> Tab 'Account' --> API login credentials");
        }
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
            final Account account = AccountController.getInstance().getValidAccount(this.getHost());
            return requestFileInformation(link, account);
        }
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        return getDirecturlStatus(link, account);
    }

    private AvailableStatus getDirecturlStatus(final DownloadLink link, final Account account) throws Exception {
        if (requiresAccount(link) && account == null) {
            /* Account required to check given link. */
            return AvailableStatus.UNCHECKABLE;
        }
        final String dllink = getDirectURL(link, account);
        URLConnectionAdapter con = null;
        try {
            /* 2019-10-23: HEADRequest does not work anymore, use GET instead */
            con = br.openGetConnection(dllink);
            if (!this.looksLikeDownloadableContent(con)) {
                if (con.getResponseCode() == 403) {
                    /* Either invalid URL or user deleted file from Storage/Cloud --> URL is invalid now. */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    br.followConnection(true);
                    /* We expect an API error */
                    this.handleErrorsAPI(link, account, true);
                    return AvailableStatus.UNCHECKABLE;
                }
            }
            final long fileSize = con.getCompleteContentLength();
            if (fileSize <= 0) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            link.setFinalFileName(getFileNameFromHeader(con));
            link.setVerifiedFileSize(fileSize);
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
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        if (isUsenetLink(link)) {
            /* 2016-07-29: psp: Lowered this from 10 to 3 RE: admin */
            return 3;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account != null) {
            return true;
        } else {
            return false;
        }
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
        return ret;
    }
}