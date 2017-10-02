package jd.plugins.hoster;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.antiDDoSForHost;

import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision: 37876 $", interfaceVersion = 3, names = { "funimation.com" }, urls = { "https://(?:\\w+)\\.(?:dlvr1|cloudfront)\\.net/FunimationStoreFront/(?:\\d+)/(?:English|Japanese)/.*" })
public class FunimationCom extends antiDDoSForHost {
    static private Object                                    lock         = new Object();
    static private HashMap<Account, HashMap<String, String>> loginCookies = new HashMap<Account, HashMap<String, String>>();

    @SuppressWarnings("deprecation")
    public FunimationCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.funimation.com/log-in/");
    }

    private void downloadHls(final DownloadLink downloadLink) throws Exception {
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, downloadLink.getDownloadURL());
        if (dl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        final String premiumStatus = br.getRegex("var userState = '(\\w+)';").getMatch(0);
        if ("Subscriber".equals(premiumStatus)) {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
        } else if ("Free".equals(premiumStatus)) {
            account.setType(AccountType.FREE);
            ai.setStatus("Free Account");
            ai.setValidUntil(-1);
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "https://www.funimation.com/terms-of-use/";
    }

    @Override
    public String getDescription() {
        return "JDownloader's Funimation Plugin helps download videos and subtitles from funimation.com. Funimation provides a range of qualities, and this plugin will show all those available to you.";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        downloadLink.setProperty("valid", false);
        if (downloadLink.getDownloadURL().contains(".m3u8")) {
            downloadHls(downloadLink);
        } else if (downloadLink.getDownloadURL().contains(".mp4") || downloadLink.getDownloadURL().contains(".srt")) {
            downloadLink(downloadLink);
        }
    }

    private void downloadLink(final DownloadLink downloadLink) throws Exception {
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 1);
        if (!dl.getConnection().isContentDisposition() && !dl.getConnection().getContentType().startsWith("application/octet-stream")) {
            downloadLink.setProperty("valid", false);
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        downloadLink.setProperty("valid", false);
        login(account, false);
        if (downloadLink.getDownloadURL().contains(".m3u8")) {
            downloadHls(downloadLink);
        } else if (downloadLink.getDownloadURL().contains(".mp4") || downloadLink.getDownloadURL().contains(".srt")) {
            downloadLink(downloadLink);
        }
    }

    /**
     * Attempt to log into funimation.com using the given account. Cookies are cached to 'loginCookies'.
     *
     * @param account
     *            The account to use to log in.
     * @param refresh
     *            Should new cookies be retrieved (fresh login) even if cookies have previously been cached.
     */
    public void login(final Account account, final boolean refresh) throws Exception {
        synchronized (FunimationCom.lock) {
            try {
                this.setBrowserExclusive();
                // Load cookies from the cache if allowed, and they exist
                if (refresh == false && FunimationCom.loginCookies.containsKey(account)) {
                    final HashMap<String, String> cookies = FunimationCom.loginCookies.get(account);
                    if (cookies != null) {
                        if (cookies.containsKey("src_user_id")) {
                            // Save cookies to the browser
                            for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                                final String key = cookieEntry.getKey();
                                final String value = cookieEntry.getValue();
                                br.setCookie("funimation.com", key, value);
                            }
                            return;
                        }
                    }
                }
                br.setFollowRedirects(true);
                postPage("https://prod-api-funimationnow.dadcdigital.com/api/auth/login/", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.toString().contains("\"success\":false")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                String token = br.getRegex("\"token\":\"(\\w+)\"").getMatch(0);
                String id = br.getRegex("\"id\":(\\d+),").getMatch(0);
                br.setCookie("funimation.com", "src_token", token);
                br.setCookie("funimation.com", "src_user_id", id);
                getPage(br, "https://www.funimation.com");
                FunimationCom.loginCookies.put(account, fetchCookies("funimation.com"));
            } catch (final PluginException e) {
                FunimationCom.loginCookies.remove(account);
                throw e;
            }
        }
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        downloadLink.setProperty("valid", false);
        // Attempt to login
        if (br.getCookies("funimation.com").isEmpty()) {
            final Account account = AccountController.getInstance().getValidAccount(this);
            if (account != null) {
                try {
                    login(account, false);
                } catch (final Exception e) {
                }
            }
        }
        if ((Boolean) downloadLink.getProperty("valid", false)) {
            return AvailableStatus.TRUE;
        }
        return AvailableStatus.FALSE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    // required for decrypter
    public void getPage(final Browser ibr, final String page) throws Exception {
        super.getPage(ibr, page);
    }

    // required for decrypter
    public void postPage(final Browser ibr, final String page, final String postData) throws Exception {
        super.postPage(ibr, page, postData);
    }
}
