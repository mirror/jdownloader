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
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
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
import jd.utils.locale.JDL;

/**
 * IMPORTANT: Never grab IDs bigger than 7 characters because these are Thumbnails - see API description: http://api.imgur.com/models/image
 * (scroll down to "Image thumbnails"
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "imgur.com" }, urls = { "https?://imgurdecrypted\\.com/download/([A-Za-z0-9]{7}|[A-Za-z0-9]{5})" })
public class ImgUrCom extends PluginForHost {
    public ImgUrCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        // this.enablePremium("https://imgur.com/register");
    }

    @Override
    public String getAGBLink() {
        return "https://imgur.com/tos";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("imgurdecrypted.com/", "imgur.com/"));
    }

    private enum TYPE {
        MP4,
        JPGORGIF
    }

    /* User settings */
    private static final String  SETTING_MP4                     = "SETTING_MP4";
    private static final String  SETTING_CLIENT_ID               = "CLIENT_ID";
    private static final String  SETTING_CLIENT_SECRET           = "CLIENT_SECRET";
    private static final String  SETTING_USE_API                 = "SETTING_USE_API";
    private static final String  SETTING_GRAB_SOURCE_URL_VIDEO   = "SETTING_GRAB_SOURCE_URL_VIDEO";
    private static final String  SETTING_CUSTOM_FILENAME         = "SETTING_CUSTOM_FILENAME";
    private static final String  SETTING_CUSTOM_PACKAGENAME      = "SETTING_CUSTOM_PACKAGENAME";
    /* API related stuff */
    public static final String   OAUTH_CLIENTID                  = "Mzc1YmE4Y2FmNjA0ZDQy";
    /* Constants */
    public static final long     view_filesizelimit              = 20447232l;
    public static final int      responsecode_website_overloaded = 502;
    private static final int     MAX_DOWNLOADS                   = -1;
    private static final boolean RESUME                          = true;
    private static final int     MAXCHUNKS                       = 1;
    /* Variables */
    private String               dllink                          = null;
    private boolean              dl_IMPOSSIBLE_APILIMIT_REACHED  = false;
    private boolean              dl_IMPOSSIBLE_SERVER_ISSUE      = false;
    private String               imgUID                          = null;

    /* Documentation see: https://apidocs.imgur.com/ */
    public static String getAPIBase() {
        return "https://api.imgur.com";
    }

    public static String getAPIBaseWithVersion() {
        return getAPIBase() + "/3";
    }

    /**
     * TODO: 1. Maybe add a setting to download albums as .zip (if possible via site). 2. Maybe add a setting to add numbers in front of the
     * filenames (same way imgur does it when you download .zip files of albums).
     */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean start_isDownload) throws Exception {
        imgUID = getImgUID(link);
        dllink = link.getStringProperty("directlink", null);
        dl_IMPOSSIBLE_SERVER_ISSUE = false;
        /* Avoid unneccessary requests --> If we have the directlink, filesize and a nice filename, do not access site/API! */
        String filename_formatted = null;
        TYPE type = null;
        if (dllink == null || link.getLongProperty("decryptedfilesize", -1) == -1 || link.getStringProperty("decryptedfinalfilename", null) == null || getFiletype(link) == null) {
            prepBRAPI(this.br);
            boolean api_failed = false;
            if (!this.getPluginConfig().getBooleanProperty(SETTING_USE_API, false)) {
                api_failed = true;
            } else {
                br.getHeaders().put("Authorization", getAuthorization());
                try {
                    getPage(this.br, getAPIBaseWithVersion() + "/image/" + imgUID);
                    if (this.br.getHttpConnection().getResponseCode() == 429) {
                        api_failed = true;
                    }
                } catch (final BrowserException e) {
                    throw e;
                }
            }
            String apiResponse[] = null;
            if (!api_failed) {
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
                /*
                 * Note that for pictures/especially GIFs over 20 MB, the "link" value will only contain a link which leads to a preview or
                 * low quality version of the picture. This is why we need a little workaround for this case (works from 19.5++ MB).
                 */
                if (size >= view_filesizelimit) {
                    logger.info("File is bigger than 20 (19.5) MB --> Using /downloadlink as API-workaround");
                    dllink = getURLDownload(imgUID);
                }
                filename_formatted = getFormattedFilename(link);
            } else {
                /*
                 * Workaround for API limit reached or in case user disabled API - second way does return 503 response in case API limit is
                 * reached: http://imgur.com/download/ + imgUID. This code should never be reached!
                 */
                this.br = prepBRWebsite(this.br);
                getPage(this.br, "http://imgur.com/" + imgUID);
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
                /*
                 * Note that for pictures/especially GIFs over 20 MB, the "link" value will only contain a link which leads to a preview or
                 * low quality version of the picture. This is why we need a little workaround for this case (works from 19.5++ MB).
                 */
                if (userPrefersMp4() && "gif".equalsIgnoreCase(fileType) || "mp4".equalsIgnoreCase(fileType)) {
                    dllink = getMp4Downloadlink(link);
                } else if (size >= view_filesizelimit) {
                    logger.info("File is bigger than 20 (19.5) MB --> Using /downloadlink as API-workaround");
                    dllink = getURLDownload(imgUID);
                } else {
                    dllink = getURLView(link);
                }
            }
            link.setProperty("directlink", dllink);
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (type != null) {
            // uniqueID depends on preferred type
            link.setLinkID("://" + this.getHost() + type + "/" + imgUID);
        } else {
            link.setLinkID("://" + this.getHost() + "/" + imgUID);
        }
        if (filename_formatted != null) {
            if (start_isDownload) {
                link.setFinalFileName(filename_formatted);
            } else {
                link.setName(filename_formatted);
            }
        }
        if (!start_isDownload && (link.getVerifiedFileSize() == -1 && link.getView().getBytesTotal() == -1)) {
            /*
             * Only check available link if user is NOT starting the download --> Avoid to access it twice in a small amount of time -->
             * Keep server load down.
             */
            /*
             * 2016-08-10: Additionally check filesize via url if user wants to have a specified format as the filesize from website json or
             * API is usually for the original file!
             */
            URLConnectionAdapter con = null;
            try {
                con = this.br.openHeadConnection(this.dllink);
                if (con.getResponseCode() == responsecode_website_overloaded) {
                    websiteOverloaded();
                } else if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (con.getContentType().contains("html") || !con.isOK()) {
                    /* E.g. HTTP/1.1 503 first byte timeout */
                    dl_IMPOSSIBLE_SERVER_ISSUE = true;
                }
                final long size = con.getLongContentLength();
                if (size >= 0) {
                    link.setProperty("decryptedfilesize", size);
                    link.setDownloadSize(size);
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
        requestFileInformation(link, true);
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
        if (dl.getConnection().getContentType().contains("html")) {
            final int responsecode = dl.getConnection().getResponseCode();
            if (responsecode == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (responsecode == responsecode_website_overloaded) {
                websiteOverloaded();
            } else if (responsecode == 503) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 1 * 60 * 60 * 1000l);
            }
            logger.warning("Finallink leads to HTML code --> Following connection");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, true);
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
        if (dl.getConnection().getContentType().contains("html")) {
            final int responsecode = dl.getConnection().getResponseCode();
            if (responsecode == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (responsecode == responsecode_website_overloaded) {
                websiteOverloaded();
            } else if (responsecode == 503) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 1 * 60 * 60 * 1000l);
            }
            logger.warning("Finallink leads to HTML code --> Following connection");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
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

    private static final String PROPERTY_ACCOUNT_initial_password = "initial_password";
    private static final String PROPERTY_ACCOUNT_access_token     = "access_token";

    /** Checks to see if e.g. user has changed password. */
    private boolean isSamePW(final Account account) {
        final String initialPW = account.getStringProperty(PROPERTY_ACCOUNT_initial_password);
        return StringUtils.equalsIgnoreCase(initialPW, account.getPass());
    }

    private boolean isAuthorizationURL(final String str) {
        try {
            final UrlQuery query = new UrlQuery().parse(str);
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

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                prepBRAPI(br);
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
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Versuch's nochmal und gib die Autorisierungs-URL in das Passwort Feld ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Try again and enter your authorization URL in the password field.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                final UrlQuery query = new UrlQuery().parse(account.getPass());
                /*
                 * Access tokens expire! Only use the one the user has entered on first attempt e.g. user has just added this account for
                 * the first time!
                 */
                String access_token = account.getStringProperty(PROPERTY_ACCOUNT_access_token);
                if (access_token == null) {
                    logger.info("Obtaining token from URL --> Probably first time login");
                    access_token = query.get("access_token");
                }
                final String auth_refresh_token = query.get("refresh_token");
                final String auth_username = query.get("account_username");
                if (!StringUtils.equals(account.getUser(), auth_username)) {
                    /* Important as we will use Account.getUser() for later requests so it has to be correct! */
                    logger.info("Correcting Account username to API username");
                    account.setUser(auth_username);
                }
                br.getHeaders().put("Authorization", "Bearer " + access_token);
                if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 5 * 60 * 1000l) {
                    logger.info("Trust token without check");
                    return;
                }
                /* Check existing access_token */
                /* Ignore 'expires_in' and just use existing token as long as possible. */
                // final String auth_expires_in = query.get("expires_in");
                /* Request account information and, at the same time, check if authorization is still valid. */
                br.getPage(getAPIBaseWithVersion() + "/account/" + auth_username);
                /*
                 * TODO: Add check to detect expired token. See if we can differentiate between EXPIRED and INVALID tokens --> Hmm or always
                 * try refresh on failure --> If refresh fails, we can definitely deactivate account permanently!
                 */
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                boolean loggedIN = true;
                if (!loggedIN) {
                    /* Build new query containing only what we need. */
                    logger.info("Trying to generate new authorization token");
                    final UrlQuery queryLogin = new UrlQuery();
                    /* Refresh token never expires and can be used to generate new authorization token. */
                    queryLogin.add("refresh_token", auth_refresh_token);
                    queryLogin.add("client_id", getClientID());
                    queryLogin.add("client_secret", getClientSecret());
                    queryLogin.add("grant_type", "refresh_token");
                    br.postPage(getAPIBase() + "/oauth2/token", queryLogin);
                    /* TODO: Check if the refresh token also changes */
                }
                account.setProperty(PROPERTY_ACCOUNT_access_token, access_token);
                /* Every account-check will use up one API request and we have limited requests --> Do not check account that frequently. */
                account.setRefreshTimeout(5 * 60 * 1000l);
                /* Save cookies - but only to have the cookie-timestamp */
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            throw e;
        }
        if (br.getURL() == null || !br.getURL().contains("/account/" + account.getUser())) {
            br.getPage(getAPIBaseWithVersion() + "/account/" + account.getUser());
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
        final String api_limit_client_total = br.getRequest().getResponseHeader("X-RateLimit-ClientLimit");
        final String api_limit_client_remaining = br.getRequest().getResponseHeader("X-RateLimit-ClientRemaining");
        final String api_limit_user_total = br.getRequest().getResponseHeader("X-RateLimit-UserLimit");
        final String api_limit_user_remaining = br.getRequest().getResponseHeader("X-RateLimit-UserRemaining");
        if (api_limit_client_total != null && api_limit_client_remaining != null && api_limit_user_total != null && api_limit_user_remaining != null) {
            accountStatus += String.format(" | API req left user: %s/%s | client: %s/%s", api_limit_user_remaining, api_limit_user_total, api_limit_client_remaining, api_limit_client_total);
        }
        ai.setStatus(accountStatus);
        /*
         * TODO: Maybe add functionality to update account status with rate limit info more frequently e.g. after every download/api
         * request.
         */
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, true);
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

    public static String getBigFileDownloadlink(final DownloadLink dl) {
        final String imgUID = getImgUID(dl);
        final String filetype = getFiletype(dl);
        String downloadlink;
        if (filetype.equals("gif") || filetype.equals("webm") || filetype.equals("mp4")) {
            /* Small workaround for gif files. */
            downloadlink = getURLView(dl);
        } else {
            downloadlink = getURLDownload(imgUID);
        }
        return downloadlink;
    }

    public static String getMp4Downloadlink(final DownloadLink dl) {
        final String imgUID = getImgUID(dl);
        final String downloadlink = "https://i.imgur.com/" + imgUID + ".mp4";
        return downloadlink;
    }

    /** Returns a link for the user to open in browser. */
    public static final String getURLContent(final String imgUID) {
        final String url_content = "https://imgur.com/" + imgUID;
        return url_content;
    }

    /** Returns downloadable imgur link. */
    public static final String getURLDownload(final String imgUID) {
        return "https://imgur.com/download/" + imgUID;
    }

    /** Returns viewable/downloadable imgur link. */
    public static final String getURLView(final DownloadLink dl) {
        final String imgUID = getImgUID(dl);
        final String filetype = getFiletypeForUrl(dl);
        final String link_view = "https://i.imgur.com/" + imgUID + "." + filetype;
        return link_view;
    }

    public static String getImgUID(final DownloadLink dl) {
        return dl.getStringProperty("imgUID", null);
    }

    public static String getFiletype(final DownloadLink dl) {
        final String ret = dl.getStringProperty("filetype", null);
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
        }
        return ".unknown";
    }

    public static String getFiletypeForUrl(final DownloadLink dl) {
        final boolean preferMP4 = userPrefersMp4();
        String filetype_url;
        final String real_filetype = getFiletype(dl);
        if ((real_filetype.equals("gif") && preferMP4) || real_filetype.equals("mp4")) {
            filetype_url = "mp4";
        } else {
            filetype_url = real_filetype;
        }
        return filetype_url;
    }

    public static String getFiletypeForUser(final DownloadLink dl) {
        final String real_filetype = getFiletype(dl);
        final String corrected_filetype = correctFiletypeUser(real_filetype);
        return corrected_filetype;
    }

    public static String correctFiletypeUser(final String input) {
        final boolean preferMP4 = userPrefersMp4();
        String correctedfiletype_user;
        if (input.equals("gif") && preferMP4) {
            correctedfiletype_user = "mp4";
        } else {
            correctedfiletype_user = input;
        }
        return correctedfiletype_user;
    }

    public static boolean userPrefersMp4() {
        return SubConfiguration.getConfig("imgur.com").getBooleanProperty(SETTING_MP4, defaultMP4);
    }

    public static final String getAuthorization() {
        final String clientid = getClientID();
        return "Client-ID " + clientid;
    }

    public static final String getClientID() {
        final String clientid;
        final String clientid_setting = SubConfiguration.getConfig("imgur.com").getStringProperty(SETTING_CLIENT_ID, defaultAPISettingUserVisibleText);
        if (clientid_setting.equalsIgnoreCase("JDDEFAULT")) {
            clientid = Encoding.Base64Decode(OAUTH_CLIENTID);
        } else {
            clientid = clientid_setting;
        }
        return clientid;
    }

    public static final String getClientSecret() {
        final String clientsecret;
        final String clientid_setting = SubConfiguration.getConfig("imgur.com").getStringProperty(SETTING_CLIENT_ID, defaultAPISettingUserVisibleText);
        if (clientid_setting.equalsIgnoreCase("JDDEFAULT")) {
            clientsecret = "TODO_HIDDEN_UNTIL_ACCOUNT_SUPPORT_IS_COMPLETE";
        } else {
            clientsecret = clientid_setting;
        }
        return clientsecret;
    }

    private String getAuthURL() {
        return String.format("%s/oauth2/authorize?client_id=%s&response_type=token", getAPIBase(), getClientID());
    }

    public static Browser prepBRWebsite(final Browser br) {
        prepBRGeneral(br);
        return br;
    }

    public static Browser prepBRAPI(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 429 });
        prepBRGeneral(br);
        return br;
    }

    public static Browser prepBRGeneral(final Browser br) {
        /* 502 == website overloadeds */
        br.setAllowedResponseCodes(responsecode_website_overloaded);
        br.setFollowRedirects(true);
        return br;
    }

    private void websiteOverloaded() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "502: 'Imgur over capacity'", 5 * 60 * 1000l);
    }

    private Thread showLoginInformation() {
        final String autURL = getAuthURL();
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Imgur.com - Login";
                        message += "Hallo liebe(r) Imgur NutzerIn\r\n";
                        message += "Um deinen Imgur Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne diesen Link im Browser falls das nicht automatisch passiert:\r\n\t'" + autURL + "'\t\r\n";
                        message += "2. Autorisiere JDownloader auf der Imgur Webseite.\r\nDu wirst weitergeleitet auf 'my.jdownloader.org/#access_token=...'.\r\nKopiere diesen Link aus der Adresszeile und gib ihn ins 'Passwort' Feld der imgur Loginmaske in JD ein.\r\n";
                        message += "Dein Account sollte nach einigen Sekunden von JDownloader akzeptiert werden.\r\n";
                    } else {
                        title = "Imgur.com - Login";
                        message += "Hello dear Imgur user\r\n";
                        message += "In order to use imgur with JDownloader, you need to follow these steps:\r\n";
                        message += "1. Open the following URL in your browser if it is not opened automatically:\r\n\t'" + autURL + "'\t\r\n";
                        message += "2. Authorize JDownloader on the Imgur website.\r\nYou will be redirected to 'my.jdownloader.org/#access_token=...'.\r\nCopy this complete URL from the address bar of your browser and enter it into the password field of the imgur login mask in JD. \r\n";
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
    public static String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("imgur.com");
        final String ext = "." + getFiletypeForUser(downloadLink);
        final String username = downloadLink.getStringProperty("directusername", "-");
        final String title = downloadLink.getStringProperty("directtitle", "-");
        final String imgid = downloadLink.getStringProperty("imgUID", null);
        final String orderid = downloadLink.getStringProperty("orderid", "-");
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
        formattedFilename = formattedFilename.replace("*ext*", ext);
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
    private static final String defaultCustomFilename            = "*username* - *title*_*imgid**ext*";
    private static final String defaultCustomPackagename         = "*username* - *title* - *galleryid*";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_MP4, JDL.L("plugins.hoster.ImgUrCom.downloadMP4", "Download .mp4 files instead of .gif?")).setDefaultValue(defaultMP4));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "API settings - see imgur.com/account/settings/apps"));
        final ConfigEntry cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SETTING_USE_API, "Use API (recommended!)").setDefaultValue(true);
        getConfig().addEntry(cfg);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), SETTING_CLIENT_ID, "Enter your own imgur Oauth Client-ID:").setDefaultValue(defaultAPISettingUserVisibleText).setEnabledCondidtion(cfg, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), SETTING_CLIENT_SECRET, "Enter your own imgur Oauth Client-Secret:").setDefaultValue(defaultAPISettingUserVisibleText).setEnabledCondidtion(cfg, true));
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