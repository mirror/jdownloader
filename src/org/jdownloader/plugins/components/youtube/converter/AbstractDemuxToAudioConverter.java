package org.jdownloader.plugins.components.youtube.converter;

import org.jdownloader.plugins.components.youtube.variants.VariantBase;

public abstract class AbstractDemuxToAudioConverter implements YoutubeConverter {

    public double getQualityRating(VariantBase base, double qualityRating) {
        double penaltyForDemux = base.getiTagVideo().getVideoResolution().getHeight() / 100000000d;
        return base.getiTagVideo().getAudioCodec().getRating() + base.getiTagVideo().getAudioBitrate().getRating() - penaltyForDemux;
    }

}
