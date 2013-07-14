package org.jdownloader.extensions.extraction;

public enum ExtractionStatus {
    IDLE,
    RUNNING,
    ERROR,
    SUCCESSFUL,
    ERROR_CRC,
    ERROR_NOT_ENOUGH_SPACE,
    ERRROR_FILE_NOT_FOUND
}