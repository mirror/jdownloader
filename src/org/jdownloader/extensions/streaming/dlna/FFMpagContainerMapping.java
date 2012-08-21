package org.jdownloader.extensions.streaming.dlna;

import java.util.HashMap;

import org.appwork.exceptions.WTFException;

public enum FFMpagContainerMapping {

    CT_UNKNOWN(DlnaClass.UNKNOWN, null),
    CT_IMAGE(DlnaClass.IMAGE, "image2"), /* for PNG and JPEG */
    CT_ASF(DlnaClass.AUDIO, "asf"), /* usually for WMA/WMV */
    CT_AMR(DlnaClass.AUDIO, "amr"),
    CT_AAC(DlnaClass.AUDIO, "aac"),
    CT_AC3(DlnaClass.AUDIO, "ac3"),
    CT_MP3(DlnaClass.AUDIO, "mp3"),
    CT_WAV(DlnaClass.AUDIO, "wav"),
                                   
    CT_MOV(DlnaClass.AUDIO_VIDEO, "mov,mp4,m4a,3gp,3g2,mj2"),
    CT_3GP(DlnaClass.AUDIO_VIDEO, null),
    CT_MP4(DlnaClass.AUDIO_VIDEO, null),
    CT_FF_MPEG(DlnaClass.AUDIO_VIDEO, "mpeg"), /* FFMPEG "mpeg" */
    CT_FF_MPEG_TS(DlnaClass.AUDIO_VIDEO, "mpegts"), /* FFMPEG "mpegts" */
    CT_MPEG_ELEMENTARY_STREAM(DlnaClass.AUDIO_VIDEO, null),
    CT_MPEG_PROGRAM_STREAM(DlnaClass.AUDIO_VIDEO, null),
    CT_MPEG_TRANSPORT_STREAM(DlnaClass.AUDIO_VIDEO, null),
    CT_MPEG_TRANSPORT_STREAM_DLNA(DlnaClass.AUDIO_VIDEO, null),
    CT_MPEG_TRANSPORT_STREAM_DLNA_NO_TS(DlnaClass.AUDIO_VIDEO, null);
    private String    avf;
    private DlnaClass dlnaClass;

    public DlnaClass getDlnaClass() {
        return dlnaClass;
    }

    private static HashMap<String, FFMpagContainerMapping> AVF_MAPPING = new HashMap<String, FFMpagContainerMapping>();
    static {
        // map all types to get faster access
        for (FFMpagContainerMapping ct : FFMpagContainerMapping.values()) {
            if (ct.avf != null) {
                if (AVF_MAPPING.put(ct.avf, ct) != null) { throw new WTFException("MAPPING DUPES"); }
            }
        }
    }

    private FFMpagContainerMapping(DlnaClass clazz, String avfRepresentation) {
        this.avf = avfRepresentation;
        dlnaClass = clazz;

    }

    public String getAvf() {
        return avf;
    }

    public static FFMpagContainerMapping getTypeByFFMpegOutput(String formatString) {
        return AVF_MAPPING.get(formatString);

    }
}
