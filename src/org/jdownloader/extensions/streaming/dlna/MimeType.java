package org.jdownloader.extensions.streaming.dlna;

public enum MimeType {
    // images
    IMAGE_JPEG("image/jpeg"),
    IMAGE_PNG("image/png"),
    // audio
    AUDIO_3GP("audio/3gpp"),
    AUDIO_ADTS("audio/vnd.dlna.adts"),
    AUDIO_ATRAC("audio/x-sony-oma"),
    AUDIO_DOLBY_DIGITAL("audio/vnd.dolby.dd-raw"),
    AUDIO_LPCM("audio/L16"),
    AUDIO_MPEG("audio/mpeg"),
    AUDIO_MPEG_4("audio/mp4"),
    AUDIO_WMA("audio/x-ms-wma"),
    // video
    VIDEO_3GP("video/3gpp"),
    VIDEO_ASF("video/x-ms-asf"),
    VIDEO_MPEG("video/mpeg"),
    VIDEO_MPEG_4("video/mp4"),
    VIDEO_MPEG_TS("video/vnd.dlna.mpeg-tts"),
    VIDEO_WMV("video/x-ms-wmv"),
    AUDIO_LPCM_L16_44100_1("audio/L16;rate=44100;channels=1"),
    AUDIO_LPCM_L16_44100_2("audio/L16;rate=44100;channels=2"),
    AUDIO_LPCM_L16_48000_1("audio/L16;rate=48000;channels=1"),
    AUDIO_LPCM_L16_48000_2("audio/L16;rate=48000;channels=2");

    private String label;

    public String getLabel() {
        return label;
    }

    private MimeType(String label) {
        this.label = label;
    }
}
