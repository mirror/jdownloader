package org.jdownloader.captcha.v2.solver.solver9kw;

import org.appwork.utils.parser.UrlQuery;

public class RequestOptions {
    private String   proxyhostport;
    private String   proxytype;
    private int      cph;
    private int      cpm;
    private int      priothing;
    private int      timeoutthing;
    private UrlQuery moreoptions = new UrlQuery();

    public UrlQuery getMoreoptions() {
        return moreoptions;
    }

    public void setMoreoptions(UrlQuery moreoptions) {
        this.moreoptions = moreoptions;
    }

    public int getCph() {
        return cph;
    }

    public void setCph(int cph) {
        this.cph = cph;
    }

    public int getCpm() {
        return cpm;
    }

    public void setCpm(int cpm) {
        this.cpm = cpm;
    }

    public int getPriothing() {
        return priothing;
    }

    public void setPriothing(int priothing) {
        this.priothing = priothing;
    }

    public int getTimeoutthing() {
        return timeoutthing;
    }

    public String getproxytype() {
        return proxytype;
    }

    public String getproxyhostport() {
        return proxyhostport;
    }

    public void setTimeoutthing(int timeoutthing) {
        this.timeoutthing = timeoutthing;
    }

    public boolean isSelfsolve() {
        return selfsolve;
    }

    public void setSelfsolve(boolean selfsolve) {
        this.selfsolve = selfsolve;
    }

    public boolean isConfirm() {
        return confirm;
    }

    public void setConfirm(boolean confirm) {
        this.confirm = confirm;
    }

    private boolean selfsolve;
    private boolean confirm;

    public RequestOptions(Captcha9kwSettings config) {
        cph = config.gethour();
        cpm = config.getminute();
        priothing = config.getprio();
        proxyhostport = config.getproxyhostport();
        proxytype = config.getproxytype();
        timeoutthing = (config.getDefaultMaxTimeout() / 1000);
        selfsolve = config.isSelfsolve();
        confirm = config.isconfirm();
    }
}
