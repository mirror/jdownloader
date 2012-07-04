//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rutube.ru" }, urls = { "http://(www\\.)?(rutube\\.ru/tracks/\\d+\\.html|(video\\.rutube\\.ru/|rutube\\.ru/video/)[a-z0-9]{32})" }, flags = { PluginWrapper.DEBUG_ONLY })
public class RuTubeRu extends PluginForHost {

    public RuTubeRu(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://rutube.ru/agreement.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String linkurl = br.getRegex("player\\.swf\\?buffer_first=1\\.0\\&file=(.*?)\\&xurl").getMatch(0);
        if (linkurl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        linkurl = Encoding.urlDecode(linkurl, true);
        linkurl = linkurl.replaceFirst(linkurl.substring(linkurl.lastIndexOf(".") + 1, linkurl.length()), "f4m");

        br.getPage(linkurl);
        String baseUrl = br.getRegex("<baseURL>(.*?)</baseURL>").getMatch(0);
        String mediaUrl = br.getRegex("<media url=\"(/rutube[^<>\"]*?)\"").getMatch(0);

        if (baseUrl == null || mediaUrl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        mediaUrl = Encoding.htmlDecode(mediaUrl);

        if (baseUrl.startsWith("rtmp:")) {
            try {
                dl = new RTMPDownload(this, downloadLink, baseUrl + mediaUrl);
                final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

                final String playpath = mediaUrl.substring(mediaUrl.lastIndexOf("mp4:"));
                final String app = mediaUrl.replace(playpath, "").replace("/", "");
                if (playpath == null || baseUrl == null || app == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                baseUrl = baseUrl.endsWith("ru") ? baseUrl + ":1935" : baseUrl;

                if (app.equals("vod/")) {
                    rtmp.setLive(true);
                } else {
                    rtmp.setResume(true);
                }
                rtmp.setPlayPath(playpath);
                rtmp.setApp(app);
                rtmp.setUrl(baseUrl + "/" + app);
                rtmp.setSwfUrl("http://rutube.ru/player.swf");

                ((RTMPDownload) dl).startDownload();
            } catch (final Throwable e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, e.getMessage());
            }
        } else {
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        // embeded links (md5 hash)
        if (downloadLink.getDownloadURL().matches("http://[\\w\\.]*?rutube\\.ru/[a-z0-9]{32}")) {
            if (br.getRedirectLocation() != null) {
                final String redirect = Encoding.urlDecode(new Regex(br.getRedirectLocation(), "(.*)").getMatch(0), false);
                final String xml = new Regex(redirect, "xurl=(http[^\\&]+)").getMatch(0);
                if (xml == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                br.getPage(xml);
                String trackURL = br.getRegex("<location_url>(https?://([\\w\\.]+)?rutube\\.ru/tracks/\\d+\\.html)").getMatch(0);
                if (trackURL == null) {
                    trackURL = br.getRegex("<track_url>(https?://([\\w\\.]+)?rutube\\.ru/tracks/\\d+\\.html)").getMatch(0);
                }
                if (trackURL == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                br.getPage(trackURL);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (br.getRedirectLocation() != null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String fsk18 = br.getRegex("<p><b>.*?18.*?href=\"(http://rutube.ru/.*?confirm=.*?)\"").getMatch(0);
        if (fsk18 != null) {
            br.getPage(fsk18);
        }
        br.setFollowRedirects(true);
        String filename = br.getRegex("class=\"trackTitleLnk\">([^<>\"]*?)</a></h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("type=\"hidden\" name=\"subject\" value=\"([^<>\"]*?)\"").getMatch(0);
        }
        final String filesize = br.getRegex("<span class=\"icn\\-size\"[^>]*>(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        filename = filename.replaceFirst("::", "-");
        filename = filename.replace("::", "");
        downloadLink.setName(Encoding.htmlDecode(filename.trim()) + ".flv");
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}