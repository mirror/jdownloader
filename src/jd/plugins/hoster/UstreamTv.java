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

import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ustream.tv" }, urls = { "https?://(www\\.)?ustream\\.tv/(embed/?)recorded/\\d+(/highlight/\\d+)?" })
public class UstreamTv extends PluginForHost {
    private String dllink = null;

    public UstreamTv(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.ustream.tv/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private boolean is_private = false;

    private Browser prepBR(final Browser br) {
        br.setAllowedResponseCodes(401);
        return br;
    }

    /* Thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/ustream.py */
    /* 2015-10-01: Usage of new API call thx: http://ustream.github.io/api-docs/channel.html#video */
    /* Last revision containing the "old" AMF-handling: 26193 */
    /** TODO: Add/Fix errorhandling for private videos */
    @SuppressWarnings({ "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        is_private = false;
        setBrowserExclusive();
        this.br = prepBR(this.br);
        final String fid = getFID(link);
        LinkedHashMap<String, Object> entries = null;
        /* Offline links should have nice filenames as well. */
        link.setName(fid + ".mp4");
        this.br.setFollowRedirects(true);
        /* 2nd possibility (without downloadlinks): http://api.ustream.tv/json/video/<fid>/listAllVideos?key=laborautonomo&limit=1 */
        /* Old method: */
        // final String getJsonURL = "http://cdngw.ustream.tv/rgwjson/Viewer.getVideo/%7B%22brandId%22:1,%22videoId%22:" + fid +
        // ",%22autoplay%22:false%7D";
        final String getJsonURL = "https://api.ustream.tv/videos/" + fid + ".json";
        br.getPage(getJsonURL);
        if (this.br.getHttpConnection().getResponseCode() == 401) {
            is_private = true;
            link.getLinkStatus().setStatusText("This is a private video which only the owner can watch/download");
            return AvailableStatus.TRUE;
        }
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        if (this.br.getHttpConnection().getResponseCode() == 404 || (String) entries.get("error") != null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (LinkedHashMap<String, Object>) entries.get("video");
        dllink = (String) JavaScriptEngineFactory.walkJson(entries, "media_urls/smoothStreamingUrl");
        if (dllink == null) {
            /* Sometimes only lower quality mp4's are available??! */
            dllink = (String) JavaScriptEngineFactory.walkJson(entries, "media_urls/mp4");
        }
        if (dllink == null) {
            /* Sometimes only lower quality flv's are available! */
            dllink = (String) JavaScriptEngineFactory.walkJson(entries, "media_urls/flv");
        }
        final String user = (String) JavaScriptEngineFactory.walkJson(entries, "owner/username");
        String title = (String) entries.get("title");
        final String description = (String) entries.get("description");
        long filesize = JavaScriptEngineFactory.toLong(entries.get("file_size"), -1);
        if (user == null || title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = encodeUnicode(title);
        final String ext;
        if (dllink != null && dllink.contains(".mp4")) {
            ext = ".mp4";
        } else {
            ext = ".flv";
        }
        final String filename = user + " - " + fid + " - " + title + ext;
        link.setFinalFileName(filename);
        if (description != null && link.getComment() == null) {
            link.setComment(description);
        }
        if (filesize == -1 && dllink != null) {
            /* Only check if the json source did not contain filesize information */
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    filesize = con.getLongContentLength();
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        link.setDownloadSize(filesize);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (dllink == null) {
            /* 2016-12-29: For some streams, we do not get any downloadurl via API e.g. 98355455 and 98364293. */
            throw new PluginException(LinkStatus.ERROR_FATAL, "This video is not downloadable");
        } else if (is_private) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This is a private video which only the owner can watch/download");
        }
        dl = BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        final String fid = new Regex(dl.getDownloadURL(), "(\\d+)$").getMatch(0);
        if (dl.getLinkID() == null || !dl.getLinkID().matches("\\d+")) {
            dl.setLinkID(fid);
        }
        return fid;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}