package org.jdownloader.extensions.streaming.mediaarchive;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.jdownloader.extensions.streaming.mediaarchive.storage.VideoItemStorable;

public class VideoListController extends MediaListController<VideoMediaItem> {

    private static final TypeRef<VideoItemStorable> TYPE = new TypeRef<VideoItemStorable>() {
                                                         };

    @Override
    protected String objectToJson(VideoMediaItem mi) {

        return JSonStorage.toString(VideoItemStorable.create(mi));
    }

    @Override
    protected VideoMediaItem jsonToObject(String json) {
        VideoItemStorable storable = JSonStorage.restoreFromString(json, TYPE);
        return storable.toVideoMediaItem();
    }

}
