package org.jdownloader.extensions.extraction;

import java.awt.Color;

public interface ArchiveFile {

    public boolean isComplete();

    public String getFilePath();

    public long getFileSize();

    public void deleteFile();

    public void deleteLink();

    public boolean exists();

    public String getName();

    public void setStatus(ExtractionStatus error);

    public void setMessage(String plugins_optional_extraction_status_notenoughspace);

    public void setProgress(long value, long max, Color color);

    public void onCleanedUp(ExtractionController controller);

    public void setArchive(Archive archive);

}
