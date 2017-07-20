package jd.plugins.hoster;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SmoozedTranslation;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.Base64OutputStream;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.dialog.AskToUsePremiumDialog;
import org.jdownloader.gui.dialog.AskToUsePremiumDialogInterface;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.plugins.controller.host.PluginFinder;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "smoozed.com" }, urls = { "" })
public class SmoozedCom extends antiDDoSForHost {

    private final String                                     API                  = "www.smoozed.com";

    private static WeakHashMap<Account, Map<String, Object>> ACCOUNTINFOS         = new WeakHashMap<Account, Map<String, Object>>();
    public static final String                               PROPERTY_ACCOUNTINFO = "ACCOUNTINFO";
    public static final String                               PROPERTY_ACCOUNTHASH = "ACCOUNTHASH";
    private final String                                     SSL                  = "SSL";

    public SmoozedCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.smoozed.com/register");
        setConfigElements();
    }

    private static String PBKDF2Key(String password) throws Exception {
        if (StringUtils.isEmpty(password)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid Login Credentials", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        final PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Hash.getSHA256(password.getBytes("UTF-8")).getBytes("UTF-8"), 1000, 32 * 8);
        final SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        final byte[] hash = skf.generateSecret(spec).getEncoded();
        return HexFormatter.byteArrayToHex(hash);
    }

    @Override
    public List<DownloadLink> sortDownloadLinks(Account account, List<DownloadLink> downloadLinks) {
        if (getPluginConfig().getBooleanProperty(AUTOMIRROR, true) == true) {
            synchronized (ACCOUNTINFOS) {
                final Map<String, Object> map = ACCOUNTINFOS.get(account);
                if (map != null) {
                    Collections.sort(downloadLinks, new Comparator<DownloadLink>() {

                        public int compare(Number x, Number y) {
                            if (x != null && y != null) {
                                return (x.intValue() < y.intValue()) ? 1 : ((x.intValue() == y.intValue()) ? 0 : -1);
                            } else if (x == null && y == null) {
                                return 0;
                            } else if (x != null) {
                                return -1;
                            } else {
                                return 1;
                            }
                        }

                        @Override
                        public int compare(DownloadLink o1, DownloadLink o2) {
                            final Map<String, Object> o1Settings = getHostSettings(map, o1.getHost());
                            final Map<String, Object> o2Settings = getHostSettings(map, o2.getHost());
                            final Number o1Priority;
                            if (o1Settings != null) {
                                o1Priority = (Number) o1Settings.get("priority");
                            } else {
                                o1Priority = null;
                            }
                            final Number o2Priority;
                            if (o2Settings != null) {
                                o2Priority = (Number) o2Settings.get("priority");
                            } else {
                                o2Priority = null;
                            }
                            return compare(o1Priority, o2Priority);
                        }

                    });
                }
            }
        }
        return downloadLinks;
    }

    private void restoreAccountInfos(Account account) {
        synchronized (ACCOUNTINFOS) {
            if (!ACCOUNTINFOS.containsKey(account)) {
                final String responseString = account.getStringProperty(PROPERTY_ACCOUNTINFO, null);
                if (StringUtils.isNotEmpty(responseString)) {
                    try {
                        if (StringUtils.equals(Hash.getSHA256((account.getUser() + account.getPass()).toLowerCase(Locale.ENGLISH)), account.getStringProperty(PROPERTY_ACCOUNTHASH, null))) {
                            final HashMap<String, Object> responseMap = JSonStorage.restoreFromString(responseString, new TypeRef<HashMap<String, Object>>() {
                            }, null);
                            if (responseMap != null && responseMap.size() > 0) {
                                rewriteHosterNames(responseMap, new PluginFinder());
                                ACCOUNTINFOS.put(account, responseMap);
                                return;
                            }
                        }
                    } catch (final Throwable e) {
                        LogSource.exception(logger, e);
                    }
                    account.removeProperty(PROPERTY_ACCOUNTHASH);
                    account.removeProperty(PROPERTY_ACCOUNTINFO);
                }
            }
        }
    }

    private Map<String, Object> login(Account account) throws Exception {
        synchronized (ACCOUNTINFOS) {
            try {
                restoreAccountInfos(account);
                final String session_Key = get(account, String.class, "data", "session_key");
                Request request = null;
                if (session_Key != null) {
                    try {
                        // apiCheckSession(account, session_Key);
                        request = apiConfigJS(account, session_Key);
                    } catch (PluginException e) {
                        request = null;
                    }
                }
                if (request == null) {
                    request = apiLogin(account);
                    account.saveCookies(this.br.getCookies(this.getHost()), "");
                }
                final String responseString = request.getHtmlCode();
                final HashMap<String, Object> responseMap = JSonStorage.restoreFromString(responseString, new TypeRef<HashMap<String, Object>>() {
                }, null);
                final Number user_Locked = get(responseMap, Number.class, "data", "user", "user_locked");
                if (user_Locked.intValue() != 0) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account locked, please contact smoozed.com support", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                ACCOUNTINFOS.put(account, responseMap);
                account.setProperty(PROPERTY_ACCOUNTINFO, responseString);
                account.setProperty(PROPERTY_ACCOUNTHASH, Hash.getSHA256((account.getUser() + account.getPass()).toLowerCase(Locale.ENGLISH)));
                return responseMap;
            } catch (final PluginException e) {
                account.removeProperty(PROPERTY_ACCOUNTINFO);
                account.removeProperty(PROPERTY_ACCOUNTHASH);
                account.clearCookies("");
                ACCOUNTINFOS.remove(account);
                throw e;
            }
        }
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(DownloadLink downloadLink, Account account) throws Exception {
        String session_Key = null;
        int maxChunks = 1;
        synchronized (ACCOUNTINFOS) {
            session_Key = get(account, String.class, "data", "session_key");
            if (session_Key != null) {
                try {
                    apiCheckSession(account, session_Key);
                } catch (PluginException e) {
                    session_Key = null;
                }
            }
            if (session_Key == null) {
                login(account);
                session_Key = get(account, String.class, "data", "session_key");
                if (session_Key == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            /* max concurrent connections for given link and account */
            final Map<String, Object> hostSettings = getHostSettings(ACCOUNTINFOS.get(account), downloadLink.getHost());
            if (hostSettings != null && "ok".equalsIgnoreCase(String.valueOf(get(hostSettings, "state")))) {
                final Number maxChunksNumber = get(hostSettings, Number.class, "max_chunks");
                if (maxChunksNumber != null) {
                    maxChunks = Math.max(0, maxChunksNumber.intValue());
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster currently not available");
            }
            if (!canHandle(downloadLink, account)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster currently not available");
            }
        }
        apiCheck(account, session_Key, downloadLink);
        apiDownload(account, session_Key, downloadLink, maxChunks);
    }

    private void apiCheck(final Account account, final String session_Key, final DownloadLink link) throws Exception {
        if (link.getVerifiedFileSize() < 0 || (link.getFinalFileName() == null && link.getForcedFileName() == null)) {
            apiCheck(account, session_Key, link, 0);
        }
    }

    @Override
    protected void showFreeDialog(final String domain) {
        final AskToUsePremiumDialog d = new AskToUsePremiumDialog(domain, this) {
            @Override
            public String getDontShowAgainKey() {
                return "adsPremium_" + domain;
            }
        };
        try {
            d.setMessage(TranslationFactory.create(SmoozedTranslation.class).free_trial_end());
            UIOManager.I().show(AskToUsePremiumDialogInterface.class, d).throwCloseExceptions();
            CrossSystem.openURL(new URL(d.getPremiumUrl()));
        } catch (Throwable e) {
            LogSource.exception(logger, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void apiCheck(final Account account, final String session_Key, final DownloadLink link, int round) throws Exception {
        final Request request = api(account, session_Key, "/api/check", "url=" + Encoding.urlEncode(link.getDownloadURL()));
        final String responseString = request.getHtmlCode();
        HashMap<String, Object> responseMap = JSonStorage.restoreFromString(responseString, new TypeRef<HashMap<String, Object>>() {
        }, null);
        String state = (String) responseMap.get("state");
        if ("ok".equals(state)) {
            responseMap = (HashMap<String, Object>) responseMap.get("data");
            final Number seconds = get(responseMap, Number.class, "seconds");
            if (link.getFinalFileName() == null) {
                final String fileName = get(responseMap, String.class, "name");
                link.setFinalFileName(fileName);
            }
            if (link.getVerifiedFileSize() < 0) {
                final Number fileSize1 = get(responseMap, Number.class, "size");
                if (fileSize1 != null) {
                    if (fileSize1.longValue() >= 0) {
                        link.setVerifiedFileSize(fileSize1.longValue());
                    }
                } else {
                    final String fileSize2 = get(responseMap, String.class, "size");
                    if (fileSize2 != null) {
                        final long fileSize = Long.parseLong(fileSize2);
                        if (fileSize >= 0) {
                            link.setVerifiedFileSize(fileSize);
                        }
                    }
                }
            }
            if ("retry".equals(state)) {
                final String message = (String) responseMap.get("message");
                if (StringUtils.equalsIgnoreCase(message, "Hoster temporary not available")) {
                    // Hoster temporary not available
                    if (seconds != null) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster temporary not available", seconds.intValue() * 1000);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster temporary not available");
                    }
                } else if (StringUtils.equalsIgnoreCase(message, "Check currently in progress")) {
                    // Check currently in progress
                    if (seconds != null) {
                        sleep(seconds.intValue() * 1000, link, "Check currently in progress");
                        apiCheck(account, session_Key, link, round + 1);
                        return;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, message);
                    }
                } else if (StringUtils.equalsIgnoreCase(message, "Temporary not available")) {
                    // File temporary not available
                    if (seconds != null) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File temporary not downloadable via smoozed.com", seconds.intValue() * 1000);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File temporary not downloadable via smoozed.com");
                    }
                } else if (StringUtils.equalsIgnoreCase(message, "Check timed out")) {
                    // Check timed out
                    logger.info("Check timed out, trying to download without one!");
                    return;
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, message);
            } else {
                return;
            }
        } else {
            final String message = (String) responseMap.get("message");
            if (StringUtils.equalsIgnoreCase(message, "Offline")) {
                // Offline
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (StringUtils.equalsIgnoreCase(message, "Premium needed")) {
                /*
                 * TODO: Not sure if this error affects the whole account or if it only means that the users' account type is not allowed to
                 * download from currently used host.
                 */
                account.getAccountInfo().setExpired(true);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPremium expired. Premium needed to continue downloading!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, message);
        }
    }

    private final String AUTOLOG    = "AUTOLOG";
    private final String AUTOMIRROR = "AUTOMIRROR";

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SSL, "Use SSL?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), AUTOLOG, "Send debug logs to Smoozed.com automatically?").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), AUTOMIRROR, "Enable Smoozed.com mirror selection mode?").setDefaultValue(true));
    }

    private String getProtocol() {
        boolean ssl = getPluginConfig().getBooleanProperty(SSL, true);
        if (ssl && Application.getJavaVersion() >= Application.JAVA17) {
            return "https://";
        } else {
            return "http://";
        }
    }

    @Override
    public void errLog(Throwable e, Browser br, LogSource log, DownloadLink link, Account account) {
        super.errLog(e, br, log, link, account);
        if (e != null && e instanceof PluginException && log != null) {
            try {
                if (getPluginConfig().getBooleanProperty(AUTOLOG, false) == true) {

                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    final Base64OutputStream os = new Base64OutputStream(bos);
                    os.write(log.toString().getBytes("UTF-8"));
                    os.close();
                    final String postString = "api_key=x12GiLzH2bM069rxmLpCcto69&caption=errorLog&content=" + Encoding.urlEncode(bos.toString("UTF-8"));
                    postPage(new Browser(), getProtocol() + "www.smoozed.com/api/debuglog/add/jd", postString);
                }
            } catch (final Throwable ignore) {
                LogSource.exception(logger, ignore);
            }
        }
    }

    private void apiDownload(final Account account, final String session_Key, final DownloadLink link, int maxChunks) throws Exception {
        br.setFollowRedirects(false);
        final String postParam = "session_key=" + Encoding.urlEncode(session_Key) + "&" + "url=" + Encoding.urlEncode(link.getDownloadURL()) + "&silent_errors=true";
        URLConnectionAdapter con = openAntiDDoSRequestConnection(br, br.createPostRequest(getAPI() + "/api/download", postParam));
        Request request;
        if (StringUtils.contains(con.getHeaderField("Content-Type"), "application/json") || con.getRequest().getLocation() == null) {
            br.followConnection();
            errorHandling(br.getRequest(), account, session_Key, "/api/download", link);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            br.followConnection();
            request = br.createRedirectFollowingRequest(br.getRequest());
        }
        // subquent requests are to download servers, these are not hosted via cloudflare. -raztoki20160118
        int maxRedirect = 15;
        while (--maxRedirect > 0) {
            con = br.openRequestConnection(request, false);
            if (con.getRequest().getLocation() == null) {
                if (con.isContentDisposition()) {
                    con.disconnect();
                    br.setFollowRedirects(true);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, con.getRequest().getUrl(), maxChunks > 0, maxChunks >= 1 ? -maxChunks : 1);
                    if (!dl.getConnection().isContentDisposition()) {
                        br.followConnection();
                        errorHandling(br.getRequest(), account, session_Key, "/api/download", link);
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    dl.startDownload();
                    return;
                } else {
                    br.followConnection();
                    errorHandling(br.getRequest(), account, session_Key, "/api/download", link);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            br.followConnection();
            request = br.createRedirectFollowingRequest(request);
        }
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server-Error:Redirectloop");
    }

    private Request apiCheckSession(final Account account, final String session_Key) throws Exception {
        return api(account, session_Key, "/api/checksession", "");
    }

    private Request apiLogin(final Account account) throws Exception {
        final String userName = account.getUser();
        if (StringUtils.isEmpty(userName)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid Login Credentials", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        return api(account, null, "/api/login", "auth=" + Encoding.urlEncode(account.getUser()) + "&password=" + PBKDF2Key(account.getPass()));
    }

    private String getAPI() {
        return getProtocol() + API;
    }

    private Request apiConfigJS(final Account account, final String session_Key) throws Exception {
        getPage(getAPI() + "/config.js?session_key=" + Encoding.urlEncode(session_Key));
        final Request request = br.getRequest();
        errorHandling(request, account, session_Key, "/config.js", null);
        final String responseString = request.getHtmlCode();
        final HashMap<String, Object> responseMap = JSonStorage.restoreFromString(responseString, new TypeRef<HashMap<String, Object>>() {
        }, null);
        final String new_Session_Key = get(responseMap, String.class, "data", "session_key");
        if (StringUtils.isEmpty(new_Session_Key) || "error".equalsIgnoreCase(new_Session_Key)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid session_key", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        return request;
    }

    private static AtomicBoolean TRIALDIALOG = new AtomicBoolean(false);

    private void errorHandling(Request request, final Account account, final String session_Key, final String method, DownloadLink link) throws Exception {
        if (StringUtils.containsIgnoreCase(request.getResponseHeader("Content-Type"), "application/json")) {
            final HashMap<String, Object> responseMap = JSonStorage.restoreFromString(request.getHtmlCode(), new TypeRef<HashMap<String, Object>>() {
            }, null);
            final String state = (String) responseMap.get("state");
            final String message = (String) responseMap.get("message");
            final Number seconds = get(responseMap, Number.class, "seconds");
            if ("error".equals(state)) {
                if (StringUtils.equalsIgnoreCase(message, "Premium needed") && link != null && account != null) {
                    // Premium account needed
                    link.setProperty(premiumRequiredProperty, "P");
                    if (TRIALDIALOG.compareAndSet(false, true)) {
                        final String freeTrialDialog = "freeTrialDialog";
                        if (Boolean.FALSE.equals(this.getPluginConfig().getBooleanProperty(freeTrialDialog, Boolean.FALSE))) {
                            this.getPluginConfig().setProperty(freeTrialDialog, Boolean.TRUE);
                            showFreeDialog(getHost());
                        }
                    }
                    final Map<String, Object> map;
                    synchronized (ACCOUNTINFOS) {
                        restoreAccountInfos(account);
                        map = ACCOUNTINFOS.get(account);
                    }
                    if (map != null) {
                        synchronized (map) {
                            if (isPremium(map) == true) {
                                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No traffic available", 5 * 60 * 1000l);
                            }
                        }
                    }
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } else if (StringUtils.equalsIgnoreCase(message, "Invalid Login Credentials")) {
                    // Invalid Login Credentials
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid Login Credentials", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (StringUtils.equalsIgnoreCase(message, "Login Failed")) {
                    // Login Failed
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid Login Credentials", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (StringUtils.equalsIgnoreCase(message, "Invalid Password")) {
                    // Invalid Password
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid Login Credentials", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (StringUtils.equalsIgnoreCase(message, "Account locked, contact our support")) {
                    // Account locked, contact our support
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account locked, please contact smoozed.com support", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (StringUtils.equalsIgnoreCase(message, "session not found")) {
                    // session not found
                    if (session_Key != null) {
                        synchronized (ACCOUNTINFOS) {
                            if (StringUtils.equals(session_Key, get(account, String.class, "data", "session_key"))) {
                                account.removeProperty(PROPERTY_ACCOUNTINFO);
                                account.removeProperty(PROPERTY_ACCOUNTHASH);
                                ACCOUNTINFOS.remove(account);
                            }
                        }
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                } else if (StringUtils.equalsIgnoreCase(message, "URL not supported")) {
                    // URL not supported
                    throw new PluginException(LinkStatus.ERROR_FATAL, "URL not supported");
                } else if (StringUtils.equalsIgnoreCase(message, "Access denied")) {
                    // Access denied
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Access denied");
                } else if (StringUtils.equalsIgnoreCase(message, "Offline")) {
                    // Offline
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (StringUtils.equalsIgnoreCase(message, "No traffic available")) {
                    // No traffic available
                    if (session_Key != null) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No traffic available", 5 * 60 * 1000l);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                } else if (StringUtils.equalsIgnoreCase(message, "Content not supported")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "API error 'Content not supported'", 5 * 60 * 1000l);
                }
            } else if ("retry".equals(state)) {
                if (StringUtils.equalsIgnoreCase(message, "Premium needed") && link != null && account != null) {
                    if (seconds != null) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Traffic limit for this hoster via smoozed.com reached", Math.max(900, seconds.intValue()) * 1000);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Traffic limit for this hoster via smoozed.com reached");
                    }
                } else if (StringUtils.equalsIgnoreCase(message, "Hoster temporary not available") || StringUtils.equalsIgnoreCase(message, "Internal error")) {
                    // Hoster temporary not available
                    if (seconds != null) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster temporary not available via smoozed.com", Math.max(900, seconds.intValue()) * 1000);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster temporary not available via smoozed.com");
                    }
                } else if (StringUtils.equalsIgnoreCase(message, "Temporary not available")) {
                    // File temporary not available
                    if (seconds != null) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File temporary not downloadable via smoozed.com", Math.max(300, seconds.intValue()) * 1000);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File temporary not downloadable via smoozed.com");
                    }
                } else if (StringUtils.equalsIgnoreCase(message, "No traffic available")) {
                    // No traffic available
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No traffic available");
                } else if (StringUtils.equalsIgnoreCase(message, "Check timed out")) {
                    // Check timed out
                    if (seconds != null) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Check timed out at smoozed.com", Math.max(60, seconds.intValue()) * 1000);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Check timed out at smoozed.com");
                    }
                } else if (StringUtils.equalsIgnoreCase(message, "Internal server error")) {
                    if (seconds != null) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Internal server error at smoozed.com", Math.max(60, seconds.intValue()) * 1000);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Internal server error at smoozed.com");
                    }
                } else {
                    if (seconds != null) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error at smoozed.com", Math.max(60, seconds.intValue()) * 1000);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error at smoozed.com");
                    }
                }
            }
        }
    }

    private Request api(final Account account, final String session_Key, final String method, final String param) throws Exception {
        final String postParam;
        if (session_Key != null) {
            if (StringUtils.isEmpty(param)) {
                postParam = "session_key=" + Encoding.urlEncode(session_Key) + "&silent_errors=true";
            } else {
                postParam = "session_key=" + Encoding.urlEncode(session_Key) + "&" + param + "&silent_errors=true";
            }
        } else {
            postParam = param + "&silent_errors=true";
        }
        postPage(getAPI() + method, postParam);
        final Request request = br.getRequest();
        errorHandling(request, account, session_Key, method, null);
        return request;
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        if (account != null) {
            final Map<String, Object> map;
            synchronized (ACCOUNTINFOS) {
                restoreAccountInfos(account);
                map = ACCOUNTINFOS.get(account);
            }
            if (map != null) {
                synchronized (map) {
                    if (link == null) {
                        /* max concurrent downloads for this account */
                        final Number max = get(map, Number.class, "data", "max_connections");
                        if (max != null) {
                            return Math.max(0, max.intValue());
                        }
                        return 0;
                    } else {
                        /* max concurrent downloads for given link and account */
                        final Map<String, Object> hostSettings = getHostSettings(map, link.getHost());
                        if (hostSettings != null && "ok".equalsIgnoreCase(String.valueOf(get(hostSettings, "state")))) {
                            final Number max = get(hostSettings, Number.class, "max_connections");
                            if (max != null) {
                                return Math.max(0, max.intValue());
                            }
                        }
                        return 0;
                    }
                }
            }
        }
        return 0;
    }

    private Map<String, Object> rewriteHosterNames(Map<String, Object> map, final PluginFinder pluginFinder) {
        if (map != null) {
            synchronized (map) {
                final List hoster = get(map, List.class, "data", "hoster");
                if (hoster != null) {
                    final ListIterator it = hoster.listIterator();
                    while (it.hasNext()) {
                        final Map<String, Object> hostSettings = (Map<String, Object>) it.next();
                        final String hostName = pluginFinder.assignHost((String) hostSettings.get("name"));
                        if (hostName == null) {
                            it.remove();
                        } else {
                            hostSettings.put("name", hostName);
                        }
                    }
                }
                final Map limits = get(map, Map.class, "data", "hoster_limits");
                if (limits != null) {
                    final Map<String, Object> hostLimitsMap = limits;
                    for (final String key : new ArrayList<String>(hostLimitsMap.keySet())) {
                        final Object hostLimits = hostLimitsMap.remove(key);
                        final String hostName = pluginFinder.assignHost(key);
                        if (hostName != null) {
                            hostLimitsMap.put(hostName, hostLimits);
                        }
                    }
                }
                final List usages = get(map, List.class, "data", "hoster_usage");
                if (usages != null) {
                    final List<Map<String, Object>> hostUsagesMap = usages;
                    for (Map<String, Object> hostUsageMap : hostUsagesMap) {
                        final String hostName = pluginFinder.assignHost((String) hostUsageMap.get("traffic_plugin"));
                        if (hostName != null) {
                            hostUsageMap.put("traffic_plugin", hostName);
                        }
                    }
                }
            }
        }
        return map;
    }

    private Map<String, Object> getHostSettings(Map<String, Object> map, String host) {
        if (host != null && map != null) {
            synchronized (map) {
                final List hoster_List = get(map, List.class, "data", "hoster");
                if (hoster_List != null) {
                    for (Object hoster_Item : hoster_List) {
                        final Map<String, Object> hostSettings = (Map<String, Object>) hoster_Item;
                        if (host.equalsIgnoreCase((String) hostSettings.get("name"))) {
                            return hostSettings;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean enoughTrafficFor(DownloadLink downloadLink, Account account) throws Exception {
        return true;
    }

    private boolean isPremium(Map<String, Object> map) {
        final Number user_Premium = get(map, Number.class, "data", "user", "user_premium");
        return user_Premium != null && (user_Premium.longValue() * 1000l >= System.currentTimeMillis());
    }

    private final String premiumRequiredProperty = "sRAT"; // smoozedRequiredAccountType

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account != null) {
            final Map<String, Object> map;
            synchronized (ACCOUNTINFOS) {
                restoreAccountInfos(account);
                map = ACCOUNTINFOS.get(account);
            }
            if (map != null) {
                synchronized (map) {
                    final boolean isPremium = isPremium(map);
                    if ("P".equals(downloadLink.getProperty(premiumRequiredProperty)) && isPremium == false) {
                        return false;
                    }
                    final Map limits = get(map, Map.class, "data", "hoster_limits");
                    if (limits != null) {
                        final Map<String, Object> hostLimitsMap = limits;
                        final Object hostLimits = hostLimitsMap.get(downloadLink.getHost());
                        if (hostLimits != null) {
                            final Number limit = get((Map<String, Object>) hostLimits, Number.class, isPremium ? "premium" : "free");
                            return limit != null && limit.longValue() >= 0;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> map = login(account);
        final Number user_Premium = get(map, Number.class, "data", "user", "user_premium");
        final long user_Premium_Timestamp = user_Premium.longValue() * 1000l;
        if (user_Premium_Timestamp > 0) {
            ai.setStatus("Premium Account");
            ai.setValidUntil(user_Premium_Timestamp);
        } else {
            ai.setStatus("Free Account");
            ai.setValidUntil(-1);
        }
        final List traffic_Usage = get(map, List.class, "data", "traffic");
        if (traffic_Usage != null) {
            final long traffic_Max = ((Number) traffic_Usage.get(1)).longValue();
            final long traffic_Used = ((Number) traffic_Usage.get(0)).longValue();
            ai.setTrafficMax(traffic_Max);
            ai.setTrafficLeft(traffic_Max - traffic_Used);
        }

        final List hoster_List = get(map, List.class, "data", "hoster");
        if (hoster_List != null) {
            final PluginFinder pluginFinder = new PluginFinder();
            final ArrayList<String> supportedHosts = new ArrayList<String>();
            synchronized (map) {
                for (Object hostListEntry : hoster_List) {
                    final Map<String, Object> hostMapEntry = (Map<String, Object>) hostListEntry;
                    // error states
                    // No traffic available
                    // Hoster temporary not available
                    // Maintenance
                    if ("ok".equalsIgnoreCase(String.valueOf(get(hostMapEntry, "state")))) {
                        supportedHosts.add(String.valueOf(get(hostMapEntry, "name")));
                    }
                }
            }
            rewriteHosterNames(map, pluginFinder);
            ai.setMultiHostSupport(this, supportedHosts, pluginFinder);
        }
        return ai;
    }

    private <T> T get(Account account, Class<T> type, String... keyPath) {
        final Map<String, Object> map;
        synchronized (ACCOUNTINFOS) {
            map = ACCOUNTINFOS.get(account);
        }
        return get(map, type, keyPath);
    }

    private <T> T get(Map<String, Object> map, Class<T> type, String... keyPath) {
        final Object ret = get(map, keyPath);
        if (ret != null && type != null && type.isAssignableFrom(ret.getClass())) {
            return (T) ret;
        }
        return null;
    }

    private Object get(Account account, String... keyPath) {
        final Map<String, Object> map;
        synchronized (ACCOUNTINFOS) {
            map = ACCOUNTINFOS.get(account);
        }
        return get(map, keyPath);
    }

    public static Object get(Map<String, Object> map, String... keyPath) {
        if (map != null && keyPath.length > 0) {
            synchronized (map) {
                if (keyPath.length == 1) {
                    return map.get(keyPath[0]);
                } else {
                    final Object ret = map.get(keyPath[0]);
                    if (ret == null || !(ret instanceof Map)) {
                        return null;
                    }
                    return get((Map<String, Object>) ret, Arrays.copyOfRange(keyPath, 1, keyPath.length));
                }
            }
        }
        return null;
    }

    @Override
    public boolean canHandle(String data) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "https://www.smoozed.com/tos";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
