package org.jdownloader.plugins.components.youtube.variants.generics;

import org.appwork.storage.Storable;

public class GenericVideoInfo extends AbstractGenericVariantInfo implements Storable {
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

    private boolean threeD = false;

    public boolean isThreeD() {
        return threeD;
    }

    public void setThreeD(boolean threeD) {
        this.threeD = threeD;
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