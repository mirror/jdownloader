package jd.controlling;

import java.nio.ByteBuffer;

public class ByteBufferEntry {
    private ByteBuffer buffer = null;
    private int size = 0;
    private int used = 0;
    boolean inuse = false;

    public static ByteBufferEntry getByteBufferEntry(int size) {
        ByteBufferEntry ret = ByteBufferController.getInstance().getByteBufferEntry(size);
        if (ret != null) {
            ByteBufferController.getInstance().increaseReused(ret.capacity());
            ByteBufferController.getInstance().decreaseFree(ret.capacity());
            // System.out.println("Reuse old ByteBufferEntry " + ret.size());
            return ret.getbytebufferentry(size);
        } else {
            // System.out.println("Create new ByteBufferEntry " + size);
            return new ByteBufferEntry(size).getbytebufferentry(size);
        }
    }

    private ByteBufferEntry(int size) {
        this.size = size;
        ByteBufferController.getInstance().increaseFresh(size);
        buffer = ByteBuffer.allocateDirect(size);
        clear();
        used = 0;
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

    protected ByteBufferEntry getbytebufferentry(int size) {
        this.size = size;
        used++;
        inuse = true;
        clear();
        return this;
    }

    public boolean inUse() {
        return inuse;
    }

    public int used() {
        return used;
    }

    public void setUnused() {
        if (!inuse) return;
        inuse = false;
        ByteBufferController.getInstance().increaseFree(capacity());
        if (used > 1) {
            ByteBufferController.getInstance().decreaseReused(capacity());
        } else {
            ByteBufferController.getInstance().decreaseFresh(capacity());
        }
        ByteBufferController.getInstance().putByteBufferEntry(this);
    }

}