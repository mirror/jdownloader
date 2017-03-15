package org.jdownloader.plugins.components.putio;

import org.appwork.storage.Storable;

public class PutIOInfo implements Storable {
    private String access_token = null;

    public PutIOInfo() {
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }
}