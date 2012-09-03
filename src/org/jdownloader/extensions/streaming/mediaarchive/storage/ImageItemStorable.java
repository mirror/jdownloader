package org.jdownloader.extensions.streaming.mediaarchive.storage;

import org.appwork.storage.Storable;
import org.jdownloader.extensions.streaming.mediaarchive.ImageMediaItem;

public class ImageItemStorable extends MediaItemStorable implements Storable {

    private ImageItemStorable(/* Storable */) {

    }

    public static ImageItemStorable create(ImageMediaItem mi) {
        ImageItemStorable ret = new ImageItemStorable();
        ret.init(mi);
        ret.setWidth(mi.getWidth());
        ret.setHeight(mi.getHeight());

        return ret;
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

    public ImageMediaItem toImageMediaItem() {
        ImageMediaItem ret = new ImageMediaItem(getDownloadLink()._getDownloadLink());
        fillMediaItem(ret);
        ret.setWidth(getWidth());
        ret.setHeight(getHeight());

        return ret;
    }

}
