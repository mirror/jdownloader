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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.oauth.AccountLoginOAuthChallenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.plugins.components.realDebridCom.RealDebridComConfig;
import org.jdownloader.plugins.components.realDebridCom.api.Error;
import org.jdownloader.plugins.components.realDebridCom.api.json.CheckLinkResponse;
import org.jdownloader.plugins.components.realDebridCom.api.json.ClientSecret;
import org.jdownloader.plugins.components.realDebridCom.api.json.CodeResponse;
import org.jdownloader.plugins.components.realDebridCom.api.json.ErrorResponse;
import org.jdownloader.plugins.components.realDebridCom.api.json.HostsResponse;
import org.jdownloader.plugins.components.realDebridCom.api.json.TokenResponse;
import org.jdownloader.plugins.components.realDebridCom.api.json.UnrestrictLinkResponse;
import org.jdownloader.plugins.components.realDebridCom.api.json.UserResponse;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.translate._JDT;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.captcha.SkipException;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.CaptchaException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.HashInfo;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "real-debrid.com" }, urls = { "https?://(?:\\w+(?:\\.download)?\\.)?(?:real\\-debrid\\.com|rdb\\.so|rdeb\\.io)/dl?/\\w+(?:/.+)?" })
public class RealDebridCom extends PluginForHost {

    private static final String CLIENT_SECRET_KEY = "client_secret";
    private static final String CLIENT_ID_KEY     = "client_id";
    private static final String CLIENT_SECRET     = "CLIENT_SECRET";
    private static final String TOKEN             = "TOKEN";

    private static final String AUTHORIZATION     = "Authorization";

    private static class APIException extends Exception {

        private final URLConnectionAdapter connection;

        public APIException(URLConnectionAdapter connection, Error error, String msg) {
            super(msg);
            this.error = error;
            this.connection = connection;
        }

        public URLConnectionAdapter getConnection() {
            return connection;
        }

        private final Error error;

        public Error getError() {
            return error;
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

    private final String                                   mName              = "real-debrid.com";

    private final String                                   mProt              = "https://";

    private Browser                                        apiBrowser;

    private TokenResponse                                  token;

    public RealDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/");
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
        Browser.setRequestIntervalLimitGlobal("rdb.so", 500);
        Browser.setRequestIntervalLimitGlobal("rdeb.io", 500);
    }

    private <T> T callRestAPI(final Account account, String method, UrlQuery query, TypeRef<T> type) throws Exception {
        if (account == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        ensureAPIBrowser();
        login(account, false);
        try {
            return callRestAPIInternal("https://api.real-debrid.com/rest/1.0" + method, query, type);
        } catch (APIException e) {
            switch (e.getError()) {
            case BAD_LOGIN:
            case BAD_TOKEN:
                // refresh Token

                login(account, true);
                return callRestAPIInternal("https://api.real-debrid.com/rest/1.0" + method, query, type);
            default:
                throw e;
            }
        }
    }

    protected synchronized <T> T callRestAPIInternal(String url, UrlQuery query, TypeRef<T> type) throws IOException, APIException {
        if (token != null) {
            apiBrowser.getHeaders().put(AUTHORIZATION, "Bearer " + token.getAccess_token());
        }
        final Request request;
        if (query != null) {
            request = apiBrowser.createPostRequest(url, query);
        } else {
            request = apiBrowser.createGetRequest(url);
        }
        final String json = apiBrowser.getPage(request);
        if (request.getHttpConnection().getResponseCode() != 200) {
            if (json.trim().startsWith("{")) {
                final ErrorResponse errorResponse = JSonStorage.restoreFromString(json, new TypeRef<ErrorResponse>(ErrorResponse.class) {
                });
                Error errorCode = Error.getByCode(errorResponse.getError_code());
                if (Error.UNKNOWN.equals(errorCode) && request.getHttpConnection().getResponseCode() == 403) {
                    errorCode = Error.BAD_TOKEN;
                }
                throw new APIException(request.getHttpConnection(), errorCode, errorResponse.getError());
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

    private <T> T callRestAPI(final Account account, String method, TypeRef<T> type) throws Exception {
        return callRestAPI(account, method, null, type);

    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (isDirectRealDBUrl(downloadLink)) {
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
        final AccountInfo ai = new AccountInfo();
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(-1);
        final UserResponse user = callRestAPI(account, "/user", UserResponse.TYPE);
        ai.setValidUntil(TimeFormatter.getTimestampByGregorianTime(user.getExpiration()));
        if ("premium".equalsIgnoreCase(user.getType())) {
            ai.setStatus("Premium Account");
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Free Account");
            ai.setProperty("multiHostSupport", Property.NULL);
            return ai;
        }
        final HashMap<String, HostsResponse> hosts = callRestAPI(account, "/hosts", new TypeRef<HashMap<String, HostsResponse>>() {
        });
        final ArrayList<String> supportedHosts = new ArrayList<String>();
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

    private void handleDL(final Account acc, final DownloadLink link, final String dllink, final UnrestrictLinkResponse linkresp) throws Exception {
        // real debrid connections are flakey at times! Do this instead of repeating download steps.
        final int maxChunks;
        if (linkresp == null) {
            maxChunks = 0;
        } else {
            if (linkresp.getChunks() <= 0 || PluginJsonConfig.get(RealDebridComConfig.class).isIgnoreServerSideChunksNum()) {
                maxChunks = 0;
            } else {
                maxChunks = -(int) linkresp.getChunks();
            }
        }
        final boolean useSSL = PluginJsonConfig.get(RealDebridComConfig.class).isUseSSLForDownload();
        final String downloadLink;
        if (useSSL) {
            downloadLink = dllink.replaceFirst("^http:", "https:");
        } else {
            downloadLink = dllink.replaceFirst("^https:", "http:");
        }
        final String host = Browser.getHost(downloadLink);
        final DownloadLinkDownloadable downloadLinkDownloadable = new DownloadLinkDownloadable(link) {
            @Override
            public HashInfo getHashInfo() {
                if (linkresp == null || linkresp.getCrc() == 1) {
                    return super.getHashInfo();
                } else {
                    return null;
                }
            }

            @Override
            public long getVerifiedFileSize() {
                if (linkresp == null || linkresp.getCrc() == 1) {
                    return super.getVerifiedFileSize();
                } else {
                    return -1;
                }
            }

            @Override
            public String getHost() {
                return host;
            }
        };
        final boolean resume;
        if ("mega.co.nz".equals(link.getHost())) {
            resume = false;
        } else {
            resume = true;
        }
        final Browser br2 = br.cloneBrowser();
        br2.setAllowedResponseCodes(new int[0]);
        boolean increment = false;
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br2, downloadLinkDownloadable, br2.createGetRequest(downloadLink), resume, resume ? maxChunks : 1);
            if (dl.getConnection().isContentDisposition() || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "octet-stream")) {
                /* content disposition, lets download it */
                RUNNING_DOWNLOADS.incrementAndGet();
                increment = true;
                dl.startDownload();
            } else {
                br2.followConnection();
                this.br = br2;// required for error handling outside this method
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        handleDL(null, link, link.getPluginPatternMatcher(), null);
    }

    private AvailableStatus check(final Account account, DownloadLink link) throws Exception {
        if (account != null && !isDirectRealDBUrl(link)) {
            final String dllink = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
            final String password = link.getStringProperty("pass", null);
            final CheckLinkResponse checkresp = callRestAPI(account, "/unrestrict/check", new UrlQuery().append("link", dllink, true).append("password", password, true), CheckLinkResponse.TYPE);
            if (checkresp != null) {
                if (StringUtils.isEmpty(checkresp.getHost()) || (checkresp.getFilename() == null && checkresp.getFilesize() == 0)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (checkresp.getFilesize() > 0) {
                    link.setVerifiedFileSize(checkresp.getFilesize());
                }
                if (checkresp.getFilename() != null) {
                    link.setFinalFileName(checkresp.getFilename());
                }
                return AvailableStatus.TRUE;
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean hasConfig() {
        return true;
    }

    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        handleMultiHost(0, link, account);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final int startTaskIndex, final DownloadLink link, final Account account) throws Exception {
        try {
            synchronized (hostUnavailableMap) {
                final HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
                if (unavailableMap != null) {
                    final Long lastUnavailable = unavailableMap.get(link.getHost());
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
            showMessage(link, "Task " + (startTaskIndex + 1) + ": Generating Link");
            /* request Download */
            final String dllink = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
            final String password = link.getStringProperty("pass", null);
            final UnrestrictLinkResponse linkresp = callRestAPI(account, "/unrestrict/link", new UrlQuery().append("link", dllink, true).append("password", password, true), UnrestrictLinkResponse.TYPE);
            final String genLnk = linkresp.getDownload();
            if (StringUtils.isEmpty(genLnk) || !genLnk.startsWith("http")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported protocol");
            }
            showMessage(link, "Task " + (startTaskIndex + 2) + ": Download begins!");
            try {
                handleDL(account, link, genLnk, linkresp);
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
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
                }
                if (br.containsHTML("Your code is not or no longer valid")) {
                    tempUnavailableHoster(account, link, 30 * 60 * 1000l);
                }
                if (br.containsHTML("You can not download this file because you have exceeded your traffic on this hoster")) {
                    tempUnavailableHoster(account, link, 60 * 60 * 1000l);
                }
                if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 404 && br.containsHTML(">The download server is down or banned from our server")) {
                    // <div class="alert alert-danger">An error occurred while read your file on the remote host ! Timeout of 60s exceeded
                    // !<br/>The download server is down or banned from our server, please contact an Administrator with these following
                    // informations :<br/><br/>Link: http://abc <br/>Server: 130<br/>Code: HASH</div>
                    tempUnavailableHoster(account, link, 5 * 60 * 1000l);
                }
                throw e1;
            }
        } catch (APIException e) {
            switch (e.getError()) {
            case IP_ADRESS_FORBIDDEN:
                throw new PluginException(LinkStatus.ERROR_FATAL, e.getMessage());
            case FILE_UNAVAILABLE:
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, _JDT.T.downloadlink_status_error_hoster_temp_unavailable(), 10 * 60 * 1000l);
            case UNSUPPORTED_HOSTER:
                // logger.severe("Unsupported Hoster: " + link.getDefaultPlugin().buildExternalDownloadURL(link, this));
                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unsupported Hoster: " + link.getHost());
                // 20170501-raztoki not unsupported url format. When this error is returned by rd, we should just jump to the next download
                // candidate, as another link from the same host might work.
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
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
        showMessage(link, "Task 1: Check URL validity!");
        final AvailableStatus status = requestFileInformation(link);
        if (AvailableStatus.UNCHECKABLE.equals(status)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 1000l);
        }
        if (isDirectRealDBUrl(link)) {
            showMessage(link, "Task 2: Download begins!");
            handleDL(account, link, link.getPluginPatternMatcher(), null);
        } else {
            handleMultiHost(2, link, account);
        }
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    public ClientSecret checkCredentials(CodeResponse code) throws IOException, APIException {
        return callRestAPIInternal(API + "/oauth/v2/device/credentials?client_id=" + Encoding.urlEncode(CLIENT_ID) + "&code=" + Encoding.urlEncode(code.getDevice_code()), null, ClientSecret.TYPE);
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // first try to use the stored token
            if (!force) {
                final String tokenJSon = account.getStringProperty(TOKEN);
                if (StringUtils.isNotEmpty(tokenJSon)) {
                    final TokenResponse token = JSonStorage.restoreFromString(tokenJSon, new TypeRef<TokenResponse>(TokenResponse.class) {
                    });
                    // ensure that the token is at elast 5 minutes valid
                    final long expireTime = token.getExpires_in() * 1000 + token.getCreateTime();
                    final long now = System.currentTimeMillis();
                    if ((expireTime - 5 * 60 * 1000l) > now) {
                        this.token = token;
                        return;
                    }
                }
            }
            // token invalid, forcerefresh active or token expired.
            // Try to refresh the token
            final String tokenJSon = account.getStringProperty(TOKEN);
            final String clientSecretJson = account.getStringProperty(CLIENT_SECRET);
            if (StringUtils.isNotEmpty(tokenJSon) && StringUtils.isNotEmpty(clientSecretJson)) {
                final TokenResponse token = JSonStorage.restoreFromString(tokenJSon, TokenResponse.TYPE);
                final ClientSecret clientSecret = JSonStorage.restoreFromString(clientSecretJson, ClientSecret.TYPE);
                final String tokenResponseJson = br.postPage(API + "/oauth/v2/token", new UrlQuery().append(CLIENT_ID_KEY, clientSecret.getClient_id(), true).append(CLIENT_SECRET_KEY, clientSecret.getClient_secret(), true).append("code", token.getRefresh_token(), true).append("grant_type", "http://oauth.net/grant_type/device/1.0", true));
                final TokenResponse newToken = JSonStorage.restoreFromString(tokenResponseJson, TokenResponse.TYPE);
                if (newToken.validate()) {
                    this.token = newToken;
                    account.setProperty(TOKEN, JSonStorage.serializeToJson(newToken));
                    return;
                }
            }
            this.token = null;
            // Could not refresh the token. login using username and password
            br.setCookiesExclusive(true);
            prepBrowser(br);
            br.clearCookies(API);
            final Browser autoSolveBr = br.cloneBrowser();
            final CodeResponse code = JSonStorage.restoreFromString(br.getPage(API + "/oauth/v2/device/code?client_id=" + CLIENT_ID + "&new_credentials=yes"), new TypeRef<CodeResponse>(CodeResponse.class) {
            });
            ensureAPIBrowser();
            final AtomicReference<ClientSecret> clientSecretResult = new AtomicReference<ClientSecret>(null);
            final AtomicBoolean loginsInvalid = new AtomicBoolean(false);
            final AccountLoginOAuthChallenge challenge = new AccountLoginOAuthChallenge(getHost(), null, account, code.getDirect_verification_url()) {

                private volatile long lastValidation = -1;

                @Override
                public Plugin getPlugin() {
                    return RealDebridCom.this;
                }

                @Override
                public void poll(SolverJob<Boolean> job) {
                    if (System.currentTimeMillis() - lastValidation >= code.getInterval() * 1000) {
                        lastValidation = System.currentTimeMillis();
                        try {
                            final ClientSecret clientSecret = checkCredentials(code);
                            if (clientSecret != null) {
                                clientSecretResult.set(clientSecret);
                                job.addAnswer(new AbstractResponse<Boolean>(this, ChallengeSolver.EXTERN, 100, true));
                            }
                        } catch (Throwable e) {
                            logger.log(e);
                        }
                    }
                }

                @Override
                public boolean autoSolveChallenge(SolverJob<Boolean> job) {
                    try {
                        final String verificationUrl = getUrl();
                        autoSolveBr.clearCookies(verificationUrl);
                        autoSolveBr.getPage(verificationUrl);
                        Form loginForm = autoSolveBr.getFormbyActionRegex("/authorize\\?.+");
                        if (loginForm == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        if (loginForm.containsHTML("g-recaptcha")) {
                            logger.info("Login requires Recaptcha");
                            final DownloadLink dummyLink = new DownloadLink(RealDebridCom.this, "Account:" + getAccount().getPass(), getHost(), "https://real-debrid.com", true);
                            RealDebridCom.this.setDownloadLink(dummyLink);
                            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(RealDebridCom.this, autoSolveBr).getToken();
                            loginForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                        }
                        loginForm.getInputField("p").setValue(Encoding.urlEncode(getAccount().getPass()));
                        loginForm.getInputField("u").setValue(Encoding.urlEncode(getAccount().getUser()));
                        autoSolveBr.submitForm(loginForm);
                        if (autoSolveBr.containsHTML("Your login informations are incorrect")) {
                            loginsInvalid.set(true);
                            job.addAnswer(new AbstractResponse<Boolean>(this, this, 100, false));
                            return false;
                        }
                        Form allow = autoSolveBr.getFormBySubmitvalue("Allow");
                        if (allow == null) {
                            loginForm = autoSolveBr.getFormbyActionRegex("/authorize\\?.+");
                            if (loginForm == null) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            if (loginForm.containsHTML("g-recaptcha")) {
                                logger.info("Login requires Recaptcha");
                                final DownloadLink dummyLink = new DownloadLink(RealDebridCom.this, "Account:" + getAccount().getPass(), getHost(), "https://real-debrid.com", true);
                                RealDebridCom.this.setDownloadLink(dummyLink);
                                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(RealDebridCom.this, autoSolveBr).getToken();
                                loginForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                            }
                            loginForm.getInputField("p").setValue(Encoding.urlEncode(getAccount().getPass()));
                            loginForm.getInputField("u").setValue(Encoding.urlEncode(getAccount().getUser()));
                            autoSolveBr.submitForm(loginForm);
                            if (autoSolveBr.containsHTML("Your login informations are incorrect")) {
                                loginsInvalid.set(true);
                                job.addAnswer(new AbstractResponse<Boolean>(this, this, 100, false));
                                return false;
                            }
                            allow = autoSolveBr.getFormBySubmitvalue("Allow");
                        }
                        if (allow != null) {
                            allow.setPreferredSubmit("Allow");
                            autoSolveBr.submitForm(allow);
                            final ClientSecret clientSecret = checkCredentials(code);
                            if (clientSecret != null) {
                                clientSecretResult.set(clientSecret);
                                job.addAnswer(new AbstractResponse<Boolean>(this, this, 100, true));
                                return true;
                            }
                        }
                    } catch (CaptchaException e) {
                        logger.log(e);
                        job.addAnswer(new AbstractResponse<Boolean>(this, this, 100, false));
                    } catch (PluginException e) {
                        logger.log(e);
                        job.addAnswer(new AbstractResponse<Boolean>(this, this, 100, false));
                    } catch (InterruptedException e) {
                        logger.log(e);
                        job.addAnswer(new AbstractResponse<Boolean>(this, this, 100, false));
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                    return false;
                }
            };
            challenge.setTimeout(5 * 60 * 1000);
            try {
                ChallengeResponseController.getInstance().handle(challenge);
            } catch (SkipException e) {
                logger.log(e);
            }
            final ClientSecret clientSecret = clientSecretResult.get();
            if (clientSecret == null) {
                if (loginsInvalid.get()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Your login informations are incorrect", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "OAuth Failed", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            final String tokenResponseJson = br.postPage(API + "/oauth/v2/token", new UrlQuery().append(CLIENT_ID_KEY, clientSecret.getClient_id(), true).append(CLIENT_SECRET_KEY, clientSecret.getClient_secret(), true).append("code", code.getDevice_code(), true).append("grant_type", "http://oauth.net/grant_type/device/1.0", true));
            final TokenResponse token = JSonStorage.restoreFromString(tokenResponseJson, new TypeRef<TokenResponse>(TokenResponse.class) {
            });
            if (token.validate()) {
                this.token = token;
                final UserResponse user = callRestAPIInternal("https://api.real-debrid.com/rest/1.0" + "/user", null, UserResponse.TYPE);
                if (!StringUtils.equalsIgnoreCase(account.getUser(), user.getEmail()) && !StringUtils.equalsIgnoreCase(account.getUser(), user.getUsername())) {
                    this.token = null;
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "User Mismatch. You try to add the account " + account.getUser() + "\r\nBut in your browser you are logged in as " + user.getUsername() + "\r\nPlease make sure that there is no username mismatch!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
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

    private boolean isDirectRealDBUrl(DownloadLink dl) {
        final String url = dl.getDownloadURL();
        if (url.contains("download.")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        prepBrowser(br);
        if (isDirectRealDBUrl(link)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(link.getDownloadURL());
                if (con.isContentDisposition() && con.isOK()) {
                    if (link.getFinalFileName() == null) {
                        link.setFinalFileName(getFileNameFromHeader(con));
                    }
                    link.setVerifiedFileSize(con.getLongContentLength());
                    link.setAvailable(true);
                    return AvailableStatus.TRUE;
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } catch (final PluginException e) {
                throw e;
            } catch (final Throwable e) {
                logger.log(e);
                return AvailableStatus.UNCHECKABLE;
            } finally {
                try {
                    /* make sure we close connection */
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            final ArrayList<Account> accounts = AccountController.getInstance().getValidAccounts(getHost());
            if (accounts != null && accounts.size() > 0) {
                return check(accounts.get(0), link);
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

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
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return RealDebridComConfig.class;
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