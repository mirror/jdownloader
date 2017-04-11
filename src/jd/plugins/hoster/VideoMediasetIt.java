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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "video.mediaset.it", "wittytv.it" }, urls = { "https?://(?:www\\.)?video\\.mediaset\\.it/(video/.*?\\.html|player/playerIFrame\\.shtml\\?id=\\d+)", "https?://(?:www\\.)?wittytv\\.it/[^/]+/([^/]+/)?\\d+/?" })
public class VideoMediasetIt extends PluginForHost {

    public VideoMediasetIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.licensing.mediaset.it/";
    }

    private static final String  TYPE_VIDEO_MEDIASET_EMBED  = "https?://(?:www\\.)?video\\.mediaset\\.it/player/playerIFrame\\.shtml\\?id=\\d+";
    private static final String  TYPE_VIDEO_MEDIASET_NORMAL = "https?://(?:www\\.)?video\\.mediaset\\.it/video/.+";
    private static final String  TYPE_VIDEO_WITTYTV         = "https?://(?:www\\.)?wittytv\\.it/.+";
    private static final String  HTML_MS_SILVERLIGHT        = "silverlight/playerSilverlight\\.js\"";
    private static final boolean use_player_json            = true;
    private boolean              dlImpossible               = false;

    // Important info: Can only handle normal videos, NO
    // "Microsoft Silverlight forced" videos!
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        String streamID;
        if (downloadLink.getDownloadURL().matches(TYPE_VIDEO_WITTYTV)) {
            streamID = new Regex(downloadLink.getDownloadURL(), "(\\d+)/?$").getMatch(0);
        } else {
            streamID = new Regex(downloadLink.getDownloadURL(), "video\\.mediaset\\.it/video/[^<>/\"]*?/[^<>/\"]*?/(\\d+)/").getMatch(0);
            if (streamID == null) {
                streamID = new Regex(downloadLink.getDownloadURL(), "(\\d+)\\.html$").getMatch(0);
            }
        }
        if (streamID == null) {
            /* Whatever the user added it is probably not a video! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setReadTimeout(3 * 60 * 1000);
        /* 2016-06-16 TODO: Fix support for embedded URLs */
        if (downloadLink.getDownloadURL().matches(TYPE_VIDEO_MEDIASET_EMBED)) {
            br.getPage(downloadLink.getDownloadURL());
            downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0));
            if (!br.getURL().matches(TYPE_VIDEO_MEDIASET_NORMAL)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setUrlDownload(br.getURL());
            /* 2016-06-16 TODO: Fix support for embedded URLs! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        String date = null;
        String date_formatted = null;
        String filename;
        if (use_player_json) {
            this.br.getPage("http://plr.video.mediaset.it/html/metainfo.sjson?id=" + streamID);
            filename = PluginJSonUtils.getJsonValue(this.br, "title");
            date = PluginJSonUtils.getJsonValue(this.br, "production-date");
        } else {
            br.getPage(downloadLink.getDownloadURL());
            if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Il video che stai cercando non")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML(HTML_MS_SILVERLIGHT)) {
                downloadLink.getLinkStatus().setStatusText("JDownloader can't download MS Silverlight videos!");
                return AvailableStatus.TRUE;
            }
            filename = br.getRegex("content=\"([^<>]*?) \\| Video Mediaset\" name=\"title\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("class=\"title\">([^<>\"]+)<").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("<h2 class=\"titleWrap\">([^<>]*?)</h2>").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("<title>([^<>]*?)\\- Video Mediaset</title>").getMatch(0);
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename.trim());
        filename = encodeUnicode(filename);
        date_formatted = formatDate(date);
        if (date_formatted != null) {
            filename = date_formatted + "_video_mediaset_it_" + filename;
        }
        /** New way, thx to: http://userscripts.org/scripts/review/151516 */
        br.getPage("http://www.video.mediaset.it/player/js/mediaset/Configuration.js");
        final String var_CDN_SELECTOR_URL = this.br.getRegex("var CDN_SELECTOR_URL = (.*?);").getMatch(0);
        if (var_CDN_SELECTOR_URL != null && var_CDN_SELECTOR_URL.contains("mediaset.net")) {
            /* For new content */
            br.getPage(String.format("http://cdnsel01.mediaset.net/GetCdn.aspx?format=json&streamid=%s", streamID));
        } else {
            /* For older content */
            br.getPage(String.format("http://cdnselector.xuniplay.fdnames.com/GetCDN.aspx?format=json&streamid=%s", streamID));
        }
        final String videoList = br.getRegex("\"videoList\":\\[(.*?)\\]").getMatch(0);
        if (videoList == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String[] dllinks = new Regex(videoList, "\"(http://[^<>\"]*?)\"").getColumn(0);
        if (dllinks != null && dllinks.length != 0) {
            final int length = dllinks.length;
            if (length >= 3) {
                dllink = dllinks[2];
            } else if (length >= 2) {
                dllink = dllinks[1];
            } else {
                dllink = dllinks[0];
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains("Error400") || br.containsHTML("/Cartello_NotAvailable\\.wmv")) {
            downloadLink.getLinkStatus().setStatusText("JDownloader can't download this video (either blocked in your country or MS Silverlight)");
            downloadLink.setName(filename + ".mp4");
            return AvailableStatus.TRUE;
        }
        dllink = Encoding.htmlDecode(dllink);
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        downloadLink.setFinalFileName(filename + ext);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                if (con.getLongContentLength() < 200) {
                    dlImpossible = true;
                } else {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                }
            } else {
                dlImpossible = true;
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(HTML_MS_SILVERLIGHT)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "JDownloader can't download MS Silverlight videos!");
        }
        if (dllink.contains("Error400") || br.containsHTML("/Cartello_NotAvailable\\.wmv")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "JDownloader can't download this video (either blocked in your country or MS Silverlight)");
        }
        if (dlImpossible) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error, try again later", 10 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String formatDate(final String input) {
        if (input == null) {
            return null;
        }
        final long date = TimeFormatter.getMilliSeconds(input, "dd/MM/yyyy", Locale.ENGLISH);
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = input;
        }
        return formattedDate;
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
