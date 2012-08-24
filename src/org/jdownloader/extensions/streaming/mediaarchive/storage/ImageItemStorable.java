package org.jdownloader.extensions.streaming.mediaarchive.storage;

import jd.controlling.downloadcontroller.DownloadLinkStorable;

import org.appwork.storage.Storable;
import org.jdownloader.extensions.streaming.mediaarchive.ImageMediaItem;

public class ImageItemStorable extends MediaItemStorable implements Storable {

    private ImageItemStorable(/* Storable */) {

    }

    public static ImageItemStorable create(ImageMediaItem mi) {
        ImageItemStorable ret = new ImageItemStorable();
        ret.setDownloadLink(new DownloadLinkStorable(mi.getDownloadLink()));
        ret.setWidth(mi.getWidth());
        ret.setHeight(mi.getHeight());
        ret.setSize(mi.getSize());
        ret.setContainerFormat(mi.getContainerFormat());
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
        ret.setWidth(getWidth());
        ret.setHeight(getHeight());
        ret.setSize(getSize());
        ret.setContainerFormat(getContainerFormat());
        return ret;
    }

}
