package org.jdownloader.plugins.components.youtube.variants;

import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.TooltipInterface;
import org.jdownloader.plugins.components.youtube.YT_STATICS;

public enum FileContainer implements LabelInterface, TooltipInterface {
    // order is important!
    // videos
    MP4("mp4", "MP4 Video"),
    WEBM("webm", "Google WebM Video"),
    FLV("flv", "Flash FLV Video"),
    THREEGP("3gp", "3GP Video"),
    MKV("mkv", "Matroška MKV Video"),
    /// audio
    MP3("mp3", "MP3 Audio"),
    M4A("m4a", "M4A Audio"),
    AAC("aac", "AAC Audio"),
    OGG("ogg", "OGG Audio"),
    // rest
    JPG("jpg", "JPEG Image"),
    SRT("srt", "Subtitle"),
    TXT("txt", "Text");
    private String extension = null;
    private String longLabel;

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

    public static int getSortId(AbstractVariant v) {
        FileContainer res = v.getContainer();
        if (res == null) {
            return -1;
        }
        Object intObj = YT_STATICS.SORTIDS_FILE_CONTAINER.get(res);
        if (intObj == null) {
            return -1;
        }
        return ((Number) intObj).intValue();
    }
}
