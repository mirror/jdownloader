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
            ByteBufferController.getInstance().increaseReused(ret.maxsize());
            ByteBufferController.getInstance().decreaseFree(ret.maxsize());
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
        buffer.clear();
        used = 0;
    }

    public int maxsize() {
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
        buffer.clear();
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
        ByteBufferController.getInstance().increaseFree(maxsize());
        if (used > 1) {
            ByteBufferController.getInstance().decreaseReused(maxsize());
        } else {
            ByteBufferController.getInstance().decreaseFresh(maxsize());
        }
        ByteBufferController.getInstance().putByteBufferEntry(this);
    }

}