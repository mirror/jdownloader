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
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
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
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freakshare.net", "freakshare.com" }, urls = { "REGEXNOTUSED_BLAHASDAHAHDAHAHSDHAHDASDHAHD1223", "http://(www\\.)?freakshare\\.(net|com)/files?/[\\w]+/.+(\\.html)?" }, flags = { 0, 2 })
public class Freaksharenet extends PluginForHost {

    private static final String  WAIT1              = "WAIT1";
    private static AtomicInteger MAXPREMDLS         = new AtomicInteger(-1);
    private static final String  MAXDLSLIMITMESSAGE = "Sorry, you cant download more then";
    private static final String  LIMITREACHED       = "Your Traffic is used up for today|Der Traffic für heute ist verbraucht";
    private static Object        LOCK               = new Object();

    public Freaksharenet(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(1000l);
        this.enablePremium("http://freakshare.com/shop.html");
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("freakshare.net", "freakshare.com"));
    }

    @Override
    public Boolean rewriteHost(Account acc) {
        if ("freakshare.net".equals(getHost())) {
            if (acc != null && "freakshare.net".equals(acc.getHoster())) {
                acc.setHoster("freakshare.com");
                return true;
            }
            return false;
        }
        return null;
    }

    @Override
    public boolean isPremiumEnabled() {
        if ("freakshare.com".equals(getHost())) return true;
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(false);
        // br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:28.0) Gecko/20100101 Firefox/28.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        /*
         * set english language in phpsession
         */
        br.getPage("http://freakshare.com/index.php?language=EN");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("We are back soon")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        if (br.containsHTML("(Sorry but this File is not avaible|Sorry, this Download doesnt exist anymore)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No Downloadserver\\. Please try again")) return AvailableStatus.UNCHECKABLE;
        if (br.containsHTML(LIMITREACHED)) {
            String nameFromLink = new Regex(downloadLink.getDownloadURL(), "freakshare\\.com/files/[a-z0-9]+/(.*?)\\.html").getMatch(0);
            if (nameFromLink != null) downloadLink.setName(Encoding.htmlDecode(nameFromLink));
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.freaksharecom.limitreached", "Limit reached: No full availablecheck possible at the moment!"));
            return AvailableStatus.TRUE;
        }

        final String filename = br.getRegex("\"box_heading\" style=\"text\\-align:center;\">(.*?)\\- .*?</h1>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        final String filesize = br.getRegex("\"box_heading\" style=\"text\\-align:center;\">.*?\\- (.*?)</h1>").getMatch(0);
        if (filesize != null) {
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        final boolean resume = false;
        final int maxchunks = 1;
        final boolean waitReconnecttime = getPluginConfig().getBooleanProperty(WAIT1, false);
        handleFreeErrors();
        br.setFollowRedirects(false);
        Form form = br.getForm(1);
        final String ajax = br.getRegex("\\$\\.get\\(\"\\.\\./\\.\\.(.*?)\",").getMatch(0);
        if (form == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        if (form.getAction().contains("shop")) {
            logger.info("Wrong form = shop form!");
            form = null;
            Form[] forms = br.getForms();
            if (forms == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            for (Form a : forms) {
                if (a.getAction() != null) {
                    if (a.getAction().contains("shop")) continue;
                    if (a.getAction().contains("payment.html")) continue;
                }
                form = a;
                break;
            }
        }
        if (form == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        form.remove(null);

        String ttt = null;
        if (ajax != null) {
            final Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br2.getPage("http://freakshare.com" + ajax);
            ttt = br2.getRegex("SUCCESS:(\\d+)").getMatch(0);
        } else {
            ttt = br.getRegex("var time = (\\d+)\\.0").getMatch(0);
        }
        int tt = 0;
        if (ttt != null) {
            tt = Integer.parseInt(ttt);
        }
        if ((tt > 600 && !waitReconnecttime) || tt > 1800) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        sleep((tt + new Random().nextInt(10)) * 1001l, downloadLink);
        br.submitForm(form);

        boolean captchaFailed = false;
        for (int i = 1; i <= 5; i++) {
            handleFreeErrors();
            Form[] forms = br.getForms();
            form = null;
            if (forms == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            for (Form a : forms) {
                if (a.getAction() != null) {
                    if (a.getAction().contains("shop")) continue;
                    if (a.getAction().contains("payment.html")) continue;
                }
                form = a;
                break;
            }
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                captchaFailed = true;
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.parse();
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, downloadLink);
                form.put("recaptcha_challenge_field", rc.getChallenge());
                form.put("recaptcha_response_field", c);
                form.remove("submit");
                form.remove(null);
                br.submitForm(form);
                if (br.containsHTML(MAXDLSLIMITMESSAGE)) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Maximum concurrent download sessions reached.", 30 * 60 * 1000l); }
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/|>Wrong Captcha)")) {
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
                final String finallink = br.getRedirectLocation();
                if (finallink == null) continue;
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, resume, maxchunks);
                captchaFailed = false;
                break;
            } else {
                captchaFailed = false;
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, resume, maxchunks);
            }
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection();
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/|>Wrong Captcha)")) continue;
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            captchaFailed = false;
            break;
        }
        if (captchaFailed) {
            handleServerErrors(downloadLink);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        setBrowserExclusive();
        MAXPREMDLS.set(1);
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        final String usedSpace = br.getRegex(">Used Space:</td>[\n\r\t ]+<td>(.*?) of ").getMatch(0);
        if (usedSpace != null) {
            ai.setUsedSpace(usedSpace.toLowerCase().replace("yte", ""));
        }
        final String points = br.getRegex(">Points:</td>[\n\r\t ]+<td>(\\d+)</td>").getMatch(0);
        if (points != null) {
            ai.setPremiumPoints(points);
        }
        final String hostedFiles = br.getRegex(">Hosted Files:</td>[\n\r\t ]+<td>(\\d+)</td>").getMatch(0);
        if (hostedFiles != null) {
            ai.setFilesNum(Integer.parseInt(hostedFiles));
        }
        if (account.getBooleanProperty("freeacc", false)) {
            try {
                MAXPREMDLS.set(1);
                account.setMaxSimultanDownloads(1);
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
            }
            ai.setStatus("Registered (free) User");
        } else {
            final String left = br.getRegex(">Traffic left:</td>.*?<td>(.*?)</td>").getMatch(0);
            if (left != null) {
                ai.setTrafficLeft(left);
            }
            final String validUntil = br.getRegex(">valid until:</td>.*?<td><b>(.*?)</b></td>").getMatch(0);
            if (validUntil == null) {
                account.setValid(false);
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "dd.MM.yyyy - HH:mm", null));
                account.setValid(true);
            }
            try {
                MAXPREMDLS.set(20);
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
            ai.setStatus("Premium User");
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://freakshare.com/terms-of-service.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return MAXPREMDLS.get();
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void handleFreeErrors() throws PluginException {
        if (br.containsHTML("Sorry, you cant download more then 50 files at time")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        if (br.containsHTML("You can Download only 1 File in")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        if (br.containsHTML("No Downloadserver\\. Please try again") || br.containsHTML("Downloadserver im Moment nicht erreichbar.")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No Downloadserver. Please try again later", 15 * 60 * 1000l);
        if (br.containsHTML(LIMITREACHED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
    }

    private void handleServerErrors(DownloadLink downloadLink) throws PluginException {
        if (br.containsHTML(MAXDLSLIMITMESSAGE)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Maximum concurrent download sessions reached.", 30 * 60 * 1000l); }
        if (br.containsHTML("File can not be found")) {
            logger.info("File for the following is offline (server error): " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(">404 Not Found<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
        if (br.containsHTML("bad try")) {
            logger.warning("Hoster said \"bad try\" which means that jd didn't wait enough time before trying to start the download!");
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: Server sent empty site", 5 * 60 * 1000l);
        if (br.containsHTML("your Traffic is used up for today")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001); }
        if (br.containsHTML("No Downloadserver\\. Please try again") || br.containsHTML("Downloadserver im Moment nicht erreichbar.")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No Downloadserver. Please try again later", 15 * 60 * 1000l); }
        if (br.containsHTML("you cant  download more then 1 at time")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1001); }
        if (br.getURL().contains("section=filenotfound")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account, false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Sorry, you cant download more then 50 files at time\\.")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1001); }
        if (account.getBooleanProperty("freeacc", false)) {
            doFree(downloadLink);
        } else {
            String url = null;
            if (br.getRedirectLocation() == null) {
                if (br.containsHTML("No Downloadserver. Please try again")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No Downloadserver. Please try again later", 15 * 60 * 1000l); }
                if (br.containsHTML("Traffic is used up for today")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE); }
                final Form form = br.getForm(0);
                if (form == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                br.submitForm(form);
                url = br.getRedirectLocation();
            } else {
                url = br.getRedirectLocation();
            }
            if (url == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, -5);
            if (!dl.getConnection().isContentDisposition()) {
                logger.info("The finallink is no file, trying to handle errors...");
                br.followConnection();
                handleServerErrors(downloadLink);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private static final String COOKIE_HOST = "http://freakshare.com";

    public void login(final Account account, final boolean force) throws IOException, PluginException {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                setBrowserExclusive();
                br.setCustomCharset("UTF-8");/* workaround for buggy server */
                br.setFollowRedirects(false);
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
                /*
                 * set english language in phpsession because we have no cookie for that
                 */
                br.getPage("http://freakshare.com/?language=US");
                br.postPage("http://freakshare.com/login.html", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&submit=Login");
                if (br.getCookie("http://freakshare.com", "login") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                br.getPage("http://freakshare.com/");
                if (!br.containsHTML("<td><b>Member \\(free\\)</b></td>") && !br.containsHTML("<td><b>Member \\(premium\\)</b></td>")) {
                    logger.info("JD couldn't find out the membership of this account!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (!br.containsHTML("<td><b>Member \\(premium\\)</b></td>")) {
                    account.setProperty("freeacc", true);
                } else {
                    account.setProperty("freeacc", false);
                }
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {

    }

    @Override
    public void resetPluginGlobals() {
    }

    private void setConfigElements() {
        final ConfigEntry cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), WAIT1, JDL.L("plugins.hoster.Freaksharenet.waitInsteadOfReconnect", "Wait and download instead of reconnecting if wait time is under 30 minutes")).setDefaultValue(true);
        getConfig().addEntry(cond);
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