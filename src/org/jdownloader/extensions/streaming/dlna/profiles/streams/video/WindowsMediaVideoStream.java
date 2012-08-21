package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;

import org.jdownloader.extensions.streaming.dlna.MimeType;

public class WindowsMediaVideoStream extends InternalVideoStream {
    public static enum Profile {
        SIMPLE,
        MAIN,
        ADVANCED
    }

    public static enum Level {
        LOW,
        MEDIUM,
        HIGH,
        L0,
        L1,
        L2,
        L3,
        L4
    }

    private Profile profile;
    private Level   level;

    public WindowsMediaVideoStream(Profile p, Level l) {
        super(MimeType.VIDEO_WMV.getLabel());
        this.profile = p;
        this.level = l;
    }

}
