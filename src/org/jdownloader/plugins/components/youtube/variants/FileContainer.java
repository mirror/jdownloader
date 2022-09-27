package org.jdownloader.plugins.components.youtube.variants;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.TooltipInterface;
import org.jdownloader.plugins.components.youtube.YT_STATICS;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;

public enum FileContainer implements LabelInterface, TooltipInterface {
    // order is important!
    // videos
    MP4("mp4", "MP4 Video"),
    MKV("mkv", "Matroška MKV Video"),
    WEBM("webm", "Google WebM Video"),
    FLV("flv", "Flash FLV Video"),
    THREEGP("3gp", "3GP Video"),
    // / audio
    M4A("m4a", "M4A Audio"),
    AAC("aac", "AAC Audio"),
    OPUS("opus", "Opus Audio"),
    OGG("ogg", "Vorbis Audio"),
    MP3("mp3", "MP3 Audio"),
    // rest
    JPG("jpg", "JPEG Image"),
    SRT("srt", "Subtitle"),
    TXT("txt", "Text");
    private final String extension;
    private final String longLabel;

    private FileContainer(String fileExtension, String longLabel) {
        this.extension = fileExtension;
        this.longLabel = longLabel;
    }

    public String getLabel() {
        return extension;
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public String getTooltip() {
        return longLabel;
    }

    public static FileContainer getVideoContainer(YoutubeITAG dashVideo, YoutubeITAG dashAudio) {
        return getAllVideoContainer(dashVideo, dashAudio)[0];
    }

    public static FileContainer[] getAllVideoContainer(YoutubeITAG dashVideo, YoutubeITAG dashAudio) {
        switch (dashVideo.getVideoCodec()) {
        case AV1:
            return new FileContainer[] { FileContainer.MP4, FileContainer.MKV };
        default:
            break;
        }
        switch (dashVideo.getRawContainer()) {
        case DASH_VIDEO:
            switch (dashVideo.getVideoCodec()) {
            case H264:
                return new FileContainer[] { FileContainer.MP4, FileContainer.MKV };
            case AV1:
                return new FileContainer[] { FileContainer.MP4, FileContainer.MKV };
            case H263:
                throw new WTFException("Unsupported:" + dashVideo.getVideoCodec());
            default:
                final boolean isOpus = dashAudio.getAudioCodec().name().startsWith("OPUS");
                final boolean isVorbis = dashAudio.getAudioCodec().name().startsWith("VORBIS");
                final boolean isVPx = dashVideo.getVideoCodec().name().startsWith("VP");
                if (isVPx && (isOpus || isVorbis)) {
                    // Only VP8 or VP9 video and Vorbis or Opus audio and WebVTT subtitles are supported for WebM.
                    return new FileContainer[] { FileContainer.WEBM, FileContainer.MKV };
                }
                return new FileContainer[] { FileContainer.MKV };
            }
        case FLV:
            return new FileContainer[] { FileContainer.FLV };
        case MP4:
            return new FileContainer[] { FileContainer.MP4 };
        case THREEGP:
            return new FileContainer[] { FileContainer.THREEGP };
        case WEBM:
            return new FileContainer[] { FileContainer.WEBM };
        default:
            throw new WTFException("Unsupported:" + dashVideo.getVideoCodec());
        }
    }

    public static FileContainer getAudioContainer(YoutubeITAG dashVideo, YoutubeITAG audioCodec) {
        switch (audioCodec.getAudioCodec()) {
        case AAC:
        case AAC_SPATIAL:
            return FileContainer.AAC;
        case OPUS:
        case OPUS_SPATIAL:
            return FileContainer.OPUS;
        case VORBIS:
        case VORBIS_SPATIAL:
            return FileContainer.OGG;
        case MP3:
            return FileContainer.MP3;
        default:
            return FileContainer.MKV;
        }
    }

    public static int getSortId(AbstractVariant v) {
        final FileContainer res = v.getContainer();
        if (res == null) {
            return -1;
        } else {
            final Object intObj = YT_STATICS.SORTIDS_FILE_CONTAINER.get(res);
            if (intObj == null) {
                return -1;
            } else {
                return ((Number) intObj).intValue();
            }
        }
    }
}
