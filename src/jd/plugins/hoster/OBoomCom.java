package jd.plugins.hoster;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision: 24660 $", interfaceVersion = 2, names = { "oboom.com" }, urls = { "https?://(www\\.)?oboom\\.com/#?[A-Z0-9]{8}" }, flags = { 2 })
public class OBoomCom extends PluginForHost {
    
    private static Map<Account, Map<String, String>> ACCOUNTINFOS = new HashMap<Account, Map<String, String>>();
    private final String                             APPID        = "43340D9C23";
    
    public OBoomCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("https://www.oboom.com");
    }
    
    @Override
    public String getAGBLink() {
        return "https://www.oboom.com/#agb";
    }
    
    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        Map<String, String> infos = loginAPI(account, true);
        String premium = infos.get("premium");
        if (premium != null) {
            long premiumUntil = TimeFormatter.getMilliSeconds(premium, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
            ai.setValidUntil(premiumUntil);
            if (!ai.isExpired()) {
                account.setConcurrentUsePossible(true);
                account.setMaxSimultanDownloads(0);
                ai.setStatus("Premium account");
                return ai;
            }
        }
        ai.setValidUntil(-1);
        account.setConcurrentUsePossible(false);
        account.setMaxSimultanDownloads(1);
        ai.setStatus("Free Account");
        return ai;
    }
    
    private Map<String, String> loginAPI(Account account, boolean forceLogin) throws Exception {
        synchronized (ACCOUNTINFOS) {
            boolean follow = br.isFollowingRedirects();
            try {
                Map<String, String> infos = ACCOUNTINFOS.get(account);
                if (infos == null || forceLogin) {
                    br.setFollowRedirects(true);
                    String response = br.getPage("https://www.oboom.com/1.0/login?auth=" + Encoding.urlEncode(account.getUser()) + "&pass=" + PBKDF2Key(account.getPass()) + "&source=" + APPID);
                    infos = new HashMap<String, String>();
                    String keys[] = getKeys(response);
                    for (String key : keys) {
                        String value = getValue(response, key);
                        if (value != null) infos.put(key, value);
                    }
                    String premium = infos.get("premium");
                    if (premium == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, "Free accounts are not supported at the moment", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    long timeStamp = TimeFormatter.getMilliSeconds(premium, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
                    if (timeStamp < System.currentTimeMillis()) throw new PluginException(LinkStatus.ERROR_PREMIUM, "Free accounts are not supported at the moment", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    ACCOUNTINFOS.put(account, infos);
                }
                return infos;
            } catch (final Exception e) {
                ACCOUNTINFOS.remove(account);
                throw e;
            } finally {
                br.setFollowRedirects(follow);
            }
        }
    }
    
    private String[] getKeys(String response) {
        return new Regex(response, "\"([a-zA-Z0-9]+)\":").getColumn(0);
    }
    
    private String getValue(String response, String key) {
        String ret = new Regex(response, "\"" + key + "\":\\s*?\"(.*?)\"").getMatch(0);
        if (ret == null) ret = new Regex(response, "\"" + key + "\":\\s*?(\\d+)").getMatch(0);
        return ret;
    }
    
    private static String PBKDF2Key(String password) throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder(password);
        byte[] salt = sb.reverse().toString().getBytes("UTF-8");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 1000, 16 * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return HexFormatter.byteArrayToHex(hash);
    }
    
    private String getGuestSession(boolean forceNew, String forceNewIfSession, AtomicBoolean newSignal) throws Exception {
        synchronized (ACCOUNTINFOS) {
            boolean follow = br.isFollowingRedirects();
            try {
                Map<String, String> infos = ACCOUNTINFOS.get(null);
                if (infos == null || forceNew || forceNewIfSession != null && forceNewIfSession.equals(infos.get("guestSession"))) {
                    br.setFollowRedirects(true);
                    br.getPage("https://www.oboom.com/1.0/guestsession?source=" + APPID);
                    String guestSession = br.getRegex("200,.*?\"(.*?)\"").getMatch(0);
                    if (guestSession == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    infos = new HashMap<String, String>();
                    infos.put("guestSession", guestSession);
                    if (newSignal != null) newSignal.set(true);
                    ACCOUNTINFOS.put(null, infos);
                }
                if (newSignal != null) newSignal.set(false);
                return infos.get("guestSession");
            } catch (final Exception e) {
                ACCOUNTINFOS.remove(null);
                throw e;
            } finally {
                br.setFollowRedirects(follow);
            }
        }
    }
    
    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        AtomicBoolean newSignal = new AtomicBoolean(false);
        String session = getGuestSession(false, null, newSignal);
        int maxLoop = 2;
        while (maxLoop-- >= 0) {
            AvailableStatus ret = fetchFileInformation(parameter, session);
            switch (ret) {
                case TRUE:
                    return ret;
                case UNCHECKABLE:
                    if (newSignal.get()) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    session = getGuestSession(false, session, newSignal);
                    continue;
                default:
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }
    
    protected AvailableStatus fetchFileInformation(DownloadLink link, String session) throws Exception {
        String response = br.getPage("https://api.oboom.com/1.0/ls?token=" + session + "&item=" + getFileID(link));
        if (response.contains("404,\"token")) { return AvailableStatus.UNCHECKABLE; }
        String size = getValue(response, "size");
        String name = getValue(response, "name");
        String state = getValue(response, "state");
        if (name != null) link.setFinalFileName(name);
        if (size != null) link.setVerifiedFileSize(Long.parseLong(size));
        if (!"online".equals(state)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }
    
    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        Map<String, String> infos = loginAPI(account, false);
        fetchFileInformation(link, infos.get("session"));
        br.getPage("https://api.oboom.com/1.0/dl?token=" + infos.get("session") + "&item=" + getFileID(link));
        String urlInfos[] = br.getRegex("200,\"(.*?)\",\"(.*?)\"").getRow(0);
        if (urlInfos == null || urlInfos[0] == null || urlInfos[1] == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String url = "https://" + urlInfos[0] + "/1.0/dlh?ticket=" + urlInfos[1];
        br.setFollowRedirects(true);
        if (infos.containsKey("premium")) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 0);
        } else {
            /* TODO: not yet supported */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, false, 1);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }
    
    private String getFileID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "oboom\\.com/#?([A-Z0-9]{8})").getMatch(0);
    }
    
    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_FATAL, "Free Accounts are not supported at the moment");
    }
    
    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        return account != null;
    }
    
    @Override
    public void reset() {
    }
    
    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
    
}
