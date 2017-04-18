package org.jdownloader.plugins.components.hds;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.http.Browser;
import jd.plugins.DownloadLink;

import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HDSContainer {

    public static HDSContainer findBestVideoByResolution(final List<HDSContainer> list) {
        if (list != null) {
            HDSContainer best = null;
            for (final HDSContainer container : list) {
                if (best == null || (container.getHeight() * container.getWidth() > best.getHeight() * best.getWidth()) || (container.getBitrate() > best.getBitrate() && (container.getHeight() * container.getWidth() == best.getHeight() * best.getWidth()))) {
                    best = container;
                }
            }
            return best;
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

    public static long parseDuration(final String duration) {
        final int dotIndex = duration.indexOf('.');
        if (dotIndex != -1) {
            final String msnsCheck = duration.substring(dotIndex + 1);
            if (msnsCheck.length() == 6) {
                return Long.parseLong(duration.replace(".", "")) / 1000;
            } else if (msnsCheck.length() == 3) {
                return Long.parseLong(duration.replace(".", ""));
            }
        }
        return -1;
    }

    public static HDSContainer getBestMatchingContainer(List<HDSContainer> all, final HDSContainer searchFor) {
        if (searchFor != null && all != null) {
            for (final HDSContainer container : all) {
                if (searchFor.getFragmentURL().equals(container.getFragmentURL())) {
                    return container;
                }
                if (searchFor.getStreamId() != null && searchFor.getStreamId().equals(container.getStreamId())) {
                    return container;
                }
                if (searchFor.getBitrate() != container.getBitrate()) {
                    continue;
                }
                if (searchFor.getHeight() != container.getHeight()) {
                    continue;
                }
                if (searchFor.getWidth() != container.getWidth()) {
                    continue;
                }
                return container;
            }
        }
        return null;
    }

    public final static String PROPERTY_FRAGMENTURL = "fragmentUrl";
    public final static String PROPERTY_STREAMID    = "streamId";
    public final static String PROPERTY_ID          = "id";
    public final static String PROPERTY_WIDTH       = "width";
    public final static String PROPERTY_HEIGHT      = "height";
    public final static String PROPERTY_BITRATE     = "bitrate";
    public final static String PROPERTY_DURATION    = "duration";

    public void write(DownloadLink destination) {
        write(destination, null);
    }

    public void write(DownloadLink destination, final String propertyPrefix) {
        if (destination != null) {
            final String prefix;
            if (propertyPrefix != null) {
                prefix = propertyPrefix + "_";
            } else {
                prefix = "";
            }
            destination.setProperty(prefix + PROPERTY_FRAGMENTURL, getFragmentURL());
            destination.setProperty(prefix + PROPERTY_STREAMID, getStreamId());
            destination.setProperty(prefix + PROPERTY_WIDTH, getWidth());
            destination.setProperty(prefix + PROPERTY_HEIGHT, getHeight());
            destination.setProperty(prefix + PROPERTY_BITRATE, getBitrate());
            destination.setProperty(prefix + PROPERTY_DURATION, getDuration());
            destination.setProperty(prefix + PROPERTY_DURATION, getId());
        }
    }

    public static HDSContainer read(DownloadLink source) {
        return read(source, null);
    }

    public static HDSContainer read(DownloadLink source, final String propertyPrefix) {
        if (source != null) {
            final String prefix;
            if (propertyPrefix != null) {
                prefix = propertyPrefix + "_";
            } else {
                prefix = "";
            }
            final String fragmentURL = source.getStringProperty(prefix + PROPERTY_FRAGMENTURL, null);
            if (fragmentURL != null) {
                final HDSContainer ret = new HDSContainer();
                ret.fragmentURL = fragmentURL;
                ret.streamId = source.getStringProperty(prefix + PROPERTY_STREAMID, null);
                ret.bitrate = source.getIntegerProperty(prefix + PROPERTY_BITRATE, -1);
                ret.height = source.getIntegerProperty(prefix + PROPERTY_HEIGHT, -1);
                ret.width = source.getIntegerProperty(prefix + PROPERTY_WIDTH, -1);
                ret.duration = source.getLongProperty(prefix + PROPERTY_DURATION, -1);
                ret.id = source.getStringProperty(prefix + PROPERTY_ID, null);
                return ret;
            }
        }
        return null;
    }

    public static List<HDSContainer> getHDSQualities(final Browser br) throws Exception {
        final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document d = parser.parse(new ByteArrayInputStream(br.getRequest().getHtmlCode().getBytes("UTF-8")));
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final NodeList root = (NodeList) xPath.evaluate("/manifest", d, XPathConstants.NODESET);
        final Node durationNode = getNodeByName(root.item(0).getChildNodes(), "duration");
        final Node id = getNodeByName(root.item(0).getChildNodes(), "id");
        final long duration;
        if (durationNode != null) {
            duration = parseDuration(durationNode.getTextContent());
        } else {
            duration = -1;
        }
        final NodeList mediaUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
        if (mediaUrls != null) {
            final List<HDSContainer> ret = new ArrayList<HDSContainer>();
            for (int j = 0; j < mediaUrls.getLength(); j++) {
                final Node media = mediaUrls.item(j);
                final String fragmentUrl = getAttByNamedItem(media, "url");
                if (fragmentUrl != null) {
                    final HDSContainer container = new HDSContainer();
                    container.fragmentURL = fragmentUrl;
                    final String width = getAttByNamedItem(media, "width");
                    if (width != null && width.matches("^\\d+$")) {
                        container.width = Integer.parseInt(width);
                    }
                    final String height = getAttByNamedItem(media, "height");
                    if (height != null && height.matches("^\\d+$")) {
                        container.height = Integer.parseInt(height);
                    }
                    final String bitrate = getAttByNamedItem(media, "bitrate");
                    if (bitrate != null && bitrate.matches("^\\d+$")) {
                        container.bitrate = Integer.parseInt(bitrate);
                    }
                    container.streamId = getAttByNamedItem(media, "streamId");
                    if (id != null) {
                        container.id = id.getTextContent();
                    }
                    container.duration = duration;
                    ret.add(container);
                }
            }
            if (ret.size() > 0) {
                return ret;
            }
        }
        return null;
    }

    private int    width       = -1;
    private int    height      = -1;
    private int    bitrate     = -1;
    private String fragmentURL = null;
    private String streamId    = null;
    private long   duration    = -1;
    private String id          = null;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getEstimatedFileSize() {
        if (duration > 0 && bitrate > 0) {
            return duration / 1000 * (bitrate * 1000l / 8);
        }
        return -1;
    }

    public long getDuration() {
        return duration;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getInternalID() {
        return Hash.getMD5(toString());
    }

    public String getFragmentURL() {
        return fragmentURL;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBitrate() {
        return bitrate;
    }

    @Override
    public String toString() {
        return "FragmentURL:" + getFragmentURL() + "|ID:" + getId() + "|StreamID:" + getStreamId() + "|Resolution:" + getWidth() + "x" + getHeight() + "|Est.FileSize:" + SizeFormatter.formatBytes(getEstimatedFileSize());
    }

    public String getResolution() {
        return this.getWidth() + "x" + this.getHeight();
    }

    public HDSContainer() {
    }

}