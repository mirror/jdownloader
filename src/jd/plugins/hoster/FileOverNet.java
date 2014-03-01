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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileover.net" }, urls = { "http(s)?://(www\\.)?fileover\\.net/\\d+" }, flags = { 2 })
public class FileOverNet extends PluginForHost {

    private static final String MAINPAGE = "http://fileover.net/";

    private static Object       LOCK     = new Object();

    public FileOverNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fileover.net/premium");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("https://", "http://"));
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage(MAINPAGE + "user/account");
        ai.setUnlimitedTraffic();
        // <p>Mon, 30 May 2011 15:39:10 +0000</p>
        String expire = br.getRegex("<h2>Expires</h2>[\t\n\r ]+<p>[A-Za-z]+, (.*?) (\\+|\\-)\\d+</p>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", null));
        }
        ai.setStatus("Premium User");
        if (!br.containsHTML("<h2>Active</h2>[\t\n\r ]+<p>Yes</p>")) account.setValid(false);
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "https://fileover.net/terms-of-service/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        int minutes = 0, seconds = 0;
        Regex limits = br.getRegex(">You have to wait: (\\d+) minutes (\\d+) seconds");
        String mins = limits.getMatch(0);
        String secs = limits.getMatch(1);
        if (secs == null) secs = br.getRegex(">You have to wait: (\\d+) seconds").getMatch(0);
        if (mins != null) minutes = Integer.parseInt(mins);
        if (secs != null) seconds = Integer.parseInt(secs);
        int waittime = ((60 * minutes) + seconds + 1) * 1000;
        if (waittime > 1000) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
        br.setFollowRedirects(false);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String fileID = new Regex(downloadLink.getDownloadURL(), "fileover\\.net/(\\d+)").getMatch(0);
        br.getPage("https://fileover.net/ax/timereq.flo?" + fileID);
        String hash = br.getRegex("hash\":\"(.*?)\"").getMatch(0);
        if (hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("https://fileover.net/ax/timepoll.flo?file=" + fileID + "&hash=" + hash);
        Form recaptchaForm = new Form();
        recaptchaForm.setMethod(MethodType.POST);
        recaptchaForm.setAction("https://fileover.net/ax/timepoll.flo");
        recaptchaForm.put("file", fileID);
        recaptchaForm.put("hash", hash);
        boolean failed = true;
        for (int i = 0; i <= 5; i++) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setForm(recaptchaForm);
            String id = this.br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
            rc.setId(id);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.setCode(c);
            if (br.containsHTML("google\\.com/recaptcha/api/")) {
                rc.reload();
                continue;
            }
            failed = false;
            break;
        }
        if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (br.containsHTML("Not Ready")) {
            logger.info(br.toString());
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
        }
        String dllink = br.getRegex("</style></head><body><a href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://\\d+\\.pool\\.fileover\\.net/\\d+/[a-z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.getRedirectLocation() == null) br.getPage(link.getDownloadURL());
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            logger.warning("Final downloadlink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        // Multiple downloads only work when saving cookies, i think they block
        // users on too many logins!
        synchronized (LOCK) {
            this.setBrowserExclusive();
            br.setCookiesExclusive(true);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = account.getUser().matches(account.getStringProperty("name", account.getUser()));
            if (acmatch) acmatch = account.getPass().matches(account.getStringProperty("pass", account.getPass()));
            if (acmatch && ret != null && ret instanceof HashMap<?, ?>) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (cookies.containsKey("SSL") && account.isValid() && !force) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(MAINPAGE, key, value);
                    }
                    return;
                }
            }
            br.postPage(MAINPAGE + "api/users/login/", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            if (br.getCookie(MAINPAGE, "hash") == null || br.getCookie(MAINPAGE, "email") == null || br.getCookie(MAINPAGE, "sess") == null) {
                final String lang = System.getProperty("user.language");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account Typ!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or unsupported account type!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", account.getUser());
            account.setProperty("pass", account.getPass());
            account.setProperty("cookies", cookies);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("/deleted/") || br.containsHTML("(<title>No such file \\| Fileover\\.Net\\! \\- Error</title>|>No such file<|The following file is unavailable\\.|>Not found or deleted by a user\\.<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h2 style=\"text\\-align: center; padding: 0 50px 10px 50px; word\\-wrap: break\\-word;\">(.*?)</h2>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?) \\| Fileover\\.Net\\! \\- Download</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        // Filesize is only shown if no limits are reached
        String filesize = br.getRegex(">File Size: (.*?)</h3>").getMatch(0);
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
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