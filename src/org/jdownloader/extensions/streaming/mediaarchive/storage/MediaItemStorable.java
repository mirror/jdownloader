package org.jdownloader.extensions.streaming.mediaarchive.storage;

import jd.controlling.downloadcontroller.DownloadLinkStorable;

import org.appwork.storage.Storable;

public class MediaItemStorable implements Storable {

    private String               thumbnailPath;
    private DownloadLinkStorable downloadLink;

    protected MediaItemStorable(/* Storable */) {

    }

    public DownloadLinkStorable getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(DownloadLinkStorable downloadLink) {
        this.downloadLink = downloadLink;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    private String containerFormat;
    private long   size = -1;

    public void setContainerFormat(String majorBrand) {
        this.containerFormat = majorBrand;

    }

    public String getContainerFormat() {
        return containerFormat;
    }

    public void setSize(long l) {
        this.size = l;
    }

    public long getSize() {

        return size;
    }
}
