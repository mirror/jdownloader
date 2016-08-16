//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.TimeFormatter;

/*Similar websites: bca-onlive.de, asscompact.de*/
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "asscompact.de" }, urls = { "https?://(www\\.)?asscompactdecrypted\\.de/.+" }, flags = { 0 })
public class AsscompactDe extends PluginForHost {

    public AsscompactDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.asscompact.de/node/95";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("asscompactdecrypted.de/", "asscompact.de/"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (isOffline(this.br, link.getDownloadURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String date = br.getRegex("class=\"showDate\">([^<>\"]*?)<").getMatch(0);
        String filename = getFilename(this, this.br, link.getDownloadURL());
        if (filename == null) {
            filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null || date == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        date = date.trim();
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);

        final String date_formatted = formatDate(date);

        filename = date_formatted + "_asscompact_" + filename + ".mp4";
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    public static boolean isOffline(final Browser br, final String url_source) throws IOException {
        final URLConnectionAdapter con = br.openGetConnection(url_source);
        if (!con.getContentType().contains("html")) {
            return true;
        }
        br.followConnection();
        con.disconnect();
        if (br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML("class=\"artikelTeaser\"")) {
            return true;
        }
        return false;
    }

    public static String getFilename(final Plugin plugin, final Browser br, final String url_source) {
        String date = br.getRegex("class=\"showDate\">([^<>\"]*?)<").getMatch(0);
        String filename = br.getRegex("id=\"page\\-title\">([^<>]*?)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null || date == null) {
            filename = new Regex(url_source, "asscompact\\.de/(.+)").getMatch(0);
        }
        date = date.trim();
        filename = Encoding.htmlDecode(filename).trim();
        filename = plugin.encodeUnicode(filename);

        final String date_formatted = formatDate(date);

        filename = date_formatted + "_asscompact_" + filename + ".mp4";
        return filename;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String url_iframe = br.getRegex("\"(https?://(?:www\\.)?assfocus\\.de/mediathek/iframe\\-tiny/[^<>\"]*?)\"").getMatch(0);
        if (url_iframe == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.getPage(Encoding.htmlDecode(url_iframe));
        final String url_xml = br.getRegex("\"(typo3temp/[^<>\"]+\\.xml)\"").getMatch(0);
        if (url_xml == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.getPage("/" + url_xml);
        final String rtmp_fid = br.getRegex("<id>(\\d+)</id>").getMatch(0);
        String url_playpath = br.getRegex("<archive_video>(fileadmin/[^<>\"]*?)</archive_video>").getMatch(0);
        if (url_playpath == null || rtmp_fid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        final String rtmp_app = "ondemand?cast_id=" + rtmp_fid;
        url_playpath = "mp4:asscompact_" + url_playpath;
        final String url_rtmp = "rtmp://adiacom.custom.solutionpark.tv/" + rtmp_app;
        dl = new RTMPDownload(this, downloadLink, url_rtmp);
        final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

        rtmp.setPlayPath(url_playpath);
        rtmp.setPageUrl(url_iframe);
        rtmp.setSwfVfy("http://www.assfocus.de/[[IMPORT]]/players.edgesuite.net/flash/plugins/osmf/advanced-streaming-plugin/v2.5/osmf1.6/AkamaiAdvancedStreamingPlugin.swf");
        rtmp.setFlashVer("WIN 18,0,0,232");
        rtmp.setApp(rtmp_app);
        rtmp.setUrl(url_rtmp);
        rtmp.setResume(true);
        dl.startDownload();
    }

    public static String formatDate(final String input) {
        final long date = TimeFormatter.getMilliSeconds(input, "dd. MMMM yyyy", Locale.GERMAN);
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
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}