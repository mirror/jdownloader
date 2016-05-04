package org.jdownloader.plugins.components.youtube.variants;

import java.util.List;

import javax.swing.Icon;

import org.appwork.exceptions.WTFException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
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
        return _GUI.T.Youtube_imagevariant_name(getBaseVariant().getiTagData().getImageQuality().getLocaleName());
    }

    // @Override
    // public double getQualityRating() {
    // return super.getQualityRating();
    // }

    @Override
    public String getFileNameQualityTag() {

        return getBaseVariant().getiTagData().getImageQuality().getLocaleTag();
    }

    @Override
    public String getTypeId() {
        return getBaseVariant().name();
    }

    public int getWidth() {
        switch (getBaseVariant()) {
        case IMAGE_HQ:
            return 480;
        case IMAGE_LQ:
            return 120;
        case IMAGE_MAX:
            return 1280;
        case IMAGE_MQ:
            return 320;

        default:
            throw new WTFException();
        }

    }

    public int getHeight() {
        switch (getBaseVariant()) {
        case IMAGE_HQ:
            return 360;
        case IMAGE_LQ:
            return 90;
        case IMAGE_MAX:
            return 720;
        case IMAGE_MQ:
            return 180;

        default:
            throw new WTFException();
        }
    }

}
