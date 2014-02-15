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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "odimusic.net", "megabox.ro" }, urls = { "fbvihj549uzj0sklncnkovdesnirohionrsoDELETE_MEfgztjm", "http://(www\\.)?(odimusic\\.net|megabox\\.ro)/load/\\?d=[A-Z0-9]+" }, flags = { 0, 2 })
public class OdiMusicNet extends PluginForHost {

    public OdiMusicNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST + "/register.php?g=3");
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/rules.php";
    }

    private static final String COOKIE_HOST     = "http://megabox.ro";
    private final int           DEFAULTWAITTIME = 30;

    // MhfScriptBasic 1.7
    // FREE limits: 20 * 20
    // PREMIUM limits: 20 * 20
    // Captchatype: none
    // Other notes:

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        br.setCookie(COOKIE_HOST, "yab_mylang", "en");
        br.getPage(parameter.getDownloadURL());
        if (br.getURL().contains("&code=DL_FileNotFound") || br.containsHTML("(Your requested file is not found|No file found)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h2 class=\"float\\-left\">([^<>\"]*?)</h2>").getMatch(0);
        String filesize = getData("File size");
        if (filename == null || filename.matches("")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setFinalFileName(filename.trim());
        if (filesize != null) parameter.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        if (br.containsHTML("value=\"Free Users\""))
            br.postPage(downloadLink.getDownloadURL(), "Free=Free+Users");
        else if (br.getFormbyProperty("name", "entryform1") != null) br.submitForm(br.getFormbyProperty("name", "entryform1"));
        final Browser ajaxBR = br.cloneBrowser();
        ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");

        final String rcID = br.getRegex("challenge\\?k=([^<>\"]*?)\"").getMatch(0);
        if (rcID != null) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            ajaxBR.postPage(downloadLink.getDownloadURL(), "downloadverify=1&d=1&recaptcha_response_field=" + c + "&recaptcha_challenge_field=" + rc.getChallenge());
            if (ajaxBR.containsHTML("incorrect\\-captcha\\-sol")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else {
            ajaxBR.postPage(downloadLink.getDownloadURL(), "downloadverify=1&d=1");
        }
        if (ajaxBR.containsHTML("This file is available Only with a premium account")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only available to Premium Members");
        }
        final String reconnectWaittime = ajaxBR.getRegex("You must wait (\\d+) mins\\. for next download.").getMatch(0);
        if (reconnectWaittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWaittime) * 60 * 1001l);
        final String finalLink = findLink(ajaxBR);
        if (finalLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int wait = DEFAULTWAITTIME;
        final String waittime = ajaxBR.getRegex("countdown\\((\\d+)\\);").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalLink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();

            if (br.containsHTML(">AccessKey is expired, please request")) throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL server error, waittime skipped?");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String findLink(final Browser br) throws Exception {
        return br.getRegex("(http://[a-z0-9\\-\\.]{5,30}/.*?getfile\\.php\\?id=\\d+[^<>\"\\']*?)(\"|\\')").getMatch(0);
    }

    private String getData(final String data) {
        return br.getRegex(">" + data + "</strong></li>[\t\n\r ]+<li class=\"col\\-w50\">([^<>\"]*?)</li>").getMatch(0);
    }

    private static Object LOCK = new Object();

    @SuppressWarnings("unchecked")
    public void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
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
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
                br.setCookie(COOKIE_HOST, "yab_mylang", "en");
                br.getPage(COOKIE_HOST + "/load/en/login.php");
                final String lang = System.getProperty("user.language");
                Form form = br.getFormbyProperty("name", "loginfrm");
                if (form == null) form = br.getForm(0);
                if (form == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                form.put("user", Encoding.urlEncode(account.getUser()));
                form.put("pass", Encoding.urlEncode(account.getPass()));
                // If the referer is still in the form (and if it is a valid
                // downloadlink) the download starts directly after logging in
                // so we
                // MUST remove it!
                form.remove("refer_url");
                form.put("refer_url", "");
                br.submitForm(form);
                if (br.getCookie(COOKIE_HOST, "mfh_passhash") == null || "0".equals(br.getCookie(COOKIE_HOST, "mfh_uid"))) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.getPage(COOKIE_HOST + "/load/en/members.php");
                final String premium = br.getRegex("return overlay\\(this, \\'package_details\\',\\'width=\\d+px,height=\\d+px,center=1,resize=1,scrolling=1\\'\\)\">(Premium)</a>").getMatch(0);
                if (premium == null) {
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
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage(COOKIE_HOST + "/load/en/members.php");
        String expired = getData("Expired\\?");
        if (expired != null) {
            expired = expired.trim();
            if (expired.equalsIgnoreCase("No"))
                ai.setExpired(false);
            else if (expired.equalsIgnoreCase("Yes")) ai.setExpired(true);
        }
        String expires = getData("Package Expire Date");
        if (expires != null) {
            expires = expires.trim();
            if (!expires.equals("Never")) {
                String[] e = expires.split("/");
                Calendar cal = new GregorianCalendar(Integer.parseInt("20" + e[2]), Integer.parseInt(e[0]) - 1, Integer.parseInt(e[1]));
                ai.setValidUntil(cal.getTimeInMillis());
            }
        }
        String create = getData("Register Date");
        if (create != null) {
            String[] c = create.split("/");
            Calendar cal = new GregorianCalendar(Integer.parseInt("20" + c[2]), Integer.parseInt(c[0]) - 1, Integer.parseInt(c[1]));
            ai.setCreateTime(cal.getTimeInMillis());
        }
        String files = getData("Hosted Files");
        if (files != null) {
            ai.setFilesNum(Integer.parseInt(files.trim()));
        }
        if (account.getBooleanProperty("freeacc")) {
            ai.setStatus("Registered (free) User");
        } else {
            ai.setStatus("Premium User");
        }
        account.setValid(true);

        return ai;
    }

    public void handlePremium(final DownloadLink parameter, final Account account) throws Exception {
        requestFileInformation(parameter);
        login(account, false);
        br.setFollowRedirects(false);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        br.getPage(parameter.getDownloadURL());
        if (account.getBooleanProperty("freeacc")) {
            doFree(parameter);
        } else {
            String finalLink = null;
            if (br.getRedirectLocation() != null && (br.getRedirectLocation().contains("access_key=") || br.getRedirectLocation().contains("getfile.php"))) {
                finalLink = br.getRedirectLocation();
            } else {
                if (br.containsHTML("You have got max allowed download sessions from the same IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
                String passCode = null;
                if (br.containsHTML("downloadpw")) {
                    logger.info("The file you're trying to download seems to be password protected...");
                    Form pwform = br.getFormbyProperty("name", "myform");
                    if (pwform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    if (parameter.getStringProperty("pass", null) == null) {
                        passCode = Plugin.getUserInput("Password?", parameter);
                    } else {
                        /* gespeicherten PassCode holen */
                        passCode = parameter.getStringProperty("pass", null);
                    }
                    pwform.put("downloadpw", passCode);
                    br.submitForm(pwform);
                }
                if (br.containsHTML("You have got max allowed download sessions from the same IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
                if (br.containsHTML("Password Error")) {
                    logger.warning("Wrong password!");
                    parameter.setProperty("pass", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }

                if (passCode != null) {
                    parameter.setProperty("pass", passCode);
                }
                finalLink = findLink(br);
            }
            if (finalLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, finalLink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
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