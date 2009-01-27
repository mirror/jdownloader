package jd.http.download;

public abstract class DownloadChunkInterface {

    /**
     * chunkStart. The first Byte that contains to the chunk. border included
     */
    private long chunkStart = 0;
    /**
     * The last Byte that contains to the chunk. Border included
     */
    private long chunkEnd = 0;

    abstract public long getWritePosition();

    public long getChunkStart() {
        return chunkStart;
    }

    public void setChunkStart(long chunkStart) {
        this.chunkStart = chunkStart;
    }

    public long getChunkEnd() {
        return chunkEnd;
    }

    public void setChunkEnd(long chunkEnd) {
        System.out.println(this + " Chunkend to " + chunkEnd);
        this.chunkEnd = chunkEnd;
    }
    abstract public boolean isConnected();
    abstract  public boolean isAlive();
    abstract public long getRemainingChunkBytes();
    abstract public long getChunkBytes();
    abstract     public long getSpeed();
}
