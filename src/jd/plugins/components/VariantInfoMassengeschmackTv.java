package jd.plugins.components;

import javax.swing.Icon;

import org.appwork.storage.Storable;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.controlling.linkcrawler.LinkVariant;

import jd.parser.Regex;

public class VariantInfoMassengeschmackTv implements Storable, LinkVariant {
    private String url;
    private String qualityName;
    private String resolution;
    private String filesize;
    private long   filesizeL;

    public String getFilesize() {
        return this.filesize;
    }

    public long getFilesizeLong() {
        return this.filesizeL;
    }

    public void setFilesize(final String filesize) {
        this.filesize = filesize;
        this.filesizeL = SizeFormatter.getSize(filesize);
    }

    public void setFilesize(final long filesize) {
        this.filesizeL = filesize;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setResolution(final String resolution) {
        this.resolution = resolution;
    }

    public void setQualityName(final String qualityName) {
        this.qualityName = qualityName;
    }

    /** Returns quality name similar to website e.g. 'SD 432p 768x432 (434 MiB)' */
    public String getVariantName() {
        String variantName = null;
        if (resolution != null) {
            variantName = this.qualityName + " " + this.resolution;
        } else {
            variantName = this.qualityName;
        }
        if (this.filesize != null) {
            variantName += " (" + this.filesize + ")";
        }
        return variantName;
    }

    /** Returns quality name without filesize e.g. 'SD 432p 768x432' */
    public String getVariantNameWithoutFilesize() {
        String variantName = null;
        if (resolution != null) {
            variantName = this.qualityName + " " + this.resolution;
        } else {
            variantName = this.qualityName;
        }
        return variantName;
    }

    public VariantInfoMassengeschmackTv(/* Storable */) {
    }

    public VariantInfoMassengeschmackTv(final String url, final String qualityName) {
        this.url = url;
        this.qualityName = qualityName;
    }

    public VariantInfoMassengeschmackTv(final String url, final String qualityName, final String filesize) {
        this.url = url;
        this.qualityName = qualityName;
        this.filesize = filesize;
    }

    public VariantInfoMassengeschmackTv(final String url, final String qualityName, final long filesize) {
        this.url = url;
        this.qualityName = qualityName;
        this.filesizeL = filesize;
    }

    @Override
    public String _getUniqueId() {
        if (this.qualityName == null) {
            /** TODO */
            return "WTF";
        } else {
            return this.qualityName;
        }
    }

    public int _getQuality() {
        final String xx_p = new Regex(this.qualityName, "(\\d+)").getMatch(0);
        if (xx_p != null) {
            return Integer.parseInt(xx_p);
        }
        return -1;
    }

    @Override
    public String _getName(Object caller) {
        return this.qualityName;
    }

    @Override
    public Icon _getIcon(Object caller) {
        return null;
    }

    @Override
    public String _getTooltipDescription(Object caller) {
        return qualityName;
    }
}
