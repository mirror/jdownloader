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

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filecloud.io", "ifile.it" }, urls = { "http://(www\\.)?(ifile\\.it|filecloud\\.io)/[a-z0-9]+", "fhrfzjnerhfDELETEMEdhzrnfdgvfcas4378zhb" }, flags = { 2, 0 })
public class IFileIt extends PluginForHost {

    private final String        useragent               = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:12.0) Gecko/20100101 Firefox/12.0";
    /* must be static so all plugins share same lock */
    private static final Object LOCK                    = new Object();
    private boolean             showDialog              = false;
    private int                 MAXFREECHUNKS           = 1;
    private static final String ONLY4REGISTERED         = "\"message\":\"signup\"";
    private static final String ONLY4REGISTEREDUSERTEXT = JDL.LF("plugins.hoster.ifileit.only4registered", "Only downloadable for registered users");
    private static final String NOCHUNKS                = "NOCHUNKS";
    private static final String NORESUME                = "NORESUME";
    private static final String MAINPAGE                = "http://filecloud.io/";

    public IFileIt(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filecloud.io/user-register.html");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("ifile.it/", "filecloud.io/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        updateBrowser(br);
        br.setRequestIntervalLimit(getHost(), 250);
        simulateBrowser();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("var __ab1 = 0")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return AvailableStatus.TRUE;
    }

    public void doFree(final DownloadLink downloadLink, boolean resume, boolean viaAccount) throws Exception, PluginException {
        String ab1 = br.getRegex("var __ab1 = (\\d+);").getMatch(0);
        if (ab1 == null) ab1 = br.getRegex("\\$\\(\\'#fsize\\'\\)\\.empty\\(\\)\\.append\\( toMB\\( (\\d+) \\) \\);").getMatch(0);
        if (ab1 == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.setFollowRedirects(true);
        // Br2 is our xml browser now!
        final Browser br2 = br.cloneBrowser();
        br2.setReadTimeout(40 * 1000);
        final String ukey = new Regex(downloadLink.getDownloadURL(), "filecloud\\.io/(.+)").getMatch(0);
        xmlrequest(br2, "http://filecloud.io/download-request.json", "ukey=" + ukey + "&__ab1=" + ab1);
        if (!viaAccount && br2.containsHTML(ONLY4REGISTERED)) { throw new PluginException(LinkStatus.ERROR_FATAL, ONLY4REGISTEREDUSERTEXT); }
        if (br2.containsHTML("\"captcha\":1")) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br2);
            // Semi-automatic reCaptcha handling
            final String k = br.getRegex("recaptcha_public.*?=.*?\\'([^<>\"]*?)\\';").getMatch(0);
            if (k == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            rc.setId(k);
            rc.load();
            for (int i = 0; i <= 5; i++) {
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                xmlrequest(br2, "http://filecloud.io/download-request.json", "ukey=" + ukey + "&__ab1=" + ab1 + "&ctype=recaptcha&recaptcha_response=" + Encoding.urlEncode_light(c) + "&recaptcha_challenge=" + rc.getChallenge());
                if (br2.containsHTML("(\"retry\":1|\"captcha\":1)")) {
                    rc.reload();
                    continue;
                }
                break;
            }
        }
        if (br2.containsHTML("(\"retry\":1|\"captcha\":1)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.getPage("http://filecloud.io/download.html");
        String dllink = br.getRegex("id=\"requestBtnHolder\">[\t\n\r ]+<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br2.getRegex("\"(http://s\\d+\\.filecloud\\.io/[a-z0-9]+/\\d+/[^<>\"/]*?)\"").getMatch(0);
        if (dllink == null) {
            logger.info("last try getting dllink failed, plugin must be defect!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(false);
        int chunks = MAXFREECHUNKS;
        if (downloadLink.getBooleanProperty(IFileIt.NOCHUNKS, false) || resume == false) {
            chunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 503) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l); }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty(IFileIt.NORESUME, false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty(IFileIt.NORESUME, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(IFileIt.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(IFileIt.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    @Override
    public String getAGBLink() {
        return "http://filecloud.io/misc-tos.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        doFree(downloadLink, true, false);
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
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
                updateBrowser(br);
                br.getPage("http://filecloud.io/user-login.html");
                // We don't know if a captcha is needed so first we try without,
                // if we get an errormessage we know a captcha is needed
                boolean accountValid = false;
                boolean captchaNeeded = false;
                for (int i = 0; i <= 2; i++) {
                    final String rcID = br.getRegex("var.*?recaptcha_public.*?=.*?\\'([^<>\"]*?)\\';").getMatch(0);
                    if (captchaNeeded) {
                        if (rcID == null) {
                            logger.warning("recaptcha ID not found, stoping!");
                            break;
                        }
                        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        rc.setId(rcID);
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        DownloadLink dummyLink = new DownloadLink(null, "Account", "filecloud.io", "http://filecloud.io", true);
                        String c = getCaptchaCode(cf, dummyLink);
                        br.postPage("https://secure.filecloud.io/user-login_p.html", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
                    } else {
                        br.postPage("https://secure.filecloud.io/user-login_p.html", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    }
                    if (br.getURL().contains("RECAPTCHA__INCORRECT")) {
                        captchaNeeded = true;
                        logger.info("Wrong captcha and/or username/password entered!");
                        continue;
                    }
                    if (!br.containsHTML("class=\"icon\\-info\\-sign\"></i> you have successfully logged in")) {
                        continue;
                    }
                    accountValid = true;
                    break;
                }
                if (!accountValid) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        final AccountInfo ai = new AccountInfo();
        showDialog = true;
        try {
            // Use cookies, check and refresh if needed
            login(account, false);
            br.getPage("https://secure.filecloud.io/user-account.html");
            if (br.containsHTML("No htmlCode read")) {
                logger.info("Account invalid or old cookies, refreshing...");
                login(account, true);
                br.getPage("https://secure.filecloud.io/user-account.html");
            }

        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("https://secure.filecloud.io/user-account.html");
        // Only free acc support till now
        if (br.containsHTML("<span class=\"label label\\-important\">no</span>")) {
            ai.setStatus("Normal User");
            account.setProperty("typ", "free");
        } else if (br.containsHTML("<span class=\"label label\\-success\">yes</span>")) {
            ai.setStatus("Premium User");
            account.setProperty("typ", "premium");
            final String expires = br.getRegex("\\$\\(\\'#expires\\'\\)\\.empty\\(\\)\\.append\\(  show_local_dt\\( (\\d+) \\) \\);").getMatch(0);
            if (expires != null) {
                ai.setValidUntil(Long.parseLong(expires) * 1000);
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        if ("premium".equals(account.getStringProperty("typ", null))) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 503) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l); }
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            br.setFollowRedirects(true);
            br.getPage(link.getDownloadURL());
            doFree(link, true, true);
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    private void updateBrowser(Browser br) {
        if (br == null) return;
        br.getHeaders().put("User-Agent", useragent);
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        br.setCookie("http://filecloud.io/", "lang", "en");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    private void simulateBrowser() throws IOException {
        br.cloneBrowser().getPage("http://ifile.it/ads/adframe.js");
    }

    private void xmlrequest(final Browser br, final String url, final String postData) throws IOException {
        br.getHeaders().put("User-Agent", useragent);
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.postPage(url, postData);
        br.getHeaders().remove("X-Requested-With");
    }

}