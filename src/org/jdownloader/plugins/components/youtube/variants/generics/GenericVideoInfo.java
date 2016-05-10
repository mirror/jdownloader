package org.jdownloader.plugins.components.youtube.variants.generics;

import org.appwork.storage.Storable;
import org.jdownloader.plugins.components.youtube.Projection;

public class GenericVideoInfo extends GenericAudioInfo implements Storable {
    public GenericVideoInfo(/* Storable */) {
    }

    private int height;
    private int width;
    private int fps;

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    private Projection projection = null;

    public Projection getProjection() {
        if (projection == null) {
            return Projection.NORMAL;
        }
        return projection;
    }

    public void setProjection(Projection projection) {
        this.projection = projection;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }
}