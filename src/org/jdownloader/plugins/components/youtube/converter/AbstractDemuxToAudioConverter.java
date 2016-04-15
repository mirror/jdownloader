package org.jdownloader.plugins.components.youtube.converter;

import org.jdownloader.plugins.components.youtube.variants.VariantBase;

public abstract class AbstractDemuxToAudioConverter implements YoutubeConverter {

    public double getQualityRating(VariantBase base, double qualityRating) {

        // substract all video quality ratings from the demux rating and a demux penalty - we prefer audio streams that do not require
        // demuxing
        double penaltyForDemux = base.getiTagVideo().getVideoResolution().getHeight() / 100000000d;
        double ret = qualityRating - base.getiTagVideo().getVideoCodec().getRating() - base.getiTagVideo().getVideoFrameRate().getRating() - base.getiTagVideo().getVideoResolution().getRating() - penaltyForDemux;
        return ret;
    }

}
