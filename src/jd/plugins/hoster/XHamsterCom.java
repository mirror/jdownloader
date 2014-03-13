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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xhamster.com" }, urls = { "https?://(www\\.)?([a-z]{2}\\.)?(m\\.xhamster\\.com/preview/\\d+|xhamster\\.com/(xembed\\.php\\?video=\\d+|movies/[0-9]+/.*?\\.html))" }, flags = { 0 })
public class XHamsterCom extends PluginForHost {

    public XHamsterCom(PluginWrapper wrapper) {
        super(wrapper);
        // Actually only free accounts are supported
        this.enablePremium("http://xhamsterpremiumpass.com/");
    }

    @Override
    public String getAGBLink() {
        return "http://xhamster.com/terms.php";
    }

    private static final String MOBILELINK = "http://(www\\.)?m\\.xhamster\\.com/preview/\\d+";

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
        String dllink = br.getRegex("\"(https?://\\d+\\.xhcdn\\.com/key=[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            final Regex secondway = br.getRegex("\\&srv=(https?%3A%2F%2F3\\.xhcdn\\.com)\\&file=([^<>\"]*?)\\&");
            String server = br.getRegex("\\'srv\\': \\'(.*?)\\'").getMatch(0);
            if (server == null) server = secondway.getMatch(0);
            String file = br.getRegex("\\'file\\': \\'(.*?)\\'").getMatch(0);
            if (file == null) file = secondway.getMatch(1);
            if (server == null || file == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (file.startsWith("http")) {
                // Examplelink (ID): 968106
                dllink = file;
            } else {
                // Examplelink (ID): 986043
                dllink = server + "/key=" + file;
            }
        }
        return Encoding.htmlDecode(dllink);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBr();
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) login(this.br, aa, false);
        br.getPage(downloadLink.getDownloadURL());
        // embeded correction
        if (downloadLink.getDownloadURL().contains(".com/xembed.php")) {
            String realpage = br.getRegex("main_url=(http[^\\&]+)").getMatch(0);
            if (realpage != null) {
                downloadLink.setUrlDownload(Encoding.htmlDecode(realpage));
                br.getPage(downloadLink.getDownloadURL());
            }
        }
        if (br.containsHTML("(Video Not found|403 Forbidden|>This video was deleted<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String onlyfor = br.getRegex(">([^<>\"]*?)</a>\\'s friends only</div>").getMatch(0);
        if (onlyfor != null) {
            downloadLink.getLinkStatus().setStatusText("Only downloadable for friends of " + onlyfor);
            downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "movies/[0-9]+/(.*?)\\.html").getMatch(0) + ".flv");
            return AvailableStatus.TRUE;
        }
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
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String dllink = getDllink();
        String ext = dllink.substring(dllink.lastIndexOf("."));
        if (ext == null || ext.length() > 5) ext = ".flv";
        filename = Encoding.htmlDecode(filename.trim() + ext);
        downloadLink.setFinalFileName(filename);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dllink);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(DownloadLink downloadLink) throws Exception {
        // Access the page again to get a new direct link because by checking the availability the first linkisn't valid anymore
        br.getPage(downloadLink.getDownloadURL());
        final String onlyfor = br.getRegex(">([^<>\"]*?)</a>\\'s friends only</div>").getMatch(0);
        if (onlyfor != null) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for friends of " + onlyfor);
        }
        final String dllink = getDllink();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">Video not found<")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l); }
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
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://xhamster.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBr();
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
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
                        final String c = getCaptchaCode(cf, dummyLink);
                        final String loginlink = "http://xhamster.com/ajax/login.php?act=login&ref=http%3A%2F%2Fxhamster.com%2F&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=on&_=" + System.currentTimeMillis() + "&recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response_field=" + Encoding.urlEncode(c);
                        br.getPage(loginlink);
                        if (br.containsHTML("\\'Recaptcha does not match")) continue;
                        break;
                    }
                    if (br.containsHTML("\\'Recaptcha does not match")) {
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
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Registered (free) User");
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
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
}