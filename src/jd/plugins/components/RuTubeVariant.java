package jd.plugins.components;

import javax.swing.Icon;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

public class RuTubeVariant implements LinkVariant, Storable {
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof RuTubeVariant)) {
            return false;
        }
        return _getUniqueId().equals(((RuTubeVariant) obj)._getUniqueId());
    }

    private String width;

    public String getWidth() {
        return width;
    }

    @Override
    public String _getTooltipDescription() {
        return _getExtendedName();
    }

    public void setWidth(String width) {
        this.width = width;
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

    public RuTubeVariant(/* storable */) {

    }

    public RuTubeVariant(String width, String height, String bitrate, String streamID) {
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
    public String _getName() {
        return height + "p";
    }

    @Override
    public Icon _getIcon() {
        return icon;
    }

    @Override
    public String _getExtendedName() {
        return height + "p (" + bitrate + "bps)";
    }
}