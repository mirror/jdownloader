package jd.controlling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
                    JDLogger.getLogger().severe("found bytebufferentry with " + entry.size() + " to serve request with " + size);
                    ret = entry;
                    bufferpool.remove(entry);
                    return ret.getByteBufferEntry();
                }
            }
        }
        JDLogger.getLogger().severe("no bytebufferentry found to serve request with " + size);
        return null;
    }

    protected void putByteBufferEntry(ByteBufferEntry entry) {
        synchronized (bufferpool) {
            if (!bufferpool.contains(entry)) bufferpool.add(entry);
            Collections.sort(bufferpool, new Comparator<ByteBufferEntry>() {
                public int compare(ByteBufferEntry a, ByteBufferEntry b) {
                    return a.size() == b.size() ? 0 : a.size() > b.size() ? 1 : -1;
                }
            });
        }
    }
}
