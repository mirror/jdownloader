package org.jdownloader.extensions.eventscripter.sandboxobjects;

import jd.plugins.DownloadLink;

import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.DownloadLinkCondition;
import org.jdownloader.plugins.TimeOutCondition;
import org.jdownloader.plugins.WaitingSkipReason;

public class ConditionalSkipReasonSandbox {
    private final ConditionalSkipReason conditionalSkipReason;
    private final DownloadLink          downloadLink;

    public ConditionalSkipReasonSandbox(DownloadLink downloadLink, ConditionalSkipReason conditionalSkipReason) {
        this.conditionalSkipReason = conditionalSkipReason;
        this.downloadLink = downloadLink;
    }

    public boolean isConditionReached() {
        return conditionalSkipReason.isConditionReached();
    }

    public String getMessage() {
        return conditionalSkipReason.getMessage(conditionalSkipReason, downloadLink);
    }

    public String getClassName() {
        return conditionalSkipReason.getClass().getSimpleName();
    }

    public boolean isTimeOutCondition() {
        return conditionalSkipReason instanceof TimeOutCondition;
    }

    public boolean isDownloadLinkCondition() {
        return conditionalSkipReason instanceof DownloadLinkCondition;
    }

    public DownloadLinkSandBox getDownloadLinkCondition() {
        return new DownloadLinkSandBox(((DownloadLinkCondition) conditionalSkipReason).getDownloadLink());
    }

    public long getTimeOutTimeStamp() {
        if (isTimeOutCondition()) {
            return ((TimeOutCondition) conditionalSkipReason).getTimeOutTimeStamp();
        } else {
            return -1;
        }
    }

    public String getWaitingSkipReason() {
        if (conditionalSkipReason instanceof WaitingSkipReason) {
            return ((WaitingSkipReason) conditionalSkipReason).getCause().name();
        } else {
            return null;
        }
    }

    public long getTimeOutLeft() {
        if (isTimeOutCondition()) {
            return ((TimeOutCondition) conditionalSkipReason).getTimeOutLeft();
        } else {
            return -1;
        }
    }
}
