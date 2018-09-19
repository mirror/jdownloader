package org.jdownloader.plugins.components.youtube.itag;

import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.TooltipInterface;
import org.jdownloader.plugins.components.youtube.YT_STATICS;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;

public enum VideoCodec implements LabelInterface, TooltipInterface {
    H264("AVC H.264", "H264"),
    VP9_BETTER_PROFILE_1("Google VP9 Medium Quality Profile", "VP9 MQ"),
    VP9_BETTER_PROFILE_2("Google VP9 High Quality Profile", "VP9 HQ"),
    VP9("Google VP9", "VP9"),
    VP9_WORSE_PROFILE_1("Google VP9 Low Quality Profile", "VP9 LQ"),
    H263("AVC H.263", "H263"),
    VP8("Google VP8", "VP8"),
    VP9_HDR("Google VP9 HDR", "VP9 HDR"),
    AV1("AOMedia Video 1", "AV1");
    private String label;
    private String labelLong;

    public String getLabel() {
        return label;
    }

    private VideoCodec(String labelLong, String label) {
        this.label = label;
        this.labelLong = labelLong;
    }

    public String getLabelLong() {
        return labelLong;
    }

    @Override
    public String getTooltip() {
        return labelLong;
    }

    public static VideoCodec getByVariant(AbstractVariant o1) {
        if (o1 instanceof VideoVariant) {
            return ((VideoVariant) o1).getVideoCodec();
        }
        return null;
    }

    public static int getSortId(AbstractVariant v) {
        VideoCodec res = getByVariant(v);
        if (res == null) {
            return -1;
        }
        Object intObj = YT_STATICS.SORTIDS_VIDEO_CODEC.get(res);
        if (intObj == null) {
            return -1;
        }
        return ((Number) intObj).intValue();
    }
}
