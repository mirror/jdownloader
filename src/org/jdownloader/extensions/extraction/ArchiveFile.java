package org.jdownloader.extensions.extraction;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdownloader.controlling.FileCreationManager;

public interface ArchiveFile {

    public static class ArchiveID {
        private final String archiveID;

        public final String getArchiveID() {
            return archiveID;
        }

        public final int getScore() {
            return score.get();
        }

        public void increaseScore() {
            score.incrementAndGet();
        }

        private final AtomicInteger score = new AtomicInteger(1);

        public ArchiveID(final String archiveID) {
            this.archiveID = archiveID;
        }
    }

    public Boolean isComplete();

    public String getFilePath();

    public long getFileSize();

    public void deleteFile(FileCreationManager.DeleteOption option);

    public boolean exists();

    public void invalidateExists();

    public String getName();

    public void setStatus(ExtractionController controller, ExtractionStatus error);

    public void setMessage(ExtractionController controller, String plugins_optional_extraction_status_notenoughspace);

    public void setProgress(ExtractionController controller, long value, long max, Color color);

    public void removePluginProgress(ExtractionController controller);

    public void onCleanedUp(ExtractionController controller);

    public void setArchive(Archive archive);

    public void setPartOfAnArchive(Boolean b);

    public Boolean isPartOfAnArchive();

    public void notifyChanges(Object type);

    public String getArchiveID();

}
