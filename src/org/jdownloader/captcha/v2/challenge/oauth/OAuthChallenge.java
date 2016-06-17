package org.jdownloader.captcha.v2.challenge.oauth;

import org.jdownloader.captcha.v2.Challenge;

public abstract class OAuthChallenge extends Challenge<Boolean> {

    public OAuthChallenge(String method, String url, String explain) {
        super(method, explain);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private String url;
}
