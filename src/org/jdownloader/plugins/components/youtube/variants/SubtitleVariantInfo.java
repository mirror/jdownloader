package org.jdownloader.plugins.components.youtube.variants;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;

public class SubtitleVariantInfo extends VariantInfo {
    public SubtitleVariantInfo(SubtitleVariant v, YoutubeClipData vid) {
        super(v, null, null, createDummyList(vid, v.getGenericInfo().createUrl()));
    }

    @Override
    public SubtitleVariant getVariant() {
        return (SubtitleVariant) super.getVariant();
    }
    // @Override
    // public void fillExtraProperties(DownloadLink thislink, List<VariantInfo> alternatives) {
    // thislink.setProperty(YoutubeHelper.YT_SUBTITLE_CODE, si._getIdentifier());
    // final ArrayList<String> lngCodes = new ArrayList<String>();
    // for (final VariantInfo si : alternatives) {
    // lngCodes.add(si.getIdentifier());
    // }
    // thislink.setProperty(YoutubeHelper.YT_SUBTITLE_CODE_LIST, JSonStorage.serializeToJson(lngCodes));
    // }

    private static List<YoutubeStreamData> createDummyList(YoutubeClipData vid, String url) {
        ArrayList<YoutubeStreamData> l = new ArrayList<YoutubeStreamData>();
        l.add(new YoutubeStreamData(vid, url, YoutubeITAG.SUBTITLE, null));
        return l;
    }

    public static int compare(boolean x, boolean y) {
        return (x == y) ? 0 : (x ? 1 : -1);
    }

    @Override
    public int compareTo(VariantInfo o) {
        if (o instanceof SubtitleVariantInfo) {
            int ret = compare(getVariant().getGenericInfo()._isSpeechToText(), ((SubtitleVariantInfo) o).getVariant().getGenericInfo()._isSpeechToText());
            if (ret != 0) {
                return ret;
            }
            ret = compare(getVariant().getGenericInfo()._isTranslated(), ((SubtitleVariantInfo) o).getVariant().getGenericInfo()._isTranslated());
            if (ret != 0) {
                return ret;
            }

            return this.getVariant()._getName(null).compareToIgnoreCase(o.getVariant()._getName(null));
        }
        return -1;
    }
}