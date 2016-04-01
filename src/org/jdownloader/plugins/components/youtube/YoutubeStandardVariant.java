package org.jdownloader.plugins.components.youtube;

import java.io.File;
import java.util.List;

import javax.swing.Icon;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public class YoutubeStandardVariant implements YoutubeVariantInterface {

    @Override
    public String _getUniqueId() {
        return null;
    }

    @Override
    public String _getName() {
        return null;
    }

    @Override
    public Icon _getIcon() {
        return null;
    }

    @Override
    public String _getExtendedName() {
        return null;
    }

    @Override
    public String _getTooltipDescription() {
        return null;
    }

    @Override
    public String getFileExtension() {
        return null;
    }

    @Override
    public String getMediaTypeID() {
        return null;
    }

    @Override
    public YoutubeITAG getiTagVideo() {
        return null;
    }

    @Override
    public YoutubeITAG getiTagAudio() {
        return null;
    }

    @Override
    public YoutubeITAG getiTagData() {
        return null;
    }

    @Override
    public double getQualityRating() {
        return 0;
    }

    @Override
    public String getTypeId() {
        return null;
    }

    @Override
    public DownloadType getType() {
        return null;
    }

    @Override
    public VariantGroup getGroup() {
        return null;
    }

    @Override
    public void convert(DownloadLink downloadLink, PluginForHost plugin) throws Exception {
    }

    @Override
    public String getQualityExtension() {
        return null;
    }

    @Override
    public String modifyFileName(String formattedFilename, DownloadLink link) {
        return null;
    }

    @Override
    public boolean hasConverter(DownloadLink downloadLink) {
        return false;
    }

    @Override
    public List<File> listProcessFiles(DownloadLink link) {
        return null;
    }

}
