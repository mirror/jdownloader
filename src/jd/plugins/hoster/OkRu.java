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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ok.ru" }, urls = { "https?://(?:www\\.|m\\.)?ok\\.ru/(?:video|videoembed)/\\d+" })
public class OkRu extends PluginForHost {
    public OkRu(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
    }

    /* DEV NOTES */
    // Porn_plugin
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume         = true;
    private static final int     free_maxchunks      = 0;
    private static final int     free_maxdownloads   = -1;
    private final String         PREFER_480P         = "PREFER_480P";
    private String               dllink              = null;
    private boolean              download_impossible = false;

    public static void prepBR(final Browser br) {
        /* Use mobile website to get http urls. */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile");
        // with jd default lang we get non english (homepage) or non russian responses (mobile)
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        br.setFollowRedirects(true);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        download_impossible = false;
        this.setBrowserExclusive();
        prepBR(this.br);
        br.getPage(downloadLink.getDownloadURL());
        /* Offline or private video */
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"mvtitle clamp __2\">([^<>\"]*?)</div").getMatch(0);
        if (filename == null) {
            filename = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        if (br.containsHTML("class=\"fierr\"")) {
            if (!downloadLink.isNameSet()) {
                downloadLink.setName(filename + ".mp4");
            }
            download_impossible = true;
            return AvailableStatus.TRUE;
        }
        dllink = br.getRegex("embedVPlayer\\(this,&#39;(https?[^<>\"]*?)&#39;,&#39;").getMatch(0);
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
        } else {
            dllink = br.getRegex("videoSrc&quot;:&quot;(https[^<>\"]*?)(&quot;)").getMatch(0);
            if (dllink != null) {
                dllink = Encoding.unicodeDecode(dllink);
            } else {
                dllink = br.getRegex("data-embedclass=\"yt_layer\" data-objid=\"\\d+\" href=\"(https?[^<>\"]*?)\"").getMatch(0);
                if (dllink != null) {
                    dllink = Encoding.htmlDecode(dllink);
                }
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String url_quality = new Regex(dllink, "(st.mq=\\d+)").getMatch(0);
        if (url_quality != null) {
            /* st.mq: 2 = 480p (mobile format), 3=?, 4=? 5 = highest */
            if (this.getPluginConfig().getBooleanProperty(PREFER_480P, false)) {
                dllink = dllink.replace(url_quality, "st.mq=2");
            } else {
                /* Prefer highest quality available */
                dllink = dllink.replace(url_quality, "st.mq=5");
            }
        }
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
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
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    public static boolean isOffline(final Browser br) throws IOException {
        // class=\"empty\" is NOT an indication for an offline video!
        if (br.containsHTML("error-page\"") || br.getHttpConnection().getResponseCode() == 404) {
            return true;
        }
        /* Offline or private video */
        if (br.containsHTML(">Access to this video has been restricted|>Access to the video has been restricted") || br.getURL().contains("/main/st.redirect/")) {
            return true;
        }
        if (br.getURL().contains("?")) {
            /* Redirect --> Offline! */
            return true;
        }
        // video blocked | video not found
        if (br.containsHTML(">Видеоролик заблокирован<|>Видеоролик не найден<")) {
            return true;
        }
        if (br.containsHTML(">Video has not been found</div") || br.containsHTML(">Video hasn't been found</div")) {
            return true;
        }
        // offline due to copyright claim
        if (br.containsHTML("<div class=\"empty\"")) {
            final String vid = new Regex(br.getURL(), "(\\d+)$").getMatch(0);
            // mobile page .... get standard browser
            final Browser br2 = new Browser();
            br2.setFollowRedirects(true);
            br2.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
            br2.getPage(br.createGetRequest("/video/" + vid));
            if (br2.containsHTML(">Video has been blocked due to author's rights infingement<|>The video is blocked<|>Group, where this video was posted, has not been found")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://ok.ru/";
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (download_impossible) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download impossible", 3 * 60 * 60 * 1000l);
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), PREFER_480P, JDL.L("plugins.hoster.OkRu.preferLow480pQuality", "Prefer download of 480p version instead of the highest video quality?")).setDefaultValue(false));
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
