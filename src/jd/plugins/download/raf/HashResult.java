package jd.plugins.download.raf;

public class HashResult {

    public static enum TYPE {
        MD5,
        CRC32,
        SHA1
    }

    private final String hashExpected;

    public String getHashExpected() {
        return hashExpected;
    }

    public String getHashLocal() {
        return hashLocal;
    }

    public TYPE getType() {
        return type;
    }

    private final String hashLocal;
    private final TYPE   type;

    public HashResult(String hashExpected, String hashLocal, TYPE type) {
        this.hashExpected = hashExpected;
        this.hashLocal = hashLocal;
        this.type = type;
    }

    public boolean hashMatch() {
        return hashExpected != null && hashExpected.equalsIgnoreCase(hashLocal);
    }
}
