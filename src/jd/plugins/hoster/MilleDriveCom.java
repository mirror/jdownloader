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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "milledrive.com" }, urls = { "http://[\\w\\.]*?milledrive\\.com/(music|files|videos(/.*?)?|files/video|files/music)/\\d+/.*" }, flags = { 0 })
public class MilleDriveCom extends PluginForHost {

    public MilleDriveCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://milledrive.com/terms_of_service/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String firstlink = downloadLink.getDownloadURL();
        br.getPage(firstlink);
        if (!br.containsHTML("<head>")) {
            this.sleep(2000, downloadLink);
            br.getPage(firstlink);
        }
        if (br.containsHTML("(URL does not exist|onclick=\"javascript:alert\\('This video has been Removed|404 not found)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("/wait_encode.png") || br.containsHTML("This video is still being encoded")) downloadLink.getLinkStatus().setStatusText(JDL.L("plugin.hoster.milledrive.com.stillencoding", "This video is still being encoded"));
        String filename, filesize;
        if (!firstlink.contains("/files/")) // for videos & music links
        {
            filename = br.getRegex("down.direct\"\\s+href=\"http://.*?milledrive.com/files/\\w+/\\d+/(.*?)\"").getMatch(0);
            if (filename == null) filename = br.getRegex("<title>(.*?) - Milledrive</title>").getMatch(0);
            if (filename == null) filename = br.getRegex("<title>Milledrive - (.*?)</title>").getMatch(0);
            filesize = br.getRegex("Size:</span>\\s(.*?)\\s</span>").getMatch(0);
            if (filesize == null) filesize = br.getRegex("Size:</b>\\s(.*?)\\s</").getMatch(0);
        } else {
            filename = br.getRegex("id=\"free-down\" action=\".*milledrive.com/files/\\d+/(.*?)\"").getMatch(0);
            filesize = br.getRegex("\\|\\s+<span style=[^>]*>(.*?)</span>").getMatch(0);
        }
        // System.out.println(filename+" "+filesize);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        if (filesize != null) {
            filesize = filesize.replaceFirst("bytes", "B");
            downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("/wait_encode.png") || br.containsHTML("This video is still being encoded")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugin.hoster.milledrive.com.stillencoding", "This video is still being encoded"), 15 * 60 * 1000);
        String directlink = br.getRegex("file:\"(.*?)\"").getMatch(0);
        if (directlink == null) directlink = br.getRegex("url:'(.*?)'").getMatch(0);
        if (directlink == null) {
            String firstlink = downloadLink.getDownloadURL();
            if (!firstlink.contains("/files/")) {
                firstlink = br.getRegex("id=\"down-direct\"\\s+href=\"(.*?)\"").getMatch(0);
                if (firstlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                br.getPage(firstlink);
            }
            Form down1 = br.getFormbyProperty("id", "free-down");
            if (down1 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            this.sleep(30001, downloadLink);
            br.submitForm(down1);
            if (br.containsHTML("currently in use")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED); }
            String url = br.getRegex("<a href=\"(http://cache[^\"]+)").getMatch(0);
            if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (br.containsHTML("The requested URL does not exist")) throw new PluginException(LinkStatus.ERROR_RETRY);
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
            }
        } else {
            directlink = Encoding.htmlDecode(directlink);
            String finalfilename = downloadLink.getName();
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, directlink, true, 1);
            if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("text/xml")) {
                br.followConnection();
                if (br.containsHTML("File not found")) {
                    logger.info("The following link is eigher down or is an RTMP stream: " + downloadLink.getDownloadURL());
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (br.containsHTML("rtmp://")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugin.hoster.milledrive.com.rtmpVideo", "JD cannot download RTMP streams at the moment!"));
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
            }
            downloadLink.setFinalFileName(finalfilename);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
