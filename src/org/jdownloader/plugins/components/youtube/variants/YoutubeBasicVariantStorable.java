package org.jdownloader.plugins.components.youtube.variants;

import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;

public class YoutubeBasicVariantStorable implements Storable {
    private String id;
    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static final TypeRef<YoutubeBasicVariantStorable> TYPE = new TypeRef<YoutubeBasicVariantStorable>(YoutubeBasicVariantStorable.class) {
    };

    public YoutubeBasicVariantStorable(/* Storable */) {

    }

    public YoutubeBasicVariantStorable(String name) {
        id = name;
    }

}