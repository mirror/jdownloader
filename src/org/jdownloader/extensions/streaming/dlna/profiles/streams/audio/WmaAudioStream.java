package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

public class WmaAudioStream extends InternalAudioStream {
    /**
     * Example:
     * 
     * { "index": 0, "codec_name": "wmav2", "codec_long_name": "Windows Media Audio 2", "codec_type": "audio", "codec_time_base": "1/48000",
     * "codec_tag_string": "WMA2", "codec_tag": "0x32414d57", "sample_fmt": "s16", "sample_rate": "48000", "channels": 2, "bits_per_sample":
     * 0, "r_frame_rate": "0/0", "avg_frame_rate": "0/0", "time_base": "1/48000", "start_time": "0.000000", "duration": "3.669333",
     * "bit_rate": "97626", "nb_frames": "8", "tags": { "creation_time": "2008-12-19 17:14:58", "language": "eng", "handler_name":
     * "Apple Alias Data Handler" } },
     * 
     * 
     */
    private int[] wmaVersions;

    public int[] getWmaVersions() {
        return wmaVersions;
    }

    public WmaAudioStream() {
        super("audio/x-ms-wma");
        setCodecNames("wmav1", "wmav2", "wmav3");
        setCodecTags("WMA1", "WMA2", "WMA3");

    }

    public InternalAudioStream setWmaVersions(int... versions) {
        wmaVersions = versions;
        return this;
    }
}
