package org.jdownloader.plugins.components.realDebridCom.api.json;

import org.appwork.storage.Storable;

public class CheckLinkResponse implements Storable {
    public static final org.appwork.storage.TypeRef<CheckLinkResponse> TYPE = new org.appwork.storage.TypeRef<CheckLinkResponse>(CheckLinkResponse.class) {
    };

    private String                                                     filename;
    private long                                                       filesize;

    private String                                                     host;

    private String                                                     link;

    public String getFilename() {
        return filename;
    }

    public long getFilesize() {
        return filesize;
    }

    public String getHost() {
        return host;
    }

    public String getLink() {
        return link;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setLink(String link) {
        this.link = link;
    }

}
