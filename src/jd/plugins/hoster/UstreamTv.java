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
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ustream.tv" }, urls = { "http://(www\\.)?ustream\\.tv/recorded/\\d+(/highlight/\\d+)?" }, flags = { 0 })
public class UstreamTv extends PluginForHost {

    private String DLLINK = null;

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

    private static final String MESSAGETOKEN_PRIVATEVIDEO = "errorVideoPrivated";

    private String              errormessage              = null;

    /* Thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/ustream.py */
    /* Last revision containing the "old" AMF-handling: 26193 */
    @SuppressWarnings({ "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        errormessage = null;
        setBrowserExclusive();
        getFID(link);
        LinkedHashMap<String, Object> entries = null;
        final String fid = link.getLinkID();
        /* Offline links should have nice filenames as well. */
        link.setName(fid + ".mp4");
        this.br.setFollowRedirects(true);
        /* 2nd possibility: http://api.ustream.tv/json/video/<fid>/listAllVideos?key=laborautonomo&limit=1 */
        final String getJsonURL = "http://cdngw.ustream.tv/rgwjson/Viewer.getVideo/%7B%22brandId%22:1,%22videoId%22:" + fid + ",%22autoplay%22:false%7D";
        br.getPage(getJsonURL);
        entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        /* If !success the video is probably offline for some reason */
        if (!((Boolean) entries.get("success")).booleanValue()) {
            entries = (LinkedHashMap<String, Object>) entries.get("error");
            errormessage = (String) entries.get("messageToken");
            if (errormessage != null && errormessage.equals(MESSAGETOKEN_PRIVATEVIDEO)) {
                link.getLinkStatus().setStatusText("This is a private video which only the owner can watch/download");
                return AvailableStatus.TRUE;
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DLLINK = (String) entries.get("smoothStreamingUrl");
        if (DLLINK == null) {
            /* Sometimes only lower quality flv's are available! */
            DLLINK = (String) entries.get("flv");
        }
        entries = (LinkedHashMap<String, Object>) entries.get("moduleConfig");
        entries = (LinkedHashMap<String, Object>) entries.get("meta");
        final String channel = (String) entries.get("channelUrl");
        final String user = (String) entries.get("userName");
        String title = (String) entries.get("title");
        if (DLLINK == null || channel == null || user == null || title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = encodeUnicode(title);

        final String ext;
        if (DLLINK.contains(".mp4")) {
            ext = ".mp4";
        } else {
            ext = ".flv";
        }
        final String filename = channel + " - " + user + " - " + fid + " - " + title + ext;

        link.setFinalFileName(filename);

        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (errormessage != null && errormessage.equals(MESSAGETOKEN_PRIVATEVIDEO)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This is a private video which only the owner can watch/download");
        }
        dl = BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    /* Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
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