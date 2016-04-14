package org.jdownloader.plugins.components.youtube.variants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.converter.YoutubeConverter;
import org.jdownloader.plugins.components.youtube.converter.YoutubeExternConverter;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;
import org.jdownloader.plugins.components.youtube.variants.generics.GenericCustomVariantInfo;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public class YoutubeCustomConvertVariant extends AbstractVariant<GenericCustomVariantInfo> {
    private String           typeID;
    private VariantGroup     group;
    private double           qualityRating;
    private String           name;
    private String           tag;
    private String           extension;

    private YoutubeConverter converter;
    private String           uniqueID;
    private VariantBase      source;

    @Override
    public String _getTooltipDescription(Object link) {
        return _getExtendedName(link);
    }

    public static AbstractVariant parse(YoutubeCustomVariantStorable storable) {

        return new YoutubeCustomConvertVariant(storable.getUniqueID(), storable.getTypeID(), storable.getSource(), storable.getGroup(), storable.getQualityRating(), storable.getName(), storable.getNameTag(), storable.getExtension(), new YoutubeExternConverter(storable.getBinary(), storable.getParameters()));
    }

    public YoutubeCustomConvertVariant(String uniqueID, String typeID, VariantBase source, VariantGroup group, double qualityRating, String variantName, String fileNameExtender, String fileExtension, YoutubeConverter converter) {
        super(null);
        this.typeID = typeID;
        this.group = group;
        this.qualityRating = qualityRating;
        this.name = variantName;
        this.tag = fileNameExtender;
        this.extension = fileExtension;
        this.source = source;

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
    public boolean hasConverter(DownloadLink downloadLink) {
        return converter != null;
    }

    @Override
    public String toString() {
        return uniqueID;
    }

    @Override
    public String _getName(Object caller) {
        return name;
    }

    @Override
    public Icon _getIcon(Object caller) {
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
    public YoutubeITAG getiTagVideo() {
        return source.getiTagVideo();
    }

    public VariantBase getSource() {
        return source;
    }

    @Override
    public YoutubeITAG getiTagAudioOrVideoItagEquivalent() {
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
    public String getFileNameQualityTag() {
        return tag;
    }

    @Override
    public String modifyFileName(String formattedFilename, DownloadLink link) {

        return formattedFilename;
    }

    @Override
    public List<File> listProcessFiles(DownloadLink link) {
        List<File> ret = new ArrayList<File>();
        return ret;
    }

    @Override
    public String _getExtendedName(Object caller) {
        return name;
    }

    @Override
    public String getStorableString() {
        return null;
    }

    @Override
    public void setJson(String jsonString) {
    }

    @Override
    protected void fill(YoutubeClipData vid, List<YoutubeStreamData> audio, List<YoutubeStreamData> video, List<YoutubeStreamData> data) {
    }

    @Override
    public String getFileNamePattern() {
        return PluginJsonConfig.get(YoutubeConfig.class).getAudioFilenamePattern();
    }

}
