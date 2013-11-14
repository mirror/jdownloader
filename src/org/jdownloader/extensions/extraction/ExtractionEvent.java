package org.jdownloader.extensions.extraction;

import org.appwork.utils.event.SimpleEvent;

public class ExtractionEvent extends SimpleEvent<ExtractionController, Object, ExtractionEvent.Type> {
    public static enum Type {
        QUEUED, // Archive was queued
        START, // Start ExtractionController
        OPEN_ARCHIVE_SUCCESS, // Extraction possible
        START_CRACK_PASSWORD, // Start password finding algo
        PASSWORT_CRACKING, // Password finding is in progress
        PASSWORD_NEEDED_TO_CONTINUE, // No Password was found. Get password from
                                     // elsewhere, like from user
        PASSWORD_FOUND, // Password found
        START_EXTRACTION, // Start extraction
        EXTRACTING, // Extracting in progress
        FINISHED, // Extraction finished
        CLEANUP, // Cleanup after extraction is finished

        EXTRACTION_FAILED, // Extraction was not successful
        EXTRACTION_FAILED_CRC, // CRC error occurred
        NOT_ENOUGH_SPACE, // Not enough space for the archive
        FILE_NOT_FOUND // Archive was not found
        ,
        ACTIVE_ITEM// new file is beeing extracted
    }

    public ExtractionEvent(ExtractionController caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }

    public ExtractionEvent(ExtractionController caller, Type type) {
        super(caller, type);
    }
}