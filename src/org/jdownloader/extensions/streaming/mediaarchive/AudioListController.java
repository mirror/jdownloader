package org.jdownloader.extensions.streaming.mediaarchive;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.jdownloader.extensions.streaming.mediaarchive.storage.AudioItemStorable;

public class AudioListController extends MediaListController<AudioMediaItem> {

    private static final TypeRef<AudioItemStorable> TYPE = new TypeRef<AudioItemStorable>() {
                                                         };

    @Override
    protected String objectToJson(AudioMediaItem mi) {

        return JSonStorage.serializeToJson(AudioItemStorable.create(mi));
    }

    @Override
    protected AudioMediaItem jsonToObject(String json) {
        AudioItemStorable storable = JSonStorage.restoreFromString(json, TYPE);
        return storable.toAudioMediaItem();
    }
}
