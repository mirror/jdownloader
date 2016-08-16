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
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vid.me" }, urls = { "https://viddecrypted\\.me/[A-Za-z0-9]+" }, flags = { 0 })
public class VidMe extends PluginForHost {

    public VidMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Extension which will be used if no correct extension is found */
    public static final String   default_Extension = ".mp4";

    public static final String   API_ENDPOINT      = "https://api.vid.me/";
    private String               DLLINK            = null;

    private static final boolean api_use_api       = true;

    @Override
    public String getAGBLink() {
        return "https://vid.me/terms-of-use";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("viddecrypted.me/", "vid.me/"));
    }

    public static void api_prepBR(final Browser br) {
        br.setAllowedResponseCodes(400);
    }

    public static void website_prepBR(final Browser br) {
        br.setAllowedResponseCodes(410);
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        DLLINK = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename;
        if (api_use_api) {
            api_prepBR(this.br);
            this.br.getPage(api_get_video(downloadLink.getDownloadURL()));
            if (this.br.getHttpConnection().getResponseCode() != 200) {
                /* Typically 400 video offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
            entries = (LinkedHashMap<String, Object>) entries.get("video");
            final String state = (String) entries.get("state");
            if (!state.equals("success")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = getVideoTitle(this, entries);
            DLLINK = (String) entries.get("complete_url");
        } else {
            website_prepBR(this.br);
            br.getPage(downloadLink.getDownloadURL());
            if (!br.getHttpConnection().isOK() || !br.containsHTML("property=\"og:video\"") || br.containsHTML("class=\"note downloading\\-header\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            DLLINK = checkDirectLink(downloadLink, "directlink");
            if (DLLINK == null) {
                DLLINK = br.getRegex("property=\"og:video:url\" content=\"(http[^<>\"]*?/videos/[^<>\"]*?)\"").getMatch(0);
            }
        }
        if (filename == null || DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        /* Make sure that we get a correct extension */
        if (ext == null || !ext.matches("\\.[A-Za-z0-9]{3,5}")) {
            ext = default_Extension;
        }
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
                con = br2.openHeadConnection(DLLINK);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("directlink", DLLINK);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1 || !con.isOK()) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    public static final long api_get_max_videos_per_page() {
        return 100;
    }

    public static String api_get_video(final String url) {
        return API_ENDPOINT + "videoByUrl?url=" + Encoding.urlEncode(url);
    }

    public static String api_get_userinfo(final String user) {
        return API_ENDPOINT + "userByUsername?username=" + Encoding.urlEncode(user);
    }

    /* Limit = 100 == max */
    public static String api_get_user_videos(final String user_id, final String offset) {
        return API_ENDPOINT + "list?moderated=-1&private=0&nsfw=-1&order=date_completed&direction=DESC&limit=" + Long.toString(api_get_max_videos_per_page()) + "&offset=" + offset + "&user=" + user_id;
    }

    public static String getVideoTitle(final Plugin plugin, final LinkedHashMap<String, Object> sourcemap) {
        String title = (String) sourcemap.get("title");
        if (title != null && !"".equals(title)) {
            title = plugin.encodeUnicode(title);
        } else {
            /* Title is not always given - use video-ID then. */
            title = getVideoID(sourcemap);
        }
        return title;
    }

    public static String getVideoID(final LinkedHashMap<String, Object> sourcemap) {
        final String videoid = (String) sourcemap.get("url");
        return videoid;
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
