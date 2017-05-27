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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pbs.org" }, urls = { "https?://video\\.pbs\\.org/video/\\d+|https?://(?:www\\.)?pbs\\.org/.+|https?://player\\.pbs\\.org/[a-z]+/\\d+" })
public class PbsOrg extends PluginForHost {

    @SuppressWarnings("deprecation")
    public PbsOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.pbs.org/about/policies/terms-of-use/";
    }

    /* Thx to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/pbs.py */
    /* According to the youtube-dl plugin they have a lof of other TV stations / websites - consider adding them in the future .... */
    private final String TYPE_VIDEO = "https?://video\\.pbs\\.org/video/\\d+.*?|https?://player\\.pbs\\.org/[a-z]+/\\d+.*?|https?://(?:www\\.)?pbs\\.org/video/\\d+.*?";
    private final String TYPE_OTHER = "https?://(www\\.)?pbs\\.org/.+";

    /* Decrypter */
    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> getDownloadLinks(String data, FilePackage fp) {
        ArrayList<DownloadLink> ret = super.getDownloadLinks(data, fp);
        prepBR(this.br);
        try {
            if (ret != null && ret.size() > 0) {
                /*
                 * we make sure only one result is in ret, thats the case for svn/next major version
                 */
                String vid = null;
                final DownloadLink link = ret.get(0);
                correctDownloadLink(link);
                if (link.getDownloadURL().matches(TYPE_VIDEO)) {
                    vid = new Regex(link.getDownloadURL(), "^.+/(\\d+)").getMatch(0);
                } else {
                    br.getPage(link.getDownloadURL());
                    vid = br.getRegex("mediaid:\\s*?\\'(\\d+)\\'").getMatch(0);
                    if (vid == null) {
                        /* Seems to happen when they embed their own videos: http://www.pbs.org/wgbh/nova/tech/rise-of-the-hackers.html */
                        vid = br.getRegex("class=\"watch\\-full\\-now\" onclick=\"startVideo\\(\\'(\\d+)'").getMatch(0);
                    }
                    if (vid == null) {
                        vid = br.getRegex("startVideo\\(\\'(\\d+)\\'").getMatch(0);
                    }
                    if (vid == null) {
                        /*
                         * 2016-04-12: Example url for this case:
                         * http://www.pbs.org/wnet/gperf/tony-bennett-lady-gaga-cheek-cheek-live-full-episode/3574/
                         */
                        vid = br.getRegex("class=\"wnetvid_videoid\">(\\d+)<").getMatch(0);
                    }
                }
                if (vid != null) {
                    /* Single video */
                    final String videourl = "http://www.pbs.org/video/" + vid;
                    final DownloadLink fina = new DownloadLink(this, null, getHost(), videourl, true);
                    ret.add(fina);
                    return ret;
                }
                /* Either nothing or multiple urls. */
                final String[] videoids = br.getRegex("data\\-mediaid=\"(\\d+)\"").getColumn(0);
                if (videoids != null && videoids.length > 0) {
                    for (final String videoid : videoids) {
                        final String videourl = "http://www.pbs.org/video/" + videoid;
                        final DownloadLink fina = new DownloadLink(this, null, getHost(), videourl, true);
                        fina.setContentUrl(videourl);
                        ret.add(fina);
                    }
                } else {
                    /* Whatever the user added - it doesn't seem to be a video or contain a video --> Don't add it */
                }
            }
        } catch (final Throwable e) {
            logger.severe(e.getMessage());
        }
        return ret;
    }

    private Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 101, 404, 410 });
        /* This cookie is needed for some videos */
        br.setCookie("player.pbs.org", "pbsol.station", "WMPN");
        // br.setCookie("player.pbs.org", "pbsol.common.name", "MPB");
        // br.setCookie("player.pbs.org", "pbskids.localized", "WMPN");
        /* E.g. full string 'z%3D39232%23t%3Db%23s%3D%5B%22WMPN%22%2C%22WRLK%22%5D%23st%3DMS%23co%3DUS' */
        // br.setCookie("player.pbs.org", "pbsol.sta_extended", "%23co%3DUS");
        return br;
    }

    @SuppressWarnings({ "deprecation" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        prepBR(this.br);
        String vid;
        if (link.getDownloadURL().matches(TYPE_VIDEO)) {
            vid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        } else {
            br.getPage(link.getDownloadURL().replace("directhttp://", ""));
            vid = br.getRegex("mediaid:\\s*?\\'(\\d+)\\'").getMatch(0);
            if (vid == null) {
                /* Seems to happen when they embed their own videos: http://www.pbs.org/wgbh/nova/tech/rise-of-the-hackers.html */
                vid = br.getRegex("class=\"watch\\-full\\-now\" onclick=\"startVideo\\(\\'(\\d+)'").getMatch(0);
            }
            if (vid == null) {
                /* Seems to happen when they embed their own videos: http://www.pbs.org/wgbh/nova/tech/rise-of-the-hackers.html */
                vid = br.getRegex("class=\"vid\\-link\" onclick=\"startVideo\\(\\'(\\d+)\\'").getMatch(0);
            }
            if (vid == null) {
                /* Seems to happen when they embed their own videos: http://www.pbs.org/wgbh/nova/tech/rise-of-the-hackers.html */
                vid = br.getRegex("startVideo\\(\\'(\\d+)\\'").getMatch(0);
            }
            if (vid == null) {
                /* 2016-11-23: E.g. embedded video: http://www.pbs.org/wgbh/frontline/film/policing-the-police */
                /* --> If they got pages with multiple videos we'll need a decrypter! */
                vid = br.getRegex("<div[^<>]*?class=\"film__stage\"[^<>]*?id=\"video\\-(\\d+)\"[^<>]*?>").getMatch(0);
            }
            /* Whatever the user added - it doesn't seem to be a video --> Offline */
            if (vid == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        link.setLinkID(vid);
        br.getPage("http://player.pbs.org/viralplayer/" + vid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("\\'title\\'[\t\n\r ]*?:[\t\n\r ]*?\\'([^<>\"\\']+)\\'").getMatch(0);
        if (title != null) {
            title = vid + "_" + title;
        } else {
            /* Fallback to url-filename */
            title = vid;
        }
        title = Encoding.unicodeDecode(title);
        link.setName(title + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        this.br.setFollowRedirects(false);
        /* Might come handy in the future */
        // final String embedType = this.br.getRegex("embedType[\t\n\r ]*?:[\t\n\r ]*?\\'([^<>\"\\']+)\\'").getMatch(0);
        /* Find available streaming formats/protocols */
        String url_http = null;
        String url_hls_base = null;
        final String[] urls = this.br.getRegex("\\'url\\'[\t\n\r ]*?:[\t\n\r ]*?\\'(https?://urs\\.pbs\\.org/redirect/[^<>\"\\']+)\\'").getColumn(0);
        if (urls != null) {
            for (final String url : urls) {
                try {
                    this.br.getPage(url);
                    final String redirect = this.br.getRedirectLocation();
                    if (redirect == null) {
                        continue;
                    }
                    if (redirect.contains(".m3u8") && url_hls_base == null) {
                        url_hls_base = redirect;
                    } else if (url_http == null) {
                        url_http = redirect;
                    }

                } catch (final Throwable e) {
                }
            }
        }
        if (url_http == null && url_hls_base == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.setFollowRedirects(true);
        /* TODO: Make sure that the errorhandling for GEO-blocked content is working fine! */
        if (url_hls_base != null) {
            /* Prefer hls as video- and audio bitrate is higher and also the resolution. */
            br.getPage(url_hls_base);
            handleResponsecodeErrors(this.br.getHttpConnection());
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, this.br, url_hls);
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, url_http, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                handleResponsecodeErrors(dl.getConnection());
                br.followConnection();
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private void handleResponsecodeErrors(final URLConnectionAdapter con) throws PluginException {
        if (con == null) {
            return;
        }
        final int responsecode = con.getResponseCode();
        switch (responsecode) {
        case 101:
            throw new PluginException(LinkStatus.ERROR_FATAL, "Server error 101 We're sorry, but this video is not yet available.'", 1 * 60 * 60 * 1000l);
        case 403:
            /* 'We're sorry, but this video is not available in your region due to right restrictions.' */
            throw new PluginException(LinkStatus.ERROR_FATAL, "This content is GEO-blocked in your country", 3 * 60 * 60 * 1000l);
        case 404:
            /*
             * 'We are experiencing technical difficulties that are preventing us from playing the video at this time. Please check back
             * again soon.'
             */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Server error 404 'We are experiencing technical difficulties that are preventing us from playing the video at this time. Please check back again soon.'", 30 * 60 * 1000l);
        case 410:
            /* 'This video has expired and is no longer available for online streaming.' */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        default:
            break;
        }
    }

    // private String checkDirectLink(final DownloadLink downloadLink, final String property) {
    // String dllink = downloadLink.getStringProperty(property);
    // if (dllink != null) {
    // URLConnectionAdapter con = null;
    // try {
    // final Browser br2 = br.cloneBrowser();
    // if (isJDStable()) {
    // con = br2.openGetConnection(dllink);
    // } else {
    // con = br2.openHeadConnection(dllink);
    // }
    // if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
    // downloadLink.setProperty(property, Property.NULL);
    // dllink = null;
    // }
    // } catch (final Exception e) {
    // downloadLink.setProperty(property, Property.NULL);
    // dllink = null;
    // } finally {
    // try {
    // con.disconnect();
    // } catch (final Throwable e) {
    // }
    // }
    // }
    // return dllink;
    // }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}