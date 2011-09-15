package org.jdownloader.controlling.filter;

import org.appwork.storage.Storable;

public class FilersizeFilter extends Filter implements Storable {
    private long from;

    private FilersizeFilter() {
        // Storable
    }

    /**
     * @param from
     * @param to
     * @param enabled
     */
    public FilersizeFilter(long from, long to, boolean enabled) {
        super();
        this.from = from;
        this.to = to;
        this.enabled = enabled;
    }

    private long to;

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }

}
