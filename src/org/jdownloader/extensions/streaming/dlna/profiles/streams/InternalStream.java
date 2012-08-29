package org.jdownloader.extensions.streaming.dlna.profiles.streams;

import java.util.HashSet;

public class InternalStream {
    protected String  contentType;
    protected boolean systemStream;

    private String[]  codecTags;

    /**
     * Sets the possible codec ids for this streams. these ids have to be equal with the ffmpeg codec ids
     * 
     * @param ids
     */
    public void setCodecTags(String... ids) {
        this.codecTags = ids;
    }

    public String[] getCodecTags() {
        return codecTags;
    }

    private String[] codecNames;

    /**
     * Sets the possible codec ids for this streams. these ids have to be equal with the ffmpeg codec ids
     * 
     * @param ids
     */
    public void setCodecNames(String... ids) {
        this.codecNames = ids;
    }

    public String[] getCodecNames() {
        return codecNames;
    }

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
