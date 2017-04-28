//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "onf.ca" }, urls = { "https?://(www\\.)?(onf|nfb)\\.ca/film/[a-z0-9\\-_]+" })
public class OnfCa extends PluginForHost {

    public OnfCa(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.nfb.ca/about/important-notices/";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    private static final String app = "a8908/v5";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"title\">([^<>\"]*?)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(br.getURL() + "/player_config");
        if (br.toString().equals("No htmlCode read")) {
            /* Media is only available as paid-version. */
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        final String[] playpaths = br.getRegex("<url>(mp4:[^<>\"]*?)</url>").getColumn(0);
        if (playpaths == null || playpaths.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String currentdomain = new Regex(br.getURL(), "https?://(?:www\\.)?([^<>\"/]+)/").getMatch(0);
        final String filmtitle_url = new Regex(br.getURL(), "/film/([^<>/\"]+)/").getMatch(0);
        final String rtmpurl = "rtmp://nfbca-stream-rtmp.nfbcdn.ca/" + app;
        final String pageurl = "http://www." + currentdomain + "/film/" + filmtitle_url + "/embed/player?player_mode=&embed_mode=0&context_type=film";
        try {
            dl = new RTMPDownload(this, downloadLink, rtmpurl);
        } catch (final NoClassDefFoundError e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
        }
        /* Setup rtmp connection */
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPageUrl(pageurl);
        rtmp.setUrl(rtmpurl);
        /* Chose highest quality available */
        final String playpath = playpaths[playpaths.length - 1];
        rtmp.setPlayPath(playpath);
        rtmp.setApp(app);
        rtmp.setFlashVer("WIN 16,0,0,235");
        rtmp.setSwfUrl("http://media1.nfb.ca/medias/flash/NFBVideoPlayer.swf");
        rtmp.setResume(true);
        ((RTMPDownload) dl).startDownload();
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