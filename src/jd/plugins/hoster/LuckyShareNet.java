//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "luckyshare.net" }, urls = { "http://(www\\.)?luckyshare\\.net/\\d+" }, flags = { 2 })
public class LuckyShareNet extends PluginForHost {

    public LuckyShareNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://luckyshare.net/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://luckyshare.net/termsofservice";
    }

    private final String           MAINPAGE      = "http://luckyshare.net/";
    private static Object          LOCK          = new Object();
    private static StringContainer AGENT         = new StringContainer(null);
    private static AtomicBoolean   FAILED409     = new AtomicBoolean(false);
    private final String           ONLYBETAERROR = "Downloading from luckyshare.net is only possible with the JDownloader 2 BETA";
    private static AtomicInteger   maxPrem       = new AtomicInteger(1);

    public static class StringContainer {
        public String string = null;

        public StringContainer(String string) {
            this.string = string;
        }

        public void set(String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            if (con.getResponseCode() == 409 && oldStyle()) {
                // Hier krachts
                link.getLinkStatus().setStatusText(ONLYBETAERROR);
                FAILED409.set(true);
                return AvailableStatus.UNCHECKABLE;
            }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(There is no such file available|<title>LuckyShare \\- Download</title>)")) {
            // Some links only work via account and are shown as "offline"
            // without account
            final Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa != null) {
                login(aa, false, null);
                try {
                    con = br.openGetConnection(link.getDownloadURL());
                    link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                    if (!con.getContentType().contains("html")) {
                        link.setDownloadSize(con.getLongContentLength());
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    return AvailableStatus.TRUE;
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h1 class=\\'file_name\\'>([^<>\"/]+)</h1>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>LuckyShare \\- ([^<>\"/]+)</title>").getMatch(0);
        /* It might be an empty filename - as long as we get the size its all fine */
        if (filename == null) filename = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        String filesize = br.getRegex("<span class=\\'file_size\\'>Filesize: ([^<>\"/]+)</span>").getMatch(0);
        if (filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (oldStyle() && FAILED409.get()) throw new PluginException(LinkStatus.ERROR_FATAL, ONLYBETAERROR);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        String dllink = downloadLink.getDownloadURL();
        final String filesizelimit = br.getRegex(">Files with filesize over ([^<>\"\\'/]+) are available only for Premium Users").getMatch(0);
        if (filesizelimit != null) throw new PluginException(LinkStatus.ERROR_FATAL, "Free users can only download files up to " + filesizelimit);
        if (br.containsHTML("This file is Premium only\\. Only Premium Users can download this file")) throw new PluginException(LinkStatus.ERROR_FATAL, "Only Premium Users can download this file");
        String reconnectWait = br.getRegex("id=\"waitingtime\">(\\d+)</span>").getMatch(0);
        if (reconnectWait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(reconnectWait) * 1001l);
        final String rcID = br.getRegex("Recaptcha\\.create\\(\"([^<>\"/]+)\"").getMatch(0);
        if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        Browser ajax = br.cloneBrowser();
        prepareHeader(ajax, dllink);
        String hash = getHash(ajax, dllink);
        sleep(getWaitTime(ajax) * 1001l, downloadLink);

        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);

        for (int i = 0; i <= 5; i++) {
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            c = c != null && "".equals(c) ? c = null : c;

            /* simple 'reload button' method */
            while (c == null) {
                rc.reload();
                cf = rc.downloadCaptcha(getLocalCaptchaFile());
                c = getCaptchaCode(cf, downloadLink);
                c = c != null && "".equals(c) ? c = null : c;
            }

            try {
                ajax.getPage("http://luckyshare.net/download/verify/challenge/" + rc.getChallenge() + "/response/" + c.replaceAll("\\s", "%20") + "/hash/" + hash);
            } catch (Throwable e) {
                if (ajax.getHttpConnection().getResponseCode() == 500) continue;
            }
            if (ajax.containsHTML("<strong>Wait:</strong>")) {
                reconnectWait = br.getRegex("id=\"waitingtime\">(\\d+)</span>").getMatch(0);
                if (reconnectWait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWait) * 1001l);
            }
            if (!ajax.getRegex("\"link\":\"http:[^<>\"\\']*?\"").matches()) {
                hash = getHash(ajax, dllink);
                sleep(getWaitTime(ajax) * 1001l, downloadLink);
                rc.reload();
                try {
                    invalidateLastChallengeResponse();
                } catch (final Throwable e) {
                }
                continue;
            } else {
                try {
                    validateLastChallengeResponse();
                } catch (final Throwable e) {
                }
            }
            break;
        }
        if (ajax.containsHTML("(Verification failed|You can renew the verification image by clicking on a corresponding button near the validation input area)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (ajax.containsHTML("(Hash expired|Please supply a valid hash)")) throw new PluginException(LinkStatus.ERROR_FATAL, "Plugin outdated!");
        dllink = ajax.getRegex("\"link\":\"(http:[^<>\"\\']*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private int getWaitTime(final Browser b) {
        int wait = 30;
        String waittime = b.getRegex("\"time\":(\\d+)").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        return wait;
    }

    private void prepBrowser(final Browser b) {
        if (AGENT.string == null) {
            synchronized (AGENT) {
                /* we first have to load the plugin, before we can reference it */
                JDUtilities.getPluginForHost("mediafire.com");
                AGENT.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
            }
        }
        b.getHeaders().put("User-Agent", AGENT.toString());
        b.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        b.setReadTimeout(3 * 60 * 1000);
        b.setConnectTimeout(3 * 60 * 1000);
        try {
            /* not available in old stable */
            br.setAllowedResponseCodes(new int[] { 409 });
        } catch (Throwable e) {
        }
    }

    private void prepareHeader(final Browser b, final String s) {
        b.getHeaders().put("Accept-Encoding", "deflate");
        b.getHeaders().put("Accept-Charset", null);
        b.getHeaders().put("Accept", "*/*");
        b.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        b.getHeaders().put("Referer", s);
        b.getHeaders().put("Pragma", null);
        b.getHeaders().put("Cache-Control", null);
    }

    private String getHash(final Browser b, final String s) {
        try {
            b.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            b.getPage("http://luckyshare.net/download/request/type/time/file/" + new Regex(s, "(\\d+)$").getMatch(0));
        } catch (Throwable e) {
            return null;
        }
        prepareHeader(b, s);
        return b.getRegex("\"hash\":\"([a-z0-9]+)\"").getMatch(0);
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, String> login(final Account account, final boolean force, AtomicBoolean fresh) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBrowser(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        if (fresh != null) fresh.set(false);
                        return cookies;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("http://luckyshare.net/auth/login");
                final String token = br.getRegex("type=\"hidden\" name=\"token\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (token == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                br.postPage("http://luckyshare.net/auth/login", "token=" + Encoding.urlEncode(token) + "&remember=&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (!br.containsHTML(">Logout</a>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                if (fresh != null) fresh.set(true);
                return cookies;
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account, true, null);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://luckyshare.net/account/");
        final String filesNum = br.getRegex("<strong>Number of files:</strong><br /><span>(\\d+)</span>").getMatch(0);
        if (filesNum != null) ai.setFilesNum(Integer.parseInt(filesNum));
        String space = br.getRegex("<strong>Storage Used:</strong><br /><span>([^<>\"]*?)</span></td>").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("<strong>Pro Membership Valid Until:</strong><br /><span>[A-Za-z]+, (\\d{2} [A-Za-z]+ \\d{4} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        if (expire == null) {
            try {
                maxPrem.set(1);
                // free accounts can still have captcha.
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
                // not available in old Stable 0.9.581
            }
            account.setProperty("freeacc", true);
            ai.setStatus("Registered (free) user");
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy hh:mm:ss", null));
            try {
                maxPrem.set(20);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                // not available in old Stable 0.9.581
            }
            account.setProperty("freeacc", false);
            ai.setStatus("Premium user");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (oldStyle() && FAILED409.get()) { throw new PluginException(LinkStatus.ERROR_FATAL, ONLYBETAERROR); }
        final AtomicBoolean fresh = new AtomicBoolean(false);
        HashMap<String, String> cookies = login(account, false, fresh);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (account.getBooleanProperty("freeacc", false)) {
            if (!br.containsHTML(">Logout</a>")) {
                refreshCookies(fresh, cookies, account);
                br.getPage(link.getDownloadURL());
            }

        } else {
            String dllink = br.getRedirectLocation();
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                refreshCookies(fresh, cookies, account);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private void refreshCookies(final AtomicBoolean fresh, final HashMap<String, String> cookies, final Account account) throws PluginException {
        logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
        synchronized (LOCK) {
            final Object ret = account.getProperty("cookies", null);
            if (fresh.get()) {
                account.setProperty("cookies", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (cookies == ret || ret == null) {
                account.setProperty("cookies", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
            }
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private boolean oldStyle() {
        String style = System.getProperty("ftpStyle", null);
        if ("new".equalsIgnoreCase(style)) return false;
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        int rev = Integer.parseInt(prev);
        if (rev < 10000) return true;
        return false;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
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