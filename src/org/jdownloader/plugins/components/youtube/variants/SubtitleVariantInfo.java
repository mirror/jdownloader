package org.jdownloader.plugins.components.youtube.variants;

import org.jdownloader.plugins.components.youtube.StreamCollection;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;

public class SubtitleVariantInfo extends VariantInfo {
    public SubtitleVariantInfo(SubtitleVariant v, YoutubeClipData vid) {
        super(v, null, null, createDummyList(vid, v.getGenericInfo().getUrl()));
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
    private static StreamCollection createDummyList(YoutubeClipData vid, String url) {
        StreamCollection l = new StreamCollection();
        l.add(new YoutubeStreamData(null, vid, url, YoutubeITAG.SUBTITLE, null));
        return l;
    }
}