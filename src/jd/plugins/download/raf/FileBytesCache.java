package jd.plugins.download.raf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.jdownloader.logging.LogController;

public class FileBytesCache {
    protected final byte[]                     writeCache;
    protected volatile int                     writeCachePosition  = 0;
    protected volatile long                    lastWriteCacheFlush = -1l;
    protected final ArrayList<WriteCacheEntry> writeCacheEntries   = new ArrayList<WriteCacheEntry>();
    protected final int                        flushTimeout;
    
    private class WriteCacheEntry {
        private volatile int size;
        
        private final int getSize() {
            return size;
        }
        
        private final void increaseSize(int size) {
            this.size += size;
        }
        
        private final FileBytesCacheFlusher getFlusher() {
            return flusher;
        }
        
        private final FileBytesCacheFlusher flusher;
        private final int                   writeCachePosition;
        private final long                  fileWritePosition;
        
        public long getFileWritePosition() {
            return fileWritePosition;
        }
        
        public final int getWriteCachePosition() {
            return writeCachePosition;
        }
        
        private WriteCacheEntry(FileBytesCacheFlusher flusher, int writeCachePosition, long fileWritePosition) {
            this.writeCachePosition = writeCachePosition;
            this.fileWritePosition = fileWritePosition;
            this.size = 0;
            this.flusher = flusher;
        }
        
        @Override
        public String toString() {
            return flusher + ":" + writeCachePosition + "=" + size + "->" + fileWritePosition;
        }
    }
    
    public FileBytesCache(int writeCacheSize, int flushTimeout) {
        writeCache = new byte[writeCacheSize];
        this.flushTimeout = flushTimeout;
    }
    
    public synchronized void write(FileBytesCacheFlusher flusher, long fileWritePosition, byte[] readBuffer, int length) {
        final int writeCacheRemaining = writeCache.length - writeCachePosition;
        if (length > writeCacheRemaining || lastWriteCacheFlush > 0 && System.currentTimeMillis() > lastWriteCacheFlush) {
            flush();
        }
        WriteCacheEntry entry = null;
        boolean add = false;
        if (writeCacheEntries.size() > 0 && (entry = writeCacheEntries.get(writeCacheEntries.size() - 1)) != null && entry.getFlusher() == flusher && entry.getFileWritePosition() + entry.getSize() == fileWritePosition) {
        } else {
            entry = new WriteCacheEntry(flusher, writeCachePosition, fileWritePosition);
            add = true;
        }
        System.arraycopy(readBuffer, 0, writeCache, writeCachePosition, length);
        writeCachePosition += length;
        entry.increaseSize(length);
        if (add) writeCacheEntries.add(entry);
        if (lastWriteCacheFlush < 0) lastWriteCacheFlush = System.currentTimeMillis() + flushTimeout;
    }
    
    public synchronized void flushIfContains(FileBytesCacheFlusher flusher) {
        boolean flush = false;
        for (WriteCacheEntry writeCacheEntry : writeCacheEntries) {
            if (writeCacheEntry.getFlusher() == flusher) {
                flush = true;
                break;
            }
        }
        if (flush) flush();
    }
    
    public synchronized void flush() {
        System.out.println("FLUSH: " + writeCacheEntries.size() + "=" + writeCachePosition + "/" + writeCache.length);
        lastWriteCacheFlush = -1;
        writeCachePosition = 0;
        HashMap<FileBytesCacheFlusher, ArrayList<WriteCacheEntry>> writeEntries = new HashMap<FileBytesCacheFlusher, ArrayList<WriteCacheEntry>>();
        for (WriteCacheEntry writeCacheEntry : writeCacheEntries) {
            FileBytesCacheFlusher flusher = writeCacheEntry.getFlusher();
            ArrayList<WriteCacheEntry> cacheList = writeEntries.get(flusher);
            if (cacheList == null) {
                cacheList = new ArrayList<FileBytesCache.WriteCacheEntry>();
                writeEntries.put(flusher, cacheList);
            }
            cacheList.add(writeCacheEntry);
        }
        writeCacheEntries.clear();
        for (Entry<FileBytesCacheFlusher, ArrayList<WriteCacheEntry>> writeEntry : writeEntries.entrySet()) {
            FileBytesCacheFlusher flusher = writeEntry.getKey();
            try {
                for (WriteCacheEntry writeCacheEntry : writeEntry.getValue()) {
                    flusher.flush(writeCache, writeCacheEntry.getWriteCachePosition(), writeCacheEntry.getSize(), writeCacheEntry.getFileWritePosition());
                }
                flusher.flushed();
            } catch (final Throwable e) {
                LogController.CL().log(e);
            }
        }
    }
}
