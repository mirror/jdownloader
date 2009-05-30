package jd.controlling;

import java.util.ArrayList;

import jd.nutils.Formatter;

public class MemoryController {

    private ArrayList<ByteBufferEntry> bufferpool;

    public final static String MAXBUFFERSIZE = "MAX_BUFFER_SIZE_V2";

    private static MemoryController INSTANCE;

    protected Integer BufferCreated = new Integer(0);
    protected Integer BufferFreed = new Integer(0);
    protected boolean MemoryControllerEnabled = true;

    public synchronized static MemoryController getInstance() {
        if (INSTANCE == null) INSTANCE = new MemoryController();
        return INSTANCE;
    }

    private MemoryController() {
        bufferpool = new ArrayList<ByteBufferEntry>();
        Thread thread = new Thread() {
            public void run() {
                while (true) {
                    MemoryController.getInstance().printDebug();
                    try {
                        sleep(10000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        };
        if (MemoryControllerEnabled) thread.start();
    }

    public void printDebug() {
        System.out.println("MemoryController: " + Formatter.formatReadable(BufferCreated) + " in " + bufferpool.size() + " BufferEntries!");
    }

    protected void increaseCreated(int size) {
        synchronized (BufferCreated) {
            BufferCreated += size;
        }
    }

    protected void increaseFreed(int size) {
        synchronized (BufferFreed) {
            BufferFreed += size;
        }
    }

    // private void preAlloc() {
    // int prems = AccountController.getInstance().validAccounts();
    // int chunks =
    // SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS,
    // 2);
    // int buffersize =
    // SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(MAXBUFFERSIZE,
    // 100) * 1024;
    // int downloads =
    // SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN,
    // 2);
    // }

    public ByteBufferEntry getByteBufferEntry(int size) {
        if (!MemoryControllerEnabled) return new ByteBufferEntry(size).getByteBufferEntry();
        synchronized (bufferpool) {
            for (ByteBufferEntry entry : bufferpool) {
                if (!entry.inUse() && entry.size() >= size) return entry.getByteBufferEntry();
            }
            ByteBufferEntry newentry = new ByteBufferEntry(size);
            bufferpool.add(newentry);
            return newentry.getByteBufferEntry();
        }
    }
}
