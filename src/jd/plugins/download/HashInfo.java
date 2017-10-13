package jd.plugins.download;

import java.util.Locale;

import org.appwork.utils.StringUtils;

public class HashInfo {
    public static enum TYPE {
        // order is important! see isStrongerThan/isWeakerThan
        SHA512("SHA-512", 128),
        SHA384("SHA-384", 96),
        SHA256("SHA-256", 64),
        SHA224("SHA-224", 56),
        SHA1("SHA1", 40),
        MD5("MD5", 32),
        CRC32("CRC32", 8),
        NONE("NONE", 0);
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

    public boolean isStrongerThan(final HashInfo hashInfo) {
        if (hashInfo != null) {
            return getType().ordinal() < hashInfo.getType().ordinal();
        } else {
            return true;
        }
    }

    public boolean isWeakerThan(final HashInfo hashInfo) {
        if (hashInfo != null) {
            return getType().ordinal() > hashInfo.getType().ordinal();
        } else {
            return false;
        }
    }

    private final String hash;

    public String getHash() {
        return hash;
    }

    public TYPE getType() {
        return type;
    }

    public boolean isNone() {
        return TYPE.NONE.equals(type);
    }

    private final TYPE    type;
    private final boolean trustworthy;
    private final boolean forced;

    public boolean isTrustworthy() {
        return trustworthy;
    }

    public boolean isForced() {
        return forced;
    }

    public String exportAsString() {
        return getType() + "|" + (isTrustworthy() ? "1" : "0") + "|" + (isForced() ? "1" : "0") + "|" + getHash();
    }

    public static HashInfo parse(final String hash) {
        return parse(hash, true, false);
    }

    public static HashInfo parse(final String hash, boolean isTrustWorthy, boolean isForced) {
        if (hash != null) {
            for (final TYPE type : TYPE.values()) {
                if (type.getSize() == hash.length()) {
                    return new HashInfo(hash, type, isTrustWorthy, isForced);
                }
            }
        }
        return null;
    }

    public static HashInfo importFromString(final String hashInfo) {
        if (hashInfo != null) {
            try {
                final String parts[] = hashInfo.split("\\|");
                if (parts != null) {
                    if (parts.length == 3) {
                        final TYPE type = TYPE.valueOf(parts[0]);
                        final boolean trustworthy = "1".equals(parts[1]);
                        return new HashInfo(parts[2], type, trustworthy);
                    } else if (parts.length == 4) {
                        final TYPE type = TYPE.valueOf(parts[0]);
                        final boolean trustworthy = "1".equals(parts[1]);
                        final boolean forced = "1".equals(parts[2]);
                        return new HashInfo(parts[3], type, trustworthy, forced);
                    }
                }
            } catch (final Throwable e) {
            }
        }
        return null;
    }

    public HashInfo(String hash, TYPE type, boolean trustworthy, boolean forced) {
        if (type == null) {
            throw new IllegalArgumentException("type is missing");
        }
        this.type = type;
        if (TYPE.NONE.equals(type)) {
            this.hash = "";
        } else {
            if (StringUtils.isEmpty(hash)) {
                throw new IllegalArgumentException("hash is empty");
            } else if (hash.length() < type.getSize()) {
                hash = String.format("%0" + (type.getSize() - hash.length()) + "d%s", 0, hash);
            } else if (hash.length() > type.getSize()) {
                throw new IllegalArgumentException("invalid hash size");
            }
            this.hash = hash.toLowerCase(Locale.ENGLISH);
        }
        this.trustworthy = trustworthy;
        this.forced = forced;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof HashInfo) {
            final HashInfo other = (HashInfo) obj;
            return other.getType().equals(getType()) && other.getHash().equals(getHash());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getHash().hashCode();
    }

    /**
     * @param hash
     * @param type
     */
    public HashInfo(String hash, TYPE type, boolean trustworthy) {
        this(hash, type, trustworthy, false);
    }

    public HashInfo(String hash, TYPE type) {
        this(hash, type, true);
    }

    public static HashInfo newInstanceSafe(String hash, TYPE type) {
        return newInstanceSafe(hash, type, true);
    }

    public static HashInfo newInstanceSafe(String hash, TYPE type, boolean trustworthy) {
        return newInstanceSafe(hash, type, trustworthy, false);
    }

    public static HashInfo newInstanceSafe(String hash, TYPE type, boolean trustworthy, boolean forced) {
        try {
            return new HashInfo(hash, type, trustworthy, forced);
        } catch (IllegalArgumentException e) {
        }
        return null;
    }

    @Override
    public String toString() {
        return "HashInfo:TYPE:" + type + "|Hash:" + hash + "|Trustworthy:" + trustworthy + "|Forced:" + forced;
    }
}
