package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

public class AACAudioStream extends AbstractMpegAudioStream {
    /*
     * "index": 0, "codec_name": "aac", "codec_long_name": "AAC (Advanced Audio Coding)", "codec_type": "audio", "codec_time_base":
     * "1/44100", "codec_tag_string": "mp4a", "codec_tag": "0x6134706d", "sample_fmt": "s16", "sample_rate": "44100", "channels": 2,
     * "bits_per_sample": 0, "r_frame_rate": "0/0", "avg_frame_rate": "0/0", "time_base": "1/44100", "start_time": "0.000000", "duration":
     * "50.085442", "bit_rate": "3100", "nb_frames": "2157", "tags": { "creation_time": "2009-11-04 05:13:32", "language": "und",
     * "handler_name": "(C) 2007 Google Inc. v08.13.2007." }
     */
    public AACAudioStream() {
        super("audio/vnd.dlna.adts");
        setCodecNames("aac");
        setCodecTags("mp4a");
        mpegVersions = new int[] { 2, 4 };
        getProfileTags().add("lc");

    }

}
