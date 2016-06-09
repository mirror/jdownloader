package org.jdownloader.plugins.components.youtube.itag;

import org.appwork.storage.config.annotations.IntegerInterface;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.components.youtube.YT_STATICS;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.ImageVariant;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;

public enum VideoResolution implements LabelInterface,IntegerInterface {
    // Order is default quality sort order
    P_4320(7680, 4320),
    P_2160(3840, 2160),
    P_1920(1080, 1920),
    P_1440(2560, 1440),
    P_1280(2560, 1280),
    P_1080(1920, 1080),
    P_720(1280, 720),
    P_480(640, 480),
    P_360(480, 360),
    P_270(480, 270),
    P_240(352, 240),
    P_180(320, 180),
    P_144(256, 144),
    P_90(120, 90),
    P_72(128, 72),;

    private int height;
    private int width;

    private VideoResolution(int width, int height) {
        this.height = height;
        this.width = width;

    }

    public int getHeight() {
        return height;
    }

    public String getLabel() {

        return height + "p";
    }

    public int getWidth() {
        return width;
    }

    @Override
    public int getInt() {
        return height;
    }

    public static VideoResolution getByHeight(int height) {
        for (VideoResolution r : values()) {
            if (r.getHeight() == height) {
                return r;
            }
        }
        return null;
    }

    public static VideoResolution getByVariant(AbstractVariant o1) {
        if (o1 instanceof VideoVariant) {
            return ((VideoVariant) o1).getVideoResolution();
        } else if (o1 instanceof ImageVariant) {
            return getByHeight(((ImageVariant) o1).getHeight());
        }
        return null;
    }

    public static int getSortId(AbstractVariant v) {

        VideoResolution res = getByVariant(v);
        if (res == null) {
            return -1;
        }
        Object intObj = YT_STATICS.SORTIDS_VIDEO_RESOLUTION.get(res);
        if (intObj == null) {
            return -1;
        }
        return ((Number) intObj).intValue();
    }

}
