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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "film.bild.de" }, urls = { "https?://(www\\.)?bild\\.de/video/[^<>\"]+\\.bild\\.html" })
public class FilmBildDe extends PluginForHost {
    public FilmBildDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.bild.de/corporate-site/agb/bild-de/artikel-agb-2952414.bild.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        final String linkpart = Encoding.htmlDecode(new Regex(downloadLink.getDownloadURL(), "video/([^<>\"]+\\d{5,8})").getMatch(0));
        if (linkpart == null) {
            /* Fail safe as the input RegEx is more open than this. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(downloadLink.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.containsHTML("window._videoJSON")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String title = br.getRegex("\"title\"\\s*:\\s*\"([^<>\"]*?)\"").getMatch(0);
        final String subtitle = br.getRegex("\"headline\"\\s*:\\s*\"([^<>\"]*?)\"").getMatch(0);
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = null;
        if (subtitle == null) {
            filename = Encoding.htmlDecode(title).trim();
        } else {
            filename = Encoding.htmlDecode(title).trim() + " - " + Encoding.htmlDecode(subtitle).trim();
        }
        downloadLink.setFinalFileName(filename.trim() + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("\"src\"\\s*:\\s*\"(https?://[^\"]*.mp4)\"\\s*,\\s*\"type\"\\s*:\\s*\"video/mp4\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (!looksLikeDownloadableContent(con)) {
                br.followConnection();
            } else {
                if (con.getCompleteContentLength() > 0) {
                    downloadLink.setVerifiedFileSize(con.getCompleteContentLength());
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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