package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

public class AC3AudioStream extends InternalAudioStream {
    /*
     * "index": 1, "codec_name": "ac3", "codec_long_name": "ATSC A/52A (AC-3)", "codec_type": "audio", "codec_time_base": "1/48000",
     * "codec_tag_string": "[0][0][0][0]", "codec_tag": "0x0000", "sample_fmt": "s16", "sample_rate": "48000", "channels": 2,
     * "bits_per_sample": 0, "dmix_mode": "-1", "ltrt_cmixlev": "-1.000000", "ltrt_surmixlev": "-1.000000", "loro_cmixlev": "-1.000000",
     * "loro_surmixlev": "-1.000000", "id": "0x80", "r_frame_rate": "0/0", "avg_frame_rate": "0/0", "time_base": "1/90000", "start_time":
     * "0.440000", "duration": "5.568000", "bit_rate": "320000"
     */
    public AC3AudioStream(String contentType) {
        super(contentType);
        setCodecNames("ac3");

    }

}
