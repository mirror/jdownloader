package org.jdownloader.plugins.components.youtube;

import org.appwork.storage.Storable;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.AudioVariant;
import org.jdownloader.plugins.components.youtube.variants.ImageVariant;
import org.jdownloader.plugins.components.youtube.variants.SubtitleVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;

public class VariantIDStorable implements Storable {
    private String group;
    private String container;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getProjection() {
        return projection;
    }

    public void setProjection(String projection) {
        this.projection = projection;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getaBitrate() {
        return aBitrate;
    }

    public void setaBitrate(int aBitrate) {
        this.aBitrate = aBitrate;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public String getvCodec() {
        return vCodec;
    }

    public void setvCodec(String vCodec) {
        this.vCodec = vCodec;
    }

    public String getaCodec() {
        return aCodec;
    }

    public void setaCodec(String aCodec) {
        this.aCodec = aCodec;
    }

    private String projection;
    private int    height;
    private int    aBitrate;
    private int    fps;
    private String vCodec;
    private String aCodec;
    private String base;

    public VariantIDStorable(/* Storable */) {
    }

    public VariantIDStorable(AbstractVariant variant) {
        this.container = variant.getContainer().name();
        this.group = variant.getGroup().name();
        if (variant instanceof AudioVariant) {
            AudioVariant avar = (AudioVariant) variant;
            this.aBitrate = avar.getiTagAudioOrVideoItagEquivalent().getAudioBitrate().getKbit();
            this.aCodec = avar.getiTagAudioOrVideoItagEquivalent().getAudioCodec().name();
        }
        if (variant instanceof VideoVariant) {
            VideoVariant vvar = (VideoVariant) variant;

            this.aBitrate = vvar.getiTagAudioOrVideoItagEquivalent().getAudioBitrate().getKbit();
            this.aCodec = vvar.getiTagAudioOrVideoItagEquivalent().getAudioCodec().name();
            this.vCodec = vvar.getiTagVideo().getVideoCodec().name();
            this.fps = vvar.getiTagVideo().getVideoFrameRate().getInt();
            this.height = vvar.getiTagVideo().getVideoResolution().getHeight();
            this.projection = vvar.getProjection().name();

        }
        if (variant instanceof SubtitleVariant) {

        }
        if (variant instanceof ImageVariant) {
            this.base = variant.getBaseVariant().name();
        }
    }

    public String createUniqueID() {
        StringBuilder sb = new StringBuilder();
        sb.append(aBitrate).append("_");
        sb.append(aCodec).append("_");
        sb.append(container).append("_");
        sb.append(fps).append("_");
        sb.append(group).append("_");
        sb.append(height).append("_");
        sb.append(projection).append("_");
        sb.append(vCodec).append("_");
        sb.append(base).append("_");
        return sb.toString();
    }

    public String createGroupingID() {
        if (VariantGroup.VIDEO.name().equals(getGroup())) {
            return getGroup() + "_" + projection;
        }
        return getGroup();
    }

}