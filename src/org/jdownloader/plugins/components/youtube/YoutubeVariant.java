package org.jdownloader.plugins.components.youtube;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public enum YoutubeVariant implements YoutubeVariantInterface {
    OLD(null, YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "aac", null, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        @Override
        public String _getName() {
            return _GUI.T.YoutubeVariant_name_AAC_128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI.T.YoutubeVariant_filenametag_AAC_128();
        }
    }
    // ###APPEND###
    ;

    private YoutubeConverter                           converter;
    final private String                               fileExtension;
    private YoutubeFilenameModifier                    filenameModifier;
    final private YoutubeVariantInterface.VariantGroup group;
    final private String                               id;

    final private YoutubeITAG                          iTagAudio;

    final private YoutubeITAG                          iTagData;
    final private YoutubeITAG                          iTagVideo;
    private final double                               qualityRating;
    final private YoutubeVariantInterface.DownloadType type;

    private YoutubeVariant(final String id, final YoutubeVariantInterface.VariantGroup group, final YoutubeVariantInterface.DownloadType type, final String fileExtension, final YoutubeITAG video, final YoutubeITAG audio, YoutubeITAG data, YoutubeFilenameModifier filenameModifier, YoutubeConverter converter) {
        this.group = group;
        this.id = id;

        this.fileExtension = fileExtension;
        if (type == null) {
            throw new NullPointerException();
        }
        this.type = type;
        this.iTagVideo = video;
        this.iTagAudio = audio;
        this.qualityRating = 0d + (video != null ? video.getQualityRating() : 0) + (audio != null ? audio.getQualityRating() : 0) + (data != null ? data.getQualityRating() : 0);
        this.iTagData = data;
        this.converter = converter;
        this.filenameModifier = filenameModifier;
    }

    public static HashMap<String, YoutubeVariant> COMPATIBILITY_MAP = new HashMap<String, YoutubeVariant>();

    static {
        // ###APPEND_COMPATIBILITY_MAP###
    }

    @Override
    public String _getExtendedName() {
        // String nadsme = _getName();
        String ret = null;
        switch (getGroup()) {
        case AUDIO:
            if (getiTagVideo() != null) {
                ret = getFileExtension().toUpperCase(Locale.ENGLISH) + "-" + _GUI.T.lit_audio() + " [" + getAudioQuality() + "-" + getAudioCodec() + "-DEMUX]";

            } else {
                ret = getFileExtension().toUpperCase(Locale.ENGLISH) + "-" + _GUI.T.lit_audio() + " [" + getAudioQuality() + "-" + getAudioCodec() + "]";

            }
            break;
        case IMAGE:
            ret = getFileExtension().toUpperCase(Locale.ENGLISH) + "-" + _GUI.T.lit_image() + " [" + getResolution() + "]";
            break;
        case SUBTITLES:
            ret = _getName();
            break;
        case VIDEO:
            ret = getFileExtension().toUpperCase(Locale.ENGLISH) + "-" + _GUI.T.lit_video() + "[" + getResolution() + "-" + getVideoCodec() + "_" + getAudioQuality() + "-" + getAudioCodec() + "]";
            break;
        case VIDEO_3D:
            ret = getFileExtension().toUpperCase(Locale.ENGLISH) + "-" + _GUI.T.lit_3d_video() + "[" + getResolution() + "-" + getVideoCodec() + "_" + getAudioQuality() + "-" + getAudioCodec() + "]";
            break;
        }

        ret += "(" + name() + " A:" + getiTagAudio() + " V:" + getiTagVideo() + ") " + getQualityRating();
        return _getName() + " - " + ret;
    }

    private static final Icon VIDEO = new AbstractIcon(IconKey.ICON_VIDEO, 16);
    private static final Icon AUDIO = new AbstractIcon(IconKey.ICON_AUDIO, 16);
    private static final Icon IMAGE = new AbstractIcon(IconKey.ICON_IMAGE, 16);
    private static final Icon TEXT  = new AbstractIcon(IconKey.ICON_TEXT, 16);

    public Icon _getIcon() {
        final VariantGroup lGroup = getGroup();
        if (lGroup != null) {
            switch (lGroup) {
            case AUDIO:
                return AUDIO;
            case VIDEO:
            case VIDEO_3D:
                return VIDEO;
            case IMAGE:
                return IMAGE;
            default:
                return TEXT;
            }
        }
        return null;
    }

    public abstract String _getName();

    public String _getUniqueId() {
        return name();
    }

    public void convert(DownloadLink downloadLink, PluginForHost plugin) throws Exception {
        if (converter != null) {
            converter.run(downloadLink, plugin);
        }
    }

    public String getAudioCodec() {
        if (getiTagAudio() != null) {
            return getiTagAudio().getCodecAudio();
        }
        if (getiTagVideo() != null) {
            return getiTagVideo().getCodecAudio();
        }
        return null;
    }

    public String getAudioQuality() {
        if (getiTagAudio() != null) {
            return getiTagAudio().getQualityAudio();
        }
        if (getiTagVideo() != null) {
            return getiTagVideo().getQualityAudio();
        }
        return null;
    }

    public String getFileExtension() {
        return this.fileExtension;
    }

    public YoutubeVariantInterface.VariantGroup getGroup() {
        return this.group;
    }

    public YoutubeITAG getiTagAudio() {
        return this.iTagAudio;
    }

    public YoutubeITAG getiTagData() {
        return iTagData;
    }

    public YoutubeITAG getiTagVideo() {
        return this.iTagVideo;
    }

    public String getMediaTypeID() {
        return getGroup().name();
    }

    public abstract String getQualityExtension();

    public double getQualityRating() {
        return this.qualityRating;
    }

    public String getResolution() {
        String ret = null;
        if (getiTagData() != null) {

            ret = getiTagData().getQualityVideo();
        }
        if (getiTagVideo() != null) {
            ret = getiTagVideo().getQualityVideo();
        }
        return ret;
    }

    public YoutubeVariantInterface.DownloadType getType() {
        return this.type;
    }

    public String getTypeId() {
        if (CFG_GUI.EXTENDED_VARIANT_NAMES_ENABLED.isEnabled()) {
            return name();
        }
        if (this.id == null) {
            return this.name();
        }
        return this.id;
    }

    public String getVideoCodec() {
        if (getiTagData() != null) {
            return getiTagData().getCodecVideo();
        }
        if (getiTagVideo() != null) {
            return getiTagVideo().getCodecVideo();
        }
        return null;
    }

    @Override
    public boolean hasConverter(DownloadLink downloadLink) {
        return converter != null;
    }

    /**
     * returns true if this variant requires a video tool like ffmpge for muxing, demuxing or container converting
     *
     * @return
     */
    public boolean isVideoToolRequired() {
        if (iTagVideo != null && iTagAudio != null) {
            return true;
        }
        if (iTagVideo != null && iTagVideo.name().contains("DASH")) {
            return true;
        }
        if (iTagAudio != null && iTagAudio.name().contains("DASH")) {
            return true;
        }
        if (converter != null) {
            if (converter instanceof YoutubeMp4ToAACAudio) {
                return true;
            }
            if (converter instanceof YoutubeMp4ToM4aAudio) {
                return true;
            }
            if (converter instanceof YoutubeFlvToMp3Audio) {
                return true;
            }
            if (converter instanceof YoutubeExternConverter) {
                return true;
            }

        }
        return false;
    }

    @Override
    public String _getTooltipDescription() {
        return _getExtendedName() + " [" + name() + "]";
    }

    @Override
    public List<File> listProcessFiles(DownloadLink link) {
        ArrayList<File> ret = new ArrayList<File>();

        return ret;
    }

    public String modifyFileName(String formattedFilename, DownloadLink link) {
        if (filenameModifier != null) {
            return filenameModifier.run(formattedFilename, link);
        }
        return formattedFilename;
    }

}