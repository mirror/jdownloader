package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;

public class AVCStream extends Mpeg4VideoStream {
    public static final String BASELINE_CONSTRAINED = "Baseline Constrained";

    /*
     * Example:
     * 
     * "codec_name": "h264", "codec_long_name": "H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10", "profile": "Baseline", "codec_type": "video",
     * "codec_time_base": "1/50", "codec_tag_string": "H264", "codec_tag": "0x34363248", "width": 704, "height": 288, "has_b_frames": 0,
     * "sample_aspect_ratio": "0:1", "display_aspect_ratio": "0:1", "pix_fmt": "yuv420p", "level": 21, "is_avc": "0", "nal_length_size":
     * "0", "r_frame_rate": "25/1", "avg_frame_rate": "25/1", "time_base": "1/25", "start_time": "0.000000", "duration": "9.960000",
     * "nb_frames": "249"
     */
    public AVCStream() {
        setContentType("video/x-h264");
        setCodecNames("h264");
        // 0x31637661 avc1 profile HIGH
        // 0x34363248 H264 profile Baseline
        setCodecTags("H264", "avc1");
        getProfileTags().add(BASELINE_CONSTRAINED);
        mpegVersions = new int[] { 4 };
        systemStream = false;

    }

    private String[] level;

    public AVCStream setLevel(String[] strings) {
        this.level = strings;
        return this;
    }

}
