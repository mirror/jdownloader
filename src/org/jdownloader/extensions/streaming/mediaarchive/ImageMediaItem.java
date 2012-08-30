package org.jdownloader.extensions.streaming.mediaarchive;

import jd.plugins.DownloadLink;

import org.appwork.utils.Files;
import org.jdownloader.extensions.streaming.dlna.Extensions;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.image.AbstractImageProfile;

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

    @Override
    public String getMimeTypeString() {
        return "image/" + getContainerFormat();
    }

    private int height;

    public void update(ImageMediaItem node) {
        super.update(node);
        this.width = node.width;
        this.height = node.height;

    }

    public ProfileMatch matches(AbstractImageProfile p) {
        String ext = Files.getExtension(getDownloadLink().getName());
        for (AbstractMediaContainer c : p.getContainer()) {
            for (Extensions e : c.getExtensions()) {
                if (e.getExtension().equalsIgnoreCase(ext)) {
                    if (p.checkResolution(width, height)) { return new ProfileMatch(p); }

                }

            }
        }

        return null;
    }

}
