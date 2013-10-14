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

import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

import org.w3c.dom.Document;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "video.cnnturk.com" }, urls = { "http://(www\\.)?video\\.cnnturk\\.com/\\d+/\\w+/\\d+/\\d+/[a-z0-9\\-]+" }, flags = { 0 })
public class VideoCnnturkCom extends PluginForHost {

    private String   DLLINK = null;

    private Document doc;

    public VideoCnnturkCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            if ("/".equals(br.getRedirectLocation()) || "http://video.cnnturk.com/".equals(br.getRedirectLocation())) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        }

        final String xmlUrl = br.getRegex("\"FlashPlayerConfigUrl\": \"(http://[^<>\"]*?)\"").getMatch(0);
        String filename = null, server = null, playPath = null;
        if (xmlUrl != null) {
            final XPath xPath = xmlParser(Encoding.htmlDecode(xmlUrl));
            try {
                filename = xPath.evaluate("/VideoRoot/Video/Title", doc);
                server = xPath.evaluate("/VideoRoot/Video/Server", doc);
                playPath = xPath.evaluate("/VideoRoot/Video/Url", doc);
            } catch (final Throwable e) {
            }
            if (filename == null || server == null || playPath == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            filename = Encoding.htmlDecode(filename.trim()) + ".mp4";
            DLLINK = server + "@" + playPath;
        } else {
            filename = br.getRegex("<title>([^<>]*?) CNN TÜRK Video([\t\n\r ]+)?</title>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            filename = Encoding.htmlDecode(filename.trim()).replace("\"", "'");
            final Regex urlinfo = new Regex(downloadLink.getDownloadURL(), "video\\.cnnturk\\.com/(\\d+)/(\\w+)/(\\d+)/(\\d+)/([a-z0-9\\-]+)");
            final String playerURL = "http://video.cnnturk.com/actions/Video/NetDVideoPlayer?year=" + urlinfo.getMatch(0) + "&category=" + urlinfo.getMatch(1) + "&month=" + urlinfo.getMatch(2) + "&day=" + urlinfo.getMatch(3) + "&name=" + urlinfo.getMatch(4) + "&height=360";
            br.getPage(playerURL);
            DLLINK = br.getRegex("path: \\'([^<>\"]*?)\\'").getMatch(0);
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (DLLINK.contains(".mp4")) {
                filename = filename + ".mp4";
            } else {
                filename = filename + ".m3u";
                downloadLink.getLinkStatus().setStatusText("Download impossible: Cannot download segmented streams");
                SEGMENTSTREAM = true;
            }
        }

        downloadLink.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    public void download(final DownloadLink downloadLink) throws Exception {
        if (DLLINK.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, DLLINK.split("@")[0] + DLLINK.split("@")[1]);
            setupRTMPConnection(dl);
            ((RTMPDownload) dl).startDownload();

        } else if (DLLINK.startsWith("http")) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://video.cnnturk.com/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private boolean SEGMENTSTREAM = false;

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (SEGMENTSTREAM) throw new PluginException(LinkStatus.ERROR_FATAL, "Download impossible: Cannot download segmented streams");
        download(downloadLink);
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

    private void setupRTMPConnection(final DownloadInterface dl) {
        final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

        rtmp.setPlayPath(DLLINK.split("@")[1]);
        rtmp.setApp(DLLINK.split("@")[0].replaceAll("\\w+://[\\w\\.]+/", ""));
        rtmp.setUrl(DLLINK.split("@")[0]);
        rtmp.setSwfVfy("http://video.cnnturk.com/content/playermed/mplayer.v14.swf");
        rtmp.setResume(true);
        rtmp.setTimeOut(1);
    }

    private XPath xmlParser(final String linkurl) throws Exception {
        try {
            final URL url = new URL(linkurl);
            final InputStream stream = url.openStream();
            final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final XPath xPath = XPathFactory.newInstance().newXPath();
            try {
                doc = parser.parse(stream);
                return xPath;
            } finally {
                try {
                    stream.close();
                } catch (final Throwable e) {
                }
            }
        } catch (final Throwable e2) {
            return null;
        }
    }

}
