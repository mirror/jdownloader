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
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wdr.de" }, urls = { "http://([a-z0-9]+\\.)?wdr\\.de/([a-z0-9\\-_/]+/sendungen/[a-z0-9\\-_/]+\\.html|tv/rockpalast/extra/videos/\\d+/\\d+/\\w+\\.jsp)" }, flags = { 0 })
public class WdrDeMediathek extends PluginForHost {

    public WdrDeMediathek(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www1.wdr.de/themen/global/impressum/impressum116.html";
    }

    private static final String TYPE_ROCKPALAST = "http://(www\\.)?wdr\\.de/tv/rockpalast/extra/videos/\\d+/\\d+/\\w+\\.jsp";
    private static final String TYPE_INVALID    = "http://([a-z0-9]+\\.)?wdr\\.de/[a-z0-9\\-_/]+/sendungen/filterseite[a-z0-9\\-_/]+\\.html";

    public void correctDownloadLink(final DownloadLink link) {
        final String player_part = new Regex(link.getDownloadURL(), "(\\-videoplayer(_size\\-[A-Z])?\\.html)").getMatch(0);
        if (player_part != null) link.setUrlDownload(link.getDownloadURL().replace(player_part, ".html"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        if (downloadLink.getDownloadURL().contains("filterseite-")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String startLink = downloadLink.getDownloadURL();
        br.getPage(startLink);

        if (br.getHttpConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        if (startLink.matches(TYPE_ROCKPALAST)) return requestRockpalastFileInformation(downloadLink);

        if (br.getURL().contains("/fehler.xml")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String sendung = br.getRegex("<strong>([^<>\"]*?)<span class=\"hidden\">:</span></strong>[\t\n\r ]+Die Sendungen im Überblick[\t\n\r ]+<span>\\[mehr\\]</span>").getMatch(0);
        if (sendung == null) sendung = br.getRegex(">Sendungen</a></li>[\t\n\r ]+<li>([^<>\"]*?)<span class=\"hover\">").getMatch(0);
        if (sendung == null) sendung = br.getRegex("<li class=\"active\" >[\t\n\r ]+<strong>([^<>\"]*?)</strong>").getMatch(0);
        String episode_name = br.getRegex("</li><li>[^<>\"/]+: ([^<>\"]*?)<span class=\"hover\"").getMatch(0);
        if (episode_name == null) episode_name = br.getRegex("class=\"hover\">:([^<>\"]*?)</span>").getMatch(0);
        if (sendung == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String preferedExt = ".mp4";
        if (br.containsHTML("<div class=\"audioContainer\">")) {
            DLLINK = br.getRegex("dslSrc: \\'dslSrc=(http://[^<>\"]*?)\\&amp;mediaDuration=\\d+\\'").getMatch(0);
            preferedExt = ".mp3";
        } else {
            String player_link = br.getRegex("class=\"videoLink\" >[\t\n\r ]+<a href=\"(/[^<>\"]*?)\"").getMatch(0);
            if (player_link == null) player_link = br.getRegex("\"(/[^<>\"]*?)\" rel=\"nofollow\" class=\"videoButton play\"").getMatch(0);
            if (player_link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage("http://www1.wdr.de" + player_link);
            /* Avoid HDS */
            final String[] qualities = br.getRegex("(CMS2010/mdb/ondemand/weltweit/fsk\\d+/[^<>\"]*?)\"").getColumn(0);
            if (qualities == null || qualities.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = "http://http-ras.wdr.de/" + qualities[0];
        }
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        DLLINK = Encoding.htmlDecode(DLLINK.trim());
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) ext = preferedExt;
        sendung = encodeUnicode(Encoding.htmlDecode(sendung).trim());
        if (episode_name != null) {
            episode_name = Encoding.htmlDecode(episode_name).trim();
            episode_name = encodeUnicode(episode_name);
            downloadLink.setFinalFileName(sendung + " - " + episode_name + ext);
        } else {
            downloadLink.setFinalFileName(sendung + ext);
        }
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    private AvailableStatus requestRockpalastFileInformation(final DownloadLink downloadlink) throws IOException, PluginException {
        String fileName = br.getRegex("<h1 class=\"wsSingleH1\">([^<]+)</h1>[\r\n]+<h2>([^<]+)<").getMatch(0);
        DLLINK = br.getRegex("dslSrc=(.*?)\\&amp").getMatch(0);
        if (fileName == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadlink.setFinalFileName(encodeUnicode(Encoding.htmlDecode(fileName).trim()) + ".mp4");
        return AvailableStatus.TRUE;
    }

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
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (DLLINK.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, DLLINK);
            setupRTMPConnection(DLLINK, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private void setupRTMPConnection(String dllink, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setUrl(dllink);
        rtmp.setResume(true);
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
