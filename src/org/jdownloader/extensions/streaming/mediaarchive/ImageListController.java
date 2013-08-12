package org.jdownloader.extensions.streaming.mediaarchive;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.jdownloader.extensions.streaming.mediaarchive.storage.ImageItemStorable;

public class ImageListController extends MediaListController<ImageMediaItem> {
    private static final TypeRef<ImageItemStorable> TYPE = new TypeRef<ImageItemStorable>() {
                                                         };

    @Override
    protected String objectToJson(ImageMediaItem mi) {

        return JSonStorage.serializeToJson(ImageItemStorable.create(mi));
    }

    @Override
    protected ImageMediaItem jsonToObject(String json) {
        ImageItemStorable storable = JSonStorage.restoreFromString(json, TYPE);
        return storable.toImageMediaItem();
    }

}
