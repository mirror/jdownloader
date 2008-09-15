package jd.plugins.optional.jdunrar;

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

    public ArchivFile(String name) {
        this.filepath = name;
    }

    public void setSize(long size) {
        this.size = size;

    }

    public void setProtected(boolean b) {
        this.isProtected = b;

    }

}
