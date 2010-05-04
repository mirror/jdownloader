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

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4freeload.net" }, urls = { "http://[\\w\\.]*?4freeload\\.net/download\\.php\\?id=[A-Fa-f0-9]+" }, flags = { 2 })
public class FourFreeLoadDotNet extends PluginForHost {

    public FourFreeLoadDotNet(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://4freeload.net/register.php");
    }

    @Override
    public String getAGBLink() {
        return "http://4freeload.net/rules.php";
    }

    public boolean nopremium = false;

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://4freeload.net", "yab_mylang", "de");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("konnte leider nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(Pattern.compile("Dateiname:</b></td>\\s+<td[^>]*>(.*?)</td>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("Dateigr[^<]*</b></td>\\s+<td[^>]*>(.*?)</td>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(DownloadLink downloadLink) throws Exception {

        /* CaptchaCode holen */
        String captchaCode = getCaptchaCode("http://4freeload.net/captcha.php", downloadLink);
        Form form = br.getFormbyProperty("name", "myform");
        if (form == null) form = br.getForm(1);
        if (form == null || !br.containsHTML("captcha.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.setMethod(MethodType.POST);
        String passCode = null;
        if (form.containsHTML("name=downloadpw")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            form.put("downloadpw", passCode);
        }

        /* Überprüfen(Captcha,Password) */
        form.put("captchacode", captchaCode);
        br.submitForm(form);
        if (br.containsHTML("Code fehler oder abgelaufen") || br.containsHTML("asswort")) {
            if (br.containsHTML("asswort")) {
                /* PassCode war falsch, also Löschen */
                downloadLink.setProperty("pass", null);
            }
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }

        /* Downloadlimit erreicht */
        String finalurl;
        if (br.containsHTML("Sie haben die max. Download") || br.containsHTML("Download Gr")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l); }
        /* PassCode war richtig, also Speichern */
        downloadLink.setProperty("pass", passCode);
        finalurl = br.getRegex("document\\.location=\"(.*?)\"").getMatch(0);
        if (finalurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        // waittime not needed right now
        // sleep(20000, downloadLink);
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalurl, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        br.getPage(parameter.getDownloadURL());
        if (nopremium)
            doFree(parameter);
        else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, br.getRedirectLocation(), false, 1);
            dl.startDownload();
        }
    }

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCookie("http://www.4freeload.net", "yab_mylang", "de");
        br.getPage("http://4freeload.net/login.php");
        Form form = br.getFormbyProperty("name", "lOGIN");
        if (form == null) form = br.getForm(0);
        form.put("user", Encoding.urlEncode(account.getUser()));
        form.put("pass", Encoding.urlEncode(account.getPass()));
        form.put("autologin", "0");
        br.submitForm(form);
        br.getPage("http://4freeload.net/members.php");
        String accType = br.getRegex("'package_info'\\)\"><b>(.*?)</b>").getMatch(0);
        if (accType == null) {
            logger.warning("accType Regex failed!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (accType.equals("Registered")) nopremium = true;
        if (br.getCookie("http://www.4freeload.net", "yab_passhash") == null || br.getCookie("http://www.4freeload.net", "yab_uid").equals("0")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String expired = br.getRegex("Paket abgelaufen\\?.+?left\">(.*?)<a").getMatch(0).trim();
        if (expired != null) {
            if (expired.equalsIgnoreCase("Nein"))
                ai.setExpired(false);
            else if (expired.equalsIgnoreCase("Ja")) ai.setExpired(true);
        }
        if (!nopremium) {
            String expires = br.getRegex("Paket läuft ab am.+?left\">(.*?)</td>").getMatch(0).trim();
            if (expires != null) {
                String[] e = expires.split("/");
                Calendar cal = new GregorianCalendar(Integer.parseInt("20" + e[2]), Integer.parseInt(e[0]) - 1, Integer.parseInt(e[1]));
                ai.setValidUntil(cal.getTimeInMillis());
            }
        }
        String create = br.getRegex("Registriert am.+?left\">(.*?)</td>").getMatch(0).trim();
        if (create != null) {
            String[] c = create.split("/");
            Calendar cal = new GregorianCalendar(Integer.parseInt("20" + c[2]), Integer.parseInt(c[0]) - 1, Integer.parseInt(c[1]));
            ai.setCreateTime(cal.getTimeInMillis());
        }

        ai.setFilesNum(0);
        String files = br.getRegex("Hochgeladene Dateien.+?left\">(.*?)<a").getMatch(0).trim();
        if (files != null) {
            ai.setFilesNum(Integer.parseInt(files));
        }

        ai.setPremiumPoints(0);
        String points = br.getRegex("Bonuspunkte insgesamt.+?left\">(.*?)</td>").getMatch(0).trim();
        if (points != null) {
            ai.setPremiumPoints(Integer.parseInt(points));
        }

        if (nopremium)
            ai.setStatus("Registered (free) User");
        else
            ai.setStatus("Premium User");
        account.setValid(true);

        return ai;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
