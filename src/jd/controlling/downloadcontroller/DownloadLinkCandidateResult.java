package jd.controlling.downloadcontroller;

import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.SkipReason;

public class DownloadLinkCandidateResult {

    public static enum RESULT {
        /* DO NOT CHANGE ORDER HERE, we use compareTo which uses ordinal */
        CONNECTION_UNAVAILABLE,
        CONNECTION_ISSUES,
        FATAL_ERROR,
        RETRY,
        ACCOUNT_INVALID,
        ACCOUNT_REQUIRED,
        ACCOUNT_UNAVAILABLE,
        HOSTER_UNAVAILABLE,
        FILE_UNAVAILABLE,
        PROXY_UNAVAILABLE,
        IP_BLOCKED,
        CAPTCHA,
        PLUGIN_DEFECT,
        CONDITIONAL_SKIPPED,
        SKIPPED,
        FINISHED,
        FINISHED_EXISTS,
        FAILED,
        FAILED_INCOMPLETE,
        FAILED_EXISTS,
        OFFLINE_UNTRUSTED,
        OFFLINE_TRUSTED,
        STOPPED;
    }

    private final RESULT     result;
    private final SkipReason skipReason;

    private long             startTime = -1;

    private long             waitTime  = -1;
    private String           message   = null;

    public String getMessage() {
        return message;
    }

    protected void setMessage(String message) {
        this.message = message;
    }

    public long getWaitTime() {
        return waitTime;
    }

    protected void setWaitTime(long waitTime) {
        this.waitTime = Math.max(-1, waitTime);
    }

    public long getRemainingTime() {
        long waitTime = getWaitTime();
        long finishTime = getFinishTime();
        if (waitTime > 0 && finishTime > 0) {
            long current = System.currentTimeMillis();
            long ret = Math.max(0, (finishTime + waitTime) - current);
            return ret;
        }
        return 0;
    }

    public long getStartTime() {
        return startTime;
    }

    protected void setStartTime(long startTime) {
        this.startTime = Math.max(-1, startTime);
    }

    public long getFinishTime() {
        return finishTime;
    }

    private long finishTime = -1;

    protected void setFinishTime(long finishTime) {
        this.finishTime = Math.max(-1, finishTime);
    }

    private final ConditionalSkipReason conditionalSkip;

    public ConditionalSkipReason getConditionalSkip() {
        return conditionalSkip;
    }

    public SkipReason getSkipReason() {
        return skipReason;
    }

    public DownloadLinkCandidateResult(RESULT result) {
        this.result = result;
        this.skipReason = null;
        conditionalSkip = null;
    }

    public DownloadLinkCandidateResult(ConditionalSkipReason conditionalSkip) {
        this.result = RESULT.CONDITIONAL_SKIPPED;
        this.conditionalSkip = conditionalSkip;
        this.skipReason = null;
    }

    public DownloadLinkCandidateResult(SkipReason skipReason) {
        this.result = RESULT.SKIPPED;
        this.skipReason = skipReason;
        conditionalSkip = null;
    }

    @Override
    public String toString() {
        return "TODO";
    }

    /**
     * @return the result
     */
    public RESULT getResult() {
        return result;
    }

}
