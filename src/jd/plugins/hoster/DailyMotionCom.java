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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dailymotion.com" }, urls = { "http://[\\w\\.]*?dailymotion\\.com/video/[a-z0-9]+_.{1}" }, flags = { 0 })
public class DailyMotionCom extends PluginForHost {

    public DailyMotionCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.dailymotion.com/de/legal/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.dailymotion.com", "family_filter", "off");
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex("<title>Dailymotion -(.*?)- ein Film \\& Kino Video</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("name=\"title\" content=\"Dailymotion -(.*?)- ein Film").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("videos</a><span> > </span><b>(.*?)</b></div>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("class=\"title\" title=\"(.*?)\"").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("vs_videotitle:\"(.*?)\"").getMatch(0);
                    }
                }
            }
        }
        dllink = br.getRegex("\\.addVariable\\(\"sequence\",  \"(.*?)\"").getMatch(0);
        if (dllink != null) {
            String allLinks = Encoding.htmlDecode(dllink);
            if (allLinks.contains("Dein Land nicht abrufbar")) {
                // Video not available for your country, let's get the
                // downloadUrl from another place
                logger.info("This video is not available for this country, trying to get the url from another place...");
                dllink = br.getRegex("addVariable\\(\"video\", \"(http://.*?)\"\\)").getMatch(0);
                if (dllink == null) dllink = br.getRegex("\"(http://(www\\.)?dailymotion\\.com/cdn/.*?)\"").getMatch(0);
            } else {
                // Prefer HD videos
                dllink = new Regex(allLinks, "hqURL\":\"(http:.*?)\"").getMatch(0);
                if (dllink == null) dllink = new Regex(allLinks, "sdURL\":\"(http:.*?)\"").getMatch(0);
            }
        }
        if (filename == null || dllink == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        dllink = dllink.replace("\\", "");
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ".mp4");
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // They do allow resume and unlimited chunks but resuming or using more
        // than 1 chunk causes problems, the file will then b corrupted!
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
