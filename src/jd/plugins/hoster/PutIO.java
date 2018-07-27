package jd.plugins.hoster;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.http.requests.HeadRequest;
import jd.http.requests.PutRequest;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
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
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.HTTPHeader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "put.io" }, urls = { "https?://(?:[a-z0-9\\-]+\\.)?put\\.io/(?:(?:v2/)?files/\\d+/(mp4/download|download)|zipstream/\\d+.*|download/\\d+.*)\\?oauth_token=[A-Z0-9]+" })
public class PutIO extends PluginForHost {
    private final String CLIENT_ID     = "181";
    private final String CLIENT_SECRET = "ga38bm4yv546pzauepok";

    public PutIO(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("https://put.io");
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            if (downloadLink != null) {
                final String user = downloadLink.getStringProperty("requires_account", null);
                if (user != null) {
                    return StringUtils.equalsIgnoreCase(user, account.getUser());
                }
            }
            return super.canHandle(downloadLink, account);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final String access_token = login(account);
        final AccountInfo ai = new AccountInfo();
        final GetRequest infoRequest = new GetRequest("https://api.put.io/v2/account/info");
        infoRequest.getHeaders().put(new HTTPHeader("Authorization", "token " + access_token, false));
        infoRequest.getHeaders().put(new HTTPHeader("User-Agent", "jdownloader", false));
        infoRequest.getHeaders().put(new HTTPHeader("Accept", "application/json", false));
        br.setFollowRedirects(true);
        br.getPage(infoRequest);
        final int responseCode = br.getHttpConnection().getResponseCode();
        if (responseCode != 200) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final HashMap<String, Object> infoResponse = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP, null);
        final HashMap<String, Object> info = (HashMap<String, Object>) infoResponse.get("info");
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

    @Override
    public String getAGBLink() {
        return "https://put.io/tos";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private String getToken(String url) {
        return new Regex(url, "token=([a-fA-f0-9]+)").getMatch(0);
    }

    private boolean isZipStreamURL(final String url) {
        return StringUtils.containsIgnoreCase(url, "/zipstream/");
    }

    private String getID(String url) throws PluginException {
        final String id = new Regex(url, "(files|download)/(\\d+)").getMatch(1);
        if (id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return id;
        }
    }

    private boolean isDownload(URLConnectionAdapter con) {
        return con.isOK() && (con.isContentDisposition() || !StringUtils.containsIgnoreCase(con.getContentType(), "text"));
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        final String access_token = login(account);
        final String url = getDownloadURL(link, access_token);
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        brc.getHeaders().put("Authorization", "token " + access_token);
        dl = jd.plugins.BrowserAdapter.openDownload(brc, link, url, true, 0);
        final int responseCode = dl.getConnection().getResponseCode();
        if (isDownload(dl.getConnection())) {
            dl.startDownload();
        } else if (responseCode == 404) {
            try {
                br.followConnection();
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            try {
                br.followConnection();
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    private String login(final Account account) throws Exception {
        synchronized (account) {
            final PutRequest authRequest = new PutRequest("https://api.put.io/v2/oauth2/authorizations/clients/" + CLIENT_ID + "?client_secret=" + CLIENT_SECRET);
            final String credentials = account.getUser() + ":" + account.getPass();
            final String auth = org.appwork.utils.encoding.Base64.encodeToString(credentials.getBytes("UTF-8"));
            authRequest.getHeaders().put(new HTTPHeader("Authorization", "Basic " + auth, false));
            authRequest.getHeaders().put(new HTTPHeader("User-Agent", "jdownloader", false));
            authRequest.getHeaders().put(new HTTPHeader("Accept", "application/json", false));
            br.setFollowRedirects(true);
            br.getPage(authRequest);
            final int responseCode = br.getHttpConnection().getResponseCode();
            if (responseCode == 200) {
                final HashMap<String, Object> authResponse = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP, null);
                final String access_token = (String) authResponse.get("access_token");
                if (access_token != null) {
                    account.setProperty("access_token", access_token);
                    return access_token;
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else if (responseCode == 401) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    private String getDownloadURL(final DownloadLink link, String access_token) throws PluginException {
        final String finalLink;
        if (isZipStreamURL(link.getPluginPatternMatcher())) {
            finalLink = link.getPluginPatternMatcher();
        } else {
            final String id = getID(link.getPluginPatternMatcher());
            // final String linkToken = getToken(link.getPluginPatternMatcher());
            if (id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (access_token == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            finalLink = "https://api.put.io/v2/files/" + id + "/download?oauth_token=" + access_token;
        }
        if (finalLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return finalLink;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        for (final Account account : AccountController.getInstance().getValidAccounts(getHost())) {
            try {
                final String access_token = login(account);
                final Request request = new HeadRequest(getDownloadURL(link, access_token));
                request.getHeaders().put(new HTTPHeader("Authorization", "token " + access_token, false));
                br.setFollowRedirects(true);
                br.getPage(request);
                final URLConnectionAdapter connection = br.getHttpConnection();
                final int responseCode = connection.getResponseCode();
                if (isDownload(connection)) {
                    link.setDownloadSize(connection.getCompleteContentLength());
                    link.setProperty("requires_account", account.getUser());
                    if (connection.isContentDisposition()) {
                        link.setFinalFileName(getFileNameFromDispositionHeader(connection));
                    }
                    return AvailableStatus.TRUE;
                } else if (responseCode == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                    throw e;
                }
                logger.log(e);
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}