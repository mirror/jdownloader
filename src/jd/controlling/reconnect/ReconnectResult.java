package jd.controlling.reconnect;

public class ReconnectResult {
    public ReconnectResult() {

    }

    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    private long startTime;

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getOfflineTime() {
        return offlineTime;
    }

    public void setOfflineTime(long offlineTime) {
        this.offlineTime = offlineTime;
    }

    public long getSuccessTime() {
        return successTime;
    }

    public void setSuccessTime(long successTime) {
        this.successTime = successTime;
    }

    private long offlineTime;
    private long successTime;

    public long getOfflineDuration() {
        return offlineTime - startTime;
    }

    public long getSuccessDuration() {
        return successTime - startTime;
    }
}
