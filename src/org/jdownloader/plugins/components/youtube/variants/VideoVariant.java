package org.jdownloader.plugins.components.youtube.variants;

import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.logging2.extmanager.Log;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.youtube.Projection;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.variants.generics.GenericVideoInfo;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.translate._JDT;

public class VideoVariant extends AbstractVariant<GenericVideoInfo> implements VideoInterface, AudioInterface {
    public VideoVariant(VariantBase base) {
        super(base);
    }

    // @Override
    // public VariantGroup getGroup() {
    // switch (getGenericInfo().getProjection()) {
    // case ANAGLYPH_3D:
    // return VariantGroup.VIDEO_3D;
    // case SPHERICAL:
    // return VariantGroup.VIDEO_360;
    // case SPHERICAL_3D:
    // return VariantGroup.VIDEO_3D_360;
    // case NORMAL:
    // return VariantGroup.VIDEO;
    // default:
    // throw new WTFException();
    // }
    //
    // }
    @Override
    public String createAdvancedName() {
        switch (getProjection()) {
        case SPHERICAL:
            return "360° VR, " + super.createAdvancedName();
        case ANAGLYPH_3D:
            return "3D, " + super.createAdvancedName();
        case SPHERICAL_3D:
            return "360° VR, 3D, " + super.createAdvancedName();
        }
        return super.createAdvancedName();
    }

    @Override
    public void setJson(String jsonString) {
        setGenericInfo(JSonStorage.restoreFromString(jsonString, new TypeRef<GenericVideoInfo>() {
        }));
    }

    private static final Icon   VIDEO           = new AbstractIcon(IconKey.ICON_VIDEO, 16);
    private static final String TYPE_ID_PATTERN = PluginJsonConfig.get(YoutubeConfig.class).getVariantNamePatternVideo();

    public String getTypeId() {
        String id = TYPE_ID_PATTERN;
        id = id.replace("*CONTAINER*", getBaseVariant().getContainer().name() + "");
        id = id.replace("*HEIGHT*", getVideoHeight() + "");
        id = id.replace("*FPS*", getVideoFrameRate() + "");
        id = id.replace("*AUDIO_CODEC*", getAudioCodec() + "");
        id = id.replace("*AUDIO_BITRATE*", getAudioBitrate().getKbit() + "");
        switch (getProjection()) {
        case NORMAL:
            id = id.replace("*360*", "");
            id = id.replace("*3D*", "");
            break;
        case SPHERICAL:
            id = id.replace("*360*", "360°");
            id = id.replace("*3D*", "");
            break;
        case ANAGLYPH_3D:
            id = id.replace("*3D*", "3D");
            id = id.replace("*360*", "");
            break;
        case SPHERICAL_3D:
            id = id.replace("*3D*", "3D");
            id = id.replace("*360*", "360°");
            break;
        }
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

    public Projection getProjection() {
        return getGenericInfo().getProjection();
    }

    @Override
    protected void fill(YoutubeClipData vid, List<YoutubeStreamData> audio, List<YoutubeStreamData> video, List<YoutubeStreamData> data) {
        if (vid != null) {
            //
            getGenericInfo().setProjection(vid.getProjection());
        }
        if (getBaseVariant().name().contains("_3D")) {
            getGenericInfo().setProjection(Projection.ANAGLYPH_3D);
        }
        if (video != null) {
            for (YoutubeStreamData stream : video) {
                if (stream.getHeight() > 0) {
                    getGenericInfo().setHeight(stream.getHeight());
                }
                if (stream.getWidth() > 0) {
                    getGenericInfo().setWidth(stream.getWidth());
                }
                if (stream.getFps() != null) {
                    try {
                        int intf = Integer.parseInt(new Regex(stream.getFps(), "(\\d+)").getMatch(0));
                        getGenericInfo().setFps(intf);
                    } catch (Throwable e) {
                        Log.log(e);
                    }
                }
            }
        }
    }

    @Override
    public int getVideoHeight() {
        int height = getGenericInfo().getHeight();
        if (height < 3) {
            height = getiTagVideo().getVideoResolution().getHeight();
        }
        return height;
    }

    @Override
    public int getVideoFrameRate() {
        int fps = getGenericInfo().getFps();
        if (fps < 3) {
            fps = (int) Math.ceil(getiTagVideo().getVideoFrameRate().getFps());
        }
        return fps;
    }

    @Override
    public Icon _getIcon(Object caller) {
        return VIDEO;
    }

    // @Override
    // public double getQualityRating() {
    // double base = getBaseVariant().getQualityRating();
    // if (getGenericInfo().getHeight() > 0) {
    // // we got the actuall height. let's use it for quality rating
    // base -= getBaseVariant().getiTagVideo().getVideoResolution().getRating();
    // base += Math.min(getGenericInfo().getWidth(), getGenericInfo().getHeight());
    // }
    // return base;
    // }
    @Override
    public String getStandardGroupingID() {
        return getGroup().name() + "_" + getProjection().name();
    }

    @Override
    public String _getName(Object caller) {
        String id = TYPE_ID_PATTERN;
        id = id.replace("*CONTAINER*", getBaseVariant().getContainer().name() + "");
        id = id.replace("*HEIGHT*", getVideoHeight() + "");
        id = id.replace("*FPS*", getVideoFrameRate() + "");
        id = id.replace("*AUDIO_CODEC*", getAudioCodec().getLabel() + "");
        id = id.replace("*AUDIO_BITRATE*", getAudioBitrate().getKbit() + "");
        switch (getProjection()) {
        case NORMAL:
            id = id.replace("*360*", "");
            id = id.replace("*3D*", "");
            break;
        case SPHERICAL:
            id = id.replace("*360*", "[360°]");
            id = id.replace("*3D*", "");
            break;
        case ANAGLYPH_3D:
            id = id.replace("*3D*", "[3D]");
            id = id.replace("*360*", "");
            break;
        case SPHERICAL_3D:
            id = id.replace("*3D*", "[3D]");
            id = id.replace("*360*", "[360°]");
            break;
        }
        switch (getiTagAudioOrVideoItagEquivalent().getAudioCodec()) {
        case AAC_SPATIAL:
        case VORBIS_SPATIAL:
            id = id.replace("*SPATIAL*", _JDT.T.YOUTUBE_surround());
            break;
        default:
            id = id.replace("*SPATIAL*", "");
        }
        id = id.trim().replace(" - ", "-").replaceAll("[ ]+", " ");
        return id.trim();
    }

    @Override
    public String getFileNamePattern() {
        return PluginJsonConfig.get(YoutubeConfig.class).getVideoFilenamePattern();
    }

    @Override
    public String getFileNameQualityTag() {
        switch (getProjection()) {
        case SPHERICAL:
            return getVideoHeight() + "p " + getVideoFrameRate() + "fps" + " 360VR";
        case ANAGLYPH_3D:
            return getVideoHeight() + "p " + getVideoFrameRate() + "fps" + " 3D";
        case SPHERICAL_3D:
            return getVideoHeight() + "p " + getVideoFrameRate() + "fps" + " 360VR 3D";
        default:
            return getVideoHeight() + "p " + getVideoFrameRate() + "fps";
        }
    }

    public AudioCodec getAudioCodec() {
        return getiTagAudioOrVideoItagEquivalent().getAudioCodec();
    }

    public AudioBitrate getAudioBitrate() {
        final int bitRate = getGenericInfo().getaBitrate();
        if (bitRate > 0) {
            return AudioBitrate.getByInt(bitRate);
        } else {
            return getiTagAudioOrVideoItagEquivalent().getAudioBitrate();
        }
    }

    @Override
    public VideoCodec getVideoCodec() {
        if (getiTagVideo() == null) {
            return null;
        } else {
            return getiTagVideo().getVideoCodec();
        }
    }

    @Override
    public VideoResolution getVideoResolution() {
        if (getiTagVideo() == null) {
            return null;
        } else {
            return getiTagVideo().getVideoResolution();
        }
    }

    @Override
    public int getVideoWidth() {
        int width = getGenericInfo().getWidth();
        if (width < 3) {
            width = getiTagVideo().getVideoResolution().getWidth();
        }
        return width;
    }
}
