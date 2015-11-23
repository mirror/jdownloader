package org.jdownloader.captcha.v2;

import org.appwork.storage.Storable;

public class CaptchaQualityEnsuranceRule implements Storable {
    private int limit    = 0;
    private int interval = 0;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public CaptchaQualityEnsuranceRule(/* Storable */) {
    }

    public CaptchaQualityEnsuranceRule(int limit, int interval) {
        this.limit = limit;
        this.interval = interval;
    }
}
