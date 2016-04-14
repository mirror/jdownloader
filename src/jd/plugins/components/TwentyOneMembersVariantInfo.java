package jd.plugins.components;

import javax.swing.Icon;

import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.linkcrawler.LinkVariant;

public class TwentyOneMembersVariantInfo implements Storable, LinkVariant {
    private String url;

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

    private String shortType;

    public TwentyOneMembersVariantInfo(/* Storable */) {
    }

    public TwentyOneMembersVariantInfo(String url, String shortType) {
        this.url = url;
        this.shortType = shortType;
    }

    @Override
    public String _getUniqueId() {
        return shortType;
    }

    public int _getQuality() {
        if (StringUtils.equals("fullhd", shortType)) {
            return 1000;
        }
        if (StringUtils.equals("hd", shortType)) {
            return 999;
        }
        if (StringUtils.equals("hq", shortType)) {
            return 998;
        }
        if (StringUtils.equals("phone480", shortType)) {
            return 997;
        }
        if (StringUtils.equals("phone272", shortType)) {
            return 996;
        }
        if (StringUtils.equals("ziph", shortType)) {
            return 0;
        }
        if (StringUtils.equals("hiresh", shortType)) {
            return 1;
        }
        return -1;
    }

    @Override
    public String _getName(Object caller) {
        if (StringUtils.equals("fullhd", shortType)) {
            return "FullHD 1080p Video";
        }
        if (StringUtils.equals("hd", shortType)) {
            return "HD 720p Video";
        }
        if (StringUtils.equals("hq", shortType)) {
            return "HQ 540p Video";
        }
        if (StringUtils.equals("phone480", shortType)) {
            return "Phone 480p Video";
        }
        if (StringUtils.equals("phone272", shortType)) {
            return "Phone 272p Video";
        }
        if (StringUtils.equals("ziph", shortType)) {
            return "Low Quality Photos";
        }
        if (StringUtils.equals("hiresh", shortType)) {
            return "High Quality Photos";
        }
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
