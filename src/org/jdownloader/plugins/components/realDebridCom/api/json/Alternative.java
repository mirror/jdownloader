package org.jdownloader.plugins.components.realDebridCom.api.json;

import org.appwork.storage.Storable;

public class Alternative implements Storable {

    private String download;
    private String filename;
    private String id;

    private String quality;

    public String getDownload() {
        return download;
    }

    public String getFilename() {
        return filename;
    }

    public String getId() {
        return id;
    }

    public String getQuality() {
        return quality;
    }

    public void setDownload(String download) {
        this.download = download;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

}