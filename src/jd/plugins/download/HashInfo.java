package jd.plugins.download;

public class HashInfo {
    
    public static enum TYPE {
        MD5,
        CRC32,
        SHA1
    }
    
    private final String hash;
    
    public String getHash() {
        return hash;
    }
    
    public TYPE getType() {
        return type;
    }
    
    private final TYPE    type;
    private final boolean trustworthy;
    
    public boolean isTrustworthy() {
        return trustworthy;
    }
    
    /**
     * @param hash
     * @param type
     */
    public HashInfo(String hash, TYPE type, boolean trustworthy) {
        super();
        this.hash = hash;
        this.type = type;
        this.trustworthy = trustworthy;
    }
    
    public HashInfo(String hash, TYPE type) {
        this(hash, type, true);
    }
    
    @Override
    public String toString() {
        return "HashInfo:TYPE:" + type + "|Hash:" + hash + "|Trustworthy:" + trustworthy;
    }
}
