package jd.controlling;

import java.nio.ByteBuffer;

public class ByteBufferEntry {
    private ByteBuffer buffer = null;
    private int used = 0;
    boolean inuse = false;

    public static ByteBufferEntry getByteBufferEntry(int size) {
        ByteBufferEntry ret = ByteBufferController.getInstance().getByteBufferEntry(size);
        if (ret != null) {
            ByteBufferController.getInstance().increaseReused(ret.size());
            ByteBufferController.getInstance().decreaseFree(ret.size());
            //System.out.println("Reuse old ByteBufferEntry " + ret.size());
            return ret.getByteBufferEntry();
        } else {
            //System.out.println("Create new ByteBufferEntry " + size);
            return new ByteBufferEntry(size).getByteBufferEntry();
        }
    }

    private ByteBufferEntry(int size) {
        ByteBufferController.getInstance().increaseFresh(size);
        buffer = ByteBuffer.allocateDirect(size);
        used = 0;
    }

    public int size() {
        return buffer.capacity();
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    protected ByteBufferEntry getByteBufferEntry() {
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
        ByteBufferController.getInstance().increaseFree(size());
        if (used > 1) {
            ByteBufferController.getInstance().decreaseReused(size());
        } else {
            ByteBufferController.getInstance().decreaseFresh(size());
        }
        ByteBufferController.getInstance().putByteBufferEntry(this);
    }

}