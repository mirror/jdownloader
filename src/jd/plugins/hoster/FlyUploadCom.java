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
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flyupload.com" }, urls = { "http://[\\w\\.]*?flyupload\\.com/\\w*[\\?fid=]+\\d+" }, flags = { 0 })
public class FlyUploadCom extends PluginForHost {

    public FlyUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    // @Override
    public String getAGBLink() {
        return "http://www.flyupload.com/tos";
    }
    
    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (!(br.containsHTML("The file you requested has either expired or the URL has an invalid fid"))) {
            String filename = br.getRegex("Filename:</td><td style=\"border-top: 1px none #cccccc;\">(.*?)</td>").getMatch(0);
            String filesize = br.getRegex("File Size:</td><td style=\"border-top: 1px dashed #cccccc;\">(.*?)</td>").getMatch(0);
            if (!(filename == null || filesize  == null)) {
                downloadLink.setName(filename);
                downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
                return AvailableStatus.TRUE;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }
    
    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String linkurl = br.getRegex("<a href=\"http://www28.flyupload.com/dl\\?fid=(.*?)\">Download Now</a>").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        linkurl = "http://www28.flyupload.com/dl?fid=" + linkurl;
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, linkurl, false, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }
    
    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
