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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "novamov.me" }, urls = { "http://(www\\.)?novamov\\.me/video/[a-z0-9]+" }, flags = { 0 })
public class NovaMovMe extends PluginForHost {

    private String DLLINK = "";

    public NovaMovMe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.novamov.me/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    /** Clone of site: novamov.com */

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("This file no longer exists on our servers|The file has failed to convert!") || br.getURL().contains("novamov.me/index.php")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("name=\"title\" content=\"Watch(.*?)online\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Watch(.*?)online \\| NovaMov - Free and reliable flash video hosting</title>").getMatch(0);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim();
        downloadLink.setFinalFileName(filename.replace(filename.substring(filename.length() - 4, filename.length()), "") + ".mp4");
        if (br.containsHTML("error_msg=The video is being transfered")) {
            downloadLink.getLinkStatus().setStatusText("Not downloadable at the moment, try again later...");
        }

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (br.containsHTML("error_msg=The video is being transfered")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not downloadable at the moment, try again later...", 60 * 60 * 1000l);
        br.postPage(br.getURL(), "videoplayer=go");
        getVideoLink();
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void getVideoLink() throws PluginException, IOException {
        DLLINK = br.getRegex("file: \"(http://[^<>\"]*?)\"").getMatch(0);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}