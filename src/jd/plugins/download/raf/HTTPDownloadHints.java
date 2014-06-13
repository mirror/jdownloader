package jd.plugins.download.raf;

import jd.plugins.DownloadLinkDatabindingInterface;

public interface HTTPDownloadHints extends DownloadLinkDatabindingInterface {
    final String RANGE     = "RANGE_REQUEST_SUPPORTED";
    final String MAXCHUNKS = "MAX_CHUNKS_SUPPORTED";
    
    @Key(RANGE)
    public Boolean isRangeRequestSupported();
    
    @Key(RANGE)
    public void setRangeRequestSupported(Boolean b);
    
    public void reset();
    
    @Key(MAXCHUNKS)
    public Integer getMaxChunksSupported();
    
    @Key(MAXCHUNKS)
    public void setMaxChunksSupported(Integer max);
    
}
