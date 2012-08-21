package org.jdownloader.extensions.streaming.dlna.profiles.video;

import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.Break;

public class FrameRate {
    private Break min;
    private Break max;

    public FrameRate(Break min, Break max) {
        this.min = min;
        this.max = max;
    }

    public FrameRate(Break max) {
        this(new Break(0, 1), max);
    }

    public FrameRate(int c, int d) {
        this(new Break(c, d));
    }

    public static final FrameRate FPS_15    = new FrameRate(15, 1);
    public static final FrameRate FPS_25    = new FrameRate(25, 1);
    public static final FrameRate FPS_30    = new FrameRate(30, 1);
    public static final FrameRate FPS_50    = new FrameRate(50, 1);
    public static final FrameRate FPS_60    = new FrameRate(60, 1);
    public static final FrameRate FPS_59_94_60000_1001 = new FrameRate(60000, 1001);
    public static final FrameRate FPS_29_97_30000_1001 = new FrameRate(30000, 1001);
    public static final FrameRate FPS_23_97_24000_1001 = new FrameRate(24000, 1001);
    public static final FrameRate FPS_24    = new FrameRate(24, 1);
}
