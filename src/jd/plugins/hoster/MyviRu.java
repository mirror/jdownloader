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

import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "myvi.ru" }, urls = { "https?://(?:www\\.)?myvi\\.ru/(watch/[^/#\\?]+|[A-Za-z]{2}/flash/player/[A-Za-z0-9_\\-]+|player/embed/html/[A-Za-z0-9_\\-]+)" })
public class MyviRu extends PluginForHost {
    public MyviRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Porn_plugin
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = true;
    /* Chunkload possible but not really stable --> Disable it right away */
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              temp_unavailable  = false;
    private final String         TYPE_NORMAL       = "https?://(?:www\\.)?myvi\\.ru/watch/[^/#\\?]+";
    private final String         TYPE_PLAYER       = "https?://(?:www\\.)?myvi\\.ru/([A-Za-z]{2}/flash/player/[A-Za-z0-9_\\-]+|player/embed/html/[A-Za-z0-9_\\-]+)";

    @Override
    public String getAGBLink() {
        return "http://myvi.ru/ru/help/license.aspx";
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        temp_unavailable = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename = null;
        final String html_embed_url;
        if (downloadLink.getDownloadURL().matches(TYPE_NORMAL)) {
            /* Normal video url */
            br.getPage(downloadLink.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.br.getURL().contains("myvi.ru/coming-soon?aspxerrorpath")) {
                temp_unavailable = true;
                return AvailableStatus.TRUE;
            }
            filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (filename == null) {
                filename = new Regex(downloadLink.getDownloadURL(), "myvi\\.ru/watch/(.+)").getMatch(0);
            }
            html_embed_url = br.getRegex("(//(myvi|netvi)\\.ru/player/embed/html/[^<>\"/]+)").getMatch(0);
            if (html_embed_url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // br.getPage("http://myvi.ru" + html_embed_url);
            br.getPage(html_embed_url);
        } else {
            /*
             * Embed url --> We could simply find the normal URL and continue from there but we can save 1 request by directly accessing our
             * html_embed_url
             */
            final String video_embed_id = new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9_\\-]+)$").getMatch(0);
            html_embed_url = "http://myvi.ru/player/embed/html/" + video_embed_id;
            this.br.getPage(html_embed_url);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String videojson = this.br.getRegex("sprutoData:(\\{.+\\}),").getMatch(0);
            try {
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(videojson);
                /*
                 * This is a possible direct way to find our final downloadlink but lets prefer the API as formats of this json can differ
                 * (last time I got a .flv - API usually delivers .mp4).
                 */
                // dllink = (String) JavaScriptEngineFactory.walkJson(entries, "playlist/{0}/video/{0}/url");
                filename = (String) JavaScriptEngineFactory.walkJson(entries, "playlist/{0}/title");
            } catch (final Throwable e) {
            }
            if (filename == null) {
                filename = video_embed_id;
            }
        }
        String player = br.getRegex("createPlayer\\(\"v=([^\"]+?)\"").getMatch(0).replace("\\", "'");
        dllink = new Regex(player, "([^']+)'").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        String ext = getFileNameExtensionFromString(dllink, ".mp4");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                con = br2.openHeadConnection(dllink);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("directlink", dllink);
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
        if (temp_unavailable) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file is not available at the moment", 30 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
