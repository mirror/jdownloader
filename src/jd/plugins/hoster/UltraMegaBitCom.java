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
import java.util.Locale;
import java.util.Map;

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
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ultramegabit.com" }, urls = { "https?://(www\\.)?ultramegabit\\.com/file/details/[A-Za-z0-9\\-_]+" }, flags = { 2 })
public class UltraMegaBitCom extends PluginForHost {

    public UltraMegaBitCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE);
    }

    @Override
    public String getAGBLink() {
        return "http://ultramegabit.com/terms";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("https://", "http://"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("ultramegabit.com/folder/add/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(">File not found<|>File restricted<|>File not available")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Deleted because of inactivity
        if (br.containsHTML(">File has been deleted|>We\\'re sorry\\. This file has been deleted due to inactivity\\.<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>ULTRAMEGABIT\\.COM \\- ([^<>\"]*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h4>(<img[^>]+>)?(.*?) \\(([^\\)]+)\\)</h4>").getMatch(1);
        }
        String filesize = br.getRegex("data-toggle=\"modal\">Download \\(([^<>\"]*?)\\) <span").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("id=\"download_button\" value=\"Free download \\(([^<>\"]*?)\\)\"").getMatch(0);
            if (filesize == null) filesize = br.getRegex("<h4>(<img[^>]+>)?(.*?) \\(([^\\)]+)\\)</h4>").getMatch(2);
        }

        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        if (br.containsHTML(">Premium members only<|The owner of this file has decided to only allow premium members to download it")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        // Only seen in a log
        if (br.containsHTML(">Download slot limit reached<")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        final String rcid = br.getRegex("\\?k=([^<>\"]*?)\"").getMatch(0);
        Form dlform = null;
        for (final Form form : br.getForms())
            if (form.containsHTML("ultramegabit\\.com/file/download")) dlform = form;
        if (rcid == null || dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcid);
        rc.load();
        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        final String c = getCaptchaCode(cf, downloadLink);
        dlform.put("recaptcha_response_field", c);
        dlform.put("recaptcha_challenge_field", rc.getChallenge());
        dlform.remove(null);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlform, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<b>Fatal error</b>:")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Fatal server error");
            if (br.containsHTML("<div id=\"file_delay_carousel\"")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
            if (br.containsHTML("guests are only able to download 1 file every")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
            if (br.containsHTML(">Account limitation notice|files smaller than")) {
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
            }
            if (br.containsHTML("<h3 id=\"download_delay\">Please wait\\.\\.\\.</h3>")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitSum());
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                try {
                    invalidateLastChallengeResponse();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                try {
                    validateLastChallengeResponse();
                } catch (final Throwable e) {
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://ultramegabit.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
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
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("https://ultramegabit.com/login");
                final String token = br.getRegex("name=\"csrf_token\" value=\"([^<>\"]*?)\"").getMatch(0);
                final String lang = System.getProperty("user.language");
                if (token == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.postPage("https://ultramegabit.com/login", "csrf_token=" + token + "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML(">Form validation errors found<|>Invalid username or password<") || br.getURL().contains("ultramegabit.com/login")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
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
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("/user/details");
        String space = br.getRegex("<span class=\"glyphicon glyphicon-hdd\"></span> ([\\d\\.]+ [A-Za-z]+)").getMatch(0);
        if (space == null) space = br.getRegex("<li title=\"Quota\"[^\r\n]+\">([\\d\\.]+ [A-Za-z]+) / [^\r\n]+</li>").getMatch(0);
        if (space != null) ai.setUsedSpace(SizeFormatter.getSize(space));
        String filesNum = br.getRegex("<span class=\"glyphicon glyphicon-file\"></span> ([\\d]+)").getMatch(0);
        if (filesNum != null) ai.setFilesNum(Long.parseLong(filesNum));
        ai.setUnlimitedTraffic();
        br.getPage("/user/billing");

        final boolean ispremium = (br.containsHTML("\"Premium Member\"") || br.containsHTML("premium subscription</h5>"));
        // some premiums have no expiration date, page shows only: Account status: Premium
        String expire = br.getRegex("<h5>Account expires at (\\d+:\\d+(am|pm) \\d+/\\d+/\\d+)</h5>").getMatch(0);
        if (expire == null) expire = br.getRegex("<h5>Next rebill at (\\d+:\\d+(am|pm) \\d+/\\d+/\\d+)</h5>").getMatch(0);
        if (expire == null && !ispremium) {
            // "Member"
            account.setProperty("free", true);
            account.setValid(true);
            ai.setStatus("Registered (free) User");
            return ai;
        } else if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "h:mma dd/MM/yyyy", Locale.ENGLISH));
            if (ai.isExpired()) {
                ai.setValidUntil(-1);
                account.setProperty("free", true);
                account.setValid(true);
                ai.setStatus("Registered (free) User");
                return ai;
            }
        }
        account.setProperty("free", false);
        account.setValid(true);
        ai.setStatus("Premium");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        // Force login or download will fail
        login(account, true);
        if (account.getBooleanProperty("free", false)) {
            br.getPage(link.getDownloadURL());
            doFree(link);
        } else {
            final String token = br.getCookie(MAINPAGE, "csrf_cookie");
            if (token == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.setFollowRedirects(false);
            br.postPage("https://ultramegabit.com/file/download", "csrf_token=" + token + "&encode=" + new Regex(link.getDownloadURL(), "([A-Za-z0-9\\-_]+)$").getMatch(0));
            final String finallink = br.getRedirectLocation();
            if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, finallink, true, -15);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
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

    private long waitSum() {
        // time into the future
        String test = br.getRegex("ts = \\((\\d+)").getMatch(0);
        // current time
        long ct = System.currentTimeMillis();
        // ms wait
        long wait1 = Integer.parseInt(test);
        wait1 += 3600;
        wait1 = wait1 * 1000;
        long result = wait1 - ct;
        return result;
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