package org.jdownloader.api.jdanywhere.api;

import org.appwork.storage.Storable;

public class CaptchaPushRegistration implements Storable {
    private String  url;
    private boolean withSound;

    public CaptchaPushRegistration() {

    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isWithSound() {
        return withSound;
    }

    public void setWithSound(boolean withSound) {
        this.withSound = withSound;
    }
}
