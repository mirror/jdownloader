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
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ugoupload.net" }, urls = { "http://(www\\.)?ugoupload\\.net/[A-Za-z0-9]+" }, flags = { 2 })
public class UGoUploadNet extends PluginForHost {

    public UGoUploadNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.ugoupload.net/register.html");
    }

    @Override
    public String getAGBLink() {
        return "http://www.ugoupload.net/terms.html";
    }

    // Captcha type: now: recaptcha 20141025 was solvemedia previous time tested.

    private static Object                          LOCK                     = new Object();
    private final String                           MAINPAGE                 = "http://ugoupload.net";
    private final boolean                          RESUME                   = false;
    private final int                              MAXCHUNKS                = 1;

    private static final String                    INVALIDLINKS             = "http://(www\\.)?ugoupload\\.net/(login|register|report)";

    private static final String                    PREMIUMONLY              = "?e=You+must+register+for+a+premium+account+to+download+files+of+this+size";
    private static final String                    PREMIUMONLYUSERTEXT      = "Only downloadable for premium users";
    private static final String                    SIMULTANDLSLIMIT         = "?e=You+have+reached+the+maximum+concurrent+downloads";
    private static final String                    SIMULTANDLSLIMITUSERTEXT = "Max. simultan downloads limit reached, wait or reconnect to start more downloads from this host";
    private static final String                    DLSLIMIT                 = "?e=You+must+wait+";
    private static final String                    DLSLIMITUSERTEXT         = "Max. downloads limit reached, wait or reconnect to start more downloads from this host";
    private static final String                    ERRORFILE                = "?e=Error%3A+Could+not+open+file+for+reading";
    private static final String                    OVERLOADED               = "?e=The+site+currently+overloaded+";
    private static final String                    OVERLOADEDUSERTEXT       = "The site is currently overloaded";

    private boolean                                captchaUsed              = false;
    private long                                   requestTime              = System.currentTimeMillis();
    private static final String                    regexCaptchaRecaptcha    = "api\\.recaptcha\\.net|google\\.com/recaptcha/api/";
    private static final String                    regexCaptchaSolvemedia   = "solvemedia\\.com/papi/";
    private static final String                    regexCaptchaTypes        = regexCaptchaRecaptcha + "|" + regexCaptchaSolvemedia;

    protected static final AtomicReference<String> userAgent                = new AtomicReference<String>(null);

    /** Uses same script as filegig.com */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getDownloadURL().matches(INVALIDLINKS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        prepBrowser();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        this.requestTime = System.currentTimeMillis();
        if (br.getURL().contains(PREMIUMONLY)) {
            link.setName(new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
            link.getLinkStatus().setStatusText(PREMIUMONLYUSERTEXT);
            return AvailableStatus.TRUE;
        }
        if (br.getURL().contains(SIMULTANDLSLIMIT)) {
            link.setName(new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
            link.getLinkStatus().setStatusText(SIMULTANDLSLIMITUSERTEXT);
            return AvailableStatus.TRUE;
        }
        if (br.getURL().contains(DLSLIMIT)) {
            link.setName(new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
            link.getLinkStatus().setStatusText(DLSLIMITUSERTEXT);
            return AvailableStatus.TRUE;
        }
        if (br.getURL().contains("ugoupload.net/index.html") || br.containsHTML("<title>Index of") || br.containsHTML(">File has been removed due to inactivity")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String[] fileInfo = br.getRegex("<strong>\\s*([^\r\n]+)\\(([\\d\\.]+ (?:KB|MB|GB))\\)<\\s*(?:/br|br\\s*/)\\s*>").getRow(0);
        if (fileInfo == null || fileInfo.length == 0) {
            // is very slow regex!
            fileInfo = br.getRegex("(.*?) \\(([\\d\\.]+ (KB|MB|GB))\\)").getRow(0);
            if ((fileInfo == null || fileInfo.length == 0) || fileInfo[0] == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final String filename = fileInfo[0];
        final String filesize = (fileInfo[1] != null ? fileInfo[1] : null);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);

        if (br.getURL().contains(PREMIUMONLY)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYUSERTEXT);
        }
        if (br.getURL().contains(SIMULTANDLSLIMIT)) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, SIMULTANDLSLIMITUSERTEXT, 1 * 60 * 1000l);
        }
        if (br.getURL().contains(ERRORFILE) || br.containsHTML("Error: Could not open file for reading.")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
        }
        if (br.getURL().contains(DLSLIMIT)) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
        }
        if (br.getURL().contains(OVERLOADED)) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, OVERLOADEDUSERTEXT, 3 * 60 * 1000l);
        }
        final String dllink = br.getRegex("href='(https?://(?:\\w+\\.)?ugoupload.net/.*?\\?pt=[a-zA-Z0-9%]+)").getMatch(0);
        String waittime = br.getRegex("\\$\\(\\'\\.download\\-timer\\-seconds\\'\\)\\.html\\((\\d+)\\);").getMatch(0);
        {
            waittime = waittime != null ? waittime : "420";
            // remove one second from past, to prevent returning too quickly.
            final long passedTime = ((System.currentTimeMillis() - requestTime) / 1000) - 1;
            final long tt = Long.parseLong(waittime) - passedTime;
            logger.info("WaitTime detected: " + waittime + " second(s). Elapsed Time: " + (passedTime > 0 ? passedTime : 0) + " second(s). Remaining Time: " + tt + " second(s)");
            if (tt > 0) {
                sleep(tt * 1000l, downloadLink);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, (dllink != null ? dllink : downloadLink.getDownloadURL() + "?pt=2"), RESUME, MAXCHUNKS);
        if (!dl.getConnection().isContentDisposition()) {
            if (br.getURL().contains(ERRORFILE) || br.containsHTML("Error: Could not open file for reading\\.")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
            }
            br.followConnection();

            /* Untested! */
            String wait_long = br.getRegex("<a class=\"link btn\\-free\" href=\"#\" onClick=\"display\\(\\); return false;\">([^<>\"]*?)</a>").getMatch(0);
            if (wait_long != null && !wait_long.contains("DOWNLOAD")) {
                wait_long = wait_long.trim();
                String tmphrs = new Regex(wait_long, "(\\d+)\\s+hours?").getMatch(0);
                String tmpmin = new Regex(wait_long, "(\\d+)\\s+minutes?").getMatch(0);
                String tmpsec = new Regex(wait_long, "(\\d+)\\s+seconds?").getMatch(0);
                if (tmphrs == null && tmpmin == null && tmpsec == null) {
                    logger.info("Waittime regexes seem to be broken");
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
                } else {
                    int minutes = 0, seconds = 0, hours = 0, days = 0;
                    if (tmphrs != null) {
                        hours = Integer.parseInt(tmphrs);
                    }
                    if (tmpmin != null) {
                        minutes = Integer.parseInt(tmpmin);
                    }
                    if (tmpsec != null) {
                        seconds = Integer.parseInt(tmpsec);
                    }
                    int waittime_long = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                    logger.info("Detected waittime #2, waiting " + waittime_long + "milliseconds");
                    /* Not enough wait time to reconnect -> Wait short and retry */
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime_long);
                }
            }
            Form captchaForm = getCaptchaForm();
            if (captchaForm != null && captchaForm.containsHTML(regexCaptchaRecaptcha)) {
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                final String rcID = captchaForm.getRegex("(?:/recaptcha/api/noscript|/recaptcha/api/challenge)\\?k=([^<>\"]+)\"").getMatch(0);
                rc.setId(rcID != null ? rcID : "6LeuAc4SAAAAAOSry8eo2xW64K1sjHEKsQ5CaS10");
                rc.load();
                for (int i = 0; i <= 5; i++) {
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode(cf, downloadLink);
                    captchaForm.put("recaptcha_challenge_field", rc.getChallenge());
                    captchaForm.put("recaptcha_response_field", Encoding.urlEncode(c));
                    dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, captchaForm, RESUME, MAXCHUNKS);
                    if (!dl.getConnection().isContentDisposition()) {
                        br.followConnection();
                        rc.reload();
                        continue;
                    }
                    break;
                }
            } else if (captchaForm != null && captchaForm.containsHTML(regexCaptchaSolvemedia)) {
                for (int i = 0; i <= 5; i++) {
                    final PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                    final jd.plugins.decrypter.LnkCrptWs.SolveMedia sm = ((jd.plugins.decrypter.LnkCrptWs) solveplug).getSolveMedia(br);
                    final File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    final String code = getCaptchaCode(cf, downloadLink);
                    final String chid = sm.getChallenge(code);
                    captchaForm.put("adcopy_challenge", chid);
                    captchaForm.put("adcopy_response", "manual_challenge");
                    dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, captchaForm, RESUME, MAXCHUNKS);
                    if (!dl.getConnection().isContentDisposition()) {
                        br.followConnection();
                        continue;
                    }
                    break;
                }
            } else {
                // new captcha type?? or unsupported error.
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (this.captchaUsed && br.containsHTML(regexCaptchaTypes)) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void prepBrowser() {
        if (userAgent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            userAgent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        br.getHeaders().put("User-Agent", userAgent.get());
    }

    private Form getCaptchaForm() {
        for (Form f : br.getForms()) {
            if (f.containsHTML(regexCaptchaTypes)) {
                this.captchaUsed = true;
                return f;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBrowser();
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
                br.postPage("http://www.ugoupload.net/login.html", "submit=Login&submitme=1&loginUsername=" + Encoding.urlEncode(account.getUser()) + "&loginPassword=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML("ugoupload\\.net/upgrade\\.html\">upgrade account</a>") || !br.containsHTML("ugoupload\\.net/upgrade\\.html\">extend account</a>")) {
                    logger.info("Accounttype FREE is not supported!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // Save cookies
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
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://www.ugoupload.net/upgrade.html");
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("Reverts To Free Account:[\t\n\r ]+</td>[\t\n\r ]+<td>[\t\n\r ]+(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd/MM/yyyy hh:mm:ss", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.getURL().contains("e=Error%3A+Could+not+open+file+for+reading")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: Could not open file for reading", 30 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // More possible but it often results in server errors
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