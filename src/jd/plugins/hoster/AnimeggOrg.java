//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import org.jdownloader.plugins.components.antiDDoSForHost;

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
import jd.plugins.components.PluginJSonUtils;

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "animegg.org" }, urls = { "http://(www\\.)?animegg\\.org/(?:embed/\\d+|[\\w\\-]+episode-\\d+)" })
public class AnimeggOrg extends antiDDoSForHost {

    // raztoki embed video player template.

    private String dllink = null;

    public AnimeggOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.animegg.org/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        final String link = downloadLink.getDownloadURL();
        getPage(link);
        // not yet available. We can only say offline!
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<img src=\"\\.\\./images/animegg-unavailable.jpg\" style=\"width: 100%\">")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!link.matches(".+/embed/\\d+")) {
            final String embed = br.getRegex("<iframe [^>]*src=(\"|')(.*?/embed/\\d+)\\1").getMatch(1);
            if (embed == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            getPage(embed);
        }
        final String filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\"").getMatch(0);
        // multiple qualities.
        final String vidquals = br.getRegex("videoSources\\s*=\\s*(\\[.*?\\]);").getMatch(0);
        final LinkedHashMap<Integer, String> results = new LinkedHashMap<Integer, String>();
        final String[] quals = PluginJSonUtils.getJsonResultsFromArray(vidquals);
        for (final String qual : quals) {
            final String label = PluginJSonUtils.getJsonValue(qual, "label");
            final String lapel = new Regex(label, "(\\d+)p").getMatch(0);
            final Integer p = lapel != null ? Integer.parseInt(lapel) : -1;
            final String lnk = PluginJSonUtils.getJsonValue(qual, "file");
            if (lnk != null && lapel != null) {
                results.put(p, lnk);
            }
        }
        // always grabs best.
        final Integer[] availQuals = { 1080, 720, 480, 360, 240 };
        for (final Integer aQ : availQuals) {
            if (results.containsKey(aQ)) {
                dllink = results.get(aQ);
                break;
            }
        }
        if (dllink == null || filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.urlDecode(dllink, false);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(dllink);
            // only way to check for made up links... or offline is here
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setFinalFileName(filename + ".mp4");
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}