package org.jdownloader.extensions.streaming.dlna.profiles.streams;

import java.util.HashSet;

public class InternalStream {
    protected String  contentType;
    protected boolean systemStream;

    public boolean isSystemStream() {
        return systemStream;
    }

    public InternalStream(String contentType2) {
        this.contentType = contentType2;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    protected HashSet<String> profileTags = new HashSet<String>();

    public HashSet<String> getProfileTags() {
        return profileTags;

    }

    public InternalStream setProfileTags(String... tags) {
        getProfileTags().clear();
        addProfileTags(tags);
        return this;
    }

    public InternalStream addProfileTags(String... tags) {
        for (String t : tags) {
            profileTags.add(t);
        }
        return this;
    }

    public String getContentType() {
        return contentType;
    }
}
