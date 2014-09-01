//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "audiomack.com" }, urls = { "http://(www\\.)?audiomack\\.com/(song/[a-z0-9\\-_]+/[a-z0-9\\-_]+|api/music/url/album/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/\\d+)" }, flags = { 0 })
public class AudioMa extends PluginForHost {

    public AudioMa(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.audiomack.com/about/terms-of-service";
    }

    private static final String TYPE_API = "http://(www\\.)?audiomack\\.com/api/music/url/album/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/\\d+";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename;
        if (link.getDownloadURL().matches(TYPE_API)) {
            br.getPage(link.getStringProperty("mainlink", null));
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = link.getStringProperty("plain_filename", null);
        } else {
            br.getPage(link.getDownloadURL());
            if (br.containsHTML(">Page not found<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<aside class=\"span2\">[\t\n\r ]+<h1>([^<>\"]*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>([^<>\"]*?) \\- download and stream \\| AudioMack</title>").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("name=\"twitter:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp3");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink;
        String apilink;
        if (downloadLink.getDownloadURL().matches(TYPE_API)) {
            apilink = downloadLink.getDownloadURL();
        } else {
            apilink = br.getRegex("\"(http://(www\\.)?audiomack\\.com/api/[^<>\"]*?)\"").getMatch(0);
            if (apilink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        br.getPage(apilink);
        dllink = br.getRegex("\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}