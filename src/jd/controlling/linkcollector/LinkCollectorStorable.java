package jd.controlling.linkcollector;

import org.appwork.storage.Storable;

public class LinkCollectorStorable implements Storable {
    private String rootPath  = null;
    private long   timeStamp = -1;

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public LinkCollectorStorable(/* Storable */) {
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
