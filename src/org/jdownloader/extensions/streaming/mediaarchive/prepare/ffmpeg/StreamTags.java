package org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg;

import org.appwork.storage.Storable;

public class StreamTags implements Storable {
    public StreamTags(/* storable */) {

    }

    private String creation_time;

    public String getCreation_time() {
        return creation_time;
    }

    public void setCreation_time(String creation_time) {
        this.creation_time = creation_time;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getHandler_name() {
        return handler_name;
    }

    public void setHandler_name(String handler_name) {
        this.handler_name = handler_name;
    }

    private String language;
    private String handler_name;
}
