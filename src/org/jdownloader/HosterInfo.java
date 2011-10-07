package org.jdownloader;

public class HosterInfo {
    private HosterInfo(String tld) {
        this.tld = tld;
    }

    private String tld;

    public String getTld() {
        return tld;
    }

    public void setTld(String tld) {
        this.tld = tld;
    }

    public static HosterInfo getInstance(String host) {
        return new HosterInfo(host);
    }
}
