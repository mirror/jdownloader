//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.CustomStorageName;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.http.Browser;
import jd.http.QueryInfo;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.HashInfo;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "real-debrid.com" }, urls = { "https?://\\w+(\\.download)?\\.(?:real\\-debrid\\.com|rdb\\.so|rdeb\\.io)/dl?/\\w+/.+" }, flags = { 2 })
public class RealDebridCom extends PluginForHost {

    private static final String CLIENT_SECRET_KEY = "client_secret";
    private static final String CLIENT_ID_KEY     = "client_id";
    private static final String CLIENT_SECRET     = "CLIENT_SECRET";
    private static final String TOKEN             = "TOKEN";

    private static enum Errors {
        INTERNAL(-1, "Internal error"),
        MISSING_PARAMETER(1, "Missing parameter"),
        BAD_PARAMETER_VALUE(2, "Bad parameter value"),
        UNKNOWN_METHOD(3, "Unknown method"),
        METHOD_NOT_ALLOWED(4, "Method not allowed"),
        SLOW_DOWN(5, "Slow down"),
        RESOURCE_UNREACHABLE(6, "Ressource unreachable"),
        RESOURCE_NOT_FOUND(7, "Resource not found"),
        BAD_TOKEN(8, "Bad token"),
        PERMISSION_DENIED(9, "Permission denied"),
        AUTH_PENDING(10, "Authorization pending"),
        TWO_FACTOR_AUTH_REQUIRED(11, "Two-Factor authentication needed"),
        TWO_FACTOR_AUTH_PENDING(12, "Two-Factor authentication pending"),
        BAD_LOGIN(13, "Invalid login"),
        ACCOUNT_LOCKED(14, "Account locked"),
        ACCOUNT_NOT_ACTIVATED(15, "Account not activated"),
        UNSUPPORTED_HOSTER(16, "Unsupported hoster"),
        HOSTER_IN_MAINTENANCE(17, "Hoster in maintenance"),
        HOSTER_LIMIT_REACHED(18, "Hoster limit reached"),
        HOSTER_TEMP_UNAVAILABLE(19, "Hoster temporarily unavailable"),
        HOSTER_PREMIUM_ONLY(20, "Hoster not available for free users"),
        TOO_MANY_ACTIVE_DOWNLOADS(21, "Too many active downloads"),
        IP_ADRESS_FORBIDDEN(22, "IP Address not allowed"),
        TRAFFIC_EXHAUSTED(23, "Traffic exhausted"),
        FILE_UNAVAILABLE(24, "File unavailable"),
        SERVICE_UNAVAILABLE(25, "Service unavailable"),
        UPLOAD_TOO_BIG(26, "Upload too big"),
        UPLOAD_ERROR(27, "Upload error"),
        FILE_NOT_ALLOWED(28, "File not allowed"),
        TORRENT_TOO_BIG(29, "Torrent too big"),
        TORRENT_FILE_INVALID(30, "Torrent file invalid"),

        ACTION_ALREADY_DONE(31, "Action already done"),

        IMAGE_RESOLUTION_ERROR(32, "Image resolution error"),
        // DUmmy Code:
        UNKNOWN(-99, "Unknown Error ID");
        public static Errors getByCode(int id) {
            for (Errors e : values()) {
                if (id == e.code) {
                    return e;
                }
            }
            return UNKNOWN;
        };

        private int    code;
        private String msg;

        Errors(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

    }

    private static final String AUTHORIZATION = "Authorization";

    private static class Alternative implements Storable {

        private String download;
        private String filename;
        private String id;

        private String quality;

        public String getDownload() {
            return download;
        }

        public String getFilename() {
            return filename;
        }

        public String getId() {
            return id;
        }

        public String getQuality() {
            return quality;
        }

        public void setDownload(String download) {
            this.download = download;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setQuality(String quality) {
            this.quality = quality;
        }

    }

    private static class APIException extends Exception {

        public APIException(Errors error, String msg) {
            super(msg);
            this.error = error;
        }

        private Errors error;

        public Errors getError() {
            return error;
        }

    }

    private static class ClientSecret implements Storable {

        private String client_id;

        private String client_secret;

        public ClientSecret(/* Storable */) {
        }

        public String getClient_id() {
            return client_id;
        }

        public String getClient_secret() {
            return client_secret;
        }

        public void setClient_id(String client_id) {
            this.client_id = client_id;
        }

        public void setClient_secret(String client_secret) {
            this.client_secret = client_secret;
        }
    }

    private static class CodeResponse implements Storable {

        private String device_code;

        private int    expires_in;

        private int    interval;

        private String user_code;

        private String verification_url;

        public CodeResponse(/* Storable */) {
        }

        public String getDevice_code() {
            return device_code;
        }

        public int getExpires_in() {
            return expires_in;
        }

        public int getInterval() {
            return interval;
        }

        public String getUser_code() {
            return user_code;
        }

        public String getVerification_url() {
            return verification_url;
        }

        public void setDevice_code(String device_code) {
            this.device_code = device_code;
        }

        public void setExpires_in(int expires_in) {
            this.expires_in = expires_in;
        }

        public void setInterval(int interval) {
            this.interval = interval;
        }

        public void setUser_code(String user_code) {
            this.user_code = user_code;
        }

        public void setVerification_url(String verification_url) {
            this.verification_url = verification_url;
        }
    }

    private static class ErrorResponse implements Storable {

        private String error;

        private int    error_code;

        public ErrorResponse(/* Storable */) {
        }

        public String getError() {
            return error;
        }

        public int getError_code() {
            return error_code;
        }

        public void setError(String error) {
            this.error = error;
        }

        public void setError_code(int error_code) {
            this.error_code = error_code;
        }
    }

    private static class HostsResponse implements Storable {

        private String id;
        private String image;

        private String image_big;

        private String name;

        public HostsResponse(/* Storable */) {
        }

        public String getId() {
            return id;
        }

        public String getImage() {
            return image;
        }

        public String getImage_big() {
            return image_big;
        }

        public String getName() {
            return name;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public void setImage_big(String image_big) {
            this.image_big = image_big;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    private static class TokenResponse implements Storable {

        private String access_token;

        private long   createTime = System.currentTimeMillis();

        private int    expires_in;

        private String refresh_token;
        private String token_type;

        public TokenResponse(/* Storable */) {
        }

        public String getAccess_token() {
            return access_token;
        }

        public long getCreateTime() {
            return createTime;
        }

        public int getExpires_in() {
            return expires_in;
        }

        public String getRefresh_token() {
            return refresh_token;
        }

        public String getToken_type() {
            return token_type;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }

        public void setCreateTime(long createTime) {
            this.createTime = createTime;
        }

        public void setExpires_in(int expires_in) {
            this.expires_in = expires_in;
        }

        public void setRefresh_token(String refresh_token) {
            this.refresh_token = refresh_token;
        }

        public void setToken_type(String token_type) {
            this.token_type = token_type;
        }

        public boolean validate() {
            return StringUtils.isNotEmpty(access_token) && StringUtils.isNotEmpty(refresh_token);
        }

    }

    private static class UnrestrictLinkResponse implements Storable {

        private ArrayList<Alternative> alternative;
        private int                    chunks;
        private int                    crc;
        private String                 download;
        private String                 filename;
        private int                    filesize;

        private String                 host;

        private String                 id;

        private String                 link;

        private String                 quality;

        private int                    streamable;

        public ArrayList<Alternative> getAlternative() {
            return alternative;
        }

        public int getChunks() {
            return chunks;
        }

        public int getCrc() {
            return crc;
        }

        public String getDownload() {
            return download;
        }

        public String getFilename() {
            return filename;
        }

        public int getFilesize() {
            return filesize;
        }

        public String getHost() {
            return host;
        }

        public String getId() {
            return id;
        }

        public String getLink() {
            return link;
        }

        public String getQuality() {
            return quality;
        }

        public int getStreamable() {
            return streamable;
        }

        public void setAlternative(ArrayList<Alternative> alternative) {
            this.alternative = alternative;
        }

        public void setChunks(int chunks) {
            this.chunks = chunks;
        }

        public void setCrc(int crc) {
            this.crc = crc;
        }

        public void setDownload(String download) {
            this.download = download;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public void setFilesize(int filesize) {
            this.filesize = filesize;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public void setQuality(String quality) {
            this.quality = quality;
        }

        public void setStreamable(int streamable) {
            this.streamable = streamable;
        }

    }

    private static class UserResponse implements Storable {

        private String avatar;

        private String email;
        private String expiration;

        private int    id;

        private String locale;

        private int    points;

        private int    premium;

        private String type;

        private String username;

        public UserResponse(/* Storable */) {
        }

        public String getAvatar() {
            return avatar;
        }

        public String getEmail() {
            return email;
        }

        public String getExpiration() {
            return expiration;
        }

        public int getId() {
            return id;
        }

        public String getLocale() {
            return locale;
        }

        public int getPoints() {
            return points;
        }

        public int getPremium() {
            return premium;
        }

        public String getType() {
            return type;
        }

        public String getUsername() {
            return username;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setExpiration(String expiration) {
            this.expiration = expiration;
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setLocale(String locale) {
            this.locale = locale;
        }

        public void setPoints(int points) {
            this.points = points;
        }

        public void setPremium(int premium) {
            this.premium = premium;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    private static final String                            API                = "https://api.real-debrid.com";
    private static final String                            CLIENT_ID          = "NJ26PAPGHWGZY";
    // DEV NOTES
    // supports last09 based on pre-generated links and jd2 (but disabled with interfaceVersion 3)
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static Object                                  LOCK               = new Object();
    private static AtomicInteger                           MAX_DOWNLOADS      = new AtomicInteger(Integer.MAX_VALUE);
    private static AtomicInteger                           RUNNING_DOWNLOADS  = new AtomicInteger(0);

    private int                                            maxChunks          = 0;

    private final String                                   mName              = "real-debrid.com";

    private final String                                   mProt              = "https://";

    private Browser                                        apiBrowser;
    private RealDebridConfigPanel                          panel;
    private TokenResponse                                  token;
    private Account                                        account;

    public RealDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/");

        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
        Browser.setRequestIntervalLimitGlobal("rdb.so", 500);
        Browser.setRequestIntervalLimitGlobal("rdeb.io", 500);
    }

    private <T> T callRestAPI(String method, QueryInfo query, TypeRef<T> type) throws Exception {
        Request request;
        ensureAPIBrowser();
        login(account, false);
        try {
            return callRestAPIInternal(method, query, type);
        } catch (APIException e) {
            switch (e.getError()) {
            case BAD_LOGIN:
            case BAD_TOKEN:
                // refresh Token

                login(account, true);
                return callRestAPIInternal(method, query, type);
            default:
                throw e;
            }
        }
    }

    protected <T> T callRestAPIInternal(String method, QueryInfo query, TypeRef<T> type) throws IOException, APIException {
        Request request;
        apiBrowser.getHeaders().put(AUTHORIZATION, "Bearer " + token.getAccess_token());

        if (query != null) {
            request = apiBrowser.createPostRequest("https://api.real-debrid.com/rest/1.0" + method, query);
        } else {
            request = apiBrowser.createGetRequest("https://api.real-debrid.com/rest/1.0" + method);
        }
        String json = apiBrowser.getPage(request);
        if (request.getHttpConnection().getResponseCode() != 200) {
            if (json.trim().startsWith("{")) {
                ErrorResponse errorResponse = JSonStorage.restoreFromString(json, new TypeRef<ErrorResponse>(ErrorResponse.class) {
                });
                throw new APIException(Errors.getByCode(errorResponse.error_code), errorResponse.error);
            } else {
                throw new IOException("Unexpected Response: " + json);
            }
        }
        return JSonStorage.restoreFromString(json, type);
    }

    private void ensureAPIBrowser() {
        if (apiBrowser == null) {
            apiBrowser = br.cloneBrowser();
            for (int i = 200; i < 600; i++) {
                apiBrowser.addAllowedResponseCodes(i);
            }
        }
    }

    private <T> T callRestAPI(String method, TypeRef<T> type) throws Exception {
        return callRestAPI(method, null, type);

    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (isDirectLink(downloadLink)) {
            // generated links do not require an account to download
            return true;
        } else if (account == null) {
            // no non account handleMultiHost support.
            return false;
        } else {
            return true;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.account = account;
        login(account, true);
        account.setError(null, null);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(-1);

        UserResponse user = callRestAPI("/user", new TypeRef<UserResponse>(UserResponse.class) {
        });

        ai.setValidUntil(TimeFormatter.getTimestampByGregorianTime(user.getExpiration()));

        if ("premium".equalsIgnoreCase(user.getType())) {
            ai.setStatus("Premium User");
            account.setType(AccountType.PREMIUM);

        } else {
            account.setType(AccountType.FREE);

            ai.setProperty("multiHostSupport", Property.NULL);
            return ai;
        }
        HashMap<String, HostsResponse> hosts = callRestAPI("/hosts", new TypeRef<HashMap<String, HostsResponse>>() {
        });

        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (Entry<String, HostsResponse> es : hosts.entrySet()) {
            if (StringUtils.isNotEmpty(es.getKey())) {
                supportedHosts.add(es.getKey());
            }

        }
        ai.setMultiHostSupport(this, supportedHosts);

        return ai;
    }

    @Override
    public String getAGBLink() {
        return mProt + mName + "/terms";
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, final Account account) {
        return MAX_DOWNLOADS.get();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return MAX_DOWNLOADS.get();
    }

    private void handleDL(final Account acc, final DownloadLink link, final String dllink) throws Exception {
        // real debrid connections are flakey at times! Do this instead of repeating download steps.

        final String host = Browser.getHost(dllink);
        final DownloadLinkDownloadable downloadLinkDownloadable = new DownloadLinkDownloadable(link) {
            @Override
            public HashInfo getHashInfo() {

                return super.getHashInfo();
            }

            @Override
            public String getHost() {
                return host;
            }
        };
        final Browser br2 = br.cloneBrowser();
        boolean increment = false;
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br2, downloadLinkDownloadable, br2.createGetRequest(dllink), true, maxChunks);
            if (dl.getConnection().isContentDisposition() || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "octet-stream")) {
                /* content disposition, lets download it */
                RUNNING_DOWNLOADS.incrementAndGet();
                increment = true;
                boolean ret = dl.startDownload();
                if (ret && link.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                    // download is 100%
                    return;
                }

            }

        } finally {
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable t) {
            }
            if (increment && RUNNING_DOWNLOADS.decrementAndGet() == 0) {
                MAX_DOWNLOADS.set(Integer.MAX_VALUE);
            }
        }

    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        showMessage(link, "Task 1: Check URL validity!");
        final AvailableStatus status = requestFileInformation(link);
        if (AvailableStatus.UNCHECKABLE.equals(status)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 1000l);
        }
        showMessage(link, "Task 2: Download begins!");
        handleDL(account, link, link.getPluginPatternMatcher());
    }

    @Override
    public boolean hasConfig() {
        return true;
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.account = account;
        try {
            synchronized (hostUnavailableMap) {
                HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
                if (unavailableMap != null) {
                    Long lastUnavailable = unavailableMap.get(link.getHost());
                    if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                        final long wait = lastUnavailable - System.currentTimeMillis();
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost(), wait);
                    } else if (lastUnavailable != null) {
                        unavailableMap.remove(link.getHost());
                        if (unavailableMap.size() == 0) {
                            hostUnavailableMap.remove(account);
                        }
                    }
                }
            }

            prepBrowser(br);
            login(account, false);
            showMessage(link, "Task 1: Generating Link");
            /* request Download */
            String dllink = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
            UnrestrictLinkResponse linkresp = callRestAPI("/unrestrict/link", new QueryInfo().append("link", dllink, true).append("password", link.getStringProperty("pass", null), true), new TypeRef<UnrestrictLinkResponse>(UnrestrictLinkResponse.class) {
            });
            maxChunks = linkresp.getChunks();
            if (maxChunks == -1 || PluginJsonConfig.get(RealDebridComConfig.class).isIgnoreServerSideChunksNum()) {
                maxChunks = 0;
            } else {
                maxChunks = -maxChunks;
            }

            String genLnk = linkresp.getDownload();

            if (!genLnk.startsWith("http")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported protocol");
            }

            showMessage(link, "Task 2: Download begins!");
            try {
                handleDL(account, link, genLnk);
                return;
            } catch (PluginException e1) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                if (br.containsHTML("An error occurr?ed while generating a premium link, please contact an Administrator")) {
                    logger.info("Error while generating premium link, removing host from supported list");
                    tempUnavailableHoster(account, link, 60 * 60 * 1000l);
                }
                if (br.containsHTML("An error occurr?ed while attempting to download the file.")) {
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }

                throw e1;
            }
        } catch (APIException e) {
            switch (e.getError()) {

            case HOSTER_TEMP_UNAVAILABLE:
            case HOSTER_IN_MAINTENANCE:
            case HOSTER_LIMIT_REACHED:
            case HOSTER_PREMIUM_ONLY:
                tempUnavailableHoster(account, link, 30 * 60 * 1000l);
                return;
            default:
                throw e;
            }

        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleFree(link);
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    private boolean isDirectLink(final DownloadLink downloadLink) {
        if (downloadLink.getPluginPatternMatcher().matches(this.getLazyP().getPatternSource())) {
            return true;
        }
        return false;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            /** Load cookies */
            if (!force) {
                String tokenJSon = account.getStringProperty(TOKEN);
                if (StringUtils.isNotEmpty(tokenJSon)) {
                    TokenResponse token = JSonStorage.restoreFromString(tokenJSon, new TypeRef<TokenResponse>(TokenResponse.class) {
                    });
                    // ensure that the token is at elast 5 minutes valid
                    long expireTime = token.getExpires_in() * 1000 + token.getCreateTime();
                    long now = System.currentTimeMillis();
                    if ((expireTime - 5 * 60 * 1000l) > now) {

                        this.token = token;

                        return;
                    }
                }
            }
            TypeRef<ClientSecret> clientSecretType = new TypeRef<ClientSecret>(ClientSecret.class) {
            };
            // frist try refresh
            String tokenJSon = account.getStringProperty(TOKEN);
            String clientSecretJson = account.getStringProperty(CLIENT_SECRET);
            if (StringUtils.isNotEmpty(tokenJSon) && StringUtils.isNotEmpty(clientSecretJson)) {
                TokenResponse token = JSonStorage.restoreFromString(tokenJSon, new TypeRef<TokenResponse>(TokenResponse.class) {
                });
                ClientSecret clientSecret = JSonStorage.restoreFromString(clientSecretJson, clientSecretType);

                String tokenResponseJson = br.postPage(API + "/oauth/v2/token", new QueryInfo().append(CLIENT_ID_KEY, clientSecret.getClient_id(), true).append(CLIENT_SECRET_KEY, clientSecret.getClient_secret(), true).append("code", token.getRefresh_token(), true).append("grant_type", "http://oauth.net/grant_type/device/1.0", true));
                TokenResponse newToken = JSonStorage.restoreFromString(tokenResponseJson, new TypeRef<TokenResponse>(TokenResponse.class) {
                });
                if (newToken.validate()) {
                    this.token = newToken;

                    account.setProperty(TOKEN, JSonStorage.serializeToJson(newToken));
                    return;
                }

            }
            br.setCookiesExclusive(true);

            prepBrowser(br);
            br.clearCookies(API);
            CodeResponse code = JSonStorage.restoreFromString(br.getPage(API + "/oauth/v2/device/code?client_id=" + CLIENT_ID + "&new_credentials=yes"), new TypeRef<CodeResponse>(CodeResponse.class) {
            });
            // TODO: CReate a oauth Challenge instead of doing this inapp
            String verificationUrl = code.getVerification_url();
            br.clearCookies(verificationUrl);
            br.getPage(verificationUrl);
            Form form = br.getFormbyAction("/device");
            form.getInputField("usercode").setValue(code.getUser_code());
            br.submitForm(form);
            Form loginForm = br.getFormbyActionRegex("/authorize\\?.+");
            loginForm.getInputField("p").setValue(account.getPass());
            loginForm.getInputField("u").setValue(account.getUser());
            br.submitForm(loginForm);
            Form allow = br.getFormBySubmitvalue("Allow");
            allow.setPreferredSubmit("Allow");
            br.submitForm(allow);
            //
            ClientSecret clientSecret = JSonStorage.restoreFromString(br.getPage(API + "/oauth/v2/device/credentials?client_id=" + Encoding.urlEncode(CLIENT_ID) + "&code=" + Encoding.urlEncode(code.getDevice_code())), clientSecretType);

            String tokenResponseJson = br.postPage(API + "/oauth/v2/token", new QueryInfo().append(CLIENT_ID_KEY, clientSecret.client_id, true).append(CLIENT_SECRET_KEY, clientSecret.client_secret, true).append("code", code.getDevice_code(), true).append("grant_type", "http://oauth.net/grant_type/device/1.0", true));
            TokenResponse token = JSonStorage.restoreFromString(tokenResponseJson, new TypeRef<TokenResponse>(TokenResponse.class) {
            });
            if (token.validate()) {
                account.setProperty(TOKEN, JSonStorage.serializeToJson(token));
                account.setProperty(CLIENT_SECRET, JSonStorage.serializeToJson(clientSecret));
                this.token = token;
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Unknown Error", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    private Browser prepBrowser(Browser prepBr) {
        // define custom browser headers and language settings.
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.setCookie(mProt + mName, "lang", "en");
        prepBr.getHeaders().put("User-Agent", "JDownloader");
        prepBr.setCustomCharset("utf-8");
        prepBr.setConnectTimeout(2 * 60 * 1000);
        prepBr.setReadTimeout(2 * 60 * 1000);
        prepBr.setFollowRedirects(true);
        prepBr.setAllowedResponseCodes(new int[] { 504 });
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink dl) throws PluginException, IOException {
        prepBrowser(br);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dl.getDownloadURL());
            if (con.isContentDisposition() && con.isOK()) {
                if (dl.getFinalFileName() == null) {
                    dl.setFinalFileName(getFileNameFromHeader(con));
                }
                dl.setVerifiedFileSize(con.getLongContentLength());
                dl.setAvailable(true);
                return AvailableStatus.TRUE;
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } catch (final Throwable e) {
            return AvailableStatus.UNCHECKABLE;
        } finally {
            try {
                /* make sure we close connection */
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

    }

    @CustomStorageName("RealDebridCom")
    public interface RealDebridComConfig extends PluginConfigInterface {
        @AboutConfig
        @DefaultBooleanValue(false)
        void setIgnoreServerSideChunksNum(boolean b);

        boolean isIgnoreServerSideChunksNum();

    }

    @Override
    public ConfigContainer getConfig() {
        throw new WTFException("Not implemented");
    }

    @Override
    public SubConfiguration getPluginConfig() {
        throw new WTFException("Not implemented");
    }

    @Override
    public Class<? extends ConfigInterface> getConfigInterface() {
        return RealDebridComConfig.class;
    }

    // public void setConfigElements() {
    // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), IGNOREMAXCHUNKS, "Ignore max chunks set by
    // real-debrid.com?").setDefaultValue(false));
    // }
    private static class RealDebridConfigPanel extends PluginConfigPanelNG {
        private RealDebridComConfig cf;

        public RealDebridConfigPanel() {
            cf = PluginJsonConfig.get(RealDebridComConfig.class);

            addPair("Ignore max chunks set by real-debrid.com?", null, null, new Checkbox(cf._getStorageHandler().getKeyHandler("IgnoreServerSideChunksNum", BooleanKeyHandler.class), null));

        }

        @Override
        public void reset() {
            for (KeyHandler m : cf._getStorageHandler().getMap().values()) {

                m.setValue(m.getDefaultValue());
            }
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    updateContents();
                }
            };
        }

        @Override
        public void save() {
        }

        @Override
        public void updateContents() {
        }

    }

    @Override
    public PluginConfigPanelNG createConfigPanel() {
        if (panel == null) {
            panel = new RealDebridConfigPanel();
        }
        return panel;
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait 30 mins to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

}