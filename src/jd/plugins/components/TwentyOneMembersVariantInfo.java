package jd.plugins.components;

import javax.swing.Icon;

import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.linkcrawler.LinkVariant;

import jd.parser.Regex;

public class TwentyOneMembersVariantInfo implements Storable, LinkVariant {
    private String url;
    private String shortType;
    private String filesize;

    public String getFilesize() {
        return this.filesize;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getShortType() {
        return shortType;
    }

    public void setShortType(String shortType) {
        this.shortType = shortType;
    }

    public TwentyOneMembersVariantInfo(/* Storable */) {
    }

    public TwentyOneMembersVariantInfo(final String url, final String shortType) {
        this.url = url;
        this.shortType = shortType;
    }

    public TwentyOneMembersVariantInfo(final String url, final String shortType, final String filesize) {
        this.url = url;
        this.shortType = shortType;
        this.filesize = filesize;
    }

    @Override
    public String _getUniqueId() {
        return shortType;
    }

    public int _getQuality() {
        final String xx_p = new Regex(this.shortType, "(\\d+)").getMatch(0);
        if (xx_p != null) {
            return Integer.parseInt(xx_p);
        }
        return -1;
    }

    @Override
    public String _getName(Object caller) {
        return shortType;
    }

    @Override
    public Icon _getIcon(Object caller) {
        return null;
    }

    @Override
    public String _getTooltipDescription(Object caller) {
        return shortType;
    }

    public String _getExtension() {
        if (StringUtils.equals("ziph", shortType)) {
            return "zip";
        }
        if (StringUtils.equals("hiresh", shortType)) {
            return "zip";
        }
        return "mp4";
    }

    public boolean isPhoto() {
        if (StringUtils.equals("ziph", shortType)) {
            return true;
        }
        if (StringUtils.equals("hiresh", shortType)) {
            return true;
        }
        return false;
    }
}
