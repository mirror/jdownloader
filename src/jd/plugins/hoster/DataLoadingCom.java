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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "data-loading.com" }, urls = { "http://[\\w\\.]*?data-loading\\.com/.{2}/file/\\d+/\\w+" }, flags = { 2 })
public class DataLoadingCom extends PluginForHost {

    public DataLoadingCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://data-loading.com/en/register.php?g=3");
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(/de/|/ru/|/es/)", "/en/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.data-loading.com/en/rules.html";
    }

    public String freeOrPremium = null;

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://data-loading.com", "yab_mylang", "en");
        br.getPage("http://data-loading.com/en/login.html");
        Form form = br.getFormbyProperty("name", "lOGIN");
        if (form == null) form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("user", Encoding.urlEncode(account.getUser()));
        form.put("pass", Encoding.urlEncode(account.getPass()));
        form.put("autologin", "0");
        br.submitForm(form);
        freeOrPremium = br.getRegex("<b>Your Package</b></td>.*?<b>(.*?)</b></A>").getMatch(0);
        if (br.getCookie("data-loading.com", "yab_passhash") == null || br.getCookie("data-loading.com", "yab_uid").equals("0") || freeOrPremium == null || (!freeOrPremium.equals("Premium") && !freeOrPremium.equals("Registriert"))) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String expired = br.getRegex("Expired\\?</b></td>.*?<td align=.*?>(.*?)<").getMatch(0);
        if (expired != null) {
            if (expired.trim().equalsIgnoreCase("No"))
                ai.setExpired(false);
            else if (expired.equalsIgnoreCase("Yes")) ai.setExpired(true);
        }

        String expires = br.getRegex("Package Expire Date</b></td>.*?<td align=.*?>(.*?)</td>").getMatch(0);
        if (expires != null && !expires.equals("Never")) {
            expires = expires.trim();
            String[] e = expires.split("/");
            Calendar cal = new GregorianCalendar(Integer.parseInt("20" + e[2]), Integer.parseInt(e[0]) - 1, Integer.parseInt(e[1]));
            ai.setValidUntil(cal.getTimeInMillis());
        }

        String create = br.getRegex("Register Date</b></td>.*?<td align=.*?>(.*?)<").getMatch(0);
        if (create != null) {
            String[] c = create.split("/");
            Calendar cal = new GregorianCalendar(Integer.parseInt("20" + c[2]), Integer.parseInt(c[0]) - 1, Integer.parseInt(c[1]));
            ai.setCreateTime(cal.getTimeInMillis());
        }

        ai.setFilesNum(0);
        String files = br.getRegex("<b>Hosted Files</b></td>.*?<td align=.*?>(.*?)<").getMatch(0);
        if (files != null) {
            ai.setFilesNum(Integer.parseInt(files.trim()));
        }

        ai.setPremiumPoints(0);
        String points = br.getRegex("<b>Total Points</b></td>.*?<td align=.*?>(.*?)</td>").getMatch(0);
        if (points != null) {
            ai.setPremiumPoints(Integer.parseInt(points.trim()));
        }
        if (freeOrPremium.equals("Premium")) {
            ai.setStatus("Premium User");
        } else {
            ai.setStatus("Registered (Free) User");
        }
        account.setValid(true);

        return ai;
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        br.setCookie("http://data-loading.com", "yab_mylang", "en");
        br.getPage(parameter.getDownloadURL());
        if (freeOrPremium.equals("Premium")) {
            if (br.containsHTML("You have got max allowed download sessions from the same IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
            String passCode = null;
            if (br.containsHTML("downloadpw")) {
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
            String linkurl = br.getRegex("<input.*document.location=\"(.*?)\";").getMatch(0);
            if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, linkurl, true, -2);
            if (!(dl.getConnection().isContentDisposition())) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            doFree2(parameter);
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://data-loading.com", "yab_mylang", "en");
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (!(br.containsHTML("Your requested file is not found") || br.containsHTML("File Not Found"))) {
            String filename = br.getRegex("<td align=left width=150px id=filename>(.*?)</td>").getMatch(0);
            String filesize = br.getRegex("<td align=left id=filezize>(.*?)</td>").getMatch(0);
            if (!(filename == null || filesize == null)) {
                downloadLink.setName(filename);
                downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
                return AvailableStatus.TRUE;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    public void doFree2(DownloadLink downloadLink) throws Exception {
        String passCode = null;
        // Captcha required or not ?
        if (br.containsHTML("captcha.php")) {
            for (int i = 0; i <= 3; i++) {
                Form captchaform = br.getFormbyProperty("name", "myform");
                String captchaurl = "http://data-loading.com/captcha.php";
                if (captchaform == null || !br.containsHTML("captcha.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (br.containsHTML("downloadpw")) {
                    if (downloadLink.getStringProperty("pass", null) == null) {
                        passCode = Plugin.getUserInput("Password?", downloadLink);

                    } else {
                        /* gespeicherten PassCode holen */
                        passCode = downloadLink.getStringProperty("pass", null);
                    }
                    captchaform.put("downloadpw", passCode);
                }
                String code = getCaptchaCode(captchaurl, downloadLink);
                captchaform.put("captchacode", code);
                br.submitForm(captchaform);
                if (br.containsHTML("Password Error")) {
                    logger.warning("Wrong password!");
                    downloadLink.setProperty("pass", null);
                    continue;
                }
                if (br.containsHTML("Captcha number error") || br.containsHTML("captcha.php") && !br.containsHTML("You have got max allowed bandwidth size per hour")) {
                    logger.warning("Wrong captcha or wrong password!");
                    downloadLink.setProperty("pass", null);
                    continue;
                }
                break;
            }
        } else {
            if (br.containsHTML("downloadpw")) {
                Form pwform = br.getFormbyProperty("name", "myform");
                if (pwform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                pwform.put("downloadpw", passCode);
                br.submitForm(pwform);
            }
        }
        if (br.containsHTML("Password Error")) {
            logger.warning("Wrong password!");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (br.containsHTML("Captcha number error") || br.containsHTML("captcha.php") && !br.containsHTML("You have got max allowed bandwidth size per hour")) {
            logger.warning("Wrong captcha or wrong password!");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        if (br.containsHTML("You have got max allowed bandwidth size per hour")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
        String linkurl = br.getRegex("<input.*document.location=\"(.*?)\";").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, false, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree2(downloadLink);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
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
