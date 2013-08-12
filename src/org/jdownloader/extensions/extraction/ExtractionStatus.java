package org.jdownloader.extensions.extraction;

import org.jdownloader.gui.translate._GUI;

public enum ExtractionStatus {
    IDLE(null),
    RUNNING(null),
    ERROR(_GUI._.TaskColumn_getStringValue_extraction_error()),
    SUCCESSFUL(_GUI._.TaskColumn_getStringValue_extraction_success()),
    ERROR_CRC(_GUI._.TaskColumn_getStringValue_extraction_error_crc()),
    ERROR_NOT_ENOUGH_SPACE(_GUI._.TaskColumn_getStringValue_extraction_error_space()),
    ERRROR_FILE_NOT_FOUND(_GUI._.TaskColumn_getStringValue_extraction_error_file_not_found());

    private final String exp;

    private ExtractionStatus(String exp) {
        this.exp = exp;
    }

    public String getExplanation() {
        return exp;
    }
}