package org.jdownloader.statistics;

import org.appwork.storage.Storable;

public class ClickedAffLinkStorable implements Storable {
    private String url;
    private String source;
    private long   time;

    private ClickedAffLinkStorable(/* Storable */) {

    }

    public ClickedAffLinkStorable(String url, String source) {
        this.url = url;
        this.source = source;
        this.time = System.currentTimeMillis();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
