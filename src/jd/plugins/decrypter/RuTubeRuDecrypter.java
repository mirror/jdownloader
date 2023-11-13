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
package jd.plugins.decrypter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.RuTubeVariant;
import jd.plugins.hoster.RuTubeRu;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rutube.ru" }, urls = { "https?://((?:www\\.)?rutube\\.ru/(tracks/\\d+\\.html|(play/|video/)?embed/\\w+(.*?p=[A-Za-z0-9\\-_]+)?|video/[a-f0-9]{32})|video\\.rutube.ru/([a-f0-9]{32}|\\d+))" })
public class RuTubeRuDecrypter extends PluginForDecrypt {
    public RuTubeRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* TODO: Eliminate this global variable. */
    private String privatevalue = null;

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final DownloadLink link = crawlSingleVideo(param);
        if (link == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            ret.add(link);
        }
        return ret;
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

    protected DownloadLink crawlSingleVideo(final CryptedLink param) throws PluginException, Exception {
        String videoHash = new Regex(param.getCryptedUrl(), "/([a-f0-9]{32})").getMatch(0);
        String videoID = new Regex(param.getCryptedUrl(), "rutube\\.ru/(?:play/|video/)?embed/(\\d{3,})").getMatch(0);
        if (videoID == null) {
            videoID = new Regex(param.getCryptedUrl(), "video\\.rutube\\.ru/(\\d{3,})").getMatch(0);
        }
        if (videoID == null) {
            videoID = new Regex(param.getCryptedUrl(), "/tracks/(\\d+)").getMatch(0);
        }
        privatevalue = new Regex(param.getCryptedUrl(), "p=([A-Za-z0-9\\-_]+)").getMatch(0);
        if (videoID == null) {
            if (videoHash == null) {
                /* Developer mistake */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Find videoID */
            final Browser br = this.br.cloneBrowser();
            // since we know the embed url for player/embed link types no need todo this step
            getPage(br, "http://" + this.getHost() + "/api/video/" + videoHash);
            /* 2020-02-11: This may return HTTP/1.1 401 UNAUTHORIZED for normal offline content too. */
            if (br.getHttpConnection().getResponseCode() == 401 || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<root><detail>Not found</detail></root>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            videoID = entries.get("track_id").toString();
        }
        final Browser ajax = getAjaxBR(br.cloneBrowser());
        getPage(ajax, "http://" + this.getHost() + "/api/play/options/" + videoID + "/?format=json&no_404=true&sqr4374_compat=1&referer=" + Encoding.urlEncode(param.getCryptedUrl()) + "&_t=" + System.currentTimeMillis());
        final Map<String, Object> entries = restoreFromString(ajax.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> detailmap = (Map<String, Object>) entries.get("detail");
        videoHash = (String) entries.get("effective_video");
        if (Boolean.FALSE.equals(entries.get("has_video"))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (ajax.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (StringUtils.isEmpty(videoHash)) {
            if (detailmap != null && detailmap.get("name").toString().equalsIgnoreCase("default_does_not_exists_video")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final DownloadLink ret = super.createDownloadlink("https://" + this.getHost() + "/video/" + videoHash);
        final String title = (String) entries.get("title");
        final long durationSeconds = ((Number) entries.get("duration")).longValue();
        final String videoDescription = (String) entries.get("description");
        final Map<String, Object> author = (Map<String, Object>) entries.get("author");
        final String uploaderName = (String) author.get("name");
        final Map<String, Object> videoBalancer = (Map<String, Object>) entries.get("video_balancer");
        final String streamDefault = (String) videoBalancer.get("default");
        final String streamHLSMaster = (String) videoBalancer.get("m3u8");
        final String expireTimestampStr;
        /* 2021-09-17: Prefer HLS over HDS */
        ArrayList<RuTubeVariant> variantsVideo = new ArrayList<RuTubeVariant>();
        RuTubeVariant bestVariant = null;
        if (!StringUtils.isEmpty(streamHLSMaster)) {
            /* HLS */
            expireTimestampStr = UrlQuery.parse(streamHLSMaster).get("expire");
            if (expireTimestampStr == null || !expireTimestampStr.matches("\\d+")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Browser hlsBR = br.cloneBrowser();
            hlsBR.getPage(streamHLSMaster);
            final List<HlsContainer> qualities = HlsContainer.getHlsQualities(hlsBR);
            int highestBandwidth = -1;
            int counter = 0;
            for (final HlsContainer quality : qualities) {
                final RuTubeVariant variant = new RuTubeVariant(Integer.toString(quality.getWidth()), Integer.toString(quality.getHeight()), Integer.toString(quality.getBandwidth()), "hls_" + counter);
                variantsVideo.add(variant);
                if (quality.getBandwidth() > highestBandwidth) {
                    highestBandwidth = quality.getBandwidth();
                    bestVariant = variant;
                }
                ret.setProperty("directurl_" + variant.getStreamID(), quality.getDownloadurl());
                counter += 1;
            }
        } else {
            if (streamDefault == null || !streamDefault.contains(".f4m")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            expireTimestampStr = UrlQuery.parse(streamDefault).get("expire");
            if (expireTimestampStr == null || !expireTimestampStr.matches("\\d+")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final XPath xPath = XPathFactory.newInstance().newXPath();
            final Browser streamBR = getAjaxBR(br.cloneBrowser());
            streamBR.getPage(streamDefault);
            Document d = parser.parse(new ByteArrayInputStream(streamBR.toString().getBytes("UTF-8")));
            String baseUrl = xPath.evaluate("/manifest/baseURL", d).trim();
            NodeList f4mUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
            Node best = f4mUrls.item(f4mUrls.getLength() - 1);
            long bestSizeEstimation = 0;
            for (int i = 0; i < f4mUrls.getLength(); i++) {
                best = f4mUrls.item(i);
                String width = getAttByNamedItem(best, "width");
                String height = getAttByNamedItem(best, "height");
                String bitrate = getAttByNamedItem(best, "bitrate");
                String f4murl = getAttByNamedItem(best, "href");
                bestSizeEstimation = (durationSeconds * Long.parseLong(bitrate) * 1024l) / 8;
                streamBR.getPage(baseUrl + f4murl);
                d = parser.parse(new ByteArrayInputStream(streamBR.toString().getBytes("UTF-8")));
                // baseUrl = xPath.evaluate("/manifest/baseURL", d).trim();
                // double duration = Double.parseDouble(xPath.evaluate("/manifest/duration", d));
                NodeList mediaUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
                Node media;
                for (int j = 0; j < mediaUrls.getLength(); j++) {
                    media = mediaUrls.item(j);
                    // System.out.println(new String(Base64.decode(xPath.evaluate("/manifest/media[" + (j + 1) + "]/metadata",
                    // d).trim())));
                    final RuTubeVariant variant = new RuTubeVariant(width, height, bitrate, getAttByNamedItem(media, "streamId"));
                    variantsVideo.add(variant);
                    ret.setProperty("directurl_" + variant.getStreamID(), Request.getLocation(getAttByNamedItem(media, "url"), ajax.getRequest()));
                    bestVariant = variant;
                }
            }
            if (bestSizeEstimation > 0) {
                ret.setDownloadSize(bestSizeEstimation);
            }
        }
        ret.setProperty(RuTubeRu.PROPERTY_INTERNAL_VIDEOID, videoID);
        ret.setProperty(RuTubeRu.PROPERTY_EXPIRE_TIMESTAMP, Long.parseLong(expireTimestampStr) * 1000);
        ret.setProperty(RuTubeRu.PROPERTY_TITLE, title);
        ret.setProperty(RuTubeRu.PROPERTY_DURATION, durationSeconds);
        if (!StringUtils.isEmpty(uploaderName)) {
            ret.setProperty(RuTubeRu.PROPERTY_UPLOADER, uploaderName);
        }
        if (privatevalue != null) {
            ret.setProperty(RuTubeRu.PROPERTY_PRIVATEVALUE, privatevalue);
        }
        if (!StringUtils.isEmpty(videoDescription)) {
            ret.setComment(videoDescription);
        }
        if (variantsVideo.size() > 0) {
            ret.setVariants(variantsVideo);
            ret.setVariant(bestVariant);
            return ret;
        } else {
            /* TODO: Check this */
            logger.info("Video not available in your country: http://rutube.ru/api/video/" + videoID);
            ret.setAvailable(false);
            return ret;
        }
    }

    private Browser getAjaxBR(final Browser br) {
        // rv40.0 don't get "video_balancer".
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:51.0) Gecko/20100101 Firefox/51.0");
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Content-Type", "application/json");
        return br;
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

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}