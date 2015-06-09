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

import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.downloader.hls.HLSDownloader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pbs.org" }, urls = { "https?://(www\\.)?(video\\.pbs\\.org/video/\\d+|pbs\\.org/.+)" }, flags = { 3 })
public class PbsOrg extends PluginForHost {

    @SuppressWarnings("deprecation")
    public PbsOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.pbs.org/about/policies/terms-of-use/";
    }

    private static final String           TYPE_VIDEO = "https?://(www\\.)?video\\.pbs\\.org/video/\\d+";
    private static final String           TYPE_OTHER = "https?://(www\\.)?pbs\\.org/.+";

    private LinkedHashMap<String, Object> entries    = null;

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String vid;
        if (link.getDownloadURL().matches(TYPE_VIDEO)) {
            vid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        } else {
            br.getPage(link.getDownloadURL());
            vid = br.getRegex("mediaid:\\s*?\\'(\\d+)\\'").getMatch(0);
            /* Whatever the user added - it doesn't seem to be a video --> Offline */
            if (vid == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        link.setLinkID(vid);
        /* These Headers are not necessarily needed! */
        br.getHeaders().put("Accept", "text/javascript, application/javascript, application/ecmascript, application/x-ecmascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Referer", "http://video.pbs.org/video/" + vid + "/");
        br.getPage("http://video.pbs.org/videoInfo/" + vid + "/?callback=video_info&format=jsonp&type=portal&_=" + System.currentTimeMillis());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex("^video_info\\((.+)\\)$").getMatch(0);
        if (json == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(json);
        final String description = (String) entries.get("description");
        final String title = (String) entries.get("title");
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        if (description != null && link.getComment() == null) {
            link.setComment(description);
        }
        link.setName(title + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = (String) jd.plugins.hoster.DummyScriptEnginePlugin.walkJson(entries, "recommended_encoding/url");
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(dllink);
        final String[] qualities = br.getRegex("([^/\n]*?\\-hls\\-\\d+k\\.m3u8)").getColumn(0);
        if (qualities == null || qualities.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = br.getURL();
        /* Find the highest bitrate & download */
        String final_downloadlink = null;
        long bitrate_max = 0;
        for (final String hls_url_part : qualities) {
            final String bitrate_str = new Regex(hls_url_part, "hls\\-(\\d+)k").getMatch(0);
            final long bitrate_tmp = Long.parseLong(bitrate_str);
            if (bitrate_tmp > bitrate_max) {
                bitrate_max = bitrate_tmp;
                final_downloadlink = dllink.substring(0, dllink.lastIndexOf("/")) + "/" + hls_url_part;
            }
        }
        if (final_downloadlink == null) {
            /* This is nearly impossible */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, final_downloadlink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                if (isJDStable()) {
                    con = br2.openGetConnection(dllink);
                } else {
                    con = br2.openHeadConnection(dllink);
                }
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

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