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
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

//This plugin gets all its links from a decrypter!
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chomikuj.pl" }, urls = { "\\&id=.*?\\&gallerylink=.*?60423fhrzisweguikipo9re.*?\\&" }, flags = { 0 })
public class ChoMikujPl extends PluginForHost {

    public ChoMikujPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://chomikuj.pl/Regulamin.aspx";
    }

    public String dllink = null;

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("60423fhrzisweguikipo9re", "chomikuj.pl").replace("amp;", ""));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        Browser br2 = br.cloneBrowser();
        String gallerypage = new Regex(link.getDownloadURL(), "\\&gallerylink=(.*?)\\&").getMatch(0);
        br.getPage(gallerypage);
        getDllink(link);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = br2.openGetConnection(dllink);
        if (!con.getContentType().contains("html"))
            link.setDownloadSize(con.getLongContentLength());
        else
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        getDllink(downloadLink);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void getDllink(DownloadLink theLink) throws NumberFormatException, PluginException, IOException {
        Browser br2 = br.cloneBrowser();
        String id = new Regex(theLink.getDownloadURL(), "\\&id=(.*?)\\&").getMatch(0);
        String tmp = "http://chomikuj.pl/services/InterfaceCommandService.asmx/DownloadFile";
        String postData = String.format("language=pl-PL&fileId=%s&confirmed=false&ownTransfer=false&getWindow=true", id);
        br2.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.postPage(tmp, postData);
        dllink = br2.getRegex("<RedirectUrl>(.*?)</RedirectUrl>").getMatch(0);
        if (dllink != null) dllink = dllink.replace("amp;", "");
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