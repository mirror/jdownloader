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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.text.TextDownloader;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DegooCom extends PluginForHost {
    public DegooCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://degoo.com/");
    }

    @Override
    public String getAGBLink() {
        return "https://app.degoo.com/";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "degoo.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://cloud\\." + buildHostsPatternPart(domains) + "/share/([A-Za-z0-9\\-_]+)\\?ID=(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private final boolean        FREE_RESUME                    = false;
    private final int            FREE_MAXCHUNKS                 = 1;
    private final int            FREE_MAXDOWNLOADS              = 20;
    private static final boolean ACCOUNT_FREE_RESUME            = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS         = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS      = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME         = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS      = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS   = 20;
    public static final String   PROPERTY_DIRECTURL             = "free_directlink";
    public static final String   PROPERTY_DIRECTURL_ACCOUNT     = "account_directlink";
    private static final String  PROPERTY_ACCOUNT_TOKEN         = "token";
    private static final String  PROPERTY_ACCOUNT_REFRESH_TOKEN = "refresh_token";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getUniqueLinkIDFromURL(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getUniqueLinkIDFromURL(final DownloadLink link) {
        final String folderID = getFolderID(link);
        final String fileID = getFileID(link);
        if (folderID != null && fileID != null) {
            return folderID + "_" + fileID;
        } else {
            return null;
        }
    }

    private String                         dllink           = null;
    private static final String            API_BASE         = "https://rest-api.degoo.com";
    private static final String            API_BASE_GRAPHQL = "https://production-appsync.degoo.com/graphql";
    private static HashMap<String, Object> API_INFO         = new HashMap<String, Object>();

    private String getFolderID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getFileID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 400 });
        final String folderID = getFolderID(link);
        final String fileID = getFileID(link);
        if (folderID == null || fileID == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("HashValue", folderID);
        params.put("FileID", fileID);
        br.postPageRaw(API_BASE + "/overlay", JSonStorage.serializeToJson(params));
        /* 2021-01-17: HTTP 400: {"Error": "Not authorized!"} == File offline */
        if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        final String filename = (String) entries.get("Name");
        final int filesize = ((Number) entries.get("Size")).intValue();
        this.dllink = (String) entries.get("URL");
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        }
        if (filesize > 0) {
            link.setVerifiedFileSize(filesize);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        if (this.attemptStoredDownloadurlDownload(link, PROPERTY_DIRECTURL, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS)) {
            logger.info("Re-using stored directurl");
            dl.startDownload();
        } else {
            requestFileInformation(link);
            if (!StringUtils.isEmpty(this.dllink)) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    br.followConnection(true);
                    checkDownloadErrorsLastResort(dl.getConnection());
                }
                link.setProperty(PROPERTY_DIRECTURL, dl.getConnection().getURL().toString());
                dl.startDownload();
            } else {
                /* For text files they may return the full content of a file as base64 encoded text right away. */
                final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                final String b64EncodedData = (String) entries.get("Data");
                if (StringUtils.isEmpty(b64EncodedData)) {
                    /* 2021-08-16: Don't use PLUGIN_DEFECT LinkStatus here as we're using an API which is supposed to be fairly stable. */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadurl");
                }
                final String text = Encoding.Base64Decode(b64EncodedData);
                /* Write text to file */
                dl = new TextDownloader(this, link, text);
                dl.startDownload();
            }
        }
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directurlproperty, final boolean resume, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directurlproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    private boolean login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                /* 2021-08-19: 429 is only allowed for debug purposes */
                br.setAllowedResponseCodes(400, 429);
                String token = account.getStringProperty(PROPERTY_ACCOUNT_TOKEN);
                if (token != null) {
                    logger.info("Attempting token login");
                    if (!force) {
                        logger.info("Trust token without checking");
                        return false;
                    } else {
                        accessAccountInfoIfNotAlreadyAccessed(br, account);
                        try {
                            checkLoginErrorsAPI(br, account);
                            logger.info("Token login successful");
                            return true;
                        } catch (final Throwable e) {
                            logger.info("Token login failed");
                            logger.log(e);
                            account.removeProperty(PROPERTY_ACCOUNT_TOKEN);
                            account.removeProperty(PROPERTY_ACCOUNT_REFRESH_TOKEN);
                        }
                    }
                }
                logger.info("Performing full login");
                /* Login is possible with both requests */
                br.postPageRaw(API_BASE + "/login", "{\"Username\":\"" + account.getUser() + "\",\"Password\":\"" + account.getPass() + "\",\"GenerateToken\":true}");
                checkLoginErrorsAPI(br, account);
                // br.postPageRaw(API_BASE + "/register", "{\"Username\":\"" + account.getUser() + "\",\"Password\":\"" +
                // account.getPass() + "\",\"LanguageCode\":\"de-DE\",\"CountryCode\":\"DE\",\"Source\":\"Web
                // App\",\"GenerateToken\":true}");
                final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                final String refreshToken = (String) entries.get("RefreshToken");
                token = (String) entries.get("Token");
                if (StringUtils.isEmpty(refreshToken) || StringUtils.isEmpty(token)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Unknown login failure", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                account.setProperty(PROPERTY_ACCOUNT_TOKEN, token);
                /* TODO: Check if/how we can make use of that refresh token */
                account.setProperty(PROPERTY_ACCOUNT_REFRESH_TOKEN, refreshToken);
                return true;
            } catch (final PluginException e) {
                throw e;
            }
        }
    }

    private Browser prepBRGraphQL(final Browser br) throws IOException, PluginException {
        br.getHeaders().put("x-api-key", getAPIKey(br));
        br.getHeaders().put("Content-Type", "application/json");
        br.getHeaders().put("Origin", "https://app." + this.getHost());
        return br;
    }

    private String getAPIKey(final Browser br) throws IOException, PluginException {
        synchronized (API_INFO) {
            if (!API_INFO.containsKey("apikey") || !API_INFO.containsKey("timestamp") || System.currentTimeMillis() - ((Number) API_INFO.get("timestamp")).longValue() > 2 * 60 * 60 * 1000) {
                logger.info("Refreshing public apikey");
                final Browser brc = br.cloneBrowser();
                brc.getPage("https://app." + this.getHost() + "/8314-es2015.67eb1618e0f6169f0c76.js");
                final String key = brc.getRegex("this\\.API_KEY=this\\.useProduction\\(\\)\\?\"([^\"]+)\"").getMatch(0);
                if (key == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                API_INFO.put("apikey", key);
                API_INFO.put("timestamp", System.currentTimeMillis());
                return key;
            } else {
                /* Return existing apikey */
                return API_INFO.get("apikey").toString();
            }
        }
    }

    private void checkLoginErrorsAPI(final Browser br, final Account account) throws Exception {
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Object errorO = entries.get("Error");
        final Object errorsO = entries.get("errors");
        /* Collection of possible errors below */
        /* Response 400: {"Error": "Not authorized!"} */
        if (errorO instanceof String) {
            final String error = errorO.toString();
            throw new AccountInvalidException(error);
        } else if (errorsO instanceof List) {
            /**
             * E.g. after calling accessAccountInfoIfNotAlreadyAccessed: </br>
             * {"data":{"getUserInfo3":null},"errors":[{"path":["getUserInfo3"],"data":null,"errorType":"Unauthorized","errorInfo":null,
             * "locations":[{"line":1,"column":42,"sourceName":null}],"message":"Not Authorized to access getUserInfo3 on type Query"}]}
             */
            final List<Map<String, Object>> errors = (List<Map<String, Object>>) errorsO;
            for (final Map<String, Object> error : errors) {
                throw new AccountInvalidException(error.get("message").toString());
            }
        }
    }

    private String getToken(final Account account) {
        return account.getStringProperty(PROPERTY_ACCOUNT_TOKEN);
    }

    private void accessAccountInfoIfNotAlreadyAccessed(final Browser br, final Account account) throws IOException, PluginException {
        if (!br.containsHTML("getUserInfo3")) {
            prepBRGraphQL(br);
            br.postPageRaw(API_BASE_GRAPHQL, "{\"operationName\":\"GetUserInfo3\",\"variables\":{\"Token\":\"" + this.getToken(account) + "\"},\"query\":\"query GetUserInfo3($Token: String!) {    getUserInfo3(Token: $Token) {      ID      FirstName      LastName      Email      AvatarURL      CountryCode      LanguageCode      Phone      AccountType      UsedQuota      TotalQuota      OAuth2Provider      GPMigrationStatus    }  }\"}");
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(this.br, account, true);
        accessAccountInfoIfNotAlreadyAccessed(this.br, account);
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        final Map<String, Object> data = (Map<String, Object>) entries.get("data");
        final Map<String, Object> userinfo = (Map<String, Object>) data.get("getUserInfo3");
        ai.setUsedSpace(JavaScriptEngineFactory.toLong(userinfo.get("UsedQuota"), 0));
        ai.setUnlimitedTraffic();
        /* 1 = Free Account, 2 = Pro Account, 3 = Ultimate */
        final int accType = ((Number) userinfo.get("AccountType")).intValue();
        if (accType == 1) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
        } else if (accType == 2) {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setStatus("Pro Account");
        } else if (accType == 3) {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setStatus("Ultimate Account");
        } else {
            /* This should never happen */
            account.setType(AccountType.UNKNOWN);
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (this.attemptStoredDownloadurlDownload(link, PROPERTY_DIRECTURL_ACCOUNT, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS)) {
            logger.info("Re-using stored directurl");
            dl.startDownload();
        } else {
            requestFileInformation(link);
            final Browser brAPI = br.cloneBrowser();
            this.login(brAPI, account, false);
            this.prepBRGraphQL(brAPI);
            brAPI.postPageRaw(API_BASE_GRAPHQL, "{\"operationName\":\"GetOverlay4\",\"variables\":{\"Token\":\"" + this.getToken(account) + "\",\"ID\":{\"FileID\":\"" + this.getFileID(link)
                    + "\"}},\"query\":\"query GetOverlay4($Token: String!, $ID: IDType!) {    getOverlay4(Token: $Token, ID: $ID) {      ID      MetadataID      UserID      DeviceID      MetadataKey      Name      FilePath      LocalPath      LastUploadTime      LastModificationTime      ParentID      Category      Size      Platform      URL      ThumbnailURL      CreationTime      IsSelfLiked      Likes      Comments      IsHidden      IsInRecycleBin      Description      Location {        Country        Province        Place        GeoLocation {          Latitude          Longitude        }      }      Location2 {        Country        Region        SubRegion        Municipality        Neighborhood        GeoLocation {          Latitude          Longitude        }      }      Data      DataBlock      CompressionParameters      Shareinfo {        Status        ShareTime      }      HasViewed      QualityScore    }  }\"}");
            // this.checkErrorsAPI(this.br, account);
            final Map<String, Object> entries = restoreFromString(brAPI.toString(), TypeRef.MAP);
            this.dllink = JavaScriptEngineFactory.walkJson(entries, "data/getOverlay4/URL").toString();
            if (!StringUtils.isEmpty(this.dllink)) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    br.followConnection(true);
                    checkDownloadErrorsLastResort(dl.getConnection());
                }
                link.setProperty(PROPERTY_DIRECTURL_ACCOUNT, dl.getConnection().getURL().toString());
                dl.startDownload();
            } else {
                /* For text files they may return the full content of a file as base64 encoded text right away. */
                final String b64EncodedData = JavaScriptEngineFactory.walkJson(entries, "data/getOverlay4/Data").toString();
                if (StringUtils.isEmpty(b64EncodedData)) {
                    /* 2021-08-16: Don't use PLUGIN_DEFECT LinkStatus here as we're using an API which is supposed to be fairly stable. */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadurl");
                }
                final String text = Encoding.Base64Decode(b64EncodedData);
                /* Write text to file */
                dl = new TextDownloader(this, link, text);
                dl.startDownload();
            }
        }
    }

    private void checkDownloadErrorsLastResort(final URLConnectionAdapter con) throws PluginException {
        if (con.getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
        } else if (con.getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
        } else if (con.getResponseCode() == 429) {
            /**
             * 2021-01-17: Plaintext response: "Rate Limit" </br>
             * This limit sits on the files themselves and/or the uploader account. There is no way to bypass this by reconnecting! </br>
             * Displayed error on website: "Daily limit reached, upgrade to increase this limit or wait until tomorrow"
             */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Daily limit reached");
        } else {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error");
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        /* 2020-07-21: No captchas at all */
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}