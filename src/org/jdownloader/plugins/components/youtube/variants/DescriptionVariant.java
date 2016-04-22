package org.jdownloader.plugins.components.youtube.variants;

import java.util.List;

import javax.swing.Icon;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.variants.DescriptionVariant.DescriptionGenericInfo;
import org.jdownloader.plugins.components.youtube.variants.generics.AbstractGenericVariantInfo;
import org.jdownloader.plugins.config.PluginJsonConfig;

public class DescriptionVariant extends AbstractVariant<DescriptionGenericInfo> {
    public static class DescriptionGenericInfo extends AbstractGenericVariantInfo {

        public DescriptionGenericInfo() {

        }

    }

    public DescriptionVariant() {
        super(VariantBase.DESCRIPTION);

    }

    private static final Icon TEXT = new AbstractIcon(IconKey.ICON_TEXT, 16);

    @Override
    public Icon _getIcon(Object caller) {
        return TEXT;
    }

    public DescriptionVariant(String description) {
        this();
        setGenericInfo(new DescriptionGenericInfo());
    }

    @Override
    public void setJson(String jsonString) {
        // setGenericInfo(new DescriptionGenericInfo(JSonStorage.restoreFromString(jsonString, TypeRef.STRING)));
    }

    @Override
    protected void fill(YoutubeClipData vid, List<YoutubeStreamData> audio, List<YoutubeStreamData> video, List<YoutubeStreamData> data) {
    }

    @Override
    public String _getName(Object caller) {
        return _GUI.T.lit_desciption();
    }

    @Override
    public String getFileNameQualityTag() {
        return _GUI.T.YoutubeVariant_filenametag_DESCRIPTION();
    }

    @Override
    public String getFileNamePattern() {
        return PluginJsonConfig.get(YoutubeConfig.class).getDescriptionFilenamePattern();
    }

    @Override
    public String getTypeId() {
        return getBaseVariant().name();

    }

}
