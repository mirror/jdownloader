package org.jdownloader.plugins.components.youtube.variants;

import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.plugins.components.youtube.variants.generics.GenericAudioInfo;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.translate._JDT;

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
        if (audio != null && vid != null) {
            for (final YoutubeStreamData a : audio) {
                if (a.getBitrate() > 0 && vid.duration > 0 && a.getContentLength() > 0) {
                    final long abr = (8 * a.getContentLength()) / (1024l * vid.duration / 1000);
                    getGenericInfo().setaBitrate((int) abr);
                    break;
                }
            }
        }
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
        String id = TYPE_ID_PATTERN;
        id = id.replace("*CONTAINER*", getContainer().getLabel().toUpperCase(Locale.ENGLISH) + "");
        id = id.replace("*AUDIO_CODEC*", getAudioCodec().getLabel() + "");
        id = id.replace("*AUDIO_BITRATE*", getAudioBitrate().getKbit() + "");
        id = id.replace("*DEMUX*", (getBaseVariant().getiTagAudio() == null) ? "[DEMUX]" : "");
        switch (getiTagAudioOrVideoItagEquivalent().getAudioCodec()) {
        case AAC_SPATIAL:
        case VORBIS_SPATIAL:
            id = id.replace("*SPATIAL*", _JDT.T.YOUTUBE_surround());
            break;
        default:
            id = id.replace("*SPATIAL*", "");
        }
        id = id.replace(" - ", "-").trim().replaceAll("[ ]+", " ");
        return id;
    }

    @Override
    public AudioCodec getAudioCodec() {
        return getiTagAudioOrVideoItagEquivalent().getAudioCodec();
    }

    @Override
    public AudioBitrate getAudioBitrate() {
        final int bitRate = getGenericInfo().getaBitrate();
        if (bitRate > 0) {
            return AudioBitrate.getByInt(bitRate);
        } else {
            return getiTagAudioOrVideoItagEquivalent().getAudioBitrate();
        }
    }

    @Override
    public String getFileNamePattern() {
        return PluginJsonConfig.get(YoutubeConfig.class).getAudioFilenamePattern();
    }

    private static final String TYPE_ID_PATTERN = PluginJsonConfig.get(YoutubeConfig.class).getVariantNamePatternAudio();

    public String getTypeId() {
        String id = TYPE_ID_PATTERN;
        id = id.replace("*CONTAINER*", getContainer().name() + "");
        id = id.replace("*AUDIO_CODEC*", getAudioCodec() + "");
        id = id.replace("*AUDIO_BITRATE*", getAudioBitrate().getKbit() + "");
        id = id.replace("*DEMUX*", (getBaseVariant().getiTagAudio() == null) ? "DEMUX" : "");
        switch (getiTagAudioOrVideoItagEquivalent().getAudioCodec()) {
        case AAC_SPATIAL:
        case VORBIS_SPATIAL:
            id = id.replace("*SURROUND*", "Spatial");
            break;
        default:
            id = id.replace("*SURROUND*", "");
        }
        id = id.trim().replaceAll("\\s+", "_").toUpperCase(Locale.ENGLISH);
        return id;
    }
}
