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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 28691 $", interfaceVersion = 2, names = { "viki.com" }, urls = { "https?://(www\\.)?viki\\.(com|mx|jp)/videos/\\d+v" })
public class VikiCom extends PluginForHost {

    public VikiCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Extension which will be used if no correct extension is found */
    private static final String  default_Extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;
    private boolean              server_issues     = false;
    private boolean              geoblocked        = false;

    @Override
    public String getAGBLink() {
        return "https://www.viki.com/terms_of_use";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload("https://www.viki.com/videos/" + getVID(link));
    }

    /** Thanks for the html5 idea guys: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/viki.py */
    /**
     * To get more/better qualities and streaming types (protocols) we'd have to use the API: http://dev.viki.com/. Unfortunately the call
     * to get the video streams is not public and only works with their own API secret which we don't have: http://dev.viki.com/v4/streams/
     * ...which is why we're using the html5 version for now. In case we ever use the API - here is the needed hmac hash function:
     * http://stackoverflow.com/questions/6312544/hmac-sha1-how-to-do-it-properly-in-java
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        geoblocked = false;
        final String vid = getVID(downloadLink);
        downloadLink.setName(vid);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        if (br.containsHTML("\"geo\":true")) {
            geoblocked = true;
        } else {
            br.getPage("https://www.viki.com/player5_fragment/" + vid + "?action=show&controller=videos");
            geoblocked = this.br.containsHTML("Sorry, this content is( currently)? not available in your region|\"geo\":true");
        }
        if (geoblocked) {
            downloadLink.getLinkStatus().setStatusText("Not available in your country (geo block)");
            downloadLink.setName("Geo blocked! " + filename + default_Extension);
            return AvailableStatus.TRUE;
        }
        server_issues = this.br.containsHTML("Video playback for this video is not supported by your browser");
        /* Should never happen */
        if (br.containsHTML("Video is Unavailable")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (server_issues) {
            downloadLink.getLinkStatus().setStatusText("Linktype not yet supported");
            downloadLink.setName(filename + default_Extension);
            return AvailableStatus.TRUE;
        }
        dllink = br.getRegex("<source type=\"video/mp4\" src=\"(https?://[^<>\"]*?)\">").getMatch(0);
        final String idpart = this.br.getMatch("oster=\"httpss?://[^/]+/videos/\\d+v/[^_]+_(\\d+)_");
        if (idpart != null) {
            /* Thx: https://github.com/dknlght/dkodi/blob/master/plugin.video.viki/plugin.video.viki-1.1.44.zip */
            /* 2017-03-11 - also possible for: 360p, 480p */
            dllink = String.format("http://content.viki.com/%s/%s_high_720p_%s.mp4", vid, vid, idpart);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        String ext = getFileNameExtensionFromString(dllink, default_Extension);
        if (ext == null) {
            ext = default_Extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            downloadLink.setFinalFileName(filename);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    downloadLink.setProperty("directlink", dllink);
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            downloadLink.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (geoblocked) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Not available in your country (geo blocked)");
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getVID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "(\\d+v)$").getMatch(0);
    }

    // private String checkDirectLink(final DownloadLink downloadLink, final String property) {
    // String dllink = downloadLink.getStringProperty(property);
    // if (dllink != null) {
    // URLConnectionAdapter con = null;
    // try {
    // final Browser br2 = br.cloneBrowser();
    // con = br2.openGetConnection(dllink);
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
