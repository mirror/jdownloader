package jd.controlling;

import java.nio.ByteBuffer;

public class ByteBufferEntry {
    private ByteBuffer buffer = null;
    private int size = 0;
    private boolean unused = true;

    public static ByteBufferEntry getByteBufferEntry(int size) {
        ByteBufferEntry ret = ByteBufferController.getInstance().getByteBufferEntry(size);
        if (ret != null) {
            return ret.getbytebufferentry(size);
        } else {
            return new ByteBufferEntry(size).getbytebufferentry(size);
        }
    }

    private ByteBufferEntry(int size) {
        this.size = size;
        buffer = ByteBuffer.allocateDirect(size);
        clear();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void clear() {
        buffer.clear();
        buffer.limit(size);
    }

    public int size() {
        return buffer.limit();
    }

    public void limit(int size) {
        this.size = size;
        buffer.limit(size);
    }

    protected ByteBufferEntry getbytebufferentry(int size) {
        unused = false;
        this.size = size;
        clear();
        return this;
    }

    /*
     * may be called only once in lifetime of the bytebufferentry!, please call
     * this only at the end of usage, because buffer is instantly available for
     * others to use
     */
    public void setUnused() {
        if (unused) return;
        unused = true;
        ByteBufferController.getInstance().putByteBufferEntry(this);
    }

}