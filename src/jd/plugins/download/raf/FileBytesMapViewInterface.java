package jd.plugins.download.raf;

public interface FileBytesMapViewInterface {
    /**
     * return a list of begin/length entries of the map
     * 
     * @return
     */
    public abstract long[][] getMarkedAreas();
    
    public abstract long getFinalSize();
    
}