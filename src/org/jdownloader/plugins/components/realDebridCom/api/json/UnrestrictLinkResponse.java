package org.jdownloader.plugins.components.realDebridCom.api.json;

import java.util.ArrayList;

import org.appwork.storage.Storable;

public class UnrestrictLinkResponse implements Storable {
    public static final org.appwork.storage.TypeRef<UnrestrictLinkResponse> TYPE = new org.appwork.storage.TypeRef<UnrestrictLinkResponse>(UnrestrictLinkResponse.class) {
                                                                                 };
    private ArrayList<Alternative>                                          alternative;
    private long                                                            chunks;
    private long                                                            crc;
    private String                                                          download;
    private String                                                          filename;
    private long                                                            filesize;

    private String                                                          host;

    private String                                                          id;

    private String                                                          link;

    private String                                                          quality;

    private long                                                            streamable;

    public ArrayList<Alternative> getAlternative() {
        return alternative;
    }

    public long getChunks() {
        return chunks;
    }

    public long getCrc() {
        return crc;
    }

    public String getDownload() {
        return download;
    }

    public String getFilename() {
        return filename;
    }

    public long getFilesize() {
        return filesize;
    }

    public String getHost() {
        return host;
    }

    public String getId() {
        return id;
    }

    public String getLink() {
        return link;
    }

    public String getQuality() {
        return quality;
    }

    public long getStreamable() {
        return streamable;
    }

    public void setAlternative(ArrayList<Alternative> alternative) {
        this.alternative = alternative;
    }

    public void setChunks(long chunks) {
        this.chunks = chunks;
    }

    public void setCrc(long crc) {
        this.crc = crc;
    }

    public void setDownload(String download) {
        this.download = download;
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

    public void setId(String id) {
        this.id = id;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public void setStreamable(long streamable) {
        this.streamable = streamable;
    }

}