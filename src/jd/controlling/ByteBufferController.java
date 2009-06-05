package jd.controlling;

import java.util.ArrayList;

import jd.nutils.Formatter;

public class ByteBufferController {

    private ArrayList<ByteBufferEntry> bufferpool;

    public final static String MAXBUFFERSIZE = "MAX_BUFFER_SIZE_V3";

    private static ByteBufferController INSTANCE;

    protected Integer BufferFresh = new Integer(0);
    protected Integer BufferReused = new Integer(0);
    protected Integer BufferFree = new Integer(0);

    public synchronized static ByteBufferController getInstance() {
        if (INSTANCE == null) INSTANCE = new ByteBufferController();
        return INSTANCE;
    }

    public void printDebug() {
        JDLogger.getLogger().info("ByteBufferController: Fresh: " + Formatter.formatReadable(BufferFresh) + " Reused: " + Formatter.formatReadable(BufferReused) + " Free: " + Formatter.formatReadable(BufferFree));
    }

    private ByteBufferController() {
        bufferpool = new ArrayList<ByteBufferEntry>();
        Thread thread = new Thread() {
            public void run() {
                while (true) {
                    ByteBufferController.getInstance().printDebug();
                    try {
                        sleep(1000 * 60);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        };
        thread.start();
    }

    protected void increaseFresh(int size) {
        synchronized (BufferFresh) {
            BufferFresh += size;
        }
    }

    protected void decreaseFresh(int size) {
        synchronized (BufferFresh) {
            BufferFresh -= size;
        }
    }

    protected void increaseFree(int size) {
        synchronized (BufferFree) {
            BufferFree += size;
        }
    }

    protected void decreaseFree(int size) {
        synchronized (BufferFree) {
            BufferFree -= size;
        }
    }

    protected void increaseReused(int size) {
        synchronized (BufferReused) {
            BufferReused += size;
        }
    }

    protected void decreaseReused(int size) {
        synchronized (BufferReused) {
            BufferReused -= size;
        }
    }

    protected ByteBufferEntry getByteBufferEntry(int size) {
        ByteBufferEntry ret = null;
        synchronized (bufferpool) {
            for (ByteBufferEntry entry : bufferpool) {
                if (entry.size() >= size) {
                    if (ret != null && ret.size() > entry.size()) {
                        ret = entry;
                    } else if (ret == null) ret = entry;
                }
            }
            if (ret != null) {
                bufferpool.remove(ret);
                return ret.getByteBufferEntry();
            }
        }
        return null;
    }

    protected void putByteBufferEntry(ByteBufferEntry entry) {
        synchronized (bufferpool) {
            if (!bufferpool.contains(entry)) {
                bufferpool.add(entry);
            }
        }
    }
}
