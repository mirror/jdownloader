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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "luv2skeet.com" }, urls = { "http://(?:www\\.)?luv2skeet\\.com/(\\d+)(?:/[a-z0-9\\-]+)?" })
public class Luv2skeetCom extends PluginForHost {
    public Luv2skeetCom(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    // Tags: Porn Plugin
    // protocol: no https

    /* Extension which will be used if no correct extension is found */
    private static final String default_Extension = ".mp4";
    /* Connection stuff */
    private final int           free_maxdownloads = -1;

    @Override
    public String getAGBLink() {
        return "http://www.luv2skeet.com/static/terms/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + default_Extension);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getPluginPatternMatcher(), "([a-z0-9\\-]+)$").getMatch(0).replace("-", " ");
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        link.setFinalFileName(filename + default_Extension);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        /*
         * 2021-09-02: I noticed that this website is buggy. Some videos won't play via browser but we can still get working streaming URLs
         * via the fallback method below though I haven't added a check for this for now!
         */
        String url = br.getRegex("src='([^<>\"\\']+\\.mp4)' type='video/mp4'").getMatch(0);
        String rtmpurl = null;
        if (url == null) {
            /* Fallback/old */
            br.getPage("/modules/video/player/jw/config.php?id=" + this.getFID(link));
            rtmpurl = this.br.getRegex("<streamer>(rtmp[^<>\"]+)</streamer>").getMatch(0);
            url = this.br.getRegex("<file>([^<>\"]+)</file>").getMatch(0);
            if (url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (!url.startsWith("rtmp")) {
            /* 2021-09-02: http download */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 1);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            }
            dl.startDownload();
        } else {
            // final String app = "l2svod";
            try {
                dl = new RTMPDownload(this, link, rtmpurl);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            /* Setup rtmp connection */
            jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPageUrl(this.br.getURL());
            rtmp.setUrl(rtmpurl);
            rtmp.setPlayPath(url);
            // rtmp.setApp(app);
            rtmp.setFlashVer("WIN 20,0,0,306");
            rtmp.setSwfUrl("http://www." + this.br.getHost() + "/misc/jwplayer/player.swf");
            rtmp.setResume(false);
            ((RTMPDownload) dl).startDownload();
        }
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
