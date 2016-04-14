package org.jdownloader.plugins.components.youtube.variants;

import java.io.File;
import java.util.List;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.Log;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeHelper;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;
import org.jdownloader.plugins.components.youtube.variants.generics.AbstractGenericVariantInfo;

import jd.http.QueryInfo;
import jd.http.Request;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public abstract class AbstractVariant<Data extends AbstractGenericVariantInfo> implements LinkVariant {
    @Override
    public String toString() {
        return baseVariant.toString();
    }

    public static AbstractVariant get(String ytv) {
        if (ytv.matches("[A-Z0-9_]+")) {
            // old youtubevariant+try{
            VariantBase base = VariantBase.get(ytv);
            if (base != null) {

                AbstractVariant ret = null;
                switch (base.getGroup()) {
                case AUDIO:
                    ret = new AudioVariant(base);
                    ret.setJson("{}");
                    break;
                case DESCRIPTION:
                    ret = new DescriptionVariant();
                    ret.setJson("null");
                    break;
                case IMAGE:
                    ret = new ImageVariant(base);
                    ret.setJson("{}");
                    break;
                case SUBTITLES:
                    ret = new SubtitleVariant();

                    ret.setJson("{}");
                    break;
                case VIDEO:
                case VIDEO_3D:
                    ret = new VideoVariant(base);
                    ret.setJson("{}");
                    break;

                }

                return ret;
            }

        }
        YoutubeBasicVariantStorable storable = JSonStorage.restoreFromString(ytv, YoutubeBasicVariantStorable.TYPE);

        AbstractVariant ret = null;
        VariantBase base = null;
        try {
            base = VariantBase.valueOf(storable.getId());
            ;

        } catch (Throwable e) {
            try {
                base = VariantBase.COMPATIBILITY_MAP.get(base.getId());
                ;

            } catch (Throwable e2) {

            }
        }
        if (base == null) {
            return null;
        }

        switch (base.getGroup()) {
        case AUDIO:
            ret = new AudioVariant(base);
            break;
        case DESCRIPTION:
            ret = new DescriptionVariant();
            break;
        case IMAGE:
            ret = new ImageVariant(base);
            break;
        case SUBTITLES:
            ret = new SubtitleVariant();
            break;
        case VIDEO_3D:
        case VIDEO:
            ret = new VideoVariant(base);
            break;

        }
        ret.setJson(storable.getData());

        return ret;
    }

    abstract public void setJson(String jsonString);

    private VariantBase baseVariant;
    // private YoutubeBasicVariantStorable storable;
    private Data        genericInfo;

    // public YoutubeBasicVariant(YoutubeBasicVariantStorable base, YoutubeVariant baseVariant) {
    // this.baseVariant = baseVariant;
    // this.storable = base;
    // }

    protected AbstractVariant(VariantBase v) {
        baseVariant = v;
    }

    @Override
    public String _getUniqueId() {
        return baseVariant.name();
    }

    @Override
    public String _getExtendedName(Object caller) {
        return getBaseVariant().name().replace("_DASH", "").replace("_HLS", "").replace("HLS_", "").replace("_3D", "").replaceAll("_\\d+$", "").replace("_", ", ");
    }

    @Override
    public String _getTooltipDescription(Object caller) {
        return _getExtendedName(caller);
    }

    public String getFileExtension() {
        return baseVariant.getFileExtension();
    }

    public YoutubeITAG getiTagVideo() {
        return baseVariant.getiTagVideo();
    }

    public YoutubeITAG getiTagAudioOrVideoItagEquivalent() {
        if (baseVariant.getiTagAudio() != null) {
            return baseVariant.getiTagAudio();
        }

        return baseVariant.getiTagVideo();
    }

    public YoutubeITAG getiTagData() {
        return baseVariant.getiTagData();
    }

    public VariantBase getBaseVariant() {
        return baseVariant;
    }

    @Override
    public int hashCode() {
        return _getUniqueId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbstractVariant) {

            return StringUtils.equals(_getUniqueId(), ((AbstractVariant) obj)._getUniqueId());
        }
        return false;
    }

    public double getQualityRating() {
        return baseVariant.getQualityRating();
    }

    public String getTypeId() {
        return baseVariant.getTypeId();
    }

    public DownloadType getType() {
        return baseVariant.getType();
    }

    public VariantGroup getGroup() {
        return baseVariant.getGroup();
    }

    public void convert(DownloadLink downloadLink, PluginForHost plugin) throws Exception {
        baseVariant.convert(downloadLink, plugin);
    }

    public abstract String getFileNameQualityTag();

    public String modifyFileName(String formattedFilename, DownloadLink link) {

        return formattedFilename;
    }

    public boolean hasConverter(DownloadLink downloadLink) {
        return baseVariant.hasConverter(downloadLink);
    }

    public List<File> listProcessFiles(DownloadLink link) {
        return baseVariant.listProcessFiles(link);
    }

    public String getStorableString() {
        YoutubeBasicVariantStorable storable = new YoutubeBasicVariantStorable();
        storable.setId(baseVariant.name());
        storable.setData(JSonStorage.serializeToJson(genericInfo));
        return JSonStorage.serializeToJson(storable);
    }

    public static AbstractVariant get(DownloadLink downloadLink) {
        Object tmp = downloadLink.getTempProperties().getProperty(YoutubeHelper.YT_VARIANT, null);
        if (tmp != null && tmp instanceof AbstractVariant) {
            return (AbstractVariant) tmp;
        }
        AbstractVariant ret = get(downloadLink.getStringProperty(YoutubeHelper.YT_VARIANT));
        if (ret instanceof SubtitleVariant) {
            String old = downloadLink.getStringProperty(YoutubeHelper.YT_SUBTITLE_CODE);
            if (old != null) {
                try {
                    QueryInfo q = Request.parseQuery(old);
                    ((SubtitleVariant) ret).getGenericInfo().setBase(null);
                    ((SubtitleVariant) ret).getGenericInfo().setLanguage(q.get("lng"));
                    ((SubtitleVariant) ret).getGenericInfo().setSourceLanguage(q.get("src"));
                    ((SubtitleVariant) ret).getGenericInfo().setKind(q.get("kind"));
                    downloadLink.removeProperty(YoutubeHelper.YT_SUBTITLE_CODE);
                    YoutubeHelper.writeVariantToDownloadLink(downloadLink, ret);

                } catch (Throwable e) {
                    Log.log(e);
                }

            }
        }

        if (ret != null) {
            downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT, ret);
        }
        return ret;
    }

    public abstract String getFileNamePattern();
    //
    //
    // YoutubeConfig cfg = PluginJsonConfig.get(YoutubeConfig.class);
    //
    // String ret = null;
    // switch (baseVariant.getGroup()) {
    // case AUDIO:
    // ret = cfg.getAudioFilenamePattern();
    // break;
    // case DESCRIPTION:
    // ret = cfg.getDescriptionFilenamePattern();
    // break;
    // case IMAGE:
    // ret = cfg.getImageFilenamePattern();
    // break;
    // case SUBTITLES:
    // ret = cfg.getSubtitleFilenamePattern();
    // break;
    // case VIDEO:
    // ret = cfg.getVideoFilenamePattern();
    // break;
    // case VIDEO_3D:
    // ret = cfg.getVideo3DFilenamePattern();
    // break;
    // }
    // if (StringUtils.isEmpty(ret)) {
    // ret = cfg.getVideoFilenamePattern();
    // }
    // return ret;
    //
    // }

    public void setGenericInfo(Data genericInfo) {
        this.genericInfo = genericInfo;
    }

    public Data getGenericInfo() {
        return genericInfo;
    }

    public static AbstractVariant get(VariantBase base) {
        return get(base, null, null, null, null);
    }

    public static AbstractVariant get(VariantBase base, YoutubeClipData vid, List<YoutubeStreamData> audio, List<YoutubeStreamData> video, List<YoutubeStreamData> data) {

        AbstractVariant v = get(base.name());
        v.fill(vid, audio, video, data);

        return v;
    }

    protected abstract void fill(YoutubeClipData vid, List<YoutubeStreamData> audio, List<YoutubeStreamData> video, List<YoutubeStreamData> data);

    // public abstract void setAlternatives(List<String> altIds);
}
