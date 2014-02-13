package jd.controlling.downloadcontroller;

public abstract class AbstractWaittimeException extends Exception {
    private long waittime = -1;

    public long getWaittime() {
        return waittime;
    }

    public void setWaittime(long waittime) {
        this.waittime = waittime;
    }

    public AbstractWaittimeException(Throwable e, long waittime) {
        super(e);
        this.waittime = waittime;

    }

}
