package jd.plugins.optional.jdunrar;

import java.util.ArrayList;

public class ArchivFile {

    private String filepath;
    private int percent;

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public long getSize() {
        return size;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }
    public String toString(){
        return this.filepath;
    }

    public int getPercent() {
        return percent;
    }

    public boolean isProtected() {
        return isProtected;
    }

    private long size = 0;
    private boolean isProtected = false;
  
    private ArrayList<String> volumes;

    public ArchivFile(String name) {
        this.filepath = name;
        volumes=new ArrayList<String>();
    }

    public void setSize(long size) {
        this.size = size;

    }

    public void setProtected(boolean b) {
        this.isProtected = b;

    }

    public void addVolume(String vol) {
        if(vol==null)return;
        if(volumes.indexOf(vol)>=0)return;
        this.volumes.add(vol);
        
    }

    public ArrayList<String> getVolumes() {
        return volumes;
    }

}
