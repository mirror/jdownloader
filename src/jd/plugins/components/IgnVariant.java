package jd.plugins.components;

import javax.swing.Icon;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

public class IgnVariant implements LinkVariant, Storable {
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof IgnVariant)) {
            return false;
        }
        return _getUniqueId().equals(((IgnVariant) obj)._getUniqueId());
    }

    private String width;

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    @Override
    public String _getTooltipDescription(Object caller) {
        return height + "p (" + bitrate + "bps)";
    }

    public String getBitrate() {
        return bitrate;
    }

    public void setBitrate(String bitrate) {
        this.bitrate = bitrate;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getStreamID() {
        return streamID;
    }

    public void setStreamID(String streamID) {
        this.streamID = streamID;
    }

    private String       bitrate;
    private String       height;
    private String       streamID;
    private AbstractIcon icon;

    public IgnVariant(/* storable */) {

    }

    public IgnVariant(String width, String height, String bitrate, String streamID) {
        this.width = width;
        this.height = height;
        this.bitrate = bitrate;
        this.streamID = streamID;
        icon = new AbstractIcon(IconKey.ICON_VIDEO, 16);
    }

    @Override
    public String _getUniqueId() {
        return width + "x" + height + "_bitrate_" + streamID;
    }

    @Override
    public String _getName(Object caller) {
        return height + "p";
    }

    @Override
    public Icon _getIcon(Object caller) {
        return icon;
    }

}
