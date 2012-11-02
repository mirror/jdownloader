//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
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

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filer.net" }, urls = { "http://[\\w\\.]*?filer.net/(file[\\d]+|get|dl)/.*" }, flags = { 2 })
public class FilerNet extends PluginForHost {

    private final String        COOKIE_HOST = "http://filer.net";
    private static final String SLOTSFILLED = ">Slots filled<|>Download Slots max<|You have used all your available download-slots";
    private static Object       LOCK        = new Object();

    public FilerNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filer.net/upgrade");
    }

    private void prepBrowser() {
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());

    }

    @Override
    public String getAGBLink() {
        return "http://www.filer.net/agb.htm";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBrowser();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        final String reconWait = br.getRegex("<p>Please wait <span id=\"time\">(\\d+)</span> seconds").getMatch(0);
        if (reconWait != null) {
            link.setName(new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
            link.getLinkStatus().setStatusText("Cannot show filename while the downloadlimit is reached");
            return AvailableStatus.TRUE;
        } else if (br.containsHTML(SLOTSFILLED)) {
            link.setName(new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
            link.getLinkStatus().setStatusText("Cannot show filename while all slots are filled");
            return AvailableStatus.TRUE;
        }
        br.setFollowRedirects(false);
        if (br.containsHTML(">Not found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex(">Free Download ([^<>\"]*?)<").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // The don't show a filesize but there is an estiminate download time :D
        final Regex downloadTime = br.getRegex("<td><em>~ (\\d+):(\\d+):(\\d+)</em></td>");
        if (downloadTime.getMatches().length == 1) {
            int seconds = Integer.parseInt(downloadTime.getMatch(0)) * 60 * 60 + Integer.parseInt(downloadTime.getMatch(1)) * 60 + Integer.parseInt(downloadTime.getMatch(2));
            link.setDownloadSize((long) (seconds * 2119462.127659574));
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(SLOTSFILLED)) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "All slots are filled", 10 * 60 * 1000l);
        final String reconWait = br.getRegex("<p>Please wait <span id=\"time\">(\\d+)</span> seconds").getMatch(0);
        int wait = 0;
        if (reconWait != null) wait = Integer.parseInt(reconWait);
        if (wait < 180) {
            sleep(wait * 1001l, downloadLink);
            br.getPage(downloadLink.getDownloadURL());
        } else {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
        }
        final String token = br.getRegex("name=\"token\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (token == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage(br.getURL(), "token=" + Encoding.urlEncode(token));
        br.setFollowRedirects(false);
        int maxCaptchaTries = 5;
        br.setCookiesExclusive(true);
        int tries = 0;
        while (tries < maxCaptchaTries) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            String id = this.br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
            if (id == null) id = this.br.getRegex("Recaptcha\\.create\\(\"([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
            if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            rc.setId(id);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            tries++;
            br.postPage(br.getURL(), "recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&hash=" + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)§").getMatch(0));
            if (!br.containsHTML("google\\.com/recaptcha/")) {
                break;
            }
        }
        if (br.containsHTML("google\\.com/recaptcha/")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        final String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setAllowFilenameFromURL(true);
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    public void login(final Account account, final boolean force) throws IOException, PluginException, InterruptedException {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                prepBrowser();
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("http://filer.net/login");
                final String token = br.getRegex("name=\"_csrf_token\" value=\"([a-z0-9]+)\"").getMatch(0);
                if (token == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);

                br.postPage("http://filer.net/login_check", "_remember_me=on&_submit=&_csrf_token=" + token + "&_target_path=http%3A%2F%2Ffiler.net%2F&_username=" + Encoding.urlEncode(account.getUser()) + "&_password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(COOKIE_HOST, "REMEMBERME") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
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
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://filer.net/locale/en");
        br.getPage("http://filer.net/profile");
        final String trafficleft = br.getRegex(Pattern.compile("<th>Traffic</th>[\t\n\r ]+<td>([^<>\"]*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        final String validuntil = br.getRegex(Pattern.compile("is valid until ([A-Za-z]{3} \\d+, \\d{4} \\d{2}:\\d{2}:\\d{2} (AM|PM))", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (trafficleft == null || validuntil == null) {
            account.setValid(false);
            return ai;
        }
        ai.setTrafficLeft(trafficleft);
        ai.setValidUntil(TimeFormatter.getMilliSeconds(validuntil, "MMMM dd, yyyy h:mm:ss a", Locale.US));
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        setBrowserExclusive();
        requestFileInformation(downloadLink);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = br.getRegex("\"(/dl/[a-z0-9]+/[a-z0-9]+)\"").getMatch(0);
            if (dllink != null) dllink = "http://filer.net" + dllink;
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        // Chunks deactivated to prevent errors
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        dl.startDownload();
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