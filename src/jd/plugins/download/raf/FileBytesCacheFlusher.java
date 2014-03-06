package jd.plugins.download.raf;

public interface FileBytesCacheFlusher {
    
    public void flush(byte[] writeCache, int writeCachePosition, int length, long fileWritePosition);
    
    public void flushed();
}
