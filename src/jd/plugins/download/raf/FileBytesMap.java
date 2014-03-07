package jd.plugins.download.raf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.appwork.storage.Storable;

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
    
    protected final ArrayList<FileBytesMapEntry>         fileBytesMapEntries = new ArrayList<FileBytesMapEntry>();
    protected final static Comparator<FileBytesMapEntry> sorter              = new Comparator<FileBytesMapEntry>() {
                                                                                 
                                                                                 private int compare(long x, long y) {
                                                                                     return (x < y) ? -1 : ((x == y) ? 0 : 1);
                                                                                 }
                                                                                 
                                                                                 @Override
                                                                                 public int compare(FileBytesMapEntry o1, FileBytesMapEntry o2) {
                                                                                     return compare(o1.getBegin(), o2.getBegin());
                                                                                 }
                                                                             };
    
    protected volatile long                              finalSize           = -1;
    protected volatile long                              markedBytes         = 0;
    
    public static class FileBytesMapViewInterfaceStorable implements Storable {
        protected long finalSize = -1;
        
        public long getFinalSize() {
            return finalSize;
        }
        
        public void setFinalSize(long finalSize) {
            this.finalSize = finalSize;
        }
        
        public List<Long[]> getMarkedAreas() {
            return markedAreas;
        }
        
        public void setMarkedAreas(List<Long[]> markedAreas) {
            this.markedAreas = markedAreas;
        }
        
        protected List<Long[]> markedAreas = null;
        
        private FileBytesMapViewInterfaceStorable(/* Storable */) {
        }
        
        public FileBytesMapViewInterfaceStorable(FileBytesMapViewInterface fileBytesMapInfo) {
            this.finalSize = fileBytesMapInfo.getFinalSize();
            markedAreas = new ArrayList<Long[]>(fileBytesMapInfo.getMarkedAreas().length);
            for (long[] markedArea : fileBytesMapInfo.getMarkedAreas()) {
                markedAreas.add(new Long[] { markedArea[0], markedArea[1] });
            }
        }
        
        public FileBytesMapViewInterfaceStorable(FileBytesMap fileBytesMap) {
            synchronized (fileBytesMap) {
                this.finalSize = fileBytesMap.getFinalSize();
                markedAreas = new ArrayList<Long[]>(fileBytesMap.fileBytesMapEntries.size());
                for (int index = 0; index < fileBytesMap.fileBytesMapEntries.size(); index++) {
                    FileBytesMapEntry fileBytesMapEntry = fileBytesMap.fileBytesMapEntries.get(index);
                    markedAreas.add(new Long[] { fileBytesMapEntry.getBegin(), fileBytesMapEntry.getLength() });
                }
            }
        }
        
    }
    
    public static class FileBytesMapView implements FileBytesMapViewInterface {
        protected final long finalSize;
        protected final long marked;
        protected final long size;
        
        @Override
        public long[][] getMarkedAreas() {
            return markedAreas;
        }
        
        protected final long[][] markedAreas;
        
        @Override
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
        
        public FileBytesMapView(FileBytesMap fileBytesMap) {
            synchronized (fileBytesMap) {
                this.finalSize = fileBytesMap.getFinalSize();
                markedAreas = new long[fileBytesMap.fileBytesMapEntries.size()][2];
                for (int index = 0; index < fileBytesMap.fileBytesMapEntries.size(); index++) {
                    FileBytesMapEntry fileBytesMapEntry = fileBytesMap.fileBytesMapEntries.get(index);
                    markedAreas[index][0] = fileBytesMapEntry.getBegin();
                    markedAreas[index][1] = fileBytesMapEntry.getLength();
                }
                this.marked = fileBytesMap.getMarkedBytes();
                this.size = fileBytesMap.getSize();
            }
        }
    }
    
    public long getFinalSize() {
        return finalSize;
    }
    
    public synchronized void set(FileBytesMapViewInterface fileBytesMapInfo) {
        reset();
        setFinalSize(fileBytesMapInfo.getFinalSize());
        for (long[] markedArea : fileBytesMapInfo.getMarkedAreas()) {
            mark(markedArea[0], markedArea[1]);
        }
    }
    
    public void setFinalSize(long finalSize) {
        this.finalSize = Math.max(-1, finalSize);
    }
    
    public synchronized void resetMarkedBytesLive() {
        markedBytes = 0;
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
                    if (overlap >= 0) {
                        fileBytesMapEntries.remove(index + 1);
                        long overlapLength = nextFileBytesMapEntry.getLength() - overlap;
                        fileBytesMapEntry.increaseLength(overlapLength);
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
    
    public synchronized List<Long[]> getUnMarkedAreas() {
        ArrayList<Long[]> ret = new ArrayList<Long[]>();
        for (int index = 0; index < fileBytesMapEntries.size(); index++) {
            FileBytesMapEntry currentMapEntry = fileBytesMapEntries.get(index);
            if (index + 1 < fileBytesMapEntries.size()) {
                FileBytesMapEntry nextMapEntry = fileBytesMapEntries.get(index + 1);
                ret.add(new Long[] { currentMapEntry.getEnd(), nextMapEntry.getBegin() - currentMapEntry.getEnd() });
            } else {
                long finalSize = getFinalSize();
                if (finalSize >= 0) {
                    long length = finalSize - currentMapEntry.getEnd();
                    if (finalSize == 0) return ret;
                    ret.add(new Long[] { currentMapEntry.getEnd(), length });
                } else {
                    ret.add(new Long[] { currentMapEntry.getEnd(), -1l });
                }
            }
        }
        if (ret.size() == 0) {
            long finalSize = getFinalSize();
            if (finalSize == 0) {
                return ret;
            } else if (finalSize > 0) {
                ret.add(new Long[] { 0l, finalSize });
            } else {
                ret.add(new Long[] { 0l, -1l });
            }
        }
        return ret;
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
