package org.jdownloader.plugins.components.realDebridCom.api.json;

import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;

public class HostsResponse implements Storable {
    public static final TypeRef<HostsResponse> TYPE = new TypeRef<HostsResponse>(HostsResponse.class) {
                                                    };

    private String                             id;
    private String                             image;

    private String                             image_big;

    private String                             name;

    public HostsResponse(/* Storable */) {
    }

    public String getId() {
        return id;
    }

    public String getImage() {
        return image;
    }

    public String getImage_big() {
        return image_big;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setImage_big(String image_big) {
        this.image_big = image_big;
    }

    public void setName(String name) {
        this.name = name;
    }

}