package jd.plugins.components;

import javax.swing.Icon;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

public class DailyMotionVariant implements Storable, LinkVariant {
    private static final Icon VIDEO = new AbstractIcon(IconKey.ICON_VIDEO, 16);
    private static final Icon AUDIO = new AbstractIcon(IconKey.ICON_AUDIO, 16);
    private String            directlink;
    private String            qName;

    public String getLink() {
        return directlink;
    }

    public void setLink(String link) {
        this.directlink = link;
    }

    public String getqName() {
        return qName;
    }

    public void setqName(String qName) {
        this.qName = qName;
    }

    private int videoHeight;

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int num) {
        this.videoHeight = num;
    }

    private String convertTo = null;

    public String getConvertTo() {
        return convertTo;
    }

    public void setConvertTo(String convertTo) {
        this.convertTo = convertTo;
    }

    public DailyMotionVariant(/* storable */) {
    }

    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public DailyMotionVariant(DailyMotionVariant variant, String convert, String qualityName, String name) {
        this(variant.getLink(), variant.getVideoHeight(), convert, qualityName, name);
    }

    public DailyMotionVariant(String directlink, int videoHeight, String convert, String qualityName, String name) {
        this.directlink = directlink;
        qName = qualityName;
        this.videoHeight = videoHeight;
        this.convertTo = convert;
        displayName = name;
    }

    @Override
    public String _getUniqueId() {
        return convertTo == null ? displayName : (displayName + "->" + convertTo);
    }

    @Override
    public String _getName(Object caller) {
        return getDisplayName();
    }

    @Override
    public Icon _getIcon(Object caller) {
        if ("m4a".equals(getConvertTo()) || "aac".equals(getConvertTo())) {
            return AUDIO;
        } else {
            return VIDEO;
        }
    }

    @Override
    public String _getTooltipDescription(Object caller) {
        return _getName(caller);
    }
}