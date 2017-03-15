package org.jdownloader.plugins.components.putio;

import org.appwork.storage.Storable;

public class PutIOFileWrapper implements Storable {
    private PutIOFile file   = null;
    private String    status = null;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private PutIOFileWrapper() {

    }

    public PutIOFile getFile() {
        return file;
    }

    public void setFile(PutIOFile file) {
        this.file = file;
    }
}