package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import jd.plugins.DownloadLink;

import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;

public class UndefinedMediaItem extends MediaItem {

    public UndefinedMediaItem(DownloadLink dl) {
        super(dl);
    }

    @Override
    public String getMimeTypeString() {
        return null;
    }

}
