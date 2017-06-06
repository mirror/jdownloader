package jd.plugins.download.raf;

import java.util.List;

public interface ChunkStrategy {
    
    public List<ChunkRange> getUnMarkedAreas();
    
    public List<HTTPChunk> getNextChunks(List<HTTPChunk> finishedChunks);
}
