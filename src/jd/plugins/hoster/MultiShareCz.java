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
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multishare.cz" }, urls = { "http://[\\w\\.]*?multishare\\.cz/stahnout/[0-9]+/" }, flags = { 2 })
public class MultiShareCz extends PluginForHost {

    public MultiShareCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.multishare.cz/cenik/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.multishare.cz/kontakt/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(Požadovaný soubor neexistuje|Je možné, že byl již tento soubor vymazán uploaderem nebo porušoval autorská práva)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>MultiShare\\.cz :: Stáhnout soubor \"(.*?)\"</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<li>Název: <strong>(.*?)</strong>").getMatch(0);
        String filesize = br.getRegex("Velikost: <strong>(.*?)</strong").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        filesize = filesize.replace("&nbsp;", "");
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String fileid = new Regex(downloadLink.getDownloadURL(), "/stahnout/(\\d+)/").getMatch(0);
        String dllink = "http://www.multishare.cz/html/download_free.php?ID=" + fileid;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage("http://www.multishare.cz/");
        br.postPage("http://www.multishare.cz/html/prihlaseni_process.php", "jmeno=" + Encoding.urlEncode(account.getUser()) + "&heslo=" + Encoding.urlEncode(account.getPass()) + "&trvale=ano&akce=P%C5%99ihl%C3%A1sit");
        if (br.getCookie("http://www.multishare.cz", "sess_ID") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        account.setValid(true);
        String space = br.getRegex("Velikost nahraných souborů:</span>.*?<strong>(.*?)</strong>").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim().replace("&nbsp;", ""));
        String trafficleft = br.getRegex("Kredit:</span>.*?<strong>(.*?)</strong").getMatch(0);
        if (trafficleft == null) trafficleft = br.getRegex("class=\"big\"><strong>Kredit:(.*?)</strong>").getMatch(0);
        if (trafficleft != null) {
            trafficleft = trafficleft.replace("&nbsp;", "");
            trafficleft = trafficleft.replace(" ", "");
            ai.setTrafficLeft(Regex.getSize(trafficleft));
        }
        String hostedFiles = br.getRegex("Počet nahraných souborů:</span>.*?<strong>(\\d+)</strong>").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Integer.parseInt(hostedFiles));
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String fileid = new Regex(link.getDownloadURL(), "/stahnout/(\\d+)/").getMatch(0);
        String dllink = "http://www.multishare.cz/html/download_premium.php?ID=" + fileid;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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