package org.jdownloader.plugins.components.realDebridCom.api.json;

import org.appwork.storage.Storable;

public class CodeResponse implements Storable {
    public static final org.appwork.storage.TypeRef<CodeResponse> TYPE = new org.appwork.storage.TypeRef<CodeResponse>(CodeResponse.class) {
                                                                       };
    private String                                                device_code;

    private long                                                  expires_in;

    private long                                                  interval;
    private String                                                direct_verification_url;

    public String getDirect_verification_url() {
        return direct_verification_url;
    }

    public void setDirect_verification_url(String direct_verification_url) {
        this.direct_verification_url = direct_verification_url;
    }

    private String user_code;

    private String verification_url;

    public CodeResponse(/* Storable */) {
    }

    public String getDevice_code() {
        return device_code;
    }

    public long getExpires_in() {
        return expires_in;
    }

    public long getInterval() {
        return interval;
    }

    public String getUser_code() {
        return user_code;
    }

    public String getVerification_url() {
        return verification_url;
    }

    public void setDevice_code(String device_code) {
        this.device_code = device_code;
    }

    public void setExpires_in(long expires_in) {
        this.expires_in = expires_in;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public void setUser_code(String user_code) {
        this.user_code = user_code;
    }

    public void setVerification_url(String verification_url) {
        this.verification_url = verification_url;
    }
}