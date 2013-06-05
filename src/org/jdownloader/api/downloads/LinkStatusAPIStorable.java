package org.jdownloader.api.downloads;

import jd.plugins.LinkStatus;

import org.appwork.storage.Storable;

public class LinkStatusAPIStorable implements Storable {

    public LinkStatusAPIStorable() {
        /* Storable */
    }

    private LinkStatus ls;

    public LinkStatusAPIStorable(LinkStatus ls) {
        this.ls = ls;
    }

    public String getErrorMessage() {
        return ls.getErrorMessage();
    }

    public Integer getStauts() {
        return ls.getStatus();
    }

    public String getStatusText() {
        return ls.getStatusText();
    }

    public Long getValue() {
        return ls.getValue();
    }

    public Integer getRetryCount() {
        return ls.getRetryCount();
    }

    public Long getRemainingWaittime() {
        return ls.getRemainingWaittime();
    }

    public Boolean getPluginInProgress() {
        return ls.isPluginInProgress();
    }

    public Boolean getFinished() {
        return ls.isFinished();
    }

    public Boolean getFailed() {
        return ls.isFailed();
    }

    public Long getWaitTime() {
        return ls.getWaitTime();
    }

    public Boolean getPluginActive() {
        return ls.isPluginActive();
    }

    public Integer getLatestStatus() {
        return ls.getLatestStatus();
    }
}
