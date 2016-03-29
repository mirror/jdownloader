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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "depfile.com" }, urls = { "https?://(www\\.)?depfiledecrypted\\.com/(downloads/i/\\d+/f/.+|[a-zA-Z0-9]+)" }, flags = { 2 })
public class IFilezCom extends PluginForHost {

    private static final String CAPTCHATEXT          = "includes/vvc\\.php\\?vvcid=";
    private static final String MAINPAGE             = "http://depfile.com/";
    private static Object       LOCK                 = new Object();
    private static final String ONLY4PREMIUM         = ">Owner of the file is restricted to download this file only Premium users|>File is available only for Premium users.<";
    private static final String ONLY4PREMIUMUSERTEXT = "Only downloadable for premium users";

    public IFilezCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "premium");
        // Would be needed if multiple free-downloads were possible
        // this.setStartIntervall(11 * 1000l);
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "i-filez.com".equals(host) || "depfile.com".equals(host)) {
            return "depfile.com";
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getAGBLink() {
        return "http://depfile.com/terms";
    }

    public void correctDownloadLink(DownloadLink link) {
        // Links come from a decrypter
        link.setUrlDownload(link.getDownloadURL().replace("depfiledecrypted.com/", "depfile.com/"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Set English language
        br.setCookie(MAINPAGE, "sdlanguageid", "2");
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        handleErrors();
        if (isOffline()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = new Regex(link.getDownloadURL(), "depfile\\.com/downloads/i/\\d+/f/(.+)").getMatch(0);
        if (!link.getDownloadURL().matches("http://(www\\.)?depfiledecrypted\\.com/downloads/i/\\d+/f/.+")) {
            filename = br.getRegex("<th>File name:</th>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        }
        String filesize = br.getRegex("<th>Size:</th>[\r\t\n ]+<td>(.*?)</td>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim().replace(".html", "")));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        String md5 = br.getRegex("<th>MD5:</th>[\r\t\n ]+<td>([a-f0-9]{32})</td>").getMatch(0);
        if (md5 != null) {
            link.setMD5Hash(md5);
        }
        if (br.containsHTML(ONLY4PREMIUM)) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.ifilezcom.only4premium", ONLY4PREMIUMUSERTEXT));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        if (br.containsHTML(ONLY4PREMIUM)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.ifilezcom.only4premium", ONLY4PREMIUMUSERTEXT), PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        String verifycode = br.getRegex("name='vvcid\' value=\'(\\d+)\'").getMatch(0);
        if (verifycode == null) {
            verifycode = br.getRegex("\\?vvcid=(\\d+)").getMatch(0);
        }
        if (!br.containsHTML(CAPTCHATEXT) || verifycode == null) {
            logger.warning("Captchatext not found or verifycode null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String code = getCaptchaCode("http://depfile.com/includes/vvc.php?vvcid=" + verifycode, downloadLink);
        br.postPage(br.getURL(), "vvcid=" + verifycode + "&verifycode=" + code + "&FREE=Download+for+free");
        if (br.getURL().endsWith("/premium")) {
            // no reason why
            // jdlog://2883963166931/ = maybe hit some known session limit?? I could not reproduce this myself -raztoki
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "unknown limit reached", 30 * 60 * 1000l);
        }
        String additionalWaittime = br.getRegex("was recently downloaded from your IP address. No less than (\\d+) min").getMatch(0);
        if (additionalWaittime != null) {
            /* wait 1 minute more to be sure */
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (Integer.parseInt(additionalWaittime) + 1) * 60 * 1001l);
        }
        additionalWaittime = br.getRegex("was recently downloaded from your IP address. No less than (\\d+) sec").getMatch(0);
        if (additionalWaittime != null) {
            /* wait 15 secs more to be sure */
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (Integer.parseInt(additionalWaittime) + 15) * 1001l);
        }
        if (br.containsHTML(">Free users can download up to \\d+G per day.")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily download limit reached", 4 * 60 * 60 * 1000l);
        }
        if (br.containsHTML(CAPTCHATEXT) || br.containsHTML(">The image code you entered is incorrect\\!<")) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        br.setFollowRedirects(false);
        String dllink = br.getRegex("document\\.getElementById\\(\"wait_input\"\\)\\.value= unescape\\('(.*?)'\\);").getMatch(0);
        if (dllink == null) {
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.deepHtmlDecode(dllink).trim();
        int wait = 60;
        String regexedWaittime = br.getRegex("var sec=(\\d+);").getMatch(0);
        if (regexedWaittime != null) {
            wait = Integer.parseInt(regexedWaittime);
        }
        sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                br.setCustomCharset("utf-8");
                // Set English language
                br.setCookie(MAINPAGE, "sdlanguageid", "2");
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
                br.postPage("https://depfile.com/", "login=login&loginemail=" + Encoding.urlEncode(account.getUser()) + "&loginpassword=" + Encoding.urlEncode(account.getPass()) + "&submit=login&rememberme=on");
                // Language not English? Change setting and go on!
                if (!"2".equals(br.getCookie(MAINPAGE, "sdlanguageid"))) {
                    br.setCookie(MAINPAGE, "sdlanguageid", "2");
                    br.getPage("https://depfile.com/");
                }

                if (br.getCookie(MAINPAGE, "sduserid") == null || br.getCookie(MAINPAGE, "sdpassword") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (isAccountPremium() || isAccountAffiliate()) {
                    account.setType(AccountType.PREMIUM);
                } else {
                    account.setType(AccountType.FREE);
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
            } catch (PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    private boolean isAccountAffiliate() {
        return br.containsHTML("/myspace/space/income");
    }

    private boolean isAccountPremium() {
        return br.containsHTML("/myspace/space/premium");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        account.setValid(true);
        ai.setUnlimitedTraffic();
        if (AccountType.FREE.equals(account.getType())) {
            // free accounts can still have captcha.
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Free Account");
        } else {
            String expire = br.getRegex("href='/myspace/space/premium'>(\\d{2}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2})<").getMatch(0);
            // only premium accounts expire, affiliate doesn't.. they are a form of premium according to admin.
            if (expire == null && isAccountPremium()) {
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            } else if (expire != null && isAccountPremium()) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yy hh:mm", null));
                ai.setStatus("Premium Account");
            } else if (expire == null && isAccountAffiliate()) {
                ai.setStatus("Affiliate Account");
            }
            /* Max 20 downloads * each 1 connection in total */
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        if (AccountType.FREE.equals(account.getType())) {
            br.getPage(link.getDownloadURL());
            doFree(link);
        } else {
            br.setFollowRedirects(true);
            br.getPage(link.getDownloadURL());
            if (br.containsHTML("class='notice'>You spent limit on urls/files per \\d+ hours|class='notice'>Sorry, you spent downloads limit on urls/files per \\d+ hours")) {
                logger.info("Daily limit reached, temp disabling premium");
                final AccountInfo ai = account.getAccountInfo();
                ai.setTrafficLeft(0);
                account.setAccountInfo(ai);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            String dllink = br.getRegex("<th>A link for 24 hours:</th>[\t\n\r ]+<td><input type=\"text\" readonly=\"readonly\" class=\"text_field width100\" onclick=\"this\\.select\\(\\);\" value=\"(http://.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(\"|')(http://[a-z0-9]+\\.depfile\\.com/premdw/\\d+/[a-z0-9]+/.*?)\\1").getMatch(1);
            }
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many connections - wait before starting new downloads", 3 * 60 * 1000l);
            }
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private boolean isOffline() {
        if (this.br.getURL().contains("depfile.com/premium")) {
            return true;
        }
        return false;
    }

    public void handleErrors() throws PluginException {
        if (br.containsHTML("(>File was not found in the depFile database\\.|It is possible that you provided wrong link.<|>Файл не найден в базе depfile\\.com\\. Возможно Вы неправильно указали ссылку\\.<|The file was blocked by the copyright holder|>Page Not Found)")) {
            logger.warning("File not found OR file removed from provider");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Invalid links
        if (br.containsHTML(">403 Forbidden</")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        //
        if (br.containsHTML(">The file was deleted by the owner\\.</")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (AccountType.FREE.equals(acc.getType())) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}