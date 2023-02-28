package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils.DispositionHeader;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
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
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            this.enablePremium("https://copycase.com/pricing");
        }
    }

    @Override
    public String getAGBLink() {
        return "https://copycase.com/page/en-US/terms";
    }

    // private final String FILE_PATTERN = "https?://[^/]+/file/([a-zA-Z0-9]{16})/?([^/]+)?";
    private String       freeDownloadURL        = null;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:file|download)/([a-zA-Z0-9]{16})(/[^/]+)?");
        }
        return ret.toArray(new String[0]);
    }

    private String getAPIBase() {
        return "https://" + this.getHost() + "/api/v1";
    }

    @Override
    public void clean() {
        freeDownloadURL = null;
        super.clean();
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
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        freeDownloadURL = null;
        final String fileID = getFID(link);
        if (fileID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!link.isNameSet()) {
            final String urlFileName = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
            if (urlFileName != null) {
                link.setName(URLEncode.decodeURIComponent(urlFileName));
            } else {
                link.setName(fileID);
            }
        }
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(false);
        brc.getPage("https://" + this.getHost() + "/file/" + fileID);
        final String downloadRedirect = brc.getRedirectLocation();
        if (downloadRedirect == null) {
            if (brc.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (brc.containsHTML("\"errors.not_found\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final URLConnectionAdapter con = brc.openHeadConnection(downloadRedirect);
        try {
            if (looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final DispositionHeader fileName = parseDispositionHeader(con);
                if (fileName != null && StringUtils.isNotEmpty(fileName.getFilename())) {
                    link.setFinalFileName(fileName.getFilename());
                }
                brc.followConnection();
                freeDownloadURL = downloadRedirect;
                return AvailableStatus.TRUE;
            } else {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                handleError(brc, link, con);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } finally {
            con.disconnect();
        }
    }

    private void handleError(final Browser br, DownloadLink link, final URLConnectionAdapter con) throws Exception {
        switch (con.getResponseCode()) {
        case 404:
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 429:
            // HTTP/1.1 429 Too Many Requests
            final DispositionHeader fileName = parseDispositionHeader(con);
            if (fileName != null && StringUtils.isNotEmpty(fileName.getFilename())) {
                link.setFinalFileName(fileName.getFilename());
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
        default:
            break;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        handleDownload(null, link, freeDownloadURL);
    }

    private int getMaxChunks(final Account account) {
        if (account == null || AccountType.FREE.equals(account.getType())) {
            return -4;
        } else {
            return -4;
        }
    }

    private void handleDownload(final Account account, final DownloadLink link, final String directurl) throws Exception {
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
            handleError(br, link, dl.getConnection());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private Browser prepBrowserAPI(final Browser br) {
        br.getHeaders().put("Accept", "application/json");
        return br;
    }

    private Map<String, Object> callAPI(final Browser br, final Request req, final boolean checkErrors) throws IOException, PluginException {
        br.getPage(req);
        if (checkErrors) {
            return checkErrorsAPI(br);
        } else {
            return restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        }
    }

    private Map<String, Object> login(final Browser br, final Account account, final boolean force) throws Exception {
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
            final Map<String, Object> resp = this.callAPI(br, req, true);
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
        return this.callAPI(br, br.createGetRequest(getAPIBase() + "/account"), true);
    }

    private Map<String, Object> checkErrorsAPI(final Browser br) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
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
        requestFileInformation(link);
        this.login(br, account, false);
        final Map<String, Object> postData = new HashMap<String, Object>();
        postData.put("password", link.getDownloadPassword());
        final PostRequest req = br.createPostRequest(this.getAPIBase() + "/account/files/" + this.getFID(link) + "/download", JSonStorage.serializeToJson(postData));
        final Map<String, Object> resp = this.callAPI(br, req, true);
        final Map<String, Object> error_info = (Map<String, Object>) resp.get("error_info");
        if (error_info != null) {
            final String errorID = error_info.get("id").toString();
            if (errorID.equalsIgnoreCase("time")) {
                final Map<String, Object> error_data = (Map<String, Object>) error_info.get("data");
                final Number time = (Number) error_data.get("time");
            } else {
                // TODO: Add errorhandling/wait handling
            }
        }
        final String directurl = resp.get("redirect").toString();
        if (StringUtils.isEmpty(directurl)) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.handleDownload(account, link, directurl);
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
