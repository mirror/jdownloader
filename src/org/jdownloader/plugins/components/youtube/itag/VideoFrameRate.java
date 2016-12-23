package org.jdownloader.plugins.components.youtube.itag;

import org.appwork.storage.config.annotations.IntegerInterface;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.components.youtube.YT_STATICS;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;

public enum VideoFrameRate implements IntegerInterface, LabelInterface {
    FPS_60(60),
    FPS_50(50),
    FPS_30(30),
    FPS_24(24),
    FPS_15(15),
    FPS_6(6);

    private final double fps;

    public final double getFps() {
        return fps;
    }

    private VideoFrameRate(double fps) {
        this.fps = fps;
    }

    public String getLabel() {
        return (int) Math.ceil(getFps()) + "fps";
    }

    @Override
    public int getInt() {
        return (int) Math.ceil(getFps());
    }

    public static VideoFrameRate getByVariant(AbstractVariant o1) {
        if (o1 instanceof VideoVariant) {
            return ((VideoVariant) o1).getiTagVideo().getVideoFrameRate();
        }
        return null;
    }

    public static int getSortId(AbstractVariant v) {
        final VideoFrameRate res = getByVariant(v);
        if (res == null) {
            return -1;
        }
        Object intObj = YT_STATICS.SORTIDS_VIDEO_FRAMERATE.get(res);
        if (intObj == null) {
            return -1;
        }
        return ((Number) intObj).intValue();
    }
}
