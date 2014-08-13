package org.jdownloader.downloader.hds;

public class AudioTag {

    // 0 = Linear PCM, platform endian
    public static final int PCM_PE           = 0;
    // 1 = ADPCM
    public static final int ADPCM            = 1;
    // 2 = MP3
    public static final int MP3              = 2;
    // 3 = Linear PCM, little endian
    public static final int PCM_LE           = 3;
    // 4 = Nellymoser 16 kHz mono
    public static final int NELLYMOSER_16KHZ = 4;
    // 5 = Nellymoser 8 kHz mono
    public static final int NELLYMOSER_8KHZ  = 5;
    // 6 = Nellymoser
    public static final int NELLYMOSER       = 6;
    // 7 = G.711 A-law logarithmic PCM
    public static final int PCM_A_LAW_LOG    = 7;
    // 8 = G.711 mu-law logarithmic PCM
    public static final int PCM_MU_LAW_LOG   = 8;
    // 9 = reserved

    // 10 = AAC
    public static final int AAC              = 10;
    // 11 = Speex
    public static final int SPEEX            = 11;
    // 14 = MP3 8 kHz
    public static final int MP3_8KHZ         = 14;
    // 15 = Device-specific sound
    public static final int DEV_SPEC         = 15;
    // Formats 7, 8, 14, and 15 are reserved.
    // AAC is supported in Flash Player 9,0,115,0 and higher.
    // Speex is supported in Flash Player 10 and higher.
}
