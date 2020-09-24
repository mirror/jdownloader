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

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.UniqueAlltimeID;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;

import jd.PluginWrapper;
import jd.SecondLevelLaunch;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.decrypter.ImgurComGallery;
import jd.utils.locale.JDL;

/**
 * IMPORTANT: Never grab IDs bigger than 7 characters because these are Thumbnails - see API description: https://api.imgur.com/models/image
 * --> New docs 2020-04-27: https://apidocs.imgur.com/?version=latest (scroll down to "Image thumbnails").
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "imgur.com" }, urls = { "https?://imgurdecrypted\\.com/download/([A-Za-z0-9]{7}|[A-Za-z0-9]{5})" })
public class ImgurComHoster extends PluginForHost {
    public ImgurComHoster(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("https://imgur.com/register");
    }

    @Override
    public String getAGBLink() {
        return "https://imgur.com/tos";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("imgurdecrypted.com/", "imgur.com/"));
    }

    private enum TYPE {
        MP4,
        JPGORGIF
    }

    /* User settings */
    private static final String  SETTING_MP4                      = "SETTING_MP4";
    private static final String  SETTING_CLIENT_ID                = "CLIENT_ID";
    private static final String  SETTING_CLIENT_SECRET            = "CLIENT_SECRET";
    private static final String  SETTING_USE_API                  = "SETTING_USE_API";
    private static final String  SETTING_GRAB_SOURCE_URL_VIDEO    = "SETTING_GRAB_SOURCE_URL_VIDEO";
    private static final String  SETTING_CUSTOM_FILENAME          = "SETTING_CUSTOM_FILENAME";
    private static final String  SETTING_CUSTOM_PACKAGENAME       = "SETTING_CUSTOM_PACKAGENAME";
    /* DownloadLink properties */
    public static final String   PROPERTY_DOWNLOADLINK_DIRECT_URL = "directlink";
    /* Constants */
    public static final int      responsecode_website_overloaded  = 502;
    private static final int     MAX_DOWNLOADS                    = -1;
    private static final boolean RESUME                           = true;
    private static final int     MAXCHUNKS                        = 1;
    /* Variables */
    private String               dllink                           = null;
    private boolean              dl_IMPOSSIBLE_APILIMIT_REACHED   = false;
    private boolean              dl_IMPOSSIBLE_SERVER_ISSUE       = false;
    private String               imgUID                           = null;

    /* Documentation see: https://apidocs.imgur.com/ */
    public static String getAPIBase() {
        return "https://api.imgur.com";
    }

    public static String getAPIBaseWithVersion() {
        return getAPIBase() + "/3";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getImgUID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    /**
     * TODO: 1. Maybe add a setting to download albums as .zip (if possible via site). 2. Maybe add a setting to add numbers in front of the
     * filenames (same way imgur does it when you download .zip files of albums).
     */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        imgUID = getImgUID(link);
        dllink = link.getStringProperty(PROPERTY_DOWNLOADLINK_DIRECT_URL, null);
        dl_IMPOSSIBLE_SERVER_ISSUE = false;
        /* Avoid unneccessary requests --> If we have the directlink, filesize and a nice filename, do not access site/API! */
        String filename_formatted = null;
        TYPE type = null;
        if (dllink == null || link.getLongProperty("decryptedfilesize", -1) == -1 || link.getStringProperty("decryptedfinalfilename", null) == null || getFiletype(link) == null) {
            prepBRAPI(this.br);
            boolean apiMode = true;
            if (account == null && false && !this.getPluginConfig().getBooleanProperty(SETTING_USE_API, true)) {
                /* Website, no longer works and requires Javascript/API */
                apiMode = false;
            } else if (account != null) {
                /* API + account */
                this.login(br, account, false);
                getPage(this.br, getAPIBaseWithVersion() + "/image/" + imgUID);
                this.checkErrors(br, link, account);
            } else {
                /* API without account */
                try {
                    br.getHeaders().put("Authorization", getAuthorization());
                    getPage(this.br, getAPIBaseWithVersion() + "/image/" + imgUID);
                    if (this.br.getHttpConnection().getResponseCode() == 429) {
                        apiMode = false;
                    }
                } catch (final PluginException e) {
                    logger.info("Disabled API mode");
                    apiMode = false;
                }
            }
            String apiResponse[] = null;
            if (apiMode) {
                if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Unable to find an image with the id")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (userPrefersMp4()) {
                    type = TYPE.MP4;
                    apiResponse = parseAPIData(type, this.br.toString());
                }
                if (apiResponse == null || Boolean.FALSE.equals(Boolean.valueOf(apiResponse[4])) || apiResponse[3] == null) {
                    type = TYPE.JPGORGIF;
                    apiResponse = parseAPIData(type, this.br.toString());
                }
                if (apiResponse != null && apiResponse[1] == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (apiResponse == null || apiResponse[3] == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = apiResponse[3];
                long size = -1;
                if (apiResponse[1] != null) {
                    size = Long.valueOf(apiResponse[1]);
                    link.setProperty("decryptedfilesize", size);
                    link.setDownloadSize(size);
                } else {
                    link.removeProperty("decryptedfilesize");
                }
                link.setProperty("filetype", apiResponse[0]);
                link.setProperty("decryptedfinalfilename", apiResponse[2]);
                if (this.dllink == null) {
                    dllink = getURLDownload(imgUID);
                }
                filename_formatted = getFormattedFilename(link);
            } else {
                /*
                 * Workaround for API limit reached or in case user disabled API - second way does return 503 response in case API limit is
                 * reached: http://imgur.com/download/ + imgUID. This code should never be reached!
                 */
                try {
                    if (true) {
                        /* 2020-09-24: https://svn.jdownloader.org/issues/88753 */
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Website mode in development");
                    }
                    this.br = prepBRWebsite(this.br);
                    getPage(this.br, "https://" + this.getHost() + "/" + imgUID);
                    if (br.getHttpConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    String api_like_json = br.getRegex("image\\s*:\\s*\\{(.*?)\\}").getMatch(0);
                    if (api_like_json == null) {
                        api_like_json = br.getRegex("bind\\(analytics\\.popAndLoad, analytics, \\{(.*?)\\}").getMatch(0);
                    }
                    /* This would usually mean out of date but we keep it simple in this case */
                    if (api_like_json == null) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    if (userPrefersMp4()) {
                        type = TYPE.MP4;
                        apiResponse = parseAPIData(type, api_like_json);
                    }
                    if (apiResponse == null || Boolean.FALSE.equals(Boolean.valueOf(apiResponse[4]))) {
                        type = TYPE.JPGORGIF;
                        apiResponse = parseAPIData(type, api_like_json);
                    }
                    if (apiResponse == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String fileType = apiResponse[0];
                    link.setProperty("filetype", fileType);
                    link.setProperty("decryptedfinalfilename", apiResponse[2]);
                    long size = -1;
                    if (apiResponse[1] != null) {
                        size = Long.valueOf(apiResponse[1]);
                        link.setProperty("decryptedfilesize", size);
                        link.setDownloadSize(size);
                    } else {
                        link.removeProperty("decryptedfilesize");
                    }
                    filename_formatted = getFormattedFilename(link);
                } catch (final PluginException e) {
                    logger.warning("Website handling failed --> Handling plain download");
                }
                if (this.dllink == null) {
                    dllink = getURLDownload(imgUID);
                }
            }
            link.setProperty(PROPERTY_DOWNLOADLINK_DIRECT_URL, dllink);
        } else {
            filename_formatted = getFormattedFilename(link);
        }
        if (dllink == null) {
            logger.warning("Failed to find final downloadurl");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename_formatted != null) {
            if (isDownload) {
                link.setFinalFileName(filename_formatted);
            } else {
                link.setName(filename_formatted);
            }
        }
        if (!isDownload) {
            /*
             * Only check available link if user is NOT starting the download --> Avoid to access it twice in a small amount of time -->
             * Keep server load down.
             */
            /*
             * 2016-08-10: Additionally check filesize via url if user wants to have a specified format as the filesize from website json or
             * API is usually for the original file!
             */
            URLConnectionAdapter con = null;
            br.setFollowRedirects(true);
            try {
                con = this.br.openHeadConnection(this.dllink);
                if (con.getResponseCode() == responsecode_website_overloaded) {
                    websiteOverloaded();
                } else if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (con.getURL().toString().contains("/removed.png")) {
                    /* 2020-09-24: Redirect to: https://i.imgur.com/removed.png */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (!this.looksLikeDownloadableContent(con)) {
                    /* E.g. HTTP/1.1 503 first byte timeout */
                    dl_IMPOSSIBLE_SERVER_ISSUE = true;
                } else {
                    /* All OK */
                    final long size = con.getCompleteContentLength();
                    if (size > 0) {
                        link.setProperty("decryptedfilesize", size);
                        link.setDownloadSize(size);
                    }
                    if (filename_formatted == null) {
                        /* Hm still no filename given but we need one! */
                        logger.info("No user defined filename given/possible --> Looking for last chance fallback filename");
                        String contentDispositionFilename = Plugin.getFileNameFromDispositionHeader(con);
                        if (contentDispositionFilename != null) {
                            /* Might sometimes be given when "/download" URL is used. */
                            logger.info("Using content-disposition filename");
                            /* 2020-09-24: Host is tagging their filenames --> Remove that */
                            contentDispositionFilename = contentDispositionFilename.replaceAll(" ?- Imgur", "");
                            /* Small workaround for serverside bug: Content-Disposition: attachment; filename=" - Imgur.jpg" */
                            if (contentDispositionFilename.matches("[ ]*?\\.[A-Za-z0-9]+")) {
                                contentDispositionFilename = this.imgUID + contentDispositionFilename;
                            }
                            link.setFinalFileName(contentDispositionFilename);
                        } else {
                            /* Set filename based on mime-type */
                            final String mimeTypeExt = jd.plugins.hoster.DirectHTTP.getExtensionFromMimeType(con.getRequest().getResponseHeader("Content-Type"));
                            if (mimeTypeExt == null) {
                                /* This should never happen */
                                logger.warning("Unable to determine any filename");
                            } else {
                                logger.info("Set filename based on Content-Type header file-extension");
                                link.setFinalFileName(this.imgUID + "." + mimeTypeExt);
                            }
                        }
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, account, true);
        if (dl_IMPOSSIBLE_APILIMIT_REACHED) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Api limit reached", 10 * 60 * 1000l);
        } else if (this.dl_IMPOSSIBLE_SERVER_ISSUE) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            /* Should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Disable chunks as servers are fast and we usually only download small files. */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, RESUME, MAXCHUNKS);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            final int responsecode = dl.getConnection().getResponseCode();
            if (responsecode == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (responsecode == responsecode_website_overloaded) {
                websiteOverloaded();
            } else if (responsecode == 503) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 1 * 60 * 60 * 1000l);
            }
            logger.warning("Finallink leads to HTML code --> Following connection");
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dl.getConnection().getCompleteContentLength() == 0) {
            /* 2020-09-24: Rare case */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Filesize mismatch: File is empty");
        }
        dl.startDownload();
    }

    private String getPage(final Browser br, final String url) throws IOException, PluginException {
        br.getPage(url);
        if (br.getHttpConnection().getResponseCode() == responsecode_website_overloaded) {
            websiteOverloaded();
        }
        return br.toString();
    }

    private static final String PROPERTY_ACCOUNT_initial_password          = "initial_password";
    private static final String PROPERTY_ACCOUNT_access_token              = "access_token";
    private static final String PROPERTY_ACCOUNT_refresh_token             = "refresh_token";
    private static final String PROPERTY_ACCOUNT_valid_until               = "token_valid_until";
    private static final String PROPERTY_ACCOUNT_token_first_use_timestamp = "token_first_use_timestamp";

    /** Checks to see if e.g. user has changed password. */
    private boolean isSamePW(final Account account) {
        final String initialPW = account.getStringProperty(PROPERTY_ACCOUNT_initial_password);
        return StringUtils.equalsIgnoreCase(initialPW, account.getPass());
    }

    private boolean isAuthorizationURL(final String str) {
        try {
            final UrlQuery query = UrlQuery.parse(str);
            final String access_token = query.get("access_token");
            final String expires_in = query.get("expires_in");
            final String refresh_token = query.get("refresh_token");
            final String username = query.get("account_username");
            if (StringUtils.isAllNotEmpty(access_token, expires_in, refresh_token, username)) {
                return true;
            }
        } catch (final Throwable e) {
            /* No logging needed */
            // logger.log(e);
        }
        return false;
    }

    public void login(final Browser brlogin, final Account account, final boolean force) throws Exception {
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            loginOauthTest(brlogin, account, force);
        } else {
            synchronized (account) {
                brlogin.setFollowRedirects(true);
                brlogin.setCookiesExclusive(true);
                prepBRAPI(brlogin);
                /* Correct input so it is parsable via UrlQuery. */
                if (!account.getPass().contains("?")) {
                    account.setPass(account.getPass().replace("/#", "/?"));
                }
                if (!isAuthorizationURL(account.getPass())) {
                    /* Reset this property to e.g. try again right away with new token once set by user e.g. if user changes 'password'. */
                    account.setProperty(PROPERTY_ACCOUNT_access_token, Property.NULL);
                    if (account.getPass().contains("error=")) {
                        /* User has tried authorization but for some reason it failed. */
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    /*
                     * User entered normal username & password but we need something else as password --> Show message on what to do and let
                     * the user try again!
                     */
                    showLoginInformation();
                    /* Display error to tell user to try again and this time, enter URL into PW field. */
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Versuch's nochmal und gib die Autorisierungs-URL in das Passwort Feld ein.\r\nGib NICHT dein Passwort ins Passwort Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Try again and enter your authorization URL in the password field.\r\nDo NOT enter your password into the password field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                final UrlQuery query = UrlQuery.parse(account.getPass());
                /*
                 * Access tokens expire (after ~30 days) according to API docs. Only use the one the user has entered on first attempt e.g.
                 * user has just added this account for the first time! Save it as a property on the account and then use that because once
                 * token gets refreshed it will differ from the token the user initially entered!
                 */
                final String auth_access_token = query.get("access_token");
                final String auth_refresh_token = query.get("refresh_token");
                /* Ignore 'expires_in' and just use existing token as long as possible. This is only used for debugging. */
                final String auth_valid_until = query.get("expires_in");
                final String auth_username = query.get("account_username");
                if (!StringUtils.equals(account.getUser(), auth_username)) {
                    /* Important as we will use Account.getUser() for later requests so it has to be correct! */
                    logger.info("Correcting Account username to API username");
                    account.setUser(auth_username);
                }
                /*
                 * Now set active values - prefer stored values over user-entered as they will change! User-Entered will be used on first
                 * login OR if user changes account password!
                 */
                String active_refresh_token;
                String active_access_token;
                String active_valid_until;
                long token_first_use_timestamp;
                if (this.isSamePW(account)) {
                    /* Login with same password as before. */
                    active_refresh_token = account.getStringProperty(PROPERTY_ACCOUNT_refresh_token, auth_refresh_token);
                    active_access_token = account.getStringProperty(PROPERTY_ACCOUNT_access_token, auth_access_token);
                    active_valid_until = account.getStringProperty(PROPERTY_ACCOUNT_valid_until);
                    token_first_use_timestamp = account.getLongProperty(PROPERTY_ACCOUNT_token_first_use_timestamp, System.currentTimeMillis());
                } else {
                    /* First login with new logindata */
                    active_refresh_token = auth_refresh_token;
                    active_access_token = auth_access_token;
                    active_valid_until = auth_valid_until;
                    token_first_use_timestamp = System.currentTimeMillis();
                }
                boolean loggedIN = false;
                brlogin.getHeaders().put("Authorization", "Bearer " + active_access_token);
                if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 5 * 60 * 1000l) {
                    logger.info("Trust token without check");
                    return;
                }
                /* Check existing access_token */
                /* Request account information and, at the same time, check if authorization is still valid. */
                brlogin.getPage(getAPIBaseWithVersion() + "/account/" + auth_username);
                checkErrors(brlogin, null, account);
                /* TODO: Check which error API will return on expired token. */
                Map<String, Object> entries = JSonStorage.restoreFromString(brlogin.toString(), TypeRef.HASHMAP);
                entries = (Map<String, Object>) entries.get("data");
                loggedIN = entries != null && entries.containsKey("id");
                /*
                 * E.g. 403 with
                 * {"data":{"error":"The access token provided is invalid.","request":"\/3\/account\/<username>","method":"GET"},"success":
                 * false,"status":403}
                 */
                if (!loggedIN) {
                    /* Build new query containing only what we need. */
                    logger.info("Trying to generate new authorization token");
                    final UrlQuery queryLogin = new UrlQuery();
                    /* Refresh token never expires and can be used to generate new authorization token. */
                    queryLogin.add("refresh_token", auth_refresh_token);
                    queryLogin.add("client_id", getClientID());
                    queryLogin.add("client_secret", getClientSecret());
                    queryLogin.add("grant_type", "refresh_token");
                    brlogin.postPage(getAPIBase() + "/oauth2/token", queryLogin);
                    active_access_token = PluginJSonUtils.getJson(brlogin, "access_token");
                    active_refresh_token = PluginJSonUtils.getJson(brlogin, "refresh_token");
                    active_valid_until = PluginJSonUtils.getJson(brlogin, "expires_in");
                    if (StringUtils.isEmpty(active_access_token) || StringUtils.isEmpty(active_refresh_token)) {
                        /* Failure e.g. user revoked API access --> Invalid logindata --> Permanently disable account */
                        /*
                         * E.g.
                         * {"data":{"error":"Invalid refresh token","request":"\/oauth2\/token","method":"POST"},"success":false,"status":
                         * 400}
                         */
                        checkErrors(this.br, null, account);
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    /* Update authorization header */
                    brlogin.getHeaders().put("Authorization", "Bearer " + active_access_token);
                    /* Update token first use timestamp */
                    token_first_use_timestamp = System.currentTimeMillis();
                }
                account.setProperty(PROPERTY_ACCOUNT_access_token, active_access_token);
                account.setProperty(PROPERTY_ACCOUNT_refresh_token, active_refresh_token);
                if (active_valid_until != null && active_valid_until.matches("\\d+")) {
                    account.setProperty(PROPERTY_ACCOUNT_valid_until, Long.parseLong(active_valid_until));
                }
                account.setProperty(PROPERTY_ACCOUNT_token_first_use_timestamp, token_first_use_timestamp);
                account.setProperty(PROPERTY_ACCOUNT_initial_password, account.getPass());
                /* Every account-check will use up one API request and we have limited requests --> Do not check account that frequently. */
                account.setRefreshTimeout(5 * 60 * 1000l);
                /* Save cookies - but only to have the cookie-timestamp */
                account.saveCookies(brlogin.getCookies(this.getHost()), "");
            }
        }
    }

    public static final String getClientID2() {
        return "85b2dae0c74bb4d";
    }

    public static final String getClientSecret2() {
        return "b03ad3c3efe39644aaea039a4ea3b1e677800558";
    }

    private String getAuthURLTest(final String state, final RESPONSE_TYPE responseType) {
        final String responseTypeStr;
        switch (responseType) {
        case CODE:
            responseTypeStr = "code";
            break;
        case PIN:
            responseTypeStr = "pin";
            break;
        default:
            responseTypeStr = "token";
            break;
        }
        final UrlQuery query = new UrlQuery();
        query.add("client_id", Encoding.urlEncode(getClientID2()));
        query.add("response_type", responseTypeStr);
        query.add("state", Encoding.urlEncode(state));
        return getAPIBase() + "/oauth2/authorize?" + query.toString();
    }

    private Thread showLoginInformationTest(final String autURL) {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Imgur.com - Login";
                        message += "Hallo liebe(r) Imgur NutzerIn\r\n";
                        message += "Um deinen Imgur Account in JD verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne diesen Link im Browser falls das nicht automatisch passiert:\r\n\t'" + autURL + "'\t\r\n";
                        message += "2. Autorisiere JD auf der Imgur Webseite.\r\n";
                        message += "3. Du wirst auf die jdownloader.org Webseite weitergeleitet und dein Account sollte nach einigen Sekunden von JD akzeptiert werden.\r\n";
                    } else {
                        title = "Imgur.com - Login";
                        message += "Hello dear Imgur user\r\n";
                        message += "In order to use imgur with JD, you need to follow these steps:\r\n";
                        message += "1. Open the following URL in your browser if it is not opened automatically:\r\n\t'" + autURL + "'\t\r\n";
                        message += "2. Authorize JD on the Imgur website.\r\n";
                        message += "3. You will be forwarded to the jdownloader.org website and your account should be accepted in JDownloader after a few seconds.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(2 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(autURL);
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

    public void loginOauthTest(final Browser brlogin, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            brlogin.setFollowRedirects(true);
            brlogin.setCookiesExclusive(true);
            prepBRAPI(brlogin);
            String auth_access_token = null;
            String auth_refresh_token = null;
            String auth_valid_until = null;
            String auth_username = null;
            if (this.isSamePW(account)) {
                /* Login with same password as before. */
                auth_refresh_token = account.getStringProperty(PROPERTY_ACCOUNT_refresh_token, auth_refresh_token);
                auth_access_token = account.getStringProperty(PROPERTY_ACCOUNT_access_token, auth_access_token);
                auth_valid_until = account.getStringProperty(PROPERTY_ACCOUNT_valid_until);
            }
            boolean loggedIN = false;
            if (auth_access_token != null) {
                brlogin.getHeaders().put("Authorization", "Bearer " + auth_access_token);
                if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 5 * 60 * 1000l) {
                    logger.info("Trust auth_token without check");
                    return;
                }
                /* Check existing access_token */
                /* Request account information and, at the same time, check if authorization is still valid. */
                logger.info("Checking existing auth_token");
                brlogin.getPage(getAPIBaseWithVersion() + "/account/" + auth_username);
                checkErrors(brlogin, null, account);
                /* TODO: Check which error API will return on expired token. */
                Map<String, Object> entries = JSonStorage.restoreFromString(brlogin.toString(), TypeRef.HASHMAP);
                entries = (Map<String, Object>) entries.get("data");
                loggedIN = entries != null && entries.containsKey("id");
                if (!loggedIN) {
                    /* Build new query containing only what we need. */
                    logger.info("Old token expired? Trying to generate new authorization token");
                    final UrlQuery queryLogin = new UrlQuery();
                    /* Refresh token never expires and can be used to generate new authorization token. */
                    queryLogin.add("refresh_token", auth_refresh_token);
                    queryLogin.add("client_id", getClientID2());
                    queryLogin.add("client_secret", getClientSecret2());
                    queryLogin.add("grant_type", "refresh_token");
                    brlogin.postPage(getAPIBase() + "/oauth2/token", queryLogin);
                    auth_access_token = PluginJSonUtils.getJson(brlogin, "access_token");
                    auth_refresh_token = PluginJSonUtils.getJson(brlogin, "refresh_token");
                    auth_valid_until = PluginJSonUtils.getJson(brlogin, "expires_in");
                    if (StringUtils.isEmpty(auth_access_token) || StringUtils.isEmpty(auth_refresh_token)) {
                        /* Failure e.g. user revoked API access --> Invalid logindata --> Permanently disable account */
                        /*
                         * E.g.
                         * {"data":{"error":"Invalid refresh token","request":"\/oauth2\/token","method":"POST"},"success":false,"status":
                         * 400}
                         */
                        checkErrors(this.br, null, account);
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    /* Ignore 'expires_in' and just use existing token as long as possible. This is only used for debugging. */
                    account.setProperty(PROPERTY_ACCOUNT_access_token, auth_access_token);
                    account.setProperty(PROPERTY_ACCOUNT_refresh_token, auth_refresh_token);
                    if (auth_valid_until != null && auth_valid_until.matches("\\d+")) {
                        account.setProperty(PROPERTY_ACCOUNT_valid_until, Long.parseLong(auth_valid_until));
                    }
                    /* Update token first use timestamp */
                    account.setProperty(PROPERTY_ACCOUNT_token_first_use_timestamp, System.currentTimeMillis());
                    /* Update authorization header */
                    brlogin.getHeaders().put("Authorization", "Bearer " + auth_access_token);
                }
            }
            if (!loggedIN) {
                logger.info("Performing full login");
                genericOauthLogin(account, brlogin);
                final UrlQuery query = UrlQuery.parse(br.toString());
                auth_access_token = query.get("access_token");
                auth_refresh_token = query.get("refresh_token");
                auth_username = query.get("account_username");
                if (StringUtils.isEmpty(auth_username)) {
                    /* E.g.required for response_type=token */
                    auth_username = PluginJSonUtils.getJson(brlogin, "account_username");
                }
                if (!StringUtils.isEmpty(auth_username) && !StringUtils.equals(account.getUser(), auth_username)) {
                    /* Important as we will use Account.getUser() for later requests so it has to be correct! */
                    logger.info("Correcting Account username to API username");
                    account.setUser(auth_username);
                }
            }
            /*
             * Access tokens expire (after ~30 days) according to API docs. Only use the one the user has entered on first attempt e.g. user
             * has just added this account for the first time! Save it as a property on the account and then use that because once token
             * gets refreshed it will differ from the token the user initially entered!
             */
            account.setProperty(PROPERTY_ACCOUNT_initial_password, account.getPass());
            /* Every account-check will use up one API request and we have limited requests --> Do not check account that frequently. */
            account.setRefreshTimeout(5 * 60 * 1000l);
            /* Save cookies - but only to have the cookie-timestamp */
            account.saveCookies(brlogin.getCookies(this.getHost()), "");
        }
    }

    private enum RESPONSE_TYPE {
        TOKEN,
        CODE,
        PIN
    }

    /**
     * 2020-08-07: Cheap oauth login test
     *
     * @throws PluginException
     * @throws IOException
     * @throws InterruptedException
     */
    private void genericOauthLogin(final Account account, final Browser brlogin) throws PluginException, IOException, InterruptedException {
        final String state = generateUniqueID();
        final RESPONSE_TYPE responseType = RESPONSE_TYPE.CODE;
        /* Reset this property to e.g. try again right away with new token once set by user e.g. if user changes 'password'. */
        account.setProperty(PROPERTY_ACCOUNT_access_token, Property.NULL);
        if (account.getPass().contains("error=")) {
            /* User has tried authorization but for some reason it failed. */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        /* Wait for user to complete oauth process in browser. */
        boolean success = false;
        final Thread dialog = showLoginInformationTest(getAuthURLTest(state, responseType));
        String auth_access_token = null;
        String auth_refresh_token = null;
        String auth_valid_until = null;
        final Browser jdpull = brlogin.cloneBrowser();
        try {
            for (int i = 0; i <= 23; i++) {
                logger.info("Waiting for user to authorize application: " + i);
                Thread.sleep(5000);
                jdpull.getPage("http://jdserv.jdownloader.org/oauth2?state=" + state);
                final UrlQuery query = UrlQuery.parse(jdpull.toString());
                final String error = query.get("error");
                if (error != null) {
                    /* Something went wrong */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Error: " + error, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (responseType == RESPONSE_TYPE.CODE) {
                    /* --> Generate required tokens */
                    final String auth_code = query.get("code");
                    if (!StringUtils.isEmpty(auth_code)) {
                        logger.info("Found auth_code --> Trying to get access_token");
                        final UrlQuery codeAuthQuery = new UrlQuery();
                        codeAuthQuery.add("client_id", Encoding.urlEncode(getClientID2()));
                        codeAuthQuery.add("client_secret", Encoding.urlEncode(getClientSecret2()));
                        codeAuthQuery.add("response_type", "code");
                        codeAuthQuery.add("state", Encoding.urlEncode(state));
                        codeAuthQuery.add("grant_type", "authorization_code");
                        codeAuthQuery.add("code", Encoding.urlEncode(auth_code));
                        /*
                         * This parameter is only needed if it's included in the first request too (= the URL which the user has to open in
                         * his browser)
                         */
                        codeAuthQuery.add("redirect_uri", Encoding.urlEncode("http://jdserv.jdownloader.org/oauth"));
                        brlogin.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                        brlogin.postPage(getAPIBase() + "/oauth2/token", codeAuthQuery);
                        final Map<String, Object> entries = JSonStorage.restoreFromString(brlogin.toString(), TypeRef.HASHMAP);
                        auth_access_token = (String) entries.get("access_token");
                        auth_refresh_token = (String) entries.get("refresh_token");
                        success = true;
                        break;
                    }
                } else if (responseType == RESPONSE_TYPE.TOKEN) {
                    auth_access_token = query.get("access_token");
                    auth_refresh_token = query.get("refresh_token");
                    /* Check for success */
                    if (auth_access_token != null && auth_refresh_token != null) {
                        logger.info("Authorization successful");
                        success = true;
                        break;
                    }
                } else {
                    /* E.g. PIN auth */
                    /* TODO */
                }
                /* Check for user abort */
                if (!dialog.isAlive()) {
                    logger.info("Dialog closed!");
                    break;
                } else {
                    logger.info("Authorization not done so far ...");
                    /* Continue */
                }
            }
        } finally {
            dialog.interrupt();
        }
        if (!success) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Timeout! Versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Timeout! Try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else if (StringUtils.isEmpty(auth_access_token)) {
            /* This should never happen. E.g. token was retrieved successfully but access_token generation failed afterwards */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        account.setProperty(PROPERTY_ACCOUNT_access_token, auth_access_token);
        account.setProperty(PROPERTY_ACCOUNT_refresh_token, auth_refresh_token);
        if (auth_valid_until != null && auth_valid_until.matches("\\d+")) {
            account.setProperty(PROPERTY_ACCOUNT_valid_until, Long.parseLong(auth_valid_until));
        }
        account.setProperty(PROPERTY_ACCOUNT_token_first_use_timestamp, System.currentTimeMillis());
        brlogin.getHeaders().put("Authorization", "Bearer " + auth_access_token);
    }

    private String generateUniqueID() {
        return Hash.getStringHash((Application.getRoot(PluginForHost.class) + SecondLevelLaunch.startup + UniqueAlltimeID.next()), Hash.HASH_TYPE_SHA512);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(this.br, account, true);
        if (br.getURL() == null || !br.getURL().contains("/account/" + account.getUser())) {
            br.getPage(getAPIBaseWithVersion() + "/account/" + account.getUser());
            checkErrors(this.br, null, account);
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("data");
        final Object pro_expiration = entries.get("pro_expiration");
        String accountStatus = null;
        if (pro_expiration != null && ((Boolean) pro_expiration).booleanValue() == false) {
            account.setType(AccountType.FREE);
            accountStatus = "Free user";
        } else {
            account.setType(AccountType.PREMIUM);
            accountStatus = "Premium user";
        }
        // final long token_first_usage_timestamp = account.getLongProperty(PROPERTY_ACCOUNT_token_first_use_timestamp, 0);
        // final long token_valid_until = account.getLongProperty(PROPERTY_ACCOUNT_valid_until, 0);
        // if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && token_first_usage_timestamp > 0 && token_valid_until > 0) {
        // ai.setValidUntil(token_first_usage_timestamp + token_valid_until);
        // }
        final String api_limit_reset_timestamp = br.getRequest().getResponseHeader("X-RateLimit-UserReset");
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && api_limit_reset_timestamp != null && api_limit_reset_timestamp.matches("\\d+")) {
            ai.setValidUntil(Long.parseLong(api_limit_reset_timestamp) * 1000l);
        }
        final String api_limit_client_total = br.getRequest().getResponseHeader("X-RateLimit-ClientLimit");
        final String api_limit_client_remaining = br.getRequest().getResponseHeader("X-RateLimit-ClientRemaining");
        final String api_limit_user_total = br.getRequest().getResponseHeader("X-RateLimit-UserLimit");
        final String api_limit_user_remaining = br.getRequest().getResponseHeader("X-RateLimit-UserRemaining");
        /* Some errorhandling */
        if (api_limit_user_remaining != null && Integer.parseInt(api_limit_user_remaining) <= 0) {
            rateLimitReached(this.br, account);
        }
        if (api_limit_client_total != null && api_limit_client_remaining != null && api_limit_user_total != null && api_limit_user_remaining != null) {
            accountStatus += String.format(" | API req left user: %s/%s | client: %s/%s", api_limit_user_remaining, api_limit_user_total, api_limit_client_remaining, api_limit_client_total);
        }
        ai.setStatus(accountStatus);
        ai.setUnlimitedTraffic();
        return ai;
    }

    private void checkErrors(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final boolean success = ((Boolean) entries.get("success")).booleanValue();
        if (!success) {
            /*
             * E.g.
             * {"data":{"error":"The access token provided is invalid.","request":"\/3\/account\/xnull","method":"GET"},"success":false,
             * "status":403}
             */
            entries = (Map<String, Object>) entries.get("data");
            final String errorStr = (String) entries.get("error");
            if (!StringUtils.isEmpty(errorStr)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, errorStr, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        if (br.getHttpConnection().getResponseCode() == 429) {
            rateLimitReached(br, account);
        }
    }

    /**
     * E.g. {"data":{"error":"User request limit
     * exceeded","request":"\/3\/account\/<USERNAME>","method":"GET"},"success":false,"status":429}
     *
     * @throws PluginException
     */
    private void rateLimitReached(final Browser br, final Account account) throws PluginException {
        long reset_in = 0;
        /* This header will usually tell us once rate limit is over (at least when an account was used) */
        final String api_reset_in = br.getRequest().getResponseHeader("X-RateLimit-UserReset");
        if (api_reset_in != null && api_reset_in.matches("\\d+")) {
            reset_in = Long.parseLong(api_reset_in);
        }
        final long waittime;
        if (reset_in > System.currentTimeMillis()) {
            waittime = reset_in - System.currentTimeMillis() + 10000l;
        } else {
            /* Default waittime */
            waittime = 5 * 60 * 1000l;
        }
        if (account != null) {
            throw new AccountUnavailableException("API Rate Limit reached", waittime);
        } else {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "API Rate Limit reached", waittime);
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return MAX_DOWNLOADS;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return MAX_DOWNLOADS;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        /* 2020-04-26: No captchas at all */
        return false;
    }

    /**
     * Parses json either from API, or also if previously set in the browser - it's basically the same as a response similar to the API isa
     * stored in the html code when accessing normal content links: imgur.com/xXxXx
     */
    private String[] parseAPIData(TYPE type, final String json) throws PluginException {
        final boolean animated = Boolean.TRUE.equals(Boolean.valueOf(PluginJSonUtils.getJsonValue(json, "animated")));
        String url = null;
        String fileSize = null;
        String fileType = null;
        switch (type) {
        case MP4:
            url = PluginJSonUtils.getJsonValue(json, "mp4");
            fileSize = PluginJSonUtils.getJsonValue(json, "mp4_size");
            fileType = "mp4";
            break;
        case JPGORGIF:
            url = PluginJSonUtils.getJsonValue(json, "link");
            fileSize = PluginJSonUtils.getJsonValue(json, "size");
            fileType = new Regex(json, "\"(mime)?type\"\\s*:\\s*\"image\\\\?/([^<>]*?)\"").getMatch(1);
            if (fileType == null) {
                fileType = new Regex(json, "\"ext\"\\s*:\\s*\"\\.(.*?)\"").getMatch(0);
            }
            break;
        }/* "mimetype" = site, "type" = API */
        if (fileType == null) {
            if (animated) {
                if (TYPE.MP4.equals(type)) {
                    fileType = "mp4";
                } else {
                    fileType = "gif";
                }
            } else {
                fileType = "jpeg";
            }
        }
        String finalFileName = null;
        String title = PluginJSonUtils.getJsonValue(json, "title");
        if (StringUtils.isEmpty(title)) {
            finalFileName = imgUID + "." + fileType;
        } else {
            title = Encoding.htmlDecode(title);
            title = HTMLEntities.unhtmlentities(title);
            title = HTMLEntities.unhtmlAmpersand(title);
            title = HTMLEntities.unhtmlAngleBrackets(title);
            title = HTMLEntities.unhtmlSingleQuotes(title);
            title = HTMLEntities.unhtmlDoubleQuotes(title);
            finalFileName = title + "." + fileType;
        }
        return new String[] { fileType, fileSize, finalFileName, url, Boolean.toString(animated) };
    }

    /** Returns a link for the user to open in browser. */
    public static final String getURLContent(final String imgUID) {
        final String contentURL = "https://imgur.com/" + imgUID;
        return contentURL;
    }

    /** Returns downloadable imgur link. */
    public static final String getURLDownload(final String imgUID) {
        return "https://imgur.com/download/" + imgUID;
    }

    public static String getImgUID(final DownloadLink dl) {
        return new Regex(dl.getPluginPatternMatcher(), "/([^/]+)$").getMatch(0);
    }

    public static String getFiletype(final DownloadLink dl) {
        final String ret = dl.getStringProperty("filetype", null);
        final String storedDirectURL = dl.getStringProperty(PROPERTY_DOWNLOADLINK_DIRECT_URL);
        if (ret != null) {
            final String image = new Regex(ret, "images/(.+)").getMatch(0);
            if (image != null) {
                if (StringUtils.equals("jpeg", image)) {
                    return "jpg";
                } else {
                    return image;
                }
            }
            final String video = new Regex(ret, "video/(.+)").getMatch(0);
            if (video != null) {
                return video;
            }
            if (StringUtils.equals("jpeg", ret)) {
                return "jpg";
            } else {
                return ret;
            }
        } else if (storedDirectURL != null && storedDirectURL.matches(ImgurComGallery.type_single_direct)) {
            return Plugin.getFileNameExtensionFromURL(storedDirectURL).replace(".", "");
        }
        return null;
    }

    public static boolean userPrefersMp4() {
        return SubConfiguration.getConfig("imgur.com").getBooleanProperty(SETTING_MP4, defaultMP4);
    }

    public static final String getAuthorization() throws Exception {
        final String clientid = getClientID();
        return "Client-ID " + clientid;
    }

    public static final String getClientID() throws Exception {
        final String clientid_setting = SubConfiguration.getConfig("imgur.com").getStringProperty(SETTING_CLIENT_ID, defaultAPISettingUserVisibleText);
        if (StringUtils.isEmpty(clientid_setting) || StringUtils.equalsIgnoreCase("JDDEFAULT", clientid_setting)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return clientid_setting;
        }
    }

    public static final String getClientSecret() throws Exception {
        final SubConfiguration conf = SubConfiguration.getConfig("imgur.com");
        final String clientsecret_setting = conf.getStringProperty(SETTING_CLIENT_SECRET, defaultAPISettingUserVisibleText);
        if (StringUtils.isEmpty(clientsecret_setting) || StringUtils.equalsIgnoreCase("JDDEFAULT", clientsecret_setting)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return clientsecret_setting;
        }
    }

    private String getAuthURL() throws Exception {
        return String.format("%s/oauth2/authorize?client_id=%s&response_type=token", getAPIBase(), getClientID());
    }

    public static Browser prepBRWebsite(final Browser br) {
        br.setAllowedResponseCodes(responsecode_website_overloaded);
        br.setFollowRedirects(true);
        /* Skip "are you over 19?" Form. */
        br.setCookie("imgur.com", "over18", "1");
        return br;
    }

    public static Browser prepBRAPI(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 400, 429, responsecode_website_overloaded });
        return br;
    }

    private void websiteOverloaded() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "502: 'Imgur over capacity'", 5 * 60 * 1000l);
    }

    private Thread showLoginInformation() throws Exception {
        final String autURL = getAuthURL();
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Imgur.com - Login";
                        message += "Hallo liebe(r) Imgur NutzerIn\r\n";
                        message += "Um deinen Imgur Account in JD verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne diesen Link im Browser falls das nicht automatisch passiert:\r\n\t'" + autURL + "'\t\r\n";
                        message += "2. Autorisiere JD auf der Imgur Webseite sofern du nicht sofort von imgur auf die jdownloader Webseite weitergeleitet wirst.\r\nDu wirst weitergeleitet auf 'jdownloader.org/#access_token=...'.\r\nKopiere diesen Link aus der Adresszeile und gib ihn ins 'Passwort' Feld der imgur Loginmaske in JD ein.\r\n";
                        message += "Dein Account sollte nach einigen Sekunden von JDownloader akzeptiert werden.\r\n";
                    } else {
                        title = "Imgur.com - Login";
                        message += "Hello dear Imgur user\r\n";
                        message += "In order to use imgur with JD, you need to follow these steps:\r\n";
                        message += "1. Open the following URL in your browser if it is not opened automatically:\r\n\t'" + autURL + "'\t\r\n";
                        message += "2. Authorize JD on the Imgur website in case you are not automatically redirected from imgur to the jdownloader website immediately.\r\nYou will be redirected to 'jdownloader.org/#access_token=...'.\r\nCopy this complete URL from the address bar of your browser and enter it into the password field of the imgur login mask in JD. \r\n";
                        message += "Your account should be accepted in JDownloader within a few seconds.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(2 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(autURL);
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
    public String getDescription() {
        return "This Plugin can download galleries/albums/images from imgur.com.";
    }

    /** Returns either the original server filename or one that is very similar to the original */
    @SuppressWarnings("deprecation")
    public static String getFormattedFilename(final DownloadLink link) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("imgur.com");
        final String ext = getFiletype(link);
        if (ext == null) {
            /* We cannot generate a user-preferred filename without extension! */
            return null;
        }
        final String username = link.getStringProperty("directusername", "-");
        final String title = link.getStringProperty("directtitle", "-");
        final String imgid = getImgUID(link);
        final String orderid = link.getStringProperty("orderid", "-");
        /* Date: Maybe add this in the future, if requested by a user. */
        // final long date = getLongProperty(downloadLink, "originaldate", 0l);
        // String formattedDate = null;
        // /* Get correctly formatted date */
        // String dateFormat = "yyyy-MM-dd";
        // SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
        // Date theDate = new Date(date);
        // try {
        // formatter = new SimpleDateFormat(dateFormat);
        // formattedDate = formatter.format(theDate);
        // } catch (Exception e) {
        // /* prevent user error killing plugin */
        // formattedDate = "";
        // }
        // /* Get correctly formatted time */
        // dateFormat = "HHmm";
        // String time = "0000";
        // try {
        // formatter = new SimpleDateFormat(dateFormat);
        // time = formatter.format(theDate);
        // } catch (Exception e) {
        // /* prevent user error killing plugin */
        // time = "0000";
        // }
        String formattedFilename = cfg.getStringProperty(SETTING_CUSTOM_FILENAME, defaultCustomFilename);
        if (!formattedFilename.contains("*imgid*") && !formattedFilename.contains("*ext*")) {
            formattedFilename = defaultCustomFilename;
        }
        formattedFilename = formattedFilename.replace("*orderid*", orderid);
        formattedFilename = formattedFilename.replace("*imgid*", imgid);
        formattedFilename = formattedFilename.replace("*ext*", "." + ext);
        if (username != null) {
            formattedFilename = formattedFilename.replace("*username*", username);
        }
        if (title != null) {
            formattedFilename = formattedFilename.replace("*title*", title);
        }
        formattedFilename = formattedFilename.replaceFirst("^([ \\-_]+)", "").trim();
        return formattedFilename.trim();
    }

    /** Returns either the original server filename or one that is very similar to the original */
    @SuppressWarnings("deprecation")
    public static String getFormattedPackagename(final String... params) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("imgur.com");
        String username = params[0];
        String title = params[1];
        final String galleryid = params[2];
        if (username == null) {
            username = "-";
        }
        if (title == null) {
            title = "-";
        }
        String formattedFilename = cfg.getStringProperty(SETTING_CUSTOM_PACKAGENAME, defaultCustomPackagename);
        if (!formattedFilename.contains("*galleryid*")) {
            formattedFilename = defaultCustomPackagename;
        }
        formattedFilename = formattedFilename.replace("*galleryid*", galleryid);
        if (username != null) {
            formattedFilename = formattedFilename.replace("*username*", username);
        }
        if (title != null) {
            formattedFilename = formattedFilename.replace("*title*", title);
        }
        formattedFilename = formattedFilename.replaceFirst("^([ \\-_]+)", "").trim();
        return formattedFilename;
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
                                                  {
                                                      put("SETTING_GRAB_SOURCE_URL_VIDEO", "For video (.gif) urls: Grab source url (e.g. youtube url)?");
                                                      put("SETTING_TAGS", "Explanation of the available tags:\r\n*username* = Name of the user who posted the content\r\n*title* = Title of the picture\r\n*imgid* = Internal imgur id of the picture e.g. 'BzdfkGj'\r\n*orderid* = Order-ID of the picture e.g. '007'\r\n*ext* = Extension of the file");
                                                      put("LABEL_FILENAME", "Define custom filename:");
                                                      put("SETTING_TAGS_PACKAGENAME", "Explanation of the available tags:\r\n*username* = Name of the user who posted the content\r\n*title* = Title of the gallery\r\n*galleryid* = Internal imgur id of the gallery e.g. 'AxG3w'");
                                                      put("LABEL_PACKAGENAME", "Define custom packagename for galleries:");
                                                  }
                                              };
    private HashMap<String, String> phrasesDE = new HashMap<String, String>() {
                                                  {
                                                      put("SETTING_GRAB_SOURCE_URL_VIDEO", "Für video (.gif) urls: Quell-urls (z.B. youtube urls) auch hinzufügen?");
                                                      put("SETTING_TAGS", "Erklärung der verfügbaren Tags:\r\n*username* = Name des Benutzers, der die Inhalte hochgeladen hat\r\n*title* = Titel des Bildes\r\n*imgid* = Interne imgur id des Bildes z.B. 'DcTnzPt'\r\n*orderid* = Platzierungs-ID des Bildes z.B. '007'\r\n*ext* = Dateiendung");
                                                      put("LABEL_FILENAME", "Gib das Muster des benutzerdefinierten Dateinamens an:");
                                                      put("SETTING_TAGS_PACKAGENAME", "Erklärung der verfügbaren Tags:\r\n*username* = Name des Benutzers, der die Inhalte hochgeladen hat\r\n*title* = Titel der Gallerie\r\n*galleryid* = Interne imgur id der Gallerie z.B. 'AxG3w'");
                                                      put("LABEL_PACKAGENAME", "Gib das Muster des benutzerdefinierten Paketnamens für Gallerien an:");
                                                  }
                                              };

    /**
     * Returns a German/English translation of a phrase. We don't use the JDownloader translation framework since we need only German and
     * English.
     *
     * @param key
     * @return
     */
    private String getPhrase(String key) {
        if ("de".equals(System.getProperty("user.language")) && phrasesDE.containsKey(key)) {
            return phrasesDE.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        }
        return "Translation not found!";
    }

    private static final String defaultAPISettingUserVisibleText = "JDDEFAULT";
    public static final boolean defaultMP4                       = false;
    public static final boolean defaultSOURCEVIDEO               = false;
    private static final String defaultCustomFilename            = "*username* - *title*_*orderid*_*imgid**ext*";
    private static final String defaultCustomPackagename         = "*username* - *title* - *galleryid*";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_MP4, JDL.L("plugins.hoster.ImgUrCom.downloadMP4", "Download .mp4 files instead of .gif?")).setDefaultValue(defaultMP4));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "API settings - see imgur.com/account/settings/apps"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SETTING_USE_API, "Use API in anonymous mode too (recommended - API will always be used in account mode!)").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), SETTING_CLIENT_ID, "Enter your own imgur Oauth Client-ID\r\nOn change, you will have to remove- and re-add your imgur account to JDownloader!").setDefaultValue(defaultAPISettingUserVisibleText));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), SETTING_CLIENT_SECRET, "Enter your own imgur Oauth Client-Secret\r\nOn change, you will have to remove- and re-add your imgur account to JDownloader!").setDefaultValue(defaultAPISettingUserVisibleText));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Other settings:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SETTING_GRAB_SOURCE_URL_VIDEO, getPhrase("SETTING_GRAB_SOURCE_URL_VIDEO")).setDefaultValue(defaultSOURCEVIDEO));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), SETTING_CUSTOM_FILENAME, getPhrase("LABEL_FILENAME")).setDefaultValue(defaultCustomFilename));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_TAGS")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), SETTING_CUSTOM_PACKAGENAME, getPhrase("LABEL_PACKAGENAME")).setDefaultValue(defaultCustomPackagename));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_TAGS_PACKAGENAME")));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}