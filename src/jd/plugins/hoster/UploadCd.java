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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "upload.cd" }, urls = { "http://(www\\.)?upload\\.cd/(files/)?[a-z0-9]+" })
public class UploadCd extends PluginForHost {

    public UploadCd(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://upload.cd/premium/");
    }

    @Override
    public String getAGBLink() {
        return "http://upload.cd/tos";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 0;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    private static final String  INVALIDLINKS                 = "http://(www\\.)?upload\\.cd/premium";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getDownloadURL().matches(INVALIDLINKS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        /* 2017-03-08: Old UA got blocked - try this for now, blocking an up-to-date-User-Agent is not a good idea. */
        this.br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:51.0) Gecko/20100101 Firefox/51.0");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">The requested file does not exist|The requested page does not exist\\.") || br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML("class=\"container download\\-page\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex finfo = br.getRegex("class=\"download\\-file pull\\-left\">[\t\n\r ]+<h3>([^<>\"]*?)</h3><p>([^<>\"]*?)</p>");
        final String filename = finfo.getMatch(0);
        final String filesize = finfo.getMatch(1);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = br.getRegex("\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        /*
         * 2017-03-08: Usually we'll get 403 on free resume attempt --> Does not matter, we can try it anyways and continue below on failure
         * :)
         */
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            /* Not needed, can still be skipped */
            // if (br.containsHTML(">This file can be downloaded only by")) {
            // try {
            // throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            // } catch (final Throwable e) {
            // if (e instanceof PluginException) {
            // throw (PluginException) e;
            // }
            // }
            // throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
            // }
            if (br.containsHTML("<b>Error\\!</b>")) {
                /** Wait time reconnect handling */
                /* adjust this regex to catch the wait time string for COOKIE_HOST */
                String WAIT = br.getRegex("> You have to wait ([^<>\"]*?) until the next download becomes available").getMatch(0);
                String tmphrs = new Regex(WAIT, "(\\d+)\\s+hours?").getMatch(0);
                String tmpmin = new Regex(WAIT, "\\s+(\\d+)\\s+minutes?").getMatch(0);
                String tmpsec = new Regex(WAIT, "\\s+(\\d+)\\s+seconds?").getMatch(0);
                if (tmphrs == null && tmpmin == null && tmpsec == null) {
                    logger.info("Waittime regexes seem to be broken");
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
                } else {
                    int minutes = 0, seconds = 0, hours = 0;
                    if (tmphrs != null) {
                        hours = Integer.parseInt(tmphrs);
                    }
                    if (tmpmin != null) {
                        minutes = Integer.parseInt(tmpmin);
                    }
                    if (tmpsec != null) {
                        seconds = Integer.parseInt(tmpsec);
                    }
                    int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                    logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                    /* Not enough wait time to reconnect -> Wait short and retry */
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
                }
            }
            final String fid = new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            br.postPage("http://" + this.getHost() + "/download/startTimer", "fid=" + fid);

            final String seconds = getJson("seconds");
            final String sid = getJson("sid");
            if (seconds == null || sid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            this.sleep((Integer.parseInt(seconds) + 5) * 1001l, downloadLink);

            /* 2017-03-08: Added 5 extra seconds to avoid failures! */
            br.postPage("/download/checkTimer", "sid=" + sid);
            /* E.g. positive response: {"status":"success","state":"ended"} */
            if (!PluginJSonUtils.getJsonValue(this.br, "status").equalsIgnoreCase("success")) {
                /* E.g. bad response: {"status":"error","msg":"There are some errors. Please try again later"} */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            /* 2017-03-08: On failure/when we get blocked: "Hello downloaders! ;)" */
            /* 2017-03-08: 'rand' parameter has been addd and is required! */
            final String uuid = UUID.randomUUID().toString().split("-")[0];
            br.postPage("/download/url", "fileid=" + fid + "&usid=" + sid + "&rand=" + uuid + "&referer=&premium_dl=0");
            final Recaptcha rc = new Recaptcha(br, this);
            rc.findID();
            rc.load();
            for (int i = 1; i <= 5; i++) {
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                br.postPage("http://" + this.getHost() + "/download/url", "fileid=" + fid + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&CaptchaForm%5Bvalidacion%5D=");
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    rc.reload();
                    continue;
                }
                break;
            }
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            dllink = br.getRegex("class=\"btn download\\-btn btn\\-success\" href=\"(http[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://upload.cd";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
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
                br.setFollowRedirects(true);
                br.postPage("http://upload.cd/user/auth/", "User%5Buemail%5D=" + Encoding.urlEncode(account.getUser()) + "&User%5Bupass%5D=" + Encoding.urlEncode(account.getPass()) + "&User%5BrememberMe%5D=0&User%5BrememberMe%5D=1&logink=111&backurl=http%3A%2F%2Fupload.cd%2F");
                if (!br.containsHTML("\"status\":\"success\"") && !br.containsHTML("href=\"/site/logout/\">Logout \\(")) {
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

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        br.getPage("http://upload.cd/user/profile/");
        final String space = br.getRegex("class=\"pull\\-right\">([^<>\"]*?)<span>\\(of maximum \\d+ GB\\)</span>").getMatch(0);
        if (space != null) {
            ai.setUsedSpace(space.trim());
        }
        if (!br.containsHTML("class=\"pull\\-right\">Premium")) {
            account.setProperty("free", true);
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            try {
                account.setType(AccountType.FREE);
                /* free accounts can still have captcha */
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            ai.setStatus("Registered (free) user");
        } else {
            account.setProperty("free", false);
            final String expire = br.getRegex("valid until: ([^<>\"]*?)\\)").getMatch(0);
            if (expire == null) {
                final String lang = System.getProperty("user.language");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account Typ!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or unsupported account type!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy HH:mm", Locale.GERMAN));
            }
            maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            try {
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            ai.setStatus("Premium Account");
        }
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (account.getBooleanProperty("free", false)) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                br.postPage("http://upload.cd/download/url", "fileid=" + getFID(link) + "&usid=&referer=&premium_dl=1");
                dllink = br.getRegex("class=\"btn download-btn btn-success\" href=\"(http://[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}