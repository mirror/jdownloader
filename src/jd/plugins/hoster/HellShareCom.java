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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hellshare.com" }, urls = { "http://[\\w\\.]*?download\\.((sk|cz|en)\\.hellshare\\.com|hellshare\\.hu)/.+/[0-9]+" }, flags = { 2 })
public class HellShareCom extends PluginForHost {

    public HellShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.en.hellshare.com/register");
    }

    @Override
    public String getAGBLink() {
        return "http://www.en.hellshare.com/terms";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("http://www.en.hellshare.com/log-in");
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        form.put("lgnp7_lg", Encoding.urlEncode(account.getUser()));
        form.put("lgnp7_psw", Encoding.urlEncode(account.getPass()));
        br.setFollowRedirects(true);
        br.submitForm(form);
        if (!br.containsHTML("credit for downloads") || br.containsHTML("Špatně zadaný login nebo heslo uživatele")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String hostedFiles = br.getRegex(">Number of your files:</label></th>.*?<td id=\"info_files_counter\"><strong>(\\d+)</strong></td>").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Long.parseLong(hostedFiles));
        String trafficleft = br.getRegex("id=\"info_credit\">.*?<strong>(.*?)</strong>").getMatch(0);
        if (trafficleft != null) {
            ai.setTrafficLeft(Regex.getSize(trafficleft));
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        String dllink = br.getRegex("launchFullDownload\\('(.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://data.*?\\.helldata\\.com.*?)\"").getMatch(0);
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            System.out.print(br.toString());
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 20;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<h1>File not found</h1>") || br.containsHTML("<h1>Soubor nenalezen</h1>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filesize = br.getRegex("FileSize_master\">(.*?)</strong>").getMatch(0);
        if (filesize != null && filesize.contains("&nbsp")) {
            filesize = filesize.replace("&nbsp;", "");
        }
        if (filesize == null) {
            filesize = br.getRegex("\"The content.*?with a size of (.*?) has been uploaded").getMatch(0);
            if (filesize != null && filesize.contains("&nbsp")) {
                filesize = filesize.replace("&nbsp;", "");
            }
        }
        String filename = br.getRegex("\"FileName_master\">(.*?)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"The content (.*?) with a size").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) – Download").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("hidden\">Downloading file (.*?)</h1>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("keywords\" content=\"HellShare, (.*?)\"").getMatch(0);
                    }
                }
            }
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Current load 100%, take advantage of unlimited")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "No free slots available");
        } else {
            System.out.print(br.toString());
            throw new PluginException(LinkStatus.ERROR_FATAL, "Please send the log to a supporter");
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
