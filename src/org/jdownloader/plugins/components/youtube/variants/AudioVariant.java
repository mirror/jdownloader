package org.jdownloader.plugins.components.youtube.variants;

import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;
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
                if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    if (audio.size() > 1) {
                        DebugMode.debugger();
                    } else if (getGenericInfo().getaId() != null && !StringUtils.equals(getGenericInfo().getaId(), a.getLngId())) {
                        DebugMode.debugger();
                    }
                }
                getGenericInfo().setaId(a.getLngId());
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

    protected String uniqueIDString = null;

    public synchronized String _getUniqueId() {
        if (uniqueIDString == null) {
            uniqueIDString = super._getUniqueId();
            final String aId = getGenericInfo().getaId();
            if (aId != null) {
                uniqueIDString += ".aid" + aId;
            }
        }
        return uniqueIDString;
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
        id = id.replace("*LNG*", StringUtils.valueOrEmpty(getAudioIdForPattern()));
        id = id.replace("*DEMUX*", (getBaseVariant().getiTagAudio() == null) ? "[DEMUX]" : "");
        switch (getiTagAudioOrVideoItagEquivalent().getAudioCodec()) {
        case AAC_SPATIAL:
        case VORBIS_SPATIAL:
        case OPUS_SPATIAL:
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

    private String getAudioIdForPattern() {
        final Locale locale = getAudioLocale();
        if (locale != null) {
            return locale.getDisplayName();
        } else {
            return null;
        }
    }

    public String getTypeId() {
        String id = TYPE_ID_PATTERN;
        id = id.replace("*CONTAINER*", getContainer().name() + "");
        id = id.replace("*AUDIO_CODEC*", getAudioCodec() + "");
        id = id.replace("*AUDIO_BITRATE*", getAudioBitrate().getKbit() + "");
        id = id.replace("*LNG*", StringUtils.valueOrEmpty(getAudioId()));
        id = id.replace("*DEMUX*", (getBaseVariant().getiTagAudio() == null) ? "DEMUX" : "");
        switch (getiTagAudioOrVideoItagEquivalent().getAudioCodec()) {
        case AAC_SPATIAL:
        case VORBIS_SPATIAL:
        case OPUS_SPATIAL:
            id = id.replace("*SURROUND*", "Spatial");
            break;
        default:
            id = id.replace("*SURROUND*", "");
        }
        id = id.trim().replaceAll("\\s+", "_").toUpperCase(Locale.ENGLISH);
        return id;
    }

    @Override
    public YoutubeITAG getAudioITAG() {
        return getiTagAudioOrVideoItagEquivalent();
    }

    @Override
    public String getAudioId() {
        return getGenericInfo().getaId();
    }

    @Override
    public Locale getAudioLocale() {
        return getGenericInfo()._getLocale();
    }
}
