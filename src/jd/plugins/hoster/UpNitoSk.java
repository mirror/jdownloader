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
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "upnito.sk" }, urls = { "http://[\\w\\.]*?upnito\\.sk/(download\\.php\\?dwToken=[a-z0-9]+|subor/[a-z0-9]+\\.html)" }, flags = { 2 })
public class UpNitoSk extends PluginForHost {

    public UpNitoSk(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.upnito.sk/kredit.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.upnito.sk/pravidla.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("windows-1250");
        br.getPage(link.getDownloadURL());
        String whySoComplicated = br.getRegex("'(http://dl[0-9]+\\.upnito\\.sk/download\\.php\\?dwToken=[a-z0-9]+)'").getMatch(0);
        if (whySoComplicated != null) br.getPage(whySoComplicated);
        if (br.containsHTML("location.href='/notfound.php'")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Nemozete tolkokrat za sebou stahovat ten isty subor!")) return AvailableStatus.UNCHECKABLE;
        String filename = br.getRegex("Súbor:</strong>(.*?)<br").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<strong style=\"color: #663333;\">(.*?)</strong>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("Ahoj, chystáš sa stiahnuť súbor(.*?)\\(<strong").getMatch(0);
            }
        }
        String filesize = br.getRegex("Veľkosť:</strong>(.*?)<br>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Nemozete tolkokrat za sebou stahovat ten isty subor!")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        Form freeform = br.getFormbyProperty("name", "gdl");
        if (freeform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String thisDamnToken = new Regex(downloadLink.getDownloadURL(), "dwToken=([a-z0-9]+)").getMatch(0);
        Browser br2 = br.cloneBrowser();
        br2.getPage("http://dl1.upnito.sk/getwait.php?dwToken=" + thisDamnToken);
        String gwt_validate = br2.toString().trim();
        freeform.put("gwt_validate", gwt_validate);
        sleep(600 * 1001l, downloadLink);
        br.submitForm(freeform);
        freeform = br.getFormbyProperty("name", "gdl");
        if (freeform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        freeform.put("gwt_validate", gwt_validate);
        sleep(5 * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, freeform, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getPage("http://www.upnito.sk/badlogin.php");
        br.postPage("http://www.upnito.sk/?action=doLogin", "meno=" + Encoding.urlEncode(account.getUser()) + "&heslo=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie("http://www.upnito.sk", "uid") == null || br.getCookie("http://www.upnito.sk", "pass") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://upnito.sk/account.php");
        account.setValid(true);
        String files = br.getRegex("Poèet súborov:</strong>.*?(\\d+)<br>").getMatch(0);
        if (files != null) {
            ai.setFilesNum(Integer.parseInt(files.trim()));
        }
        String trafficLeft = br.getRegex(">Aktuálny kredit:</strong>(.*?)\\(").getMatch(0);
        if (trafficLeft != null) {
            trafficLeft = trafficLeft.trim().replace(",", "");
            trafficLeft = new Regex(trafficLeft, "(\\d+)").getMatch(0);
            if (trafficLeft != null) {
                int traffic = Integer.parseInt(trafficLeft);
                traffic = traffic * 1024;
                trafficLeft = traffic + "KB";
                ai.setTrafficLeft(Regex.getSize(trafficLeft));
            }
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String whySoComplicated = br.getRegex("'(http://dl[0-9]+\\.upnito\\.sk/download\\.php\\?dwToken=[a-z0-9]+)'").getMatch(0);
        if (whySoComplicated != null) br.getPage(whySoComplicated);
        String dllink = br.getRegex("'(http://dl[0-9]+\\.upnito\\.sk/ddl\\.php\\?dwToken=[a-z0-9]+)'").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}