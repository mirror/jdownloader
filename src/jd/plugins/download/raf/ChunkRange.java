package jd.plugins.download.raf;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ChunkRange {

    private final long from;

    public long getFrom() {
        return from;
    }

    public Long getTo() {
        return to;
    }

    public long getLength() {
        Long to = getTo();
        if (to == null || to < 0) {
            return -1;
        }
        return to - from + 1;
    }

    private final Long          to;
    private final AtomicLong    loaded      = new AtomicLong(0);
    private final AtomicBoolean validLoaded = new AtomicBoolean(false);
    private final boolean       rangeRequested;

    public boolean isRangeRequested() {
        return rangeRequested;
    }

    public boolean isValidLoaded() {
        return validLoaded.get();
    }

    public void setValidLoaded(boolean validLoaded) {
        this.validLoaded.set(validLoaded);
    }

    public ChunkRange() {
        rangeRequested = false;
        from = 0;
        to = null;
    }

    public ChunkRange(final long from) {
        this(from, null);
    }

    /* create a ChunkRange from index 'from' to index 'to' (included) */
    public ChunkRange(final long from, final Long to) {
        rangeRequested = true;
        if (from < 0) {
            throw new IllegalArgumentException("from < 0");
        }
        this.from = from;
        if (to != null && to >= 0) {
            if (from > to) {
                throw new IllegalArgumentException("from > to");
            }
            if (to < from) {
                throw new IllegalArgumentException("to < from");
            }
        }
        this.to = to;
    }

    public long getLoaded() {
        return loaded.get();
    }

    public long getPosition() {
        return from + getLoaded();
    }

    public void incLoaded(long incr) {
        if (incr > 0) {
            this.loaded.addAndGet(incr);
        }
    }

    @Override
    public String toString() {
        return "ChunkRange: " + getFrom() + "-" + getTo() + "/" + getLength() + "|" + getLoaded() + "=" + isValidLoaded();
    }
}
