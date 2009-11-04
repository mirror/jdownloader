//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
    private int freeParallelDownloads = 1;
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

    public int getFreeParallelDownloads() {
        return freeParallelDownloads;
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

    public void setFreeParallelDownloads(int i) {
        this.freeParallelDownloads = i;
    }

    public void setFreeResumable(boolean b) {
        this.freeResumable = b;
    }

    public void setHost(String host) {
        this.host = host;
    }

}
