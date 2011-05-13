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

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.zip.CRC32;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.network.rtmp.url.RtmpUrlConnection;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.w3c.dom.Document;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rtl-now.rtl.de", "voxnow.de", "superrtlnow.de" }, urls = { "http://(www\\.)?rtl-now\\.rtl\\.de/\\w+\\.php\\?(container_id=.+|player=.+|film_id=.+)", "http://(www\\.)?voxnow\\.de/\\w+\\.php\\?(container_id=.+|player=.+|film_id=.+)", "http://(www\\.)?superrtlnow\\.de/\\w+\\.php\\?(container_id=.+|player=.+|film_id=.+)" }, flags = { PluginWrapper.DEBUG_ONLY, PluginWrapper.DEBUG_ONLY, PluginWrapper.DEBUG_ONLY })
public class RTLnowDe extends PluginForHost {

    public RTLnowDe(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(5000l);
    }

    private long crc32Hash(final String wahl) throws UnsupportedEncodingException {
        String a = Long.toString(System.currentTimeMillis()) + Double.toString(Math.random());
        if (wahl == "session") {
            a = Long.toString(System.currentTimeMillis()) + Double.toString(Math.random()) + Long.toString(Runtime.getRuntime().totalMemory());
        }
        final CRC32 c = new CRC32();
        c.update(a.getBytes("UTF-8"));
        return c.getValue();
    }

    @Override
    public String getAGBLink() {
        return "http://rtl-now.rtl.de/nutzungsbedingungen";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String linkurl = br.getRegex("data:\"(.*?)\"").getMatch(0);
        final String ivw = br.getRegex("ivw:'/(.*?)',").getMatch(0);
        final String client = br.getRegex("id:\"(.*?)\"").getMatch(0);
        final String swfurl = br.getRegex("swfobject.embedSWF\\(\"(.*?)\",").getMatch(0);
        if (linkurl == null || ivw == null || client == null || swfurl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        linkurl = Encoding.urlDecode("http://" + br.getHost() + linkurl, true);
        final URL url = new URL(linkurl + "&ts=" + System.currentTimeMillis() / 1000);

        final InputStream stream = url.openStream();
        final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document doc = parser.parse(stream);
        final XPath xPath = XPathFactory.newInstance().newXPath();

        final String query = "/data/playlist/videoinfo";

        final String dllink = xPath.evaluate(query + "/filename", doc).replace("rtmpe:", "rtmp:");
        final String fkcont = xPath.evaluate("/data/fkcontent", doc);
        final String timetp = xPath.evaluate("/data/timetype", doc);
        final String season = xPath.evaluate("/data/season", doc);

        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        if (dllink.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, dllink);
            final RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            String playpath = new Regex(dllink, "(rtlnow|voxnow|superrtlnow)/(.*?)$").getMatch(1);
            String host = new Regex(dllink, "(.*?)(rtl.de/|rtl.de:1935/)").getMatch(-1);
            final String app = dllink.replace(playpath, "").replace(host, "");
            if (playpath == null || host == null || app == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            if (host.endsWith("de/")) {
                host = host.replace("de/", "de:1935/");
            }

            String play = playpath.substring(0, playpath.lastIndexOf("."));
            if (dllink.endsWith(".f4v")) {
                play = "mp4:" + playpath;
            }
            playpath = play + "?ivw=" + ivw + "&client=" + client + "&type=content&user=" + crc32Hash("user") + "&session=" + crc32Hash("session") + "&angebot=rtlnow&starttime=00:00:00:00";
            if (timetp != null) {
                playpath = playpath + "&timetype=" + timetp;
            }
            if (fkcont != null) {
                playpath = playpath + "&fkcontent=" + fkcont;
            }
            if (season != null) {
                playpath = playpath + "&season=" + season;
            }

            rtmp.setPlayPath(playpath);
            rtmp.setPageUrl(downloadLink.getDownloadURL());
            rtmp.setSwfVfy(swfurl);
            rtmp.setFlashVer("WIN 10,1,102,64");
            rtmp.setApp(app);
            rtmp.setUrl(host + app);
            rtmp.setResume(true);

            ((RTMPDownload) dl).startDownload();
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
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        final String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        String filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\">").getMatch(0);
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String folge = br.getRegex("Folge: '(.*?)'").getMatch(0);
        if (folge != null && filename.contains(folge)) {
            filename = filename.substring(0, filename.lastIndexOf("-")).trim();
        }
        final String season = dllink.contains("season=") ? new Regex(dllink, "season=(\\d+)").getMatch(0) : "0";
        if (!season.equals("0")) {
            filename += " - Staffel " + season;
        }
        filename = filename.trim();
        if (downloadLink.isDefaultFilePackage()) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(filename.replaceAll(folge, ""));
            downloadLink.setFilePackage(fp);
        }
        downloadLink.setName(folge + ".flv");
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
