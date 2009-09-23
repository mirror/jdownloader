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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megashare.gr" }, urls = { "http://[\\w\\.]*?megashare\\.gr/file/[0-9]+/" }, flags = { 0 })
public class MegaShareGr extends PluginForHost {

    public MegaShareGr(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://megashare.gr/register.php");
    }

    @Override
    public String getAGBLink() {
        return "http://megashare.gr/en/rules.php";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCookie("http://megashare.gr", "yab_mylang", "en");
        br.getPage("http://megashare.gr/login.php");
        Form form = br.getFormbyProperty("name", "lOGIN");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        form.put("user", Encoding.urlEncode(account.getUser()));
        form.put("pass", Encoding.urlEncode(account.getPass()));
        br.setFollowRedirects(true);
        br.submitForm(form);
        if (!br.containsHTML("\\(this, 'package_info'\\)\"><b>Premium</b") || br.containsHTML("can not be found in our database")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String hostedFiles = br.getRegex("<b>Hosted Files</b></td>.*?<td align=\"left\">(\\d+).*?<a href").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Long.parseLong(hostedFiles));
        account.setValid(true);
        ai.isUnlimitedTraffic();
        String points = br.getRegex("<b>Total Downloads</b></td>.*?<td align=\"left\">(\\d+)</td>").getMatch(0);
        if (points != null) {
            ai.setPremiumPoints(Long.parseLong(points));
        }
        return ai;
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, (downloadLink.getDownloadURL()), true, 1);
        if (!(dl.getConnection().isContentDisposition())) throw new PluginException(LinkStatus.ERROR_FATAL);
        dl.startDownload();
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return 20;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Your requested file is not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<b>File name:</b></td>.*?<td align=.*?width=[0-9]+px>(.*?)</td>").getMatch(0);

        if (filename == null) {
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("title=\"Click this to report for (.*?)\"").getMatch(0);
            }
        }
        String filesize = br.getRegex("<b>File size:</b></td>.*?<td align=left>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL() + "?setlang=en");
    }

    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        Form captchaForm = br.getForm(0);
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);

        for (int i = 0; i < 5; i++) {
            String captchaurl = null;
            if (br.containsHTML("captcha.php")) {
                captchaurl = "http://megashare.gr/captcha.php";
            }
            if (captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            String code = getCaptchaCode(captchaurl, downloadLink);
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            captchaForm.put("captchacode", code);
            br.submitForm(captchaForm);
            if (br.containsHTML("You have got max allowed bandwidth size per hour")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1001l);
            if (!br.containsHTML("captcha.php")) break;
        }
        if (br.containsHTML("captcha.php") || br.containsHTML("Captcha number error or expired")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String dllink = br.getRegex("(http://(megashare|[a-z0-9]+\\.megashare)\\.gr/getfile\\.php\\?id=.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);

        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
