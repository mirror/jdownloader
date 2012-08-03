package org.jdownloader.extensions.vlcstreaming.upnp.content.mediainfo;

import java.io.File;

import org.seamless.util.MimeType;

public class AudioMediaInfo implements MediaInfo {

    private File file;

    public AudioMediaInfo(File f) {
        this.file = f;
    }

    public int getDuration() {
        return 300;
    }

    public String getArtist() {
        return "Unknown Artist";
    }

    public MimeType getMimeType() {
        return new MimeType("audio", "mpeg");
    }

    public Long getContentLength() {
        return file.length();
    }

    public Long getBitrate() {
        return 4000l;
    }

    public String getTitle() {
        return file.getName();
    }

    public String getAlbum() {
        return "Unknown Album";
    }

}
