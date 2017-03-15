package org.jdownloader.plugins.components.putio;

import org.appwork.storage.Storable;

public class PutIOFile implements Storable {
    private String crc32 = null;

    private String name  = null;

    private long   size  = -1;

    public PutIOFile() {
    }

    public String getCrc32() {
        return crc32;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public void setCrc32(String crc32) {
        this.crc32 = crc32;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSize(long size) {
        this.size = size;
    }
}