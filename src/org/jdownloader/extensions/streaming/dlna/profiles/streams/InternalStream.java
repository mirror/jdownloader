package org.jdownloader.extensions.streaming.dlna.profiles.streams;

public class InternalStream {
    protected String  contentType;
    protected boolean systemStream;

    public boolean isSystemStream() {
        return systemStream;
    }

    public InternalStream(String contentType2) {
        this.contentType = contentType2;
    }

    protected String[] profileTags;

    public InternalStream setProfileTags(String[] profileTags) {
        this.profileTags = profileTags;
        return this;
    }

    public String[] getProfileTags() {
        return profileTags;
    }

    public String getContentType() {
        return contentType;
    }
}
