package org.jdownloader.plugins.components.usenet;


import jd.plugins.download.HashInfo;

import org.appwork.storage.Storable;

public class UsenetFileSegment implements Storable {
    private int    index     = -1;
    private long   partBegin = -1;
    private String hash      = null;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void _setHashInfo(HashInfo hashInfo) {
        if (hashInfo != null) {
            this.hash = hashInfo.exportAsString();
        } else {
            this.hash = null;
        }
    }

    public HashInfo _getHashInfo() {
        return HashInfo.importFromString(hash);
    }

    public long getPartBegin() {
        return partBegin;
    }

    public void setPartBegin(long partBegin) {
        this.partBegin = partBegin;
    }

    public long getPartEnd() {
        return partEnd;
    }

    public void setPartEnd(long partEnd) {
        this.partEnd = partEnd;
    }

    private long partEnd = -1;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    private long   size      = -1;
    private String messageID = null;

    public UsenetFileSegment() {
    }

    public UsenetFileSegment(final int index, final long bytes, final String messageID) {
        this.index = index;
        this.size = bytes;
        this.messageID = messageID;
    }

}
