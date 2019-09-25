package jd.plugins.hoster;

import java.io.IOException;
import java.net.URL;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.plugins.components.config.DropBoxConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dropbox.com" }, urls = { "https?://(?:www\\.)?(dropbox\\.com/sc/[^/]+/[^/]+|dl\\-web\\.dropbox\\.com/get/.*?w=[0-9a-f]+|([\\w]+:[\\w]+@)?api\\-content\\.dropbox\\.com/\\d+/files/.+|dropboxdecrypted\\.com/.+)" })
public class DropboxCom extends PluginForHost {
    public DropboxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.dropbox.com/pricing");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("dropboxdecrypted.com/", "dropbox.com/").replaceAll("#", "%23").replaceAll("\\?dl=\\d", ""));
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return DropBoxConfig.class;
    }

    public static Browser prepBrWebsite(final Browser br) {
        br.setCookie("https://dropbox.com", "locale", "en");
        br.setAllowedResponseCodes(new int[] { 429, 460, 509 });
        return br;
    }

    public static Browser prepBrAPI(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setAllowedResponseCodes(new int[] { 400, 409 });
        br.setFollowRedirects(true);
        /* Clear eventually existing old Authorization values */
        br.getHeaders().remove("Authorization");
        return br;
    }

    public static boolean useAPI() {
        return PluginJsonConfig.get(DropBoxConfig.class).isUseAPI() || HARDCODED_ENFORCE_API;
    }

    public static final String              TYPE_S                                           = "https?://[^/]+/(s/.+)";
    public static final String              TYPE_SH                                          = "https?://[^/]+/sh/[^/]+/[^/]+/[^/]+";
    public static final String              TYPE_SC                                          = "https?://[^/]+/sc/.+";
    private static HashMap<String, Cookies> accountMap                                       = new HashMap<String, Cookies>();
    private String                          url                                              = null;
    private boolean                         temp_unavailable_file_generates_too_much_traffic = false;
    public static final String              API_BASE                                         = "https://api.dropboxapi.com/2";
    private static final String             API_BASE_CONTENT                                 = "https://content.dropboxapi.com/2";
    /** 2019-09-25: Website login is broken - enforce API usage for all users! */
    private static final boolean            HARDCODED_ENFORCE_API                            = true;
    public static String                    PROPERTY_MAINPAGE                                = "mainlink";
    public static String                    PROPERTY_INTERNAL_PATH                           = "serverside_path_to_file_relative";
    public static String                    PROPERTY_IS_PASSWORD_PROTECTED                   = "is_password_protected";
    public static String                    PROPERTY_PASSWORD_COOKIE                         = "password_cookie";
    public static String                    PROPERTY_IS_SINGLE_FILE                          = "is_single_file";
    public static String                    PROPERTY_ACCOUNT_ACCESS_TOKEN                    = "access_token";
    public static String                    PROPERTY_ACCOUNT_LAST_AUTH_VALIDATION            = "last_auth_validation";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        url = null;
        temp_unavailable_file_generates_too_much_traffic = false;
        br = new Browser();
        /*
         * Setting this cookie may save some http requests as the website will not ask us to enter the password again if it has been entered
         * successfully before!
         */
        final String password_cookie = link.getStringProperty(PROPERTY_PASSWORD_COOKIE, null);
        if (password_cookie != null) {
            br.setCookie(this.getHost(), "sm_auth", password_cookie);
        }
        URLConnectionAdapter con = null;
        prepBrWebsite(br);
        /**
         * 2019-09-24: Consider updating to the new/current website method: https://www.dropbox.com/sharing/fetch_user_content_link. See
         * also handling for 'TYPE_SC' linktype! </br>
         * This might not be necessary for any other linktype as the old '?dl=1' method is working just fine!
         */
        if (link.getPluginPatternMatcher().matches(TYPE_SC)) {
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            url = URLHelper.parseLocation(new URL(this.getRootFolderURL(link, link.getPluginPatternMatcher())), "?dl=1");
            for (int i = 0; i < 2; i++) {
                try {
                    br.setFollowRedirects(true);
                    con = i == 0 ? br.openHeadConnection(url) : br.openGetConnection(url);
                    if (con.getResponseCode() == 400) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else if (con.getResponseCode() == 403) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else if (con.getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else if (con.getResponseCode() == 460) {
                        /* Restricted Content: This file is no longer available. For additional information contact Dropbox Support. */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else if (con.getResponseCode() == 509) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
                    }
                    if (!con.getContentType().contains("html")) {
                        link.setProperty("directlink", con.getURL().toString());
                        link.setVerifiedFileSize(con.getLongContentLength());
                        String name = getFileNameFromHeader(con);
                        if (!StringUtils.isEmpty(name)) {
                            name = Encoding.htmlDecode(name).trim();
                            link.setFinalFileName(name);
                        }
                        return AvailableStatus.TRUE;
                    }
                    if (br.getURL().contains("/speedbump/")) {
                        url = br.getURL().replace("/speedbump/", "/speedbump/dl/");
                    }
                    if (isPasswordProtectedWebsite(br)) {
                        /* Password handling is located in download handling */
                        link.setProperty(PROPERTY_IS_PASSWORD_PROTECTED, true);
                        return AvailableStatus.TRUE;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
            url = link.getPluginPatternMatcher();
            /* Either offline or password protected */
            br.getPage(url);
            if (this.br.getHttpConnection().getResponseCode() == 429) {
                /* 2017-01-30 */
                temp_unavailable_file_generates_too_much_traffic = true;
                return AvailableStatus.TRUE;
            }
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(>Error \\(404\\)<|>Dropbox \\- 404<|>We can\\'t find the page you\\'re looking for|>The file you're looking for has been)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.br.containsHTML("images/sharing/error_")) {
                /* Offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.br.containsHTML("/images/precaution")) {
                /* A previously public shared url is now private (== offline) */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        String json_source = jd.plugins.decrypter.DropBoxCom.getSharedJsonSource(br);
        final boolean isShared;
        if (json_source != null) {
            isShared = true;
        } else {
            isShared = false;
            json_source = jd.plugins.decrypter.DropBoxCom.getJsonSource(this.br);
        }
        if (json_source == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json_source);
        if (link.getPluginPatternMatcher().matches(TYPE_SC)) {
            /** TODO: Add support for single gallery objects and fix json grabbing */
            try {
                entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "props/collectionSharedLinkInfo");
                final String filename = (String) entries.get("displayName");
                if (!StringUtils.isEmpty(filename)) {
                    link.setFinalFileName("Gallery " + filename + ".zip");
                }
            } catch (final Throwable e) {
                /* Fallback */
                final String urlpart = new Regex(link.getPluginPatternMatcher(), "/sc/(.+)").getMatch(0);
                link.setFinalFileName(urlpart + ".zip");
            }
        } else {
            entries = (Map<String, Object>) jd.plugins.decrypter.DropBoxCom.getFilesList(entries, isShared).get(0);
            final String filename = (String) entries.get("filename");
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("bytes"), 0);
            if (filesize > 0) {
                link.setDownloadSize(filesize);
            }
            if (!StringUtils.isEmpty(filename)) {
                link.setName(filename);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (useAPI()) {
            return fetchAccountInfoAPI(account);
        } else {
            return fetchAccountInfoWebsite(account);
        }
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        br.setDebug(true);
        loginWebsite(account, true);
        /* 2019-09-19: Treat all accounts as FREE accounts */
        account.setType(AccountType.FREE);
        ai.setStatus("Registered (free) user");
        ai.setUnlimitedTraffic();
        return ai;
    }

    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.loginAPI(this.br, account, true);
        accessAPIAccountInfo(this.br);
        final boolean account_disabled = "true".equals(PluginJSonUtils.getJson(br, "disabled"));
        if (account_disabled) {
            /* 2019-09-19: No idea what this means - probably banned accounts?! */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Your account has been disabled/banned by Dropbox", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        /* Make sure we do not store the users' real logindata as he will likely enter them into our login-mask! */
        account.setUser(null);
        account.setPass(null);
        try {
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final String given_name = (String) JavaScriptEngineFactory.walkJson(entries, "name/given_name");
            final String surname = (String) JavaScriptEngineFactory.walkJson(entries, "name/surname");
            if (!StringUtils.isEmpty(given_name)) {
                String jd_account_manager_username = given_name;
                if (!StringUtils.isEmpty(surname)) {
                    jd_account_manager_username += " " + surname.substring(0, 1) + ".";
                }
                /* Save this as username - no one can start attacks on stored logindata based on this! */
                account.setUser(jd_account_manager_username);
            }
            final String account_type = (String) JavaScriptEngineFactory.walkJson(entries, "account_type/.tag");
            if (!StringUtils.isEmpty(account_type)) {
                ai.setStatus("Account type: " + account_type);
            } else {
                /* Fallback */
                ai.setStatus("Registered (free) user");
            }
            /* 2019-09-19: Treat all accounts as FREE accounts - the Account-Type does not change the download procedure! */
            account.setType(AccountType.FREE);
        } catch (final Throwable e) {
            /* 2019-09-19: On failure: Treat all accounts as FREE accounts */
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) user");
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    protected String generateNonce() {
        return Long.toString(new Random().nextLong());
    }

    protected String generateTimestamp() {
        return Long.toString(System.currentTimeMillis() / 1000L);
    }

    @Override
    public String getAGBLink() {
        return "https://www.dropbox.com/terms";
    }

    // TODO: Move into Utilities (It's here for a hack)
    // public class OAuth {
    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        String passCode = link.getDownloadPassword();
        String t1 = new Regex(link.getPluginPatternMatcher(), "://(.*?):.*?@").getMatch(0);
        String t2 = new Regex(link.getPluginPatternMatcher(), "://.*?:(.*?)@").getMatch(0);
        if (t1 != null && t2 != null) {
            handlePremium(link, null);
            return;
        }
        requestFileInformation(link);
        if (temp_unavailable_file_generates_too_much_traffic) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error 429: 'This account's links are generating too much traffic and have been temporarily disabled!'", 60 * 60 * 1000l);
        }
        /*
         * Important! Files may be password protected but if PROPERTY_PASSWORD_COOKIE has been set before we will not have to send the
         * password again! This is why it is crucial to also check for isPasswordProtectedWebsite here!!
         */
        final boolean resume_supported;
        if (link.getPluginPatternMatcher().matches(TYPE_SC)) {
            /* Complete image gallery */
            resume_supported = false;
            final String cookie_t = br.getCookie(this.getHost(), "t");
            if (cookie_t == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Form dlform = new Form();
            dlform.setMethod(MethodType.POST);
            dlform.setAction("https://www." + this.getHost() + "/sharing/fetch_user_content_link");
            dlform.put("is_xhr", "true");
            dlform.put("t", cookie_t);
            dlform.put("url", link.getPluginPatternMatcher());
            br.getHeaders().put("x-requested-with", "XMLHttpRequest");
            /*
             * All this does is basically change our main URL: https://www.dropbox.com/sc/<value1>/<value2> -->
             * https://dl-web.dropbox.com/zip_collection/<value1>/<value2>
             */
            br.submitForm(dlform);
            url = br.toString();
            if (!url.startsWith("http")) {
                logger.warning("Failed to find final downloadurl");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else if (this.isPasswordProtected(link) && isPasswordProtectedWebsite(br)) {
            /* Single file */
            resume_supported = true;
            final String content_id = new Regex(br.getURL(), "content_id=([^\\&]+)").getMatch(0);
            if (content_id == null) {
                logger.warning("Failed to find content_id");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            Form pwform = this.br.getFormbyProperty("id", "password-form");
            if (pwform == null) {
                /* 2019-05-22: New */
                pwform = this.br.getFormbyAction("/ajax_verify_code");
            }
            if (pwform == null) {
                pwform = new Form();
                pwform.setMethod(MethodType.POST);
            }
            pwform.setAction("https://www.dropbox.com/sm/auth");
            if (passCode == null) {
                passCode = getUserInput("Password?", link);
            }
            final String cookie_t = br.getCookie(getHost(), "t");
            if (cookie_t != null) {
                pwform.put("t", cookie_t);
            }
            pwform.put("password", passCode);
            pwform.put("is_xhr", "true");
            pwform.put("content_id", content_id);
            br.getHeaders().put("x-requested-with", "XMLHttpRequest");
            this.br.submitForm(pwform);
            /* 2019-09-24: E.g. positive response: {"status": "authed"} */
            final String status = PluginJSonUtils.getJson(br, "status");
            if ("error".equalsIgnoreCase(status)) {
                link.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            final String password_cookie = br.getCookie(br.getHost(), "sm_auth");
            if (!StringUtils.isEmpty(password_cookie)) {
                /* This may save us some http requests next time! */
                link.setProperty(PROPERTY_PASSWORD_COOKIE, password_cookie);
            }
            link.setDownloadPassword(passCode);
            this.br.getPage(link.getPluginPatternMatcher());
            url = br.getURL("?dl=1").toString();
        } else {
            resume_supported = true;
            if (url == null) {
                url = URLHelper.parseLocation(new URL(link.getPluginPatternMatcher()), "&dl=1");
            }
        }
        handleDownload(link, url, resume_supported);
    }

    private void handleDownload(final DownloadLink link, final String dllink, final boolean resume) throws Exception {
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find final downloadurl");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            final URLConnectionAdapter con = dl.getConnection();
            if (con.getResponseCode() == 401) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            br.followConnection();
            logger.warning("Final downloadlink lead to HTML code");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        String dlURL = link.getPluginPatternMatcher();
        boolean resume = true;
        if (dlURL.matches(".*api-content.dropbox.com.*")) {
            /** 2019-09-24: TODO: Check when this happens, find testlinks for this case! */
            /* api downloads via tokens */
            resume = false;
            try {
                /* Decrypt oauth token and secret */
                byte[] crypted_oauth_consumer_key = org.appwork.utils.encoding.Base64.decode("1lbl8Ts5lNJPxMOBzazwlg==");
                byte[] crypted_oauth_consumer_secret = org.appwork.utils.encoding.Base64.decode("cqqyvFx1IVKNPennzVKUnw==");
                byte[] iv = new byte[] { (byte) 0xF0, 0x0B, (byte) 0xAA, (byte) 0x69, 0x42, (byte) 0xF0, 0x0B, (byte) 0xAA };
                byte[] secretKey = (new Regex(dlURL, "passphrase=([^&]+)").getMatch(0).substring(0, 8)).getBytes("UTF-8");
                SecretKey key = new SecretKeySpec(secretKey, "DES");
                AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
                Cipher dcipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
                dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
                String oauth_consumer_key = new String(dcipher.doFinal(crypted_oauth_consumer_key), "UTF-8");
                String oauth_token_secret = new String(dcipher.doFinal(crypted_oauth_consumer_secret), "UTF-8");
                /* remove existing tokens from url */
                dlURL = dlURL.replaceFirst("://[\\w:]+@", "://");
                /* remove passphrase from url */
                dlURL = dlURL.replaceFirst("[\\?&]passphrase=[^&]+", "");
                String t1 = new Regex(link.getPluginPatternMatcher(), "://(.*?):.*?@").getMatch(0);
                String t2 = new Regex(link.getPluginPatternMatcher(), "://.*?:(.*?)@").getMatch(0);
                if (t1 == null) {
                    t1 = account.getUser();
                }
                if (t2 == null) {
                    t2 = account.getPass();
                }
                dlURL = signOAuthURL(dlURL, oauth_consumer_key, oauth_token_secret, t1, t2);
            } catch (PluginException e) {
                throw e;
            } catch (Exception e) {
                logger.log(e);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else if (!useAPI()) {
            dlURL = getDllinkAccountWebsite(account, link);
        } else {
            /* API download - uses different handling + errorhandling! */
            handleDownloadAccountAPI(account, link);
            return;
        }
        handleDownload(link, dlURL, resume);
    }

    private String getDllinkAccountWebsite(final Account account, final DownloadLink link) throws Exception {
        String dlURL = link.getPluginPatternMatcher();
        /* website downloads */
        loginWebsite(account, false);
        if (!dlURL.contains("?dl=1") && !dlURL.contains("&dl=1")) {
            dlURL = dlURL + "&dl=1";
        }
        return dlURL;
    }

    /** API download (account required) */
    private void handleDownloadAccountAPI(final Account account, final DownloadLink link) throws Exception {
        /*
         * 2019-09-25: Do not check login at all. If it is invalid, errorhandling will detect that after download attempt and display
         * account as invalid!
         */
        prepBrAPI(this.br);
        setAPILoginHeaders(this.br, account);
        // this.loginAPI(this.br, account, false);
        final String contentURL = getRootFolderURL(link, link.getPluginPatternMatcher());
        String serverside_path_to_file_relative = link.getStringProperty(PROPERTY_INTERNAL_PATH, null);
        if (serverside_path_to_file_relative != null && !this.isSingleFile(link)) {
            /* Fix json */
            serverside_path_to_file_relative = "\"" + serverside_path_to_file_relative + "\"";
        } else {
            /* We expect this to be a single file --> 'path' value must be 'null'! */
            serverside_path_to_file_relative = "null";
        }
        String download_password = link.getDownloadPassword();
        if (download_password == null) {
            /* Do not send 'null' value to API! */
            download_password = "";
        }
        /**
         * https://www.dropbox.com/developers/documentation/http/documentation#sharing-get_shared_link_file </br>
         * There is a serverside bug which prevents us from downloading password protected content via API. This issue has been submitted to
         * their support 2019-09-23 and we're waiting for a response.
         */
        final String jsonHeader = "{ \"url\": \"" + contentURL + "\", \"path\":" + serverside_path_to_file_relative + ", \"link_password\":\"" + download_password + "\"  }";
        br.getHeaders().put("Dropbox-API-Arg", jsonHeader);
        br.getHeaders().put("Content-Type", "text/plain;charset=UTF-8");
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, API_BASE_CONTENT + "/sharing/get_shared_link_file", "", true, 1);
        final String contenttype = dl.getConnection().getContentType();
        if (contenttype.contains("html") || contenttype.contains("application/json")) {
            br.followConnection();
            final String error_summary = getErrorSummaryField(this.br);
            if (error_summary.contains("shared_link_access_denied")) {
                /*
                 * Request password and check it on the next retry. This is a rare case because at least if the user adds URLÖs via crawler
                 * + API, he will already have entered the correct password by now!
                 */
                logger.info("URL is either password protected or user is lacking the rights to view it");
                link.setProperty(PROPERTY_IS_PASSWORD_PROTECTED, true);
                final boolean enable_password_protected_workaround = true;
                if (enable_password_protected_workaround) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Rights missing or password protected", 1 * 60 * 60 * 1000l);
                } else {
                    download_password = getUserInput("Password?", link);
                    link.setDownloadPassword(download_password);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
                }
            }
            handleAPIErrors();
            /** TODO: Improve errorhandling */
            final URLConnectionAdapter con = dl.getConnection();
            handleAPIResponseCodes(con.getResponseCode());
            br.followConnection();
            logger.warning("Final downloadlink lead to HTML code");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleAPIErrors() throws PluginException {
        final String error_summary = getErrorSummaryField(this.br);
        if (!StringUtils.isEmpty(error_summary)) {
            if (error_summary.contains("shared_link_not_found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                logger.warning("Unknown API error occured");
            }
        }
    }

    private void handleAPIResponseCodes(final long responsecode) throws PluginException {
        if (responsecode == 400) {
            /*
             * This should never happen but could happen when we e.g. try to download content which does not exist anymore or when we try to
             * download URLs which have been added via website and not API - sometimes we then use bad parameters to request
             * fileinfo/downloads! This will also happen if we e.g. have an invalid Authorization Header but it happened only during testing
             * with false value on purpose! With the Authorization-Header values we get via API this should never happen!
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "API error 400");
        } else if (responsecode == 401) {
            /* HTTP/1.1 401 invalid_access_token/ */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (responsecode == 409) {
            /* 2019-09-20: E.g. "error_summary": "shared_link_not_found/" */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private boolean itemHasBeenCrawledViaAPI(final DownloadLink link) {
        return getRootFolderURL(link, null) != null;
    }

    /** Returns either the URL to the root folder of our current file or the link that goes to that particular file. */
    private String getRootFolderURL(final DownloadLink link, final String fallback) {
        String contentURL = link.getStringProperty(PROPERTY_MAINPAGE, null);
        if (contentURL == null) {
            /* Fallback for old URLs or such, added without API */
            contentURL = fallback;
        }
        return contentURL;
    }

    /** Only use this in crawler!! In host-plugins, use isSingleFile(final DownloadLink link)!! */
    public static boolean isSingleFile(final String url) {
        if (url.matches(TYPE_S)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isSingleFile(final DownloadLink link) {
        if (isSingleFile(this.getRootFolderURL(link, link.getPluginPatternMatcher())) || link.getBooleanProperty(PROPERTY_IS_SINGLE_FILE, false)) {
            return true;
        } else {
            return false;
        }
    }

    // /**
    // * Posts json data first without checking login and if that fails, again with ensuring login!
    // */
    // private void postPageRawAndEnsureLogin(final Account account, final String url, final String data) throws Exception {
    // boolean verifiedCookies = this.loginAPI(br, account, false);
    // this.br.postPageRaw(url, data);
    // /** TODO: Add isLoggedIN function and check */
    // if (!verifiedCookies && !this.isLoggedinAPI(this.br)) {
    // logger.info("Retrying with ensured login");
    // verifiedCookies = this.loginAPI(this.br, account, false);
    // this.br.postPageRaw(url, data);
    // }
    // }
    public static String getErrorSummaryField(final Browser br) {
        return PluginJSonUtils.getJson(br, "error_summary");
    }

    /** Returns whether or not a file/folder is password protected. */
    public static boolean isPasswordProtectedWebsite(final Browser br) {
        final String currentURL = br.getURL();
        final String redirectURL = br.getRedirectLocation();
        final String pwProtectedIndicator = "/sm/password";
        boolean passwordProtected = false;
        if (currentURL != null && currentURL.contains(pwProtectedIndicator) || redirectURL != null && redirectURL.contains(pwProtectedIndicator)) {
            passwordProtected = true;
        }
        return passwordProtected;
    }

    /** 2019-09-20: Avoid using this. It is outdated - does not support 2FA login and is broken for a long time already! */
    @Deprecated
    private void loginWebsite(final Account account, boolean refresh) throws Exception {
        boolean ok = false;
        synchronized (account) {
            setBrowserExclusive();
            br.setFollowRedirects(true);
            this.br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0");
            // this.br.setCookie("dropbox.com", "puc", "");
            this.br.setCookie("dropbox.com", "goregular", "");
            if (refresh == false) {
                Cookies accCookies = accountMap.get(account.getUser());
                if (accCookies != null) {
                    br.getCookies("https://www.dropbox.com").add(accCookies);
                    return;
                }
            }
            try {
                br.getPage("https://www.dropbox.com/login");
                final String lang = System.getProperty("user.language");
                String t = br.getRegex("type=\"hidden\" name=\"t\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (t == null) {
                    t = this.br.getCookie("dropbox.com", "t");
                }
                if (t == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("Accept", "text/plain, */*; q=0.01");
                br.getHeaders().put("Accept-Language", "en-US;q=0.7,en;q=0.3");
                br.postPage("/needs_captcha", "is_xhr=true&t=" + t + "&email=" + Encoding.urlEncode(account.getUser()));
                br.postPage("/sso_state", "is_xhr=true&t=" + t + "&email=" + Encoding.urlEncode(account.getUser()));
                String postdata = "is_xhr=true&t=" + t + "&cont=%2F&require_role=&signup_data=&third_party_auth_experiment=CONTROL&signup_tag=&login_email=" + Encoding.urlEncode(account.getUser()) + "&login_password=" + Encoding.urlEncode(account.getPass()) + "&remember_me=True";
                postdata += "&login_sd=";
                postdata += "";
                br.postPage("/ajax_login", postdata);
                if (br.getCookie("https://www.dropbox.com", "jar") == null || !"OK".equals(PluginJSonUtils.getJsonValue(br, "status"))) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                ok = true;
            } finally {
                if (ok) {
                    accountMap.put(account.getUser(), br.getCookies("https://www.dropbox.com"));
                } else {
                    accountMap.remove(account.getUser());
                }
            }
        }
    }

    /**
     * API login: https://www.dropbox.com/developers/documentation/http/documentation#oa2-authorize and
     * https://www.dropbox.com/developers/reference/oauth-guide
     */
    public boolean loginAPI(final Browser br, final Account account, boolean validateAuthorization) throws Exception {
        synchronized (account) {
            prepBrAPI(br);
            boolean loggedIN = false;
            if (setAPILoginHeaders(br, account)) {
                final long last_auth_validation = account.getLongProperty(PROPERTY_ACCOUNT_LAST_AUTH_VALIDATION, 0);
                if (!validateAuthorization && System.currentTimeMillis() - last_auth_validation <= 300000l) {
                    return false;
                }
                accessAPIAccountInfo(br);
                loggedIN = isLoggedinAPI(br);
            }
            if (!loggedIN) {
                /* Important: This is required as it will also clear any previous "Authorization" headers! */
                prepBrAPI(br);
                logger.info("Performing full login");
                /* Perform full login */
                final String user_auth_url = "https://www." + account.getHoster() + "/oauth2/authorize?client_id=" + getAPIClientID() + "&response_type=code&force_reapprove=false";
                showOauthLoginInformation(user_auth_url);
                final DownloadLink dl_dummy;
                if (this.getDownloadLink() != null) {
                    dl_dummy = this.getDownloadLink();
                } else {
                    dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                }
                final String user_code = getUserInput("Authorization code?", dl_dummy);
                if (StringUtils.isEmpty(user_code)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Authorization code has not been entered", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final Form loginform = new Form();
                loginform.setMethod(MethodType.POST);
                loginform.setAction("https://api.dropboxapi.com/oauth2/token");
                loginform.put("code", user_code);
                loginform.put("grant_type", "authorization_code");
                loginform.put("client_id", getAPIClientID());
                loginform.put("client_secret", getAPISecret());
                /*
                 * 2019-09-19: redirect_uri field is not required as we're not yet able to use it, thus we're using 'response_type=code'
                 * above.
                 */
                // loginform.put("redirect_uri", "TODO");
                br.submitForm(loginform);
                String access_token = PluginJSonUtils.getJson(br, "access_token");
                /* This is required to obtain account-information later on! */
                // final String account_id = PluginJSonUtils.getJson(br, "account_id");
                /* We do not need this 2nd user-id! */
                // final String uid = PluginJSonUtils.getJson(br, "uid");
                if (StringUtils.isEmpty(access_token)) {
                    /* 2019-09-19: We do not care about the fail-reason - failure = wrong logindata! */
                    /* E.g. expired token: {"error_description": "code has expired (within the last hour)", "error": "invalid_grant"} */
                    final String error_description = PluginJSonUtils.getJson(br, "error_description");
                    if (!StringUtils.isEmpty(error_description)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid login: " + error_description, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /*
                 * 2019-09-19: This token should last until the user revokes access! It can even survive password changes if the user wants
                 * it to! It will even last when a user switches from normal login to 2-factor-authorization! Users can open up unlimited(?)
                 * logins via one Application. If they revoke access for that application, all of them will be gone:
                 * https://www.dropbox.com/account/security
                 */
                account.setProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN, access_token);
                /* We do not need to store this value */
                // if (!StringUtils.isEmpty(account_id)) {
                // account.setProperty("account_id", account_id);
                // }
                setAPILoginHeaders(br, account);
            }
            account.setProperty(PROPERTY_ACCOUNT_LAST_AUTH_VALIDATION, System.currentTimeMillis());
            return true;
        }
    }

    private boolean isLoggedinAPI(final Browser br) {
        final String error_summary = getErrorSummaryField(br);
        return br.getHttpConnection().getResponseCode() == 200 && StringUtils.isEmpty(error_summary);
    }

    private void accessAPIAccountInfo(final Browser br) throws IOException {
        /** https://www.dropbox.com/developers/documentation/http/documentation#users-get_current_account */
        if (br.getURL() == null || !br.getURL().contains("/users/get_current_account")) {
            /* 'null' is required to send otherwise we'll get an error-response!! */
            br.postPageRaw(API_BASE + "/users/get_current_account", "null");
        }
    }

    /**
     * Sets Authorization header. Because once generated, an oauth token is valid 'forever' until user revokes access to application, it
     * must not necessarily be re-validated!
     *
     * @return true = api_token found and set </br>
     *         false = no api_token found
     */
    public static boolean setAPILoginHeaders(final Browser br, final Account account) {
        if (account == null || br == null) {
            return false;
        }
        final String access_token = getAPIToken(account);
        if (access_token == null) {
            return false;
        }
        br.getHeaders().put("Authorization", "Bearer " + access_token);
        br.getHeaders().put("Content-Type", "application/json");
        return true;
    }

    // private String getAPIAccountID(final Account account) {
    // return account.getStringProperty("account_id", null);
    // }
    public static String getAPIToken(final Account account) {
        return account.getStringProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN, null);
    }

    private boolean isPasswordProtected(final DownloadLink dl) {
        /* Do NOT check this via existing password as user can set a download-password at any time even for unprotected URLs!! */
        // return dl.getDownloadPassword();
        return dl.getBooleanProperty(PROPERTY_IS_PASSWORD_PROTECTED, false);
    }

    /** Also called App-key and can be found here: https://www.dropbox.com/developers/apps */
    private String getAPIClientID() {
        return "j0mjfuvazxa9ye4";
    }

    /** Can be found here: https://www.dropbox.com/developers/apps */
    private String getAPISecret() {
        /* 2019-09-19: We do not really have good ways to hide such things in the JDownloader project ... */
        return Encoding.Base64Decode("ZGF3eDYzdmVrdDUybmVo");
    }

    private Thread showOauthLoginInformation(final String auth_url) {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Dropbox.com - neue Login-Methode";
                        message += "Hallo liebe(r) Dropbox NutzerIn\r\n";
                        message += "Seit diesem Update hat sich die Login-Methode dieses Anbieters geändert um die Sicherheit zu erhöhen!\r\n";
                        message += "Um deinen Account weiterhin in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Gehe sicher, dass du im Browser in deinem Dropbox Account eingeloggt bist.\r\n";
                        message += "2. Öffne diesen Link im Browser falls das nicht automatisch geschieht:\r\n\t'" + auth_url + "'\t\r\n";
                        message += "3. Gib den Code, der im Browser angezeigt wird hier ein.\r\n";
                        message += "Dein Account sollte nach einigen Sekunden von JDownloader akzeptiert werden.\r\n";
                    } else {
                        title = "Dropbox.com - New login method";
                        message += "Hello dear Dropbox user\r\n";
                        message += "This update has changed the login method of Dropbox in favor of security.\r\n";
                        message += "In order to keep using this service in JDownloader you need to follow these steps:\r\n";
                        message += "1. Make sure that you're logged in your Dropbox account with your default browser.\r\n";
                        message += "2. Open this URL in your browser if it does not happen automatically:\r\n\t'" + auth_url + "'\t\r\n";
                        message += "3. Enter the code you see in your browser here.\r\n";
                        message += "Your account should be accepted in JDownloader within a few seconds.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(30 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(auth_url);
                    }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if ((!useAPI() || account == null) && this.itemHasBeenCrawledViaAPI(link) && !isSingleFile(link)) {
            /* API-item not downloadable via website */
            /*
             * API items have other content-IDs which cannot be accessed via website. This means some items which have been crawled via API
             * can only be downloaded via API, NOT via website!
             */
            return false;
        } else if ((useAPI() && account != null) && (isPasswordProtected(link) || link.getPluginPatternMatcher().matches(TYPE_SC))) {
            /* API cannot download password protected files and image gallerys atm. */
            /**
             * 2019-09-25: Due to an API bug, password protected content is NOT downloadable via API at all! </br>
             * TODO: Remove this once API gets updated and can handle password protected content (bug has been reported to Dropbox support!)
             */
            return false;
        }
        /* All other cases should work fine via API and website! */
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /**
     * Sign an OAuth GET request with HMAC-SHA1 according to OAuth Core spec 1.0
     *
     * @return new url including signature
     * @throws PluginException
     */
    public/* static */String signOAuthURL(String url, String oauth_consumer_key, String oauth_consumer_secret, String oauth_token, String oauth_token_secret) throws PluginException {
        // At first, we remove all OAuth parameters from the url. We add
        // them
        // all manually.
        url = url.replaceAll("[\\?&]oauth_\\w+?=[^&]+", "");
        url += (url.contains("?") ? "&" : "?") + "oauth_consumer_key=" + oauth_consumer_key;
        url += "&oauth_nonce=" + generateNonce();
        url += "&oauth_signature_method=HMAC-SHA1";
        url += "&oauth_timestamp=" + generateTimestamp();
        url += "&oauth_token=" + oauth_token;
        url += "&oauth_version=1.0";
        String signatureBaseString = Encoding.urlEncode(url);
        signatureBaseString = signatureBaseString.replaceFirst("%3F", "&");
        // See OAuth 1.0 spec Appendix A.5.1
        signatureBaseString = "GET&" + signatureBaseString;
        String keyString = oauth_consumer_secret + "&" + oauth_token_secret;
        String signature = "";
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secret = new SecretKeySpec(keyString.getBytes("UTF-8"), "HmacSHA1");
            mac.init(secret);
            byte[] digest = mac.doFinal(signatureBaseString.getBytes("UTF-8"));
            signature = new String(org.appwork.utils.encoding.Base64.encodeToString(digest, false)).trim();
        } catch (Exception e) {
            logger.log(e);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        url += "&oauth_signature=" + Encoding.urlEncode(signature);
        return url;
    }
}