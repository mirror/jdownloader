package jd.plugins.download;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.FinalLinkState;

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

    public FinalLinkState getFinalLinkState() {
        if (match()) {
            switch (getHashInfo().getType()) {
            case SHA256:
                return FinalLinkState.FINISHED_SHA256;
            case SHA1:
                return FinalLinkState.FINISHED_SHA1;
            case MD5:
                return FinalLinkState.FINISHED_MD5;
            case CRC32:
                return FinalLinkState.FINISHED_CRC32;
            default:
                return FinalLinkState.FINISHED;
            }
        } else {
            switch (getHashInfo().getType()) {
            case SHA256:
                return FinalLinkState.FAILED_SHA256;
            case SHA1:
                return FinalLinkState.FAILED_SHA1;
            case MD5:
                return FinalLinkState.FAILED_MD5;
            case CRC32:
                return FinalLinkState.FAILED_CRC32;
            default:
                return FinalLinkState.FAILED;
            }
        }
    }

    public boolean match() {
        return StringUtils.equalsIgnoreCase(hashInfo.getHash(), fileHash);
    }

    public HashInfo getHashInfo() {
        return hashInfo;
    }

    @Override
    public String toString() {
        return "HashInfo: " + hashInfo + "|HashResult:" + getFileHash() + "=" + match();
    }
}
