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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vivatube.com" }, urls = { "http://(?:[a-z0-9]+\\.)?vivatube\\.com/(?:[a-z]{2}/)?video/\\d+/[A-Za-z0-9\\-_]+" })
public class VivatubeCom extends PluginForHost {

    public VivatubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    private static final String  default_extension = ".mp4";

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "http://de.vivatube.com/static/terms";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        server_issues = false;

        this.setBrowserExclusive();
        br.setFollowRedirects(true);

        final String linkid = new Regex(link.getDownloadURL(), "/video/(\\d+)").getMatch(0);
        link.setLinkID(linkid);

        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("class=\"notifications__item notifications__item\\-error\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String videoid = this.br.getRegex("var\\s*?video_id\\s*?=\\s*?\"(\\d+)\"").getMatch(0);
        if (videoid == null) {
            videoid = this.br.getRegex("vid\\s*?:\\s*?(\\d+),").getMatch(0);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "/video/\\d+/(.+)").getMatch(0).replace("-", " ");
        String filename = null;
        if (filename == null) {
            filename = url_filename;
        }
        dllink = br.getRegex("\\'(?:file|video)\\'[\t\n\r ]*?:[\t\n\r ]*?\\'(http[^<>\"]*?)\\'").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<source src=\"(https?://[^<>\"]*?)\" type=(?:\"|\\')video/(?:mp4|flv)(?:\"|\\')").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("property=\"og:video\" content=\"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null && videoid != null) {
            this.br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            this.br.getPage("http://www." + this.getHost() + "/player_config_json/?vid=" + videoid + "&aid=0&domain_id=0&embed=0&ref=&check_speed=0");
            dllink = PluginJSonUtils.getJsonValue(this.br, "hq");
            if (dllink == null || dllink.equals("")) {
                dllink = PluginJSonUtils.getJsonValue(this.br, "lq");
            }
        }
        if (dllink == null || dllink.equals("")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext;
        if (dllink != null && !dllink.equals("")) {
            ext = getFileNameExtensionFromString(dllink, default_extension);
        } else {
            ext = default_extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        if (dllink != null && !dllink.equals("")) {
            URLConnectionAdapter con = null;
            try {
                /* Do NOT use Head-Request here!! */
                con = br.openHeadConnection(dllink);
                if (con.getResponseCode() == 404) {
                    /*
                     * Small workaround for buggy servers that redirect and fail if the Referer is wrong then. Examples: hdzog.com
                     */
                    final String redirect_url = con.getRequest().getUrl();
                    con = br.openHeadConnection(redirect_url);
                }
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    link.setProperty("directlink", dllink);
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
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
