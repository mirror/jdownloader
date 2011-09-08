package jd.controlling.reconnect;

import org.appwork.utils.event.ProcessCallBackAdapter;

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

    private long             offlineTime;
    private long             successTime;
    private long             averageSuccessDuration = -1;
    private ReconnectInvoker invoker;
    private long             maxSuccessDuration     = -1;

    public long getMaxSuccessDuration() {
        // in case there has not been an optimization
        if (maxSuccessDuration < 0) return getSuccessDuration() * 10;
        return maxSuccessDuration;
    }

    public void setMaxSuccessDuration(long maxSuccessDuration) {
        this.maxSuccessDuration = maxSuccessDuration;
    }

    public long getOfflineDuration() {
        return offlineTime - startTime;
    }

    public long getSuccessDuration() {
        return successTime - startTime;
    }

    public long getAverageSuccessDuration() {
        // in case there has not been an optimization
        if (averageSuccessDuration < 0) return getSuccessDuration();
        return averageSuccessDuration;
    }

    public void setAverageSuccessDuration(long averageSuccessDuration) {
        this.averageSuccessDuration = averageSuccessDuration;
    }

    public void setInvoker(ReconnectInvoker reconnectInvoker) {
        this.invoker = reconnectInvoker;
    }

    public ReconnectInvoker getInvoker() {
        return invoker;
    }

    /**
     * Optimization does reconnect loops to find best average timings
     * 
     * @param processCallBackAdapter
     */
    public void optimize(ProcessCallBackAdapter processCallBackAdapter) throws InterruptedException {
        getInvoker().doOptimization(this, processCallBackAdapter);
    }

}
