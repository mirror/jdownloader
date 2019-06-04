package jd.plugins.hoster;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import jd.PluginWrapper;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "oboom.com" }, urls = { "https?://(www\\.)?oboom\\.com/(#(id=)?|#/)?[A-Z0-9]{8}" })
public class OBoomCom extends antiDDoSForHost {
    private static Map<Account, Map<String, String>> ACCOUNTINFOS          = new HashMap<Account, Map<String, String>>();
    private final String                             APPID                 = "43340D9C23";
    private final String                             REF_TOKEN             = "REF_TOKEN";
    /* Reconnect-workaround-related */
    private static final long                        FREE_RECONNECTWAIT    = 3 * 60 * 60 * 1000L;
    private static final String                      PROPERTY_LASTDOWNLOAD = "oboom_lastdownload_timestamp";

    public OBoomCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("https://www.oboom.com");
    }

    @Override
    public String getAGBLink() {
        return "https://www.oboom.com/#agb";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        // clean links so prevent dupes and has less side effects with multihosters...
        // website redirects to domain/#fuid
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.com/#id=", "\\.com/#"));
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.com/#/", "\\.com/#"));
        final String linkID = getHost() + "://" + getFileID(link);
        try {
            link.setLinkID(linkID);
        } catch (Throwable e) {
            link.setProperty("LINKDUPEID", linkID);
        }
    }

    @Override
    public String getBuyPremiumUrl() {
        return "https://www.oboom.com/ref/C0ACB0?ref_token=" + getLatestRefID();
    }

    private String getLatestRefID() {
        String refID = "";
        try {
            final SubConfiguration pluginConfig = getPluginConfig();
            if (pluginConfig != null) {
                refID = pluginConfig.getStringProperty(REF_TOKEN, null);
                if (refID == null || refID.trim().length() == 0) {
                    refID = "";
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return refID;
    }

    private void setLatestRefID(String ID) {
        final SubConfiguration pluginConfig = getPluginConfig();
        if (pluginConfig != null) {
            pluginConfig.setPropertyWithoutMark(REF_TOKEN, ID);
        }
    }

    /**
     * defines custom browser requirements.
     */
    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.addAllowedResponseCodes(500);
        }
        return prepBr;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        Map<String, String> infos = loginAPI(account, true);
        String max = infos.get("max");
        String current = infos.get("current");
        if (max != null && current != null) {
            long limit = Long.parseLong(max);
            long free = Long.parseLong(current);
            if (limit > 0) {
                ai.setTrafficMax(limit);
                ai.setTrafficLeft(Math.max(0, free));
            } else {
                ai.setUnlimitedTraffic();
            }
        } else {
            ai.setUnlimitedTraffic();
        }
        final String premium = infos.get("premium_unix");
        if (premium != null && !"null".equalsIgnoreCase(premium.trim())) {
            long premiumUntil = Long.parseLong(premium) * 1000l;
            ai.setValidUntil(premiumUntil);
            if (!ai.isExpired()) {
                ai.setStatus("Premium Account");
                return ai;
            }
        }
        ai.setExpired(false);
        ai.setValidUntil(-1);
        ai.setStatus("Free Account");
        return ai;
    }

    private Map<String, String> loginAPI(Account account, boolean forceLogin) throws Exception {
        synchronized (ACCOUNTINFOS) {
            final boolean follow = br.isFollowingRedirects();
            try {
                Map<String, String> infos = ACCOUNTINFOS.get(account);
                if (infos == null || forceLogin) {
                    br.setFollowRedirects(true);
                    if (account.getUser() == null || account.getUser().trim().length() == 0) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    if (account.getPass() == null || account.getPass().trim().length() == 0) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    getPage("https://www.oboom.com/1/login?auth=" + Encoding.urlEncode(account.getUser()) + "&pass=" + PBKDF2Key(account.getPass()) + "&source=" + APPID);
                    final String response = br.toString();
                    if (br.containsHTML("400,\"Invalid Login") || !response.startsWith("[200")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    infos = new HashMap<String, String>();
                    final String keys[] = getKeys(response);
                    for (String key : keys) {
                        final String value = PluginJSonUtils.getJsonValue(response, key);
                        if (value != null) {
                            infos.put(key, value);
                        }
                    }
                    // infos.put("premium_unix", ("" + (System.currentTimeMillis() + 6 * 60 * 60 * 1000l) / 1000));
                    String premium_unix = infos.get("premium_unix");
                    if (premium_unix != null && premium_unix.matches("^[0-9]+$")) {
                        long timeStamp = Long.parseLong(premium_unix) * 1000l;
                        account.setProperty("PREMIUM_UNIX", timeStamp);
                        if (timeStamp <= System.currentTimeMillis()) {
                            infos.remove("premium");
                        }
                    } else {
                        infos.remove("premium");
                    }
                    if (infos.get("premium") != null) {
                        account.setProperty("PREMIUM", true);
                        account.setConcurrentUsePossible(true);
                        account.setMaxSimultanDownloads(0);
                    } else {
                        account.setProperty("PREMIUM", Property.NULL);
                        account.setConcurrentUsePossible(false);
                        account.setMaxSimultanDownloads(1);
                    }
                    ACCOUNTINFOS.put(account, infos);
                }
                return infos;
            } catch (final Exception e) {
                account.setProperty("PREMIUM", Property.NULL);
                ACCOUNTINFOS.remove(account);
                throw e;
            } finally {
                br.setFollowRedirects(follow);
            }
        }
    }

    private String[] getKeys(String response) {
        return new Regex(response, "\"([a-zA-Z0-9\\_]+)\":").getColumn(0);
    }

    private static String PBKDF2Key(String password) throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder(password);
        /* evtl muss hier UTF-16 ? */
        byte[] salt = sb.reverse().toString().getBytes("UTF-8");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 1000, 16 * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return HexFormatter.byteArrayToHex(hash);
    }

    private static Map<String, Map<String, String>> GUESTSESSION = new HashMap<String, Map<String, String>>();
    private String                                  guestIP      = null;

    private String getIP() {
        try {
            return new BalancedWebIPCheck(br.getProxy()).getExternalIP().getIP();
        } catch (final Exception e) {
            LogSource.exception(logger, e);
        }
        return null;
    }

    private Map<String, String> getGuestSession(boolean forceNew, String forceNewIfSession, AtomicBoolean newSignal) throws Exception {
        guestIP = getIP();
        synchronized (GUESTSESSION) {
            final boolean follow = br.isFollowingRedirects();
            try {
                Map<String, String> infos = GUESTSESSION.get(guestIP);
                if (infos == null || forceNew || forceNewIfSession != null && forceNewIfSession.equals(infos.get("guestSession"))) {
                    br.setFollowRedirects(true);
                    getPage("https://www.oboom.com/1/guestsession?source=" + APPID);
                    handleErrorResponseCodes();
                    String guestSession = br.getRegex("200,.*?\"(.*?)\"").getMatch(0);
                    if (guestSession == null) {
                        if (br.containsHTML("<h1>OBOOM.com is currently under heavy attack</h1>")) {
                            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "OBOOM.com is currently under heavy attack", 15 * 60 * 1000l);
                        }
                        this.handleResponseCodeErrors(br.getHttpConnection());
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    infos = new HashMap<String, String>();
                    infos.put("guestSession", guestSession);
                    if (newSignal != null) {
                        newSignal.set(true);
                    }
                    GUESTSESSION.put(guestIP, infos);
                }
                if (newSignal != null) {
                    newSignal.set(false);
                }
                return infos;
            } catch (final Exception e) {
                GUESTSESSION.remove(guestIP);
                throw e;
            } finally {
                br.setFollowRedirects(follow);
            }
        }
    }

    @Override
    public boolean checkLinks(DownloadLink[] links) {
        if (links == null || links.length == 0) {
            return true;
        }
        try {
            final StringBuilder sb = new StringBuilder();
            final HashMap<String, DownloadLink> idLinks = new HashMap<String, DownloadLink>();
            for (DownloadLink link : links) {
                final String id = getFileID(link);
                idLinks.put(id, link);
                idLinks.put("lower_" + id.toLowerCase(Locale.ENGLISH), link);
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(id);
            }
            br.setReadTimeout(60 * 1000);
            getPage("https://api.oboom.com/1/info?items=" + sb.toString() + "&http_errors=0&with_ref_token=true");
            final String fileInfos[] = br.getRegex("\\{(.*?)\\}").getColumn(0);
            if (fileInfos != null) {
                for (String fileInfo : fileInfos) {
                    final String directdownload = PluginJSonUtils.getJsonValue(fileInfo, "ddl");
                    final String id = PluginJSonUtils.getJsonValue(fileInfo, "id");
                    final String size = PluginJSonUtils.getJsonValue(fileInfo, "size");
                    final String name = PluginJSonUtils.getJsonValue(fileInfo, "name");
                    final String state = PluginJSonUtils.getJsonValue(fileInfo, "state");
                    final String refToken = PluginJSonUtils.getJsonValue(fileInfo, "ref_token");
                    DownloadLink link = idLinks.get(id);
                    if (link == null) {
                        link = idLinks.get("lower_" + id.toLowerCase(Locale.ENGLISH));
                    }
                    if (link == null) {
                        continue;
                    }
                    if (name != null) {
                        link.setFinalFileName(name);
                    }
                    link.setProperty("obm_directdownload", Boolean.parseBoolean(directdownload));
                    try {
                        if (size != null) {
                            link.setDownloadSize(Long.parseLong(size));
                            link.setVerifiedFileSize(Long.parseLong(size));
                        }
                    } catch (final Throwable e) {
                    }
                    if ("online".equals(state)) {
                        setLatestRefID(refToken);
                        link.setAvailable(true);
                    } else {
                        link.setAvailable(false);
                    }
                }
                return fileInfos.length == links.length;
            }
            return false;
        } catch (final Throwable e) {
            LogSource.exception(logger, e);
            return false;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return fetchFileInformation(parameter, null);
    }

    protected AvailableStatus fetchFileInformation(DownloadLink link, String session) throws Exception {
        final String response;
        final String ID = getFileID(link);
        if (session != null) {
            getPage("https://api.oboom.com/1/info?token=" + session + "&items=" + ID + "&http_errors=0&with_ref_token=true");
            response = br.toString();
        } else {
            getPage("https://api.oboom.com/1/info?items=" + ID + "&http_errors=0&with_ref_token=true");
            response = br.toString();
        }
        if (response.contains("404,\"token") || response.contains("403,\"token")) {
            if (session != null) {
                return AvailableStatus.UNCHECKABLE;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final String size = PluginJSonUtils.getJsonValue(response, "size");
        final String name = PluginJSonUtils.getJsonValue(response, "name");
        final String state = PluginJSonUtils.getJsonValue(response, "state");
        final String refToken = PluginJSonUtils.getJsonValue(response, "ref_token");
        if (name != null) {
            link.setFinalFileName(name);
        }
        try {
            if (size != null) {
                link.setDownloadSize(Long.parseLong(size));
                link.setVerifiedFileSize(Long.parseLong(size));
            }
        } catch (final Throwable e) {
        }
        if (!"online".equals(state)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        setLatestRefID(refToken);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        Map<String, String> usedInfos = null;
        boolean freshInfos = false;
        synchronized (ACCOUNTINFOS) {
            Map<String, String> currentInfos = ACCOUNTINFOS.get(account);
            usedInfos = loginAPI(account, false);
            freshInfos = currentInfos != usedInfos;
        }
        if (!usedInfos.containsKey("premium")) {
            handleFree(link, account);
            return;
        }
        if (AvailableStatus.UNCHECKABLE == fetchFileInformation(link, usedInfos.get("session"))) {
            refreshTokenHandling(usedInfos, account, freshInfos);
        }
        final String ID = getFileID(link);
        getPage("https://api.oboom.com/1/dl?token=" + usedInfos.get("session") + "&item=" + ID + "&http_errors=0");
        downloadErrorHandling(account);
        /* Handling for possible error 400 */
        refreshTokenHandling(usedInfos, account, freshInfos);
        String urlInfos[] = br.getRegex("200,\"(.*?)\",\"(.*?)\"").getRow(0);
        if (urlInfos == null || urlInfos[0] == null || urlInfos[1] == null) {
            if (br.toString().length() > 200) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Error: " + br.toString());
            }
        }
        String url = "http://" + urlInfos[0] + "/1/dlh?ticket=" + urlInfos[1] + "&http_errors=0";
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            if (dl.getConnection().getResponseCode() == 500) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
            br.followConnection();
            downloadErrorHandling(account);
            refreshTokenHandling(usedInfos, account, freshInfos);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void downloadErrorHandling(Account account) throws PluginException {
        if (br.containsHTML("403,\"(abused|blocked|deleted)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("404,\"ticket")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download ticket invalid", 1 * 60 * 1000l);
        }
        if (br.containsHTML("410,\"(abused|blocked|deleted)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("403,\"permission")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (br.containsHTML("500,\"internal")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Internal server error", 15 * 60 * 1000l);
        }
        if (br.containsHTML("503,\"download")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download currently unavailable", 15 * 60 * 1000l);
        }
        if (br.containsHTML("509,\"bandwidth limit exceeded")) {
            if (account != null) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Bandwidth limit exceeded", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Bandwidth limit exceeded", 30 * 60 * 1000l);
            }
        }
        String waitTime = br.getRegex("421,\"ip_blocked\",(\\d+)").getMatch(0);
        if (waitTime != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waitTime) * 1000l);
        }
        if (/*
         * HAS NOTHING TODO WITH ACCOUNT SEE http://board.jdownloader.org/showthread.php?p=317616#post317616 jdlog://6507583568141/
         * account != null &&
         */
                br.getRegex("421,\"connections\",(\\d+)").getMatch(0) != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Already downloading?", 5 * 60 * 1000l);
        }
        handleErrorResponseCodes();
    }

    private void handleErrorResponseCodes() throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "503 Website is under maintenance", 30 * 60 * 1000l);
        }
    }

    private void handleResponseCodeErrors(final URLConnectionAdapter con) throws PluginException {
        if (con == null) {
            return;
        }
        final int responsecode = con.getResponseCode();
        if (responsecode == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 10 * 60 * 1000l);
        } else if (responsecode == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        }
    }

    private void refreshTokenHandling(Map<String, String> usedInfos, Account account, final boolean freshInfos) throws PluginException {
        final boolean auth_problem = br.containsHTML("400,\"auth") || br.containsHTML("403,\"token") || br.containsHTML("404,\"token") || br.containsHTML("403,\"resume") || br.containsHTML("421,\"connections");
        if (auth_problem && freshInfos == false) {
            /* only retry on NON-fresh tokens */
            if (account != null) {
                synchronized (ACCOUNTINFOS) {
                    if (ACCOUNTINFOS.get(account) == usedInfos) {
                        ACCOUNTINFOS.remove(account);
                    }
                }
            } else {
                synchronized (GUESTSESSION) {
                    if (GUESTSESSION.get(guestIP) == usedInfos) {
                        GUESTSESSION.remove(guestIP);
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } else if (auth_problem) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error authentification failed (?)", 1 * 60 * 1000l);
        }
    }

    private String getFileID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "oboom\\.com/(#(id=)?|#/)?([A-Z0-9]{8})").getMatch(2);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        handleFree(link, null);
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        if (acc != null && acc.getMaxSimultanDownloads() == 0) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    public void handleFree(final DownloadLink link, final Account account) throws Exception {
        int maxchunks = 1;
        boolean resumable = false;
        String dllink = null;
        AtomicBoolean freshInfos = new AtomicBoolean(false);
        final String ID = getFileID(link);
        Map<String, String> usedInfos = null;
        String session = null;
        if (account != null) {
            synchronized (ACCOUNTINFOS) {
                Map<String, String> currentInfos = ACCOUNTINFOS.get(account);
                usedInfos = loginAPI(account, false);
                session = usedInfos.get("session");
                freshInfos.set(currentInfos != usedInfos);
            }
        } else {
            usedInfos = getGuestSession(false, null, freshInfos);
            session = usedInfos.get("guestSession");
        }
        if (AvailableStatus.UNCHECKABLE == fetchFileInformation(link, session) && session != null) {
            refreshTokenHandling(usedInfos, account, freshInfos.get());
        }
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        if (session == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setAllowedResponseCodes(421, 509);
        if (link.getBooleanProperty("obm_directdownload", false)) {
            resumable = true;
            maxchunks = 0;
            dllink = link.getDownloadURL();
        } else {
            this.br.postPage("https://www." + this.getHost() + "/1.0/download/config", "token=" + session);
            int pre_download_wait = 0;
            final String wait_pre_download_str = PluginJSonUtils.getJsonValue(this.br, "waiting");
            if (wait_pre_download_str == null || !wait_pre_download_str.matches("\\d+")) {
                pre_download_wait = 30;
            } else {
                pre_download_wait = Integer.parseInt(wait_pre_download_str);
            }
            if (this.br.containsHTML("\"blocked_wait\"")) {
                /*
                 * 2016-10-17: Information about limits: Via browser: Reconnect limits are only on IP (free account also). After a
                 * reconnect, a new download is possible via free account even if it has been previously used for downloading. Via API:
                 * Reconnect limits are bound to free accounts and also IP. After a reconnect the waittime will remain on free accounts!
                 */
                /* E.g. [400,"blocked_wait"] */
                long waittime;
                final long timestamp_lastdownload = this.getPluginConfig().getLongProperty(PROPERTY_LASTDOWNLOAD, 0);
                if (timestamp_lastdownload > 0) {
                    final long time_next_download_possible = timestamp_lastdownload + FREE_RECONNECTWAIT;
                    if (time_next_download_possible > System.currentTimeMillis()) {
                        /*
                         * We have a timestamp of the last downloadstart which means we can calculate how long we have to wait. This will
                         * usually be the case!
                         */
                        waittime = time_next_download_possible - System.currentTimeMillis();
                    } else {
                        /* Fallback - we know that we have to wait but for some reason we have no idea how long --> Use default waittime. */
                        waittime = FREE_RECONNECTWAIT - (System.currentTimeMillis() - timestamp_lastdownload);
                    }
                } else {
                    /*
                     * We do not have a timestamp of the last downloadstart which means we will simply wait the default waittime between
                     * downloads.
                     */
                    waittime = FREE_RECONNECTWAIT;
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
            }
            boolean captchaFailed = true;
            for (int i = 1; i <= 5; i++) {
                final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6Lc7b0IUAAAAAJ7LJfEl9rYKtcxoqyOpuiCzw0eI");
                final String recaptchaV2Response = rc2.getToken();
                if (recaptchaV2Response == null) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                getPage("https://www.oboom.com/1/dl/ticket?token=" + session + "&download_id=" + ID + "&source=" + APPID + "&recaptcha_response_field=&g-recaptcha-response=" + URLEncoder.encode(recaptchaV2Response, "UTF-8") + "&http_errors=0");
                if (br.containsHTML("incorrect-captcha-sol") || br.containsHTML("400,\"captcha-timeout")) {
                    continue;
                }
                if (br.containsHTML("400,\"Forbidden")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Try again later.", 5 * 60 * 1000l);
                }
                captchaFailed = false;
                break;
            }
            if (captchaFailed) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (br.containsHTML("400,\"slot_error\"")) {
                /* country slot block. try again in 5 minutes */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Try again later.", 5 * 60 * 1000l);
            }
            String waitTime = br.getRegex("403,(\\-?\\d+)").getMatch(0);
            if (waitTime != null) {
                /* there is already a download running.? */
                if (Integer.parseInt(waitTime) < 0) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waitTime) * 1000l);
            }
            downloadErrorHandling(account);
            String urlInfos[] = br.getRegex("200,\"(.*?)\",\"(.*?)\"").getRow(0);
            if (urlInfos == null || urlInfos[0] == null || urlInfos[1] == null) {
                if (br.toString().length() > 200) {
                    logger.info("Unknown error - probably downloadlimit reached!");
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Error: " + br.toString());
                }
            }
            sleep(pre_download_wait * 1001l, link);
            getPage("https://api.oboom.com/1/dl?token=" + urlInfos[0] + "&item=" + ID + "&auth=" + urlInfos[1] + "&http_errors=0");
            downloadErrorHandling(account);
            urlInfos = br.getRegex("200,\"(.*?)\",\"(.*?)\"").getRow(0);
            if (urlInfos == null || urlInfos[0] == null || urlInfos[1] == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = "http://" + urlInfos[0] + "/1/dlh?ticket=" + urlInfos[1] + "&http_errors=0";
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!dl.getConnection().isContentDisposition()) {
            if (dl.getConnection().getResponseCode() == 500) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
            br.followConnection();
            downloadErrorHandling(account);
            refreshTokenHandling(usedInfos, account, freshInfos.get());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Save property to be able to calculate correct waittime for next free download. */
        this.getPluginConfig().setProperty(PROPERTY_LASTDOWNLOAD, System.currentTimeMillis());
        dl.startDownload();
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account != null && account.getMaxSimultanDownloads() == 0) {
            return true;
        }
        if (downloadLink.getVerifiedFileSize() >= 0) {
            return downloadLink.getVerifiedFileSize() < 1024 * 1024 * 1024l;
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
