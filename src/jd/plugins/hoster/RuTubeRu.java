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
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.downloader.hds.HDSDownloader;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.RuTubeVariant;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rutube.ru" }, urls = { "http://(www\\.)?video\\.decryptedrutube\\.ru/[0-9a-f]{32}" })
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
     */
    @Override
    public void correctDownloadLink(final DownloadLink downloadLink) {
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceFirst("decryptedrutube\\.ru", "rutube\\.ru"));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String privatevalue = null;

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
        final Browser ajax = cloneBrowser(br);
        dl = new HDSDownloader(downloadLink, ajax, downloadLink.getStringProperty("f4vUrl"));
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        privatevalue = downloadLink.getStringProperty("privatevalue", null);
        RuTubeVariant var = downloadLink.getVariant(RuTubeVariant.class);
        String dllink = downloadLink.getDownloadURL();
        String regId = "http://video\\.rutube\\.ru/([0-9a-f]{32})";
        final String nextId = new Regex(dllink, regId).getMatch(0);
        br.setCustomCharset("utf-8");
        /*
         * 2017-02-21: Using User-Agent 'Mozilla/5.0 (Windows NT 6.3; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0' will return a different
         * 'video_balancer' object which leads to a (lower quality?) http videourl.
         */
        // br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:50.0) Gecko/20100101 Firefox/50.0");
        getPage("https://rutube.ru/api/play/trackinfo/" + nextId + "/");
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<root><detail>Not found</detail></root>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(br.toString().getBytes("UTF-8")));
        final XPath xPath = XPathFactory.newInstance().newXPath();
        String filename = getText(doc, xPath, "/root/title");
        if (filename == null) {
            /* Fallback */
            filename = nextId;
        }
        if (var != null) {
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()) + "_" + var.getHeight() + "p" + ".mp4");
        }
        /* 2019-09-19: This value was usually something else than 'nextId' but it seems to be the same now! */
        final String vid = br.getRegex("/play/embed/([^/<>\"]+)").getMatch(0);
        if (vid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* 2019-09-19: This request is not required anymore */
        // getPage("https://rutube.ru/play/embed/" + vid + "?wmode=opaque&autoStart=true");
        // swf requests over json
        Browser ajax = cloneBrowser(br);
        getPage(ajax, "/api/play/options/" + vid + "/?format=json&no_404=true&referer=" + Encoding.urlEncode(br.getURL()) + "&_t=" + System.currentTimeMillis());
        // getPage(ajax, "/api/play/options/" + vid + "/?format=json&no_404=true&sqr4374_compat=1&referer=" +
        // Encoding.urlEncode(br.getURL()) + "&_t=" + System.currentTimeMillis());
        final HashMap<String, Object> entries = (HashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
        final String videoBalancer = (String) JavaScriptEngineFactory.walkJson(entries, "video_balancer/default");
        if (videoBalancer == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        ajax = cloneBrowser(br);
        ajax.getPage(videoBalancer);
        Document d = parser.parse(new ByteArrayInputStream(ajax.toString().getBytes("UTF-8")));
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
            ajax = cloneBrowser(br);
            ajax.getPage(baseUrl + f4murl);
            d = parser.parse(new ByteArrayInputStream(ajax.toString().getBytes("UTF-8")));
            double duration = Double.parseDouble(xPath.evaluate("/manifest/duration", d));
            bestSizeEstimation = (long) ((duration * Long.parseLong(bitrate) * 1024l) / 8);
            NodeList mediaUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
            Node media;
            for (int j = 0; j < mediaUrls.getLength(); j++) {
                media = mediaUrls.item(j);
                if (var == null) {
                    downloadLink.setDownloadSize(bestSizeEstimation);
                    downloadLink.setProperty("f4vUrl", Request.getLocation(getAttByNamedItem(media, "url"), ajax.getRequest()));
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
                                    downloadLink.setProperty("f4vUrl", Request.getLocation(getAttByNamedItem(media, "url"), ajax.getRequest()));
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
    }

    private void getPage(final String url) throws IOException {
        getPage(this.br, url);
    }

    private void getPage(final Browser br, String url) throws IOException {
        if (privatevalue != null) {
            if (!url.contains("?")) {
                url += "?";
            } else {
                url += "&";
            }
            url += "p=" + Encoding.urlEncode(privatevalue);
        }
        br.getPage(url);
    }

    private Browser cloneBrowser(Browser br) {
        final Browser ajax = br.cloneBrowser();
        // rv40.0 don't get "video_balancer".
        ajax.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:50.0) Gecko/20100101 Firefox/50.0");
        ajax.getHeaders().put("Accept", "*/*");
        ajax.getHeaders().put("X-Requested-With", "ShockwaveFlash/22.0.0.209");
        return ajax;
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
        if (link != null) {
            link.removeProperty(HDSDownloader.RESUME_FRAGMENT);
        }
    }

    @Override
    public void resetPluginGlobals() {
    }
}