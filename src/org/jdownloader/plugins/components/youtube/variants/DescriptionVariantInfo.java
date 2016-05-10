package org.jdownloader.plugins.components.youtube.variants;

import org.jdownloader.plugins.components.youtube.StreamCollection;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;

public class DescriptionVariantInfo extends VariantInfo {

    public DescriptionVariantInfo(String description, YoutubeClipData vid) {
        super(new DescriptionVariant(description), null, null, createDummy(vid));
    }

    private static StreamCollection createDummy(YoutubeClipData vid) {
        StreamCollection list = new StreamCollection();
        list.add(new YoutubeStreamData(null, vid, null, YoutubeITAG.DESCRIPTION, null));
        ;
        return list;

    }

    @Override
    public int compareTo(VariantInfo o) {
        return this.getVariant()._getName(null).compareToIgnoreCase(o.getVariant()._getName(null));

    }
}