package jd.plugins.hoster;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;

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
                ai.setStatus("Premium account");
                return ai;
            }
        }
        ai.setValidUntil(-1);
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
                    if (premium != null) {
                        long timeStamp = TimeFormatter.getMilliSeconds(premium, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
                        if (timeStamp <= System.currentTimeMillis()) {
                            infos.remove("premium");
                        }
                    }
                    if (infos.get("premium") != null) {
                        account.setConcurrentUsePossible(true);
                        account.setMaxSimultanDownloads(0);
                    } else {
                        account.setConcurrentUsePossible(false);
                        account.setMaxSimultanDownloads(1);
                    }
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
        try {
            if (size != null) {
                link.setDownloadSize(Long.parseLong(size));
                link.setVerifiedFileSize(Long.parseLong(size));
            }
        } catch (final Throwable e) {
        }
        if (!"online".equals(state)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }
    
    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        Map<String, String> infos = loginAPI(account, false);
        if (!infos.containsKey("premium")) {
            handleFree(link, account);
            return;
        }
        fetchFileInformation(link, infos.get("session"));
        br.getPage("https://api.oboom.com/1.0/dl?token=" + infos.get("session") + "&item=" + getFileID(link) + "&http_errors=0");
        String urlInfos[] = br.getRegex("200,\"(.*?)\",\"(.*?)\"").getRow(0);
        if (urlInfos == null || urlInfos[0] == null || urlInfos[1] == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String url = "https://" + urlInfos[0] + "/1.0/dlh?ticket=" + urlInfos[1];
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 0);
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
        handleFree(link, null);
    }
    
    private void checkShowFreeDialog() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("premAdShown", Boolean.FALSE) == false) {
                if (config.getProperty("premAdShown2") == null) {
                    File checkFile = JDUtilities.getResourceFile("tmp/oboom");
                    if (!checkFile.exists()) {
                        checkFile.mkdirs();
                        showFreeDialog("oboom.com");
                    }
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("premAdShown", Boolean.TRUE);
                config.setProperty("premAdShown2", "shown");
                config.save();
            }
        }
    }
    
    protected void showFreeDialog(final String domain) {
        if (System.getProperty("org.jdownloader.revision") != null) { /* JD2 ONLY! */
            super.showFreeDialog(domain);
            return;
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                
                @Override
                public void run() {
                    try {
                        String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        String tab = "                        ";
                        if ("de".equalsIgnoreCase(lng)) {
                            title = domain + " Free Download";
                            message = "Du lädst im kostenlosen Modus von " + domain + ".\r\n";
                            message += "Wie bei allen anderen Hostern holt JDownloader auch hier das Beste für dich heraus!\r\n\r\n";
                            message += tab + "  Falls du allerdings mehrere Dateien\r\n" + "          - und das möglichst mit Fullspeed und ohne Unterbrechungen - \r\n" + "             laden willst, solltest du dir den Premium Modus anschauen.\r\n\r\nUnserer Erfahrung nach lohnt sich das - Aber entscheide am besten selbst. Jetzt ausprobieren?  ";
                        } else {
                            title = domain + " Free Download";
                            message = "You are using the " + domain + " Free Mode.\r\n";
                            message += "JDownloader always tries to get the best out of each hoster's free mode!\r\n\r\n";
                            message += tab + "   However, if you want to download multiple files\r\n" + tab + "- possibly at fullspeed and without any wait times - \r\n" + tab + "you really should have a look at the Premium Mode.\r\n\r\nIn our experience, Premium is well worth the money. Decide for yourself, though. Let's give it a try?   ";
                        }
                        if (CrossSystem.isOpenBrowserSupported()) {
                            int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                            if (JOptionPane.OK_OPTION == result) CrossSystem.openURL(new URL("http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?" + domain + "&freedialog"));
                        }
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }
    
    @Override
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        if (acc != null && acc.getMaxSimultanDownloads() == 0) return false;
        return true;
    }
    
    public void handleFree(DownloadLink link, Account account) throws Exception {
        AtomicBoolean newSignal = new AtomicBoolean(false);
        String session = getGuestSession(false, null, newSignal);
        if (account != null) {
            Map<String, String> infos = loginAPI(account, false);
            session = infos.get("session");
        }
        fetchFileInformation(link, session);
        checkShowFreeDialog();
        br.setAllowedResponseCodes(421, 509);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        for (int i = 1; i <= 5; i++) {
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId("6LdqpO0SAAAAAJGHXo63HyalP7H4qlRs_vff0kJX");
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String code = getCaptchaCode("recaptcha", cf, link);
            br.getPage("https://www.oboom.com/1.0/dl/ticket?token=" + session + "&download_id=" + getFileID(link) + "&source=" + APPID + "&recaptcha_challenge_field=" + URLEncoder.encode(rc.getChallenge(), "UTF-8") + "&recaptcha_response_field=" + URLEncoder.encode(code, "UTF-8") + "&http_errors=0");
            if (br.containsHTML("incorrect-captcha-sol")) continue;
            break;
        }
        if (br.containsHTML("incorrect-captcha-sol")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String waitTime = br.getRegex("403,(\\d+)").getMatch(0);
        if (waitTime != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waitTime) * 1000l); }
        String urlInfos[] = br.getRegex("200,\"(.*?)\",\"(.*?)\"").getRow(0);
        if (urlInfos == null || urlInfos[0] == null || urlInfos[1] == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        sleep(30 * 1000l, link);
        br.getPage("https://api.oboom.com/1.0/dl?token=" + urlInfos[0] + "&item=" + getFileID(link) + "&auth=" + urlInfos[1] + "&http_errors=0");
        waitTime = br.getRegex("421,\".*?\",(\\d+)").getMatch(0);
        if (waitTime != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waitTime) * 1000l); }
        urlInfos = br.getRegex("200,\"(.*?)\",\"(.*?)\"").getRow(0);
        if (urlInfos == null || urlInfos[0] == null || urlInfos[1] == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String url = "https://" + urlInfos[0] + "/1.0/dlh?ticket=" + urlInfos[1];
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }
    
    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (account != null && account.getMaxSimultanDownloads() == 0) return true;
        if (downloadLink.getVerifiedFileSize() >= 0) return downloadLink.getVerifiedFileSize() < 1024 * 1024 * 1024l;
        return true;
    }
    
    @Override
    public void reset() {
    }
    
    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
    
}
