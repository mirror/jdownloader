package jd.controlling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jd.nutils.Formatter;

public class ByteBufferController {

    private ArrayList<ByteBufferEntry> bufferpool;

    public final static String MAXBUFFERSIZE = "MAX_BUFFER_SIZE_V3";

    private static ByteBufferController INSTANCE;

    private Comparator<ByteBufferEntry> bytebuffercomp = new Comparator<ByteBufferEntry>() {
        public int compare(ByteBufferEntry a, ByteBufferEntry b) {
            return a.capacity() == b.capacity() ? 0 : a.capacity() > b.capacity() ? 1 : -1;
        }
    };

    protected long BufferEntries = 0;

    public synchronized static ByteBufferController getInstance() {
        if (INSTANCE == null) INSTANCE = new ByteBufferController();
        return INSTANCE;
    }

    public void printDebug() {
        long free = 0;
        synchronized (bufferpool) {
            for (ByteBufferEntry entry : bufferpool) {
                free += entry.capacity();
            }
        }
        JDLogger.getLogger().info("ByteBufferController: Used: " + Formatter.formatReadable(BufferEntries - free) + " Free: " + Formatter.formatReadable(free));
    }

    private ByteBufferController() {
        bufferpool = new ArrayList<ByteBufferEntry>();
        Thread thread = new Thread() {
            public void run() {
                while (true) {
                    try {
                        sleep(1000 * 60 * 10);
                    } catch (InterruptedException e) {
                        break;
                    }
                    ByteBufferController.getInstance().printDebug();
                }
            }
        };
        thread.start();
    }

    protected ByteBufferEntry getByteBufferEntry(int size) {
        ByteBufferEntry ret = null;
        synchronized (bufferpool) {
            for (ByteBufferEntry entry : bufferpool) {
                if (entry.capacity() >= size) {
                    ret = entry;
                    bufferpool.remove(entry);
                    return ret.getbytebufferentry(size);
                }
            }
        }
        BufferEntries += size;
        return null;
    }

    protected void putByteBufferEntry(ByteBufferEntry entry) {
        synchronized (bufferpool) {
            if (!bufferpool.contains(entry)) bufferpool.add(entry);
            Collections.sort(bufferpool, bytebuffercomp);
        }
    }
}
