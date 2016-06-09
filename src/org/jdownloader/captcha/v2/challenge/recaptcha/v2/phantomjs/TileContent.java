package org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs;

public class TileContent {

    private Payload payload;
    private boolean noMatch                = false;
    private boolean asyncJsStuffInProgress = false;

    public boolean isAsyncJsStuffInProgress() {
        return asyncJsStuffInProgress;
    }

    public void setAsyncJsStuffInProgress(boolean asyncJsStuffInProgress) {
        this.asyncJsStuffInProgress = asyncJsStuffInProgress;
    }

    public boolean isNoMatch() {
        return noMatch;
    }

    public void setNoMatch(boolean noMatch) {
        this.noMatch = noMatch;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public TileContent(Payload payload) {
        this.payload = payload;
    }

}
