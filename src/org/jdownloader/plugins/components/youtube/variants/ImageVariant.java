package org.jdownloader.plugins.components.youtube.variants;

import java.util.List;

import javax.swing.Icon;

import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.variants.generics.ImageGenericInfo;
import org.jdownloader.plugins.config.PluginJsonConfig;

public class ImageVariant extends AbstractVariant<ImageGenericInfo> {
    private static final Icon IMAGE = new AbstractIcon(IconKey.ICON_IMAGE, 16);

    public ImageVariant(VariantBase base) {
        super(base);
    }

    @Override
    public void setJson(String jsonString) {
        setGenericInfo(new ImageGenericInfo());
    }

    @Override
    protected void fill(YoutubeClipData vid, List<YoutubeStreamData> audio, List<YoutubeStreamData> video, List<YoutubeStreamData> data) {
    }

    @Override
    public String getFileNamePattern() {
        return PluginJsonConfig.get(YoutubeConfig.class).getImageFilenamePattern();
    }

    @Override
    public Icon _getIcon(Object caller) {
        return IMAGE;
    }

    @Override
    public String _getName(Object caller) {
        return getBaseVariant().getiTagData().getImageQuality().getLocaleName();
    }

    @Override
    public String getFileNameQualityTag() {

        return getBaseVariant().getiTagData().getImageQuality().getLocaleTag();
    }

    @Override
    public String getTypeId() {
        return getBaseVariant().name();
    }

}
