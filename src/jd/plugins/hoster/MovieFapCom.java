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

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "moviefap.com" }, urls = { "https?://(?:www\\.)?moviefap\\.com/(videos/[a-z0-9]+/[a-z0-9\\-_]+\\.html|embedding_player/embedding_feed\\.php\\?viewkey=[a-z0-9]+)" })
public class MovieFapCom extends PluginForHost {
    public MovieFapCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.moviefap.com/dmca.php";
    }

    private static final String EMBEDLINK    = "http://(www\\.)?moviefap\\.com/embedding_player/embedding_feed\\.php\\?viewkey=[a-z0-9]+";
    private boolean             privatevideo = false;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        dllink = null;
        privatevideo = false;
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        final String url_filename = new Regex(link.getDownloadURL(), "([a-z0-9\\-_]+)(?:\\.html)?$").getMatch(0);
        String filename = null;
        if (link.getDownloadURL().matches(EMBEDLINK)) {
            filename = new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
            dllink = br.getRegex("<file>(http://[^<>\"]*?)</file>").getMatch(0);
        } else {
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("video does not exist")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML("This video is set to private")) {
                privatevideo = true;
            }
            filename = br.getRegex("<div id=\"view_title\"><h1>([^<>\"]*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("id=\"title\" name=\"title\" value=\"([^<>\"]*?)\"").getMatch(0);
            }
            String config = br.getRegex("flashvars\\.config = escape\\(\"(https?://[^<>\"]*?)\"\\);").getMatch(0);
            if (config == null) {
                /* 2020-01-07 */
                config = br.getRegex("id=\"config1\" name=\"config1\" value=\"(http[^<>\"]+)\"").getMatch(0);
            }
            if (!privatevideo && config != null) {
                br.getPage(config);
                /* Video offline - not playable via browser either! */
                if (br.toString().length() < 30) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final String[] vps = { "720p", "360p", "240p" }; // Vertical pixel
                for (final String vp : vps) {
                    dllink = br.getRegex("<res>" + vp + "</res>\\s*<videoLink>((https?:)?//[^<>\"]*?)</videoLink>").getMatch(0);
                    if (dllink != null) {
                        // dllink = Encoding.htmlDecode(dllink);
                        break;
                    }
                }
                if (dllink == null) {
                    /* 2020-01-18: Old content which will still require flashplayer via browser */
                    dllink = br.getRegex("<videoLink>((?:https?:)?//[^<>\"]*?)</videoLink>").getMatch(0);
                }
            }
        }
        if (filename == null) {
            filename = url_filename;
        }
        filename = filename.trim();
        filename = Encoding.htmlDecode(filename);
        String ext = null;
        if (dllink != null) {
            ext = getFileNameExtensionFromString(dllink, ".mp4");
        }
        if (ext == null || ext.length() > 5) {
            ext = ".flv";
        }
        ext = ext.replace(".fid", ".flv"); // if (ext == ".fid") doesn't work?
        link.setFinalFileName(filename + ext);
        if (dllink != null) {
            if (Encoding.isHtmlEntityCoded(dllink)) {
                dllink = Encoding.htmlDecode(dllink);
            }
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        if (privatevideo) {
            /* Account only */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
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
