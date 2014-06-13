package jd.plugins.download;


public class HashResult {
    
    private final HashInfo hashInfo;
    private final String   fileHash;
    
    public String getFileHash() {
        return fileHash;
    }
    
    public HashResult(HashInfo hashInfo, String fileHash) {
        this.hashInfo = hashInfo;
        this.fileHash = fileHash;
    }
    
    public boolean match() {
        return fileHash != null && hashInfo.getHash().equalsIgnoreCase(fileHash);
    }
    
    public HashInfo getHashInfo() {
        return hashInfo;
    }
    
    @Override
    public String toString() {
        return "HashInfo: " + hashInfo + "|HashResult:" + getFileHash() + "=" + match();
    }
}
