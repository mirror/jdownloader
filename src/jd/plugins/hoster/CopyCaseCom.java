package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
import jd.http.requests.GetRequest;
import jd.http.requests.PostRequest;
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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class CopyCaseCom extends PluginForHost {
    public CopyCaseCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://copycase.com/pricing");
    }

    @Override
    public String getAGBLink() {
        return "https://copycase.com/page/en-US/terms";
    }

    private final String PROPERTY_ACCOUNT_TOKEN = "token";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "copycase.com" });
        /* This is their own public test domain. */
        ret.add(new String[] { "uxlo.com" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/((?:file|download)/[a-zA-Z0-9]{16}(/([^/]+))?|folder/[a-zA-Z0-9]+/file/[a-zA-Z0-9]{16})");
        }
        return ret.toArray(new String[0]);
    }

    public String getAPIBase() {
        return "https://" + this.getHost() + "/api/v1";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String folderID = this.getFolderID(link);
        final String fid = getFileID(link);
        if (folderID != null) {
            /*
             * 2023-03-02: At this moment we do not know if fileIDs are unique for themselves or unique within each folder so if we know
             * that a file is part of a folder, we incoprperate the folderID in our unique LinkID.
             */
            return this.getHost() + "://" + folderID + "_" + fid;
        } else if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    /** Returns a value if this file was added in cointext/as part of a folder. */
    private String getFolderID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "/folder/([a-zA-Z0-9]+)").getMatch(0);
    }

    private String getFileID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "/(?:file|download)/([a-zA-Z0-9]{16})").getMatch(0);
    }

    private String getAPIPathFile(final DownloadLink link) {
        final String folderID = this.getFolderID(link);
        final String fileID = this.getFileID(link);
        if (folderID != null) {
            return "/file-folders/" + folderID + "/file/" + fileID;
        } else {
            return "/file/" + fileID;
        }
    }

    private Browser prepBrowserAPI(final Browser br) {
        br.getHeaders().put("Accept", "application/json");
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(429);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        final String fileID = getFileID(link);
        if (fileID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!link.isNameSet()) {
            /* Set fallback-filename */
            final String urlFileName = new Regex(link.getPluginPatternMatcher(), "/(?:file|download)/[a-zA-Z0-9]{16}/([^/]+)").getMatch(2);
            if (urlFileName != null) {
                link.setName(URLEncode.decodeURIComponent(urlFileName));
            } else {
                link.setName(fileID);
            }
        }
        this.prepBrowserAPI(br);
        this.callAPI(br, link, null, new GetRequest(this.getAPIBase() + getAPIPathFile(link)), true);
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> data = (Map<String, Object>) entries.get("data");
        link.setFinalFileName(data.get("name").toString());
        link.setVerifiedFileSize(((Number) data.get("size")).longValue());
        final String error = (String) entries.get("error");
        if (StringUtils.equalsIgnoreCase(error, "password_required")) {
            link.setPasswordProtected(true);
        } else {
            link.setPasswordProtected(false);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    private int getMaxChunks(final Account account) {
        /* Last adjusted: 2023-03-02 */
        if (account != null && account.getType() == AccountType.PREMIUM) {
            return -8;
        } else {
            return 1;
        }
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        if (account != null) {
            this.login(br, account, false);
        }
        String passCode = link.getDownloadPassword();
        if (link.isPasswordProtected() && passCode == null) {
            /* Ask user for password */
            passCode = getUserInput("Password?", link);
        }
        final Map<String, Object> postData = new HashMap<String, Object>();
        /* Null value is allowed here! */
        postData.put("password", passCode);
        final PostRequest req = br.createPostRequest(this.getAPIBase() + getAPIPathFile(link) + "/download", JSonStorage.serializeToJson(postData));
        final Map<String, Object> resp = this.callAPI(br, link, account, req, true);
        final Map<String, Object> error_info = (Map<String, Object>) resp.get("error_info");
        if (error_info != null) {
            final String errorID = error_info.get("id").toString();
            if (errorID.equalsIgnoreCase("time")) {
                final Map<String, Object> error_data = (Map<String, Object>) error_info.get("data");
                final Number timeSeconds = (Number) error_data.get("time");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorID, timeSeconds.longValue() * 1001l);
            } else {
                /* Unknown error */
                throw new PluginException(LinkStatus.ERROR_FATAL, errorID);
            }
        }
        if (passCode != null) {
            /* User entered correct password -> Save it */
            link.setDownloadPassword(passCode);
        }
        final String directurl = resp.get("redirect").toString();
        if (StringUtils.isEmpty(directurl)) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, true, getMaxChunks(account));
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server returned html code instead of file");
        }
        dl.startDownload();
    }

    public Map<String, Object> callAPI(final Browser br, final Object link, final Account account, final Request req, final boolean checkErrors) throws IOException, PluginException {
        br.getPage(req);
        if (checkErrors) {
            return checkErrorsAPI(br, link, account);
        } else {
            return restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        }
    }

    public Map<String, Object> login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            prepBrowserAPI(br);
            final String storedToken = account.getStringProperty(PROPERTY_ACCOUNT_TOKEN);
            if (storedToken != null) {
                logger.info("Attempting cookie login");
                br.getHeaders().put("Authorization", "Bearer " + storedToken);
                if (!force) {
                    /* Do not verify token */
                    return null;
                }
                try {
                    final Map<String, Object> resp = getAccountInfo(br, account);
                    logger.info("Token login successful");
                    return resp;
                } catch (final PluginException exc) {
                    logger.info("Token login failed");
                    account.removeProperty(PROPERTY_ACCOUNT_TOKEN);
                }
            }
            logger.info("Performing full login");
            final Map<String, Object> postData = new HashMap<String, Object>();
            postData.put("email", account.getUser());
            postData.put("password", account.getPass());
            final Browser brc = br.cloneBrowser();
            brc.setAllowedResponseCodes(422);
            Map<String, Object> resp = this.callAPI(brc, null, account, brc.createPostRequest(getAPIBase() + "/auth/login", JSonStorage.serializeToJson(postData)), true);
            boolean required2FALogin = false;
            if (StringUtils.equalsIgnoreCase((String) resp.get("status"), "two_factor_auth")) {
                /* Ask user for 2FA login code and try again. */
                logger.info("2FA code required");
                required2FALogin = true;
                final DownloadLink dl_dummy;
                if (this.getDownloadLink() != null) {
                    dl_dummy = this.getDownloadLink();
                } else {
                    dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + this.getHost(), true);
                }
                String twoFACode = getUserInput("Enter Google 2-Factor authentication code", dl_dummy);
                if (twoFACode != null) {
                    twoFACode = twoFACode.trim();
                }
                if (twoFACode == null || !twoFACode.matches("\\d{6}")) {
                    throw new AccountInvalidException("Invalid 2-factor-authentication code format!");
                }
                logger.info("Submitting 2FA code");
                postData.put("auth_code", twoFACode);
                resp = this.callAPI(brc, null, account, brc.createPostRequest(getAPIBase() + "/auth/login", JSonStorage.serializeToJson(postData)), true);
                // Can return: {"status":"incorrect_auth_code"}
            }
            /* No exception -> Looks like login was successful */
            final String token = (String) resp.get("token");
            if (StringUtils.isEmpty(token)) {
                if (required2FALogin) {
                    throw new AccountInvalidException("Invalid 2-factor-authentication code!");
                } else {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            account.setProperty(PROPERTY_ACCOUNT_TOKEN, token);
            br.getHeaders().put("Authorization", "Bearer " + token);
            return resp;
        }
    }

    private Map<String, Object> getAccountInfo(final Browser br, final Account account) throws IOException, PluginException {
        return this.callAPI(br, null, account, br.createGetRequest(getAPIBase() + "/account"), true);
    }

    private Map<String, Object> checkErrorsAPI(final Browser br, final Object link, final Account account) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 429) {
            /* {"message":"Too Many Attempts."} */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "429 Rate Limit reached", 30 * 1000l);
        }
        final Map<String, Object> resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Object data = resp.get("data");
        final String error = (String) resp.get("error");
        final Object errors = resp.get("errors");
        final String status = (String) resp.get("status");
        // final String message = (String) resp.get("message");
        if (data == null && (error != null || errors != null) || StringUtils.equalsIgnoreCase(status, "failed")) {
            if (link != null) {
                if (br.getHttpConnection().getResponseCode() == 403) {
                    if (link instanceof DownloadLink) {
                        ((DownloadLink) link).setDownloadPassword(null);
                    }
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } else {
                /* Mostly along with http response 422 */
                /*
                 * If user enters invalid type of logins e.g. non-email address:
                 * {"message":"The given data was invalid.","errors":{"email":[{"code":"email","data":[]}]}}
                 */
                /* Invalid logins: {"status":"failed"} */
                throw new AccountInvalidException(error);
            }
        }
        return resp;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        Map<String, Object> usermap = login(br, account, true);
        if (br.getURL() == null || !br.getURL().endsWith("/account")) {
            usermap = this.getAccountInfo(br, account);
        }
        final Map<String, Object> user = (Map<String, Object>) usermap.get("me");
        final String accounType = user.get("type").toString();
        final String premium_expire_at = (String) user.get("premium_expire_at");
        final Number download_available_transfer = (Number) user.get("download_available_transfer");
        final Number download_transfer_limit = (Number) user.get("download_transfer_limit");
        ai.setUnlimitedTraffic();
        if (accounType.equalsIgnoreCase("free")) {
            account.setType(AccountType.FREE);
        } else {
            /* Not all premium accounts have an expire date. */
            if (premium_expire_at != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(premium_expire_at, "yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ENGLISH));
            }
            account.setType(AccountType.PREMIUM);
            final String premium_package = (String) user.get("premium_package");
            if (!StringUtils.isEmpty(premium_package)) {
                ai.setStatus(account.getType().getLabel() + " | " + premium_package);
            }
        }
        if (download_available_transfer != null) {
            ai.setTrafficLeft(download_available_transfer.longValue());
        }
        if (download_transfer_limit != null) {
            ai.setTrafficMax(download_transfer_limit.longValue());
        } else {
            ai.setUnlimitedTraffic();
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        /* 2023-02-28: No captchas at all */
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 4;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
