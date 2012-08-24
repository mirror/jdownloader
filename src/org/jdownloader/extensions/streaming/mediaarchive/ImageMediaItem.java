package org.jdownloader.extensions.streaming.mediaarchive;

import jd.plugins.DownloadLink;

public class ImageMediaItem extends MediaItem {

    public ImageMediaItem(DownloadLink dl) {
        super(dl);
    }

    private int width;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    private int height;

}
