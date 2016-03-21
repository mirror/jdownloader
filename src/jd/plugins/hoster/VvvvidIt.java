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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.GenericM3u8Decrypter.HlsContainer;

import org.jdownloader.downloader.hds.HDSDownloader;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vvvvid.it" }, urls = { "http://vvvviddecrypted\\.it/\\d+" }, flags = { 0 })
public class VvvvidIt extends PluginForHost {

    public VvvvidIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://vvvvid.it/";
    }

    /* Example hls master: http://vvvvid-vh.akamaihd.net/i/Dynit/KekkaiSensen/KekkaiSensen_Ep01m.mp4/master.m3u8 */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename = link.getStringProperty("filename", null);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String stream_master = downloadLink.getStringProperty("directlink", null);
        if (stream_master == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (stream_master.contains(".f4m")) {
            /* HDS */
            /*
             * E.g.
             * 'http://vvvvid-vh.akamaihd.net/z/Dynit/Durarara/DurararaX2_Ep01_H26phJw2RNlnBURem.mp4/manifest.f4m?g=WBTXLPEAWKUL&hdcore=3.6.0&plugin=aasp-3.6.0.50.41'
             */
            stream_master += "?hdcore=3.6.0&plugin=aasp-3.6.0.50.41";
            br.getPage(stream_master);
            String finallink = null;
            final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final XPath xPath = XPathFactory.newInstance().newXPath();
            Document d = parser.parse(new ByteArrayInputStream(br.toString().getBytes("UTF-8")));
            NodeList mediaUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
            Node media;
            for (int j = 0; j < mediaUrls.getLength(); j++) {
                media = mediaUrls.item(j);

                String temp = getAttByNamedItem(media, "url");
                if (temp != null) {
                    finallink = temp;
                    break;
                }
            }
            dl = new HDSDownloader(downloadLink, br, finallink);
            dl.startDownload();
        } else {
            /* HLS */
            if (!stream_master.startsWith("http")) {
                /* Workaround for very old content e.g. http://www.vvvvid.it/#!show/52/gto-great-teacher-onizuka/50/392657/lesson-1 */
                /*
                 * Thanks:
                 * http://andrealazzarotto.com/2014/02/22/scaricare-i-contenuti-audio-e-video-presenti-nelle-pagine-web/comment-page-7/
                 */
                /* http://wowzaondemand.top-ix.org/videomg/_definst_/mp4:Dynit/GTO/GTO_Ep01.mp4/playlist.m3u8 */
                stream_master = "http://wowzaondemand.top-ix.org/videomg/_definst_/mp4:" + stream_master + "/playlist.m3u8";
            } else {
                /* No need to change our hls urls */
            }
            this.br.getPage(stream_master);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final HlsContainer hlsbest = jd.plugins.decrypter.GenericM3u8Decrypter.findBestVideoByBandwidth(jd.plugins.decrypter.GenericM3u8Decrypter.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.downloadurl;
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
            dl.startDownload();
        }
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

    /**
     * lets try and prevent possible NPE from killing the progress.
     *
     * @author raztoki
     * @param n
     * @param item
     * @return
     */
    private String getAttByNamedItem(final Node n, final String item) {
        final String t = n.getAttributes().getNamedItem(item).getTextContent();
        return (t != null ? t.trim() : null);
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