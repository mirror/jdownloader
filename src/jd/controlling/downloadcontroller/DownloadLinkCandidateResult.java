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

    private String                      errorID;

    public String getErrorID() {
        return errorID;
    }

    public ConditionalSkipReason getConditionalSkip() {
        return conditionalSkip;
    }

    public SkipReason getSkipReason() {
        return skipReason;
    }

    public DownloadLinkCandidateResult(RESULT result, Throwable throwable) {
        this.result = result;
        this.skipReason = null;
        conditionalSkip = null;
        updateErrorID(throwable);

    }

    private void updateErrorID(Throwable throwable) {
        errorID = null;

        if (throwable != null) {
            StackTraceElement[] st = throwable.getStackTrace();
            if (st != null && st.length > 0) {
                StringBuilder sb = new StringBuilder();
                StringBuilder sb2 = new StringBuilder();
                sb.append(throwable.toString());
                sb2.append(throwable.toString());

                boolean found = false;
                boolean found2 = false;
                for (int i = 0; i < st.length; i++) {
                    String line = st[i].toString();

                    if (sb.length() > 0) sb.append("\r\n");
                    sb.append(line);
                    if (!found2) {
                        if (sb2.length() > 0) sb2.append("\r\n");
                        sb2.append(line);
                    }
                    if (st[i].getLineNumber() >= 0) {
                        found2 = true;
                    }
                    if (line.startsWith("jd.plugins.hoster.")) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    errorID = sb.toString();
                } else {
                    if (found2) {
                        errorID = sb2.toString();
                    } else {
                        errorID = st[0].toString();
                    }
                }
            } else {
                errorID = throwable.toString();
            }

        }
    }

    public DownloadLinkCandidateResult(ConditionalSkipReason conditionalSkip, Throwable throwable) {
        this.result = RESULT.CONDITIONAL_SKIPPED;
        this.conditionalSkip = conditionalSkip;
        this.skipReason = null;
        updateErrorID(throwable);
    }

    public DownloadLinkCandidateResult(SkipReason skipReason, Throwable throwable) {
        this.result = RESULT.SKIPPED;
        this.skipReason = skipReason;
        conditionalSkip = null;
        updateErrorID(throwable);
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
