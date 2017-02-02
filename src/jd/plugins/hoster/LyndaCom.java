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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "lynda.com" }, urls = { "http://(www\\.)?lynda\\.com/home/Player\\.aspx\\?lpk4=\\d+" })
public class LyndaCom extends PluginForHost {

    public LyndaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String  dllink        = null;
    private boolean server_issues = false;

    @Override
    public String getAGBLink() {
        return "http://www.lynda.com/aboutus/lotterms.aspx";
    }

    public void correctDownloadLink(final DownloadLink link) {
        final String linkid = new Regex(link.getDownloadURL(), "").getMatch(0);
        link.setLinkID(linkid);
        link.setContentUrl("http://www." + this.getHost() + "/home/Player.aspx?lpk4=" + linkid);
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        server_issues = false;

        final String videoid = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        downloadLink.setName(videoid);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://www.lynda.com/player/popup?lpk4=" + videoid);
        final String courseid = this.br.getRegex("var courseId = (\\d+);").getMatch(0);
        if (courseid == null || courseid.equals("0")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (filename == null) {
            filename = courseid + "_" + videoid;
        }
        br.getHeaders().put("Content-Type", "application/json; charset=utf-8");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        this.br.getPage("http://www.lynda.com/ajax/player?videoId=" + videoid + "&courseId=" + courseid + "&type=video&_=" + System.currentTimeMillis());
        try {
            long max_bitrate = 0;
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final String status = (String) entries.get("Status");
            if ("ExpiredSession".equals(status)) {
                logger.info("Only preview available --> Item can be considered as offline at least for users without accounts!");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Object prioritizedStreams_o = entries.get("PrioritizedStreams");
            if (prioritizedStreams_o instanceof ArrayList) {
                final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("PrioritizedStreams");
                entries = (LinkedHashMap<String, Object>) ressourcelist.get(0);
            } else {
                LinkedHashMap<String, Object> prioritizedStreams_map = (LinkedHashMap<String, Object>) entries.get("PrioritizedStreams");
                for (final Map.Entry<String, Object> entry : prioritizedStreams_map.entrySet()) {
                    entries = (LinkedHashMap<String, Object>) entry.getValue();
                    break;
                }
            }
            for (final Map.Entry<String, Object> entry : entries.entrySet()) {
                final String bitrate_str = entry.getKey();
                final String url = (String) entry.getValue();
                final long bitrate_temp = Long.parseLong(bitrate_str);
                if (bitrate_temp > max_bitrate) {
                    max_bitrate = bitrate_temp;
                    dllink = url;
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String ext;
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            ext = getFileNameExtensionFromString(dllink, ".mp4");
        } else {
            ext = ".mp4";
        }
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        if (dllink != null) {
            this.br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
