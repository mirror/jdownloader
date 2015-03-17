//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xhamster.com" }, urls = { "https?://(www\\.)?([a-z]{2}\\.)?(m\\.xhamster\\.com/preview/\\d+|xhamster\\.com/(xembed\\.php\\?video=\\d+|movies/[0-9]+/.*?\\.html))" }, flags = { 2 })
public class XHamsterCom extends PluginForHost {

    public XHamsterCom(PluginWrapper wrapper) {
        super(wrapper);
        // Actually only free accounts are supported
        this.enablePremium("http://xhamsterpremiumpass.com/");
        setConfigElements();
    }

    private static final String  ALLOW_MULTIHOST_USAGE           = "ALLOW_MULTIHOST_USAGE";
    private static final boolean default_allow_multihoster_usage = false;

    private void setConfigElements() {
        String user_text;
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            user_text = "Erlaube den Download von Links dieses Anbieters über Multihoster (nicht empfohlen)?\r\n<html><b>Kann die Anonymität erhöhen, aber auch die Fehleranfälligkeit!</b>\r\nAktualisiere deine(n) Multihoster Account(s) nach dem Aktivieren dieser Einstellung um diesen Hoster in der Liste der unterstützten Hoster deines/r Multihoster Accounts zu sehen (sofern diese/r ihn unterstützen).</html>";
        } else {
            user_text = "Allow links of this host to be downloaded via multihosters (not recommended)?\r\n<html><b>This might improve anonymity but perhaps also increase error susceptibility!</b>\r\nRefresh your multihoster account(s) after activating this setting to see this host in the list of the supported hosts of your multihost account(s) (in case this host is supported by your used multihost(s)).</html>";
        }
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_MULTIHOST_USAGE, JDL.L("plugins.hoster." + this.getClass().getName() + ".ALLOW_MULTIHOST_USAGE", user_text)).setDefaultValue(default_allow_multihoster_usage));
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        if (this.getPluginConfig().getBooleanProperty(ALLOW_MULTIHOST_USAGE, default_allow_multihoster_usage)) {
            return true;
        } else {
            return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        }
    }

    @Override
    public String getAGBLink() {
        return "http://xhamster.com/terms.php";
    }

    private static final String MOBILELINK = "http://(www\\.)?m\\.xhamster\\.com/preview/\\d+";
    private static final String NORESUME   = "NORESUME";
    private String              DLLINK     = null;

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("://(www\\.)?([a-z]{2}\\.)?", "://"));
        if (link.getDownloadURL().matches(MOBILELINK)) {
            link.setUrlDownload("http://xhamster.com/movies/" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0) + "/" + System.currentTimeMillis() + new Random().nextInt(10000) + ".html");
        }
    }

    /**
     * NOTE: They also have .mp4 version of the videos in the html code -> For mobile devices Those are a bit smaller in size
     * */
    public String getDllink() throws IOException, PluginException {
        String dllink = null;
        int urlmodeint = 0;
        final String urlmode = br.getRegex("url_mode=(\\d+)").getMatch(0);
        if (urlmode != null) {
            urlmodeint = Integer.parseInt(urlmode);
        }
        if (urlmodeint == 1) {
            /* Example-ID: 1815274, 1980180 */
            final Regex secondway = br.getRegex("\\&srv=(https?[A-Za-z0-9%\\.]+\\.xhcdn\\.com)\\&file=([^<>\"]*?)\\&");
            String server = br.getRegex("\\'srv\\': \\'(.*?)\\'").getMatch(0);
            if (server == null) {
                server = secondway.getMatch(0);
            }
            String file = br.getRegex("\\'file\\': \\'(.*?)\\'").getMatch(0);
            if (file == null) {
                file = secondway.getMatch(1);
            }
            if (server == null || file == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (file.startsWith("http")) {
                // Examplelink (ID): 968106
                dllink = file;
            } else {
                // Examplelink (ID): 986043
                dllink = server + "/key=" + file;
            }
        } else {
            /* E.g. url_mode == 3 */
            /* Example-ID: 685813 */
            dllink = br.getRegex("\"(https?://\\d+\\.xhcdn\\.com/key=[^<>\"]*?)\" class=\"mp4Thumb\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://\\d+\\.xhcdn\\.com/key=[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("flashvars.*?file=(http%3.*?)&").getMatch(0);
            }
        }
        DLLINK = Encoding.htmlDecode(dllink);
        return Encoding.htmlDecode(dllink);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBr();
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            login(this.br, aa, false);
        }
        br.getPage(downloadLink.getDownloadURL());
        // embeded correction
        if (downloadLink.getDownloadURL().contains(".com/xembed.php")) {
            String realpage = br.getRegex("main_url=(http[^\\&]+)").getMatch(0);
            if (realpage != null) {
                downloadLink.setUrlDownload(Encoding.htmlDecode(realpage));
                br.getPage(downloadLink.getDownloadURL());
            }
        }
        if (br.containsHTML("(403 Forbidden|>This video was deleted<)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String onlyfor = br.getRegex(">([^<>\"]*?)</a>\\'s friends only</div>").getMatch(0);
        if (onlyfor != null) {
            downloadLink.getLinkStatus().setStatusText("Only downloadable for friends of " + onlyfor);
            downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "movies/[0-9]+/(.*?)\\.html").getMatch(0) + ".flv");
            return AvailableStatus.TRUE;
        } else if (br.containsHTML("id=\\'videoPass\\'")) {
            downloadLink.getLinkStatus().setStatusText("This video is password protected");
            return AvailableStatus.TRUE;
        }
        if (downloadLink.getFinalFileName() == null || DLLINK == null) {
            final String filename = getFilename();
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setFinalFileName(filename);
        }
        if (downloadLink.getDownloadSize() <= 0) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(DLLINK);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private String getFilename() throws PluginException, IOException {
        String filename = br.getRegex("<title>(.*?) \\- xHamster\\.com</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta name=\"description\" content=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<meta name=\"keywords\" content=\"(.*?)\"").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("height=\"26\" width=.*?align=left>\\&nbsp;(.*?)</th>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("<B>Description:</B></td>.*?<td width=[0-9]+>(.*?)</td>").getMatch(0);
                    }
                }
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        DLLINK = getDllink();
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) {
            ext = ".flv";
        }
        filename = Encoding.htmlDecode(filename.trim() + ext);
        return filename;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        // Access the page again to get a new direct link because by checking the availability the first linkisn't valid anymore
        String passCode = downloadLink.getStringProperty("pass", null);
        br.getPage(downloadLink.getDownloadURL());
        final String onlyfor = br.getRegex(">([^<>\"]*?)</a>\\'s friends only</div>").getMatch(0);
        if (onlyfor != null) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for friends of " + onlyfor);
        } else if (br.containsHTML("id=\\'videoPass\\'")) {
            if (passCode == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            }
            br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode));
            if (br.containsHTML("id=\\'videoPass\\'")) {
                downloadLink.setProperty("pass", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            downloadLink.setFinalFileName(getFilename());
        }
        final String dllink = getDllink();

        boolean resume = true;
        if (downloadLink.getBooleanProperty(NORESUME, false)) {
            resume = false;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, 0);
        if (dl.getConnection().getContentType().contains("html")) {

            if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Response code 416 --> Handling it");
                if (downloadLink.getBooleanProperty(NORESUME, false)) {
                    downloadLink.setProperty(NORESUME, Boolean.valueOf(false));
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 30 * 60 * 1000l);
                }
                downloadLink.setProperty(NORESUME, Boolean.valueOf(true));
                downloadLink.setChunksProgress(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Server error 416");
            }

            br.followConnection();
            if (br.containsHTML(">Video not found<")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            logger.info("xhamster.com: Unknown error -> Retrying!");
            int timesFailed = downloadLink.getIntegerProperty("timesfailedxhamstercom_unknown", 0);
            downloadLink.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                downloadLink.setProperty("timesfailedxhamstercom_unknown", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
            } else {
                downloadLink.setProperty("timesfailedxhamstercom_unknown", Property.NULL);
                logger.info("xhamster.com: Unknown error -> Plugin is broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        downloadLink.setProperty("pass", passCode);
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://xhamster.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            // used in finally to restore browser redirect status.
            final boolean frd = br.isFollowingRedirects();
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBr();
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.getPage("http://xhamster.com/login.php");
                br.getHeaders().put("Accept", "text/javascript, application/javascript, application/ecmascript, application/x-ecmascript, */*; q=0.01");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getPage("http://xhamster.com/ajax/login.php?act=login&ref=http%3A%2F%2Fxhamster.com%2F&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=on&_=" + System.currentTimeMillis());
                // Account is fine but we need a stupid login captcha
                if (br.containsHTML("'#loginCaptchaRow'")) {
                    for (int i = 1; i <= 5; i++) {
                        final String rcID = "6Ld7YsISAAAAAN-PZ6ABWPR9y5IhwiWbGZgeoqRa";
                        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        rc.setId(rcID);
                        rc.load();
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", "xhamster.com", "http://xhamster.com", true);
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String c = getCaptchaCode("recaptcha", cf, dummyLink);
                        final String loginlink = "http://xhamster.com/ajax/login.php?act=login&ref=http%3A%2F%2Fxhamster.com%2F&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=on&_=" + System.currentTimeMillis() + "&recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response_field=" + Encoding.urlEncode(c);
                        br.getPage(loginlink);
                        if (br.containsHTML("'Recaptcha does not match")) {
                            continue;
                        }
                        break;
                    }
                    if (br.containsHTML("'Recaptcha does not match")) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiges Login Captcha!\r\nVersuche es erneut und löse das Login Captcha richtig.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nWrong login captcha!\r\nTry again and enter the login captcha correctly.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                }
                if (br.getCookie(MAINPAGE, "PWD") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                account.setProperty("lastlogin", System.currentTimeMillis());
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                account.setProperty("lastlogin", Property.NULL);
                throw e;
            } finally {
                br.setFollowRedirects(frd);
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /*
         * logic to manipulate full login. Useful for sites that show captcha when you login too many times in a given time period. Or sites
         * that present captcha to users all the time!
         */
        if (account.getStringProperty("lastlogin", null) != null && (System.currentTimeMillis() - 6 * 3480000l <= Long.parseLong(account.getStringProperty("lastlogin")))) {
            login(this.br, account, false);
            // because we have used cached login, we should verify that the cookie is still valid...
            br.getPage(MAINPAGE);
            if (br.getCookie(MAINPAGE, "PWD") == null) {
                // we should assume cookie is invalid, and perform a full login!
                br = new Browser();
                login(this.br, account, true);
            }
        } else {
            login(this.br, account, true);
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Free Account");
        account.setProperty("free", true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(this.br, account, false);
        doFree(link);
    }

    private void prepBr() {
        br.setCookie(MAINPAGE, "lang", "en");
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}