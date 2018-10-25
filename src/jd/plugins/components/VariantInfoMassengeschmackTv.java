package jd.plugins.components;

import javax.swing.Icon;

import org.appwork.storage.Storable;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

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

    /** Returns quality name similar to website but WITHOUT the filesize e.g. 'SD 432p 768x432 (434 MiB)' */
    public String getVariantName() {
        String variantName = null;
        if (resolution != null) {
            final String height = new Regex(resolution, "(\\d+)$").getMatch(0);
            if (resolution.contains("x1080") || resolution.contains("x720")) {
                variantName = "HD " + height + "p " + resolution;
            } else {
                variantName = "SD 432p " + resolution;
            }
            variantName = this.qualityName + " " + this.resolution;
        } else {
            variantName = "AUDIO";
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
        /* TODO */
        if (this.qualityName == null) {
            /** TODO */
            return "WTF";
        } else {
            return this.qualityName;
        }
    }

    public String getQualityName() {
        return this.qualityName;
    }

    @Override
    public String _getName(Object caller) {
        return this.qualityName;
    }

    @Override
    public Icon _getIcon(Object caller) {
        return new AbstractIcon(IconKey.ICON_VIDEO, 16);
    }

    @Override
    public String _getTooltipDescription(Object caller) {
        return qualityName;
    }
}
