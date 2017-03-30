package jd.controlling.downloadcontroller;

import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.SkipReason;

import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

public class DownloadLinkCandidateResult {
    public static enum RESULT {
        /* DO NOT CHANGE ORDER HERE, we use compareTo(sorting) which uses ordinal */
        CONNECTION_TEMP_UNAVAILABLE,
        CONNECTION_ISSUES,
        FATAL_ERROR,
        RETRY,
        ACCOUNT_INVALID,
        ACCOUNT_ERROR,
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
        OFFLINE_TRUSTED,
        STOPPED,
        COUNTRY_BLOCKED;
    }

    private final RESULT     result;
    private final SkipReason skipReason;
    private long             startTime = -1;
    private long             waitTime  = -1;
    private String           message   = null;
    private final String     lastPluginHost;
    private final boolean    reachedDownloadInterface;
    private final long       timeStamp = System.currentTimeMillis();

    public long getTimeStamp() {
        return timeStamp;
    }

    public boolean isReachedDownloadInterface() {
        return reachedDownloadInterface;
    }

    public String getLastPluginHost() {
        return lastPluginHost;
    }

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
        final long finishTime = getFinishTime();
        final long waitTime = getWaitTime();
        if (finishTime > 0 && waitTime > 0) {
            final long current = System.currentTimeMillis();
            final long ret = Math.max(0, (finishTime + waitTime) - current);
            return ret;
        }
        return -1;
    }

    public long getStartTime() {
        return startTime;
    }

    protected void setStartTime(long startTime) {
        this.startTime = Math.max(-1, startTime);
    }

    public long getFinishTime() {
        if (finishTime == -1) {
            return getTimeStamp();
        } else {
            return finishTime;
        }
    }

    private long finishTime = -1;

    protected void setFinishTime(long finishTime) {
        this.finishTime = Math.max(-1, finishTime);
    }

    private final ConditionalSkipReason conditionalSkip;
    private final Throwable             throwable;

    public ConditionalSkipReason getConditionalSkip() {
        return conditionalSkip;
    }

    public SkipReason getSkipReason() {
        return skipReason;
    }

    public String getErrorID() {
        final Throwable throwable = getThrowable();
        final RESULT result = getResult();
        if (throwable != null) {
            StackTraceElement[] st = throwable.getStackTrace();
            if (st != null && st.length > 0) {
                StringBuilder sb = new StringBuilder();
                StringBuilder sb2 = new StringBuilder();
                if (throwable instanceof PluginException) {
                    sb.append("PluginException: ").append(throwable.getMessage()).append("(" + LinkStatus.toString(((PluginException) throwable).getLinkStatus()) + ")");
                    // let's test without. this should help finding the host
                    // } else if (throwable instanceof UnknownHostException) {
                    // sb.append(UnknownHostException.class.getSimpleName());
                } else {
                    // not the localized message
                    sb.append(throwable.getClass().getName() + " : " + throwable.getMessage());
                }
                sb2.append(sb.toString());
                boolean found = false;
                boolean found2 = false;
                for (int i = 0; i < st.length; i++) {
                    String line = st[i].toString();
                    if (sb.length() > 0) {
                        sb.append("\r\n");
                    }
                    sb.append(line);
                    if (!found2) {
                        if (sb2.length() > 0) {
                            sb2.append("\r\n");
                        }
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
                    return sb.toString();
                } else {
                    if (found2) {
                        return sb2.toString();
                    } else {
                        return st[0].toString();
                    }
                }
            } else {
                return throwable.toString();
            }
        } else if (result != null) {
            switch (result) {
            case FAILED:
            case FAILED_INCOMPLETE:
            case FATAL_ERROR:
            case FILE_UNAVAILABLE:
            case HOSTER_UNAVAILABLE:
            case PLUGIN_DEFECT:
                return result.name();
            default:
                return null;
            }
        }
        return null;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public DownloadLinkCandidateResult(ConditionalSkipReason conditionalSkip, Throwable throwable, String lastPluginHost) {
        this(conditionalSkip, throwable, lastPluginHost, false);
    }

    public DownloadLinkCandidateResult(ConditionalSkipReason conditionalSkip, Throwable throwable, String lastPluginHost, boolean reachedDownloadInterface) {
        this(RESULT.CONDITIONAL_SKIPPED, conditionalSkip, null, throwable, lastPluginHost, reachedDownloadInterface);
    }

    private DownloadLinkCandidateResult(RESULT result, ConditionalSkipReason conditionalSkip, SkipReason skipReason, Throwable throwable, String lastPluginHost, boolean reachedDownloadInterface) {
        this.result = result;
        this.skipReason = skipReason;
        this.lastPluginHost = lastPluginHost;
        this.conditionalSkip = conditionalSkip;
        this.throwable = throwable;
        this.reachedDownloadInterface = reachedDownloadInterface;
    }

    public DownloadLinkCandidateResult(RESULT result, Throwable throwable, String lastPluginHost) {
        this(result, throwable, lastPluginHost, false);
    }

    public DownloadLinkCandidateResult(RESULT result, Throwable throwable, String lastPluginHost, boolean reachedDownloadInterface) {
        this(result, null, null, throwable, lastPluginHost, reachedDownloadInterface);
    }

    public DownloadLinkCandidateResult(SkipReason skipReason, Throwable throwable, String lastPluginHost, boolean reachedDownloadInterface) {
        this(RESULT.SKIPPED, null, skipReason, throwable, lastPluginHost, reachedDownloadInterface);
    }

    public DownloadLinkCandidateResult(SkipReason skipReason, Throwable throwable, String lastPluginHost) {
        this(skipReason, throwable, lastPluginHost, false);
    }

    @Override
    public String toString() {
        return "RESULT:" + getResult() + "|SkipReason:" + getSkipReason() + "|Message:" + getMessage() + "|Wait:" + getWaitTime() + "|ReachedDownloadInterface:" + isReachedDownloadInterface();
    }

    /**
     * @return the result
     */
    public RESULT getResult() {
        return result;
    }
}
