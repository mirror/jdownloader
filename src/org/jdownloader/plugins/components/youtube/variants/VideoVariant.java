package org.jdownloader.plugins.components.youtube.variants;

import java.util.List;

import javax.swing.Icon;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.Log;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoContainer;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.variants.generics.GenericVideoInfo;
import org.jdownloader.plugins.config.PluginJsonConfig;

public class VideoVariant extends AbstractVariant<GenericVideoInfo> implements VideoInterface, AudioInterface {
    public VideoVariant(VariantBase base) {
        super(base);
    }

    @Override
    public VariantGroup getGroup() {
        if (getGenericInfo().isThreeD()) {
            return VariantGroup.VIDEO_3D;
        }
        return VariantGroup.VIDEO;
    }

    @Override
    public String _getExtendedName(Object caller) {
        if (getGroup() == VariantGroup.VIDEO_3D) {
            return "3D, " + super._getExtendedName(caller);
        }
        return super._getExtendedName(caller);
    }

    @Override
    public void setJson(String jsonString) {
        setGenericInfo(JSonStorage.restoreFromString(jsonString, new TypeRef<GenericVideoInfo>() {

        }));
    }

    private static final Icon VIDEO = new AbstractIcon(IconKey.ICON_VIDEO, 16);

    public String getTypeId() {
        if (getGroup() == VariantGroup.VIDEO_3D) {
            return super.getTypeId() + "_3D";
        }
        return super.getTypeId();
    }

    public AudioCodec getAudioContainer() {
        if (StringUtils.equalsIgnoreCase(getBaseVariant().getFileExtension(), "m4a")) {
            return AudioCodec.M4A;
        }
        return getiTagAudioOrVideoItagEquivalent().getAudioCodec();
    }

    @Override
    protected void fill(YoutubeClipData vid, List<YoutubeStreamData> audio, List<YoutubeStreamData> video, List<YoutubeStreamData> data) {

        if (vid != null && vid.is3D()) {
            //
            getGenericInfo().setThreeD(true);

        }
        if (getBaseVariant().name().contains("_3D")) {
            getGenericInfo().setThreeD(true);
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

    public VideoContainer getContainer() {
        return getiTagVideo().getVideoContainer();
    }

    @Override
    public Icon _getIcon(Object caller) {
        return VIDEO;
    }

    @Override
    public String _getName(Object caller) {

        int res = getVideoHeight();
        int fps = getVideoFrameRate();
        AudioCodec audio = getAudioCodec();
        if (getGroup() == VariantGroup.VIDEO_3D) {
            return _GUI.T.YoutubeVariant_name_generic_video2("3D " + res + "p " + fps + "fps", getVideoContainer().getLabel(caller), getAudioBitrate().getLabel(), audio.getLabel());
        }
        return _GUI.T.YoutubeVariant_name_generic_video2(res + "p " + fps + "fps", getVideoContainer().getLabel(caller), getAudioBitrate().getLabel(), audio.getLabel());

    }

    @Override
    public VideoContainer getVideoContainer() {
        return getiTagVideo().getVideoContainer();
    }

    @Override
    public String getFileNamePattern() {
        if (getGroup() == VariantGroup.VIDEO_3D) {
            return PluginJsonConfig.get(YoutubeConfig.class).getVideo3DFilenamePattern();
        }
        return PluginJsonConfig.get(YoutubeConfig.class).getVideoFilenamePattern();
    }

    @Override
    public String getFileNameQualityTag() {

        switch (getGroup()) {

        case VIDEO_3D:

            return getVideoHeight() + "p " + getVideoFrameRate() + "fps" + " 3D";
        default:
            return getVideoHeight() + "p " + getVideoFrameRate() + "fps";
        }

    }

    public AudioCodec getAudioCodec() {
        return getiTagAudioOrVideoItagEquivalent().getAudioCodec();
    }

    public AudioBitrate getAudioBitrate() {
        return getiTagAudioOrVideoItagEquivalent().getAudioBitrate();
    }

    @Override
    public VideoCodec getVideoCodec() {
        if (getiTagVideo() == null) {
            return null;
        }
        return getiTagVideo().getVideoCodec();
    }

    @Override
    public VideoResolution getVideoResolution() {
        if (getiTagVideo() == null) {
            return null;
        }
        return getiTagVideo().getVideoResolution();
    }

    @Override
    public int getVideoWidth() {
        int width = getGenericInfo().getHeight();
        if (width < 3) {
            width = getiTagVideo().getVideoResolution().getWidth();
        }
        return width;
    }
}
