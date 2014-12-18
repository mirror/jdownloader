package org.jdownloader.settings;

public enum MirrorDetectionDecision {

    /**
     * checks fileName(needs verified or forced fileName), fileSize(needs verifiedFileSize). fileHash is optional
     */
    SAFE,
    /**
     * checks fileName, fileSize(see ForceMirrorDetectionFileSizeCheck/MirrorDetectionFileSizeEquality). fileHash is optional
     */
    AUTO,
    /**
     * checks fileName, fileSize(see ForceMirrorDetectionFileSizeCheck/MirrorDetectionFileSizeEquality)
     */
    FILENAME_FILESIZE,
    /**
     * checks fileName
     */
    FILENAME;
}
