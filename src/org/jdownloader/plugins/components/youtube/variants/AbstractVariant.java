package org.jdownloader.plugins.components.youtube.variants;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import jd.config.Property;
import jd.http.Request;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.SimpleMapper;
import org.appwork.storage.simplejson.JSonFactory;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.Log;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.plugins.components.youtube.Projection;
import org.jdownloader.plugins.components.youtube.YT_STATICS;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeHelper;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.itag.QualitySortIdentifier;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;
import org.jdownloader.plugins.components.youtube.variants.generics.AbstractGenericVariantInfo;

public abstract class AbstractVariant<Data extends AbstractGenericVariantInfo> implements LinkVariant, Comparable {
    private static ArrayList<AbstractVariant> VARIANTS_LIST;

    @Override
    public String toString() {
        return baseVariant.toString();
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof AbstractVariant)) {
            return -1;
        }
        final AbstractVariant o1 = this;
        final AbstractVariant o2 = (AbstractVariant) o;
        for (final QualitySortIdentifier q : YT_STATICS.SORTIDS) {
            final int ret = q.compare(o1, o2);
            if (ret != 0) {
                return ret;
            }
        }
        return 0;
    }

    public static AbstractVariant get(final String ytv) {
        try {
            if (ytv != null && ytv.matches("[A-Z0-9_]+")) {
                // old youtubevariant+try{
                final VariantBase base = VariantBase.get(ytv);
                if (base != null) {
                    final AbstractVariant ret;
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
                        ret = new VideoVariant(base);
                        ret.setJson("{}");
                        break;
                    default:
                        ret = null;
                        break;
                    }
                    return ret;
                }
            }
            if (ytv == null) {
                return null;
            } else if (!ytv.contains("{")) {
                return null;
            }
            final YoutubeBasicVariantStorable storable = JSonStorage.restoreFromString(ytv, YoutubeBasicVariantStorable.TYPE);
            VariantBase base = null;
            try {
                base = VariantBase.valueOf(storable.getId());
            } catch (Throwable e) {
                try {
                    base = VariantBase.COMPATIBILITY_MAP.get(storable.getId());
                } catch (Throwable e2) {
                }
            }
            if (base == null) {
                return null;
            }
            final AbstractVariant ret;
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
            case VIDEO:
                ret = new VideoVariant(base);
                break;
            default:
                ret = null;
                break;
            }
            if (ret != null) {
                ret.setJson(storable.getData());
                return ret;
            } else {
                return null;
            }
        } catch (Throwable e) {
            Log.log(e);
            return null;
        }
    }

    abstract public void setJson(String jsonString);

    protected final VariantBase baseVariant;
    // private YoutubeBasicVariantStorable storable;
    private Data                genericInfo;
    private VariantInfo         variantInfo;

    // public YoutubeBasicVariant(YoutubeBasicVariantStorable base, YoutubeVariant baseVariant) {
    // this.baseVariant = baseVariant;
    // this.storable = base;
    // }
    protected AbstractVariant(VariantBase v) {
        baseVariant = v;
    }

    @Override
    public String _getUniqueId() {
        final VariantBase variantBase = this.getBaseVariant();
        if (variantBase != null) {
            return variantBase._getUniqueId();
        } else {
            throw new WTFException();
        }
    }

    public String createAdvancedName() {
        return getBaseVariant().name().replace("_DASH", "").replace("_HLS", "").replace("HLS_", "").replace("_3D", "").replaceAll("_\\d+$", "").replace("_", ", ");
    }

    @Override
    public String _getTooltipDescription(Object caller) {
        return createAdvancedName();
    }

    public FileContainer getContainer() {
        return getBaseVariant().getContainer();
    }

    public YoutubeITAG getiTagVideo() {
        return baseVariant.getiTagVideo();
    }

    public YoutubeITAG getiTagAudioOrVideoItagEquivalent() {
        if (baseVariant.getiTagAudio() != null) {
            return baseVariant.getiTagAudio();
        } else {
            return baseVariant.getiTagVideo();
        }
    }

    public YoutubeITAG getiTagData() {
        return baseVariant.getiTagData();
    }

    public VariantBase getBaseVariant() {
        return baseVariant;
    }

    public String getVariantDetails() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.toString());
        sb.append("[");
        final FileContainer container = getContainer();
        if (container != null) {
            sb.append("%Container:").append(container.name());
        }
        final YoutubeITAG video = getiTagVideo();
        if (video != null) {
            sb.append("%Video:").append(video.name()).append("(").append(video.getITAG()).append(")");
        }
        final YoutubeITAG audio = getiTagAudioOrVideoItagEquivalent();
        if (audio != null) {
            sb.append("%Audio:").append(audio.name()).append("(").append(audio.getITAG()).append(")");
        }
        final YoutubeITAG data = getiTagData();
        if (data != null) {
            sb.append("%Data:").append(data.name()).append("(").append(data.getITAG()).append(")");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return _getUniqueId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbstractVariant) {
            AbstractVariant var = (AbstractVariant) obj;
            return obj == this || (var.getBaseVariant() == getBaseVariant() && StringUtils.equals(_getUniqueId(), var._getUniqueId()));
        }
        return false;
    }

    public abstract String getTypeId();

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

    private volatile String storableString = null;

    public List<File> listProcessFiles(DownloadLink link) {
        return baseVariant.listProcessFiles(link);
    }

    private static final SimpleMapper MAPPER = new SimpleMapper() {
        @Override
        protected JSonFactory newJsonFactory(String jsonString) {
            return new JSonFactory(jsonString) {
                @Override
                protected String dedupeString(String string) {
                    return string;
                }
            };
        }

        @Override
        protected void initMapper() {
        }

        @Override
        public boolean isPrettyPrintEnabled() {
            return false;
        }
    };

    public String getStorableString() {
        String ret = storableString;
        if (ret == null) {
            synchronized (this) {
                final YoutubeBasicVariantStorable storable = new YoutubeBasicVariantStorable();
                storable.setId(getBaseVariant().name());
                storable.setData(MAPPER.objectToString(getGenericInfo()));
                ret = MAPPER.objectToString(storable);
                storableString = Property.dedupeString(ret);
            }
        }
        return ret;
    }

    public static AbstractVariant get(DownloadLink downloadLink) {
        return get(downloadLink, true);
    }

    public static AbstractVariant get(DownloadLink downloadLink, final boolean storeTempProperty) {
        final Object tmp = downloadLink.hasTempProperties() ? downloadLink.getTempProperties().getProperty(YoutubeHelper.YT_VARIANT, null) : null;
        if (tmp != null && tmp instanceof AbstractVariant) {
            return (AbstractVariant) tmp;
        }
        final AbstractVariant ret = get(downloadLink.getStringProperty(YoutubeHelper.YT_VARIANT));
        if (ret instanceof SubtitleVariant) {
            final String old = downloadLink.getStringProperty(YoutubeHelper.YT_SUBTITLE_CODE);
            if (old != null) {
                if (old.length() == 2) {
                    ((SubtitleVariant) ret).getGenericInfo().setLanguage(old);
                    downloadLink.removeProperty(YoutubeHelper.YT_SUBTITLE_CODE);
                    YoutubeHelper.writeVariantToDownloadLink(downloadLink, ret);
                } else {
                    try {
                        final UrlQuery q = Request.parseQuery(old);
                        ((SubtitleVariant) ret).getGenericInfo().setBase(null);
                        ((SubtitleVariant) ret).getGenericInfo().setLanguage(q.get("lng"));
                        ((SubtitleVariant) ret).getGenericInfo().setSourceLanguage(q.get("src"));
                        ((SubtitleVariant) ret).getGenericInfo().setKind(q.get("kind"));
                        downloadLink.removeProperty(YoutubeHelper.YT_SUBTITLE_CODE);
                        YoutubeHelper.writeVariantToDownloadLink(downloadLink, ret);
                    } catch (Throwable e) {
                        throw new WTFException(e);
                    }
                }
            }
        }
        if (ret != null && storeTempProperty) {
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
        storableString = null;
        this.genericInfo = genericInfo;
    }

    public Data getGenericInfo() {
        return genericInfo;
    }

    public static AbstractVariant get(VariantBase base) {
        return get(base, null, null, null, null);
    }

    public static AbstractVariant get(VariantBase base, YoutubeClipData vid, List<YoutubeStreamData> audio, List<YoutubeStreamData> video, List<YoutubeStreamData> data) {
        final AbstractVariant v = get(base.name());
        if (v != null) {
            v.fill(vid, audio, video, data);
            return v;
        } else {
            return null;
        }
    }

    protected abstract void fill(YoutubeClipData vid, List<YoutubeStreamData> audio, List<YoutubeStreamData> video, List<YoutubeStreamData> data);

    private static String dupeid(AbstractVariant var) {
        StringBuilder sb = new StringBuilder();
        sb.append(var.getGroup().name()).append("_");
        sb.append(var.getContainer().name()).append("_");
        if (var instanceof VideoVariant) {
            VideoVariant vvar = (VideoVariant) var;
            sb.append(vvar.getProjection().name()).append("_");
            sb.append(vvar.getVideoResolution().name()).append("_");
            sb.append(vvar.getVideoCodec().name()).append("_");
            sb.append(vvar.getiTagVideo().getVideoFrameRate().name()).append("_");
        }
        if (var instanceof AudioInterface) {
            AudioInterface avar = (AudioInterface) var;
            sb.append(avar.getAudioBitrate().name()).append("_");
            sb.append(avar.getAudioCodec().name()).append("_");
        }
        if (var instanceof ImageVariant) {
            sb.append(var.getBaseVariant().name()).append("_");
        }
        if (var instanceof DescriptionVariant) {
            sb.append(var.getBaseVariant().name()).append("_");
        }
        if (var instanceof SubtitleVariant) {
            sb.append(var.getBaseVariant().name()).append("_");
        }
        return sb.toString();
    }

    public static List<AbstractVariant> listVariants() {
        if (VARIANTS_LIST != null) {
            return Collections.unmodifiableList(VARIANTS_LIST);
        }
        ArrayList<AbstractVariant> sorted = new ArrayList<AbstractVariant>();
        HashSet<String> dupes = new HashSet<String>();
        for (VariantBase b : VariantBase.values()) {
            AbstractVariant var = AbstractVariant.get(b);
            if (var == null) {
                continue;
            }
            if (dupes.add(dupeid(var))) {
                sorted.add((var));
            }
            if (var instanceof VideoVariant && ((VideoVariant) var).getGenericInfo().getProjection() == Projection.NORMAL) {
                var = AbstractVariant.get(b);
                ((VideoVariant) var).getGenericInfo().setProjection(Projection.ANAGLYPH_3D);
                if (dupes.add(dupeid(var))) {
                    sorted.add((var));
                }
                var = AbstractVariant.get(b);
                ((VideoVariant) var).getGenericInfo().setProjection(Projection.SPHERICAL);
                if (dupes.add(dupeid(var))) {
                    sorted.add((var));
                }
                var = AbstractVariant.get(b);
                ((VideoVariant) var).getGenericInfo().setProjection(Projection.SPHERICAL_3D);
                if (dupes.add(dupeid(var))) {
                    sorted.add((var));
                }
            }
        }
        VARIANTS_LIST = sorted;
        return Collections.unmodifiableList(sorted);
    }

    public String getStandardGroupingID() {
        return getGroup().name();
    }

    public void setVariantInfo(VariantInfo variantInfo) {
        this.variantInfo = variantInfo;
    }

    public VariantInfo getVariantInfo() {
        return variantInfo;
    }
    // public abstract void setAlternatives(List<String> altIds);
}
