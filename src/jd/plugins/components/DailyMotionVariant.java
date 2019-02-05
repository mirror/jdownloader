package jd.plugins.components;

import javax.swing.Icon;

import jd.plugins.DownloadLink;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

public class DailyMotionVariant implements Storable, LinkVariant {
    private static final Icon VIDEO = new AbstractIcon(IconKey.ICON_VIDEO, 16);
    private static final Icon AUDIO = new AbstractIcon(IconKey.ICON_AUDIO, 16);
    private String            link;
    private String            qValue;
    private String            qName;

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getqValue() {
        return qValue;
    }

    public void setqValue(String qValue) {
        this.qValue = qValue;
    }

    public String getqName() {
        return qName;
    }

    public void setqName(String qName) {
        this.qName = qName;
    }

    public String getOrgQName() {
        return orgQName;
    }

    public void setOrgQName(String orgQName) {
        this.orgQName = orgQName;
    }

    private String orgQName;
    private int    qrate;

    public int getQrate() {
        return qrate;
    }

    public void setQrate(int qrate) {
        this.qrate = qrate;
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
        link = dl.getStringProperty("directlink");
        // "5"
        qValue = dl.getStringProperty("qualityvalue");
        // H264-320x240
        qName = dl.getStringProperty("qualityname");
        // stream_h264_ld_url
        orgQName = dl.getStringProperty("originalqualityname");
        // "5"
        qrate = Integer.parseInt(dl.getStringProperty("qualitynumber"));
        displayName = qName;
    }

    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public DailyMotionVariant(DailyMotionVariant bestVi, String convert, String qualityName, String name) {
        link = bestVi.link;
        qValue = bestVi.qValue;
        qName = name;
        orgQName = bestVi.orgQName;
        qrate = bestVi.qrate;
        this.convertTo = convert;
        displayName = name;
    }

    @Override
    public String _getUniqueId() {
        return convertTo == null ? orgQName : (orgQName + "->" + convertTo);
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