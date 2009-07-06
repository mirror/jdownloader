package jd.plugins;

import jd.config.Property;

/**
 * Class for collection hosterinformation. must be extended.
 * 
 * @author Coalado
 */
public class HosterInfo extends Property {

    private static final long serialVersionUID = 4558692274458602589L;
    /**
     * allowd chunknum in freemode
     */
    private int freeChunks = 1;
    /**
     * ip blocked waittime after download (max)
     */
    private int freeIPBlockWaittime = -1;
    /**
     * max freespeed possible
     */
    private int freeMaxSpeed = -1;
    /**
     * ax free waittime before the download starts
     */
    private long freeMaxWaittime = -1;
    /**
     * allowed paralell downloads in freemode
     */
    private int freeParalellDownloads = 1;
    /**
     * Resumable in free mode?
     */
    private boolean freeResumable = false;
    /**
     * domain
     */
    private String host;

    public HosterInfo(String host) {
        this.host = host;
    }

    public int getFreeChunks() {
        return freeChunks;
    }

    public int getFreeIPBlockWaittime() {
        return freeIPBlockWaittime;
    }

    public int getFreeMaxSpeed() {
        return freeMaxSpeed;
    }

    public long getFreeMaxWaittime() {
        return freeMaxWaittime;
    }

    public int getFreeParalellDownloads() {
        return freeParalellDownloads;
    }

    public String getHost() {
        return host;
    }

    public boolean isFreeResumable() {
        return freeResumable;
    }

    public void setFreeChunks(int i) {
        this.freeChunks = i;
    }

    public void setFreeIPBlockWaittime(int i) {
        this.freeIPBlockWaittime = i;
    }

    public void setFreeMaxSpeed(int i) {
        this.freeMaxSpeed = i;
    }

    public void setFreeMaxWaittime(long i) {
        this.freeMaxWaittime = i;
    }

    public void setFreeParalellDownloads(int i) {
        this.freeParalellDownloads = i;
    }

    public void setFreeResumable(boolean b) {
        this.freeResumable = b;
    }

    public void setHost(String host) {
        this.host = host;
    }

}
