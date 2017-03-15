package org.jdownloader.plugins.components.putio;

import org.appwork.storage.Storable;

public class PutIOInfoWrapper implements Storable {
    private PutIOInfo info;

    public PutIOInfoWrapper() {
    }

    public PutIOInfo getInfo() {
        return info;
    }

    public void setInfo(PutIOInfo info) {
        this.info = info;
    }
}