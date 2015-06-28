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
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.TimeFormatter;

/*Similar websites: bca-onlive.de, asscompact.de*/
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "asscompact.de" }, urls = { "https?://(www\\.)?asscompact\\.de/.+" }, flags = { 0 })
public class AsscompactDe extends PluginForHost {

    public AsscompactDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.asscompact.de/node/95";
    }

    /* Decrypter */
    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> getDownloadLinks(String data, FilePackage fp) {
        ArrayList<DownloadLink> ret = super.getDownloadLinks(data, fp);
        try {
            if (ret != null && ret.size() > 0) {
                /*
                 * we make sure only one result is in ret, thats the case for svn/next major version
                 */
                final DownloadLink link = ret.get(0);
                br.getPage(link.getDownloadURL());
                /* Look for external urls */
                final String externID = br.getRegex("\"(https?://player\\.vimeo\\.com/video/\\d+)\"").getMatch(0);
                if (externID != null) {
                    ret.clear();
                    final DownloadLink fina = new DownloadLink(this, null, getHost(), externID, true);
                    ret.add(fina);
                }
            }
        } catch (final Throwable e) {
            logger.severe(e.getMessage());
        }
        return ret;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final URLConnectionAdapter con = this.br.openGetConnection(link.getDownloadURL());
        if (!con.getContentType().contains("html")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.br.followConnection();
        con.disconnect();
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.containsHTML("class=\"artikelTeaser\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String date = br.getRegex("class=\"showDate\">([^<>\"]*?)<").getMatch(0);
        String filename = br.getRegex("id=\"page\\-title\">([^<>]*?)<").getMatch(0);
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
        rtmp.setFlashVer("WIN 18,0,0,194");
        rtmp.setApp(rtmp_app);
        rtmp.setUrl(url_rtmp);
        rtmp.setResume(true);
        dl.startDownload();
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

    private String formatDate(final String input) {
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