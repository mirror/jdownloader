package jd.plugins.download.raf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FileBytesMap {
    
    private final static class FileBytesMapEntry {
        private final long begin;
        private long       end;
        
        private FileBytesMapEntry(final long begin) {
            this(begin, 0);
        }
        
        private FileBytesMapEntry(final long begin, final long length) {
            this.begin = begin;
            this.end = begin + length;
        }
        
        private final long getBegin() {
            return begin;
        }
        
        private final long getEnd() {
            return end;
        }
        
        private final long getLength() {
            return end - begin;
        }
        
        private final void increaseLength(long length) {
            this.end += length;
        }
        
        @Override
        public String toString() {
            return getBegin() + "-" + getEnd() + "=" + getLength();
        }
    }
    
    protected ArrayList<FileBytesMapEntry>        fileBytesMapEntries = new ArrayList<FileBytesMapEntry>();
    protected final Comparator<FileBytesMapEntry> sorter              = new Comparator<FileBytesMapEntry>() {
                                                                          
                                                                          private int compare(long x, long y) {
                                                                              return (x < y) ? -1 : ((x == y) ? 0 : 1);
                                                                          }
                                                                          
                                                                          @Override
                                                                          public int compare(FileBytesMapEntry o1, FileBytesMapEntry o2) {
                                                                              return compare(o1.getBegin(), o2.getBegin());
                                                                          }
                                                                      };
    
    protected volatile long                       finalSize           = -1;
    protected volatile long                       markedBytes         = 0;
    
    public static class FileBytesMapInfo {
        private final long finalSize;
        private final long marked;
        private final long size;
        
        public long[][] getMarkedAreas() {
            return markedAreas;
        }
        
        private final long[][] markedAreas;
        
        public long getFinalSize() {
            return finalSize;
        }
        
        public long getSize() {
            return size;
        }
        
        public long getUnMarkedBytes() {
            return Math.max(0, getSize()) - getMarkedBytes();
        }
        
        public long getMarkedBytes() {
            return marked;
        }
        
        protected FileBytesMapInfo(FileBytesMap fileBytesMap) {
            synchronized (fileBytesMap) {
                this.finalSize = fileBytesMap.getFinalSize();
                markedAreas = new long[fileBytesMap.fileBytesMapEntries.size()][2];
                for (int index = 0; index < fileBytesMap.fileBytesMapEntries.size(); index++) {
                    FileBytesMapEntry fileBytesMapEntry = fileBytesMap.fileBytesMapEntries.get(index);
                    markedAreas[index][0] = fileBytesMapEntry.getBegin();
                    markedAreas[index][1] = fileBytesMapEntry.getEnd();
                }
                this.marked = fileBytesMap.getMarkedBytes();
                this.size = fileBytesMap.getSize();
            }
        }
        
    }
    
    public long getFinalSize() {
        return finalSize;
    }
    
    public synchronized void set(FileBytesMapInfo fileBytesMapInfo) {
        reset();
        setFinalSize(fileBytesMapInfo.getFinalSize());
        for (long[] markedArea : fileBytesMapInfo.getMarkedAreas()) {
            mark(markedArea[0], markedArea[1]);
        }
    }
    
    public FileBytesMapInfo getFileBytesMapInfo() {
        return new FileBytesMapInfo(this);
    }
    
    public void setFinalSize(long finalSize) {
        this.finalSize = Math.max(-1, finalSize);
    }
    
    public synchronized boolean mark(long begin, long length) {
        for (int index = 0; index < fileBytesMapEntries.size(); index++) {
            FileBytesMapEntry fileBytesMapEntry = fileBytesMapEntries.get(index);
            if ((begin >= fileBytesMapEntry.getBegin()) && (begin <= fileBytesMapEntry.getEnd())) {
                long beginOffset = begin - fileBytesMapEntry.getBegin();
                long endOffset = beginOffset + length - fileBytesMapEntry.getLength();
                if (endOffset <= 0) return true;
                fileBytesMapEntry.increaseLength(endOffset);
                markedBytes += endOffset;
                if (index + 1 < fileBytesMapEntries.size()) {
                    FileBytesMapEntry nextFileBytesMapEntry = fileBytesMapEntries.get(index + 1);
                    long overlap = fileBytesMapEntry.getEnd() - nextFileBytesMapEntry.getBegin();
                    if (overlap > 0) {
                        markedBytes -= overlap;
                        return true;
                    }
                    return false;
                } else {
                    return false;
                }
            }
        }
        fileBytesMapEntries.add(new FileBytesMapEntry(begin, length));
        markedBytes += length;
        Collections.sort(fileBytesMapEntries, sorter);
        return false;
    }
    
    public synchronized void reset() {
        fileBytesMapEntries.clear();
        markedBytes = 0;
        finalSize = 0;
    }
    
    public synchronized long getSize() {
        long size = getFinalSize();
        if (size >= 0) return size;
        if (fileBytesMapEntries.size() > 0) { return fileBytesMapEntries.get(fileBytesMapEntries.size() - 1).getEnd(); }
        return -1;
    }
    
    public synchronized long getUnMarkedBytes() {
        return Math.max(0, getSize()) - getMarkedBytes();
    }
    
    public long getMarkedBytesLive() {
        return markedBytes;
    }
    
    public synchronized long getMarkedBytes() {
        long ret = 0;
        FileBytesMapEntry previousFileBytesMapEntry = null;
        for (int index = 0; index < fileBytesMapEntries.size(); index++) {
            FileBytesMapEntry fileBytesMapEntry = fileBytesMapEntries.get(index);
            if (previousFileBytesMapEntry != null) {
                long overlap = previousFileBytesMapEntry.getEnd() - fileBytesMapEntry.getBegin();
                if (overlap > 0) ret -= overlap;
            }
            ret += fileBytesMapEntry.getLength();
            previousFileBytesMapEntry = fileBytesMapEntry;
        }
        return ret;
    }
}
