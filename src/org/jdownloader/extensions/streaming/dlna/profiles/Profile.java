package org.jdownloader.extensions.streaming.dlna.profiles;

import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;

public abstract class Profile {
    protected MimeType                 mimeType;
    private String                     id;

    protected AbstractMediaContainer[] containers;
    protected String[]                 profileTags;

    public String[] getProfileTags() {
        return profileTags;
    }

    public Profile(String id) {
        this.id = id;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public String getProfileID() {
        return id;
    }

    public AbstractMediaContainer[] getContainer() {
        return containers;
    }

}
