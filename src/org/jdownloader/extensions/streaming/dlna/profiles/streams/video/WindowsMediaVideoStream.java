package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;

import org.jdownloader.extensions.streaming.dlna.MimeType;

public class WindowsMediaVideoStream extends InternalVideoStream {

    /**
     * Example: "codec_name": "wmv3", "codec_long_name": "Windows Media Video 9", "profile": "Main", "codec_type": "video",
     * "codec_time_base": "1/2500", "codec_tag_string": "WMV3", "codec_tag": "0x33564d57", "width": 1440, "height": 1080, "has_b_frames": 0,
     * "sample_aspect_ratio": "0:1", "display_aspect_ratio": "0:1", "pix_fmt": "yuv420p", "level": -99, "r_frame_rate": "25/1",
     * "avg_frame_rate": "25/1", "time_base": "1/2500", "start_time": "0.000000", "duration": "3.480000", "bit_rate": "15966733",
     * "nb_frames": "87", "tags": { "creation_time": "2008-12-19 17:14:58", "language": "eng", "handler_name": "Apple Alias Data Handler" }
     * 
     * @author Thomas
     * 
     */
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
        setCodecNames("wmv1", "wmv2", "wmv3");
        setCodecTags("WMV1", "WMV2", "WMV3");

        this.profile = p;
        this.level = l;
    }

}
