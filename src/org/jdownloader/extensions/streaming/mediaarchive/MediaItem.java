package org.jdownloader.extensions.streaming.mediaarchive;

import javax.swing.ImageIcon;

import jd.plugins.DownloadLink;

import org.jdownloader.images.NewTheme;

public abstract class MediaItem implements MediaNode {
    private DownloadLink downloadLink;

    public MediaItem(DownloadLink dl) {
        this.downloadLink = dl;
    }

    @Override
    public String getName() {
        return downloadLink.getName();
    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("video", 20);
    }

    @Override
    public long getFilesize() {
        return downloadLink.getDownloadSize();
    }
}
