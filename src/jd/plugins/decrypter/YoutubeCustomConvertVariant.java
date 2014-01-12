package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import jd.plugins.DownloadLink;

public class YoutubeCustomConvertVariant implements YoutubeVariantInterface {
    private String                  typeID;
    private VariantGroup            group;
    private double                  qualityRating;
    private String                  name;
    private String                  tag;
    private String                  extension;
    private YoutubeITAG             audioTag;
    private YoutubeITAG             videoTag;
    private YoutubeFilenameModifier nameModifier;
    private YoutubeConverter        converter;
    private String                  uniqueID;
    private DownloadType            downloadType;

    public YoutubeCustomConvertVariant(String uniqueID, String typeID, DownloadType dltype, VariantGroup group, double qualityRating, String variantName, String fileNameExtender, String fileExtension, YoutubeITAG audio, YoutubeITAG video, YoutubeFilenameModifier nameModifier, YoutubeConverter converter) {
        this.typeID = typeID;
        this.group = group;
        this.qualityRating = qualityRating;
        this.name = variantName;
        this.tag = fileNameExtender;
        this.extension = fileExtension;
        this.audioTag = audio;
        this.videoTag = video;
        this.nameModifier = nameModifier;
        this.converter = converter;
        this.uniqueID = uniqueID;
        this.downloadType = dltype;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof YoutubeCustomConvertVariant)) return false;
        return uniqueID.equals(((YoutubeCustomConvertVariant) obj).uniqueID);
    }

    @Override
    public int hashCode() {
        return getUniqueId().hashCode();
    }

    @Override
    public boolean hasConverer(DownloadLink downloadLink) {
        return converter != null;
    }

    @Override
    public String toString() {
        return uniqueID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getFileExtension() {
        return extension;
    }

    @Override
    public String getUniqueId() {
        return uniqueID;
    }

    @Override
    public String getMediaTypeID() {
        return group.name();
    }

    @Override
    public YoutubeITAG getiTagVideo() {
        return videoTag;
    }

    @Override
    public YoutubeITAG getiTagAudio() {
        return audioTag;
    }

    @Override
    public YoutubeITAG getiTagData() {
        return null;
    }

    @Override
    public double getQualityRating() {
        return qualityRating;
    }

    @Override
    public String getTypeId() {
        return typeID;
    }

    @Override
    public DownloadType getType() {
        return downloadType;
    }

    @Override
    public VariantGroup getGroup() {
        return group;
    }

    @Override
    public void convert(DownloadLink downloadLink) {
        if (converter != null) converter.run(downloadLink);
    }

    @Override
    public String getQualityExtension() {
        return tag;
    }

    @Override
    public String modifyFileName(String formattedFilename, DownloadLink link) {
        if (nameModifier != null) return nameModifier.run(formattedFilename, link);
        return formattedFilename;
    }

    @Override
    public List<File> listProcessFiles(DownloadLink link) {
        List<File> ret = new ArrayList<File>();

        return ret;
    }

}
