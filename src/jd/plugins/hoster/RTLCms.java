//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rtl.de", "vox.de", "frauenzimmer.de", "vip.de", "wetter.de", "sport.de", "kochbar.de" }, urls = { "https?://(?:[a-z0-9]+\\.)?rtl\\.de/[a-z0-9\\-/]+\\.html", "https?://(?:www\\.)?vox\\.de/[a-z0-9\\-/]+\\.html", "https?://(?:www\\.)?frauenzimmer\\.de/[a-z0-9\\-/]+\\.html", "https?://(?:www\\.)?vip\\.de/[a-z0-9\\-/]+\\.html", "https?://(?:www\\.)?wetter\\.de/[a-z0-9\\-/]+\\.html", "https?://(?:www\\.)?sport\\.de/.+", "https?://(?:www\\.)?kochbar\\.de/[a-z0-9\\-/]+\\.html" })
public class RTLCms extends PluginForHost {

    public RTLCms(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Extension which will be used if no correct extension is found */
    /* 2016-05-13, psp: #varoufake #verafake */
    /*
     * 2016-05-13, psp: TODO: Consider adding a decrypter to parse all "videoinfo" json's of rtlcms pages - example:
     * http://www.rtl.de/cms/videos.html
     */

    /* Connection stuff */
    private static final boolean free_http_resume    = true;
    private static final int     free_http_maxchunks = 1;
    private static final int     free_maxdownloads   = 1;

    private String               DLLINK              = null;

    @Override
    public String getAGBLink() {
        return "http://www.rtl.de/cms/service/service_navigation//nutzungsbedingungen.html";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        br.getRequest().setHtmlCode(br.toString().replace("playerlayer.min.js", ""));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String json = br.getRegex("data\\-player\\-layer\\-cfg=\\'(.*?)\\'>").getMatch(0);
        if (json == null) {
            /* E.g. http://www.vip.de/videos/vip-videos/georgina-fleur-wird-von-der-polizei-gesucht-431407.html */
            json = br.getRegex("var videoinfo = \\{(.*?)\\}").getMatch(0);
        }
        if (json == null) {
            /* E.g. http://www.sport.de/video/en122498/laureus-lauda-macht-mich-schon-froh/ */
            json = br.getRegex("\"videoinfo\"[\t\n\r ]*?:[\t\n\r ]*?\\{(.*?)\\}").getMatch(0);
        }
        if (json == null) {
            String player_url = "http://www." + downloadLink.getHost() + "/video/playerlayer/show/format/html/";
            if (downloadLink.getHost().equals("kochbar.de")) {
                final String vidid = br.getRegex("kbvideo:(\\d+)").getMatch(0);
                final String playlist = br.getRegex("playlist:\\'([^<>\"]*?)\\'").getMatch(0);
                if (vidid == null || playlist == null) {
                    logger.info("Probably not a video --> Offline");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                player_url = "http://www.kochbar.de/video/playerlayer/show/format/html/contensobject/0/kbvideo/" + vidid + "/start/00:00:00:00/farbwelt//playlist/" + playlist + "/modul/";
            } else {
                final String playerconfig = br.getRegex("onclick=\"Playerlayer\\.open\\(\\{([^<>\"]*?)\\}").getMatch(0);
                if (playerconfig == null) {
                    logger.info("Probably not a video --> Offline");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                logger.info("Found playerconfig, accessing player");
                int counter = 0;
                final String[][] values = new Regex(playerconfig, "([a-z0-9]+):(?:[\t\n\r ]+)?(?:\\')?([a-z0-9:]+)(?:\\')?").getMatches();
                for (final String[] value : values) {
                    final String param = value[0];
                    final String data = value[1];
                    if (counter == 1 && true) {
                    }
                    player_url += param + "/" + data;
                    if (counter != values.length - 1) {
                        player_url += "/";
                    }
                    counter++;
                }
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage(player_url);
            json = br.toString();
        }
        String headline1 = PluginJSonUtils.getJsonValue(json, "headline1");
        String headline2 = PluginJSonUtils.getJsonValue(json, "headline2");
        DLLINK = PluginJSonUtils.getJsonValue(json, "mp4url");
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (headline1 != null && (headline1.equals("null") || headline1.equals(""))) {
            headline1 = null;
        }
        if (headline2 != null && (headline2.equals("null") || headline2.equals(""))) {
            headline2 = null;
        }

        DLLINK = Encoding.htmlDecode(DLLINK);
        String filename;
        /* Verify our json data ... */
        if (headline1 != null && headline2 != null) {
            filename = Encoding.htmlDecode(headline1).trim() + " - " + Encoding.htmlDecode(headline2).trim();
        } else if (headline2 != null) {
            filename = Encoding.htmlDecode(headline2).trim();
        } else if (headline1 != null) {
            filename = Encoding.htmlDecode(headline1).trim();
        } else {
            /* Last chance - fallback to url-filename */
            filename = new Regex(downloadLink.getDownloadURL(), "https?://[^/]+\\.de/(.+)").getMatch(0);
            /* Remove nonsense */
            filename = filename.replace("cms/", "");
            filename = filename.replace(".html", "");
        }
        filename = encodeUnicode(filename);
        final String ext = getFileNameExtensionFromString(DLLINK, ".mp4");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        final Browser br2 = br.cloneBrowser();
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, free_http_resume, free_http_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
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
