//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.simplejson.ParserException;

@HostPlugin(revision = "$Revision: $", interfaceVersion = 2, names = { "adoctukuhampco.in" }, urls = { "http://adoctukuhampco.in/.*" }, flags = { 0 })
public class AdoctukuhampcoIn extends PluginForHost {

    private String dllink = null;

    public AdoctukuhampcoIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return ""; // none found on website
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException, ParserException {

        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (!br.getURL().contains("adoctukuhampco.in") || br.containsHTML("Not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // get page content and make sure we've not been redirected to another domain

        final String videoTitle = br.getRegex("<title>(.*?) - Porn Tube Video</title>").getMatch(0).trim();
        if (null == videoTitle) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Could not locate the tag containing the video title.");
        }
        // find the name of the video

        final String videoUrl = br.getRegex("<video(.*?)<source src=\"(.*?)\"").getMatch(1).trim(); // (.|\\n)*</video>
        if (null == videoUrl) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Could not locate the tag containing the video url.");
        }
        // find the url of the video file

        final String extension = videoUrl.substring(videoUrl.lastIndexOf('.'));
        final String finalFilename = videoTitle + extension;
        downloadLink.setFinalFileName(finalFilename);
        // assemble the final name of the file that is to be downloaded

        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(videoUrl);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        // find file size and return status

    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
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

    @Override
    public void resetPluginGlobals() {
    }
}