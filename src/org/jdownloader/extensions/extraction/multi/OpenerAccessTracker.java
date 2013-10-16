package org.jdownloader.extensions.extraction.multi;

import java.io.RandomAccessFile;

public final class OpenerAccessTracker {

    private final RandomAccessFile raf;
    private long                   accessIndex = 0;
    private final String           id;

    public long getAccessIndex() {
        return accessIndex;
    }

    public void setAccessIndex(long accessIndex) {
        this.accessIndex = accessIndex;
    }

    public OpenerAccessTracker(String id, RandomAccessFile raf) {
        this.id = id;
        this.raf = raf;
    }

    public RandomAccessFile getRandomAccessFile() {
        return raf;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }
}
