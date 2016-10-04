package jd.plugins.download;

import java.util.Locale;

import org.appwork.utils.StringUtils;

public class HashInfo {

    public static enum TYPE {
        // order is important!
        SHA512("SHA-512", 128),
        SHA256("SHA-256", 64),
        SHA1("SHA1", 40),
        MD5("MD5", 32),
        CRC32("CRC32", 8);

        private final String digest;
        private final int    size;

        public final String getDigest() {
            return digest;
        }

        private TYPE(final String digest, int size) {
            this.digest = digest;
            this.size = size;
        }

        public final int getSize() {
            return size;
        }
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

    public String exportAsString() {
        return getType() + "|" + (isTrustworthy() ? "1" : "0") + "|" + getHash();
    }

    public static HashInfo parse(final String hash) {
        if (hash != null) {
            for (final TYPE type : TYPE.values()) {
                if (type.size == hash.length()) {
                    return new HashInfo(hash, type, true);
                }
            }
        }
        return null;
    }

    public static HashInfo importFromString(final String hashInfo) {
        if (hashInfo != null) {
            try {
                final String parts[] = hashInfo.split("\\|");
                if (parts != null && parts.length == 3) {
                    final TYPE type = TYPE.valueOf(parts[0]);
                    final boolean trustworthy = "1".equals(parts[1]);
                    return new HashInfo(parts[2], type, trustworthy);
                }
            } catch (final Throwable e) {
            }
        }
        return null;
    }

    /**
     * @param hash
     * @param type
     */
    public HashInfo(String hash, TYPE type, boolean trustworthy) {
        if (StringUtils.isEmpty(hash)) {
            throw new IllegalArgumentException("hash is empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is missing");
        }
        this.type = type;
        if (hash.length() < type.getSize()) {
            hash = String.format("%0" + (type.getSize() - hash.length()) + "d%s", 0, hash);
        } else if (hash.length() > type.getSize()) {
            throw new IllegalArgumentException("invalid hash size");
        }
        this.hash = hash.toLowerCase(Locale.ENGLISH);
        this.trustworthy = trustworthy;
    }

    public HashInfo(String hash, TYPE type) {
        this(hash, type, true);
    }

    public static HashInfo newInstanceSafe(String hash, TYPE type) {
        return newInstanceSafe(hash, type, true);
    }

    public static HashInfo newInstanceSafe(String hash, TYPE type, boolean trustworthy) {
        try {
            return new HashInfo(hash, type, trustworthy);
        } catch (IllegalArgumentException e) {
        }
        return null;
    }

    @Override
    public String toString() {
        return "HashInfo:TYPE:" + type + "|Hash:" + hash + "|Trustworthy:" + trustworthy;
    }
}
