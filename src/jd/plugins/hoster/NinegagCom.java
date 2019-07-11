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
import java.util.Map;

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
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "9gag.com" }, urls = { "https?://(?:www\\.)?9gag\\.com/gag/[a-zA-Z0-9]+" })
public class NinegagCom extends PluginForHost {
    public NinegagCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = false;
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "http://9gag.com/tos";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("?post_removed=1")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String id = new Regex(link.getDownloadURL(), "([a-zA-Z0-9]+)$").getMatch(0);
        String filename = PluginJSonUtils.getJsonValue(this.br, "title");
        final String image_src = br.getRegex("rel=\"image_src\" href=\"(https?[^<>\"]*?)\"").getMatch(0);
        String images = br.getRegex("\"post\"\\s*:\\s*\\{\\s*\"id\"\\s*:\"" + id + "\".*?\"images\"\\s*:\\s*(\\{.*?\\}\\s*\\})\\s*,").getMatch(0);
        if (images == null) {
            String jsonParse = br.getRegex("window\\._config\\s*=\\s*JSON\\.parse\\((.*?)\\)\\s*;\\s*</script").getMatch(0);
            if (jsonParse != null) {
                jsonParse = JSonStorage.restoreFromString(jsonParse, TypeRef.STRING);
                if (filename == null) {
                    filename = PluginJSonUtils.getJsonValue(jsonParse, "title");
                }
                images = new Regex(jsonParse, "\"post\"\\s*:\\s*\\{\\s*\"id\"\\s*:\"" + id + "\".*?\"images\"\\s*:\\s*(\\{.*?\\}\\s*\\})\\s*,").getMatch(0);
            }
        }
        if (filename == null) {
            filename = id;
        }
        boolean video = false;
        if (images != null) {
            final Map<String, Object> map = JSonStorage.restoreFromString(images, TypeRef.HASHMAP);
            final Map<String, Object> image460sv = (Map<String, Object>) map.get("image460sv");
            if (image460sv != null) {
                video = true;
                dllink = (String) image460sv.get("url");
            }
        }
        if (dllink == null) {
            dllink = image_src;
        }
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = getFileNameExtensionFromString(dllink, video ? ".mp4" : ".jpg");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(dllink);
            if (!con.getContentType().contains("html") && con.getResponseCode() == 200) {
                link.setDownloadSize(con.getLongContentLength());
                link.setProperty("directlink", dllink);
            } else {
                server_issues = true;
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
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            try {
                br.followConnection();
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
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
