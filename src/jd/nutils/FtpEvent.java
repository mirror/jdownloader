package jd.nutils;

import jd.event.JDEvent;

public class FtpEvent extends JDEvent {

    public static final int DOWNLOAD_PROGRESS = 0;
    private long progress;
/**
 * Returns bytes loaded
 * @return
 */
    public long getProgress() {
        return progress;
    }

    public FtpEvent(SimpleFTP source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

    public FtpEvent(SimpleFTP source, int ID, long counter) {
        this(source,ID);
        this.progress=counter;
    }

}
