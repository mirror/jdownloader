package org.jdownloader.extensions.extraction;

import java.awt.Color;

public interface ArchiveFile {

    public static enum Status {
        IDLE,
        RUNNING,
        ERROR,
        SUCCESSFUL,
        ERROR_CRC,
        ERROR_NOT_ENOUGH_SPACE,
        ERRROR_FILE_NOT_FOUND
    }

    public boolean isComplete();

    public String getFilePath();

    public long getFileSize();

    /**
     * returns false if this file matches the patterns, but is not a valid archive file due to other reasons. for example, because it
     * contains a downloadlink that has not been downloaded.
     * 
     * @return
     */
    public boolean isValid();

    public boolean deleteFile();

    public void deleteLink();

    public boolean exists();

    public String getName();

    public void setStatus(Status error);

    public void setMessage(String plugins_optional_extraction_status_notenoughspace);

    public void setProgress(long value, long max, Color color);

    public void onCleanedUp(ExtractionController controller);

}
