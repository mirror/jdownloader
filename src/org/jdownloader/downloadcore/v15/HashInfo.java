package org.jdownloader.downloadcore.v15;

import jd.plugins.download.raf.HashResult;
import jd.plugins.download.raf.HashResult.TYPE;

public class HashInfo {
    private String hash;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public HashResult.TYPE getType() {
        return type;
    }

    public void setType(HashResult.TYPE type) {
        this.type = type;
    }

    private HashResult.TYPE type;

    /**
     * @param hash
     * @param type
     */
    public HashInfo(String hash, TYPE type) {
        super();
        this.hash = hash;
        this.type = type;
    }
}
