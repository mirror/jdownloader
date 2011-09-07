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
    private long             averageSuccessDuration;
    private ReconnectInvoker invoker;
    private long             maxSuccessDuration;

    public long getMaxSuccessDuration() {
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
