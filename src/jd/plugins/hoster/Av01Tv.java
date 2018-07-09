//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision:$", interfaceVersion = 3, names = { "AV01.tv" }, urls = { "https?://(?:www\\.)?av01\\.tv/.+" })
public class Av01Tv extends PluginForHost {
    public Av01Tv(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String DEFAULT_EXTENSION = ".mp4";
    private static final int    FREE_MAXDOWNLOADS = -1;
    private String              dllink            = null;

    @Override
    public String getAGBLink() {
        return "https://www.av01.tv/dmca/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<title>([^<>\"]*?) &#8211; ").getMatch(0);
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = Encoding.htmlDecode(title.trim());
        String filename = title;
        if (!filename.endsWith(DEFAULT_EXTENSION)) {
            filename += DEFAULT_EXTENSION;
        }
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);
        // see trigger_video_hls.js
        // getCode(){ return "xxx"};
        String getCode = br.getRegex("getCode\\(\\)\\{ return \"([^\"]+)\"\\};").getMatch(0);
        if (getCode == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.Base64Decode(getCode);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        getPage(dllink);
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(br));
        if (hlsbest == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        String ref = downloadLink.getPluginPatternMatcher();
        br.getHeaders().put("Referer", ref);
        dl = new HLSDownloader(downloadLink, br, hlsbest.getDownloadurl());
        dl.startDownload();
    }

    private void getPage(String page) throws Exception {
        br.getPage(page);
    }

    @Override
    public String encodeUnicode(final String input) {
        if (input != null) {
            String output = input;
            output = output.replace(":", "：");
            output = output.replace("|", "｜");
            output = output.replace("<", "＜");
            output = output.replace(">", "＞");
            output = output.replace("/", "⁄");
            output = output.replace("\\", "∖");
            output = output.replace("*", "＊");
            output = output.replace("?", "？");
            output = output.replace("!", "！");
            output = output.replace("\"", "”");
            return output;
        }
        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}