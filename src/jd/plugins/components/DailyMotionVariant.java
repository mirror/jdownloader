package jd.plugins.components;

import javax.swing.Icon;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

import jd.plugins.DownloadLink;
import jd.plugins.hoster.DailyMotionCom;

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

    public DailyMotionVariant(DownloadLink dl) {
        directlink = DailyMotionCom.getDirectlink(dl);
        // 1920x1080
        qName = dl.getStringProperty(DailyMotionCom.PROPERTY_QUALITY_NAME);
        // "5"
        videoHeight = DailyMotionCom.getQualityHeight(dl);
        displayName = qName;
    }

    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public DailyMotionVariant(final DailyMotionVariant bestVi, String convert, String qualityName, String name) {
        directlink = bestVi.directlink;
        qName = name;
        videoHeight = bestVi.videoHeight;
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