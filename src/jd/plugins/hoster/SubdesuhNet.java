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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "subdesu-h.net" }, urls = { "https?://(?:www\\.)?subdesu\\-h\\.net/[a-z0-9\\-]+/" })
public class SubdesuhNet extends PluginForHost {

    public SubdesuhNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               videoid           = null;

    @Override
    public String getAGBLink() {
        return "https://www.subdesu-h.net/tos/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        videoid = null;

        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        videoid = this.br.getRegex("video\\.html\\?id=([a-f0-9]{32})").getMatch(0);
        if (videoid == null) {
            logger.info("Failed to find videoid --> Probably this is not a video");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "([a-z0-9\\-]+)/?$").getMatch(0);
        String filename = br.getRegex("<title>[^<>\"]+ \\- Watch ([^<>\"]+)</title>").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = default_extension;
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        this.br.getPage("https://stream.subdesu-h.net/video.php?action=1&id=" + this.videoid + "&random=" + System.currentTimeMillis());
        /*
         * 2017-05-02: E.g. {"code":200,"array":["b76fd779-715b-40cf-af4b-b6d7f78a84b4","11704e9a-83ba-4768-ab0c-787c2870ba04"]} --> That
         * array is sorted from highest --> lowest quality (usually only 480 & 240p available[freeuser])
         */
        final String videoid_second = this.br.getRegex("\"([a-z0-9\\-]{30,})\"").getMatch(0);
        if (StringUtils.isEmpty(videoid_second)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        String dllink = null;
        short counter = 0;
        do {
            logger.info(String.format("Attempt %d of 9", counter));
            counter++;
            this.sleep(5000, downloadLink);
            br.getPage("/video.php?id=" + this.videoid + "&hash=" + videoid_second + "&random=" + System.currentTimeMillis());
            /* {"code":201,"text":"Video file is being generated, please wait !"} */
            /* {"code":200,"text":"https:\/\/stream.subdesu-h.net\/video\/b76fd779-715b-40cf-af4b-b6d7f78a84b4.m3u8"} */
            dllink = PluginJSonUtils.getJson(this.br, "text");
        } while (counter <= 9 && (dllink == null || !dllink.startsWith("http")));

        if (StringUtils.isEmpty(dllink) || !dllink.startsWith("http")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
