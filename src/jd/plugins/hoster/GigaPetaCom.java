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

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gigapeta.com" }, urls = { "http://[\\w\\.]*?gigapeta\\.com/dl/\\w+" })
public class GigaPetaCom extends PluginForHost {
    // Gehört zu tenfiles.com/tenfiles.info
    public GigaPetaCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://gigapeta.com/premium/");
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://gigapeta.com", "lang", "us");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("All threads for IP")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.gigapeta.unavailable", "Your IP is already downloading a file"));
        }
        if (br.containsHTML("<div id=\"page_error\">") && !br.containsHTML("To download this file please <a")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Regex infos = br.getRegex(Pattern.compile("<img src=\".*\" alt=\"file\" />\\-\\->(.*?)</td>.*?</tr>.*?<tr>.*?<th>.*?</th>.*?<td>(.*?)</td>", Pattern.DOTALL));
        String fileName = infos.getMatch(0);
        String fileSize = infos.getMatch(1);
        if (fileName == null || fileSize == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        downloadLink.setName(fileName.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(fileSize.trim()));
        return AvailableStatus.TRUE;
    }

    public void doFree(DownloadLink downloadLink) throws Exception {
        if (this.br.containsHTML("To download this file please")) {
            /*
             * E.g. html:
             * "To download this file please <a href=/reg>register</a>. It is fast, free, and assumes no obligations.        </p>"
             */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        String captchaKey = (int) (Math.random() * 100000000) + "";
        String captchaUrl = "/img/captcha.gif?x=" + captchaKey;
        for (int i = 1; i <= 3; i++) {
            String captchaCode = getCaptchaCode(captchaUrl, downloadLink);
            br.postPage(br.getURL(), "download=&captcha_key=" + captchaKey + "&captcha=" + captchaCode);
            if (br.containsHTML("class=\"big_error\">404</h1>")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error (404)", 60 * 60 * 1000l);
            }
            if (br.containsHTML("You will get ability to download next file after")) {
                final Regex wttime = br.getRegex("<b>(\\d+) min\\. (\\d+) sec\\.</b>");
                final String minutes = wttime.getMatch(0);
                final String seconds = wttime.getMatch(1);
                if (minutes != null && seconds != null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (Integer.parseInt(minutes) * 60 * 1001l) + (Integer.parseInt(seconds) * 1001l));
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
                }
            }
            if (br.getRedirectLocation() != null) {
                break;
            }
        }
        if (br.getRedirectLocation() == null) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("All threads for IP")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.gigapeta.unavailable", "Your IP is already downloading a file"));
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        try {
            return login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
    }

    public String getAGBLink() {
        return "http://gigapeta.com/rules/";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (AccountType.FREE.equals(account.getType())) {
            doFree(link);
        } else {
            String dllink = br.getRedirectLocation();
            if (dllink == null) {
                Form DLForm = br.getFormBySubmitvalue("Download");
                if (DLForm == null) {
                    DLForm = br.getForm(0);
                }
                if (DLForm == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.submitForm(DLForm);
                dllink = br.getRedirectLocation();
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (!(dl.getConnection().isContentDisposition())) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    // do not add @Override here to keep 0.* compatibility
    private AccountInfo login(Account account, boolean fromFetchAccount) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://gigapeta.com", "lang", "us");
        br.setDebug(true);
        /*
         * Workaround for a serverside 502 error (date: 04.03.15). Accessing the wrong ('/dl/') link next line in the code will return a 404
         * error but we can login and download fine then.
         */
        br.getPage("http://gigapeta.com/dl/");
        final String auth_token = br.getRegex("name=\"auth_token\" value=\"([a-z0-9]+)\"").getMatch(0);
        final String lang = System.getProperty("user.language");
        if (auth_token == null) {
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        br.postPage(br.getURL(), "auth_login=" + Encoding.urlEncode(account.getUser()) + "&auth_passwd=" + Encoding.urlEncode(account.getPass()) + "&auth_token=" + auth_token);
        if (br.getCookie("http://gigapeta.com/", "adv_sess") == null) {
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final AccountInfo ai = new AccountInfo();
        final String expire = br.getRegex("You have <b>premium</b> account till(.*?)</p>").getMatch(0);
        if (expire != null) {
            account.setType(AccountType.PREMIUM);
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.trim(), "dd.MM.yyyy HH:mm", null));
            if (!ai.isExpired()) {
                account.setMaxSimultanDownloads(-1);
                ai.setStatus("Premium Account");
            }
        }
        if (br.containsHTML("You have <b>basic</b> account") || ai.isExpired()) {
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
            account.setType(AccountType.FREE);
            ai.setStatus("Free Account");
        }
        account.setValid(true);
        ai.setUnlimitedTraffic();
        if (!fromFetchAccount) {
            account.setAccountInfo(ai);
        }
        return ai;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
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