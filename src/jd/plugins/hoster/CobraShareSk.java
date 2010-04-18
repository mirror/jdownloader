//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org & pspzockerscene
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
import java.net.MalformedURLException;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cobrashare.sk" }, urls = { "http://[\\w\\.]*?cobrashare\\.sk(/downloadFile\\.php\\?id=.+|:[0-9]+/CobraShare-v.0.9/download/.+id=.+)" }, flags = { 2 })
public class CobraShareSk extends PluginForHost {

    public CobraShareSk(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://cobrashare.sk/index.php?sp=cp");
    }

    @Override
    public String getAGBLink() {
        return "http://www.cobrashare.sk/index.php?sid=2";
    }

    @Override
    public void correctDownloadLink(DownloadLink downloadLink) throws MalformedURLException {
        if (downloadLink.getDownloadURL().contains("www.cobrashare.sk:38080")) {
            downloadLink.setUrlDownload("http://www.cobrashare.sk/downloadFile.php?id=" + downloadLink.getDownloadURL().split("\\?id=")[1]);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Poadovaný súbor sa na serveri nenachádza")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("File name :&nbsp;</td>.*?<td class=\"data\">(.*?)</td>").getMatch(0));
        Regex reg = br.getRegex("Size :\\&nbsp;</td>.*?<td class=\"data\">(.*?)\\&nbsp;(.*?)</td>");
        String filesize = reg.getMatch(0) + " " + reg.getMatch(1);
        if (filename == null || (filesize == null || filesize.contains("null"))) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        handleFree0(downloadLink);
    }

    public void handleFree0(DownloadLink downloadLink) throws Exception, PluginException {
        for (int i = 0; i <= 5; i++) {
            String captchaurl = Encoding.htmlDecode(br.getRegex("id=\"overImg\" src=\"(.*?)\" width=\"100\" hei").getMatch(0));
            Form captchaForm = br.getForm(0);
            if (captchaForm == null || captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode(captchaurl, downloadLink);
            captchaForm.put("over", code);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, captchaForm, false, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.containsHTML("content=\"0;url=http://www.cobrashare.sk/down")) {
                    br.getPage(downloadLink.getDownloadURL());
                    continue;
                }
                if (br.containsHTML("window.open")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            break;
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("content=\"0;url=http://www.cobrashare.sk/down")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            if (br.containsHTML("window.open")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        }
        dl.startDownload();

    }

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.getPage("http://cobrashare.sk/index.php");
        br.postPage("http://cobrashare.sk/index.php", "lo_g_in=" + Encoding.urlEncode(account.getUser()) + "&pa_ss=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie("http://cobrashare.sk", "userId") == null || br.getCookie("http://cobrashare.sk", "userHa") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://cobrashare.sk/index.php?sp=u");
        String availableTraffic = br.getRegex(">Do vyèerpania 7 dòového limitu Vám ete ostáva: </td>.*?<td style=\".*?\" align=\"right\">(.*?)</td>").getMatch(0);
        if (availableTraffic != null) {
            availableTraffic = availableTraffic.replace("&nbsp;", "");
            ai.setTrafficLeft(Regex.getSize(availableTraffic.replace("&nbsp;", "")));
        }
        String validUntil = br.getRegex(">Vá premium account je platný do:</td>.*?<td style=\"color:.*?; font-weight:bold;\" align=\"right\">(.*?)</td>").getMatch(0);
        String points = br.getRegex(">Premium points:</td>.*?<td style=\"color:.*?; font-weight:bold;\" align=\"right\">(\\d+)</td>").getMatch(0);
        if (points == null) points = br.getRegex(">Premium points: </td>.*?<td valign=\"top\"><b>(\\d+)</b></td></tr>").getMatch(0);
        if (points != null) {
            ai.setPremiumPoints(Integer.parseInt(points.trim()));
        }
        if (validUntil != null)
            ai.setValidUntil(Regex.getMilliSeconds(validUntil, "dd.MM.yyyy HH:mm", null));
        else
            account.setValid(false);
        ai.setStatus("Premium User");
        account.setValid(true);
        return ai;
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        br.getPage(parameter.getDownloadURL());
        handleFree0(parameter);
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

}