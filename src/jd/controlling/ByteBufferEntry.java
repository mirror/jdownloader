package jd.controlling;

import java.nio.ByteBuffer;

public class ByteBufferEntry {
    private ByteBuffer buffer = null;
    private long lastaccess = 0;
    private long lastfree = 0;
    boolean inuse = false;

    public ByteBufferEntry(int size) {
        MemoryController.getInstance().increaseCreated(size);
        buffer = ByteBuffer.allocateDirect(size);
    }

    public int size() {
        return buffer.capacity();
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public ByteBufferEntry getByteBufferEntry() {
        inuse = true;
        lastaccess = System.currentTimeMillis();
        return this;
    }

    public long lastAccess() {
        return lastaccess;
    }

    public long lastFree() {
        return lastfree;
    }

    public boolean inUse() {
        return inuse;
    }

    public void setUnused() {
        lastfree = System.currentTimeMillis();
        inuse = false;
    }

}