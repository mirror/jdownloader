package jd.plugins.hoster;

import org.jdownloader.plugins.components.putio.PutIOFileWrapper;
import org.jdownloader.plugins.components.putio.PutIOInfoWrapper;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;

import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.requests.GetRequest;
import jd.http.requests.HeadRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
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
import jd.plugins.download.HashInfo;
import jd.plugins.download.HashInfo.TYPE;

// "https?://put\\.io/(?:file|v2/files)/\\d+" website link
// actual downloadlink "https?://put\\.io/v2/files/\\d+/download\\?token=[a-fA-F0-9]+"
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "put.io" }, urls = { "https?://(?:[a-z0-9\\-]+\\.)?put\\.io/(?:(?:v2/)?files/\\d+/download|zipstream/\\d+.*)\\?oauth_token=[A-Z0-9]+" })
public class PutIO extends PluginForHost {

    private static final String REQUIRES_ACCOUNT = "requiresAccount";
    private static final String ACCESS_TOKEN     = "access_token";
    private static final String COOKIE_HOST      = "http://put.io";
    private static final String SESSION_TOKEN_2  = "session2";
    public static final long    trust_cookie_age = 300000l;
    private String              accessToken;

    public PutIO(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("https://put.io");
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null) {
            // freemode is possible if the link has a token
            return StringUtils.isNotEmpty(downloadLink.getPluginPatternMatcher());
        }
        if (downloadLink != null) {
            final String user = downloadLink.getStringProperty(REQUIRES_ACCOUNT, null);
            if (user != null) {
                return StringUtils.equalsIgnoreCase(user, account.getUser());
            }
        }
        return super.canHandle(downloadLink, account);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        final String date_created_str = PluginJSonUtils.getJson(this.br, "created_at");
        final String date_expire_str = PluginJSonUtils.getJson(this.br, "plan_expiration_date");
        long date_expire = 0;
        if (!StringUtils.isEmpty(date_created_str)) {
            ai.setCreateTime(TimeFormatter.getMilliSeconds(date_created_str, "yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH));
        }
        if (!StringUtils.isEmpty(date_expire_str)) {
            date_expire = TimeFormatter.getMilliSeconds(date_expire_str, "yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        }
        if (date_expire - System.currentTimeMillis() > 0) {
            ai.setValidUntil(date_expire);
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "https://put.io/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(true);
        final String accessToken = getToken(link.getPluginPatternMatcher());
        if (StringUtils.isNotEmpty(accessToken)) {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, "https://api.put.io/v2/files/" + getID(link.getPluginPatternMatcher()) + "/download?oauth_token=" + accessToken, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
            return;
        }
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        String finalLink = null;
        if (isZipStreamURL(link.getPluginPatternMatcher())) {
            finalLink = link.getPluginPatternMatcher();
        } else {
            final String id = getID(link.getPluginPatternMatcher());
            final String linkToken = getToken(link.getPluginPatternMatcher());
            Request request = null;
            br.setFollowRedirects(false);
            br.setAllowedResponseCodes(200, 302, 401);
            login(account, false);
            br.getPage(request = new HeadRequest(finalLink = createDownloadUrl(id, accessToken)));
            if (request.getHttpConnection().getResponseCode() != 302) {
                if (StringUtils.isEmpty(linkToken)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    // try with the linkToken
                    br.getPage(request = new HeadRequest(finalLink = createDownloadUrl(id, linkToken)));
                    if (request.getHttpConnection().getResponseCode() != 302) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                }
            }
        }
        if (finalLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, finalLink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String createDownloadUrl(String id, String token) throws Exception {
        if (id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (token == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return "https://api.put.io/v2/files/" + id + "/download?oauth_token=" + token;
        }
    }

    @Override
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final String access_token = account.getStringProperty(ACCESS_TOKEN);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force && StringUtils.isNotEmpty(access_token) && System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age) {
                    this.br.setCookies(this.getHost(), cookies);
                    this.accessToken = access_token;
                    setAccessTokenHeader(this.accessToken);
                    return;
                }
                br = new Browser();
                br.setFollowRedirects(true);
                br.getPage("https://put.io/");
                // login page maybe we should look and open this request
                // this request will redirect
                br.getPage("https://api.put.io/v2/oauth2/authenticate?client_id=1&response_type=token&redirect_uri=https://app.put.io/");
                // should be a form.
                final Form login = br.getForm(0);
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("name", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br.setFollowRedirects(false);
                br.submitForm(login);
                // second redirect is the token
                br.followRedirect(false);
                final String location = br.getRequest().getResponseHeader("Location");
                accessToken = new Regex(location, "[&\\?#]access_token=([^\\&=]+)").getMatch(0);
                if (accessToken == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                setAccessTokenHeader(this.accessToken);
                br.getPage("https://api.put.io/v2/account/info?download_token=1&sharing=1&intercom=1&plan=1&features=1");
                // String infoJson = br.getPage("https://put.io/v2/account/info?access_token=1&intercom=1&sharing=1");
                final PutIOInfoWrapper map = JSonStorage.restoreFromString(br.toString(), new TypeRef<PutIOInfoWrapper>() {
                });
                if (map == null || map.getInfo() == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final String cookie = br.getCookie(COOKIE_HOST, SESSION_TOKEN_2);
                if (cookie == null || "deleted".equals(cookie)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.setProperty(ACCESS_TOKEN, accessToken);
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private void setAccessTokenHeader(final String accessToken) {
        br.getHeaders().put("Authorization", "token " + accessToken);
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("Referer", "https://app.put.io/files");
        br.getHeaders().put("Origin", "https://app.put.io");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String url = link.getPluginPatternMatcher();
        final boolean isZipStreamURL = isZipStreamURL(url);
        link.setProperty(REQUIRES_ACCOUNT, null);
        Account lastValidAccount = null;
        // first try accounts
        for (final Account account : AccountController.getInstance().getValidAccounts(getHost())) {
            try {
                login(account, false);
                lastValidAccount = account;
                if (isZipStreamURL) {
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(false);
                    Request request = new HeadRequest(url);
                    brc.getPage(request);
                    if (request.getHttpConnection().getResponseCode() == 200 && request.getHttpConnection().isContentDisposition()) {
                        final String filename = getFileNameFromDispositionHeader(request.getHttpConnection());
                        final long size = request.getHttpConnection().getCompleteContentLength();
                        link.setFinalFileName(filename);
                        link.setDownloadSize(size);
                        link.setProperty(REQUIRES_ACCOUNT, account.getUser());
                        return AvailableStatus.TRUE;
                    } else {
                        request = new GetRequest(url);
                        brc.getPage(request);
                        if (request.getHttpConnection().getResponseCode() == 404) {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                } else {
                    final String id = getID(url);
                    final String infoJson = br.getPage("https://api.put.io/v2/files/" + id + "?mp4_size=1&start_from=1&stream_url=1");
                    if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    if (!br.getRequest().getHttpConnection().isOK()) {
                        continue;
                    }
                    final PutIOFileWrapper fileInfo = JSonStorage.restoreFromString(infoJson, new TypeRef<PutIOFileWrapper>() {
                    });
                    if (fileInfo == null || fileInfo.getFile() == null || !"OK".equals(fileInfo.getStatus())) {
                        continue;
                    }
                    final String filename = fileInfo.getFile().getName();
                    final long filesize = fileInfo.getFile().getSize();
                    if (filename == null) {
                        continue;
                    }
                    link.setHashInfo(HashInfo.newInstanceSafe(fileInfo.getFile().getCrc32(), TYPE.CRC32));
                    link.setFinalFileName(filename.trim());
                    link.setDownloadSize(filesize);
                    link.setProperty(REQUIRES_ACCOUNT, account.getUser());
                    return AvailableStatus.TRUE;
                }
            } catch (Throwable e) {
                logger.log(e);
            }
        }
        final String token = getToken(url);
        if (StringUtils.isNotEmpty(token) && !isZipStreamURL) {
            final String id = getID(url);
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(false);
            brc.getPage(createDownloadUrl(id, token));
            if (brc.getHttpConnection().getResponseCode() == 401) {
                /* Account required */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
            final String redirect = brc.getRedirectLocation();
            if (redirect == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            Request request = new HeadRequest(redirect);
            brc.getPage(request);
            if (request.getHttpConnection().getResponseCode() == 200 && request.getHttpConnection().isContentDisposition()) {
                final String filename = getFileNameFromDispositionHeader(request.getHttpConnection());
                final long size = request.getHttpConnection().getCompleteContentLength();
                link.setFinalFileName(filename);
                link.setDownloadSize(size);
                return AvailableStatus.TRUE;
            } else if (request.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                request = new GetRequest(url);
                brc.getPage(request);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else if (lastValidAccount == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    private String getToken(String url) {
        return new Regex(url, "token=([a-fA-f0-9]+)").getMatch(0);
    }

    private boolean isZipStreamURL(final String url) {
        return StringUtils.containsIgnoreCase(url, "/zipstream/");
    }

    private String getID(String url) throws PluginException {
        final String id = new Regex(url, "files/(\\d+)").getMatch(0);
        if (id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return id;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
