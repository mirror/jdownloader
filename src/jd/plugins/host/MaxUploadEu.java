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

package jd.plugins.host;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class MaxUploadEu extends PluginForHost {

    public MaxUploadEu(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(100l);
    }

    //@Override
    public String getAGBLink() {
        return "http://www.maxupload.eu/en/terms";
    }
    
    public String fileno;

    //@Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        fileno = new Regex(downloadLink.getDownloadURL(),"maxupload.eu/../(\\d+)").getMatch(0);
        if (fileno == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.getPage("http://www.maxupload.eu/en/"+fileno);
        String filename = br.getRegex("class=\"fname\"><strong>(.*?)</strong>").getMatch(0);
        String filesize = br.getRegex("size:</span> (.*?)<br").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    //@Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        String getlink;
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        getlink = br.getRegex("a rel=\"nofollow\" href=\"(.*?)\"").getMatch(0);
        if (getlink == null) getlink = "http://www.maxupload.eu/download.php?id=" + fileno;
        if (getlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        // this.sleep(3000, downloadLink); // uncomment when they introduce waittime
        dl = br.openDownload(downloadLink, getlink, false, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isOK()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE); 
        }
        dl.startDownload();
    }

    //@Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    //@Override
    public void reset() {
    }

    //@Override
    public void resetPluginGlobals() {
    }

    //@Override
    public void resetDownloadlink(DownloadLink link) {
    }
}