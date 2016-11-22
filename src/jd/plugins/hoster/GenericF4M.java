//    jDownloader - Downloadmanager
//    Copyright (C) 2014  JD-Team support@jdownloader.org
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hds.HDSDownloader;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@HostPlugin(revision = "$Revision: 35421 $", interfaceVersion = 3, names = { "f4m" }, urls = { "f4ms?://.+?(\\.f4m?(\\?.+)?|$)" })
public class GenericF4M extends PluginForHost {

    public GenericF4M(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getHost(DownloadLink link, Account account) {
        if (link != null) {
            return Browser.getHost(link.getDownloadURL());
        }
        return super.getHost(link, account);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        if (link.getPluginPatternMatcher().startsWith("f4m")) {
            final String url = "http" + link.getPluginPatternMatcher().substring(3);
            link.setPluginPatternMatcher(url);
        }
    }

    @Override
    public boolean isSpeedLimited(DownloadLink link, Account account) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        final String cookiesString = downloadLink.getStringProperty("cookies", null);
        if (cookiesString != null) {
            final String host = Browser.getHost(downloadLink.getPluginPatternMatcher());
            br.setCookies(host, Cookies.parseCookies(cookiesString, host, null));
        }
        final String referer = downloadLink.getStringProperty("Referer", null);
        if (referer != null) {
            br.getPage(referer);
        }
        final URLConnectionAdapter con = br.openGetConnection(downloadLink.getPluginPatternMatcher());
        if (!con.isOK()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Document d;
        try {
            final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            d = parser.parse(con.getInputStream());
        } finally {
            con.disconnect();
        }
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final NodeList root = (NodeList) xPath.evaluate("/manifest", d, XPathConstants.NODESET);
        final Node durationNode = getNodeByName(root.item(0).getChildNodes(), "duration");
        final NodeList mediaUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
        final String fragmentURL = getBestMatchingFragmentURL(downloadLink, mediaUrls);
        if (fragmentURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            downloadLink.setProperty("fragmentUrl", fragmentURL);
            final HDSDownloader dl = new HDSDownloader(downloadLink, br, fragmentURL);
            this.dl = dl;
            if (durationNode != null) {
                final String durationText = durationNode.getTextContent();
                try {
                    final long parsedDuration;
                    final int dotIndex = durationText.indexOf('.');
                    if (dotIndex != -1) {
                        final String msnsCheck = durationText.substring(dotIndex + 1);
                        if (msnsCheck.length() == 6) {
                            parsedDuration = Long.parseLong(durationText.replace(".", "")) / 1000;
                        } else if (msnsCheck.length() == 3) {
                            parsedDuration = Long.parseLong(durationText.replace(".", ""));
                        } else {
                            throw new WTFException("Unparsable duration:" + durationText);
                        }
                    } else {
                        throw new WTFException("Unparsable duration:" + durationText);
                    }
                    dl.setEstimatedDuration(parsedDuration);
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
            dl.startDownload();
        }
    }

    private String getBestMatchingFragmentURL(final DownloadLink downloadLink, NodeList mediaUrls) {
        final String fragmentUrl = downloadLink.getStringProperty("fragmentUrl", null);
        final String streamId = downloadLink.getStringProperty("streamId", null);
        final String bitrate = downloadLink.getStringProperty("bitrate", null);
        final String width = downloadLink.getStringProperty("width", null);
        final String height = downloadLink.getStringProperty("height", null);
        final String qualityMatch = bitrate + "_" + width + "_" + height;
        for (int index = 0; index < mediaUrls.getLength(); index++) {
            final Node media = mediaUrls.item(index);
            final String media_fragmentUrl = getAttByNamedItem(media, "url");
            if (media_fragmentUrl != null) {
                if (fragmentUrl.equals(media_fragmentUrl)) {
                    return media_fragmentUrl;
                }
                final String media_streamId = GenericF4M.getAttByNamedItem(media, "streamId");
                if (streamId != null && streamId.equals(media_streamId)) {
                    return media_fragmentUrl;
                }
                final String media_width = GenericF4M.getAttByNamedItem(media, "width");
                final String media_height = GenericF4M.getAttByNamedItem(media, "height");
                final String media_bitrate = GenericF4M.getAttByNamedItem(media, "bitrate");
                if (qualityMatch.equals(media_bitrate + "_" + media_width + "_" + media_height)) {
                    return media_fragmentUrl;
                }
            }
        }
        return null;
    }

    public static Node getNodeByName(final NodeList nodes, final String name) {
        if (nodes != null && nodes.getLength() > 0) {
            for (int index = 0; index < nodes.getLength(); index++) {
                final Node node = nodes.item(index);
                if (StringUtils.equals(node.getNodeName(), name)) {
                    return node;
                }
            }
        }
        return null;
    }

    public static String getAttByNamedItem(final Node node, final String item) {
        if (node != null && node.hasAttributes()) {
            final Node attribute = node.getAttributes().getNamedItem(item);
            if (attribute != null) {
                final String content = attribute.getTextContent();
                return (content != null ? content.trim() : null);
            }
        }
        return null;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.GENERIC };
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (link != null) {
            link.removeProperty(HDSDownloader.RESUME_FRAGMENT);
        }
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }
}