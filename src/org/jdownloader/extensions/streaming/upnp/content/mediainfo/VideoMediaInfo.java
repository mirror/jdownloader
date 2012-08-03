package org.jdownloader.extensions.streaming.upnp.content.mediainfo;

import java.io.File;

import org.seamless.util.MimeType;

public class VideoMediaInfo implements MediaInfo {

    private File file;

    public VideoMediaInfo(File f) {
        this.file = f;
    }

    @Override
    public MimeType getMimeType() {
        return new MimeType("video", "mp4");
    }

    @Override
    public Long getContentLength() {
        return file.length();
    }

    @Override
    public String getTitle() {
        return file.getName();
    }

    public int getDuration() {
        return 34213;
    }

    public Long getBitrate() {
        return null;
    }

}
