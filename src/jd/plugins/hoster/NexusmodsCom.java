//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.gui.swing.components.linkbutton.JLink;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nexusmods.com" }, urls = { "https?://(?:www\\.)?nexusmods\\.com+/Core/Libs/Common/Widgets/DownloadPopUp\\?id=(\\d+).+|nxm://([^/]+)/mods/(\\d+)/files/(\\d+)\\?key=([a-zA-Z0-9_/\\+\\=\\-%]+)\\&expires=(\\d+)\\&user_id=\\d+" })
public class NexusmodsCom extends antiDDoSForHost {
    public NexusmodsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://users.nexusmods.com/register/memberships");
    }

    @Override
    public String getAGBLink() {
        return "https://help.nexusmods.com/article/18-terms-of-service";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    /* API documentation: https://app.swaggerhub.com/apis-docs/NexusMods/nexus-mods_public_api_params_in_form_data/1.0 */
    public static final String   API_BASE                     = "https://api.nexusmods.com/v1";
    private String               dllink;
    private boolean              loginRequired;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String file_id = getFID(link);
        if (file_id != null) {
            final String dl_key = link.getStringProperty("dl_key", null);
            String linkid = this.getHost() + "://" + file_id;
            if (dl_key != null) {
                /*
                 * Free (account) users can add URLs which can expire. Allow them to add them again with new download_key parameters to make
                 * this easier.
                 */
                linkid += "_" + dl_key;
            }
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    /* URLs for their official downloadmanager --> We support them too --> Also see linkIsAPICompatible */
    private boolean isSpecialDownloadmanagerURL(final DownloadLink link) {
        return link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches("nxm://.+");
    }

    /** Returns the file_id (this comment is here as the URL contains multiple IDs) */
    private String getFID(final DownloadLink link) {
        if (isSpecialDownloadmanagerURL(link)) {
            final String game_domain_name = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
            final String mod_id = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
            final String dl_key = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(4);
            final String dl_key_expire = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(5);
            link.setProperty("game_domain_name", game_domain_name);
            link.setProperty("mod_id", mod_id);
            link.setProperty("dl_key", dl_key);
            link.setProperty("dl_expire", dl_key_expire);
            return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(3);
        } else {
            return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        }
    }

    public static Browser prepBrAPI(final Browser br, final Account account) throws PluginException {
        br.getHeaders().put("User-Agent", "JDownloader");
        final String apikey = getApikey(account);
        if (apikey == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("apikey", apikey);
        return br;
    }

    public static boolean isOfflineWebsite(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("No files have been uploaded yet|>File not found<|>Not found<|/noimage-1.png");
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        final String url = link.getPluginPatternMatcher();
        /*
         * 2020-01-17: TODO: Find a way to set the correct content-URL when users in free mode open up URLs to generate NXM URLs. This would
         * require "nmm" to be "1"!
         */
        if (StringUtils.contains(url, "nmm=1")) {
            link.setPluginPatternMatcher(url.replace("nmm=1", "nmm=0"));
        }
    }

    /** Account is either required because the files are 'too large' or also for 'adult files', or for 'no reason at all'. */
    public boolean isLoginRequired(final Browser br) {
        if (br.containsHTML("<h1>Error</h1>") && br.containsHTML("<h2>Adult-only content</h2>")) {
            // adult only content.
            return true;
        } else if (br.containsHTML("You need to be a member and logged in to download files larger")) {
            // large files
            return true;
        } else if (br.containsHTML(">Please login or signup to download this file<")) {
            return true;
        } else {
            return false;
        }
    }

    /** URLs added <= rev. 41547 are missing properties which are required to do download & linkcheck via API! */
    private boolean linkIsAPICompatible(final DownloadLink link) {
        final String game_domain_name = link.getStringProperty("game_domain_name", null);
        final String mod_id = link.getStringProperty("mod_id", null);
        if (game_domain_name != null && mod_id != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        final String apikey = getApikey(acc);
        if (acc != null && apikey != null && linkIsAPICompatible(link)) {
            this.prepBrAPI(br, acc);
            return requestFileInformationAPI(link);
        } else {
            return requestFileInformationWebsite(link);
        }
    }

    private AvailableStatus requestFileInformationWebsite(final DownloadLink link) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.ArchiveExtensions.ZIP);
        if (isSpecialDownloadmanagerURL(link)) {
            /* Cannot check here */
            return AvailableStatus.UNCHECKABLE;
        }
        final String fid = getFID(link.getPluginPatternMatcher());
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        {
            String loadBox = br.getRegex("loadBox\\('(https?://.*?)'").getMatch(0);
            if (loadBox != null) {
                getPage(loadBox);
                loadBox = br.getRegex("loadBox\\('(https?://.*?skipdonate)'").getMatch(0);
                if (loadBox != null) {
                    getPage(loadBox);
                }
            }
        }
        loginRequired = isLoginRequired(br);
        dllink = br.getRegex("window\\.location\\.href\\s*=\\s*\"(http[^<>\"]+)\";").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://filedelivery\\.nexusmods\\.com/[^<>\"]+)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://(?:www\\.)?nexusmods\\.com/[^<>\"]*Libs/Common/Managers/Downloads\\?Download[^<>\"]+)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("id\\s*=\\s*\"dl_link\"\\s*value\\s*=\\s*\"(https?://[^<>\"]*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("data-link\\s*=\\s*\"(https?://(?:premium-files|fs-[a-z0-9]+)\\.(?:nexusmods|nexus-cdn)\\.com/[^<>\"]*?)\"").getMatch(0);
                    }
                }
            }
        }
        String filename = br.getRegex("filedelivery\\.nexusmods\\.com/\\d+/([^<>\"]+)\\?fid=").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("data-link\\s*=\\s*\"https?://[^<>\"]+/files/\\d+/([^<>\"/]+)\\?").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("data-link\\s*=\\s*\"https?://[^<>\"]+/\\d+/\\d+/([^<>\"/]+)\\?").getMatch(0);
                if (filename == null && dllink != null) {
                    filename = getFileNameFromURL(new URL(dllink));
                }
            }
            if (filename == null && !link.isNameSet()) {
                filename = fid;
            }
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    private AvailableStatus requestFileInformationAPI(final DownloadLink link) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.ArchiveExtensions.ZIP);
        final String game_domain_name = link.getStringProperty("game_domain_name", null);
        final String mod_id = link.getStringProperty("mod_id", null);
        final String file_id = new Regex(link.getPluginPatternMatcher(), "\\?id=(\\d+)").getMatch(0);
        if (file_id == null || mod_id == null || game_domain_name == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage(API_BASE + String.format("/games/%s/mods/%s/files/%s", game_domain_name, mod_id, file_id));
        handleErrorsAPI(br);
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        return setFileInformationAPI(link, entries, game_domain_name, mod_id, file_id);
    }

    public static AvailableStatus setFileInformationAPI(final DownloadLink link, final LinkedHashMap<String, Object> entries, final String game_domain_name, final String mod_id, final String file_id) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.ArchiveExtensions.ZIP);
        String filename = (String) entries.get("file_name");
        final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        } else {
            /* Fallback */
            filename = game_domain_name + "_" + mod_id + "_" + file_id;
            link.setName(filename);
        }
        if (filesize > 0) {
            link.setDownloadSize(filesize * 1024);
        }
        final String description = (String) entries.get("description");
        if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
            link.setComment(description);
        }
        /* TODO: Add free account (error-) handling */
        // loginRequired = isLoginRequired(br);
        /* For some files we can find a sha256 hash through this sketchy hash. Example: TODO: Add example */
        final String external_virus_scan_url = (String) entries.get("external_virus_scan_url");
        if (external_virus_scan_url != null) {
            final String sha256_hash = new Regex(external_virus_scan_url, "virustotal.com/file/([a-f0-9]+)/.*").getMatch(0);
            if (sha256_hash != null) {
                link.setSha256Hash(sha256_hash);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, null, FREE_RESUME, FREE_MAXCHUNKS);
    }

    private void doFree(final DownloadLink link, final Account account, final boolean resumable, final int maxchunks) throws Exception, PluginException {
        if (dllink == null) {
            if (loginRequired) {
                if (account == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } else {
                    /*
                     * 2019-01-23: Added errorhandling but this should never happen because if an account exists we should be able to
                     * download!
                     */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Login failure");
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        dl.startDownload();
    }

    public String getFID(final String dlurl) {
        return new Regex(dlurl, "id=(\\d+)").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public void loginWebsite(final Account account) throws Exception {
        synchronized (account) {
            try {
                if (isAPIOnlyMode()) {
                    logger.info("Only API accounts are supported");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                boolean loggedIN = false;
                if (cookies != null) {
                    br.setCookies(account.getHoster(), cookies);
                    getPage("https://www." + account.getHoster());
                    if (!isLoggedinCookies()) {
                        logger.info("Existing login invalid: Full login required!");
                        br.clearCookies(getHost());
                    } else {
                        loggedIN = true;
                    }
                }
                if (!loggedIN) {
                    getPage("https://users." + this.getHost() + "/auth/sign_in");
                    final Form loginform = br.getFormbyKey("user%5Blogin%5D");
                    if (loginform == null) {
                        logger.warning("Failed to find loginform");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    String reCaptchaKey = br.getRegex("grecaptcha\\.execute\\('([^<>\"\\']+)'").getMatch(0);
                    if (reCaptchaKey == null) {
                        /* 2020-01-08: Fallback */
                        reCaptchaKey = "6Lf4vsIUAAAAAN6TyJATjxQbMAcKjBZ3rOc0ijrp";
                    }
                    loginform.put("user%5Blogin%5D", Encoding.urlEncode(account.getUser()));
                    loginform.put("user%5Bpassword%5D", Encoding.urlEncode(account.getPass()));
                    final DownloadLink original = this.getDownloadLink();
                    if (original == null) {
                        this.setDownloadLink(new DownloadLink(this, "Account", getHost(), "http://" + br.getRequest().getURL().getHost(), true));
                    }
                    try {
                        /* 2019-11-20: Invisible reCaptchaV2 is always required */
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaKey).getToken();
                        if (recaptchaV2Response == null) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        loginform.put("g-recaptcha-response%5Blogin%5D", Encoding.urlEncode(recaptchaV2Response));
                    } finally {
                        if (original == null) {
                            this.setDownloadLink(null);
                        }
                    }
                    this.submitForm(loginform);
                    if (!isLoggedinCookies()) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login failed.\r\nIf you own a premium account you should disable website login in Settings --> Plugin Settings --> nexusmods.com\r\nBe sure to delete your account and try again after changing this setting!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedinCookies() {
        return br.getCookie(br.getURL(), "pass_hash", Cookies.NOTDELETEDPATTERN) != null && br.getCookie(br.getURL(), "member_id", Cookies.NOTDELETEDPATTERN) != null && br.getCookie(br.getURL(), "sid", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        String apikey = getApikey(account);
        if (apikey != null) {
            return fetchAccountInfoAPI(account);
        }
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account);
        getPage("/users/myaccount?tab=api%20access");
        /* Try to find apikey - prefer API */
        /* TODO: Maybe generate apikey is it is not yet available */
        /* 2019-11-19: Turned this off as it is nothing that we should do. */
        // Form requestApiForm = null;
        // final Form[] forms = br.getForms();
        // for (final Form tmpForm : forms) {
        // final InputField actionField = tmpForm.getInputFieldByName("action");
        // final InputField application_slugField = tmpForm.getInputFieldByName("application_slug");
        // if (actionField != null && actionField.getValue().equals("request-key") && application_slugField == null) {
        // logger.info("Found 'request apikey' Form");
        // requestApiForm = tmpForm;
        // break;
        // }
        // }
        // if (requestApiForm != null) {
        // logger.info("Requesting apikey for the first time ...");
        // this.submitForm(requestApiForm);
        // }
        apikey = br.getRegex("id=\"personal_key\"[^>]*>([^<>\"]+)<").getMatch(0);
        if (apikey != null) {
            /* TODO: Consider removing original logindata once we found an apikey for safety reasons! */
            logger.info("Found apikey");
            saveApikey(account, apikey);
            return fetchAccountInfoAPI(account);
        } else {
            logger.info("Failed to find apikey - continuing via website");
            getPage("/users/myaccount");
            if (StringUtils.equalsIgnoreCase(br.getRegex("\"premium-desc\">\\s*(.*?)\\s*<").getMatch(0), "Inactive")) {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setConcurrentUsePossible(false);
            } else {
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setConcurrentUsePossible(true);
            }
            return ai;
        }
    }

    private AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        prepBrAPI(br, account);
        getPage(API_BASE + "/users/validate.json");
        handleErrorsAPI(br);
        final String is_premium = PluginJSonUtils.getJson(br, "is_premium");
        /* Censor original username/password to make it harder to make use of stolen account databases! */
        final String email = PluginJSonUtils.getJson(br, "email");
        if (!StringUtils.isEmpty(email) && email.length() > 3) {
            final String censored_email = "***" + email.substring(3);
            account.setUser(censored_email);
        }
        if (!isAPIOnlyMode()) {
            /* Do not store any old website login credentials! */
            account.setPass(null);
        }
        if ("true".equalsIgnoreCase(is_premium)) {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setUnlimitedTraffic();
            ai.setStatus("Premium user");
        } else {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            /*
             * 2020-01-24: Free account downloads are not possible at all because we cannot easily catch the URLs which are ment to go into
             * their own official downloadmanager as they have their own protocol: NXM://blabla. Code for this has been added nontheless.
             */
            ai.setTrafficLeft(0);
            if (isAPIOnlyMode()) {
                // ai.setStatus("Free user [Only NXM URLs can be downloaded]");
                ai.setStatus("Free user");
                /* 2020-01-15: Website mode is not supported anymore. Accept free accounts and display them with ZERO trafficleft! */
                // throw new PluginException(LinkStatus.ERROR_PREMIUM, "Free account!\r\nDownloads over API via free account are
                // impossible!\r\nTo be able to download via free account, enable website login in Settings --> Plugin Settings -->
                // nexusmods.com\r\nThis login mask will look different afterwards and you will be able to login via username/mail &
                // password.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                ai.setStatus("Free user");
            }
        }
        return ai;
    }

    public static void handleErrorsAPI(final Browser br) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 400) {
            /*
             * 2020-01-15: Attempted free account download fails:
             * {"code":400,"message":"Provided key and expire time isn't correct for this user/file."}
             */
            throw new AccountRequiredException("NXM URL expired");
        } else if (br.getHttpConnection().getResponseCode() == 401) {
            /* {"message":"Please provide a valid API Key"} */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            /*
             * According to API documentation, this may happen if we try to download a file via API with a free account (downloads are only
             * possible via website!)
             */
            /*
             * {"code":403,
             * "message":"You don't have permission to get download links from the API without visting nexusmods.com - this is for premium users only."
             * }
             */
            throw new AccountRequiredException();
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            /* {"error":"File ID '12345' not found"} */
            /* {"code":404,"message":"No Game Found: xskyrimspecialedition"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 429) {
            /* {"msg":"You have fired too many requests. Please wait for some time."} */
            /* TODO: Maybe check which limit ends first (daily / hourly) to display an even more precise waittime! */
            String reset_date = br.getRequest().getResponseHeader("X-RL-Hourly-Reset").toString();
            if (reset_date != null) {
                /* Try to find the exact waittime */
                reset_date = reset_date.substring(0, reset_date.lastIndexOf(":")) + "00";
                final long reset_timestamp = TimeFormatter.getMilliSeconds(reset_date, "yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
                final long waittime_until_reset = reset_timestamp - System.currentTimeMillis();
                if (waittime_until_reset > 0) {
                    /* Wait exact waittime + 5 extra seconds */
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "API limit has been reached", waittime_until_reset + 5000l);
                }
            }
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "API limit has been reached", 30 * 60 * 1000l);
        }
    }

    public static String getApikey(final Account account) {
        if (account == null) {
            return null;
        }
        if (isAPIOnlyMode()) {
            return account.getPass();
        } else {
            return account.getStringProperty("apikey");
        }
    }

    private void saveApikey(final Account account, final String apikey) {
        if (account == null) {
            return;
        }
        account.setProperty("apikey", apikey);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /*
         * TODO: Consider saving- and re-using direct downloadurls. Consider that premium users do not have any traffic limits so re-using
         * generated downloadurls does not bring any huge benefits. Also when re-using generated downloadlinks consider that they do have
         * different download mirrors/location and these are currently randomly selected on downloadstart!
         */
        if (getApikey(account) != null && linkIsAPICompatible(link)) {
            prepBrAPI(br, account);
            /* We do not have to perform an extra onlinecheck - if the file is offline, the download request will return 404. */
            // requestFileInformationAPI(link);
            final String game_domain_name = link.getStringProperty("game_domain_name", null);
            final String mod_id = link.getStringProperty("mod_id", null);
            final String file_id = new Regex(link.getPluginPatternMatcher(), "\\?id=(\\d+)").getMatch(0);
            if (file_id == null || mod_id == null || game_domain_name == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String action = API_BASE + String.format("/games/%s/mods/%s/files/%s/download_link.json", game_domain_name, mod_id, file_id);
            if (account.getType() == AccountType.FREE) {
                final String dl_key = link.getStringProperty("dl_key", null);
                final String dl_expire = link.getStringProperty("dl_expire", null);
                if (dl_key == null || dl_expire == null) {
                    logger.info("Only premium account download is possible as information for free account download is missing");
                    throw new AccountRequiredException();
                }
                action += "?key=" + dl_key + "&expires=" + dl_expire;
            }
            getPage(action);
            handleErrorsAPI(br);
            LinkedHashMap<String, Object> entries = null;
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            if (ressourcelist == null || ressourcelist.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unable to find downloadlink");
            }
            /*
             * First element = User preferred mirror. Users can set their preferred mirror here: https://www.nexusmods.com/users/myaccount
             * --> Premium membership preferences
             */
            entries = (LinkedHashMap<String, Object>) ressourcelist.get(0);
            final String mirrorName = (String) entries.get("name");
            this.dllink = (String) entries.get("URI");
            if (StringUtils.isEmpty(this.dllink)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unable to find downloadlink for mirror: " + mirrorName);
            }
            logger.info("Selected random mirror: " + mirrorName);
        } else {
            /* Website handling. Important! Login before requestFileInformation! */
            loginWebsite(account);
            requestFileInformation(link);
        }
        /* Free- and premium download handling is the same. */
        doFree(link, account, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS);
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        if (isAPIOnlyMode()) {
            /* API login */
            return new NexusmodsAccountFactory(callback);
        } else {
            /* Website login */
            return super.getAccountFactory(callback);
        }
    }

    public static class NexusmodsAccountFactory extends MigPanel implements AccountBuilderInterface {
        /**
        *
        */
        private static final long serialVersionUID = 1L;
        private final String      PINHELP          = "Enter your API Key";

        private String getPassword() {
            if (this.pass == null) {
                return null;
            }
            if (EMPTYPW.equals(new String(this.pass.getPassword()))) {
                return null;
            }
            return new String(this.pass.getPassword());
        }

        public boolean updateAccount(Account input, Account output) {
            boolean changed = false;
            if (!StringUtils.equals(input.getUser(), output.getUser())) {
                output.setUser(input.getUser());
                changed = true;
            }
            if (!StringUtils.equals(input.getPass(), output.getPass())) {
                output.setPass(input.getPass());
                changed = true;
            }
            return changed;
        }

        private final ExtPasswordField pass;
        private static String          EMPTYPW = " ";

        public NexusmodsAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            add(new JLabel("Click here to find your API Key:"));
            add(new JLink("https://www.nexusmods.com/users/myaccount?tab=api%20access"));
            add(new JLabel("API Key [premium accounts only]:"));
            add(this.pass = new ExtPasswordField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }
            }, "");
            pass.setHelpText(PINHELP);
        }

        @Override
        public JComponent getComponent() {
            return this;
        }

        @Override
        public void setAccount(Account defaultAccount) {
            if (defaultAccount != null) {
                // name.setText(defaultAccount.getUser());
                pass.setText(defaultAccount.getPass());
            }
        }

        @Override
        public boolean validateInputs() {
            // final String userName = getUsername();
            // if (userName == null || !userName.trim().matches("^\\d{9}$")) {
            // idLabel.setForeground(Color.RED);
            // return false;
            // }
            // idLabel.setForeground(Color.BLACK);
            return getPassword() != null;
        }

        @Override
        public Account getAccount() {
            return new Account(null, getPassword());
        }
    }

    private static boolean isAPIOnlyMode() {
        // final NexusmodsConfigInterface cfg = PluginJsonConfig.get(NexusmodsCom.NexusmodsConfigInterface.class);
        // return !cfg.isEnableWebsiteMode();
        /* 2020-01-15: Website login is broken, downloads are only possible via free account */
        return true;
    }

    public void getPage(Browser ibr, String page) throws Exception {
        super.getPage(ibr, page);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            /* Downloads without account are not possible */
            return false;
        } else if (account.getType() != AccountType.PREMIUM && !isSpecialDownloadmanagerURL(link)) {
            /* Free account users can only download special URLs which contain authorization information. */
            return false;
        } else if (!linkIsAPICompatible(link)) {
            /* E.g. older URLs or in case important properties got lost somehow, a download (via API) is not possible at all! */
            return false;
        }
        return true;
    }

    // @Override
    // public Class<? extends PluginConfigInterface> getConfigInterface() {
    // return NexusmodsConfigInterface.class;
    // }
    //
    // public static interface NexusmodsConfigInterface extends PluginConfigInterface {
    // public static class TRANSLATION {
    // public String getEnableWebsiteMode_label() {
    // return "Enable website mode (this way you are able to use free accounts for downloading)? Do NOT enable this if you own a premium
    // account!";
    // }
    // }
    //
    // public static final TRANSLATION TRANSLATION = new TRANSLATION();
    //
    // @AboutConfig
    // @DefaultBooleanValue(false)
    // @Order(5)
    // boolean isEnableWebsiteMode();
    //
    // void setEnableWebsiteMode(boolean b);
    // }
    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}