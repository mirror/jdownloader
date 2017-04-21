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

import java.util.ArrayList;
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
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tune.pk" }, urls = { "https?://(?:www\\.)?tune\\.pk/player/embed_player\\.php\\?vid=\\d+|https?://embed\\.tune\\.pk/play/\\d+|https?(?:www\\.)?://tune\\.pk/video/\\d+" })
public class TunePk extends PluginForHost {

    public TunePk(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_Extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;
    private boolean              server_issues     = false;
    BrowserException             e                 = null;

    @Override
    public String getAGBLink() {
        return "http://tune.pk/policy/terms";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        e = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String fid = new Regex(link.getDownloadURL(), "(\\d+)").getMatch(0);
        // br.getPage("http://embed." + this.getHost() + "/play/" + fid + "?autoplay=no&ssl=no&inline=true");
        br.getPage(link.getDownloadURL().replace("http:", "https:"));
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("class=\"gotune\"")) {
            /* E.g. Woops,<br>this video has been deactivated <a href="//tune.pk" class="gotune" target="_blank">Goto tune.pk</a> */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        /* Find highest quality */
        // final String json_sources = this.br.getRegex("_details\\.player\\.sources[\t\n\r ]*?=[\t\n\r ]*?(\\[\\{.*?\\}\\])").getMatch(0);
        final String json_sources = this.br.getRegex("sources\":(\\[\\{.*?\\}\\])").getMatch(0);
        if (json_sources == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        LinkedHashMap<String, Object> entries = null;
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(json_sources);
        String dllinktemp = null;
        long bitratetemp = 0;
        long bitratemax = 0;
        for (final Object qualityo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) qualityo;
            dllinktemp = (String) entries.get("file");
            bitratetemp = JavaScriptEngineFactory.toLong(entries.get("bitrate"), 0);
            if (bitratetemp > bitratemax && dllinktemp != null && !dllinktemp.equals("")) {
                bitratemax = bitratetemp;
                dllink = dllinktemp;
            }
        }

        String filename = br.getRegex("details\\.video\\.title[\t\n\r ]*?=[\t\n\r ]*?\\'([^<>\"\\']+)\\';").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]+) \\| Tune\\.pk</title>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("itemprop=\"name\">([^<>\"]*?)<").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        }
        if (filename == null) {
            filename = fid;
        }
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        String ext = PluginJSonUtils.getJsonValue(json_sources, "type");
        if (dllink != null && ext == null) {
            ext = getFileNameExtensionFromString(dllink, default_Extension);
        }
        /* Make sure that we get a correct extension */
        if (ext == null || !ext.matches("\\.[A-Za-z0-9]{3,5}")) {
            ext = default_Extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (dllink != null) {
            link.setFinalFileName(filename);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                try {
                    con = br2.openHeadConnection(dllink);
                } catch (final BrowserException ebr) {
                    this.e = ebr;
                }
                if (this.e == null) {
                    if (!con.getContentType().contains("html")) {
                        link.setDownloadSize(con.getLongContentLength());
                        link.setProperty("directlink", dllink);
                    } else {
                        server_issues = true;
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (this.e != null) {
            throw this.e;
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
