//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "viva.tv", "funnyclips.cc", "comedycentral.tv", "nick.de", "mtv.de" }, urls = { "http://www\\.viva\\.tv/.+", "http://de\\.funnyclips\\.cc/.+", "http://www\\.comedycentral\\.tv/.+", "http://www\\.nick.de/.+", "http://www\\.mtv\\.de/.+" }, flags = { 0, 0, 0, 0, 0 })
public class VivaTv extends PluginForHost {

    public VivaTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.viva.tv/agb";
    }

    private static final String type_viva_invalid  = "https?://(www\\.)?viva\\.tv/musik\\?page=.+";
    private static final String type_viva          = "http://www\\.viva\\.tv/.+";
    private static final String type_funnyclips    = "http://de\\.funnyclips\\.cc/.+";
    private static final String type_comedycentral = "http://www\\.comedycentral\\.tv/.+";
    private static final String type_nick          = "http://www\\.nick.de/.+";
    private static final String type_mtv           = "http://mtv\\.de/.+";

    private static final String player             = "http://player.mtvnn.com/g2/g2player_2.2.1.swf";

    /** TODO: Maybe add more domains which the same backend in case there are more... */
    /** Tags: Viacom International Media Networks Northern Europe, mrss, gameone.de */

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        String filename = null;
        String ext;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (link.getDownloadURL().matches(type_viva)) {
            if (link.getDownloadURL().matches(type_viva_invalid)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("class=\\'player\\'") || !br.containsHTML("PLISTA\\.items\\.push\\(") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>([^<>]*?) \\- Musikvideo \\- VIVA\\.tv</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("title: \\'([^<>]*?)\\',").getMatch(0);
            }
            ext = ".mp4";
        } else if (link.getDownloadURL().matches(type_funnyclips)) {
            try {
                br.getPage(link.getDownloadURL());
            } catch (final BrowserException e) {
                if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 500) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                throw e;
            }
            if (!br.containsHTML("class=\\'player\\'>") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String show = br.getRegex("class=\\'franchise_title\\'>([^<>\"]*?)<").getMatch(0);
            String title = br.getRegex("<h2 class=\\'title\\'>([^<>\"]*?)</h2>").getMatch(0);
            if (show == null || title == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            show = Encoding.htmlDecode(show).trim();
            title = Encoding.htmlDecode(title).trim();
            filename = show + " - " + title;
            ext = ".mp4";
        } else if (link.getDownloadURL().matches(type_comedycentral)) {
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("swfobject\\.createCSS") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<h1 class=\\'title\\'>([^<>]*?)</h1>").getMatch(0);
            ext = ".mp4";
        } else if (link.getDownloadURL().matches(type_nick)) {
            String vid = new Regex(link.getDownloadURL(), "/videos/(\\d+)").getMatch(0);
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("swfobject\\.createCSS") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (vid == null) {
                vid = br.getRegex("data\\-key=\\'local_playlist\\-(\\d+)'").getMatch(0);
            }
            String show = br.getRegex("data\\-name=\\'([^<>\"]*?)\\'").getMatch(0);
            if (show == null) {
                /* Assuming we're on a playlist */
                show = br.getRegex("<h2 class=(?:\\'|\")row\\-title videos(?:\\'|\")>([^<>]*?) Videos[\t\n\r ]+</h2>").getMatch(0);
            }
            if (show == null) {
                /* Assuming we're on a playlist */
                show = br.getRegex("<h2 class=(?:\\'|\")row\\-title videos(?:\\'|\")>([^<>]*?) Videos[\t\n\r ]+</h2>").getMatch(0);
            }
            if (show == null && vid != null) {
                show = br.getRegex("data\\-item\\-id=\\'" + vid + "\\'>.*?class=\\'title\\'>([^<>]*?)</p>").getMatch(0);
            }
            String title = br.getRegex("playlist\\-\\d+\\' data\\-title=(?:\\'|\")([^<>\"]*?)(?:\\'|\")").getMatch(0);
            if (show == null || title == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            show = Encoding.htmlDecode(show).trim();
            title = Encoding.htmlDecode(title).trim();
            filename = show + " - " + title;
            ext = ".flv";
        } else {
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("property=\"og:video\"") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<h1 class=\"page\\-title\">([^<>]*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>([^<>]*?)\\- Shows \\- MTV</title>").getMatch(0);
            }
            ext = ".flv";
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ext);
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String pageurl = br.getURL();
        String api_url = null;
        if (downloadLink.getDownloadURL().matches(type_viva) || downloadLink.getDownloadURL().matches(type_funnyclips) || downloadLink.getDownloadURL().matches(type_comedycentral) || downloadLink.getDownloadURL().matches(type_nick)) {
            api_url = br.getRegex("mrss[\t\n\r ]+:[\t\n\r ]+(?:\\'|\")(https?://[^<>\"]*?)(?:\\'|\"),").getMatch(0);
        } else {
            api_url = br.getRegex("\\&mrss=(https?://api\\.mtvnn\\.com/[^<>\"]*?)\"").getMatch(0);
        }
        if (api_url == null) {
            api_url = br.getRegex("(https?://api\\.mtvnn\\.com/v2/mrss\\.xml\\?uri=[^<>\"\\'/]+)").getMatch(0);
        }
        if (api_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Referer", player);
        br.getPage(api_url);
        String mediagen = br.getRegex("\\'(https?://[^<>\"]*?mediaGen\\.jhtml[^<>\"]*?)\\'").getMatch(0);
        if (mediagen == null) {
            mediagen = br.getRegex("\\'(https?://[^<>\"]*?/mediagen/[^<>\"/]*?)\\'").getMatch(0);
        }
        if (mediagen == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Referer", player);
        br.getPage(mediagen);
        /* Video temporarily or forever offline */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 60 * 1000l);
        }
        if (br.containsHTML("status=\"esiblocked\"")) {
            /* Geo block */
            throw new PluginException(LinkStatus.ERROR_FATAL, "This video is not available from your location");
        }
        /* Chose highest quality available */
        final String[] srcs = br.getRegex("(://[^<>\"]*?\\.(mp4|flv))</src>").getColumn(0);
        if (srcs == null || srcs.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String rtmpurl = "rtmp" + srcs[srcs.length - 1];
        final String ext = rtmpurl.substring(rtmpurl.lastIndexOf(".") + 1);
        final String app = new Regex(rtmpurl, "(ondemand/(?:(mtviestor|riptide)/)?)").getMatch(0);
        final Regex host_app = new Regex(rtmpurl, "(rtmp://[^/]*?/)ondemand/(.+)");
        String rtmphost = host_app.getMatch(0);
        String playpath = new Regex(rtmpurl, app + "(.+)").getMatch(0);
        if (rtmphost == null || playpath == null || app == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        rtmphost += app;
        playpath = ext + ":" + playpath;
        /* Small fix for wrong rtmp urls */
        playpath = playpath.replace("_od_flv.flv", "_od_flv");
        try {
            dl = new RTMPDownload(this, downloadLink, rtmphost);
        } catch (final NoClassDefFoundError e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
        }
        /* Setup rtmp connection */
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPageUrl(pageurl);
        rtmp.setUrl(rtmpurl);
        rtmp.setApp(app);
        rtmp.setPlayPath(playpath);
        /* Make sure we're using the correct protocol! */
        rtmp.setProtocol(0);
        rtmp.setFlashVer("WIN 16,0,0,305");
        rtmp.setSwfVfy(player);
        /* Our rtmp resuming isn't the best plus we got a lot of different servers so better disable resume to prevent errors. */
        rtmp.setResume(false);
        ((RTMPDownload) dl).startDownload();
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}