package jd.plugins.decrypter;

import java.io.File;
import java.util.List;

import jd.plugins.DownloadLink;

import org.jdownloader.controlling.linkcrawler.LinkVariant;

public interface YoutubeVariantInterface extends LinkVariant {

    public static enum DownloadType {
        DASH_AUDIO,
        DASH_VIDEO,

        VIDEO,
        /**
         * Static videos have a static url in YT_STATIC_URL
         */
        IMAGE,
        SUBTITLES,

    }

    public static enum VariantGroup {
        AUDIO,
        VIDEO,
        VIDEO_3D,
        IMAGE,
        SUBTITLES
    }

    String getFileExtension();

    String getUniqueId();

    String getMediaTypeID();

    YoutubeITAG getiTagVideo();

    YoutubeITAG getiTagAudio();

    YoutubeITAG getiTagData();

    double getQualityRating();

    String getTypeId();

    DownloadType getType();

    VariantGroup getGroup();

    void convert(DownloadLink downloadLink);

    String getQualityExtension();

    String modifyFileName(String formattedFilename, DownloadLink link);

    boolean hasConverer(DownloadLink downloadLink);

    List<File> listProcessFiles(DownloadLink link);

}
