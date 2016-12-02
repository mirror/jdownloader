package org.jdownloader.downloader.segment;

public class Segment {

    private final String url;
    private long         size = -1;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        if (size <= 0) {
            this.size = -1;
        } else {
            this.size = size;
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    private boolean loaded = false;

    public String getUrl() {
        return url;
    }

    public Segment(String url) {
        this.url = url;
    }

}
