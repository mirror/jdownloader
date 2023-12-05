package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.HTTPHeader;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.HeadRequest;
import jd.http.requests.PutRequest;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "put.io" }, urls = { "https?://(?:[a-z0-9\\-]+\\.)?put\\.io/(?:(?:v2/)?files/\\d+/(mp4/download(/[^/]*)?|download(/[^/]*)?)|zipstream/\\d+.*?|download/\\d+.*)\\?oauth_token=[A-Z0-9]+.*" })
public class PutIO extends PluginForHost {
    private final String API_BASE      = "https://api.put.io/v2";
    private final String CLIENT_ID     = "181";
    private final String CLIENT_SECRET = "ga38bm4yv546pzauepok";

    public PutIO(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("https://put.io");
    }

    @Override
    public String getAGBLink() {
        return "https://put.io/tos";
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.getHeaders().put(new HTTPHeader("User-Agent", "jdownloader", false));
        br.getHeaders().put(new HTTPHeader("Accept", "application/json", false));
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        try {
            final String fileID = getUniqueFileID(link.getPluginPatternMatcher());
            return "put_io://file/" + fileID;
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return super.getLinkID(link);
    }

    /* Returns unique fileID. */
    private String getUniqueFileID(final String url) throws PluginException {
        final String id = new Regex(url, "(?i)/(files|download)/(\\d+)").getMatch(1);
        if (id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return id;
        }
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (link != null && this.isZipStreamURL(link.getPluginPatternMatcher())) {
            /* Such links can be checked and downloaded without account. */
            return true;
        } else if (account == null) {
            return false;
        } else if (link != null) {
            /* Check if given link is downloadable with given account. */
            final String user = link.getStringProperty("requires_account", null);
            if (user != null) {
                return StringUtils.equalsIgnoreCase(user, account.getUser());
            }
        }
        return super.canHandle(link, account);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final ArrayList<Account> accounts = AccountController.getInstance().getValidAccounts(getHost());
        if (accounts != null && accounts.size() > 0) {
            for (final Account account : accounts) {
                try {
                    return requestFileInformation(link, account);
                } catch (final PluginException e) {
                    if (e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                        throw e;
                    } else {
                        logger.log(e);
                    }
                }
            }
            throw new AccountRequiredException();
        } else {
            /* No account */
            return requestFileInformation(link, null);
        }
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (account == null && this.requiresAccount(link.getPluginPatternMatcher())) {
            throw new AccountRequiredException();
        }
        String access_tokenFromAccount = null;
        final String directurl;
        if (account != null) {
            access_tokenFromAccount = login(account);
            directurl = getDownloadURL(link, access_tokenFromAccount);
        } else {
            directurl = getDownloadURL(link, null);
        }
        final Request request = new HeadRequest(directurl);
        if (access_tokenFromAccount != null) {
            request.getHeaders().put(new HTTPHeader("Authorization", "token " + access_tokenFromAccount, false));
        }
        br.getPage(request);
        final URLConnectionAdapter connection = br.getHttpConnection();
        try {
            handleConnectionErrorsAndSetFileInfo(link, br, connection);
            /* No Exception = Success */
            if (account != null) {
                /* Store information that this link is downloadable with this account. */
                link.setProperty("requires_account", account.getUser());
            }
        } finally {
            connection.disconnect();
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, null);
        final String url = getDownloadURL(link, null);
        this.handleDownload(br, link, null, url);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final String access_token = login(account);
        final AccountInfo ai = new AccountInfo();
        if (br.getURL() == null || !br.getURL().contains("/account/info")) {
            br.getHeaders().put(new HTTPHeader("Authorization", "token " + access_token, false));
            br.getPage(API_BASE + "/account/info");
            if (br.getHttpConnection().getResponseCode() != 200) {
                throw new AccountUnavailableException("http error " + br.getHttpConnection().getResponseCode(), 5 * 60 * 1000);
            }
        }
        final Map<String, Object> infoResponse = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> info = (Map<String, Object>) infoResponse.get("info");
        final String dateExpireStr = (String) info.get("plan_expiration_date");
        final long dateExpire = TimeFormatter.getMilliSeconds(dateExpireStr, "yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        if (dateExpire - System.currentTimeMillis() > 0) {
            ai.setValidUntil(dateExpire);
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    private String getToken(final String url) {
        return new Regex(url, "(?i)oauth_token=([a-fA-f0-9]+)").getMatch(0);
    }

    private boolean isZipStreamURL(final String url) {
        return StringUtils.containsIgnoreCase(url, "/zipstream/");
    }

    private boolean requiresAccount(final String url) {
        if (isZipStreamURL(url)) {
            return false;
        } else {
            /* All other URL-types: Account is required to be able to download. */
            return true;
        }
    }

    @Override
    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter con) {
        return con.isOK() && (con.isContentDisposition() || !StringUtils.containsIgnoreCase(con.getContentType(), "text"));
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        final String access_token = login(account);
        final String url = getDownloadURL(link, access_token);
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("Authorization", "token " + access_token);
        handleDownload(brc, link, account, url);
    }

    private void handleDownload(final Browser br, final DownloadLink link, final Account account, final String directurl) throws Exception {
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, true, 0);
        handleConnectionErrorsAndSetFileInfo(link, br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrorsAndSetFileInfo(final DownloadLink link, final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File broken?");
            }
        }
        link.setVerifiedFileSize(con.getCompleteContentLength());
        if (con.isContentDisposition()) {
            link.setFinalFileName(getFileNameFromDispositionHeader(con));
        }
    }

    @Override
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    private String login(final Account account) throws Exception {
        synchronized (account) {
            String access_token = account.getStringProperty("access_token", null);
            if (!StringUtils.isEmpty(access_token)) {
                logger.info("Trying to re-use saved access_token");
                br.getHeaders().put(new HTTPHeader("Authorization", "token " + access_token, false));
                br.getPage(API_BASE + "/account/info");
                if (br.getHttpConnection().getResponseCode() == 200) {
                    logger.info("Successfully re-used access_token");
                    return access_token;
                } else {
                    logger.info("Failed to re-use access_token --> Full login required");
                }
            }
            logger.info("Performing full login");
            final PutRequest authRequest = new PutRequest(API_BASE + "/oauth2/authorizations/clients/" + CLIENT_ID + "?client_secret=" + CLIENT_SECRET);
            final String credentials = account.getUser() + ":" + account.getPass();
            final String auth = org.appwork.utils.encoding.Base64.encodeToString(credentials.getBytes("UTF-8"));
            authRequest.getHeaders().put(new HTTPHeader("Authorization", "Basic " + auth, false));
            authRequest.getHeaders().put(new HTTPHeader("User-Agent", "jdownloader", false));
            authRequest.getHeaders().put(new HTTPHeader("Accept", "application/json", false));
            br.getPage(authRequest);
            final int responseCode = br.getHttpConnection().getResponseCode();
            if (responseCode != 200) {
                throw new AccountUnavailableException("http error " + br.getHttpConnection().getResponseCode(), 5 * 60 * 1000);
            }
            final Map<String, Object> authResponse = restoreFromString(br.toString(), TypeRef.HASHMAP);
            access_token = (String) authResponse.get("access_token");
            if (StringUtils.isEmpty(access_token)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            account.setProperty("access_token", access_token);
            return access_token;
        }
    }

    private String getDownloadURL(final DownloadLink link, final String access_token) throws PluginException {
        final String finalLink;
        if (isZipStreamURL(link.getPluginPatternMatcher())) {
            finalLink = link.getPluginPatternMatcher();
        } else {
            final String id = getUniqueFileID(link.getPluginPatternMatcher());
            // final String linkToken = getToken(link.getPluginPatternMatcher());
            if (id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (access_token == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            finalLink = API_BASE + "/files/" + id + "/download?oauth_token=" + access_token;
        }
        if (finalLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return finalLink;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}