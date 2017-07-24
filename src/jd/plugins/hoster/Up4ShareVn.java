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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "up.4share.vn" }, urls = { "https?://(?:www\\.)?(?:up\\.)?4share\\.vn/f/[a-f0-9]{16}" })
public class Up4ShareVn extends PluginForHost {
    private static final String MAINPAGE = "https://up.4share.vn/";
    private static Object       LOCK     = new Object();
    private static final String NOCHUNKS = "NOCHUNKS";

    public Up4ShareVn(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://up.4share.vn/?act=gold");
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("up.4share.vn/", "4share.vn/"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        prepBR();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.containsHTML(">FID Không hợp lệ\\!|file not found|(F|f)ile (này)? đã bị xóa|File không tồn tại?| Error: FileLink da bi xoa|>Xin lỗi Bạn, file này không còn tồn tại|File suspended:") || !this.br.getURL().matches(".+[a-f0-9]{16}$")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex(">\\s*Tên File\\s*:\\s*<strong>([^<>\"]*?)</strong>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title> 4Share\\.vn \\-([^<>\"]*?)</title>").getMatch(0);
        }
        final String filesize = br.getRegex(">\\s*Kích thước\\s*:\\s*<strong>\\s*(\\d+(?:\\.\\d+)?\\s*(?:B(?:yte)?|KB|MB|GB))\\s*</strong>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(filename.trim());
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        account.setValid(true);
        getPage("/member");
        final String[] traffic = br.getRegex("<strong>\\s*([0-9\\.,]+ [GMKB]+)\\s*</strong>\\s*/Tổng số\\s*:\\s*<strong>\\s*([0-9\\.,]+ [GMKB]+)\\s*</strong>").getRow(0);
        if (traffic != null) {
            final long max = SizeFormatter.getSize(traffic[1]);
            final long left = max - SizeFormatter.getSize(traffic[0]);
            ai.setTrafficMax(max);
            ai.setTrafficLeft(left);
        } else {
            ai.setUnlimitedTraffic();
        }
        final String expire = br.getRegex("Ngày hết hạn: <b>(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH));
        }
        ai.setStatus("Premium (VIP) User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://up.4share.vn/?act=terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String dllink = null;
        // Can be skipped
        // int wait = 60;
        // final String waittime = br.getRegex("var counter=(\\d+);").getMatch(0);
        // if (waittime != null) {
        // wait = Integer.parseInt(waittime);
        // }
        // sleep(wait * 1001l, downloadLink);
        final Recaptcha rc = new Recaptcha(br, this);
        rc.findID();
        for (int i = 0; i <= 3; i++) {
            rc.load();
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode("recaptcha", cf, downloadLink);
            br.postPage(downloadLink.getDownloadURL(), "submit=DOWNLOAD+FREE&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
            dllink = br.getRedirectLocation();
            if (dllink == null && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                continue;
            }
            break;
        }
        if (dllink == null && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        if (dllink == null) {
            handleErrorsGeneral();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int maxChunks = 1;
        if (downloadLink.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleErrorsGeneral();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(Up4ShareVn.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(Up4ShareVn.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 chunk errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(Up4ShareVn.NOCHUNKS, false) == false) {
                downloadLink.setProperty(Up4ShareVn.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        getPage(link.getDownloadURL());
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = br.getRegex("class=''> <a href='(https?://.*?)'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("('|\")(https?://sv\\d+\\.4share\\.vn/[^<>\"]*?)\\1").getMatch(1);
            }
        }
        if (dllink == null) {
            handleErrorsGeneral();
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int maxChunks = 0;
        if (link.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            handleErrorsGeneral();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(Up4ShareVn.NOCHUNKS, false) == false) {
                    link.setProperty(Up4ShareVn.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 chunk errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(Up4ShareVn.NOCHUNKS, false) == false) {
                link.setProperty(Up4ShareVn.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            boolean redirect = this.br.isFollowingRedirects();
            try {
                /* Load cookies */
                this.setBrowserExclusive();
                prepBR();
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
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                getPage("https://up.4share.vn/");
                postPage("https://up.4share.vn/index/login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember_login=on");
                final String lang = System.getProperty("user.language");
                if (br.getCookie(MAINPAGE, "info1") == null || br.getCookie(MAINPAGE, "info2") == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (!br.containsHTML("TK <b>VIP") && !br.containsHTML("Hạn sử dụng VIP: \\d{2}-\\d{2}-\\d{4}") && !br.containsHTML("Còn hạn sử dụng VIP đến: \\d{2}-\\d{2}-\\d{4}")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Account Typ!\r\nDas ist ein kostenloser Account.\r\nJDownloader unterstützt keine kostenlosen Accounts für diesen Hoster!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid account type!\r\nThis is a free account. JDownloader only supports premium (VIP) accounts for this host!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            } finally {
                this.br.setFollowRedirects(redirect);
            }
        }
    }

    private void getPage(final String string) throws Exception {
        if (string == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (int i = 0; i != 4; i++) {
            try {
                br.getPage(string);
            } catch (final BrowserException e) {
                if (e.getCause() != null && e.getCause().toString().contains("ConnectException")) {
                    if (i + 1 == 4) {
                        throw e;
                    } else {
                        continue;
                    }
                } else {
                    throw e;
                }
            }
            break;
        }
    }

    private void postPage(String string, String string2) throws Exception {
        if (string == null || string2 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (int i = 0; i != 4; i++) {
            try {
                br.postPage(string, string2);
            } catch (final BrowserException e) {
                if (e.getCause() != null && e.getCause().toString().contains("ConnectException")) {
                    continue;
                } else {
                    throw e;
                }
            }
            break;
        }
    }

    private void prepBR() {
        this.br.setReadTimeout(2 * 60 * 1000);
        this.br.setConnectTimeout(2 * 60 * 1000);
    }

    private void handleErrorsGeneral() throws PluginException {
        if (this.br.containsHTML("File này tạm dừng Download do yêu cầu của Người upload|Thông báo với Administrator\\!")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 30 * 60 * 1000l);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
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

    public boolean hasAutoCaptcha() {
        // recaptcha
        return false;
    }
}