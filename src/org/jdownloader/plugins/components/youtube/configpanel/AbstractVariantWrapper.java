package org.jdownloader.plugins.components.youtube.configpanel;

import org.jdownloader.plugins.components.youtube.Projection;
import org.jdownloader.plugins.components.youtube.VariantIDStorable;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.AudioInterface;
import org.jdownloader.plugins.components.youtube.variants.ImageVariant;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;

public class AbstractVariantWrapper {
    private VariantIDStorable blackListEntry;

    public AbstractVariantWrapper(AbstractVariant variant) {
        this.variant = variant;
        blackListEntry = new VariantIDStorable(variant);
    }

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    final public AbstractVariant variant;

    public int getWidth() {
        if (variant instanceof VideoVariant) {
            return ((VideoVariant) variant).getVideoWidth();
        }
        if (variant instanceof ImageVariant) {
            return ((ImageVariant) variant).getWidth();

        }
        return -1;
    }

    public int getAudioBitrate() {
        if (variant instanceof AudioInterface) {
            return ((AudioInterface) variant).getAudioBitrate().getKbit();
        }
        return -1;
    }

    public int getFramerate() {
        if (variant instanceof VideoVariant) {
            return ((VideoVariant) variant).getVideoFrameRate();
        }
        return -1;
    }

    public String getVideoCodec() {
        if (variant instanceof VideoVariant) {
            return ((VideoVariant) variant).getVideoCodec().getLabel();
        }
        return "";
    }

    public String getAudioCodec() {
        if (variant instanceof AudioInterface) {
            return ((AudioInterface) variant).getAudioCodec().getLabel();
        }
        return "";
    }

    public int getHeight() {
        if (variant instanceof VideoVariant) {
            return ((VideoVariant) variant).getVideoHeight();
        }
        if (variant instanceof ImageVariant) {
            return ((ImageVariant) variant).getHeight();

        }
        return -1;
    }

    public VariantIDStorable getVariableIDStorable() {
        return blackListEntry;
    }

    public Projection getProjection() {
        if (variant instanceof VideoVariant) {
            return ((VideoVariant) variant).getProjection();
        }
        return null;
    }

}
