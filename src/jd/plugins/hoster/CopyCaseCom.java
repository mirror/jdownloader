package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.TimeFormatter;

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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:file|download)/([a-zA-Z0-9]{16})(/([^/]+))?");
        }
        return ret.toArray(new String[0]);
    }

    public String getAPIBase() {
        return "https://" + this.getHost() + "/api/v1";
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        final String fileID = getFID(link);
        if (fileID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!link.isNameSet()) {
            final String urlFileName = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
            if (urlFileName != null) {
                link.setName(URLEncode.decodeURIComponent(urlFileName));
            } else {
                link.setName(fileID);
            }
        }
        this.prepBrowserAPI(br);
        this.callAPI(br, link, null, new GetRequest(this.getAPIBase() + "/files/" + fileID), true);
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
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
        if (account == null || AccountType.FREE.equals(account.getType())) {
            return -4;
        } else {
            return -4;
        }
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
        postData.put("password", passCode);
        final PostRequest req = br.createPostRequest(this.getAPIBase() + "/files/" + this.getFID(link) + "/download", JSonStorage.serializeToJson(postData));
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
        if (StringUtils.isEmpty(directurl)) {
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

    private Browser prepBrowserAPI(final Browser br) {
        br.getHeaders().put("Accept", "application/json");
        br.setFollowRedirects(true);
        return br;
    }

    public Map<String, Object> callAPI(final Browser br, final DownloadLink link, final Account account, final Request req, final boolean checkErrors) throws IOException, PluginException {
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
            // postData.put("auth_code", "");
            final PostRequest req = br.createPostRequest(getAPIBase() + "/auth/login", JSonStorage.serializeToJson(postData));
            final Map<String, Object> resp = this.callAPI(br, null, account, req, true);
            if (br.getHttpConnection().getResponseCode() == 422) {
                // TODO: Add 2FA login handling
            }
            /* No exception -> Looks like login was successful */
            final String token = (String) resp.get("token");
            if (StringUtils.isEmpty(token)) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            account.setProperty(PROPERTY_ACCOUNT_TOKEN, token);
            br.getHeaders().put("Authorization", "Bearer " + token);
            return resp;
        }
    }

    private Map<String, Object> getAccountInfo(final Browser br, final Account account) throws IOException, PluginException {
        return this.callAPI(br, null, account, br.createGetRequest(getAPIBase() + "/account"), true);
    }

    private Map<String, Object> checkErrorsAPI(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        final Map<String, Object> resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Object data = resp.get("data");
        final String error = (String) resp.get("error");
        if (data == null && error != null) {
            if (link != null) {
                if (br.getHttpConnection().getResponseCode() == 403) {
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            throw new AccountInvalidException(error);
        }
        return resp;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        Map<String, Object> usermap = login(br, account, true);
        if (!br.getURL().endsWith("/account")) {
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
