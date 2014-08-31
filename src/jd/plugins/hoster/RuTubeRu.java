//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
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
import jd.plugins.decrypter.RuTubeRuDecrypter.RuTubeVariant;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.downloader.hds.HDSDownloader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rutube.ru" }, urls = { "http://(www\\.)?video\\.decryptedrutube\\.ru/[0-9a-f]{32}" }, flags = { 0 })
public class RuTubeRu extends PluginForHost {

    public RuTubeRu(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://rutube.ru/agreement.html";
    }

    /**
     * Corrects downloadLink.urlDownload().<br/>
     * <br/>
     * The following code respect the hoster supported protocols via plugin boolean settings and users config preference
     * 
     * @author raztoki
     * */
    @Override
    public void correctDownloadLink(final DownloadLink downloadLink) {
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceFirst("decryptedrutube\\.ru", "rutube\\.ru"));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        download(downloadLink);
    }

    @Override
    public LinkVariant getActiveVariantByLink(DownloadLink downloadLink) {
        return downloadLink.getVariant(RuTubeVariant.class);
    }

    @Override
    public void setActiveVariantByLink(DownloadLink downloadLink, LinkVariant variant) {
        downloadLink.setDownloadSize(-1);
        super.setActiveVariantByLink(downloadLink, variant);

    }

    @Override
    public List<? extends LinkVariant> getVariantsByLink(DownloadLink downloadLink) {
        return downloadLink.getVariants(RuTubeVariant.class);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        dl = new HDSDownloader(downloadLink, br, downloadLink.getStringProperty("f4vUrl"));
        dl.startDownload();

    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException, SAXException, ParserConfigurationException, XPathExpressionException {
        setBrowserExclusive();
        RuTubeVariant var = downloadLink.getVariant(RuTubeVariant.class);
        String dllink = downloadLink.getDownloadURL();
        String regId = "http://video\\.rutube\\.ru/([0-9a-f]{32})";
        String nextId = new Regex(dllink, regId).getMatch(0);
        br.setCustomCharset("utf-8");
        br.getPage("http://rutube.ru/api/video/" + nextId);
        if (br.containsHTML("<root><detail>Not found</detail></root>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(br.toString().getBytes("UTF-8")));
        final XPath xPath = XPathFactory.newInstance().newXPath();

        String filename = getText(doc, xPath, "/root/title");

        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        if (var != null) {
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()) + "_" + var.getHeight() + "p" + ".mp4");
        }
        br.getPage("http://rutube.ru/play/embed/" + br.getRegex("/embed/(\\d+)").getMatch(0) + "?wmode=opaque&autoStart=true");

        String videoBalancer = br.getRegex("(http\\:\\/\\/bl\\.rutube\\.ru[^\"]+)").getMatch(0);
        if (videoBalancer != null) {
            final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            br.getPage(videoBalancer);

            Document d = parser.parse(new ByteArrayInputStream(br.toString().getBytes("UTF-8")));
            String baseUrl = xPath.evaluate("/manifest/baseURL", d).trim();

            NodeList f4mUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
            Node best = f4mUrls.item(f4mUrls.getLength() - 1);

            RuTubeVariant bestVariant = null;
            long bestSizeEstimation = 0;
            String bestUrl = null;

            String width = null;
            String height = null;
            String bitrate = null;
            String f4murl = null;
            for (int i = 0; i < f4mUrls.getLength(); i++) {
                best = f4mUrls.item(i);
                width = getAttByNamedItem(best, "width");
                height = getAttByNamedItem(best, "height");
                bitrate = getAttByNamedItem(best, "bitrate");
                f4murl = getAttByNamedItem(best, "href");

                br.getPage(baseUrl + f4murl);

                d = parser.parse(new ByteArrayInputStream(br.toString().getBytes("UTF-8")));
                double duration = Double.parseDouble(xPath.evaluate("/manifest/duration", d));
                bestSizeEstimation = (long) ((duration * Long.parseLong(bitrate) * 1024l) / 8);

                NodeList mediaUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
                Node media;
                for (int j = 0; j < mediaUrls.getLength(); j++) {
                    media = mediaUrls.item(j);
                    if (var == null) {
                        downloadLink.setDownloadSize(bestSizeEstimation);
                        downloadLink.setProperty("f4vUrl", getAttByNamedItem(media, "url"));

                        downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()) + "_" + height + "p" + ".mp4");
                        return AvailableStatus.TRUE;
                    }
                    if (var == null || StringUtils.equals(getAttByNamedItem(media, "streamId"), var.getStreamID())) {
                        // found
                        bestUrl = getAttByNamedItem(media, "url");
                        if (var != null) {
                            if (StringUtils.equals(var.getWidth(), width)) {
                                if (StringUtils.equals(var.getHeight(), height)) {
                                    if (StringUtils.equals(var.getBitrate(), bitrate)) {
                                        downloadLink.setDownloadSize(bestSizeEstimation);
                                        downloadLink.setProperty("f4vUrl", getAttByNamedItem(media, "url"));

                                        downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()) + "_" + height + "p" + ".mp4");
                                        return AvailableStatus.TRUE;
                                    }
                                }
                            }
                        }
                    }

                }

            }
            if (bestSizeEstimation > 0) {

                downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()) + "_" + height + "p" + ".mp4");
                downloadLink.setDownloadSize(bestSizeEstimation);
                downloadLink.setProperty("f4vUrl", bestUrl);
                return AvailableStatus.TRUE;
            }

            return AvailableStatus.FALSE;
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

    }

    private String getText(Document doc, XPath xPath, String string) throws XPathExpressionException {
        Node n = (Node) xPath.evaluate(string, doc, XPathConstants.NODE);
        return (n != null ? n.getFirstChild().getTextContent().trim() : null);
    }

    /**
     * lets try and prevent possible NPE from killing progress.
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
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}