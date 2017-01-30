package jd.plugins.hoster;

import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.requests.HeadRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.HashInfo;
import jd.plugins.download.HashInfo.TYPE;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.appwork.utils.parser.UrlQuery;

//"https?://put\\.io/(?:file|v2/files)/\\d+" website link
//actuall downloadlink "https?://put\\.io/v2/files/\\d+/download\\?token=[a-fA-F0-9]+"
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "put.io" }, urls = { "https?://put\\.io/(files/\\d+|v2/files/\\d+/download\\?token=[a-fA-F0-9]+)" })
public class PutIO extends PluginForHost {

    private static final String REQUIRES_ACCOUNT    = "requiresAccount";

    private static final String X_PUTIO_LOGIN_TOKEN = "X-Putio-LoginToken";

    private static class File implements Storable {
        private String crc32;

        private String name;

        private long   size;

        private File(/* storable */) {

        }

        public String getCrc32() {
            return crc32;
        }

        public String getName() {
            return name;
        }

        public long getSize() {
            return size;
        }

        public void setCrc32(String crc32) {
            this.crc32 = crc32;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    private static class FileWrapper implements Storable {
        private File   file;
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        private FileWrapper(/* storable */) {

        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }
    }

    private static class Info implements Storable {
        private String access_token;

        public Info(/* private static */) {
        }

        public String getAccess_token() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }
    }

    private static class InfoWrapper implements Storable {
        private Info info;

        public InfoWrapper(/* Storable */) {
        }

        public Info getInfo() {
            return info;
        }

        public void setInfo(Info info) {
            this.info = info;
        }
    }

    private static final String ACCESS_TOKEN2 = "access_token";

    private static final String COOKIE_HOST   = "http://put.io";
    private static Object       LOCK          = new Object();

    private static final String LOGIN_TOKEN22 = "login_token2";

    private String              accessToken;

    public PutIO(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("https://put.io");
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
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
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setStatus("Premium user");
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "https://put.io/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        final String accessToken = getToken(link.getPluginPatternMatcher());
        if (StringUtils.isNotEmpty(accessToken)) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, "https://put.io/v2/files/" + getID(link.getPluginPatternMatcher()) + "/download?token=" + accessToken, true, 0);
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
        String id = getID(link.getPluginPatternMatcher());
        String linkToken = getToken(link.getPluginPatternMatcher());
        HeadRequest head = null;
        String finalLink = null;

        br.setFollowRedirects(false);
        br.setAllowedResponseCodes(200, 302, 401);

        login(account, false);
        br.getPage(head = new HeadRequest(finalLink = createDownloadUrl(id, accessToken)));
        if (head.getHttpConnection().getResponseCode() != 302) {
            if (StringUtils.isEmpty(linkToken)) {

                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                // try with the linkToken
                finalLink = createDownloadUrl(id, linkToken);
                br.getPage(head = new HeadRequest(finalLink));
                if (head.getHttpConnection().getResponseCode() != 302) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, finalLink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String createDownloadUrl(String id, String token) {
        return "https://put.io/v2/files/" + id + "/download?token=" + token;
    }

    @Override
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                String access_token = account.getStringProperty(ACCESS_TOKEN2);
                String login_token2 = account.getStringProperty(LOGIN_TOKEN22);
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force && StringUtils.isNotEmpty(access_token) && StringUtils.isNotEmpty(login_token2)) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {

                        this.accessToken = access_token;
                        br.getHeaders().put(X_PUTIO_LOGIN_TOKEN, account.getStringProperty(LOGIN_TOKEN22));
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                String infoJson = br.postPage("https://put.io/login", new UrlQuery().append("next", "/v2/account/info?access_token=1&intercom=1&sharing=1", true).append("name", account.getUser(), true).append("password", account.getPass(), true));
                login_token2 = br.getCookie(br.getHost(), LOGIN_TOKEN22);

                br.getHeaders().put(X_PUTIO_LOGIN_TOKEN, login_token2);
                // String infoJson = br.getPage("https://put.io/v2/account/info?access_token=1&intercom=1&sharing=1");
                InfoWrapper map = JSonStorage.restoreFromString(infoJson, new TypeRef<InfoWrapper>() {
                });

                if (map == null || map.getInfo() == null || StringUtils.isEmpty(map.getInfo().getAccess_token()) || StringUtils.isEmpty(login_token2)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                accessToken = map.getInfo().getAccess_token();
                account.setProperty(ACCESS_TOKEN2, accessToken);
                account.setProperty(LOGIN_TOKEN22, login_token2);

                if (br.getCookie(COOKIE_HOST, LOGIN_TOKEN22) == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {

        // login(account, false);
        String url = link.getPluginPatternMatcher();
        link.setProperty(REQUIRES_ACCOUNT, null);
        String token = getToken(url);
        String id = getID(url);
        boolean triedAtLeastOneAccount = false;
        // first try accounts
        for (Account account : AccountController.getInstance().getValidAccounts(getHost())) {

            try {
                login(account, false);
                triedAtLeastOneAccount = true;
                String infoJson = br.getPage("https://put.io/v2/files/" + id + "?mp4_size=1&start_from=1&stream_url=1");
                if (!br.getRequest().getHttpConnection().isOK()) {
                    continue;
                }
                FileWrapper fileInfo = JSonStorage.restoreFromString(infoJson, new TypeRef<FileWrapper>() {
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

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        if (StringUtils.isNotEmpty(token)) {
            br.setFollowRedirects(false);
            br.getPage(createDownloadUrl(id, token));
            String redirect = br.getRedirectLocation();
            HeadRequest head = new HeadRequest(redirect);
            br.getPage(head);

            final String contentDisposition = head.getResponseHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_DISPOSITION);
            String filename = HTTPConnectionUtils.getFileNameFromDispositionHeader(contentDisposition);
            long size = head.getHttpConnection().getCompleteContentLength();
            if (br.containsHTML("File Not Found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }

            link.setFinalFileName(filename);
            link.setDownloadSize(size);
            return AvailableStatus.TRUE;

        } else if (!triedAtLeastOneAccount) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }

        return AvailableStatus.FALSE;
    }

    private String getToken(String url) {
        return new Regex(url, "token=([a-fA-f0-9]+)").getMatch(0);
    }

    private String getID(String url) {
        return new Regex(url, "files/(\\d+)").getMatch(0);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
