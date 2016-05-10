package org.jdownloader.plugins.components.youtube.variants.generics;

import org.appwork.storage.Storable;

public class GenericAudioInfo extends AbstractGenericVariantInfo implements Storable {
    public GenericAudioInfo(/* storable */) {
    }

    private int aBitrate = -1;

    public int getaBitrate() {
        return aBitrate;
    }

    public void setaBitrate(int bitrate) {
        this.aBitrate = bitrate;
    }
}
