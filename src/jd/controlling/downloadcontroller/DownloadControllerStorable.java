package jd.controlling.downloadcontroller;

import org.appwork.storage.Storable;

public class DownloadControllerStorable implements Storable {

    private String rootPath  = null;
    private long   timeStamp = -1;

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public DownloadControllerStorable(/* Storable */) {
        timeStamp = System.currentTimeMillis();
    }

    /**
     * @return the timeStamp
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * @param timeStamp
     *            the timeStamp to set
     */
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
