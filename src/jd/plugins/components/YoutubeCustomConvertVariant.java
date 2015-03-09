package jd.plugins.components;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public class YoutubeCustomConvertVariant implements YoutubeVariantInterface {
    private String                  typeID;
    private VariantGroup            group;
    private double                  qualityRating;
    private String                  name;
    private String                  tag;
    private String                  extension;

    private YoutubeFilenameModifier nameModifier;
    private YoutubeConverter        converter;
    private String                  uniqueID;
    private YoutubeVariant          source;

    public static YoutubeVariantInterface parse(YoutubeCustomVariantStorable storable) {

        return new YoutubeCustomConvertVariant(storable.getUniqueID(), storable.getTypeID(), storable.getSource(), storable.getGroup(), storable.getQualityRating(), storable.getName(), storable.getNameTag(), storable.getExtension(), null, new YoutubeExternConverter(storable.getBinary(), storable.getParameters()));
    }

    public YoutubeCustomConvertVariant(String uniqueID, String typeID, YoutubeVariant source, VariantGroup group, double qualityRating, String variantName, String fileNameExtender, String fileExtension, YoutubeFilenameModifier nameModifier, YoutubeConverter converter) {
        this.typeID = typeID;
        this.group = group;
        this.qualityRating = qualityRating;
        this.name = variantName;
        this.tag = fileNameExtender;
        this.extension = fileExtension;
        this.source = source;
        this.nameModifier = nameModifier;
        this.converter = converter;
        this.uniqueID = uniqueID;

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof YoutubeCustomConvertVariant)) {
            return false;
        }
        return uniqueID.equals(((YoutubeCustomConvertVariant) obj).uniqueID);
    }

    @Override
    public int hashCode() {
        return _getUniqueId().hashCode();
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
    public String _getName() {
        return name;
    }

    @Override
    public Icon _getIcon() {
        return null;
    }

    @Override
    public String getFileExtension() {
        return extension;
    }

    @Override
    public String _getUniqueId() {
        return uniqueID;
    }

    @Override
    public String getMediaTypeID() {
        return group.name();
    }

    @Override
    public YoutubeITAG getiTagVideo() {
        return source.getiTagVideo();
    }

    public YoutubeVariant getSource() {
        return source;
    }

    @Override
    public YoutubeITAG getiTagAudio() {
        return source.getiTagAudio();
    }

    @Override
    public YoutubeITAG getiTagData() {
        return source.getiTagData();
    }

    @Override
    public double getQualityRating() {
        return qualityRating < 0 ? source.getQualityRating() : qualityRating;
    }

    @Override
    public String getTypeId() {
        return typeID;
    }

    @Override
    public DownloadType getType() {
        return source.getType();
    }

    @Override
    public VariantGroup getGroup() {
        return group;
    }

    @Override
    public void convert(DownloadLink downloadLink, PluginForHost plugin) throws Exception {
        if (converter != null) {
            converter.run(downloadLink, plugin);
        }
    }

    @Override
    public String getQualityExtension() {
        return tag;
    }

    @Override
    public String modifyFileName(String formattedFilename, DownloadLink link) {
        if (nameModifier != null) {
            return nameModifier.run(formattedFilename, link);
        }
        return formattedFilename;
    }

    @Override
    public List<File> listProcessFiles(DownloadLink link) {
        List<File> ret = new ArrayList<File>();
        return ret;
    }

    @Override
    public String _getExtendedName() {
        return name;
    }

}
