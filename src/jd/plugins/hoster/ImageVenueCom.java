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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagevenue.com" }, urls = { "http://[\\w\\.]*?img[0-9]+\\.imagevenue\\.com/img\\.php.*?image=.+" }, flags = { 0 })
public class ImageVenueCom extends PluginForHost {

    public ImageVenueCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://imagevenue.com/tos.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        /* Error handling */
        if (br.containsHTML("This image does not exist on this server")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = null;
        String finallink = br.getRegex("id=\"thepic\".*?SRC=\"(.*?)\"").getMatch(0);
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String server = new Regex(link.getDownloadURL(), "(img[0-9]+\\.imagevenue\\.com/)").getMatch(0);
        finallink = "http://" + server + finallink;
        String ending = new Regex(finallink, "imagevenue\\.com.*?\\.(.{3,4}$)").getMatch(0);
        String filename0 = new Regex(finallink, "imagevenue\\.com/.*?/.*?/\\d+.*?_(.*?)($|\\..{2,4}$)").getMatch(0);
        if (ending != null && filename0 != null) filename = filename0 + "." + ending;
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(finallink);
            if (!con.isOK()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            long size = con.getLongContentLength();
            link.setDownloadSize(Long.valueOf(size));
            link.setName(filename.trim());
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        String finallink = br.getRegex("id=\"thepic\".*?SRC=\"(.*?)\"").getMatch(0);
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String server = new Regex(downloadLink.getDownloadURL(), "(img[0-9]+\\.imagevenue\\.com/)").getMatch(0);
        finallink = "http://" + server + finallink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
        dl.startDownload();
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