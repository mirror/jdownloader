package org.jdownloader.extensions.streaming.dlna;

public enum Extensions {
    IMAGE_JPG("jpg"),
    IMAGE_JPEG("jpeg"),
    IMAGE_JPE("jpe"),
    IMAGE_PNG("png"),
    AUDIO_VIDEO_MP4("mp4"),
    AUDIO_M4A("m4a"),
    AUDIO_VIDEO_GP3("gp3"),
    AUDIO_VIDEO_QT("qt"),
    AUDIO_VIDEO_MOV("mov"),
    AUDIO_ADTS("adts"),
    AUDIO_AAC("aac"),
    AUDIO_VIDEO_AC3("ac3"),
    AUDIO_AMR("amr"),
    AUDIO_MP3("mp3"),
    AUDIO_ID3("id3"),
    AUDIO_ASF("asf"),
    AUDIO_WMA("wma"),
    AUDIO_WAV("wav"),
    AUDIO_LPCM("lpcm"),
    AUDIO_AIFF("aiff"),
    AUDIO_PCM("pcm"),
    AUDIO_VIDEO_MPG("mpg"),
    AUDIO_VIDEO_MPEG("mpeg"),
    AUDIO_VIDEO_MPE("mpe"),
    AUDIO_VIDEO_M1V("m1v"),
    AUDIO_MP2("mp2"),
    AUDIO_VIDEO_M2V("m2v"),
    AUDIO_VIDEO_MP2P("mp2p"),
    AUDIO_VIDEO_MP2T("mp2t"),
    AUDIO_VIDEO_TS("ts"),
    AUDIO_VIDEO_PS("ps"),
    AUDIO_VIDEO_PES("pes"),
    AUDIO_VIDEO_ASF("asf"),
    AUDIO_VIDEO_WMV("wmv");
    private final String extension;

    public String getExtension() {
        return extension;
    }

    private Extensions(String extension) {
        this.extension = extension;
    }

}
