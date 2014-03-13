package jd.plugins.download.raf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

import org.jdownloader.logging.LogController;

public class FileBytesCache {
    protected final byte[]                     writeCache;
    protected volatile int                     writeCachePosition  = 0;
    protected volatile long                    lastWriteCacheFlush = -1l;
    protected final ArrayList<WriteCacheEntry> writeCacheEntries   = new ArrayList<WriteCacheEntry>();
    protected final int                        flushTimeout;
    
    private class WriteThroughEntry extends WriteCacheEntry {
        
        private final byte[] buffer;
        
        protected final byte[] getBuffer() {
            return buffer;
        }
        
        private WriteThroughEntry(FileBytesCacheFlusher flusher, byte[] buffer, long fileWritePosition) {
            super(flusher, 0, fileWritePosition);
            this.buffer = buffer;
        }
        
    }
    
    private class WriteCacheEntry {
        private volatile int size;
        
        protected final int getSize() {
            return size;
        }
        
        protected final void increaseSize(int size) {
            this.size += size;
        }
        
        protected final FileBytesCacheFlusher getFlusher() {
            return flusher;
        }
        
        private final FileBytesCacheFlusher flusher;
        private final int                   writeCachePosition;
        private final long                  fileWritePosition;
        
        protected long getFileWritePosition() {
            return fileWritePosition;
        }
        
        protected final int getWriteCachePosition() {
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
    
    private final static Comparator<WriteCacheEntry> sorter = new Comparator<WriteCacheEntry>() {
                                                                
                                                                private int compare(long x, long y) {
                                                                    return (x < y) ? -1 : ((x == y) ? 0 : 1);
                                                                }
                                                                
                                                                @Override
                                                                public int compare(WriteCacheEntry o1, WriteCacheEntry o2) {
                                                                    return compare(o1.getFileWritePosition(), o2.getFileWritePosition());
                                                                }
                                                                
                                                            };
    
    public FileBytesCache(int writeCacheSize, int flushTimeout) {
        writeCache = new byte[writeCacheSize];
        this.flushTimeout = flushTimeout;
    }
    
    public synchronized void write(FileBytesCacheFlusher flusher, long fileWritePosition, byte[] readBuffer, int length) {
        final int writeCacheRemaining = writeCache.length - writeCachePosition;
        if (length > writeCacheRemaining || lastWriteCacheFlush > 0 && System.currentTimeMillis() > lastWriteCacheFlush) {
            if (contains(flusher) || lastWriteCacheFlush > 0 && System.currentTimeMillis() > lastWriteCacheFlush) {
                WriteThroughEntry writeThrough = new WriteThroughEntry(flusher, readBuffer, fileWritePosition);
                writeThrough.increaseSize(length);
                writeCacheEntries.add(writeThrough);
                flush();
            } else {
                try {
                    flusher.flush(readBuffer, 0, length, fileWritePosition);
                    flusher.flushed();
                } catch (final Throwable e) {
                    LogController.CL().log(e);
                }
            }
            return;
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
    
    private synchronized boolean contains(FileBytesCacheFlusher flusher) {
        for (WriteCacheEntry writeCacheEntry : writeCacheEntries) {
            if (writeCacheEntry.getFlusher() == flusher) { return true; }
        }
        return false;
    }
    
    public synchronized void flushIfContains(FileBytesCacheFlusher flusher) {
        if (contains(flusher)) flush();
    }
    
    public synchronized void flush() {
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
                ArrayList<WriteCacheEntry> cacheEntries = writeEntry.getValue();
                Collections.sort(cacheEntries, sorter);
                for (WriteCacheEntry writeCacheEntry : cacheEntries) {
                    if (writeCacheEntry instanceof WriteThroughEntry) {
                        WriteThroughEntry writeThrough = (WriteThroughEntry) writeCacheEntry;
                        flusher.flush(writeThrough.getBuffer(), writeCacheEntry.getWriteCachePosition(), writeCacheEntry.getSize(), writeCacheEntry.getFileWritePosition());
                    } else {
                        flusher.flush(writeCache, writeCacheEntry.getWriteCachePosition(), writeCacheEntry.getSize(), writeCacheEntry.getFileWritePosition());
                    }
                }
                flusher.flushed();
            } catch (final Throwable e) {
                LogController.CL().log(e);
            }
        }
    }
}
