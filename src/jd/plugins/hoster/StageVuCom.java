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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "stagevu.com" }, urls = { "http://[\\w\\.]*?stagevu\\.com/(video/[a-z0-9]{12}|embed\\?.*)" }, flags = { 0 })
public class StageVuCom extends PluginForHost {

    public StageVuCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://stagevu.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws PluginException {
        // convert embed links back into standard links
        if (link.getDownloadURL().contains(".com/embed?")) {
            String uid = new Regex(link.getDownloadURL(), "uid=([a-z]{12})").getMatch(0);
            if (uid != null)
                link.setUrlDownload("http://stagevu.com/video/" + uid);
            else
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        // Invalid link
        if (br.containsHTML(">Error: No video with the provided information exists</div>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Offline link
        if (br.containsHTML("The video you are attempting to view has been removed<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)- Stagevu: Your View</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<div id=\"vidbox\">[\n\r\t ]+<h1>(.*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<param name=\"movieTitle\" value=\"(.*?)\"").getMatch(0);
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".avi");
        if (br.containsHTML(">Restricted Content<")) {
            link.getLinkStatus().setStatusText("Only downloadable for registered users!");
            return AvailableStatus.TRUE;
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(">Restricted Content<")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered users!");
        }
        String dllink = br.getRegex("<embed type=\"video/divx\" src=\"(http.*?\\.avi)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<param name=\"src\" value=\"(http.*?\\.avi)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(http://n\\d+\\.stagevu\\.com/v/[a-z0-9]+/[a-z0-9]{12}\\.avi)\"").getMatch(0);
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /** Sometimes 2 connections are possible but not always... */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">404 \\- Not Found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}