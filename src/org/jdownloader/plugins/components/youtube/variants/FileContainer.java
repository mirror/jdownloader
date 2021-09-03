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
    WEBM("webm", "Google WebM Video"),
    FLV("flv", "Flash FLV Video"),
    THREEGP("3gp", "3GP Video"),
    MKV("mkv", "Matroška MKV Video"),
    // / audio
    MP3("mp3", "MP3 Audio"),
    M4A("m4a", "M4A Audio"),
    AAC("aac", "AAC Audio"),
    OGG("ogg", "OGG Audio"),
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
        switch (dashVideo.getVideoCodec()) {
        case AV1:
            return FileContainer.MP4;
        default:
            break;
        }
        switch (dashVideo.getRawContainer()) {
        case DASH_VIDEO:
            switch (dashVideo.getVideoCodec()) {
            case H264:
                return FileContainer.MP4;
            case AV1:
                return FileContainer.MP4;
            case H263:
                throw new WTFException("Unsupported:" + dashVideo.getVideoCodec());
            default:
                switch (dashAudio.getAudioCodec()) {
                case AAC:
                case AAC_SPATIAL:
                case MP3:
                    return FileContainer.MKV;
                default:
                    return FileContainer.WEBM;
                }
            }
        case FLV:
            return FileContainer.FLV;
        case MP4:
            return FileContainer.MP4;
        case THREEGP:
            return FileContainer.THREEGP;
        case WEBM:
            return FileContainer.WEBM;
        default:
            throw new WTFException("Unsupported:" + dashVideo.getVideoCodec());
        }
    }

    public static FileContainer getAudioContainer(YoutubeITAG dashVideo, YoutubeITAG audioCodec) {
        switch (audioCodec.getRawContainer()) {
        case DASH_AUDIO:
            switch (audioCodec.getAudioCodec()) {
            case AAC:
            case AAC_SPATIAL:
                return FileContainer.AAC;
            default:
                return FileContainer.OGG;
            }
        default:
            switch (audioCodec.getAudioCodec()) {
            case AAC:
            case AAC_SPATIAL:
                return FileContainer.AAC;
            case MP3:
                return FileContainer.MP3;
            default:
                return FileContainer.OGG;
            }
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
