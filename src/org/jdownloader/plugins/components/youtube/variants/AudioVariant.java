package org.jdownloader.plugins.components.youtube.variants;

import java.util.List;

import javax.swing.Icon;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.plugins.components.youtube.variants.generics.GenericAudioInfo;
import org.jdownloader.plugins.config.PluginJsonConfig;

public class AudioVariant extends AbstractVariant<GenericAudioInfo> implements AudioInterface {

    public AudioVariant(VariantBase base) {
        super(base);

    }

    @Override
    public void setJson(String jsonString) {
        setGenericInfo(JSonStorage.restoreFromString(jsonString, new TypeRef<GenericAudioInfo>() {

        }));
    }

    @Override
    protected void fill(YoutubeClipData vid, List<YoutubeStreamData> audio, List<YoutubeStreamData> video, List<YoutubeStreamData> data) {
    }

    @Override
    public AudioCodec getAudioContainer() {
        if (StringUtils.equalsIgnoreCase(getBaseVariant().getFileExtension(), "m4a")) {
            return AudioCodec.M4A;
        }
        return getiTagAudioOrVideoItagEquivalent().getAudioCodec();
    }

    private static final Icon AUDIO = new AbstractIcon(IconKey.ICON_AUDIO, 16);

    @Override
    public Icon _getIcon(Object caller) {
        return AUDIO;
    }

    @Override
    public String getFileNameQualityTag() {

        return getAudioBitrate().getKbit() + "kbits " + getAudioCodec().getLabel();

    }

    @Override
    public String _getName(Object caller) {

        return _GUI.T.YoutubeVariant_name_generic_audio(getAudioBitrate().getKbit() + "kbit", getAudioContainer().getLabel());

    }

    @Override
    public AudioCodec getAudioCodec() {
        return getiTagAudioOrVideoItagEquivalent().getAudioCodec();
    }

    @Override
    public AudioBitrate getAudioBitrate() {
        return getiTagAudioOrVideoItagEquivalent().getAudioBitrate();
    }

    @Override
    public String getFileNamePattern() {
        return PluginJsonConfig.get(YoutubeConfig.class).getAudioFilenamePattern();
    }

}
